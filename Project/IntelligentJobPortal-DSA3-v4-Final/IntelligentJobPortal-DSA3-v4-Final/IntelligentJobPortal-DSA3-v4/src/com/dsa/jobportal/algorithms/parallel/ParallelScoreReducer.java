package com.dsa.jobportal.algorithms.parallel;

public class ParallelScoreReducer {
    public long parallelSum(int[] values, int threadCount) throws InterruptedException {
        if (values.length == 0) return 0;
        int workers = Math.max(1, Math.min(threadCount, values.length));
        Worker[] threads = new Worker[workers];
        int chunk = (values.length + workers - 1) / workers;
        for (int i = 0; i < workers; i++) {
            int start = i * chunk;
            int end = Math.min(values.length, start + chunk);
            threads[i] = new Worker(values, start, end);
            threads[i].start();
        }
        long total = 0;
        for (int i = 0; i < workers; i++) {
            threads[i].join();
            total += threads[i].sum;
        }
        return total;
    }

    private static class Worker extends Thread {
        private final int[] values;
        private final int start;
        private final int end;
        private long sum;

        Worker(int[] values, int start, int end) {
            this.values = values;
            this.start = start;
            this.end = end;
        }

        public void run() {
            for (int i = start; i < end; i++) sum += values[i];
        }
    }
}
