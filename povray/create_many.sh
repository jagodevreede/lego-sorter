#!/bin/bash
set -e

cat commands.txt | xargs -I CMD -P 16 bash -c CMD
echo "Done"