#!/usr/bin/env python3

import argparse, csv, multiprocessing, resource, os

import benchmark.config_benchmark as config
import benchmark.general

def test_compressor_helper(compressor_name, fname, q):
  try:
    compressor, kwargs = config.COMPRESSORS[compressor_name]

    compressor.__wrapped__.func(fname=fname, paranoia=True, **kwargs)
    stats = resource.getrusage(resource.RUSAGE_CHILDREN)

    time = stats.ru_utime + stats.ru_stime
    mem = stats.ru_maxrss

    res = (True, (time, mem))
  except Exception as e:
    res = (False, e)
  q.put(res)

def test_compressor(compressor_name, fname):
  q = multiprocessing.Queue()
  p = multiprocessing.Process(target=test_compressor_helper, args=(compressor_name, fname, q))
  p.start()
  p.join()

  success, res = q.get()
  if success:
    return res
  else:
    print("ERROR: test with %{0} on {1} failed: {2}".format(compressor_name, fname, res))
    return None

def read_file(fname):
  fname = os.path.join(config.CORPUS_DIR, fname)
  with open(fname, 'rb') as f:
    x = "X"
    while x:
      x = f.read(4096)

if __name__ == "__main__":
  description = 'Measure the CPU and memory consumption of compression algorithms.'
  parser = argparse.ArgumentParser(description=description)
  parser.add_argument('--verbose', dest='verbose', action='store_true',
                      help='produce detailed output showing work performed.')
  parser.add_argument('--out', dest='out_fname', metavar='PATH', required=True,
                      help='write the results to CSV file PATH.')
  parser.add_argument('--include', dest='include', nargs='+',
                      help='paths which match the specified regex are included; ' +
                           'if unspecified, defaults to *.')
  parser.add_argument('--exclude', dest='exclude', nargs='+',
                      help='paths which match the specified regex are excluded.')
  parser.add_argument('--replications', dest='replications', default=config.NUM_REPLICATIONS,
                      help='number of times to run each experiment (default: {0})'
                           .format(config.NUM_REPLICATIONS))
  parser.add_argument('compressor', nargs='*',
                      help='regex of compression algorithms to match; if unspecified, defaults to *.')

  args = vars(parser.parse_args())

  out_fname = args['out_fname']
  replications = int(args['replications'])
  verbose = args['verbose']

  compressors = benchmark.general.find_compressors(args['compressor'])
  if verbose:
    print("Using compressors: " + str(compressors))

  files = benchmark.general.include_exclude_files(args['include'], args['exclude'])
  if verbose:
    print("Compressing files: " + str(files))

  csvfile = open(out_fname, 'w')
  writer = csv.writer(csvfile)
  fieldnames = ['file', 'replication', 'compressor', 'runtime', 'memory']
  writer.writerow(fieldnames)

  for fname in files:
    for replication in range(replications):
      # warm up by reading the file to make sure it's in cache
      read_file(fname)

      for compressor_name in compressors:
        if verbose:
          print("{0} / {1} / {2}".format(fname, replication, compressor_name))
        runtime, memory = test_compressor(compressor_name, fname)
        writer.writerow([fname, str(replication), compressor_name, runtime, memory])
        csvfile.flush()

  csvfile.close()