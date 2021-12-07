#!/bin/bash

FILES="./real/*"
for f in $FILES
do
 echo "Processing $f"
 magick $f +noise Impulse -attenuate 0.3 -resize 'x112' -gaussian-blur 100  ./real/$f
done