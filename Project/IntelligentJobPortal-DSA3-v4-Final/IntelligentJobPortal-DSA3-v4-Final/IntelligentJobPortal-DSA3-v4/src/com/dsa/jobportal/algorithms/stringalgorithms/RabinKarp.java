package com.dsa.jobportal.algorithms.stringalgorithms;

public class RabinKarp {
    private static final long BASE = 257L;
    private static final long MOD1 = 1_000_000_007L;
    private static final long MOD2 = 1_000_000_009L;
    private long hashChecks;

    public boolean contains(String text, String pattern) {
        hashChecks = 0;
        if (pattern == null || pattern.isEmpty()) return true;
        if (text == null || pattern.length() > text.length()) return false;

        String t = text.toLowerCase();
        String p = pattern.toLowerCase();
        int m = p.length();

        long ph1 = 0, ph2 = 0, th1 = 0, th2 = 0;
        long pow1 = 1, pow2 = 1;
        for (int i = 0; i < m; i++) {
            ph1 = (ph1 * BASE + p.charAt(i)) % MOD1;
            ph2 = (ph2 * BASE + p.charAt(i)) % MOD2;
            th1 = (th1 * BASE + t.charAt(i)) % MOD1;
            th2 = (th2 * BASE + t.charAt(i)) % MOD2;
            if (i < m - 1) {
                pow1 = (pow1 * BASE) % MOD1;
                pow2 = (pow2 * BASE) % MOD2;
            }
        }

        for (int i = 0; i <= t.length() - m; i++) {
            hashChecks++;
            if (ph1 == th1 && ph2 == th2 && regionEquals(t, i, p)) return true;
            if (i < t.length() - m) {
                th1 = roll(th1, t.charAt(i), t.charAt(i + m), pow1, MOD1);
                th2 = roll(th2, t.charAt(i), t.charAt(i + m), pow2, MOD2);
            }
        }
        return false;
    }

    private long roll(long hash, char out, char in, long pow, long mod) {
        hash = (hash - (out * pow) % mod + mod) % mod;
        return (hash * BASE + in) % mod;
    }

    private boolean regionEquals(String text, int start, String pattern) {
        for (int i = 0; i < pattern.length(); i++) {
            if (text.charAt(start + i) != pattern.charAt(i)) return false;
        }
        return true;
    }

    public long getHashChecks() {
        return hashChecks;
    }
}
