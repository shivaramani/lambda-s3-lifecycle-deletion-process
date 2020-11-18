
resource "aws_iam_policy" "lifecycle_processorlambda_logging_policy" {
  name = "${var.app_prefix}-lambda-logging-policy"
  path = "/"
  description = "IAM policy for logging from a lambda"

  policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": [
        "logs:CreateLogStream",
        "logs:PutLogEvents"
      ],
      "Resource": "arn:aws:logs:*:*:*",
      "Effect": "Allow"
    }
  ]
}
EOF
}

resource "aws_iam_role_policy_attachment" "lambda_logs" {
  role = "${aws_iam_role.life_cycle_processor_role.name}"
  policy_arn = "${aws_iam_policy.lifecycle_processorlambda_logging_policy.arn}"
}
