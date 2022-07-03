#/bin/bash

#This script runs on the aws instance
sudo apt install zip

curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"

sdk install java 17.0.3.6.1-amzn
sdk install maven 3.8.6

#sudo amazon-linux-extras install java-openjdk11 -y

#sudo wget http://repos.fedorapeople.org/repos/dchen/apache-maven/epel-apache-maven.repo -O /etc/yum.repos.d/epel-apache-maven.repo
#sudo sed -i s/\$releasever/6/g /etc/yum.repos.d/epel-apache-maven.repo
#sudo yum install -y apache-maven

nvcc --version
#You can select and verify a particular CUDA version with the following bash command:

sudo rm /usr/local/cuda
sudo ln -s /usr/local/cuda-10.2 /usr/local/cuda

mkdir code
tar -xvf code.tar.gz -C ./code