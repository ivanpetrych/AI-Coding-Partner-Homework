# Test Report: [BUG-ID]

**Bug ID**: `<!-- e.g. API-404 -->`
**Title**: `<!-- short bug title -->`
**Generator**: `Unit Test Generator Agent`
**Date**: `<!-- YYYY-MM-DD -->`
**Source**: `context/bugs/<!-- BUG-ID -->/fix-summary.md`
**Test Framework**: `<!-- framework name and version, e.g. Jest 29.7.0 -->`

---

## Test Files Generated

| File | Functions Tested |
|------|-----------------|
| `tests/<!-- BUG-ID -->/<file>.test.js` | `<!-- functionName -->` |

<!-- Repeat row for each additional test file -->

---

## FIRST Compliance

| Criterion | Status | Notes |
|-----------|--------|-------|
| Fast | <!-- ✅ / ❌ --> | <!-- e.g. All tests complete in <10 ms; no I/O --> |
| Independent | <!-- ✅ / ❌ --> | <!-- e.g. Each test uses isolated mock data --> |
| Repeatable | <!-- ✅ / ❌ --> | <!-- e.g. No time/network dependencies --> |
| Self-validating | <!-- ✅ / ❌ --> | <!-- e.g. All assertions use expect(); no console inspection --> |
| Timely | <!-- ✅ / ❌ --> | <!-- e.g. Tests cover only getUserById — untouched functions excluded --> |

---

## Test Cases

| # | Test Name | Input | Expected Output | Result |
|---|-----------|-------|-----------------|--------|
| 1 | <!-- e.g. returns user for valid numeric ID --> | <!-- e.g. id = 123 --> | <!-- e.g. { id: 123, name: "Alice Smith", ... } --> | <!-- ✅ PASS / ❌ FAIL --> |
| 2 | <!-- e.g. returns 404 for unknown ID --> | <!-- e.g. id = 999 --> | <!-- e.g. 404 + { error: "User not found" } --> | <!-- ✅ PASS / ❌ FAIL --> |
| 3 | <!-- edge case --> | <!-- input --> | <!-- expected --> | <!-- ✅ PASS / ❌ FAIL --> |

<!-- Repeat row for each additional test case -->

---

## Execution Output

```
<!-- Paste full stdout from the test runner here -->
```

**Summary**:

| Metric | Value |
|--------|-------|
| Tests passed | <!-- N --> |
| Tests failed | <!-- N --> |
| Total tests | <!-- N --> |
| Execution time | <!-- e.g. 0.42 s --> |

---

## Overall Status

**<!-- PASS / FAIL -->**

<!-- One sentence: whether all tests passed and the fix is verified by tests. -->

<!-- If FAIL: list each failing test and the exact assertion error. -->

---

## References

- `context/bugs/<!-- BUG-ID -->/fix-summary.md` — primary input
- `skills/unit-tests-FIRST.md` — FIRST compliance criteria applied
- `<!-- changed source file -->` — function(s) under test
- `tests/<!-- BUG-ID -->/<file>.test.js` — generated test file(s)
