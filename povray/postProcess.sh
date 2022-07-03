#!/bin/bash
mkdir processed
cd org

# Iterate over all folders in the current directory
for d in */ ; do
    echo "$d"
    mkdir -p ../processed/$d
    for FILE in $d*.png; do
      echo $FILE
      noiseAttenuate="0.$(awk -v min=1 -v max=1000 -v seed=$RANDOM 'BEGIN{srand(seed); print int(min+rand()*(max-min+1))}' | awk '{ printf("%05d\n", $1) }' )"
      blur="$(awk -v min=10 -v max=50 -v seed=$RANDOM 'BEGIN{srand(seed); print int(min+rand()*(max-min+1))}')"
      echo $noiseAttenuate - $blur
      magick $FILE +noise Impulse -attenuate $noiseAttenuate -gaussian-blur $blur  ../processed/$FILE
    done
done
