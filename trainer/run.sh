#!/bin/bash
export SDKMAN_DIR="$HOME/.sdkman"
[[ -s "$HOME/.sdkman/bin/sdkman-init.sh" ]] && source "$HOME/.sdkman/bin/sdkman-init.sh"
rm -rf lego/model
rm -rf lego/app/model
cd lego/app
java -jar quarkus-run.jar t