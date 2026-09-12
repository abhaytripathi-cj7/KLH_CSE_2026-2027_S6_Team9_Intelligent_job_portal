package com.dsa.jobportal.web;

import com.dsa.jobportal.algorithms.dp.SkillUpgradePlanner;
import com.dsa.jobportal.algorithms.optimization.KnapsackOptimizer;
import com.dsa.jobportal.algorithms.parallel.ParallelScoreReducer;
import com.dsa.jobportal.algorithms.randomized.ReservoirSampler;
import com.dsa.jobportal.algorithms.stringalgorithms.SuffixArrayIndex;
import com.dsa.jobportal.model.Candidate;
import com.dsa.jobportal.model.Job;
import com.dsa.jobportal.model.MatchResult;
import com.dsa.jobportal.service.CandidateJobMatcher;
import com.dsa.jobportal.service.JobRecommendationEngine;
import com.dsa.jobportal.service.JobRepository;
import com.dsa.jobportal.service.JobSearchEngine;
import com.dsa.jobportal.structures.DynamicArray;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class PortalHttpServer {
    private final HttpServer server;
    private final JobRepository repository;
    private final JobSearchEngine searchEngine;
    private final JobRecommendationEngine recommendationEngine;
    private final File webRoot;

    public PortalHttpServer(int port, JobRepository repository, File webRoot) throws IOException {
        this.repository = repository;
        this.searchEngine = new JobSearchEngine(repository.all());
        this.recommendationEngine = new JobRecommendationEngine(repository.all());
        this.webRoot = webRoot.getCanonicalFile();
        this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        registerRoutes();
    }

    public void start() { server.start(); }
    public void stop(int delaySeconds) { server.stop(delaySeconds); }

    private void registerRoutes() {
        server.createContext("/api/health", api(this::health));
        server.createContext("/api/insights", api(this::insights));
        server.createContext("/api/jobs", api(this::jobs));
        server.createContext("/api/search", api(this::exactSearch));
        server.createContext("/api/fuzzy", api(this::fuzzySearch));
        server.createContext("/api/recommend", api(this::recommend));
        server.createContext("/api/skill-plan", api(this::skillPlan));
        server.createContext("/api/benchmark", api(this::benchmark));
        server.createContext("/api/suffix", api(this::suffix));
        server.createContext("/api/featured", api(this::featured));
        server.createContext("/api/allocate", api(this::allocate));
        server.createContext("/api/optimize", api(this::optimize));
        server.createContext("/api/parallel", api(this::parallel));
        server.createContext("/", new StaticHandler());
    }

    private HttpHandler api(ApiAction action) {
        return exchange -> {
            try {
                if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.getResponseHeaders().set("Allow", "GET, POST, OPTIONS");
                    send(exchange, 204, "application/json; charset=utf-8", new byte[0]);
                    return;
                }
                String json = action.handle(exchange);
                sendJson(exchange, 200, json);
            } catch (BadRequest e) {
                sendJson(exchange, 400, "{\"ok\":false,\"error\":" + Json.q(e.getMessage()) + "}");
            } catch (Exception e) {
                e.printStackTrace();
                sendJson(exchange, 500, "{\"ok\":false,\"error\":\"Internal server error\"}");
            }
        };
    }

    private String health(HttpExchange ex) {
        return "{\"ok\":true,\"app\":\"NovaHire Intelligent Job Portal\",\"version\":\"4.0\",\"jobs\":"
                + repository.all().size() + ",\"engine\":\"online\"}";
    }

    private String insights(HttpExchange ex) {
        DynamicArray<Job> jobs = repository.all();
        int salarySum = 0;
        int vacancySum = 0;
        int maxSalary = 0;
        String topCompany = "";
        int topCompanyJobs = 0;

        for (int i = 0; i < jobs.size(); i++) {
            Job j = jobs.get(i);
            salarySum += j.getSalaryLpa();
            vacancySum += j.getVacancies();
            if (j.getSalaryLpa() > maxSalary) maxSalary = j.getSalaryLpa();
            int count = 0;
            for (int k = 0; k < jobs.size(); k++) if (jobs.get(k).getCompany().equalsIgnoreCase(j.getCompany())) count++;
            if (count > topCompanyJobs) { topCompanyJobs = count; topCompany = j.getCompany(); }
        }

        int locations = uniqueCount(jobs, true);
        int companies = uniqueCount(jobs, false);
        double avgSalary = jobs.isEmpty() ? 0 : (double) salarySum / jobs.size();

        return "{\"ok\":true,\"jobs\":" + jobs.size()
                + ",\"companies\":" + companies
                + ",\"locations\":" + locations
                + ",\"vacancies\":" + vacancySum
                + ",\"avgSalary\":" + Json.num(avgSalary)
                + ",\"maxSalary\":" + maxSalary
                + ",\"topCompany\":" + Json.q(topCompany)
                + ",\"topCompanyJobs\":" + topCompanyJobs + "}";
    }

    private int uniqueCount(DynamicArray<Job> jobs, boolean location) {
        String[] seen = new String[jobs.size()];
        int count = 0;
        for (int i = 0; i < jobs.size(); i++) {
            String value = location ? jobs.get(i).getLocation() : jobs.get(i).getCompany();
            boolean exists = false;
            for (int j = 0; j < count; j++) if (seen[j].equalsIgnoreCase(value)) { exists = true; break; }
            if (!exists) seen[count++] = value;
        }
        return count;
    }

    private String jobs(HttpExchange ex) throws Exception {
        if ("POST".equalsIgnoreCase(ex.getRequestMethod())) {
            Params p = Params.fromExchange(ex);
            String title = p.required("title");
            String company = p.required("company");
            String location = p.required("location");
            String skills = normalizeSkills(p.required("skills"));
            String description = p.required("description");
            int salary = p.intValue("salary", 10, 1, 100);
            int experience = p.intValue("experience", 0, 0, 30);
            int vacancies = p.intValue("vacancies", 1, 1, 100);
            int applyHours = p.intValue("applyHours", 2, 1, 20);
            String workMode = p.get("workMode", "Hybrid").trim();
            if (!(workMode.equalsIgnoreCase("Hybrid") || workMode.equalsIgnoreCase("Remote") || workMode.equalsIgnoreCase("On-site"))) {
                throw new BadRequest("Work mode must be Hybrid, Remote, or On-site.");
            }
            if (skills.isBlank()) throw new BadRequest("Enter at least one required skill.");
            Job created = new Job(repository.nextId(), title, company, location, skills, description,
                    salary, experience, vacancies, applyHours, workMode);
            repository.add(created);
            return "{\"ok\":true,\"created\":true,\"job\":" + jobJson(created) + "}";
        }

        if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) throw new BadRequest("Unsupported method for jobs endpoint.");
        Params p = Params.fromQuery(ex);
        int limit = p.intValue("limit", 100, 1, 500);
        DynamicArray<Job> jobs = repository.all();
        StringBuilder b = new StringBuilder("{\"ok\":true,\"jobs\":[");
        int n = Math.min(limit, jobs.size());
        for (int i = 0; i < n; i++) {
            if (i > 0) b.append(',');
            b.append(jobJson(jobs.get(i)));
        }
        return b.append("]}").toString();
    }

    private String exactSearch(HttpExchange ex) throws BadRequest {
        Params p = Params.fromQuery(ex);
        String q = p.required("q");
        String algorithmRaw = p.get("algo", "kmp").toLowerCase();
        JobSearchEngine.Algorithm algorithm;
        if (algorithmRaw.equals("z")) algorithm = JobSearchEngine.Algorithm.Z;
        else if (algorithmRaw.equals("rabin-karp") || algorithmRaw.equals("rk")) algorithm = JobSearchEngine.Algorithm.RABIN_KARP;
        else algorithm = JobSearchEngine.Algorithm.KMP;

        long start = System.nanoTime();
        DynamicArray<Job> matches = searchEngine.exactSearch(q, algorithm);
        long nanos = System.nanoTime() - start;

        StringBuilder b = new StringBuilder("{\"ok\":true,\"algorithm\":").append(Json.q(algorithm.toString()))
                .append(",\"query\":").append(Json.q(q))
                .append(",\"complexity\":").append(Json.q(complexityFor(algorithm)))
                .append(",\"timeMs\":").append(Json.num(nanos / 1_000_000.0))
                .append(",\"count\":").append(matches.size()).append(",\"jobs\":[");
        for (int i = 0; i < matches.size(); i++) {
            if (i > 0) b.append(',');
            b.append(jobJson(matches.get(i)));
        }
        return b.append("]}").toString();
    }

    private String fuzzySearch(HttpExchange ex) throws BadRequest {
        Params p = Params.fromQuery(ex);
        String q = p.required("q");
        int distance = p.intValue("distance", 2, 0, 10);
        long start = System.nanoTime();
        DynamicArray<Job> matches = searchEngine.fuzzyTitleSearch(q, distance);
        long nanos = System.nanoTime() - start;

        StringBuilder b = new StringBuilder("{\"ok\":true,\"algorithm\":\"Wagner-Fischer Edit Distance\",\"complexity\":\"O(n*m) per compared word pair\",\"timeMs\":")
                .append(Json.num(nanos / 1_000_000.0)).append(",\"count\":").append(matches.size()).append(",\"jobs\":[");
        for (int i = 0; i < matches.size(); i++) {
            if (i > 0) b.append(',');
            b.append(jobJson(matches.get(i)));
        }
        return b.append("]}").toString();
    }

    private String recommend(HttpExchange ex) throws BadRequest {
        Params p = Params.fromQuery(ex);
        String name = p.get("name", "Candidate");
        String skills = normalizeSkills(p.get("skills", ""));
        String location = p.get("location", "");
        String title = p.get("title", "");
        int limit = p.intValue("limit", 8, 1, 30);
        if (skills.isBlank() && location.isBlank() && title.isBlank()) throw new BadRequest("Enter at least skills, location, or desired title.");

        Candidate candidate = new Candidate(1, name, skills, location);
        long start = System.nanoTime();
        MatchResult[] ranked = recommendationEngine.recommend(candidate, title);
        long nanos = System.nanoTime() - start;
        int n = Math.min(limit, ranked.length);

        StringBuilder b = new StringBuilder("{\"ok\":true,\"candidate\":").append(candidateJson(candidate))
                .append(",\"algorithmPath\":[\"Aho-Corasick skill matching\",\"Edit Distance location/title similarity\",\"Weighted Match DNA\",\"Randomized QuickSort ranking\"]")
                .append(",\"timeMs\":").append(Json.num(nanos / 1_000_000.0))
                .append(",\"results\":[");
        for (int i = 0; i < n; i++) {
            if (i > 0) b.append(',');
            b.append(matchJson(ranked[i], candidate));
        }
        return b.append("]}").toString();
    }

    private String skillPlan(HttpExchange ex) throws BadRequest {
        Params p = Params.fromQuery(ex);
        String name = p.get("name", "Candidate");
        String skills = normalizeSkills(p.get("skills", ""));
        String location = p.get("location", "");
        String title = p.get("title", "");
        int budget = p.intValue("budget", 2, 1, 4);
        if (skills.isBlank()) throw new BadRequest("Enter candidate skills before planning an upgrade.");

        Candidate candidate = new Candidate(1, name, skills, location);
        MatchResult[] ranked = recommendationEngine.recommend(candidate, title);
        SkillUpgradePlanner.Plan plan = new SkillUpgradePlanner().plan(candidate, ranked, Math.min(8, ranked.length), budget);

        String upgradedSkills = skills;
        for (int i = 0; i < plan.selectedSkills.length; i++) {
            if (!upgradedSkills.isBlank()) upgradedSkills += "|";
            upgradedSkills += plan.selectedSkills[i];
        }
        Candidate upgraded = new Candidate(1, name, upgradedSkills, location);
        MatchResult[] reranked = recommendationEngine.recommend(upgraded, title);
        double beforeTop = ranked.length == 0 ? 0.0 : ranked[0].getScore();
        double afterTop = reranked.length == 0 ? 0.0 : reranked[0].getScore();

        return "{\"ok\":true,\"algorithm\":\"Bitmask / Subset-State DP\",\"budget\":" + budget
                + ",\"selectedSkills\":" + Json.stringArray(plan.selectedSkills)
                + ",\"baselineUtility\":" + Json.num(plan.baselineUtility)
                + ",\"projectedUtility\":" + Json.num(plan.projectedUtility)
                + ",\"evaluatedStates\":" + plan.evaluatedStates
                + ",\"skillUniverse\":" + plan.candidateSkillUniverse
                + ",\"beforeTopScore\":" + Json.num(beforeTop)
                + ",\"afterTopScore\":" + Json.num(afterTop)
                + ",\"utilityLiftPercent\":" + Json.num(plan.baselineUtility == 0 ? 0.0 : ((plan.projectedUtility - plan.baselineUtility) * 100.0 / plan.baselineUtility))
                + ",\"topJobAfter\":" + (reranked.length == 0 ? "null" : jobJson(reranked[0].getJob()))
                + ",\"complexity\":\"O(2^m * J), bounded to m <= 15 for the demo\"}";
    }

    private String benchmark(HttpExchange ex) throws BadRequest {
        Params p = Params.fromQuery(ex);
        String q = p.required("q");
        JobSearchEngine.Benchmark x = searchEngine.benchmark(q);
        String winner = winner(x.kmpNanos, x.zNanos, x.rkNanos);
        return "{\"ok\":true,\"query\":" + Json.q(q)
                + ",\"winnerByMeasuredTime\":" + Json.q(winner)
                + ",\"note\":\"Timing varies with JVM warm-up; operation counts are the stronger academic comparison.\""
                + ",\"algorithms\":["
                + benchmarkItem("KMP", x.kmpMatches, x.kmpComparisons, x.kmpNanos, "O(n+m)", "character comparisons") + ","
                + benchmarkItem("Z Algorithm", x.zMatches, x.zComparisons, x.zNanos, "O(n+m)", "Z comparisons") + ","
                + benchmarkItem("Rabin-Karp", x.rkMatches, x.rkHashChecks, x.rkNanos, "Average O(n+m)", "rolling-hash windows")
                + "]}";
    }

    private String suffix(HttpExchange ex) throws BadRequest {
        Params p = Params.fromQuery(ex);
        String q = p.required("q");
        long start = System.nanoTime();
        SuffixArrayIndex index = searchEngine.buildSuffixIndex();
        long buildNanos = System.nanoTime() - start;
        start = System.nanoTime();
        boolean found = index.contains(q);
        long queryNanos = System.nanoTime() - start;
        int maxLcp = 0;
        int[] lcp = index.getLcp();
        for (int i = 0; i < lcp.length; i++) if (lcp[i] > maxLcp) maxLcp = lcp[i];
        return "{\"ok\":true,\"query\":" + Json.q(q) + ",\"found\":" + found
                + ",\"suffixes\":" + index.getSuffixArray().length
                + ",\"maxLcp\":" + maxLcp
                + ",\"buildMs\":" + Json.num(buildNanos / 1_000_000.0)
                + ",\"queryMs\":" + Json.num(queryNanos / 1_000_000.0)
                + ",\"complexity\":\"Index build O(n log^2 n) in this implementation; query O(m log n)\"}";
    }

    private String featured(HttpExchange ex) {
        Params p = Params.fromQuery(ex);
        int k = p.intValue("k", 5, 1, 12);
        Job[] sample = new ReservoirSampler().sample(repository.all(), k);
        StringBuilder b = new StringBuilder("{\"ok\":true,\"algorithm\":\"Reservoir Sampling\",\"complexity\":\"O(n) time, O(k) sample memory\",\"jobs\":[");
        for (int i = 0; i < sample.length; i++) {
            if (i > 0) b.append(',');
            b.append(jobJson(sample[i]));
        }
        return b.append("]}").toString();
    }

    private String allocate(HttpExchange ex) throws Exception {
        Params p = Params.fromExchange(ex);
        int count = p.intValue("count", 4, 1, 8);
        double threshold = p.doubleValue("threshold", 45.0, 0, 100);
        Candidate[] candidates = new Candidate[count];
        for (int i = 0; i < count; i++) {
            String name = p.get("name" + i, "Candidate " + (i + 1));
            String skills = normalizeSkills(p.get("skills" + i, ""));
            String location = p.get("location" + i, "");
            if (skills.isBlank()) throw new BadRequest("Candidate " + (i + 1) + " needs at least one skill.");
            candidates[i] = new Candidate(i + 1, name, skills, location);
        }

        long start = System.nanoTime();
        CandidateJobMatcher.AssignmentResult result = new CandidateJobMatcher().assign(candidates, repository.all(), threshold);
        long nanos = System.nanoTime() - start;

        StringBuilder b = new StringBuilder("{\"ok\":true,\"algorithm\":\"Edmonds-Karp Max Flow\",\"complexity\":\"O(V*E^2)\",\"threshold\":")
                .append(Json.num(threshold)).append(",\"maxAssignments\":").append(result.maxAssignments)
                .append(",\"candidateCount\":").append(candidates.length)
                .append(",\"timeMs\":").append(Json.num(nanos / 1_000_000.0)).append(",\"assignments\":[");
        for (int i = 0; i < candidates.length; i++) {
            if (i > 0) b.append(',');
            b.append("{\"candidate\":").append(Json.q(candidates[i].getName()));
            int index = result.assignedJobIndex[i];
            if (index >= 0) b.append(",\"assigned\":true,\"job\":").append(jobJson(repository.all().get(index)));
            else b.append(",\"assigned\":false,\"job\":null");
            b.append('}');
        }
        return b.append("]}").toString();
    }

    private String optimize(HttpExchange ex) throws Exception {
        Params p = Params.fromExchange(ex);
        String name = p.get("name", "Candidate");
        String skills = normalizeSkills(p.get("skills", ""));
        String location = p.get("location", "");
        String title = p.get("title", "");
        int hours = p.intValue("hours", 10, 1, 40);
        if (skills.isBlank()) throw new BadRequest("Enter candidate skills for the optimizer.");

        Candidate candidate = new Candidate(1, name, skills, location);
        MatchResult[] ranked = recommendationEngine.recommend(candidate, title);
        int n = Math.min(12, ranked.length);
        MatchResult[] candidates = new MatchResult[n];
        for (int i = 0; i < n; i++) candidates[i] = ranked[i];

        KnapsackOptimizer optimizer = new KnapsackOptimizer();
        long start = System.nanoTime();
        KnapsackOptimizer.Plan exact = optimizer.exact(candidates, hours);
        long exactNanos = System.nanoTime() - start;
        start = System.nanoTime();
        KnapsackOptimizer.Plan approx = optimizer.halfApproximation(candidates, hours);
        long approxNanos = System.nanoTime() - start;
        double ratio = exact.totalValue == 0 ? 1.0 : (double) approx.totalValue / exact.totalValue;

        return "{\"ok\":true,\"hours\":" + hours
                + ",\"exact\":" + planJson(candidates, exact, exactNanos)
                + ",\"approx\":" + planJson(candidates, approx, approxNanos)
                + ",\"observedRatio\":" + Json.num(ratio)
                + ",\"explanation\":\"The exact 0/1-knapsack DP is pseudo-polynomial in the hour budget; the comparison heuristic keeps the better of density-greedy and the best single job.\"}";
    }

    private String parallel(HttpExchange ex) throws Exception {
        Params p = Params.fromQuery(ex);
        int threads = p.intValue("threads", 4, 1, 16);
        DynamicArray<Job> jobs = repository.all();
        int[] salary = new int[jobs.size()];
        long sequential = 0;
        for (int i = 0; i < jobs.size(); i++) { salary[i] = jobs.get(i).getSalaryLpa(); sequential += salary[i]; }
        long start = System.nanoTime();
        long parallel = new ParallelScoreReducer().parallelSum(salary, threads);
        long nanos = System.nanoTime() - start;
        return "{\"ok\":true,\"threads\":" + threads + ",\"sequentialCheck\":" + sequential
                + ",\"parallelResult\":" + parallel + ",\"equal\":" + (sequential == parallel)
                + ",\"timeMs\":" + Json.num(nanos / 1_000_000.0)
                + ",\"note\":\"This small dataset is for work/span demonstration; thread overhead may dominate.\"}";
    }

    private String complexityFor(JobSearchEngine.Algorithm a) {
        return switch (a) {
            case KMP -> "O(n+m)";
            case Z -> "O(n+m)";
            case RABIN_KARP -> "Average O(n+m), verification on hash hits";
        };
    }

    private String winner(long k, long z, long r) {
        if (k <= z && k <= r) return "KMP";
        if (z <= r) return "Z Algorithm";
        return "Rabin-Karp";
    }

    private String benchmarkItem(String name, int matches, long ops, long nanos, String complexity, String metric) {
        return "{\"name\":" + Json.q(name) + ",\"matches\":" + matches + ",\"operations\":" + ops
                + ",\"timeMs\":" + Json.num(nanos / 1_000_000.0) + ",\"complexity\":" + Json.q(complexity)
                + ",\"metric\":" + Json.q(metric) + "}";
    }

    private String planJson(MatchResult[] jobs, KnapsackOptimizer.Plan plan, long nanos) {
        StringBuilder b = new StringBuilder("{\"method\":").append(Json.q(plan.method)).append(",\"totalHours\":")
                .append(plan.totalHours).append(",\"totalValue\":").append(plan.totalValue)
                .append(",\"timeMs\":").append(Json.num(nanos / 1_000_000.0)).append(",\"jobs\":[");
        boolean first = true;
        for (int i = 0; i < jobs.length; i++) {
            if (!plan.selected[i]) continue;
            if (!first) b.append(',');
            first = false;
            b.append("{\"job\":").append(jobJson(jobs[i].getJob())).append(",\"matchScore\":").append(Json.num(jobs[i].getScore())).append('}');
        }
        return b.append("]}").toString();
    }

    private String candidateJson(Candidate c) {
        return "{\"name\":" + Json.q(c.getName()) + ",\"skills\":" + Json.stringArray(c.skillArray())
                + ",\"location\":" + Json.q(c.getPreferredLocation()) + "}";
    }

    private String matchJson(MatchResult result, Candidate candidate) {
        String[] candidateSkills = candidate.skillArray();
        String[] jobSkills = result.getJob().skillArray();
        String[] matched = new String[jobSkills.length];
        String[] missing = new String[jobSkills.length];
        int mc = 0, xc = 0;
        for (int i = 0; i < jobSkills.length; i++) {
            boolean hit = false;
            for (int k = 0; k < candidateSkills.length; k++) {
                if (jobSkills[i].equalsIgnoreCase(candidateSkills[k])) { hit = true; break; }
            }
            if (hit) matched[mc++] = jobSkills[i]; else missing[xc++] = jobSkills[i];
        }
        return "{\"job\":" + jobJson(result.getJob())
                + ",\"score\":" + Json.num(result.getScore())
                + ",\"skillScore\":" + Json.num(result.getSkillScore())
                + ",\"locationScore\":" + Json.num(result.getLocationScore())
                + ",\"titleScore\":" + Json.num(result.getTitleScore())
                + ",\"matchedSkills\":" + Json.stringArray(matched, mc)
                + ",\"missingSkills\":" + Json.stringArray(missing, xc) + "}";
    }

    private String jobJson(Job j) {
        return "{\"id\":" + j.getId()
                + ",\"title\":" + Json.q(j.getTitle())
                + ",\"company\":" + Json.q(j.getCompany())
                + ",\"location\":" + Json.q(j.getLocation())
                + ",\"skills\":" + Json.stringArray(j.skillArray())
                + ",\"description\":" + Json.q(j.getDescription())
                + ",\"salaryLpa\":" + j.getSalaryLpa()
                + ",\"experienceYears\":" + j.getExperienceYears()
                + ",\"vacancies\":" + j.getVacancies()
                + ",\"applyHours\":" + j.getApplyHours()
                + ",\"workMode\":" + Json.q(j.getWorkMode()) + "}";
    }

    private String normalizeSkills(String raw) {
        if (raw == null) return "";
        String s = raw.replace(',', '|').replace(';', '|');
        String[] parts = s.split("\\|");
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String x = parts[i].trim();
            if (x.isEmpty()) continue;
            if (b.length() > 0) b.append('|');
            b.append(x);
        }
        return b.toString();
    }

    private void sendJson(HttpExchange exchange, int code, String json) throws IOException {
        send(exchange, code, "application/json; charset=utf-8", json.getBytes(StandardCharsets.UTF_8));
    }

    private void send(HttpExchange exchange, int code, String contentType, byte[] bytes) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().set("Content-Security-Policy", "default-src 'self'; style-src 'self' 'unsafe-inline'; script-src 'self'; img-src 'self' data:; connect-src 'self'");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
    }

    private class StaticHandler implements HttpHandler {
        @Override public void handle(HttpExchange exchange) throws IOException {
            try {
                if (!"GET".equalsIgnoreCase(exchange.getRequestMethod()) && !"HEAD".equalsIgnoreCase(exchange.getRequestMethod())) {
                    send(exchange, 405, "text/plain; charset=utf-8", "Method not allowed".getBytes(StandardCharsets.UTF_8));
                    return;
                }
                String raw = exchange.getRequestURI().getPath();
                if (raw != null && raw.startsWith("/api/")) {
                    sendJson(exchange, 404, "{\"ok\":false,\"error\":\"Unknown API route\"}");
                    return;
                }
                if (raw == null || raw.equals("/")) raw = "/index.html";
                String decoded = URLDecoder.decode(raw, StandardCharsets.UTF_8);
                File requested = new File(webRoot, decoded.substring(1)).getCanonicalFile();
                if (!requested.toPath().startsWith(webRoot.toPath()) || !requested.isFile()) {
                    // SPA fallback for friendly client-side navigation.
                    requested = new File(webRoot, "index.html").getCanonicalFile();
                }
                byte[] bytes = Files.readAllBytes(requested.toPath());
                if ("HEAD".equalsIgnoreCase(exchange.getRequestMethod())) bytes = new byte[0];
                send(exchange, 200, mime(requested.getName()), bytes);
            } catch (Exception e) {
                send(exchange, 404, "text/plain; charset=utf-8", "Not found".getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    private String mime(String name) {
        String lower = name.toLowerCase();
        if (lower.endsWith(".html")) return "text/html; charset=utf-8";
        if (lower.endsWith(".css")) return "text/css; charset=utf-8";
        if (lower.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".ico")) return "image/x-icon";
        return "application/octet-stream";
    }

    private interface ApiAction { String handle(HttpExchange exchange) throws Exception; }

    private static class BadRequest extends Exception { BadRequest(String message) { super(message); } }

    private static class Params {
        private String[] keys = new String[16];
        private String[] values = new String[16];
        private int size;

        static Params fromQuery(HttpExchange ex) {
            String raw = ex.getRequestURI().getRawQuery();
            return parse(raw == null ? "" : raw);
        }

        static Params fromExchange(HttpExchange ex) throws IOException {
            Params query = fromQuery(ex);
            if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) return query;
            byte[] body = readAll(ex.getRequestBody(), 64 * 1024);
            Params form = parse(new String(body, StandardCharsets.UTF_8));
            for (int i = 0; i < form.size; i++) query.put(form.keys[i], form.values[i]);
            return query;
        }

        private static byte[] readAll(InputStream in, int max) throws IOException {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int total = 0;
            int n;
            while ((n = in.read(buffer)) != -1) {
                total += n;
                if (total > max) throw new IOException("Request body too large");
                out.write(buffer, 0, n);
            }
            return out.toByteArray();
        }

        static Params parse(String encoded) {
            Params p = new Params();
            if (encoded == null || encoded.isEmpty()) return p;
            String[] pairs = encoded.split("&");
            for (int i = 0; i < pairs.length; i++) {
                int eq = pairs[i].indexOf('=');
                String k = eq >= 0 ? pairs[i].substring(0, eq) : pairs[i];
                String v = eq >= 0 ? pairs[i].substring(eq + 1) : "";
                p.put(decode(k), decode(v));
            }
            return p;
        }

        private static String decode(String s) {
            try { return URLDecoder.decode(s, StandardCharsets.UTF_8); }
            catch (Exception e) { return s; }
        }

        void put(String key, String value) {
            for (int i = 0; i < size; i++) {
                if (keys[i].equals(key)) { values[i] = value; return; }
            }
            if (size == keys.length) {
                String[] nk = new String[keys.length * 2];
                String[] nv = new String[values.length * 2];
                for (int i = 0; i < size; i++) { nk[i] = keys[i]; nv[i] = values[i]; }
                keys = nk; values = nv;
            }
            keys[size] = key; values[size] = value; size++;
        }

        String get(String key, String fallback) {
            for (int i = 0; i < size; i++) if (keys[i].equals(key)) return values[i];
            return fallback;
        }

        String required(String key) throws BadRequest {
            String value = get(key, "").trim();
            if (value.isEmpty()) throw new BadRequest("Missing required parameter: " + key);
            return value;
        }

        int intValue(String key, int fallback, int min, int max) {
            try { return Math.max(min, Math.min(max, Integer.parseInt(get(key, Integer.toString(fallback)).trim()))); }
            catch (NumberFormatException e) { return fallback; }
        }

        double doubleValue(String key, double fallback, double min, double max) {
            try { return Math.max(min, Math.min(max, Double.parseDouble(get(key, Double.toString(fallback)).trim()))); }
            catch (NumberFormatException e) { return fallback; }
        }
    }

    private static class Json {
        static String q(String value) {
            if (value == null) return "null";
            StringBuilder b = new StringBuilder("\"");
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                switch (c) {
                    case '\\' -> b.append("\\\\");
                    case '"' -> b.append("\\\"");
                    case '\n' -> b.append("\\n");
                    case '\r' -> b.append("\\r");
                    case '\t' -> b.append("\\t");
                    default -> {
                        if (c < 32) b.append(String.format("\\u%04x", (int) c));
                        else b.append(c);
                    }
                }
            }
            return b.append('"').toString();
        }

        static String num(double value) {
            if (Double.isNaN(value) || Double.isInfinite(value)) return "0";
            double rounded = Math.round(value * 1000.0) / 1000.0;
            return Double.toString(rounded);
        }

        static String stringArray(String[] values) { return stringArray(values, values == null ? 0 : values.length); }
        static String stringArray(String[] values, int count) {
            StringBuilder b = new StringBuilder("[");
            for (int i = 0; i < count; i++) {
                if (i > 0) b.append(',');
                b.append(q(values[i]));
            }
            return b.append(']').toString();
        }
    }
}
