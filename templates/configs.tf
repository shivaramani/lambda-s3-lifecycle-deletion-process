
variable "app_prefix" {
  description = "Application prefix for the AWS services that are built"
  default = "s3-lifecycle-process"
}

variable "stage_name" {
  default = "dev"
  type    = "string"
}

variable "lambda_source_zip_path" {
  description = "Java lambda zip"
  default = "..//target//s3lifecycleprocess-1.0-SNAPSHOT.jar"
}