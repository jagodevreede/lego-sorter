#!/bin/bash
#!/bin/bash
if [ "$#" -ne 1 ]; then
  echo "Illegal number of parameters"
  echo "Start script: runOnCloud.sh ip_of_machine"
  exit
fi

if [[ ! -f training_data.tar.gz ]]; then
  tar -zcvf training_data.tar.gz -C ../povray/cropped .
fi

ssh ubuntu@$1 'mkdir -p lego/povray/cropped'
scp training_data.tar.gz ubuntu@$1:./lego/training_data.tar.gz

ssh ubuntu@$1 'tar -xf lego/training_data.tar.gz -C ./lego/povray/cropped'
ssh ubuntu@$1 'rm lego/training_data.tar.gz'
