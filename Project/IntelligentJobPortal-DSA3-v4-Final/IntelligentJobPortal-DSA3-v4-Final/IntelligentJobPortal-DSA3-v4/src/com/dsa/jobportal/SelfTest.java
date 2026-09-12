package com.dsa.jobportal;

import com.dsa.jobportal.algorithms.dp.EditDistance;
import com.dsa.jobportal.algorithms.flow.EdmondsKarp;
import com.dsa.jobportal.algorithms.dp.SkillUpgradePlanner;
import com.dsa.jobportal.algorithms.optimization.KnapsackOptimizer;
import com.dsa.jobportal.algorithms.parallel.ParallelScoreReducer;
import com.dsa.jobportal.algorithms.randomized.RandomizedQuickSort;
import com.dsa.jobportal.algorithms.randomized.ReservoirSampler;
import com.dsa.jobportal.algorithms.stringalgorithms.AhoCorasick;
import com.dsa.jobportal.algorithms.stringalgorithms.KMP;
import com.dsa.jobportal.algorithms.stringalgorithms.RabinKarp;
import com.dsa.jobportal.algorithms.stringalgorithms.SuffixArrayIndex;
import com.dsa.jobportal.algorithms.stringalgorithms.ZAlgorithm;
import com.dsa.jobportal.model.Candidate;
import com.dsa.jobportal.model.Job;
import com.dsa.jobportal.model.MatchResult;
import com.dsa.jobportal.service.CandidateJobMatcher;
import com.dsa.jobportal.service.JobRecommendationEngine;
import com.dsa.jobportal.service.JobRepository;
import com.dsa.jobportal.service.JobSearchEngine;
import com.dsa.jobportal.structures.CustomHashTable;
import com.dsa.jobportal.structures.DynamicArray;
import com.dsa.jobportal.structures.IntQueue;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class SelfTest {
    private static int passed;
    private static int total;

    public static void main(String[] args) throws Exception {
        algorithmUnitTests();
        structureTests();
        projectIntegrationTests();

        System.out.println("------------------------------------------------------------");
        System.out.println("Self-test passed: " + passed + "/" + total);
        if (passed != total) System.exit(1);
    }

    private static void algorithmUnitTests() throws Exception {
        KMP kmp = new KMP();
        check(kmp.contains("java backend developer", "backend"), "KMP finds substring");
        check(!kmp.contains("java backend developer", "python"), "KMP rejects absent substring");

        ZAlgorithm z = new ZAlgorithm();
        check(z.contains("machine learning engineer", "learning"), "Z algorithm finds substring");
        check(z.contains("price $100", "$100"), "Z separator is safe for dollar input");

        RabinKarp rk = new RabinKarp();
        check(rk.contains("hyderabad software role", "software"), "Rabin-Karp finds substring");
        check(!rk.contains("hyderabad software role", "bengaluru"), "Rabin-Karp rejects absent substring");

        EditDistance edit = new EditDistance();
        check(edit.distance("pythn", "python") == 1, "Wagner-Fischer edit distance");
        check(edit.similarityPercent("java", "java") == 100.0, "Edit similarity exact match");

        AhoCorasick aho = new AhoCorasick(new String[]{"java", "sql", "aws"});
        check(aho.countDistinctMatches("java spring sql") == 2, "Aho-Corasick multi-pattern matching");

        SuffixArrayIndex suffix = new SuffixArrayIndex("banana backend");
        check(suffix.contains("backend"), "Suffix array binary search");
        check(!suffix.contains("frontend"), "Suffix array rejects absent pattern");

        int[][] capacity = {
            {0, 1, 1, 0},
            {0, 0, 1, 1},
            {0, 0, 0, 1},
            {0, 0, 0, 0}
        };
        check(new EdmondsKarp().compute(capacity, 0, 3).getMaxFlow() == 2, "Edmonds-Karp maximum flow");

        int[] values = {2, 4, 6, 8, 10};
        check(new ParallelScoreReducer().parallelSum(values, 3) == 30, "Parallel reduction equals sequential sum");
    }

    private static void structureTests() {
        DynamicArray<String> array = new DynamicArray<>();
        for (int i = 0; i < 40; i++) array.add("v" + i);
        check(array.size() == 40 && "v39".equals(array.get(39)), "Custom DynamicArray grows correctly");

        IntQueue queue = new IntQueue(2);
        for (int i = 0; i < 12; i++) queue.offer(i);
        boolean fifo = true;
        for (int i = 0; i < 12; i++) if (queue.poll() != i) fifo = false;
        check(fifo && queue.isEmpty(), "Custom IntQueue preserves FIFO through growth");

        CustomHashTable<Integer> table = new CustomHashTable<>(17);
        table.put("Java", 1); table.put("SQL", 2); table.put("java", 3);
        check(table.size() == 2 && table.get("JAVA") == 3, "CustomHashTable normalizes and updates keys");
    }

    private static void projectIntegrationTests() throws Exception {
        JobRepository repo = new JobRepository();
        repo.load(locateDataFile());
        check(repo.all().size() >= 30, "CSV job repository loads sample corpus");

        JobSearchEngine search = new JobSearchEngine(repo.all());
        int k = search.exactSearch("Java", JobSearchEngine.Algorithm.KMP).size();
        int z = search.exactSearch("Java", JobSearchEngine.Algorithm.Z).size();
        int r = search.exactSearch("Java", JobSearchEngine.Algorithm.RABIN_KARP).size();
        check(k == z && z == r && k > 0, "KMP/Z/Rabin-Karp agree on project corpus");

        check(search.fuzzyTitleSearch("Pythn Developer", 2).size() > 0, "Fuzzy title search rescues typo");

        Candidate javaCandidate = new Candidate(1, "Test", "Java", "");
        JobRecommendationEngine engine = new JobRecommendationEngine(repo.all());
        Job javascriptJob = repo.findById(14);
        check(engine.score(javaCandidate, "", javascriptJob).getSkillScore() == 0.0,
              "Skill boundaries prevent Java from matching JavaScript");

        Job amazonJava = repo.findById(1);
        Candidate exactSkills = new Candidate(9, "Exact", "Java|Spring|SQL|AWS", "Hyderabad");
        Candidate extraSkills = new Candidate(10, "Extra", "Java|Spring|SQL|AWS|Docker|Git", "Hyderabad");
        double exactCoverage = engine.score(exactSkills, "Java Backend Developer", amazonJava).getSkillScore();
        double extraCoverage = engine.score(extraSkills, "Java Backend Developer", amazonJava).getSkillScore();
        check(exactCoverage == 100.0 && extraCoverage == 100.0,
              "Extra candidate skills never reduce required-skill coverage");

        Candidate c = new Candidate(1, "Pranav", "Java|Spring|SQL|AWS", "Hyderabad");
        MatchResult[] ranked = engine.recommend(c, "Java Backend Developer");
        check(ranked.length == repo.all().size() && ranked[0].getScore() >= ranked[ranked.length - 1].getScore(),
              "Recommendation engine returns descending ranked results");

        SkillUpgradePlanner.Plan skillPlan = new SkillUpgradePlanner().plan(c, ranked, 8, 2);
        check(skillPlan.selectedSkills.length <= 2 && skillPlan.projectedUtility >= skillPlan.baselineUtility && skillPlan.evaluatedStates > 0,
              "Bitmask subset DP produces a bounded skill-upgrade plan");

        MatchResult[] copy = new MatchResult[Math.min(10, ranked.length)];
        for (int i = 0; i < copy.length; i++) copy[i] = ranked[copy.length - 1 - i];
        new RandomizedQuickSort().sortDescending(copy);
        boolean sorted = true;
        for (int i = 1; i < copy.length; i++) if (copy[i - 1].getScore() < copy[i].getScore()) sorted = false;
        check(sorted, "Randomized QuickSort orders match scores descending");

        Job[] sample = new ReservoirSampler().sample(repo.all(), 5);
        boolean unique = sample.length == 5;
        for (int i = 0; i < sample.length; i++) for (int j = i + 1; j < sample.length; j++) if (sample[i] == sample[j]) unique = false;
        check(unique, "Reservoir sampling returns requested unique sample");

        MatchResult[] top = new MatchResult[Math.min(12, ranked.length)];
        for (int i = 0; i < top.length; i++) top[i] = ranked[i];
        KnapsackOptimizer ko = new KnapsackOptimizer();
        KnapsackOptimizer.Plan exact = ko.exact(top, 10);
        KnapsackOptimizer.Plan approx = ko.halfApproximation(top, 10);
        boolean approximationValid = exact.totalValue == 0 || (approx.totalValue <= exact.totalValue && approx.totalValue * 2 >= exact.totalValue);
        check(approximationValid, "Knapsack approximation stays within expected 1/2 bound on demo data");

        Candidate[] candidates = {
            new Candidate(1, "A", "Java|Spring|SQL", "Hyderabad"),
            new Candidate(2, "B", "Python|Machine Learning|SQL", "Bengaluru"),
            new Candidate(3, "C", "SQL|Power BI|Python", "Hyderabad")
        };
        CandidateJobMatcher.AssignmentResult assignment = new CandidateJobMatcher().assign(candidates, repo.all(), 35.0);
        check(assignment.maxAssignments > 0 && assignment.maxAssignments <= candidates.length,
              "Candidate-to-job max-flow assignment is feasible");

        int beforePost = repo.all().size();
        Job posted = new Job(repo.nextId(), "QuantumPortal Backend Engineer", "Nova Labs", "Hyderabad",
                "Java|Spring|SQL|Docker", "Runtime recruiter-created opening", 16, 2, 3, 2, "Remote");
        repo.add(posted);
        check(repo.all().size() == beforePost + 1 && "Remote".equals(repo.findById(posted.getId()).getWorkMode()),
              "Recruiter-created jobs are added at runtime with work mode preserved");
        check(search.exactSearch("QuantumPortal", JobSearchEngine.Algorithm.KMP).size() == 1,
              "Live job publishing immediately updates the search engine corpus");

        check(new File("web/index.html").isFile() && new File("web/assets/app.js").isFile() && new File("web/assets/styles.css").isFile(),
              "Web V4 frontend assets are present");
        String html = Files.readString(Path.of("web/index.html"));
        String css = Files.readString(Path.of("web/assets/styles.css"));
        check(html.contains("id=\"postJobModal\" hidden") && css.contains("[hidden]{display:none!important}"),
              "Post-job modal is hidden on initial page load");
        check(!html.contains("CO1") && !html.contains("CO2") && !html.contains("25CS2103E") && !html.contains("DSA Observatory"),
              "Academic course labels are absent from the user-facing portal");
    }

    private static String locateDataFile() {
        String[] candidates = {"data/jobs.csv", "../data/jobs.csv", "../../data/jobs.csv"};
        for (String p : candidates) if (new File(p).isFile()) return p;
        return "data/jobs.csv";
    }

    private static void check(boolean condition, String name) {
        total++;
        System.out.println((condition ? "PASS" : "FAIL") + " - " + name);
        if (condition) passed++;
    }
}
