import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * V2 - algorithmic optimization (time): a HashMap instead of the O(n*d) counting.
 * The whole text and the array of words still sit in memory, as before.
 */
public class MainV2 {

    public static void main(String[] args) throws IOException {
        String file = args.length > 0 ? args[0] : "src/txt/harry.txt";
        long t0 = System.nanoTime();
        long a0 = Metrics.allocatedBytes();

        for (String line : run(args.length > 0 ? args[0] : "src/txt/harry.txt")) {
            System.out.println(line);
        }

        Metrics.report("V2 - HashMap", file, t0, a0);
    }

    /** Повертає топ-30 рядків виду "слово кількість". */
    public static String[] run(String path) throws IOException {
        String content = new String(
                Files.readAllBytes(Paths.get(path))
        );

        content = content
                .replaceAll("[^A-Za-z ]", " ")
                .toLowerCase(Locale.ROOT);

        String[] words = content.split(" +");

        Map<String, Integer> freq = new HashMap<>();
        for (String w : words) {
            if (!w.isEmpty()) {
                freq.merge(w, 1, Integer::sum);
            }
        }

        // спочатку більша частота, при рівності - слово "більше" за алфавітом (як у V1)
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(freq.entrySet());
        entries.sort((x, y) -> {
            int c = Integer.compare(y.getValue(), x.getValue());
            return c != 0 ? c : y.getKey().compareTo(x.getKey());
        });

        int n = Math.min(30, entries.size());
        String[] top = new String[n];
        for (int i = 0; i < n; i++) {
            top[i] = entries.get(i).getKey() + " " + entries.get(i).getValue();
        }
        return top;
    }
}
