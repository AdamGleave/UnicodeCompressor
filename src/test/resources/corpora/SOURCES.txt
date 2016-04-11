# Goals

The algorithm is optimised to operate over natural language text, so this forms the majority of the corpora. It is inevitable that the compression algorithm will sometimes be run on different inputs, so a number of binary files are included. The performance in this case is expected to be poor, but the algorithm should still be able to run successfully. It is also common for files to consist of a mixture of binary and text; the algorithm should be handle this gracefully, compressing the text portions at rates close to that of pure text files.

# canterbury

The various Canterbury corpora, from http://corpus.canterbury.ac.nz/

Text files are all in English, also includes some binary files and artifically generated ones (designed to trigger pathological behaviour).

# single_language

beowulf.txt: old English. Mostly ASCII, some special 'extended' Latin characters, all fairly low code points. Mixture of 1 and 2-byte long encoding.
ziemia_obiecana.txt: Polish. Extended Latin alphabet. Mixture of 1 and 2-byte long encoding.

crime_and_punishment.txt: Russian (Crylic alphabet). 2-byte encoding.

genji: Japanese. Very high code points, 3-byte long encoding.
kokoro: Japanese. Similar to Genji.

# mixedlanguage

Can the model learn two (or more) distinct clusters of codepoints?

## mixedlanguage/japanese-english

Japanese text with side-by-side English translations.

Credit to the Japan Foundation, http://www.jpf.go.jp/e/project/culture/media/supportlist_publish/worth_sharing/index.html

License unknown.

Converted from PDF with pdftotext.

all.txt is simply concatenation of the four volumes.

## creativecommonsukranian.html

From https://creativecommons.org/licenses/by/4.0/legalcode.uk

Ukranian translation of the international license.

Hybrid HTML/Ukranian.

# text_binary

genji.tar: tar archive of the genji text files
kokoroziemia.tar: tar archive of two files from single_language, in different languages.

# binary

small-xscreensaver: x64 executable.
medium-git: x64 executable.
