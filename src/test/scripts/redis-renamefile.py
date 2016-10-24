#!/usr/bin/env python3

import redis
import sys

if len(sys.argv) != 4:
    print("usage: <hostname> <old filename> <new filename>")
    sys.exit(1)
hostname = sys.argv[1]
old_filename = sys.argv[2]
new_filename = sys.argv[3]

r = redis.StrictRedis(host=hostname, port=6379, db=0)
for prefix in ['benchmark.tasks.my_compressor', 'benchmark.tasks.ext_compressor']:
    for old_key in r.keys(prefix + "('" + old_filename + "',*)"):
    	new_key = old_key.decode('utf-8').replace(old_filename, new_filename)
    	r.rename(old_key, new_key.encode('utf-8'))
