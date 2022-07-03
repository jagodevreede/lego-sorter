#!/bin/bash
AMI_ID=ami-07df274a488ca9195
TYPE=g4dn.xlarge
KEY_NAME=jago_openvalue_eu-north
GROUP_IDS=sg-0b01b22211598a093
USER_NAME=ec2-user
EBS_SIZE=200    # root EBS volume size (GB)

# See https://docs.aws.amazon.com/dlami/latest/devguide/find-dlami-id.html on how to get the ami id
# USE the following to get Deep learning ami
#AMI_ID=ami-030544fb939a57d47
# custom one with java etc installed, ready to start
AMI_ID=ami-098b759373a959119
# As the DL ami uses unbuntu we need to change the default user
USER_NAME=ubuntu

CLI_OUTPUT=$(aws ec2 run-instances --image-id $AMI_ID --count 1 --instance-type $TYPE --key-name $KEY_NAME --security-group-ids $GROUP_IDS --block-device-mapping DeviceName=/dev/sda1,Ebs={VolumeSize=$EBS_SIZE})
INSTANCE_ID=$(echo $CLI_OUTPUT | jq -r '.Instances[0].InstanceId')

echo "Waiting for instance: $INSTANCE_ID"
aws ec2 wait instance-running --instance-ids  $INSTANCE_ID
HOSTNAME=$(aws ec2 describe-instances --instance-ids $INSTANCE_ID --query 'Reservations[*].Instances[*].PublicIpAddress' --output text)
echo "EC2 instance running: ID=$INSTANCE_ID Public-IP=$HOSTNAME"

#0 : pending
#16 : running
#32 : shutting-down
#48 : terminated
#64 : stopping
#80 : stopped
STATE="0"
while [ "$STATE" != "16" ]; do
   STATE=$(aws ec2 describe-instances --instance-ids $INSTANCE_ID | jq '.Reservations[].Instances[].State.Code')
   echo $STATE
   SLEEP 2
done

#source ./before.sh

#g4dn.xlarge	4	x86_64	16	125	ssd	Up to 25 Gigabit	0.558 USD per Hour

#eu-west1
#g4dn.xlarge	4	x86_64	16	125	ssd	Up to 25 Gigabit	0.526 USD per Hour

exit 0

ssh -oStrictHostKeyChecking=no -i $HOME/.ssh/${KEY_NAME}.pem $USER_NAME@$HOSTNAME 'bash -s' < ontarget.sh

source ./after.sh

aws ec2 terminate-instances --instance-ids $INSTANCE_ID | grep -e "PreviousState" -e "CurrentState" -e "Name"
aws ec2 wait instance-stopped --instance-ids  $INSTANCE_ID

echo " EC2 instance with ID $INSTANCE_ID has stopped..."