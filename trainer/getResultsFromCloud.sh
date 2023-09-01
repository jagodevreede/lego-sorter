#!/bin/bash
if [ "$#" -ne 1 ]; then
  echo "Illegal number of parameters"
  echo "Start script: runOnCloud.sh ip_of_machine"
  exit
fi

ssh ubuntu@$1 'cd lego; tar -zcvf model.tar.gz model/;'
ssh ubuntu@$1 'cd lego; python -m SimpleHTTPServer 8000;' &
sleep 3
wget http://$1:8000/model.tar.gz
ssh ubuntu@$1 'killall -9 python'