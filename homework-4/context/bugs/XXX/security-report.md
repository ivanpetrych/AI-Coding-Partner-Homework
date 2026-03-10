# Security Report: [BUG-ID]

**Bug ID**: `<!-- e.g. API-404 -->`
**Title**: `<!-- short bug title -->`
**Reviewer**: `Security Verifier Agent`
**Date**: `<!-- YYYY-MM-DD -->`
**Source**: `context/bugs/<!-- BUG-ID -->/fix-summary.md`

---

## Scope

Files reviewed (from fix-summary.md):

- `<!-- relative/path/to/file.js -->` — <!-- one-line note on what changed -->

<!-- Repeat for each additional file reviewed -->

---

## Findings

<!-- One block per finding. If none: write "No security findings identified." -->

### Finding 1 — <!-- short title -->

| Field | Value |
|-------|-------|
| **Severity** | <!-- CRITICAL / HIGH / MEDIUM / LOW / INFO --> |
| **File** | `<!-- relative/path/to/file.js -->` |
| **Line(s)** | `<!-- N -->` |
| **Category** | <!-- e.g. Missing input validation --> |

**Description**: <!-- one to two sentences explaining the risk exactly -->

**Snippet**:
```<!-- language -->
<!-- exact line(s) from source that exhibit the issue -->
```

**Remediation**: <!-- concrete, actionable fix in plain text — no code required -->

<!-- Repeat Finding block for Finding 2 … Finding N -->

---

## Summary

| Severity | Count |
|----------|-------|
| CRITICAL | <!-- N --> |
| HIGH | <!-- N --> |
| MEDIUM | <!-- N --> |
| LOW | <!-- N --> |
| INFO | <!-- N --> |
| **Total** | <!-- N --> |

**Overall Risk**: <!-- CRITICAL / HIGH / MEDIUM / LOW / CLEAR -->

<!-- One paragraph: overall security posture of the change; whether it is safe to ship; priority order for addressing findings. -->

---

## References

- `context/bugs/<!-- BUG-ID -->/fix-summary.md` — primary input
- `<!-- changed source file -->` — reviewed in full

<!-- Repeat References entry for each additional file reviewed -->
