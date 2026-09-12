package com.dsa.jobportal.model;

public class MatchResult {
    private final Job job;
    private final double score;
    private final double skillScore;
    private final double locationScore;
    private final double titleScore;

    public MatchResult(Job job, double score, double skillScore, double locationScore, double titleScore) {
        this.job = job;
        this.score = score;
        this.skillScore = skillScore;
        this.locationScore = locationScore;
        this.titleScore = titleScore;
    }

    public Job getJob() { return job; }
    public double getScore() { return score; }
    public double getSkillScore() { return skillScore; }
    public double getLocationScore() { return locationScore; }
    public double getTitleScore() { return titleScore; }
}
