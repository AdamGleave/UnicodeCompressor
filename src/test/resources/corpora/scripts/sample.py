#!/usr/bin/env python3

import random
import csv

SEED=42
NUM_FILES=10

random.seed(SEED)

languages = []
cumulative_speakers = 0
with open('gutenberg/languages.csv') as csvfile:
    reader = csv.DictReader(csvfile)
    for row in reader:
        cumulative_speakers += int(float(row['Speakers']) * 10)
        languages.append((row['Ethnologue'], cumulative_speakers))

step = int(cumulative_speakers / NUM_FILES)
location = random.randint(0, step)

sampled_languages = {}
for x in languages:
    code, cumulative_speakers = x
    while location < cumulative_speakers:
        num_samples = sampled_languages.get(code, 0)
        sampled_languages[code] = num_samples + 1
        location += step

codes = list(sampled_languages.keys())
codes.sort()
for code in codes:
    print(code, sampled_languages[code])
