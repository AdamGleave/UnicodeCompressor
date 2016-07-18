#!/usr/bin/env python3

import sys, random

def usage():
    print("{0}: <number of characters> <start> <stop>".format(sys.argv[0]))
    sys.exit(1)

if len(sys.argv) != 4:
    usage()

num_chars = int(sys.argv[1])
start = int(sys.argv[2])
stop = int(sys.argv[3])

for i in range(num_chars):
    codepoint = random.randint(start, stop)
    sys.stdout.write(chr(codepoint))
