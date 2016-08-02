#!/bin/bash

mkdir final
IFS=$'\n'
for line in `python3 sample.py`; do
  IFS=' '
  set ${line}
  code=$1
  num_samples=$2
  bash checksum_competition.sh large/${code} final/${code} ${num_samples} 
done
