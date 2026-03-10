# Implementation Plan: [BUG-ID]

**Bug ID**: `<!-- e.g. API-404 -->`  
**Title**: `<!-- short bug title -->`  
**Planner**: `<!-- agent or human name -->`  
**Date**: `<!-- YYYY-MM-DD -->`  
**Research Quality**: `<!-- GOLD / SILVER / BRONZE — copied from verified-research.md -->`

---

## 1. Summary

<!-- REQUIRED. Two to four sentences.
     State what is broken, what the fix changes, and why that resolves the root cause.
     No speculation. Trace directly to the verified root cause. -->

---

## 2. Pre-conditions

<!-- Everything that must be true BEFORE a developer starts. -->

- [ ] `context/bugs/<ID>/research/verified-research.md` exists and quality ≥ BRONZE
- [ ] Developer has read-write access to the repository
- [ ] Local environment runs the app without errors (`npm start` / equivalent)
- [ ] <!-- Add any project-specific pre-conditions here -->

---

## 3. Implementation Steps

<!-- One numbered group per file that changes.
     Each step must be atomic and self-contained. -->

### File: `<!-- relative/path/to/file -->`

**Step 1 — <!-- one-line description of the change -->**

- **Line(s)**: `<!-- N -->` (<!-- optional: brief location hint, e.g. "inside getUserById function" -->)
- **Rationale**: <!-- one sentence — why this line must change per the root cause -->

**Before**:
```<!-- language -->
<!-- paste the exact current lines verbatim — do not paraphrase -->
```

**After**:
```<!-- language -->
<!-- paste the exact replacement lines verbatim — do not paraphrase -->
```

<!-- Repeat Step N blocks for additional changes in the same file -->

<!-- Repeat the ### File section for additional files -->

---

## 4. Verification Procedure

<!-- For each test case, provide the exact command and expected output.
     A developer should be able to copy-paste and run without modification. -->

### 4.1 Start the application

```bash
<!-- e.g. npm start -->
```

Expected: <!-- e.g. "Server running on http://localhost:3000" -->

### 4.2 Test cases

| # | Command / Action | Expected Status | Expected Body | Pass Condition |
|---|-----------------|-----------------|---------------|----------------|
| 1 | <!-- e.g. curl http://localhost:3000/api/users/123 --> | <!-- e.g. 200 --> | <!-- e.g. {"id":123,...} --> | <!-- response matches exactly --> |
| 2 | <!-- negative case --> | <!-- e.g. 404 --> | <!-- e.g. {"error":"User not found"} --> | <!-- response matches exactly --> |
| 3 | <!-- edge case --> | <!-- e.g. 404 --> | <!-- e.g. {"error":"User not found"} --> | <!-- response matches exactly --> |

### 4.3 Regression check

Verify existing working behaviour is unaffected:

| Command / Action | Expected Status | Expected Body |
|-----------------|-----------------|---------------|
| <!-- e.g. curl http://localhost:3000/api/users --> | <!-- e.g. 200 --> | <!-- e.g. array of all users --> |

---

## 5. Rollback Plan

If the fix causes regressions, revert using the following commands. Run **all** of them.

```bash
# Git revert (preferred)
<!-- e.g. git checkout HEAD -- src/controllers/userController.js -->
```

**Manual fallback** (if git is unavailable):

| File | Line(s) | Restore to |
|------|---------|-----------|
| `<!-- relative/path/to/file -->` | `<!-- N -->` | `<!-- exact original code -->` |

---

## 6. Definition of Done

- [ ] All implementation steps in Section 3 applied exactly as written
- [ ] All test cases in Section 4.2 return PASS
- [ ] Regression check in Section 4.3 returns PASS
- [ ] No new errors introduced in unrelated endpoints or modules
- [ ] Code committed with message referencing bug ID: `fix: resolve <BUG-ID> — <!-- one-line description -->`

---

## 7. References

<!-- Files read during planning. One entry per file. -->

- `context/bugs/<ID>/research/verified-research.md` — quality-gated research input
- `context/bugs/<ID>/research/codebase-research.md` — full findings, fix surface, risks, test cases
- `<!-- relative/path/to/changed/file -->` — <!-- one-line note on what was read and why -->
