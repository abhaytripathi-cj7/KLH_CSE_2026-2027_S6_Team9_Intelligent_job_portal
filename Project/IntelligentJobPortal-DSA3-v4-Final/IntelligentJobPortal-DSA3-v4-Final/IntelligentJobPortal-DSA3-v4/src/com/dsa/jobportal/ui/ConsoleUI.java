package com.dsa.jobportal.ui;

import com.dsa.jobportal.algorithms.optimization.KnapsackOptimizer;
import com.dsa.jobportal.algorithms.parallel.ParallelScoreReducer;
import com.dsa.jobportal.algorithms.randomized.ReservoirSampler;
import com.dsa.jobportal.algorithms.stringalgorithms.SuffixArrayIndex;
import com.dsa.jobportal.model.Candidate;
import com.dsa.jobportal.model.Job;
import com.dsa.jobportal.model.MatchResult;
import com.dsa.jobportal.service.AlgorithmSelector;
import com.dsa.jobportal.service.CandidateJobMatcher;
import com.dsa.jobportal.service.JobRecommendationEngine;
import com.dsa.jobportal.service.JobRepository;
import com.dsa.jobportal.service.JobSearchEngine;
import com.dsa.jobportal.structures.DynamicArray;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ConsoleUI {
    private final BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    private final JobRepository repository;
    private final JobSearchEngine search;
    private final JobRecommendationEngine recommendation;
    private final AlgorithmSelector selector = new AlgorithmSelector();

    public ConsoleUI(JobRepository repository) {
        this.repository = repository;
        this.search = new JobSearchEngine(repository.all());
        this.recommendation = new JobRecommendationEngine(repository.all());
    }

    public void start() throws IOException {
        boolean running = true;
        while (running) {
            printMenu();
            String choice = read("Choose an option: ");
            System.out.println();
            switch (choice) {
                case "1" -> listJobs();
                case "2" -> exactSearch();
                case "3" -> fuzzySearch();
                case "4" -> recommendJobs();
                case "5" -> benchmarkSearch();
                case "6" -> suffixIndexDemo();
                case "7" -> flowAssignmentDemo();
                case "8" -> applicationOptimizer();
                case "9" -> reservoirSample();
                case "10" -> algorithmGuide();
                case "11" -> parallelReductionDemo();
                case "0" -> running = false;
                default -> System.out.println("Invalid option. Try again.");
            }
            if (running) {
                System.out.println("\nPress ENTER to return to the menu...");
                in.readLine();
            }
        }
        System.out.println("Goodbye. Intelligent Job Portal closed.");
    }

    private void printMenu() {
        System.out.println("\n============================================================");
        System.out.println("              INTELLIGENT JOB PORTAL - ENGINE CONSOLE");
        System.out.println("============================================================");
        System.out.println("1.  View all jobs");
        System.out.println("2.  Exact job search (KMP / Z / Rabin-Karp)");
        System.out.println("3.  Fuzzy title search (Edit Distance DP)");
        System.out.println("4.  Intelligent job recommendations");
        System.out.println("5.  Compare string-search algorithms");
        System.out.println("6.  Suffix Array + LCP index demo");
        System.out.println("7.  Candidate-to-job assignment (Edmonds-Karp)");
        System.out.println("8.  Application-time optimizer (Knapsack + approximation)");
        System.out.println("9.  Featured jobs (Reservoir Sampling)");
        System.out.println("10. DSA engine guide");
        System.out.println("11. Parallel reduction demo");
        System.out.println("0.  Exit");
        System.out.println("============================================================");
    }

    private void listJobs() {
        DynamicArray<Job> jobs = repository.all();
        System.out.println("Loaded jobs: " + jobs.size());
        System.out.println("--------------------------------------------------------------------------------");
        for (int i = 0; i < jobs.size(); i++) System.out.println(jobs.get(i).shortLine());
    }

    private void exactSearch() throws IOException {
        String query = read("Search text: ");
        System.out.println("1. KMP   2. Z-Algorithm   3. Rabin-Karp");
        String option = read("Algorithm: ");
        JobSearchEngine.Algorithm algorithm = switch (option) {
            case "2" -> JobSearchEngine.Algorithm.Z;
            case "3" -> JobSearchEngine.Algorithm.RABIN_KARP;
            default -> JobSearchEngine.Algorithm.KMP;
        };
        long start = System.nanoTime();
        DynamicArray<Job> matches = search.exactSearch(query, algorithm);
        long elapsed = System.nanoTime() - start;
        System.out.println("\nAlgorithm: " + algorithm);
        System.out.println("Matches  : " + matches.size());
        System.out.printf("Time     : %.3f ms%n", elapsed / 1_000_000.0);
        printJobMatches(matches, 15);
    }

    private void fuzzySearch() throws IOException {
        String query = read("Approximate job title: ");
        int distance = readInt("Maximum edit distance per query word [default 2]: ", 2);
        DynamicArray<Job> matches = search.fuzzyTitleSearch(query, distance);
        System.out.println("\nWagner-Fischer Edit Distance results: " + matches.size());
        printJobMatches(matches, 15);
    }

    private void recommendJobs() throws IOException {
        Candidate candidate = readCandidate(1);
        String desiredTitle = read("Desired job title (optional): ");
        MatchResult[] results = recommendation.recommend(candidate, desiredTitle);
        int limit = Math.min(10, results.length);
        System.out.println("\nTop recommendations (ranked with Randomized QuickSort)");
        System.out.println("--------------------------------------------------------------------------------");
        for (int i = 0; i < limit; i++) printMatch(i + 1, results[i]);
    }

    private void benchmarkSearch() throws IOException {
        String query = read("Pattern to benchmark: ");
        JobSearchEngine.Benchmark b = search.benchmark(query);
        System.out.println("\nAlgorithm comparison across the full job dataset");
        System.out.println("----------------------------------------------------------------------------");
        System.out.printf("%-14s %-10s %-18s %-12s%n", "Algorithm", "Matches", "Core operations", "Time ms");
        System.out.printf("%-14s %-10d %-18d %-12.4f%n", "KMP", b.kmpMatches, b.kmpComparisons, b.kmpNanos / 1_000_000.0);
        System.out.printf("%-14s %-10d %-18d %-12.4f%n", "Z", b.zMatches, b.zComparisons, b.zNanos / 1_000_000.0);
        System.out.printf("%-14s %-10d %-18d %-12.4f%n", "Rabin-Karp", b.rkMatches, b.rkHashChecks, b.rkNanos / 1_000_000.0);
        System.out.println("Note: nanosecond timings vary by JVM warm-up and machine; operation counts are more stable.");
    }

    private void suffixIndexDemo() throws IOException {
        System.out.println("Building suffix array over the combined searchable job corpus...");
        long start = System.nanoTime();
        SuffixArrayIndex index = search.buildSuffixIndex();
        long elapsed = System.nanoTime() - start;
        System.out.println("Suffixes indexed: " + index.getSuffixArray().length);
        System.out.printf("Build time      : %.3f ms%n", elapsed / 1_000_000.0);
        String pattern = read("Substring to query with binary search: ");
        System.out.println("Present in corpus: " + index.contains(pattern));
        int[] lcp = index.getLcp();
        int max = 0;
        for (int value : lcp) if (value > max) max = value;
        System.out.println("Maximum adjacent LCP length in corpus: " + max);
    }

    private void flowAssignmentDemo() throws IOException {
        int count = readInt("Number of candidates [default 3, max 8]: ", 3);
        count = Math.max(1, Math.min(8, count));
        Candidate[] candidates = new Candidate[count];
        for (int i = 0; i < count; i++) {
            System.out.println("\nCandidate " + (i + 1));
            candidates[i] = readCandidate(i + 1);
        }
        double threshold = readDouble("Minimum eligibility score [default 45]: ", 45.0);
        CandidateJobMatcher.AssignmentResult result = new CandidateJobMatcher().assign(candidates, repository.all(), threshold);
        System.out.println("\nMAX-FLOW ASSIGNMENT RESULT");
        System.out.println("Maximum assigned candidates: " + result.maxAssignments + " / " + candidates.length);
        for (int i = 0; i < candidates.length; i++) {
            int jobIndex = result.assignedJobIndex[i];
            if (jobIndex >= 0) {
                Job job = repository.all().get(jobIndex);
                System.out.println(candidates[i].getName() + " -> " + job.getTitle() + " at " + job.getCompany());
            } else {
                System.out.println(candidates[i].getName() + " -> no eligible vacancy at this threshold");
            }
        }
    }

    private void applicationOptimizer() throws IOException {
        Candidate candidate = readCandidate(1);
        String desiredTitle = read("Desired title (optional): ");
        int maxHours = readInt("Available application/preparation hours [default 10]: ", 10);
        MatchResult[] ranked = recommendation.recommend(candidate, desiredTitle);
        int n = Math.min(12, ranked.length);
        MatchResult[] candidates = new MatchResult[n];
        for (int i = 0; i < n; i++) candidates[i] = ranked[i];

        KnapsackOptimizer optimizer = new KnapsackOptimizer();
        KnapsackOptimizer.Plan exact = optimizer.exact(candidates, maxHours);
        KnapsackOptimizer.Plan approx = optimizer.halfApproximation(candidates, maxHours);

        System.out.println("\nExact plan");
        printPlan(candidates, exact);
        System.out.println("\nApproximation plan");
        printPlan(candidates, approx);
        if (exact.totalValue > 0) {
            double ratio = (double) approx.totalValue / exact.totalValue;
            System.out.printf("Observed approximation / exact value ratio: %.3f%n", ratio);
        }
    }

    private void reservoirSample() throws IOException {
        int k = readInt("How many featured jobs? [default 5]: ", 5);
        Job[] sample = new ReservoirSampler().sample(repository.all(), k);
        System.out.println("\nUniform reservoir sample from the job stream:");
        for (int i = 0; i < sample.length; i++) System.out.println((i + 1) + ". " + sample[i].shortLine());
    }

    private void algorithmGuide() throws IOException {
        System.out.println(selector.courseMap());
        System.out.println("\nProblem examples you can ask about: exact search, fuzzy typo, skill matching, assignment, optimization, ranking.");
        String type = read("Enter a problem type for algorithm selection (or leave blank): ");
        if (!type.isBlank()) System.out.println("Selected strategy: " + selector.explain(type));
    }

    private void parallelReductionDemo() throws IOException {
        DynamicArray<Job> jobs = repository.all();
        int[] salaries = new int[jobs.size()];
        long sequential = 0;
        for (int i = 0; i < jobs.size(); i++) {
            salaries[i] = jobs.get(i).getSalaryLpa();
            sequential += salaries[i];
        }
        int threads = readInt("Worker threads [default 4]: ", 4);
        try {
            long start = System.nanoTime();
            long parallel = new ParallelScoreReducer().parallelSum(salaries, threads);
            long elapsed = System.nanoTime() - start;
            System.out.println("Sequential sum check : " + sequential);
            System.out.println("Parallel reduce result: " + parallel);
            System.out.printf("Parallel run time     : %.4f ms%n", elapsed / 1_000_000.0);
            System.out.println("For this tiny dataset thread overhead may dominate; the feature demonstrates work/span intuition.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Parallel demo interrupted.");
        }
    }

    private Candidate readCandidate(int id) throws IOException {
        String name = read("Name: ");
        String skills = read("Skills (comma separated): ").replace(',', '|');
        String location = read("Preferred location: ");
        return new Candidate(id, name.isBlank() ? "Candidate-" + id : name, skills, location);
    }

    private void printPlan(MatchResult[] jobs, KnapsackOptimizer.Plan plan) {
        System.out.println("Method     : " + plan.method);
        System.out.println("Total hours: " + plan.totalHours);
        System.out.println("Total value: " + plan.totalValue);
        boolean any = false;
        for (int i = 0; i < jobs.length; i++) {
            if (plan.selected[i]) {
                any = true;
                System.out.printf("- %-28s | %d hr | match %.1f%%%n",
                    jobs[i].getJob().getTitle(), jobs[i].getJob().getApplyHours(), jobs[i].getScore());
            }
        }
        if (!any) System.out.println("No job fits the time budget.");
    }

    private void printMatch(int rank, MatchResult result) {
        Job j = result.getJob();
        System.out.printf("%2d. %-27s | %-12s | score %5.1f%% | skills %5.1f%% | loc %5.1f%% | title %5.1f%%%n",
            rank, j.getTitle(), j.getCompany(), result.getScore(), result.getSkillScore(), result.getLocationScore(), result.getTitleScore());
    }

    private void printJobMatches(DynamicArray<Job> matches, int limit) {
        int count = Math.min(limit, matches.size());
        for (int i = 0; i < count; i++) System.out.println(matches.get(i).shortLine());
        if (matches.size() > count) System.out.println("... and " + (matches.size() - count) + " more");
    }

    private String read(String prompt) throws IOException {
        System.out.print(prompt);
        String line = in.readLine();
        return line == null ? "" : line.trim();
    }

    private int readInt(String prompt, int defaultValue) throws IOException {
        String s = read(prompt);
        if (s.isBlank()) return defaultValue;
        try { return Integer.parseInt(s); }
        catch (NumberFormatException e) { return defaultValue; }
    }

    private double readDouble(String prompt, double defaultValue) throws IOException {
        String s = read(prompt);
        if (s.isBlank()) return defaultValue;
        try { return Double.parseDouble(s); }
        catch (NumberFormatException e) { return defaultValue; }
    }
}
