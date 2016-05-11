#!/usr/bin/env python3

import argparse, csv, os

import benchmark.config_tables as config

def load_benchmark(fname):
  with open(fname) as dataf:
    reader = csv.DictReader(dataf)
    res = {}
    for row in reader:
      file = row['File']
      filesize = int(row['Size'])
      del row['File']
      del row['Size']
      res[file] = {k: float(size) / filesize * 8 for k, size in row.items()}
    return res

def terminate_row(x):
  return x[0:len(x) - 2] + r"\\" + "\n"

def autoscale(settings, data):
  subset = [data[f][a] for file in settings['files'] for algo in settings['algos']]
  return min(subset), max(subset)

def efficiency_format(x, scale, cm):
  smallest, largest = scale
  p = (x - smallest) / (largest - smallest)
  if p < 0 or p > 1:
    print("WARNING: value {0} outside {1}".format(x, scale))
  r, g, b, a = cm(p)
  return r'{\cellcolor[rgb]{' + '{0},{1},{2}'.format(r, g, b) + '}{' + '{0:.3f}'.format(x) + '}}'

def generate_table(settings, data):
  if settings['scale']:
    scale = settings['scale']
  else:
    scale = autoscale(settings, data)

  res = r'\setlength{\doublerulesep}{1pt}' + '\n'
  res += r'\arrayrulecolor{white}' + '\n'
  res += r'\setlength{\tabcolsep}{0pt}' + '\n'

  algos = settings['algos']
  algo_cols = 'l||' * len(algos)
  algo_cols = algo_cols[0:len(algo_cols) - 2]
  res += r'\begin{tabular}{l@{\hskip 1em}!{\color{black}\vrule width 0.5pt}l' + algo_cols + '}' + '\n'

  algo_row = r'\textbf{File} & \hspace{2pt} & '
  for algo in algos:
    abbrev = config.ALGO_ABBREVIATIONS[algo]
    algo_row += r'\textbf{' + abbrev + '} & '
  res += terminate_row(algo_row)
  res += r'\hhline{>{\arrayrulecolor{black}}-|--------}' + '\n'

  for file in settings['files']:
    filedata = data[file]
    file_abbrev = config.FILE_ABBREVIATIONS[file]
    row = '{0} & & '.format(file_abbrev)
    for algo in algos:
      val = efficiency_format(filedata[algo], scale, config.COLORMAP)
      row += '{0} & '.format(val)
    res += terminate_row(row)
    res += r'\hhline{~>{\arrayrulecolor{black}}|>{\arrayrulecolor{white}}=======}' + '\n'

  res += r'\end{tabular}' + '\n'
  return res

verbose = False

def main():
  description = "Produce LaTeX tables"
  parser = argparse.ArgumentParser(description=description)
  parser.add_argument('--verbose', dest='verbose', action='store_true',
                      help='produce detailed output showing work performed.')
  parser.add_argument('tables', nargs='*',
                      help='list of tables to generate')

  args = vars(parser.parse_args())
  global verbose
  verbose = args['verbose']

  tables = args['tables']
  if not tables:
    tables = config.TABLES.keys()

  data = load_benchmark(config.BENCHMARK_INPUT)

  for table in tables:
    if verbose:
      print("Generating " + table)
    settings = config.TABLES[table]
    result = generate_table(settings, data)
    os.makedirs(config.LATEX_OUTPUT_DIR, exist_ok=True)
    with open(os.path.join(config.LATEX_OUTPUT_DIR, table + '.tex'), 'w') as out:
      out.write(result)

if __name__ == "__main__":
  main()