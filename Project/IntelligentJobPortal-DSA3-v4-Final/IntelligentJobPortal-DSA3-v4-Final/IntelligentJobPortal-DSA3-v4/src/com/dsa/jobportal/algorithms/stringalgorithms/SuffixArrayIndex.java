package com.dsa.jobportal.algorithms.stringalgorithms;

public class SuffixArrayIndex {
    private final String text;
    private final int[] suffixArray;
    private final int[] lcp;

    public SuffixArrayIndex(String text) {
        this.text = text == null ? "" : text.toLowerCase();
        this.suffixArray = buildSuffixArray(this.text);
        this.lcp = buildLcp(this.text, suffixArray);
    }

    public boolean contains(String pattern) {
        if (pattern == null || pattern.isEmpty()) return true;
        String p = pattern.toLowerCase();
        int lo = 0;
        int hi = suffixArray.length - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            int cmp = compareSuffixWithPattern(suffixArray[mid], p);
            if (cmp == 0) return true;
            if (cmp < 0) lo = mid + 1;
            else hi = mid - 1;
        }
        return false;
    }

    public int[] getSuffixArray() { return suffixArray; }
    public int[] getLcp() { return lcp; }

    private int compareSuffixWithPattern(int start, String pattern) {
        int i = 0;
        while (start + i < text.length() && i < pattern.length()) {
            char a = text.charAt(start + i);
            char b = pattern.charAt(i);
            if (a < b) return -1;
            if (a > b) return 1;
            i++;
        }
        if (i == pattern.length()) return 0;
        return -1;
    }

    private int[] buildSuffixArray(String s) {
        int n = s.length();
        int[] sa = new int[n];
        int[] rank = new int[n];
        int[] nextRank = new int[n];
        int[] temp = new int[n];
        for (int i = 0; i < n; i++) {
            sa[i] = i;
            rank[i] = s.charAt(i);
        }
        for (int k = 1; k < n; k <<= 1) {
            mergeSort(sa, temp, 0, n, rank, k);
            if (n == 0) break;
            nextRank[sa[0]] = 0;
            int classes = 1;
            for (int i = 1; i < n; i++) {
                int prev = sa[i - 1];
                int curr = sa[i];
                if (compareRankPair(prev, curr, rank, k) != 0) classes++;
                nextRank[curr] = classes - 1;
            }
            for (int i = 0; i < n; i++) rank[i] = nextRank[i];
            if (classes == n) break;
        }
        return sa;
    }

    private void mergeSort(int[] sa, int[] temp, int left, int right, int[] rank, int k) {
        if (right - left <= 1) return;
        int mid = (left + right) >>> 1;
        mergeSort(sa, temp, left, mid, rank, k);
        mergeSort(sa, temp, mid, right, rank, k);
        int i = left, j = mid, p = left;
        while (i < mid && j < right) {
            if (compareRankPair(sa[i], sa[j], rank, k) <= 0) temp[p++] = sa[i++];
            else temp[p++] = sa[j++];
        }
        while (i < mid) temp[p++] = sa[i++];
        while (j < right) temp[p++] = sa[j++];
        for (i = left; i < right; i++) sa[i] = temp[i];
    }

    private int compareRankPair(int a, int b, int[] rank, int k) {
        if (rank[a] != rank[b]) return rank[a] < rank[b] ? -1 : 1;
        int ar = a + k < rank.length ? rank[a + k] : -1;
        int br = b + k < rank.length ? rank[b + k] : -1;
        return Integer.compare(ar, br);
    }

    private int[] buildLcp(String s, int[] sa) {
        int n = s.length();
        int[] rank = new int[n];
        int[] lcp = new int[n];
        for (int i = 0; i < n; i++) rank[sa[i]] = i;
        int h = 0;
        for (int i = 0; i < n; i++) {
            int r = rank[i];
            if (r == 0) continue;
            int j = sa[r - 1];
            while (i + h < n && j + h < n && s.charAt(i + h) == s.charAt(j + h)) h++;
            lcp[r] = h;
            if (h > 0) h--;
        }
        return lcp;
    }
}
