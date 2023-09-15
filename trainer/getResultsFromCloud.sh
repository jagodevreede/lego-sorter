#!/bin/bash
USERNAME=ubuntu
if [ "$#" -ne 1 ]; then
  echo "Illegal number of parameters"
  echo "Start script: runOnCloud.sh ip_of_machine"
  exit
fi

ssh $USERNAME@$1 'cd lego; tar -zcvf model.tar.gz app/model/;'
ssh $USERNAME@$1 'cd lego; python3 -m http.server 8000;' &
sleep 3
wget http://$1:8000/model.tar.gz
wget http://$1:8000/learning.log
ssh $USERNAME@$1 'killall -9 python3'