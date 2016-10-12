#!/usr/bin/env python3

import argparse, csv, random, sys

random.seed(42)

parser = argparse.ArgumentParser(description='Sample from languages in proportion to their number of speakers.')
parser.add_argument('--num-samples', type=int, default=10,
                    help='number of samples to take (default: 10)')
parser.add_argument('--no-repeat', action='store_true')
args = parser.parse_args()

languages = []
cumulative_speakers = 0
with open('metadata/languages.csv') as csvfile:
    reader = csv.DictReader(csvfile)
    for row in reader:
        speakers = int(float(row['Speakers']) * 10)
        cumulative_speakers += speakers
        languages.append((row['Ethnologue'], speakers, cumulative_speakers))
overall_population = cumulative_speakers

num_samples = args.num_samples
if args.no_repeat:
  # exclusion sampling
  for i in range(num_samples):
    location = random.randint(0, overall_population)
    for i, x in enumerate(languages):
        code, speakers, cumulative_spekers = x
        if location < cumulative_speakers:
            print(code, 1)
            deleted_speakers = speakers
            for j in range(i, len(languages)):
                code, speakers, cumulative_speakers = languages[j]
                languages[j] = (code, speakers, cumulative_speakers - deleted_speakers)
            del languages[i]
            overall_population -= deleted_speakers
            break
else:
    step = int(cumulative_speakers / num_samples)
    location = random.randint(0, step)

    sampled_languages = {}
    for x in languages:
        code, speakers, cumulative_speakers = x
        while location < cumulative_speakers:
            already_sampled = sampled_languages.get(code, 0)
            sampled_languages[code] = already_sampled + 1
            location += step

    codes = list(sampled_languages.keys())
    codes.sort()
    for code in codes:
        print(code, sampled_languages[code])
