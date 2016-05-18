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

# training

These files are used as training data for algorithms where a parameter choice is required. They are not used in the final evaluation.

gogol.txt: Evenings on a farm near Dikanka, Nikolai Gogol. Russian. Public domain. From Wikisource at https://ru.wikisource.org/wiki/%D0%92%D0%B5%D1%87%D0%B5%D1%80%D0%B0_%D0%BD%D0%B0_%D1%85%D1%83%D1%82%D0%BE%D1%80%D0%B5_%D0%B1%D0%BB%D0%B8%D0%B7_%D0%94%D0%B8%D0%BA%D0%B0%D0%BD%D1%8C%D0%BA%D0%B8_(%D0%93%D0%BE%D0%B3%D0%BE%D0%BB%D1%8C)
shimazaki.txt: The Broken Commandment, Tōson Shimazaki. Japanese. Public domain. From Aozora at http://www.aozora.gr.jp/cards/000158/card1502.htm Downloaded the plaintext version ("1502_ruby_24535.zipzip"), converted from ShiftJIS to UTF-8 using iconv. Pre/postamble manually deleted.

The following files are all from Project Gutenberg, and are under the Gutenberg license: www.gutenberg.org/license I have stripped the preamble and postamble from each file.
* aristotle.txt: The Constitution of the Athenians, Aristotle. Greek. From http://www.gutenberg.org/ebooks/39963
* austen.txt: Pride and Prejudice, Jane Austen. English. From http://www.gutenberg.org/ebooks/1342
* confucius.txt: Lunyu, Fu Zi Kong. From http://www.gutenberg.org/ebooks/23839
* doyle.txt: The Adventures of Sherlock Holmes, Arthur Conan Doyle. English. From http://www.gutenberg.org/ebooks/1661
* forsberg.txt: Svensk litteraturhistoria, Hjalmar Forsberg and H. H. Henrikz. Swedish. From http://www.gutenberg.org/ebooks/49801
* jushi.txt: Tou Peng Hsien Hua, Aina Jushi. Chinese. From http://www.gutenberg.org/ebooks/25328
* rizal.txt: Ang "Filibusterismo", José Rizal. Tagalog. From http://www.gutenberg.org/ebooks/47629 
* russel.html: An essay on the foundations of geometry, Bertrand Russel. English in HTML with mathematical symbols. From http://www.gutenberg.org/ebooks/52091
