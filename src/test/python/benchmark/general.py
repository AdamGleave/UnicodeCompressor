import glob, os, sys
import benchmark.config as config

def find_files(patterns):
  acc = set()
  for p in patterns:
    matches = glob.glob(os.path.join(config.CORPUS_DIR, p))
    matches = filter(os.path.isfile, matches)
    matches = map(lambda p: os.path.relpath(p, config.CORPUS_DIR), matches)
    acc = acc.union(matches)
  return acc

def find_all_files():
  acc = set()
  for path, subdirs, files in os.walk(config.CORPUS_DIR):
    for name in files:
      abspath = os.path.join(path, name)
      relpath = os.path.relpath(abspath, config.CORPUS_DIR)
      acc.add(relpath)
  return acc

def include_exclude_files(include, exclude):
  if include:
    include_files = find_files(include)
  else:
    include_files = find_all_files()

  if exclude:
    exclude_files = find_files(exclude)
  else:
    exclude_files = []

  files = list(include_files.difference(exclude_files))
  files.sort()

  if files == []:
    print("ERROR: " + str(include) + " - " + str(exclude) + " does not match any files.")
    sys.exit(1)

  return files