#!/bin/bash
if [ "$#" -ne 2 ]; then
    echo "Illegal number of parameters"
    echo "Start script: create.sh part_number number_of_images"
    echo "Example: create.sh 3006 5"
fi

DISABLE_AUTO_TITLE="true"

LGEO_FOLDER=/Users/jagodevreede/Downloads/ldraw/LGEO
PARTS_FOLDER=/Users/jagodevreede/Downloads/ldraw/parts/
LDVIEW_FOLDER=/Applications/LDView.app/Contents/MacOS
RENDER_SIZE=1024
#set -ex

function random_color {
  R="0.$(awk -v min=80000 -v max=99999 -v seed=$RANDOM 'BEGIN{srand(seed); print int(min+rand()*(max-min+1))}')"
  G="0.$(awk -v min=80000 -v max=99999 -v seed=$RANDOM 'BEGIN{srand(seed); print int(min+rand()*(max-min+1))}')"
  B="0.$(awk -v min=80000 -v max=99999 -v seed=$RANDOM 'BEGIN{srand(seed); print int(min+rand()*(max-min+1))}')"
  sed -i .bak "s/color rgb <LIGHT${1}RGB>/color rgb <$R,$G,$B>/" header.inc.bak
}

if [[ ! -f $1_org.pov ]]
then
    ${LDVIEW_FOLDER}/LDView ${PARTS_FOLDER}$1.dat -ExportFile=$1_org.pov
fi

function create_img {
    cp header.inc header.inc.bak

    random_color "1"
    random_color "2"
    random_color "3"

    sed '/^#version.*/ r header.inc.bak' $1_org.pov > $1.pov

    X="$(awk -v min=0 -v max=359 -v seed=$RANDOM 'BEGIN{srand(seed); print int(min+rand()*(max-min+1))}')"
    Y="$(awk -v min=0 -v max=359 -v seed=$RANDOM 'BEGIN{srand(seed); print int(min+rand()*(max-min+1))}')"
    Z="$(awk -v min=0 -v max=359 -v seed=$RANDOM 'BEGIN{srand(seed); print int(min+rand()*(max-min+1))}')"

    sed -i .bak "s/lg_$1$/&\\
    rotate<$X,$Y,$Z>/" $1.pov

    rm *.bak

    povray +L${LGEO_FOLDER}/lg +L${LGEO_FOLDER} +L${LGEO_FOLDER}/ar +W${RENDER_SIZE} +H${RENDER_SIZE} -GA +WL0 $1.pov
    mv $1.png $1/$1-$2.png
}

mkdir -p $1
mkdir -p bricks/$1

for (( i=0; i<$2; i++ ))
do
	create_img $1 $i
  magick $1/$1-$i.png +noise Impulse -attenuate 0.2 -resize 'x224' -gaussian-blur 50  ./bricks/$1/$1-$i.png &
  echo "Created $1 #$2"
  echo -en "\033]0; $1 $i \a"
done

