#!/bin/bash
AMI_ID=ami-07df274a488ca9195
TYPE=t2.micro
KEY_NAME=jago_openvalue_eu
GROUP_IDS=sg-003dc412a3f653533
USER_NAME=ec2-user

CLI_OUTPUT=$(aws ec2 run-instances --image-id $AMI_ID --count 1 --instance-type $TYPE --key-name $KEY_NAME --security-group-ids $GROUP_IDS)
INSTANCE_ID=$(echo $CLI_OUTPUT | jq -r '.Instances[0].InstanceId')

echo "Waiting for instance: $INSTANCE_ID"
aws ec2 wait instance-running --instance-ids  $INSTANCE_ID
HOSTNAME=$(aws ec2 describe-instances --instance-ids $INSTANCE_ID --query 'Reservations[*].Instances[*].PublicIpAddress' --output text)
echo "EC2 instance running: ID=$INSTANCE_ID Public-IP=$HOSTNAME"

exit 0

source ./before.sh

ssh -oStrictHostKeyChecking=no $USER_NAME@$HOSTNAME 'bash -s' < ontarget.sh

source ./after.sh

aws ec2 terminate-instances --instance-ids $INSTANCE_ID | grep -e "PreviousState" -e "CurrentState" -e "Name"
aws ec2 wait instance-stopped --instance-ids  $INSTANCE_ID

echo " EC2 instance with ID $INSTANCE_ID has stopped..."