package com.dsa.jobportal.service;

import com.dsa.jobportal.algorithms.dp.EditDistance;
import com.dsa.jobportal.algorithms.randomized.RandomizedQuickSort;
import com.dsa.jobportal.algorithms.stringalgorithms.AhoCorasick;
import com.dsa.jobportal.algorithms.stringalgorithms.KMP;
import com.dsa.jobportal.model.Candidate;
import com.dsa.jobportal.model.Job;
import com.dsa.jobportal.model.MatchResult;
import com.dsa.jobportal.structures.DynamicArray;

public class JobRecommendationEngine {
    private final DynamicArray<Job> jobs;
    private final EditDistance edit = new EditDistance();
    private final KMP kmp = new KMP();

    public JobRecommendationEngine(DynamicArray<Job> jobs) {
        this.jobs = jobs;
    }

    public MatchResult[] recommend(Candidate candidate, String desiredTitle) {
        MatchResult[] results = new MatchResult[jobs.size()];
        for (int i = 0; i < jobs.size(); i++) results[i] = score(candidate, desiredTitle, jobs.get(i));
        new RandomizedQuickSort().sortDescending(results);
        return results;
    }

    public MatchResult score(Candidate candidate, String desiredTitle, Job job) {
        String[] candidateSkills = candidate.skillArray();
        double skillScore = exactSkillScore(candidateSkills, job);

        double locationScore = candidate.getPreferredLocation().isBlank() ? 50.0 :
                (job.getLocation().equalsIgnoreCase(candidate.getPreferredLocation()) ? 100.0 :
                 edit.similarityPercent(job.getLocation(), candidate.getPreferredLocation()));

        double titleScore = titleSimilarity(desiredTitle, job.getTitle());

        double finalScore = 0.60 * skillScore + 0.25 * locationScore + 0.15 * titleScore;
        return new MatchResult(job, clamp(finalScore), clamp(skillScore), clamp(locationScore), clamp(titleScore));
    }

    /**
     * Aho-Corasick is used as a multi-pattern matcher, but with explicit '|skill|'
     * boundaries so a skill such as "Java" does not incorrectly match "JavaScript".
     */
    private double exactSkillScore(String[] candidateSkills, Job job) {
        String[] requiredJobSkills = job.skillArray();
        if (candidateSkills.length == 0 || requiredJobSkills.length == 0) return 0.0;
        String[] patterns = new String[candidateSkills.length];
        for (int i = 0; i < candidateSkills.length; i++) {
            patterns[i] = "|" + candidateSkills[i].trim().toLowerCase() + "|";
        }
        String normalizedJobSkills = "|" + job.getSkills().toLowerCase() + "|";
        AhoCorasick matcher = new AhoCorasick(patterns);
        int matchedRequiredSkills = matcher.countDistinctMatches(normalizedJobSkills);
        // Score job-skill coverage, not candidate-skill precision. Extra candidate
        // skills must never reduce a recommendation score.
        return 100.0 * matchedRequiredSkills / requiredJobSkills.length;
    }

    private double titleSimilarity(String desiredTitle, String jobTitle) {
        if (desiredTitle == null || desiredTitle.isBlank()) return 50.0;
        String q = desiredTitle.trim();
        if (kmp.contains(jobTitle, q)) return 100.0;
        return edit.similarityPercent(q, jobTitle);
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(100.0, value));
    }
}
