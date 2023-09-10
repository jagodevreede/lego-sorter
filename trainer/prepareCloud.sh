#!/bin/bash
USERNAME=ubuntu
if [ "$#" -ne 1 ]; then
  echo "Illegal number of parameters"
  echo "Start script: runOnCloud.sh ip_of_machine"
  exit
fi

if [[ ! -f training_data.tar.gz ]]; then
  echo "Compressing data"
  tar  --no-xattrs -cf  training_data.tar.gz -C ../povray/cropped .
fi

ssh $USERNAME@$1 'rm -rf lego/povray/cropped; mkdir -p lego/povray/cropped'
scp training_data.tar.gz $USERNAME@$1:./lego/training_data.tar.gz

ssh $USERNAME@$1 'tar -xf lego/training_data.tar.gz -C ./lego/povray/cropped'
ssh $USERNAME@$1 'rm lego/training_data.tar.gz; find "./lego/povray/" -type f -name ".*" -delete'
