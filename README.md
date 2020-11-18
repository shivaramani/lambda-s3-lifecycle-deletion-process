### Deleting large S3 files using AWS Lambda & Amazon S3 Expiration policies reliably

Deleting larger objects in AWS S3 can be cumbersome and sometimes demands repetition and retries possibly due to the nature of large size (& more number) of the files. In one such scenario is handled today using hourly EMR job operations (one master and multi nodes) on these S3 files. Due to the nature of large size/number of files, the process runs for longer hours and occasionally had to be retried due to failures.

To mitigate the deletion process more reliably and cheaper, we leveraged shorter compute execution using AWS Lambda in combination with AWS S3 lifecycle policies. The lambda can be scheduled using an CloudWatch Event rule (or using  AWS StepFunctions or Apache Airflow etc.,). In this situation the SLA for the deletion of the files can be upto 48 hours.

This example provides infrastructure and sample Java code (for the Lambda) to delete the s3 files using lifecycle policy leveraging object expiration techniques

High level Steps

- The solution has "src" and "templates". src has the java sample lambda. templates have the .tf files. 
- Upon "mvn clean package" "target" folder will have the jar generated for the lambda.
- terraform commands (init, plan, apply from templates directory) to setup the infrastructure. This also pushes the built jar to the lambda. 

- "exec.sh" has the aws cli/bash commands invoke the lambda and set the lifecycle expiration policy and object tagging
*** NOTE *** "exec.sh" Make sure to replace <YOUR_ACCOUNT_NUMBER> with your actual AWS Account number (Refer line DEPLOY_ACCOUNT_NUMBER=<YOUR_ACCOUNT_NUMBER> )

1) Initial Setup

	 - Build java source
	 - Run terraform templates to create S3/Lambda. This pushes the above jar to the lambda
	 - At this point you have a lambda, S3 available

2) Test the code

	 - Run the "exec.sh". 
	    - This creates sample s3 files
	 - Sample commands provided below to invoke the lambda from cli. To invoke the AWS Console > Lambda proide the below sample input json and click "test"
	    ```
            {
                "prefix": "product=1"
            }
          ```
	    
	    lambda sets the individual s3 object tags (under product=1 partition) and lifecycle policy for the tags
	    - Open the "output.txt" to see the response or refer below steps to validate in AWS Console

3) Validating the S3 Objects in console	 
    
    - After running above steps, open AWS Console > S3 > s3-lifecycle-process-dev-bucket-<Your-Accountnumber>
    - Open "Objects" tab, you should see sample json for products (generated from above exec.sh scripts execution)
    - Open "Management" tab, you should see "delete_lifecycle_<YYYY>_<MM>_<YY>" (ex: delete_lifecycle_2020_11_18 ) under "Lifecycle rules"
    - The scope of the lifecycle would be based on "product-delete-lifecycle-marker: YES" tag
    - Open "Objects" tag. Navigate to  product=1\year=<YEAR>\month=<MONTH>\day=<DAY>\<guid>.json
    - Under the "Tags" you would notice  "product-delete-lifecycle-marker: YES" tag

![Alt text](s3%20lifecycle%20process.png?raw=true "Title")

##### Commands

```
    $ mvn clean package
    
    $ cd templates
    
    $ terraform init
    
    $ terraform plan
    
    $ terraform apply
    
    $ cd ..
    
    $ chmod +x exec.sh
    
    $ ./exec.sh
    
```

# Sample commands to trigger lambda

```
    aws lambda invoke --function-name "s3-lifecycle-process-lambda" --cli-binary-format raw-in-base64-out  --payload '{"prefix":"product=1"}'  "output.txt"
    
    aws lambda invoke --function-name "s3-lifecycle-process-lambda" --cli-binary-format raw-in-base64-out  --payload '{"prefix":"product=2"}'  "output.txt"
    
    cat output.txt
    >>> Output: "200 OK"
```

### References

https://docs.aws.amazon.com/AmazonS3/latest/dev/lifecycle-configuration-examples.html