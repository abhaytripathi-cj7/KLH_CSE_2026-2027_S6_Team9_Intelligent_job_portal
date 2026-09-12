package com.dsa.jobportal.service;

import com.dsa.jobportal.algorithms.flow.EdmondsKarp;
import com.dsa.jobportal.model.Candidate;
import com.dsa.jobportal.model.Job;
import com.dsa.jobportal.model.MatchResult;
import com.dsa.jobportal.structures.DynamicArray;

public class CandidateJobMatcher {
    public static class AssignmentResult {
        public final int maxAssignments;
        public final int[] assignedJobIndex;

        public AssignmentResult(int maxAssignments, int[] assignedJobIndex) {
            this.maxAssignments = maxAssignments;
            this.assignedJobIndex = assignedJobIndex;
        }
    }

    public AssignmentResult assign(Candidate[] candidates, DynamicArray<Job> jobs, double minimumScore) {
        int c = candidates.length;
        int j = jobs.size();
        int source = 0;
        int firstCandidate = 1;
        int firstJob = firstCandidate + c;
        int sink = firstJob + j;
        int n = sink + 1;
        int[][] capacity = new int[n][n];

        JobRecommendationEngine engine = new JobRecommendationEngine(jobs);
        for (int i = 0; i < c; i++) {
            capacity[source][firstCandidate + i] = 1;
            for (int k = 0; k < j; k++) {
                MatchResult score = engine.score(candidates[i], "", jobs.get(k));
                if (score.getScore() >= minimumScore) capacity[firstCandidate + i][firstJob + k] = 1;
            }
        }
        for (int k = 0; k < j; k++) capacity[firstJob + k][sink] = Math.max(1, jobs.get(k).getVacancies());

        EdmondsKarp.Result flowResult = new EdmondsKarp().compute(capacity, source, sink);
        int[][] flow = flowResult.getFlow();
        int[] assigned = new int[c];
        for (int i = 0; i < c; i++) {
            assigned[i] = -1;
            int node = firstCandidate + i;
            for (int k = 0; k < j; k++) {
                if (flow[node][firstJob + k] > 0) {
                    assigned[i] = k;
                    break;
                }
            }
        }
        return new AssignmentResult(flowResult.getMaxFlow(), assigned);
    }
}
