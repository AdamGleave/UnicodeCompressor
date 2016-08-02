#!/bin/bash

if [[ $# -ne 3 ]]; then
  echo "usage: <src directory> <dst directory> <number to retain>"
  exit 1
fi

SRC=$1
DST=$2
NUM=$3

mkdir -p ${DST}
files=${SRC}/*.txt
i=0
for file in `md5sum ${files} | msort -e 1,32 -l -y hexadecimal | head -n ${NUM} | cut -d" " -f 3`; do 
  dir=`dirname ${file}`
  base=`basename ${file}`
  cp ${file} ${DST}/${i}_${base}
  let i=$i+1
done
