package com.example;

import com.amazonaws.services.lambda.runtime.Context;

import com.amazonaws.services.lambda.runtime.RequestHandler;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.iterable.S3Objects;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.services.s3.model.lifecycle.LifecycleFilter;
import com.amazonaws.services.s3.model.lifecycle.LifecycleTagPredicate;
import com.amazonaws.util.StringUtils;

import java.text.ParseException;
import java.util.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.stream.Collectors;


public class S3LifeCycleHandler implements RequestHandler<InputRequest, String> {

    static AmazonS3 s3Client = AmazonS3ClientBuilder.standard().build();
    static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    @Override
    public String handleRequest(final InputRequest inputRequest, final Context context) {

        if(inputRequest != null && inputRequest.getPrefix() != null) {
            String s3SourceBucket = System.getenv("S3_SOURCE_BUCKET");
            String s3CleanUpDays = System.getenv("S3_CLEANUP_DAYS");

            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
            c.set(Calendar.HOUR_OF_DAY, 0);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
            c.add(Calendar.DATE, 0);
            Date today = c.getTime();
            String runDate = dateFormat.format(today);
            System.out.println("RunDate for lifecycle - " + runDate);


            System.out.println(String.format("Received s3SourceBucket - %s and s3CleanUpDays - %s", s3SourceBucket, s3CleanUpDays));

            setDeleteLifeCycleTags(s3SourceBucket, inputRequest.getPrefix());
            setDeleteLifeCycleForPrefix(runDate, s3SourceBucket);
            cleanupPreviousLifeCycleRules(s3SourceBucket, Integer.valueOf(s3CleanUpDays));

            return "200 OK";
        }
        return "500 ERROR";
    }

    /*
     * This method adds a lifecycle rule to the prefix path. The objects within the prefix will expire the next day
     * ex: RuleId: delete_lifecycle_2020_11_18, Prefix: product=1
    * */
    private static Boolean setDeleteLifeCycleTags(String bucketName, String prefix){
      Boolean processComplete = false;
      try{
          System.out.println(String.format("Received BucketName - %s and Prefix - %s", bucketName, prefix));

          List<Tag> deleteTags = new ArrayList<Tag>();
          Tag deleteTag = new Tag("product-delete-lifecycle-marker", "YES");
          deleteTags.add(deleteTag);

          Iterable<S3ObjectSummary> objectSummaries = S3Objects.withPrefix(s3Client, bucketName, prefix);
          Stream<S3ObjectSummary> objectStream = StreamSupport.stream(objectSummaries.spliterator(), true);
          List<S3ObjectSummary> prefixedObjects = objectStream.collect(Collectors.toList());
          prefixedObjects.parallelStream().forEach(s3ObjectSummary -> {
              System.out.println("Getting each file to set object taggg");
              String key = s3ObjectSummary.getKey();
              System.out.printf("Reading Object with key '%s'n", key);

              s3Client.setObjectTagging(new SetObjectTaggingRequest(bucketName, key, new ObjectTagging(deleteTags)));
              System.out.println("Tagging object prefix DONE : " + key + " on bucket - " + bucketName);
          });

          processComplete = true;
      } catch (AmazonServiceException e) {
          e.printStackTrace();
      } catch (SdkClientException ex) {
          ex.printStackTrace();
      } catch (Exception exp){
          exp.printStackTrace();
      }

      return processComplete;

  }

  /*
   * This method adds a lifecycle rule to the prefix path. The objects within the prefix will expire the next day
   * The method removes if there are any existing rule_id_<runDate>
   * ex: RuleId: delete_lifecycle_2020_11_18
   * */
  private static Boolean setDeleteLifeCycleForPrefix(String runDate, String bucketName){
      Boolean processComplete = false;
      try{
          System.out.println(String.format("Received BucketName - %s and Date - %s", bucketName, runDate));
          BucketLifecycleConfiguration configuration = s3Client.getBucketLifecycleConfiguration(bucketName);
          String s = (configuration == null) ? "No configuration found." : "Configuration found.";
          System.out.println("Configuration - " + s);

          String idSource = runDate;
          String id = String.format("%s%s", "delete_lifecycle_", idSource.replaceAll("-","_").toLowerCase());
          System.out.println("RuleId - " + id);

          // If similar rule exist for the runDate. remove it and add them below with new expiration
          BucketLifecycleConfiguration.Rule  existingRule = null;
          if(configuration != null){
              List<BucketLifecycleConfiguration.Rule> allRules =  configuration.getRules();
              existingRule = allRules.stream()
                      .filter(rule -> rule.getId().equalsIgnoreCase(id))
                      .findFirst()
                      .orElse(null);

          }

          if(existingRule != null && !StringUtils.isNullOrEmpty(existingRule.getId())){
              configuration.getRules().remove(existingRule);
              System.out.println("Removed existing ruleId - " + existingRule.getId());
          }

          DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'00:00:000+0000");
          Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
          c.set(Calendar.HOUR_OF_DAY, 0);
          c.set(Calendar.MINUTE, 0);
          c.set(Calendar.SECOND, 0);
          c.set(Calendar.MILLISECOND, 0);
          c.add(Calendar.DATE, 1);
          Date tomorrow = c.getTime();
          System.out.println("Expiration being set to midnight tomorrow - " + dateFormat.format(tomorrow));

          List<Tag> deleteTags = new ArrayList<Tag>();
          Tag deleteTag = new Tag("product-delete-lifecycle-marker", "YES");
          deleteTags.add(deleteTag);

          // for the objects applied with deletetags(above), set a lifecycle rule to delete (with an expiration date)
          if(configuration == null){
              configuration = new BucketLifecycleConfiguration().withRules(new ArrayList<>());
          }
          configuration.getRules().add(
                  new BucketLifecycleConfiguration.Rule()
                          .withId(id)
                          .withFilter(new LifecycleFilter(new LifecycleTagPredicate(deleteTag)))
                          .withExpirationDate(c.getTime())
                          .withStatus(BucketLifecycleConfiguration.ENABLED));
          System.out.println(String.format("Adding the rule %s for %s and Date - %s", id, bucketName, runDate));

          // Save the configuration.
          s3Client.setBucketLifecycleConfiguration(bucketName, configuration);

          System.out.println(String.format("Successfully added the rule %s for %s and Date - %s", id, bucketName, runDate));

          processComplete = true;
      } catch (AmazonServiceException e) {
          e.printStackTrace();
      } catch (SdkClientException ex) {
          ex.printStackTrace();
      } catch (Exception exp){
          exp.printStackTrace();
      }

      return processComplete;

  }

  /*
   * This method removes all the lifecycle rule created prior with prefix "delete_lifecycle_".
   * Based on CLEAN_UP_DAYS any rule created prior to last X days will be removed
   * */
  private static void cleanupPreviousLifeCycleRules(String bucketName, int cleanupDays) {

      try{
          System.out.println(String.format("Received BucketName - %s to clean up S3 Lifecycle Rules", bucketName));
          BucketLifecycleConfiguration configuration = s3Client.getBucketLifecycleConfiguration(bucketName);
          String s = (configuration == null) ? "No configuration found." : "Configuration found.";
          System.out.println("Configuration - " + s);

          Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
          c.set(Calendar.HOUR_OF_DAY, 0);
          c.set(Calendar.MINUTE, 0);
          c.set(Calendar.SECOND, 0);
          c.set(Calendar.MILLISECOND, 0);
          c.add(Calendar.DATE, -cleanupDays+1);

          Date prevDate = c.getTime();
          String validationDate =  getDateSring(prevDate);
          Date valDate = getFormattedDate(validationDate);
          System.out.println("Cleanup Lifecycle for expiration date till - " +validationDate);

          String ruleIdPrefix = "delete_lifecycle_";

          List<BucketLifecycleConfiguration.Rule> allRules =  configuration.getRules();
          List<BucketLifecycleConfiguration.Rule> filteredRules = allRules.stream()
                  .filter(rule ->
                              rule.getExpirationDate() != null &&
                              rule.getId().startsWith(ruleIdPrefix) &&
                              valDate.compareTo(getFormattedDate(getDateSring(rule.getExpirationDate()))) >= 0)
                  .collect(Collectors.toList());

          if(filteredRules.size() > 0){
              System.out.println("Removing rules created on or before - " + validationDate);
              filteredRules.forEach(removeRule -> {
                  System.out.println("Removed ruleId - " + removeRule.getId());
                  configuration.getRules().remove(removeRule);
              });
              ;
              s3Client.setBucketLifecycleConfiguration(bucketName, configuration);
              System.out.println(String.format("Successfully removed the rule for bucket %s", bucketName));
          }else{
              System.out.println(String.format("No prior rules to be cleaned up for the bucket %s", bucketName));
          }
      }
      catch (AmazonServiceException e) {
          e.printStackTrace();
      } catch (SdkClientException ex) {
          ex.printStackTrace();
      } catch (Exception exp){
          exp.printStackTrace();
      }
  }

  private static String getDateSring(Date date){
      return dateFormat.format(date);
  }

  private static Date getFormattedDate(String dateTime){
      Date out = new Date();
      try {
          out = dateFormat.parse(dateTime);
      } catch (ParseException e) {
          e.printStackTrace();
      }
      return out;
  }
}