#!/usr/bin/env python3

import csv, json, math

import benchmark.config_tables as config

def convert(size):
  size = float(size)
  if size == float('inf'):
    return 'fail'
  else:
    return math.ceil(size)

def load_benchmark(fname):
  with open(fname) as dataf:
    reader = csv.DictReader(dataf)
    res = {}
    for row in reader:
      file = row['File']
      del row['File']
      res[file] = {k: convert(size) for k, size in row.items()}
    return res

def corpus(data, files):
  res = {}
  for f in files:
    for algo, size in data[f].items():
      current_sizes = res.get(algo, [])
      current_sizes.append(size)
      res[algo] = current_sizes
  res['orig'] = res['Size']
  del res['Size']
  return res

if __name__ == '__main__':
  data = load_benchmark(config.BENCHMARK_INPUT)
  cmpres = {}
  for corpus_name, files in config.STANDARD_CORPUS:
    cmpres[corpus_name] = corpus(data, files)

  with open(config.JSON_OUTPUT, 'w') as out:
    serialised = json.dumps(cmpres)
    out.write("var benchmark_results = " + serialised)