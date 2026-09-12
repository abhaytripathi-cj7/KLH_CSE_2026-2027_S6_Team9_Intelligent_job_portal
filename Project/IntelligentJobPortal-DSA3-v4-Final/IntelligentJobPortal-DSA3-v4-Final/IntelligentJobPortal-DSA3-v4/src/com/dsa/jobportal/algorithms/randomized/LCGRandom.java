package com.dsa.jobportal.algorithms.randomized;

public class LCGRandom {
    private long state;

    public LCGRandom(long seed) {
        state = seed == 0 ? 0x5DEECE66DL : seed;
    }

    public int nextInt(int bound) {
        if (bound <= 0) throw new IllegalArgumentException("bound must be positive");
        state = (state * 6364136223846793005L + 1442695040888963407L);
        long positive = state & Long.MAX_VALUE;
        return (int) (positive % bound);
    }
}
