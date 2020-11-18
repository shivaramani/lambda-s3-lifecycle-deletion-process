resource "aws_lambda_function" "life_cycle_processor" {
  filename      = "${var.lambda_source_zip_path}"
  function_name = "${var.app_prefix}-lambda"
  role          = "${aws_iam_role.life_cycle_processor_role.arn}"
  handler       = "com.example.S3LifeCycleHandler::handleRequest"
  runtime       = "java8"
  memory_size   = 2048
  timeout       = 300
  
  source_code_hash = "${filebase64sha256(var.lambda_source_zip_path)}"
  depends_on = ["aws_iam_role.life_cycle_processor_role"]

  environment {
    variables = {
      PROJECT = "Dev",
      S3_SOURCE_BUCKET = "${aws_s3_bucket.s3_lifecycle_processor_source_bucket.id}"
      S3_CLEANUP_DAYS = "2"
    }
  }
}

output "life_cycle_processor" {
  value = "${aws_lambda_function.life_cycle_processor}"
}
