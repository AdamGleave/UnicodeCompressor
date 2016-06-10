# Copyright (C) 2016, Adam Gleave
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.

#!/usr/bin/env python3

import csv, json, math

import benchmark.config_analysis as config

def convert(size):
  try:
    size = float(size)
    if size == float('inf'):
        return 'fail'
    else:
        return math.ceil(size)
  except ValueError:
    return 'fail'

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
  for corpus_name, files in config.FULL_CORPUS:
    cmpres[corpus_name] = corpus(data, files)

  with open(config.JSON_OUTPUT, 'w') as out:
    serialised = json.dumps(cmpres)
    out.write("var benchmark_results = " + serialised)
