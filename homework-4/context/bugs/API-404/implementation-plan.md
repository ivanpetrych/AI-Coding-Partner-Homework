# Implementation Plan: API-404

**Bug ID**: `API-404`  
**Title**: `GET /api/users/:id returns 404 for valid user IDs`  
**Planner**: `Bug Planner Agent`  
**Date**: `2026-03-10`  
**Research Quality**: `SILVER — 5/5 refs valid; root cause correct; one line-number discrepancy in Finding 2 (stated 20, actual 19)`

---

## 1. Summary

`GET /api/users/:id` always returns 404 because `getUserById` compares `req.params.id` — which Express delivers as a string (e.g. `"123"`) — against numeric `id` fields in the `users` array (e.g. `123`) using strict equality (`===`). Since `"123" === 123` is always `false`, `Array.prototype.find()` never matches any user and the controller unconditionally falls into the 404 branch. The fix is to parse `req.params.id` to an integer with `parseInt(..., 10)` at the point of assignment, so the comparison becomes `123 === 123` and resolves correctly.

---

## 2. Pre-conditions

- [x] `context/bugs/API-404/research/verified-research.md` exists and quality is SILVER (≥ BRONZE)
- [ ] Developer has read-write access to the repository
- [ ] Local environment runs without errors (`npm start` prints `Demo API server running on http://localhost:3000`)

---

## 3. Implementation Steps

### File: `demo-bug-fix/src/controllers/userController.js`

**Step 1 — Parse `req.params.id` to an integer before use in the `find()` comparison**

- **Line(s)**: `19` (inside `getUserById`, immediately after the function signature)
- **Rationale**: `req.params.id` is always a string in Express; converting it to an integer at the point of assignment makes the subsequent strict equality check (`u.id === userId`) compare two numbers, resolving the type mismatch that is the root cause of the bug.

**Before**:
```js
  const userId = req.params.id;
```

**After**:
```js
  const userId = parseInt(req.params.id, 10);
```

> No other files require changes. The route definition in `src/routes/users.js` and the server entry point `server.js` are correct and unaffected.

---

## 4. Verification Procedure

### 4.1 Start the application

```bash
cd demo-bug-fix
npm start
```

Expected: `Demo API server running on http://localhost:3000`

### 4.2 Test cases

| # | Command / Action | Expected Status | Expected Body | Pass Condition |
|---|-----------------|-----------------|---------------|----------------|
| 1 | `curl -s http://localhost:3000/api/users/123` | `200` | `{"id":123,"name":"Alice Smith","email":"alice@example.com"}` | Status 200 and body matches exactly |
| 2 | `curl -s http://localhost:3000/api/users/456` | `200` | `{"id":456,"name":"Bob Johnson","email":"bob@example.com"}` | Status 200 and body matches exactly |
| 3 | `curl -s http://localhost:3000/api/users/999` | `404` | `{"error":"User not found"}` | Status 404 and body matches exactly |
| 4 | `curl -s http://localhost:3000/api/users/abc` | `404` | `{"error":"User not found"}` | Status 404 and body matches exactly (`parseInt("abc",10)` → `NaN`; `NaN===NaN` is `false`; falls through to 404 correctly) |

### 4.3 Regression check

Verify existing working behaviour is unaffected:

| Command / Action | Expected Status | Expected Body |
|-----------------|-----------------|---------------|
| `curl -s http://localhost:3000/api/users` | `200` | JSON array containing all three users (Alice, Bob, Charlie) |
| `curl -s http://localhost:3000/health` | `200` | `{"status":"ok","message":"Demo API is running"}` |

---

## 5. Rollback Plan

If the fix causes regressions, revert using the following commands:

```bash
# Git revert (preferred)
git checkout HEAD -- demo-bug-fix/src/controllers/userController.js
```

**Manual fallback** (if git is unavailable):

| File | Line(s) | Restore to |
|------|---------|-----------|
| `demo-bug-fix/src/controllers/userController.js` | `19` | `  const userId = req.params.id;` |

---

## 6. Definition of Done

- [ ] Implementation step in Section 3 applied exactly as written
- [ ] All test cases in Section 4.2 return PASS
- [ ] Regression checks in Section 4.3 return PASS
- [ ] No new errors introduced in unrelated endpoints or modules
- [ ] Code committed with message referencing bug ID: `fix: resolve API-404 — parse req.params.id to integer in getUserById`

---

## 7. References

- `context/bugs/API-404/research/verified-research.md` — quality-gated research input (SILVER)
- `context/bugs/API-404/research/codebase-research.md` — full findings, fix surface, risks, and test cases
- `demo-bug-fix/src/controllers/userController.js` — sole file requiring change; contains the type-mismatch at line 19
- `demo-bug-fix/src/routes/users.js` — confirmed correct; no change required
- `demo-bug-fix/server.js` — confirmed correct; no change required
