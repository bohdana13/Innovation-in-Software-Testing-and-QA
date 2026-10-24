# Word Frequency: Memory Optimization

A small Java program that counts the 30 most frequent words in a text file (`src/txt/harry.txt`). The project compares three versions of the same task.

| Version | File | Idea |
|---|---|---|
| V1 | `src/MainV1.java` | Original code: sorted word array, `String.contains`, nested loops |
| V2 | `src/MainV2.java` | `HashMap` instead of the O(n*d) counting |
| V3 | `src/MainV3.java` | Memory optimization: streaming input, no `String`/`split`/regex, primitive hash table, top 30 without a full sort |

## What V3 does to save memory

1. Reads the file through a 64 KB buffer instead of loading the whole text.
2. Parses words byte by byte: no regex, no `split()`, no `words[]` array.
3. Stores keys in one byte pool and counters in `int[]` arrays (no `String`/`Integer` objects).
4. Picks the top 30 by insertion into a small array instead of sorting all unique words.

Memory now depends on the number of unique words, not on the file size.

## Results

Each version prints its own report after the top-30 list. "Memory" is the total amount allocated during the run, not peak heap usage.

| Version |    Time | Allocated memory | Faster than V1 | Less memory than V1 |
|---|--------:|-----------------:|---:|---:|
| V1 (original) | 1957 ms |        181.27 MB | - | - |
| V2 (HashMap) |  192 ms |          9.96 MB | 9.5x | 18x |
| V3 (memory optimization) |   50 ms |          0.61 MB | 48x | ~298x |

V3 vs V2: 5x faster, 16x less memory.

![V1](screenshots/v1.png)
![V2](screenshots/v2.png)
![V3](screenshots/v3.png)

## Conclusions

- Replacing the nested loops with a `HashMap` gives the biggest speedup (V1 to V2).
- Removing text copies and per-word objects gives the biggest memory saving (V2 to V3).
- V1 has a bug: `contains()` checks substrings, so short words such as `he` are skipped. V2 and V3 are correct and produce identical output.

## Run

```bash
javac -encoding UTF-8 -d out src/*.java
java -cp out MainV1 src/txt/harry.txt
java -cp out MainV2 src/txt/harry.txt
java -cp out MainV3 src/txt/harry.txt
java -cp out Benchmark src/txt/harry.txt 3   # all versions in one table (median of 3 runs)
```

Timings vary slightly between runs and machines; compare orders of magnitude.