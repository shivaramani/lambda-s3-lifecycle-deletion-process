CURRYEAR=`date +"%Y"`
CURRMONTH=`date +"%m"`
CURRDATE=`date +"%d"`

DEPLOY_ACCOUNT_NUMBER=<YOUR_ACCOUNT_NUMBER>

echo $CURRYEAR-$CURRMONTH-$CURRDATE

echo "Creating sample files and will load to S3"
COUNTER=0
NUMBER_OF_FILES=2

# Brew install uuidgen for guids
BASEFILENAME=`uuidgen | tr "[:upper:]" "[:lower:]"-`

EXTN=".json"
S3_SUB_PATH="year="$CURRYEAR"/month="$CURRMONTH"/day="$CURRDATE
echo $S3_SUB_PATH

REGION="us-east-1"
LIFE_CYCLE_BUCKET="s3://s3-lifecycle-process-dev-bucket-"$DEPLOY_ACCOUNT_NUMBER

cd samples

while [  $COUNTER -lt $NUMBER_OF_FILES ]; do
    FILENAME=$BASEFILENAME-$COUNTER$EXTN
    echo "filename is "$FILENAME
    echo The counter is $COUNTER
    
    echo "{\"productid\": $COUNTER , \"productdesc\": \"test desc\", \"value\": \"$FILENAME\"}" >> $FILENAME

    aws s3 --region $REGION cp $FILENAME $LIFE_CYCLE_BUCKET/product=1/$S3_SUB_PATH/$FILENAME
    aws s3 --region $REGION cp $FILENAME $LIFE_CYCLE_BUCKET/product=2/$S3_SUB_PATH/$FILENAME

    echo $FILENAME " sample uploaded into S3 lifecycle buckets"
    let COUNTER=COUNTER+1 
done

cd ..