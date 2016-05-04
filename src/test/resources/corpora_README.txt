# Goals

The algorithm is optimised to operate over natural language text, so this forms the majority of the corpora. It is inevitable that the compression algorithm will sometimes be run on different inputs, so a number of binary files are included. The performance in this case is expected to be poor, but the algorithm should still be able to run successfully. It is also common for files to consist of a mixture of binary and text; the algorithm should be handle this gracefully, compressing the text portions at rates close to that of pure text files.

# canterbury

The various Canterbury corpora, from http://corpus.canterbury.ac.nz/

Text files are all in English, also includes some binary files and artifically generated ones (designed to trigger pathological behaviour).

# single_language

beowulf.txt: old English. Mostly ASCII, some special 'extended' Latin characters, all fairly low code points. Mixture of 1 and 2-byte long encoding.
ziemia_obiecana.txt: Polish. Extended Latin alphabet. Mixture of 1 and 2-byte long encoding.

crime_and_punishment.txt: Russian (Crylic alphabet). 2-byte encoding.

genji: Japanese. Very high code points, 3-byte long encoding. Contains both single chapter (chapter2.txt) and concatenation of the complete works (all.txt)
kokoro.txt: Japanese. Similar to Genji.

# mixedlanguage

Can the model learn two (or more) distinct clusters of codepoints?

cedict.txt: Chinese to English dictionary, from http://www.mdbg.net/chindict/chindict.php?page=cedict. It's licensed under a Creative Commons Attribution-Share Alike 3.0 License. Chinese symbols with English (ASCII) text interspersed. Big: 8.4M. 
cedict_small.txt: a random selection of 10,000 entries from the above. 748K.

creativecommonsukranian.html: from https://creativecommons.org/licenses/by/4.0/legalcode.uk Ukranian translation of the international license. Cryillic characters with HTML tags (ASCII) interspersed. Small: 40K. 

# text_binary

genji.tar: tar archive of the genji text files
kokoroziemia.tar: tar archive of two files from single_language, in different languages.

# binary

small-xscreensaver: x64 executable.
medium-git: x64 executable.
