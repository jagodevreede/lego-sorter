#!/bin/bash
USERNAME=ubuntu
UPDATE=1
if [ "$#" -ne 1 ]; then
  echo "Illegal number of parameters"
  echo "Start script: runOnCloud.sh ip_of_machine"
  exit
fi

mvn -f ../pom.xml -am -pl trainer clean package -P arm

# kill old process if there are any, and create folder
ssh -o "StrictHostKeyChecking no" $USERNAME@$1 'killall -9 java; mkdir -p lego/app/app/'

if [ "$UPDATE" -eq 1 ]; then
  scp ./target/quarkus-app/app/*.jar $USERNAME@$1:./lego/app/app/
  scp ./target/quarkus-app/lib/main/org.acme.ml-common*.jar $USERNAME@$1:./lego/app/lib/main/
else
  tar -cf app.tar.gz -C target/quarkus-app .
  scp app.tar.gz $USERNAME@$1:./lego/app.tar.gz
  ssh $USERNAME@$1 'tar -xf ./lego/app.tar.gz -C ./lego/app'
fi

# node manager and gpu node exporter are running on port 9100 and 9835 on the remote machine, map them to local host so that we can always scrape the same ip (localhost)
ssh -L localhost:9101:localhost:9100 -L localhost:9835:localhost:9835 $USERNAME@$1 < run.sh

./getResultsFromCloud.sh $1