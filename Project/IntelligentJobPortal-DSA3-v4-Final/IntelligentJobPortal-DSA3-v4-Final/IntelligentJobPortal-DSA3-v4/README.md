# NovaHire — Intelligent Job Portal (Web Edition V4 — Final Product Build)

NovaHire is a polished Java-based intelligent job portal for job seekers, recruiters and administrators. The normal website behaves like a real career platform; the advanced DSA-3 algorithms remain behind the scenes as the search, matching, ranking and optimization engine.

## What is in V4

### Job seeker experience
- Premium product-first dark career-discovery hero with refined typography
- Application Strategy planner for prioritizing high-fit jobs within limited weekly time
- Professional overview dashboard
- Smart job discovery with role/location search and filters
- Personalized match percentages
- Job details with **Why should you apply?** analysis
- **Skill Gap Detector** with matched and missing skills
- Save jobs and persistent shortlist
- **Opportunity Compare** for 2–3 jobs
- Competition indicator and applicant count
- Application pipeline and status tracking
- Company discovery with verified employers
- Career analytics and profile strength
- **Skill Unlock** career suggestion
- **Opportunity Lens**: Best Fit, Growth, Salary and Low Competition views
- Smart job alerts

### Recruiter experience
- Recruiter dashboard
- Job management
- Candidate ranking
- **Anonymous Candidate Mode** to hide identity before shortlisting
- Shortlist/reject workflow
- Interview schedule
- Verified company profile
- Functional local job publishing: newly posted roles become searchable immediately

### Admin experience
- Platform dashboard
- Employer verification queue
- Report review
- Marketplace analytics

## DSA engine
The user-facing UI intentionally does not expose course codes, course-outcome labels or algorithm names. The Java source keeps the DSA implementation for academic evaluation:
- KMP, Z Algorithm, Rabin-Karp
- Aho-Corasick
- Suffix Array + LCP
- Wagner-Fischer Edit Distance
- Bitmask/subset-state dynamic programming
- Edmonds-Karp max flow
- Knapsack optimization + approximation
- Randomized QuickSort
- Reservoir Sampling
- Parallel reduction
- Custom DynamicArray, queue and hash table

## Run on Windows / VS Code
1. Extract the ZIP.
2. Open the extracted folder in VS Code.
3. Make sure JDK 17 or later is installed (`java -version` and `javac -version`).
4. Open the VS Code terminal in the project root.
5. Run:

```bat
.\run-web.bat
```

The launcher compiles all Java sources, runs the self-test suite, starts the local server and normally opens the browser automatically.

Default address:

```text
http://localhost:8080
```

If 8080 is occupied, the launcher automatically tries 8081–8089.

## Console edition
The original console workflow is still available:

```bat
.\run.bat
```

## Project structure

```text
IntelligentJobPortal-DSA3-v4/
├── src/                 Java models, services, DSA engine and HTTP server
├── web/                 NovaHire V4 frontend
├── data/jobs.csv        Sample job corpus
├── docs/                Academic/project notes and verification report
├── run-web.bat          Recommended Windows launcher
├── run-web.ps1          PowerShell launcher
├── run.bat              Console launcher
└── START_HERE.txt
```

## Notes
- This is a local academic demo, not a production recruitment system.
- Saved jobs, application state and profile edits use browser `localStorage` so the experience persists across refreshes.
- Recruiter job publishing is live for the current server session; the original CSV is intentionally left unchanged.
- Admin review controls are local demonstration workflows.
- No external web services, paid APIs or internet connection are required after download.
