package Logic;

public class FPSCounter {
    private long lastTime;
    private int frameCount;
    private int currentFPS;
    private long lastPrintTime;
    private static final long PRINT_INTERVAL = 1000;

    private static final int OPTIMAL_FPS = 60;
    private static final int GOOD_FPS = 45;
    private static final int ACCEPTABLE_FPS = 30;

    public FPSCounter() {
        lastTime = System.nanoTime();
        lastPrintTime = System.currentTimeMillis();
        frameCount = 0;
        currentFPS = 0;
    }

    /**
     * Call this once per frame in your game loop
     */
    public void update() {
        frameCount++;
        long currentTime = System.nanoTime();
        long elapsedNano = currentTime - lastTime;

        if (elapsedNano >= 1_000_000_000L) {
            currentFPS = frameCount;
            frameCount = 0;
            lastTime = currentTime;
        }

        long currentMillis = System.currentTimeMillis();
        if (currentMillis - lastPrintTime >= PRINT_INTERVAL) {
            printFPS();
            lastPrintTime = currentMillis;
        }
    }

    /**
     * Print FPS with performance status
     */
    private void printFPS() {
        String status;

        if (currentFPS >= OPTIMAL_FPS) {
            status = "✅ EXCELLENT";
        } else if (currentFPS >= GOOD_FPS) {
            status = "✔️ GOOD";
        } else if (currentFPS >= ACCEPTABLE_FPS) {
            status = "⚠️ ACCEPTABLE";
        } else {
            status = "❌ POOR - NEEDS OPTIMIZATION!";
        }

        System.out.println(String.format(
                "[FPS] Current: %d | Status: %s | Target: %d",
                currentFPS, status, OPTIMAL_FPS
        ));

        if (currentFPS < ACCEPTABLE_FPS) {
            System.err.println("⚠️ WARNING: FPS below 30! Game is lagging!");
        }
    }

    /**
     * Get current FPS value
     */
    public int getCurrentFPS() {
        return currentFPS;
    }

    /**
     * Check if performance is good
     */
    public boolean isPerformanceGood() {
        return currentFPS >= GOOD_FPS;
    }
}