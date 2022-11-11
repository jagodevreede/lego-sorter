#!/bin/bash
set -e

cat commands.txt | xargs -I CMD -P 12 bash -c CMD
