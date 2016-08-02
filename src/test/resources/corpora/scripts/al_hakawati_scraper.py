#!/usr/bin/env python3

import argparse
from bs4 import BeautifulSoup
from urllib import request
from urllib.parse import urljoin

def fetch_url(url):
    response = request.urlopen(url)
    html = response.read().decode('windows-1256')
    return BeautifulSoup(html, 'html.parser')

def extract_text(url):
    soup = fetch_url(url)
    # this works for most but not all files on the site
    # return soup("div")[5].get_text()

    # rip out script and style elements
    for script in soup(["script", "style"]):
        script.extract()
    
    # heuristic approach more reliable
    rows = soup("tr")
    texts = map(lambda x : x.get_text(), rows)
    texts_annotated = map(lambda x : (len(x), x), texts)
    _length, longest_text = max(texts_annotated)
    return longest_text

def parse_index(index_url):
    file_name = index_url.split('/')[-1]
    book_name = file_name.split("index")[0]
    soup = fetch_url(index_url)
    links = soup("a")
    urls = map(lambda x: x.get("href"), links)
    return filter(lambda x : x != None and x.find(book_name) == 0, urls) 

def save_file(text, out_fname):
    with open(out_fname, 'w') as out:
        out.write(text)

def main(args):
    text = ""
    for index, relative_url in enumerate(parse_index(args.url)):
        if args.max_files and index >= args.max_files:
            break
        url = urljoin(args.url, relative_url)
        text += "\r\n" + extract_text(url)
    save_file(text, args.out)

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Scrapes from Al-Hakawati.')
    parser.add_argument('--url', type=str, help='Base URL.')
    parser.add_argument('--out', type=str, help='Output file.')
    parser.add_argument('--max-files', type=int, help='Maximum number of files to retrieve.')
    args = parser.parse_args()
    main(args)
