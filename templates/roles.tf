
data "aws_iam_policy_document" "AWSLambdaTrustPolicy" {
  statement {
    actions    = ["sts:AssumeRole"]
    effect     = "Allow"
    principals {
      type        = "Service"
      identifiers = ["lambda.amazonaws.com"]
    }
  }
}
resource "aws_iam_role" "life_cycle_processor_role" {
  name = "${var.app_prefix}-lambda-role"
  assume_role_policy = "${data.aws_iam_policy_document.AWSLambdaTrustPolicy.json}"
}

resource "aws_iam_role_policy_attachment" "lifecycle_processorlambda_policy" {
  role       = "${aws_iam_role.life_cycle_processor_role.name}"
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

resource "aws_iam_role_policy" "s3_lambda_inline_policy" {
  name   = "${var.app_prefix}-s3_lambda_inline_policy"
  role   = "${aws_iam_role.life_cycle_processor_role.id}"
  policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:PutObjectTagging",
        "s3:PutObject*",
        "s3:PutLifecycleConfiguration",
        "s3:List*",
        "s3:GetObject*",
        "s3:GetLifecycleConfiguration",
        "s3:DeleteObject*"
      ],
      "Resource": "*"
    }
  ]
}
EOF
}


resource "aws_iam_role" "lifecycle_processor_invocation_role" {
  name = "${var.app_prefix}-api-gateway-auth-invocation"
  path = "/"

  assume_role_policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": "sts:AssumeRole",
      "Principal": {
        "Service": "apigateway.amazonaws.com"
      },
      "Effect": "Allow",
      "Sid": ""
    }
  ]
}
EOF
}

