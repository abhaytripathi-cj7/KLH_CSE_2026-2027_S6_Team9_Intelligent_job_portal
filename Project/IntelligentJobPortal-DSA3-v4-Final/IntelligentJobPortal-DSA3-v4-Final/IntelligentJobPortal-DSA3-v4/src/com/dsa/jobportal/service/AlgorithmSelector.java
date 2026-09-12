package com.dsa.jobportal.service;

public class AlgorithmSelector {
    public String explain(String queryType) {
        String q = queryType == null ? "" : queryType.toLowerCase();
        if (q.contains("exact") || q.contains("substring")) {
            return "KMP / Z / Rabin-Karp -> linear-time style string matching for job text.";
        }
        if (q.contains("multi") || q.contains("skill")) {
            return "Aho-Corasick -> one pass for multiple candidate-skill patterns.";
        }
        if (q.contains("fuzzy") || q.contains("typo")) {
            return "Wagner-Fischer Edit Distance -> dynamic programming for typo-tolerant search.";
        }
        if (q.contains("assignment") || q.contains("vacancy") || q.contains("flow")) {
            return "Edmonds-Karp Max Flow -> candidate-to-vacancy assignment with capacities.";
        }
        if (q.contains("opt") || q.contains("time") || q.contains("application")) {
            return "Knapsack formulation -> exact DP comparison and 1/2 approximation.";
        }
        if (q.contains("rank") || q.contains("sort")) {
            return "Randomized QuickSort -> expected O(n log n) ranking of match scores.";
        }
        return "Classify the problem first: string search, DP, network flow, NP-hard optimization, or randomized/parallel.";
    }

    public String courseMap() {
        return "String processing: KMP, Z, Rabin-Karp, Aho-Corasick, Suffix Array + LCP\n" +
               "Dynamic programming: Wagner-Fischer edit distance and subset-state planning\n" +
               "Network optimization: Edmonds-Karp max-flow for candidate-job assignment\n" +
               "Hard-problem strategy: Knapsack-style optimization + approximation\n" +
               "Randomized / parallel: Randomized QuickSort, reservoir sampling, parallel reduction";
    }
}
