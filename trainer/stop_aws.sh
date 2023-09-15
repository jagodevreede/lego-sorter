#!/bin/bash

# Specify the name of the EC2 instance you want to start
INSTANCE_NAME="pytorch 2"

# Find the instance ID using the AWS CLI and store it in a variable
INSTANCE_ID=$(aws ec2 describe-instances --filters "Name=tag:Name,Values=${INSTANCE_NAME}" --query "Reservations[0].Instances[0].InstanceId" --output text)

# Check if INSTANCE_ID is empty (no matching instance found)
if [ -z "$INSTANCE_ID" ]; then
  echo "No EC2 instance with the name '${INSTANCE_NAME}' found."
  exit 1
fi

# Stop the EC2 instance when done
aws ec2 stop-instances --instance-ids "$INSTANCE_ID" > /dev/null 2>&1

# Check the response to confirm if the instance has stopped successfully
if [ $? -eq 0 ]; then
  echo "Stopped EC2 instance with ID: $INSTANCE_ID"
else
  echo "Failed to stop EC2 instance with ID: $INSTANCE_ID"
fi
