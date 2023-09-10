#!/bin/bash
USERNAME=ubuntu
if [ "$#" -ne 1 ]; then
  echo "Illegal number of parameters"
  echo "Start script: runOnCloud.sh ip_of_machine"
  exit
fi

mvn -f ../pom.xml -am -pl trainer clean package -P gpu

scp target/trainer-1.0.0-SNAPSHOT-shaded.jar $USERNAME@$1:./lego/trainer-1.0.0-SNAPSHOT-shaded.jar

# kill old process if there are any
ssh $USERNAME@$1 'killall -9 java; rm -rf lego/model'

# node manager and gpu node exporter are running on port 9100 and 9835 on the remote machine, map them to local host so that we can always scrape the same ip (localhost)
ssh -L localhost:9101:localhost:9100 -L localhost:9835:localhost:9835 $USERNAME@$1 'cd lego; /home/ubuntu/.sdkman/candidates/java/17.0.3.6.1-amzn/bin/java -jar trainer-1.0.0-SNAPSHOT-shaded.jar'

./getResultsFromCloud.sh $1