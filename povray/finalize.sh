#!/bin/bash

mkdir ./bricks/$1
FILES="./$1/*"
for f in $FILES
do
 echo "Processing $f"
 magick $f +noise Impulse -attenuate 0.2 -resize 'x112' -gaussian-blur 50  ./bricks/$f &
 sleep 0.2
done