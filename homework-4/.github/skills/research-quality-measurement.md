---
name: research-quality-measurement
description: Use by verifying codebase bug research quality before implementation. Use for research verification, claim accuracy, file:line reference checking, RQ scoring, codebase-research review.
---

## Quality Levels

| Level | Label | Minimum Criteria |
|-------|-------|-----------------|
| 4 | **GOLD** | All refs valid · All snippets match · Root cause stated · 0 discrepancies |
| 3 | **SILVER** | ≥80% refs valid · Snippets match (whitespace/comment drift allowed) · Root cause stated · ≤1 discrepancy |
| 2 | **BRONZE** | ≥50% refs valid · Root cause plausible · ≤3 discrepancies |
| 1 | **FAILED** | <50% refs valid OR root cause absent/wrong OR critical snippet mismatch |

---

## Scoring Rules (apply in order, stop at first failure)

1. **Root cause missing or factually wrong** → **FAILED** immediately.
2. Count valid references: `valid / total`.
3. Any file path that does not exist → invalid ref.
4. Line number off by >3 lines → invalid ref.
5. Snippet content differs (beyond whitespace) → deduct 1 level from result.
6. Apply the table above; take the **lowest matching level**.

---

## Required Output Format

The verifier MUST write the quality result as exactly one line to `verified-research.md`:

```
Research Quality: <LEVEL> — <one-sentence reason>
```

**Examples**:
```
Research Quality: GOLD — All 3 references verified; snippets match exactly; root cause correct.
Research Quality: SILVER — 2/2 refs valid; one line number off by 2 lines.
Research Quality: BRONZE — 1/3 refs valid; root cause identified but evidence thin.
Research Quality: FAILED — Root cause absent.
```
