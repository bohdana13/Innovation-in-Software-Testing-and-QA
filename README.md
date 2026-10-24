# Word Frequency: Memory and Speed Optimization

A small Java program that counts the 30 most frequent words in a text file (`src/txt/harry.txt`). The project compares four versions of the same task.

| Version | File | Idea |
|---|---|---|
| V1 | `src/MainV1.java` | Original code: sorted word array, `String.contains`, nested loops |
| V2 | `src/MainV2.java` | `HashMap` instead of the O(n*d) counting |
| V3 | `src/MainV3.java` | Memory optimization: streaming input, no `String`/`split`/regex, primitive hash table, top 30 without a full sort |
| V4 | `src/MainV4.java` | Speed optimization: single-pass scan with a lookup table, inline hashing, fast top-30 reject, multithreading for very large files |

## What V3 does to save memory

1. Reads the file through a 64 KB buffer instead of loading the whole text.
2. Parses words byte by byte: no regex, no `split()`, no `words[]` array.
3. Stores keys in one byte pool and counters in `int[]` arrays (no `String`/`Integer` objects).
4. Picks the top 30 by insertion into a small array instead of sorting all unique words.

Memory now depends on the number of unique words, not on the file size.

## What V4 does to save time

1. Reads the whole file with one call into a `byte[]` and never copies words: keys point into that array.
2. A 256-entry lookup table maps a byte to a lowercase letter (or 0 for a separator), replacing range checks.
3. The hash is computed while scanning, so each word is touched once.
4. The top 30 uses a fast reject: after the list is full, a word with a smaller count is skipped after one comparison.
5. No lambdas on the hot path (avoids the `invokedynamic` start-up cost of a cold JVM).
6. Files of 32 MB and more are split on word boundaries and counted by several threads, then merged.

Trade-off: V4 keeps the file in memory, so it allocates a bit more than V3 (the file size plus small tables).

## Timer note

The first call of `Metrics.allocatedBytes()` initializes JMX and takes about 30 ms. In the first revision the timer was started before that call, so every version was reported ~30 ms too slow, which hid the difference between V3 and V4. Now `a0` is taken first and the timer starts right after it. Re-run all versions to get comparable numbers.

## Results

Each version prints its own report after the top-30 list. "Memory" is the total amount allocated during the run by the main thread, not peak heap usage.

Reference run: synthetic text, 0.4 MB, 77 000 words, 2-core VM, Java 21, median of 5-11 cold JVM starts. Replace with your own run on the real `harry.txt` and add your screenshots.

| Version |    Time | Allocated memory |
|---|--------:|-----------------:|
| V1 (original) | 3320 ms |         163.8 MB |
| V2 (HashMap) |  144 ms |           8.6 MB |
| V3 (memory optimization) |   34 ms |          0.73 MB |
| V4 (speed optimization) |   17 ms |          0.71 MB |

A cold start is dominated by JVM warm-up, so V3 and V4 look close. The `Benchmark` class repeats the run inside one JVM (median of 21 runs), which shows the speed of the code itself:

| Version | Time (warm) |
|---|---:|
| V1 | 2956 ms |
| V2 | 42.3 ms |
| V3 | 8.4 ms |
| V4 | 4.2 ms |

V4 is 2.0x faster than V3 when warm. On a 100 MB file (cold, 2 threads) V4 takes about 590 ms against about 930 ms for V3 (1.6x). On a 10 MB file V4 and V3 are about equal (~100 ms), which is why threads start only from 32 MB.

![V1](screenshots/v1.png)
![V2](screenshots/v2.png)
![V3](screenshots/v3.png)
![V4](screenshots/v4.png)

## Conclusions

- Replacing the nested loops with a `HashMap` gives the biggest speedup (V1 to V2).
- Removing text copies and per-word objects gives the biggest memory saving (V2 to V3).
- Removing per-word copies, branches and the full sort, and adding parallelism for huge inputs, gives the last speedup (V3 to V4). For a 0.4 MB file the process is limited by JVM start-up, not by the algorithm.
- V1 has a bug: `contains()` checks substrings, so short words such as `he` are skipped. V2, V3 and V4 are correct and produce identical output.

## Run

```bash
javac -encoding UTF-8 -d out src/*.java
java -cp out MainV1 src/txt/harry.txt
java -cp out MainV2 src/txt/harry.txt
java -cp out MainV3 src/txt/harry.txt
java -cp out MainV4 src/txt/harry.txt   # all versions in one table (median of 21 runs)
```

Timings vary between runs and machines; compare orders of magnitude.