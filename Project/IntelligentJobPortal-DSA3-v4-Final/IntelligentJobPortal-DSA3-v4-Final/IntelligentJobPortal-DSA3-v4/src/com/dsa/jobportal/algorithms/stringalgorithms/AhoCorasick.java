package com.dsa.jobportal.algorithms.stringalgorithms;

import com.dsa.jobportal.structures.IntQueue;

public class AhoCorasick {
    private static final int ALPHABET = 128;
    private int[][] next;
    private int[] fail;
    private long[] output;
    private int nodes;
    private int patternCount;

    public AhoCorasick(String[] patterns) {
        patternCount = Math.min(patterns.length, 63);
        int maxNodes = 1;
        for (int i = 0; i < patternCount; i++) maxNodes += patterns[i].length();
        next = new int[Math.max(2, maxNodes)][ALPHABET];
        fail = new int[Math.max(2, maxNodes)];
        output = new long[Math.max(2, maxNodes)];
        nodes = 1;
        buildTrie(patterns);
        buildFailureLinks();
    }

    private void buildTrie(String[] patterns) {
        for (int p = 0; p < patternCount; p++) {
            String pattern = normalize(patterns[p]);
            int state = 0;
            for (int i = 0; i < pattern.length(); i++) {
                int c = code(pattern.charAt(i));
                if (next[state][c] == 0) next[state][c] = nodes++;
                state = next[state][c];
            }
            output[state] |= (1L << p);
        }
    }

    private void buildFailureLinks() {
        IntQueue queue = new IntQueue(nodes + 2);
        for (int c = 0; c < ALPHABET; c++) {
            int child = next[0][c];
            if (child != 0) queue.offer(child);
        }

        while (!queue.isEmpty()) {
            int state = queue.poll();
            for (int c = 0; c < ALPHABET; c++) {
                int child = next[state][c];
                if (child == 0) continue;
                int f = fail[state];
                while (f != 0 && next[f][c] == 0) f = fail[f];
                if (next[f][c] != 0 && next[f][c] != child) f = next[f][c];
                fail[child] = f;
                output[child] |= output[f];
                queue.offer(child);
            }
        }
    }

    public long matchedPatternMask(String text) {
        String t = normalize(text);
        int state = 0;
        long mask = 0L;
        for (int i = 0; i < t.length(); i++) {
            int c = code(t.charAt(i));
            while (state != 0 && next[state][c] == 0) state = fail[state];
            if (next[state][c] != 0) state = next[state][c];
            mask |= output[state];
        }
        return mask;
    }

    public int countDistinctMatches(String text) {
        long mask = matchedPatternMask(text);
        int count = 0;
        while (mask != 0) {
            count += (int) (mask & 1L);
            mask >>>= 1;
        }
        return count;
    }

    private String normalize(String s) {
        return s == null ? "" : s.toLowerCase();
    }

    private int code(char c) {
        return c < ALPHABET ? c : '?';
    }
}
