#!/bin/bash
awslocal kms create-alias --alias-name alias/test_request_id --target-key-id "$(awslocal kms create-key | python3 -c "import sys, json; print(json.load(sys.stdin)['KeyMetadata']['KeyId'])")"
