import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;

/**
 * V1 - the original version (the algorithm is unchanged).
 * Only the measurements and the file path taken from the command-line argument were added.
 */
public class MainV1 {

    public static void main(String[] args) throws IOException {
        String file = args.length > 0 ? args[0] : "src/txt/harry.txt";
        long t0 = System.nanoTime();
        long a0 = Metrics.allocatedBytes();

        for (String line : run(args.length > 0 ? args[0] : "src/txt/harry.txt")) {
            System.out.println(line);
        }

        Metrics.report("V1 - original", file, t0, a0);
    }

    public static String[] run(String path) throws IOException {
        String content = new String(
                Files.readAllBytes(Paths.get(path))
        );

        content = content
                .replaceAll("[^A-Za-z ]", " ")
                .toLowerCase(Locale.ROOT);

        String[] words = content.split(" +");

        Arrays.sort(words);

        String distinctString = " ";

        for (int i = 0; i < words.length; i++) {
            if (!distinctString.contains(words[i])) {
                distinctString += words[i] + " ";
            }
        }

        String[] distincts = distinctString.split(" ");
        int[] freq = new int[distincts.length];

        for (int i = 0; i < distincts.length; i++) {
            int count = 0;

            for (int j = 0; j < words.length; j++) {
                if (distincts[i].equals(words[j])) {
                    count++;
                }
            }

            freq[i] = count;
        }

        for (int i = 0; i < distincts.length; i++) {
            distincts[i] += " " + freq[i];
        }

        Arrays.sort(
                distincts,
                Comparator.comparing(
                        str -> Integer.valueOf(
                                str.replaceAll("[^0-9]", "")
                        )
                )
        );

        String[] top = new String[30];
        for (int i = 0; i < 30; i++) {
            top[i] = distincts[distincts.length - 1 - i];
        }
        return top;
    }
}