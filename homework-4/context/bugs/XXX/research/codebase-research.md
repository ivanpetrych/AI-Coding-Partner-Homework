# Codebase Research: [BUG-ID]

<!-- Output skeleton for the Bug Researcher agent. Fill every section. Do not remove headings. -->

**Bug ID**: `<!-- e.g. API-404 -->`  
**Title**: `<!-- short bug title -->`  
**Researcher**: `<!-- agent or human name -->`  
**Date**: `<!-- YYYY-MM-DD -->`

---

## 1. Root Cause Analysis

<!-- REQUIRED. One clear paragraph — what is broken and why.
     State the exact technical mechanism. No speculation. No "maybe". -->

---

## 2. Code Findings

<!-- One sub-section per distinct finding. Maximum 5. -->

### Finding 1

- **File**: `path/to/file.js`
- **Line**: `N`
- **Snippet** (exact, copied from source):
  ```js
  // paste the exact line(s) — do not paraphrase
  ```
- **Why it matters**: <!-- one sentence linking this line to the root cause -->

<!-- Repeat for Finding 2 … Finding N -->

---

## 3. Proposed Fix Surface

<!-- List files and lines that MUST change to resolve the bug.
     Do not describe how to fix — only WHERE. -->

| File | Line(s) | Change needed (one line) |
|------|---------|--------------------------|
| `path/to/file.js` | N | <!-- e.g. "replace === with ==" --> |

---

## 4. Risks and Edge Cases

<!-- List what could break or behave unexpectedly after the fix.
     Minimum 1 item. Be specific. -->

- **Risk**: <!-- what could go wrong -->  
  **Scope**: <!-- which other code/feature is affected -->

---

## 5. Suggested Test Cases

<!-- Describe test cases that would catch this bug and verify the fix.
     Minimum 2. Format: input → expected output. -->

| # | Input | Expected Result |
|---|-------|-----------------|
| 1 | <!-- e.g. GET /api/users/123 --> | <!-- e.g. 200 + user object --> |
| 2 | <!-- negative/edge case --> | <!-- expected behaviour --> |

---

## 6. References

<!-- Files opened during research. One entry per file. -->

- `path/to/file.js` — <!-- one-line note on why it was read -->
