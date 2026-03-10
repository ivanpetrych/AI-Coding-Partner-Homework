# Fix Summary: [BUG-ID]

**Bug ID**: `<!-- e.g. API-404 -->`  
**Title**: `<!-- short bug title -->`  
**Implementer**: `<!-- agent or human name -->`  
**Date**: `<!-- YYYY-MM-DD -->`  
**Plan source**: `context/bugs/<!-- BUG-ID -->/implementation-plan.md`  
**Overall Status**: `<!-- PASS / FAIL -->`

---

## 1. Changes Made

<!-- One sub-section per file changed. -->

### File: `<!-- relative/path/to/file -->`

**Step <!-- N --> — <!-- one-line description of the change -->**

- **Line(s)**: `<!-- N -->`
- **Rationale**: <!-- one sentence from the plan explaining why this line changed -->

**Before**:
```<!-- language -->
<!-- exact code that existed before the change — verbatim, do not paraphrase -->
```

**After**:
```<!-- language -->
<!-- exact code after the change was applied — verbatim, do not paraphrase -->
```

**Applied**: <!-- YES / NO — if NO, explain why -->  
**Confirmed** (re-read after save): <!-- YES / NO -->

<!-- Repeat the Step block for additional changes in the same file -->

<!-- Repeat the ### File section for each additional file changed -->

---

## 2. Test Results

### 2.1 Application startup

```bash
<!-- exact command used to start the app -->
```

**Startup output**: <!-- actual terminal output or "Server running on http://localhost:PORT" -->  
**Status**: <!-- PASS / FAIL — if FAIL, paste the error -->

### 2.2 Test cases

| # | Command / Action | Expected Status | Actual Status | Expected Body | Actual Body | Result |
|---|-----------------|-----------------|---------------|---------------|-------------|--------|
| 1 | <!-- exact command run --> | <!-- e.g. 200 --> | <!-- actual --> | <!-- expected body --> | <!-- actual body --> | <!-- PASS / FAIL --> |
| 2 | <!-- exact command run --> | <!-- e.g. 404 --> | <!-- actual --> | <!-- expected body --> | <!-- actual body --> | <!-- PASS / FAIL --> |
| 3 | <!-- exact command run --> | <!-- e.g. 404 --> | <!-- actual --> | <!-- expected body --> | <!-- actual body --> | <!-- PASS / FAIL --> |

### 2.3 Regression check

| Command / Action | Expected Status | Actual Status | Expected Body | Actual Body | Result |
|-----------------|-----------------|---------------|---------------|-------------|--------|
| <!-- exact command run --> | <!-- e.g. 200 --> | <!-- actual --> | <!-- expected body --> | <!-- actual body --> | <!-- PASS / FAIL --> |

---

## 3. Overall Status

**Status**: `<!-- PASS / FAIL -->`

<!-- If PASS: -->
<!-- All implementation steps applied as specified. All test cases passed. Regression check passed. No unintended changes made. -->

<!-- If FAIL: -->
<!-- Describe what failed, which step or test case, the actual vs. expected output, and what action was taken (e.g. handoff to Bug Planner). -->

---

## 4. Manual Verification Steps

<!-- Steps a human reviewer can follow to independently confirm the fix is correct. -->

1. Pull the branch / check out the commit containing the fix.
2. Run `<!-- start command -->` and confirm the server starts without errors.
3. Run the following commands and confirm expected responses:

```bash
<!-- exact curl / request commands a reviewer should run -->
```

4. Confirm no regressions in other endpoints:

```bash
<!-- exact regression test commands -->
```

5. Review the diff in `<!-- relative/path/to/changed/file -->` and confirm only the intended line(s) changed.

---

## 5. Snapshot Check (Pre-application)

<!-- Confirms plan "before" snippets matched the actual source before any edits. -->

| File | Line(s) | Plan "before" snippet found verbatim | Result |
|------|---------|--------------------------------------|--------|
| `<!-- file path -->` | `<!-- N -->` | <!-- YES / NO — if NO, paste what was actually found --> | <!-- PASS / FAIL --> |

---

## 6. References

- `context/bugs/<!-- BUG-ID -->/implementation-plan.md` — plan executed
- `context/bugs/<!-- BUG-ID -->/research/verified-research.md` — quality confirmed before start
- `<!-- relative/path/to/changed/file -->` — source file modified
