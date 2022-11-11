#!/bin/bash
if [ "$#" -ne 3 ]; then
  echo "Illegal number of parameters"
  echo "Start script: create.sh part_number number_of_images postfix"
  echo "Example: create.sh 3006 5 run2"
  exit
fi
CURRENT_PART=$1

DISABLE_AUTO_TITLE="true"

LGEO_FOLDER=/Users/jagodevreede/Downloads/ldraw/LGEO
PARTS_FOLDER=/Users/jagodevreede/Downloads/ldraw/parts/
LDVIEW_FOLDER=/Applications/LDView.app/Contents/MacOS
RENDER_SIZE_H=1024
RENDER_SIZE_W=768
#set -ex

rm -rf povray_$1.log

#brick_colors=("lg_black" "lg_blue" "lg_green" "lg_teal" "lg_dark_pink" "lg_brown" "lg_grey" "lg_dark_grey" "lg_light_blue" "lg_turquoise" "lg_blue_green" "lg_pink" "lg_light_green" "lg_light_yellow" "lg_tan" "lg_light_violet" "lg_purple" "lg_violet" "lg_orange" "lg_magenta" "lg_lime" "lg_dark_tan" "lg_light_purple" "lg_medium_lavender" "lg_lavender" "lg_clear_blue" "lg_clear_neon_orange" "lg_clear_brown" "lg_clear_neon_green" "lg_clear_light_blue" "lg_clear" "lg_clear_violet" "lg_bright_purple" "lg_reddish_brown" "lg_bluish_grey" "lg_dark_bluish_grey" "lg_medium_blue" "lg_medium_green" "lg_paradisa_pink" "lg_milky_white" "lg_medium_dark_flesh" "lg_dark_purple" "lg_dark_flesh" "lg_medium_lime" "lg_pearl_blue" "lg_very_light_bluish_grey" "lg_flat_silver" "lg_pearl_white" "lg_bright_light_blue" "lg_bright_light_yellow" "lg_glow_in_dark_clear" "lg_pearl_gold" "lg_dark_brown" "lg_magnet" "lg_electric_contact_alloy" "lg_very_light_grey" "lg_undefined" "lg_medium_orange" "lg_rubber_yellow" "lg_rubber_clear_yellow" "lg_rubber_blue" "lg_rubber_red" "lg_rubber_light_gray" "lg_rubber_dark_blue" "lg_rubber_purple" "lg_rubber_light_bluish_gray" "lg_rubber_flat_silver" "lg_rubber_white")
allowed_brick_colors=("lg_solid_finish" "lg_chrome_finish" "lg_chrome_normal" "lg_metallic_finish" "lg_pearl_finish" "lg_pearl_normal" "lg_rubber_finish" "lg_rubber_normal" "lg_transparent_finish" "lg_transparent_neon_finish" "lg_transparent_normal" "lg_ior" "lg_black" "lg_blue" "lg_green" "lg_teal" "lg_cyan" "lg_red" "lg_dark_pink" "lg_brown" "lg_grey" "lg_dark_grey" "lg_light_blue" "lg_bright_green" "lg_turquoise" "lg_blue_green" "lg_salmon" "lg_pink" "lg_rose" "lg_yellow" "lg_white" "lg_light_green" "lg_light_yellow" "lg_tan" "lg_light_violet" "lg_glow_in_dark_opaque" "lg_purple" "lg_violet" "lg_violet_blue" "lg_orange" "lg_magenta" "lg_lime" "lg_dark_tan" "lg_light_purple" "lg_medium_lavender" "lg_lavender" "lg_black_ir" "lg_clear_blue" "lg_clear_green" "lg_clear_bright_green" "lg_clear_red" "lg_clear_pink" "lg_clear_neon_orange" "lg_clear_brown" "lg_clear_cyan" "lg_clear_neon_green" "lg_clear_light_blue" "lg_clear_light_purple" "lg_clear_yellow" "lg_clear" "lg_clear_violet" "lg_clear_neon_yellow" "lg_clear_orange" "lg_very_light_orange" "lg_bright_purple" "lg_reddish_brown" "lg_bluish_grey" "lg_dark_bluish_grey" "lg_medium_blue" "lg_medium_green" "lg_paradisa_pink" "lg_light_flesh" "lg_milky_white" "lg_metallic_silver" "lg_metallic_gold" "lg_medium_dark_flesh" "lg_dark_purple" "lg_dark_flesh" "lg_royal_blue" "lg_flesh" "lg_light_salmon" "lg_light_red" "lg_medium_lime" "lg_pearl_copper" "lg_pearl_grey" "lg_pearl_blue" "lg_pearl_light_gold" "lg_pearl_dark_grey" "lg_very_light_bluish_grey" "lg_flat_silver" "lg_pearl_white" "lg_bright_light_orange" "lg_bright_light_blue" "lg_rust" "lg_bright_light_yellow" "lg_sky_blue" "lg_dark_blue" "lg_dark_green" "lg_glow_in_dark_clear" "lg_pearl_gold" "lg_dark_brown" "lg_maersk_blue" "lg_dark_red" "lg_dark_azur" "lg_medium_azur" "lg_light_aqua" "lg_spring_yellowish_green" "lg_olive_green" "lg_chrome_gold" "lg_sand_red" "lg_earth_orange" "lg_sand_purple" "lg_sand_green" "lg_sand_blue" "lg_chrome" "lg_fabuland_brown" "lg_light_orange" "lg_dark_orange" "lg_magnet" "lg_electric_contact_alloy" "lg_very_light_grey" "lg_undefined" "lg_medium_orange" "lg_rubber_yellow" "lg_rubber_clear_yellow" "lg_rubber_clear" "lg_rubber_black" "lg_rubber_blue" "lg_rubber_red" "lg_rubber_light_gray" "lg_rubber_dark_blue" "lg_rubber_purple" "lg_rubber_lime" "lg_rubber_flat_silver" "lg_rubber_white")

source ./colors/color_$1.sh

function random_color {
  R="0.$(awk -v min=85000 -v max=99999 -v seed=$RANDOM 'BEGIN{srand(seed); print int(min+rand()*(max-min+1))}')"
  G="0.$(awk -v min=85000 -v max=99999 -v seed=$RANDOM 'BEGIN{srand(seed); print int(min+rand()*(max-min+1))}')"
  B="0.$(awk -v min=85000 -v max=99999 -v seed=$RANDOM 'BEGIN{srand(seed); print int(min+rand()*(max-min+1))}')"
  sed -i .bak "s/color rgb <LIGHT${1}RGB>/color rgb <$R,$G,$B>/" header.inc-$CURRENT_PART.bak
}

function random_brick_color {
  selectedexpression=""
  while [[ $selectedexpression == "" ]]; do
    selectedexpression=$(printf "%s\n" "${brick_colors[@]}" | shuf -n1)

    if [[ ! " ${allowed_brick_colors[*]} " =~ " ${selectedexpression} " ]]; then
      #echo "Color $selectedexpression was not usable"
      selectedexpression=""
    fi
  done

  sed -i .bak "s/lg_grey/$selectedexpression/" $1.pov
}

function random_background_color {
  R="0.$(awk -v min=930 -v max=999 -v seed=$RANDOM 'BEGIN{srand(seed); print int(min+rand()*(max-min+1))}' | awk '{ printf("%03d\n", $1) }' )"
  G="0.$(awk -v min=930 -v max=999 -v seed=$RANDOM 'BEGIN{srand(seed); print int(min+rand()*(max-min+1))}' | awk '{ printf("%03d\n", $1) }' )"
  B="0.$(awk -v min=910 -v max=999 -v seed=$RANDOM 'BEGIN{srand(seed); print int(min+rand()*(max-min+1))}' | awk '{ printf("%03d\n", $1) }' )"

  sed -i .bak "s/<LDXBgR>/$R/" $1.pov
  sed -i .bak "s/<LDXBgG>/$G/" $1.pov
  sed -i .bak "s/<LDXBgB>/$B/" $1.pov
}

if [[ ! -f $1_org.pov ]]
then
    ${LDVIEW_FOLDER}/LDView ${PARTS_FOLDER}$1.dat -ExportFile=$1_org.pov
fi

function create_img {
  cp header.inc header.inc-$1.bak

  random_color "1"
  random_color "2"
  random_color "3"

  rot_X="$(awk -v min=0 -v max=359 -v seed=$RANDOM 'BEGIN{srand(seed); print int(min+rand()*(max-min+1))}')"
  rot_Y="$(awk -v min=0 -v max=359 -v seed=$RANDOM 'BEGIN{srand(seed); print int(min+rand()*(max-min+1))}')"
  rot_Z="$(awk -v min=0 -v max=359 -v seed=$RANDOM 'BEGIN{srand(seed); print int(min+rand()*(max-min+1))}')"

  pos_X="$(awk -v min=-0.3 -v max=0.3 -v seed=$RANDOM 'BEGIN{srand(seed); print min+rand()*(max-min+1)}')"
  pos_Y="$(awk -v min=-0.6 -v max=0.6 -v seed=$RANDOM 'BEGIN{srand(seed); print min+rand()*(max-min+1)}')"

  # Base cam position //< 0,-2, 4 >
  cam_X="$(awk -v min=-0.2 -v max=0.2 -v seed=$RANDOM 'BEGIN{srand(seed); print min+rand()*(max-min+1)}')"
  cam_Y="$(awk -v min=-2.1 -v max=-1.9 -v seed=$RANDOM 'BEGIN{srand(seed); print min+rand()*(max-min+1)}')"
  cam_Z="$(awk -v min=17 -v max=25 -v seed=$RANDOM 'BEGIN{srand(seed); print min+rand()*(max-min+1)}')"

  sed -i .bak "s/<CAMLOC>/<$cam_X, $cam_Y, $cam_Z>/" header.inc-$1.bak

  sed "/^#version.*/ r header.inc-$1.bak" $1_org.pov > $1.pov

    sed -i .bak "s/lg_$1$/&\\
    rotate<$rot_X,$rot_Y,$rot_Z>\\
    translate <$pos_X, $pos_Y, 0>/" $1.pov

  random_brick_color $1
  random_background_color $1

  povray +L${LGEO_FOLDER}/lg +L${LGEO_FOLDER} +L${LGEO_FOLDER}/ar +W${RENDER_SIZE_W} +H${RENDER_SIZE_H} -GA +WL0 $1.pov >>povray_$1.log 2>&1
  mv $1.png $1/$1-$2-$3.png

  rm header.inc-$1.bak
  rm $1.pov.bak
  rm $1.pov
}

mkdir -p pov
mkdir -p $1

for ((i = 1; i <= $2; i++)); do
  if [[ -f stop.now ]]; then
    echo "Stopping now!!!"
    exit 1
  fi
  create_img $1 $i $3
  #magick $1/$1-$i-$3.png +noise Impulse -attenuate 0.2 -resize 'x224' -gaussian-blur 50  ./bricks/$1/$1-$i-$3.png &
  echo "Created $1 #$i/$2 $3"
  #echo -en "\033]0; $1 $i \a"
done

rm $1_org.pov
