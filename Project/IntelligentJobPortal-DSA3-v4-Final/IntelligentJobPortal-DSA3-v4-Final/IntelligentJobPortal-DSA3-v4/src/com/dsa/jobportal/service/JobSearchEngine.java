package com.dsa.jobportal.service;

import com.dsa.jobportal.algorithms.dp.EditDistance;
import com.dsa.jobportal.algorithms.stringalgorithms.KMP;
import com.dsa.jobportal.algorithms.stringalgorithms.RabinKarp;
import com.dsa.jobportal.algorithms.stringalgorithms.SuffixArrayIndex;
import com.dsa.jobportal.algorithms.stringalgorithms.ZAlgorithm;
import com.dsa.jobportal.model.Job;
import com.dsa.jobportal.structures.DynamicArray;

public class JobSearchEngine {
    public enum Algorithm { KMP, Z, RABIN_KARP }

    private final DynamicArray<Job> jobs;
    private final KMP kmp = new KMP();
    private final ZAlgorithm z = new ZAlgorithm();
    private final RabinKarp rk = new RabinKarp();
    private final EditDistance editDistance = new EditDistance();

    public JobSearchEngine(DynamicArray<Job> jobs) {
        this.jobs = jobs;
    }

    public DynamicArray<Job> exactSearch(String query, Algorithm algorithm) {
        DynamicArray<Job> result = new DynamicArray<>();
        for (int i = 0; i < jobs.size(); i++) {
            Job job = jobs.get(i);
            boolean found = switch (algorithm) {
                case KMP -> kmp.contains(job.searchableText(), query);
                case Z -> z.contains(job.searchableText(), query);
                case RABIN_KARP -> rk.contains(job.searchableText(), query);
            };
            if (found) result.add(job);
        }
        return result;
    }

    public DynamicArray<Job> fuzzyTitleSearch(String query, int maxDistance) {
        DynamicArray<Job> result = new DynamicArray<>();
        for (int i = 0; i < jobs.size(); i++) {
            Job job = jobs.get(i);
            String[] words = job.getTitle().split(" ");
            String[] queryWords = query.split(" ");
            int score = 0;
            for (String qw : queryWords) {
                int best = Integer.MAX_VALUE;
                for (String jw : words) best = Math.min(best, editDistance.distance(qw, jw));
                score += best;
            }
            if (score <= maxDistance * Math.max(1, queryWords.length)) result.add(job);
        }
        return result;
    }

    public Benchmark benchmark(String query) {
        long start;
        int countK = 0, countZ = 0, countR = 0;

        start = System.nanoTime();
        long kComparisons = 0;
        for (int i = 0; i < jobs.size(); i++) {
            if (kmp.contains(jobs.get(i).searchableText(), query)) countK++;
            kComparisons += kmp.getComparisons();
        }
        long kNanos = System.nanoTime() - start;

        start = System.nanoTime();
        long zComparisons = 0;
        for (int i = 0; i < jobs.size(); i++) {
            if (z.contains(jobs.get(i).searchableText(), query)) countZ++;
            zComparisons += z.getComparisons();
        }
        long zNanos = System.nanoTime() - start;

        start = System.nanoTime();
        long hashChecks = 0;
        for (int i = 0; i < jobs.size(); i++) {
            if (rk.contains(jobs.get(i).searchableText(), query)) countR++;
            hashChecks += rk.getHashChecks();
        }
        long rNanos = System.nanoTime() - start;

        return new Benchmark(countK, countZ, countR, kComparisons, zComparisons, hashChecks, kNanos, zNanos, rNanos);
    }

    public SuffixArrayIndex buildSuffixIndex() {
        StringBuilder corpus = new StringBuilder();
        for (int i = 0; i < jobs.size(); i++) {
            if (i > 0) corpus.append("\n");
            corpus.append(jobs.get(i).searchableText());
        }
        return new SuffixArrayIndex(corpus.toString());
    }

    public static class Benchmark {
        public final int kmpMatches, zMatches, rkMatches;
        public final long kmpComparisons, zComparisons, rkHashChecks;
        public final long kmpNanos, zNanos, rkNanos;

        public Benchmark(int kmpMatches, int zMatches, int rkMatches,
                         long kmpComparisons, long zComparisons, long rkHashChecks,
                         long kmpNanos, long zNanos, long rkNanos) {
            this.kmpMatches = kmpMatches; this.zMatches = zMatches; this.rkMatches = rkMatches;
            this.kmpComparisons = kmpComparisons; this.zComparisons = zComparisons; this.rkHashChecks = rkHashChecks;
            this.kmpNanos = kmpNanos; this.zNanos = zNanos; this.rkNanos = rkNanos;
        }
    }
}
