#!/bin/bash

source env.sh

# Find the instance ID using the AWS CLI and store it in a variable
INSTANCE_ID=$(aws ec2 describe-instances --filters "Name=tag:Name,Values=${INSTANCE_NAME}" --query "Reservations[0].Instances[0].InstanceId" --output text)

# Check if INSTANCE_ID is empty (no matching instance found)
if [ -z "$INSTANCE_ID" ]; then
  echo "No EC2 instance with the name '${INSTANCE_NAME}' found."
  exit 1
fi

# Check the current state of the EC2 instance
INSTANCE_STATE=$(aws ec2 describe-instances --instance-ids "$INSTANCE_ID" --query "Reservations[0].Instances[0].State.Name" --output text)

if [ "$INSTANCE_STATE" = "running" ]; then
  echo "EC2 instance with ID: $INSTANCE_ID is already running. Skipping start."
else
  # Start the EC2 instance using the AWS CLI
  aws ec2 start-instances --instance-ids "$INSTANCE_ID" > /dev/null 2>&1

  # Check the response to confirm if the instance has started successfully
  if [ $? -eq 0 ]; then
    echo "Started EC2 instance with ID: $INSTANCE_ID"
  else
    echo "Failed to start EC2 instance with ID: $INSTANCE_ID"
    exit 1
  fi

  # Wait for the instance to become ready
  while true; do
    INSTANCE_STATE=$(aws ec2 describe-instances --instance-ids "$INSTANCE_ID" --query "Reservations[0].Instances[0].State.Name" --output text)
    if [ "$INSTANCE_STATE" = "running" ]; then
      break
    fi
    sleep 5
  done

  # Wait for the status checks to pass
  while true; do
      STATUS_CHECK=$(aws ec2 describe-instance-status --instance-ids "$INSTANCE_ID" --query "InstanceStatuses[0].InstanceStatus.Status" --output text)
      if [ "$STATUS_CHECK" = "ok" ]; then
          break
      fi
      echo "Status is not ok yet: $STATUS_CHECK"
      sleep 5
  done

  echo "EC2 instance with ID: $INSTANCE_ID is now running and ready."
fi

PUBLIC_IP=$(aws ec2 describe-instances --instance-ids "$INSTANCE_ID" --query "Reservations[0].Instances[0].PublicIpAddress" --output text)
echo "Public IP address of the instance: $PUBLIC_IP"

# Login to the instance
ssh -o "StrictHostKeyChecking no" $USERNAME@$PUBLIC_IP 'echo "Hello from instance"'

./prepareCloud.sh $PUBLIC_IP
./runOnCloud.sh $PUBLIC_IP

exit

./stop_aws.sh