# NovaHire V4 — Project Overview

## Product vision
NovaHire is an intelligent career platform that helps candidates discover suitable opportunities and helps recruiters discover suitable candidates.

The V4 redesign deliberately separates **product experience** from **academic implementation**. Candidates and recruiters see normal hiring features; advanced algorithms work behind the scenes.

## Signature product features
The normal interface never displays course-outcome labels, course codes, algorithm names or academic terminology. The DSA engine stays behind the product experience.

1. Smart Match Score
2. Skill Gap Detector
3. Why Should You Apply? explanation
4. Opportunity Compare
5. Competition Indicator
6. Career Analytics
7. Profile Strength
8. Skill Unlock planner
9. Opportunity Lens
10. Anonymous Candidate Mode
11. Verified Employers
12. Smart Job Alerts
13. Application tracking
14. Focused Application Strategy planner

## Academic engine mapping
The following mapping is for the report and viva only; it is not displayed in the main website.

- Exact search: KMP / Z / Rabin-Karp
- Multi-skill matching: Aho-Corasick
- Fuzzy recovery and similarity: Wagner-Fischer edit distance
- Text indexing: Suffix Array + LCP
- Skill-upgrade planning: bounded subset/bitmask DP
- Candidate/opening allocation: Edmonds-Karp max flow
- Limited application-time planning: knapsack DP + approximation
- Ranking: Randomized QuickSort
- Stream sampling: Reservoir Sampling
- Scalable aggregation demonstration: parallel reduction

## Architecture
Browser UI -> Java HTTP layer -> Java services -> hand-built DSA engine -> CSV job corpus

The browser uses HTML/CSS/JavaScript only. The Java server uses the JDK built-in HTTP server, so no Spring Boot, database server, Node package install, Docker or external API is required.
