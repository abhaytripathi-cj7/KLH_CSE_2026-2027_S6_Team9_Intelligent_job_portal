package com.dsa.jobportal.algorithms.stringalgorithms;

public class KMP {
    private long comparisons;

    public boolean contains(String text, String pattern) {
        comparisons = 0;
        if (pattern == null || pattern.isEmpty()) return true;
        if (text == null || pattern.length() > text.length()) return false;

        String t = text.toLowerCase();
        String p = pattern.toLowerCase();
        int[] lps = buildLps(p);
        int i = 0;
        int j = 0;

        while (i < t.length()) {
            comparisons++;
            if (t.charAt(i) == p.charAt(j)) {
                i++;
                j++;
                if (j == p.length()) return true;
            } else if (j > 0) {
                j = lps[j - 1];
            } else {
                i++;
            }
        }
        return false;
    }

    private int[] buildLps(String pattern) {
        int[] lps = new int[pattern.length()];
        int len = 0;
        int i = 1;
        while (i < pattern.length()) {
            comparisons++;
            if (pattern.charAt(i) == pattern.charAt(len)) {
                lps[i++] = ++len;
            } else if (len > 0) {
                len = lps[len - 1];
            } else {
                lps[i++] = 0;
            }
        }
        return lps;
    }

    public long getComparisons() {
        return comparisons;
    }
}
