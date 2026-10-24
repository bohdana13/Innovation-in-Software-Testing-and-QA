import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * V4 - speed optimization.
 *
 * What changed compared to V3:
 *  1. The whole file is read with a single call into one byte[] - no chunked reads, no copying of words.
 *  2. A 256-entry lookup table turns a byte into a lowercase letter (or 0 for a separator) in one array access,
 *     replacing the chain of range comparisons.
 *  3. The hash is computed while scanning, so every word is touched only once; words are compared directly
 *     in the input array, there is no copy into a separate word buffer or byte pool.
 *  4. For large inputs the text is split into chunks on word boundaries and counted by several threads,
 *     each with its own hash table; the partial tables are merged at the end.
 *  5. The top 30 is selected by insertion with a fast reject of words whose count is below the current 30th,
 *     so almost all unique words are skipped after one comparison.
 *  6. No lambdas on the hot path: avoids the invokedynamic bootstrap cost of a cold JVM.
 *
 * Trade-off: the file is held in memory as a whole (V3 streamed it), so V4 allocates more than V3 on big files.
 * Note: the "allocated" figure reported by Metrics counts only the main thread.
 */
public class MainV4 {

    private static final int TOP = 30;

    private static final int MIN_CHUNK = 16 * 1024 * 1024;

    private static final byte[] LOWER = new byte[256];

    static {
        for (int c = 'a'; c <= 'z'; c++) {
            LOWER[c] = (byte) c;
        }
        for (int c = 'A'; c <= 'Z'; c++) {
            LOWER[c] = (byte) (c + 32);
        }
    }

    public static void main(String[] args) throws IOException {
        String file = args.length > 0 ? args[0] : "src/txt/harry.txt";
        long a0 = Metrics.allocatedBytes();
        long t0 = System.nanoTime();

        for (String line : run(file)) {
            System.out.println(line);
        }

        Metrics.report("V4 - speed optimization", file, t0, a0);
    }

    public static String[] run(String path) throws IOException {
        byte[] data = Files.readAllBytes(Paths.get(path));
        int n = data.length;

        int threads = Math.max(1, Math.min(Runtime.getRuntime().availableProcessors(), n / MIN_CHUNK));
        Table total;

        if (threads == 1) {
            total = new Table(data);
            total.scan(0, n);
        } else {
            int[] bounds = new int[threads + 1];
            for (int t = 1; t < threads; t++) {
                int b = (int) ((long) n * t / threads);
                while (b < n && LOWER[data[b] & 0xFF] != 0) {
                    b++;
                }
                bounds[t] = b;
            }
            bounds[threads] = n;

            Table[] parts = new Table[threads];
            Thread[] workers = new Thread[threads];
            for (int t = 1; t < threads; t++) {
                final int idx = t;
                workers[t] = new Thread() {
                    @Override
                    public void run() {
                        Table part = new Table(data);
                        part.scan(bounds[idx], bounds[idx + 1]);
                        parts[idx] = part;
                    }
                };
                workers[t].start();
            }
            parts[0] = new Table(data);
            parts[0].scan(bounds[0], bounds[1]);

            for (int t = 1; t < threads; t++) {
                try {
                    workers[t].join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted", e);
                }
            }

            total = parts[0];
            for (int t = 1; t < threads; t++) {
                total.merge(parts[t]);
            }
        }

        return total.top();
    }

    private static final class Table {
        private final byte[] data;
        private int[] slots = new int[1 << 14];
        private int[] hashes = new int[1 << 13];
        private int[] offs = new int[1 << 13];
        private int[] lens = new int[1 << 13];
        private int[] counts = new int[1 << 13];
        private int size = 0;

        Table(byte[] data) {
            this.data = data;
        }

        void scan(int from, int to) {
            final byte[] d = data;
            int i = from;
            while (i < to) {
                byte c = LOWER[d[i] & 0xFF];
                if (c == 0) {
                    i++;
                    continue;
                }
                int start = i;
                int h = 0;
                do {
                    h = 31 * h + c;
                    i++;
                    if (i >= to) {
                        break;
                    }
                    c = LOWER[d[i] & 0xFF];
                } while (c != 0);
                add(start, i - start, h ^ (h >>> 16), 1);
            }
        }

        void merge(Table other) {
            for (int e = 0; e < other.size; e++) {
                add(other.offs[e], other.lens[e], other.hashes[e], other.counts[e]);
            }
        }

        private void add(int start, int len, int h, int cnt) {
            int mask = slots.length - 1;
            int slot = h & mask;
            int e;
            while ((e = slots[slot]) != 0) {
                e--;
                if (hashes[e] == h && lens[e] == len && same(offs[e], start, len)) {
                    counts[e] += cnt;
                    return;
                }
                slot = (slot + 1) & mask;
            }

            if (size == hashes.length) {
                int cap = size * 2;
                hashes = Arrays.copyOf(hashes, cap);
                offs = Arrays.copyOf(offs, cap);
                lens = Arrays.copyOf(lens, cap);
                counts = Arrays.copyOf(counts, cap);
            }
            e = size++;
            hashes[e] = h;
            offs[e] = start;
            lens[e] = len;
            counts[e] = cnt;
            slots[slot] = e + 1;

            if (size * 2 > slots.length) {
                rehash();
            }
        }

        private void rehash() {
            int[] bigger = new int[slots.length * 2];
            int mask = bigger.length - 1;
            for (int e = 0; e < size; e++) {
                int slot = hashes[e] & mask;
                while (bigger[slot] != 0) {
                    slot = (slot + 1) & mask;
                }
                bigger[slot] = e + 1;
            }
            slots = bigger;
        }

        private boolean same(int a, int b, int len) {
            final byte[] d = data;
            for (int k = 0; k < len; k++) {
                if (LOWER[d[a + k] & 0xFF] != LOWER[d[b + k] & 0xFF]) {
                    return false;
                }
            }
            return true;
        }

        private boolean better(int a, int b) {
            if (counts[a] != counts[b]) {
                return counts[a] > counts[b];
            }
            return compare(a, b) > 0;
        }

        private int compare(int a, int b) {
            int min = Math.min(lens[a], lens[b]);
            for (int k = 0; k < min; k++) {
                int d = LOWER[data[offs[a] + k] & 0xFF] - LOWER[data[offs[b] + k] & 0xFF];
                if (d != 0) {
                    return d;
                }
            }
            return lens[a] - lens[b];
        }

        String[] top() {
            int[] top = new int[TOP];
            int topSize = 0;
            for (int e = 0; e < size; e++) {
                if (topSize == TOP && counts[e] < counts[top[TOP - 1]]) {
                    continue;
                }
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
                char[] chars = new char[lens[e]];
                for (int k = 0; k < chars.length; k++) {
                    chars[k] = (char) LOWER[data[offs[e] + k] & 0xFF];
                }
                result[i] = new String(chars) + " " + counts[e];
            }
            return result;
        }
    }
}
