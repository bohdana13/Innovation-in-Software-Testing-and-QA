import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * V3 - memory optimization.
 *
 * What changed compared to V2:
 *  1. The file is read as a stream through a 64 KB buffer - the text is NOT loaded into memory as a whole.
 *  2. No String copies of the text, no regex, no split() and no words[] array: words are parsed by hand, byte by byte.
 *  3. There are no String / Integer objects for words: the keys live in a single byte pool,
 *     and the counters live in primitive arrays (open addressing, linear probing).
 *  4. The top 30 is selected by insertion into a 30-element array - without sorting all the unique words.
 *
 * Memory now depends on the number of UNIQUE words, not on the size of the file.
 */
public class MainV3 {

    private static final int TOP = 30;

    private static int[] table = new int[1 << 12];
    private static int[] hashes = new int[1 << 10];
    private static int[] offsets = new int[1 << 10];
    private static int[] lengths = new int[1 << 10];
    private static int[] counts = new int[1 << 10];
    private static byte[] pool = new byte[1 << 14];
    private static int poolSize = 0;
    private static int size = 0;

    public static void main(String[] args) throws IOException {
        String file = args.length > 0 ? args[0] : "src/txt/harry.txt";
        long t0 = System.nanoTime();
        long a0 = Metrics.allocatedBytes();

        for (String line : run(args.length > 0 ? args[0] : "src/txt/harry.txt")) {
            System.out.println(line);
        }

        Metrics.report("V3 - memory optimization", file, t0, a0);
    }

    public static String[] run(String path) throws IOException {
        table = new int[1 << 12];
        hashes = new int[1 << 10];
        offsets = new int[1 << 10];
        lengths = new int[1 << 10];
        counts = new int[1 << 10];
        pool = new byte[1 << 14];
        poolSize = 0;
        size = 0;

        byte[] buf = new byte[64 * 1024];
        byte[] word = new byte[64];
        int wlen = 0;

        try (InputStream in = Files.newInputStream(
                Paths.get(path))) {
            int n;
            while ((n = in.read(buf)) > 0) {
                for (int i = 0; i < n; i++) {
                    int b = buf[i];
                    if (b >= 'A' && b <= 'Z') {
                        b += 32;
                    }
                    if (b >= 'a' && b <= 'z') {
                        if (wlen == word.length) {
                            word = Arrays.copyOf(word, wlen * 2);
                        }
                        word[wlen++] = (byte) b;
                    } else if (wlen > 0) {
                        add(word, wlen);
                        wlen = 0;
                    }
                }
            }
        }
        if (wlen > 0) {
            add(word, wlen);
        }

        int[] top = new int[TOP];
        int topSize = 0;
        for (int e = 0; e < size; e++) {
            int pos = topSize;
            while (pos > 0 && better(e, top[pos - 1])) {
                pos--;
            }
            if (pos >= TOP) {
                continue;
            }
            int last = Math.min(topSize, TOP - 1);
            System.arraycopy(top, pos, top, pos + 1, last - pos);
            top[pos] = e;
            if (topSize < TOP) {
                topSize++;
            }
        }

        String[] result = new String[topSize];
        for (int i = 0; i < topSize; i++) {
            int e = top[i];
            result[i] = new String(pool, offsets[e], lengths[e], StandardCharsets.US_ASCII) + " " + counts[e];
        }
        return result;
    }

    private static void add(byte[] w, int len) {
        int h = 1;
        for (int i = 0; i < len; i++) {
            h = 31 * h + w[i];
        }
        h ^= (h >>> 16);

        int mask = table.length - 1;
        int slot = h & mask;
        while (table[slot] != 0) {
            int e = table[slot] - 1;
            if (hashes[e] == h && lengths[e] == len && sameBytes(e, w, len)) {
                counts[e]++;
                return;
            }
            slot = (slot + 1) & mask;
        }

        if (size == hashes.length) {
            int cap = size * 2;
            hashes = Arrays.copyOf(hashes, cap);
            offsets = Arrays.copyOf(offsets, cap);
            lengths = Arrays.copyOf(lengths, cap);
            counts = Arrays.copyOf(counts, cap);
        }
        if (poolSize + len > pool.length) {
            pool = Arrays.copyOf(pool, Math.max(pool.length * 2, poolSize + len));
        }
        System.arraycopy(w, 0, pool, poolSize, len);

        int e = size++;
        hashes[e] = h;
        offsets[e] = poolSize;
        lengths[e] = len;
        counts[e] = 1;
        poolSize += len;
        table[slot] = e + 1;

        if (size * 2 > table.length) {
            rehash();
        }
    }

    private static void rehash() {
        int[] bigger = new int[table.length * 2];
        int mask = bigger.length - 1;
        for (int e = 0; e < size; e++) {
            int slot = hashes[e] & mask;
            while (bigger[slot] != 0) {
                slot = (slot + 1) & mask;
            }
            bigger[slot] = e + 1;
        }
        table = bigger;
    }

    private static boolean sameBytes(int e, byte[] w, int len) {
        int off = offsets[e];
        for (int i = 0; i < len; i++) {
            if (pool[off + i] != w[i]) {
                return false;
            }
        }
        return true;
    }

    private static boolean better(int a, int b) {
        if (counts[a] != counts[b]) {
            return counts[a] > counts[b];
        }
        return compareWords(a, b) > 0;
    }

    private static int compareWords(int a, int b) {
        int la = lengths[a], lb = lengths[b];
        int oa = offsets[a], ob = offsets[b];
        int min = Math.min(la, lb);
        for (int i = 0; i < min; i++) {
            int d = pool[oa + i] - pool[ob + i];
            if (d != 0) {
                return d;
            }
        }
        return la - lb;
    }
}
