#!/usr/bin/env python3

# Extracts plaintext from the Japanese-English parallel corpus at https://alaginrc.nict.go.jp/WikiCorpus/index_E.html

import sys
from bs4 import BeautifulSoup

soup = BeautifulSoup(sys.stdin, 'xml')
for sentence in soup("sen"):
    japanese = sentence.find("j").get_text()
    english = sentence.find("e", type="check").get_text()
    print(japanese)
    print(english)
    print("")
