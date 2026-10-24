import java.lang.management.ManagementFactory;

public final class Metrics {

    private Metrics() {
    }

    public static long allocatedBytes() {
        com.sun.management.ThreadMXBean bean =
                (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        return bean.getThreadAllocatedBytes(Thread.currentThread().getId());
    }

    public static void report(String version, String file, long startNanos, long startAlloc) {
        long ms = (System.nanoTime() - startNanos) / 1_000_000;
        long allocKb = (allocatedBytes() - startAlloc) / 1024;

        System.out.println();
        System.out.println("============== " + version + " ==============");
        System.out.println("File:               " + file);
        System.out.println("Execution time:      " + ms + " мс");
        System.out.printf("Memory:   %.2f МБ (%d KB)%n", allocKb / 1024.0, allocKb);
        System.out.println("=====================================");
    }
}
