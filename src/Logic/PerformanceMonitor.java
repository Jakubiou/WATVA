package Logic;


public class PerformanceMonitor {
    private static final int REPORT_INTERVAL_MS = 5000;
    private static final int MAX_SECTIONS = 20;

    private static final String[] names    = new String[MAX_SECTIONS];
    private static final long[]   totalNs  = new long[MAX_SECTIONS];
    private static final long[]   callCount= new long[MAX_SECTIONS];
    private static final long[]   maxNs    = new long[MAX_SECTIONS];
    private static final long[]   startNs  = new long[MAX_SECTIONS];
    private static int sectionCount = 0;

    private static long lastReportTime = System.currentTimeMillis();
    private static long frameCount = 0;

    private static int indexOf(String name) {
        for (int i = 0; i < sectionCount; i++) {
            if (names[i].equals(name)) return i;
        }
        if (sectionCount < MAX_SECTIONS) {
            names[sectionCount] = name;
            totalNs[sectionCount] = 0;
            callCount[sectionCount] = 0;
            maxNs[sectionCount] = 0;
            return sectionCount++;
        }
        return -1;
    }

    public static void begin(String section) {
        int i = indexOf(section);
        if (i >= 0) startNs[i] = System.nanoTime();
    }

    public static void end(String section) {
        int i = indexOf(section);
        if (i < 0) return;
        long elapsed = System.nanoTime() - startNs[i];
        totalNs[i]   += elapsed;
        callCount[i] ++;
        if (elapsed > maxNs[i]) maxNs[i] = elapsed;
    }

    public static void frameEnd() {
        frameCount++;
        long now = System.currentTimeMillis();
        if (now - lastReportTime < REPORT_INTERVAL_MS) return;

        double seconds = (now - lastReportTime) / 1000.0;
        double fps = frameCount / seconds;

        System.out.println("\n========== PERF REPORT (fps=" + String.format("%.1f", fps) + ") ==========");
        for (int i = 0; i < sectionCount; i++) {
            if (callCount[i] == 0) continue;
            double avgMs  = (totalNs[i] / 1_000_000.0) / callCount[i];
            double maxMs  = maxNs[i] / 1_000_000.0;
            double totalMs= totalNs[i] / 1_000_000.0;
            double pct    = (totalNs[i] / 1_000_000.0) / (seconds * 1000.0) * 100.0;
            System.out.printf("  %-22s  avg=%5.2fms  max=%6.2fms  total=%7.1fms  cpu=%.1f%%%n",
                    names[i], avgMs, maxMs, totalMs, pct);
            // Reset
            totalNs[i] = 0; callCount[i] = 0; maxNs[i] = 0;
        }
        System.out.println("====================================================\n");

        frameCount = 0;
        lastReportTime = now;
    }
}