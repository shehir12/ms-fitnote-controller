#!/bin/bash
# awslocal sns create-topic --name test-2
# awslocal sqs create-queue --queue-name test-2
# awslocal sns set-subscription-attributes --subscription-arn "$(awslocal sns subscribe --return-subscription-arn --topic-arn "arn:aws:sns:us-east-1:000000000000:test-2" --protocol sqs --notification-endpoint "arn:aws:sqs:elasticmq:000000000000:test-2" | python3 -c "import sys, json; print(json.load(sys.stdin)['SubscriptionArn'])")" --attribute-name FilterPolicy --attribute-value "{\"x-dwp-routing-key\":\{\"prefix\": \"test-2\"\}}"