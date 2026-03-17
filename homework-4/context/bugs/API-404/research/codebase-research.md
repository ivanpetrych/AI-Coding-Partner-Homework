# Codebase Research: API-404

**Bug ID**: `API-404`  
**Title**: `GET /api/users/:id returns 404 for valid user IDs`  
**Researcher**: `Bug Researcher Agent`  
**Date**: `2026-03-10`

---

## 1. Root Cause Analysis

In `demo-bug-fix/src/controllers/userController.js` at line 23, the `users.find()` call uses strict equality (`===`) to compare `req.params.id` — which Express always delivers as a string (e.g. `"123"`) — against the numeric `id` fields stored in the in-memory `users` array (e.g. `123`). Because JavaScript's strict equality operator checks both value and type simultaneously, the expression `"123" === 123` evaluates to `false` for every user, so `find()` never returns a match regardless of which valid ID is requested. As a result, the controller unconditionally falls through to the 404 branch, returning `{ "error": "User not found" }` for every call to `GET /api/users/:id`.

---

## 2. Code Findings

### Finding 1

- **File**: `demo-bug-fix/src/controllers/userController.js`
- **Line**: `23`
- **Snippet** (exact, copied from source):
  ```js
  const user = users.find(u => u.id === userId);
  ```
- **Why it matters**: This is the exact comparison that fails — strict equality between the string `userId` and the numeric `u.id` always returns `false`, so no user is ever matched.

### Finding 2

- **File**: `demo-bug-fix/src/controllers/userController.js`
- **Line**: `20`
- **Snippet** (exact, copied from source):
  ```js
  const userId = req.params.id;
  ```
- **Why it matters**: `req.params.id` is always a string in Express; no type conversion is applied here, making `userId` a string that will never strictly equal a numeric ID.

### Finding 3

- **File**: `demo-bug-fix/src/controllers/userController.js`
- **Line**: `8`
- **Snippet** (exact, copied from source):
  ```js
  const users = [
    { id: 123, name: 'Alice Smith', email: 'alice@example.com' },
  ```
- **Why it matters**: The in-memory data store defines `id` as a numeric literal, establishing the type mismatch with the string parameter that arrives from the route.

### Finding 4

- **File**: `demo-bug-fix/src/controllers/userController.js`
- **Line**: `25`
- **Snippet** (exact, copied from source):
  ```js
  if (!user) {
    return res.status(404).json({ error: 'User not found' });
  }
  ```
- **Why it matters**: Because `find()` always returns `undefined` (due to Finding 1), this branch is always executed, producing the erroneous 404 for every valid user ID.

### Finding 5

- **File**: `demo-bug-fix/src/routes/users.js`
- **Line**: `14`
- **Snippet** (exact, copied from source):
  ```js
  router.get('/api/users/:id', userController.getUserById);
  ```
- **Why it matters**: The route correctly maps `GET /api/users/:id` to `getUserById`; the fault lies entirely within the controller, not the routing layer.

---

## 3. Proposed Fix Surface

| File | Line(s) | Change needed (one line) |
|------|---------|--------------------------|
| `demo-bug-fix/src/controllers/userController.js` | 20 | Parse `req.params.id` to an integer before assignment (e.g. `parseInt(req.params.id, 10)`) |

---

## 4. Risks and Edge Cases

- **Risk**: If `:id` is a non-numeric string (e.g. `/api/users/abc`), `parseInt` returns `NaN`; `NaN === NaN` is `false` in JavaScript, so `find()` will still return `undefined` and correctly yield a 404.  
  **Scope**: Input validation — non-numeric IDs already behave correctly after the fix; no additional guard is strictly required, though an explicit validation step would be safer.

- **Risk**: If the `users` data source is later migrated to string-keyed IDs (e.g. from a MongoDB `_id` field), using `parseInt` will re-introduce a type mismatch in the opposite direction.  
  **Scope**: Future data model changes affecting `userController.js` and any other controller that follows the same pattern.

---

## 5. Suggested Test Cases

| # | Input | Expected Result |
|---|-------|-----------------|
| 1 | `GET /api/users/123` (valid, existing user) | `200 OK` with `{ "id": 123, "name": "Alice Smith", "email": "alice@example.com" }` |
| 2 | `GET /api/users/999` (valid format, ID does not exist) | `404 Not Found` with `{ "error": "User not found" }` |
| 3 | `GET /api/users/abc` (non-numeric ID) | `404 Not Found` with `{ "error": "User not found" }` |

---

## 6. References

- `demo-bug-fix/src/controllers/userController.js` — contains `getUserById` with the strict-equality type-mismatch bug and the in-memory `users` data store
- `demo-bug-fix/src/routes/users.js` — defines the `/api/users/:id` route and wires it to `getUserById`
- `demo-bug-fix/server.js` — application entry point; mounts the user routes and starts the server
- `demo-bug-fix/bugs/API-404/bug-context.md` — original bug report describing the symptoms and affected endpoint
