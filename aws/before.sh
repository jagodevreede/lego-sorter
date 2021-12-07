#/bin/bash

# This scripts runs on the local machine, after the instance is running, but before the ontarget.sh

tar -zcvf training_data.tar.gz ../povray/bricks
tar --exclude "../target" --exclude "../povray" --exclude "../lego_model.zip" --exclude "../aws" -czvf code.tar.gz ../

scp -oStrictHostKeyChecking=no code.tar.gz $USER_NAME@$HOSTNAME:./