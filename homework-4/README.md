# 🤖 4-Agent Bug Fix Pipeline

> **Student Name**: Ivan Petrych
> **Date Submitted**: 10.03.2026
> **AI Tools Used**: [Claude Sonnet 4.6, Claude Haiku 4.5]

Automated multi-agent system for bug research, implementation, security verification, and unit testing.

---

## 📋 Agents

| Agent | Purpose | Input | Output |
|-------|---------|-------|--------|
| **Bug Researcher** | Analyzes codebase to identify root cause of a bug | Bug report (`bug-context.md`) | Research findings (`codebase-research.md`) |
| **Bug Research Verifier** | Fact-checks research; verifies file:line references and snippet accuracy | Research (`codebase-research.md`) | Verified research with quality score (`verified-research.md`) |
| **Bug Planner** | Creates a deterministic implementation plan from verified research | Verified research (`verified-research.md`) | Step-by-step fix plan (`implementation-plan.md`) |
| **Bug Implementer** | Executes the plan exactly; applies code changes and runs tests | Implementation plan (`implementation-plan.md`) | Change summary (`fix-summary.md`) |
| **Security Verifier** | Scans changed code for vulnerabilities (injection, secrets, validation gaps, etc.) | Fix summary (`fix-summary.md`) + changed files | Security scan report (`security-report.md`) |
| **Unit Test Generator** | Generates and runs unit tests following FIRST criteria for changed code | Fix summary (`fix-summary.md`) + changed files | Test report + test files (`test-report.md`, `tests/`) |
| **Post-Fix Orchestrator** | Coordinates Security Verifier → Unit Test Generator pipeline; gates on findings/failures | Fix summary (`fix-summary.md`) | Pipeline summary (`pipeline-report.md`) |

---

## 🔗 Pipeline Execution Flow

### Full Pipeline (Automated)

```
Bug Context
    │
    ▼
┌─────────────────┐
│  Bug Researcher │  (human-guided or agent-powered)
└────────┬────────┘
         │ codebase-research.md
         ▼
┌─────────────────────────┐
│ Bug Research Verifier   │  
└────────┬────────────────┘
         │ verified-research.md (quality scored)
         ▼
┌─────────────────┐
│   Bug Planner   │
└────────┬────────┘
         │ implementation-plan.md
         ▼
┌──────────────────┐
│  Bug Implementer │
└────────┬─────────┘
         │ fix-summary.md (tests embedded)
         ▼
┌──────────────────────────┐
│ Post-Fix Orchestrator    │  (gates & coordinates)
├──────────────────────────┤
│ ▼ Stage 1                │
│ Security Verifier        │ → security-report.md
│ (CRITICAL/HIGH blocks)   │
│                          │
│ ▼ Stage 2 (if pass)      │
│ Unit Test Generator      │ → test-report.md
│ (test failures block)    │
└──────────────────────────┘
         │
         ▼
   pipeline-report.md (PASS or BLOCKED)
```

---

## 🚀 How to Run

### Prerequisites

- **Node.js** (v16+) and **npm**
- **VS Code** with GitHub Copilot agent support
- Repository cloned to `/Users/admin/AI-Coding-Partner-Homework/homework-4/`

### 1. Run the Demo Application

Start the API server (before running agents, to verify baseline):

```bash
cd demo-bug-fix
npm install
npm start
```

Server runs on `http://localhost:3000`. Test health endpoint:

```bash
curl http://localhost:3000/health
```

### 2. Run the Agent Pipeline

#### Step 1: Bug Research (Initial Analysis)

Invoke **Bug Researcher** agent in VS Code with the bug report path:

```
context/bugs/API-404/bug-context.md
```

Produces: `context/bugs/API-404/research/codebase-research.md`

#### Step 2: Verify Research (Quality Gate)

Invoke **Bug Research Verifier** agent:

```
context/bugs/API-404/research/codebase-research.md
```

Produces: `context/bugs/API-404/research/verified-research.md` (with quality score: GOLD/SILVER/BRONZE/FAILED)

#### Step 3: Create Implementation Plan

Invoke **Bug Planner** agent:

```
context/bugs/API-404/research/verified-research.md
```

Produces: `context/bugs/API-404/implementation-plan.md`

#### Step 4: Implement Fix

Invoke **Bug Implementer** agent:

```
context/bugs/API-404/implementation-plan.md
```

Produces: `context/bugs/API-404/fix-summary.md`

After this, the app is patched. Verify the fix works:

```bash
# In the demo-bug-fix directory (from step 1)
curl http://localhost:3000/api/users/123
# Expected: { "id": 123, "name": "Alice Smith", ... }
```

#### Step 5: Run Post-Fix Pipeline (Automated)

Invoke **Post-Fix Orchestrator** agent:

```
context/bugs/API-404/fix-summary.md
```

The orchestrator **automatically** runs:

1. **Security Verifier** → produces `context/bugs/API-404/security-report.md`
   - If CRITICAL/HIGH findings → pipeline halts; handoff to Bug Implementer
   - If clear → proceeds to step 2

2. **Unit Test Generator** → produces `context/bugs/API-404/test-report.md` + `tests/API-404/`
   - If tests fail → pipeline halts; handoff to Bug Implementer
   - If all pass → proceeds to final report

3. Writes final `context/bugs/API-404/pipeline-report.md` (PASS or BLOCKED summary)

---

## 📊 Typical Output Structure

After running the full pipeline for a bug fix:

```
context/bugs/API-404/
├── bug-context.md                      # Original bug report
├── research/
│   ├── codebase-research.md            # Initial findings
│   └── verified-research.md            # Quality-checked research
├── implementation-plan.md              # Step-by-step fix plan
├── fix-summary.md                      # What was changed & test results
├── security-report.md                  # Vulnerability scan
├── test-report.md                      # Unit test execution results
└── pipeline-report.md                  # Final orchestrator summary

tests/
└── API-404/
    └── userController.test.js          # Generated unit tests

demo-bug-fix/src/controllers/
└── userController.js                   # FIXED: Number(req.params.id)
```

---

## ✅ Success Criteria

- ✅ All agents execute in sequence
- ✅ Each agent produces its output file as specified
- ✅ Security Verifier finds no CRITICAL/HIGH issues (or blocks if it does)
- ✅ Unit Test Generator produces tests that all pass
- ✅ Pipeline report is PASS
- ✅ Manual curl tests confirm the fix works

---

## 🛠️ Skills

The pipeline uses two skill files to define quality standards:

- **`skills/research-quality-measurement.md`** — defines research quality levels (GOLD/SILVER/BRONZE/FAILED)
- **`skills/unit-tests-FIRST.md`** — defines unit test quality criteria (Fast, Independent, Repeatable, Self-validating, Timely)

Both skills are referenced and applied by their respective agents.

---

## 📁 File Structure

```
.github/
├── agents/
│   ├── bug-researcher.agent.md
│   ├── research-verifier.agent.md
│   ├── bug-planner.agent.md
│   ├── bug-implementer.agent.md
│   ├── security-verifier.agent.md
│   ├── unit-test-generator.agent.md
│   └── post-fix-orchestrator.agent.md
└── skills/
    ├── research-quality-measurement.md
    └── unit-tests-FIRST.md

context/bugs/
└── API-404/
    ├── bug-context.md
    ├── research/
    │   ├── codebase-research.md
    │   └── verified-research.md
    ├── implementation-plan.md
    ├── fix-summary.md
    ├── security-report.md
    ├── test-report.md
    └── pipeline-report.md

demo-bug-fix/
├── src/
│   ├── controllers/
│   │   └── userController.js (FIXED)
│   └── routes/
│       └── users.js
├── server.js
├── package.json
└── tests/

tests/
└── API-404/
    └── userController.test.js
```

---

## 🔄 Retry & Repair

If the pipeline halts (security findings or test failures):

1. Bug Implementer receives the block notification with specific issues
2. Developer (or agent) reads the report and identifies the problem
3. Fix is implemented and documented in a revised `implementation-plan.md`
4. Re-run Bug Implementer with the updated plan
5. Re-invoke Post-Fix Orchestrator to verify the repair

---

## 📝 Notes

- All agent invocations happen in **VS Code Copilot Chat** using the agent name and input path
- Agents are **deterministic** — given the same input, they produce consistent output
- Pipeline **gates** ensure security issues are found before tests run (avoid testing unsafe code)
- Orchestrator **halts and hands off** on any critical failure, keeping the pipeline transparent

