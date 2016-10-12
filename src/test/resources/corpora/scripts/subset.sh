#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
OUT_DIR=generated_subset

mkdir $OUT_DIR
IFS=$'\n'

for line in `python3 ${DIR}/sample.py $*`; do
  IFS=' '
  set ${line}
  code=$1
  num_samples=$2
  bash ${DIR}/checksum_competition.sh dcc_large/${code} ${OUT_DIR}/${code} ${num_samples}
done
