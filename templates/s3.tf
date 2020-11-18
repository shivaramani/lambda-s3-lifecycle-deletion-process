resource "aws_s3_bucket" "s3_lifecycle_processor_source_bucket" {
  bucket =  "${var.app_prefix}-${var.stage_name}-bucket-${data.aws_caller_identity.current.account_id}"
  acl    = "private"

  tags = {
    Name        = "${var.app_prefix}-s3"
    Environment = "Dev"
  }
}