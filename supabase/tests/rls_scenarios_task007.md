# NazeVer — TASK-007 Devices & Sessions / Revocation Test Scenarios

Reference: requirements.md FR-01.6/FR-01.7, SR-01; design.md §3, §5.
Companion to `rls_scenarios.md` (TASK-003), same structure. SQL
execution of these scenarios belongs to TASK-011 / CI once a Supabase
CLI environment is configured (see rls_scenarios.md "How to run").

Setup: user A signs in on device A and device B (two sessions); user C
is a different user with their own device/session.

| # | Scenario | Actor | Expected | Covered by |
|---|---|---|---|---|
| 19 | A lists own devices/sessions | A | **allowed** (own rows only) | devices_select / sessions_select (0001) |
| 20 | A reads C's devices/sessions | A | **denied** (0 rows) | devices_select / sessions_select (SR-02.2) |
| 21 | A inserts a session row directly via REST | A | **denied** — no insert policy; sessions are written only by edge functions | (no insert policy, 0002 comment) |
| 22 | A updates/revoke a session row directly via REST | A | **denied** — no update policy | (no update policy) |
| 23 | A deletes session rows directly | A | **denied** — no delete policy | (no delete policy) |
| 24 | Unauthenticated (anon) calls revoke_session | anon | **401** | revoke_session JWT check |
| 25 | A calls revoke_session targeting C's session | A | **403 not_found_or_forbidden** | revoke_session ownership check |
| 26 | A revokes device B, then B refreshes | B | **401 session_revoked**; B's client clears its SecureSessionStore and returns to Login | refresh_session choke point + revoked_at |
| 27 | A revokes the same session twice | A | **200 both times** (idempotent) | revoke_session (revoked_at is null filter) |
| 28 | A revokes a missing/unknown session id | A | **403** (no existence leak) | revoke_session |
| 29 | B restores after A revoked B's session | B | restore returns null locally, store cleared | client restoreSession revocation check |
| 30 | A signs out on device A; device A's own session row | A | row marked revoked + Supabase refresh token revoked + local store cleared | repository.signOut + /auth/v1/logout |
| 31 | Client sends a user_id pretending to be another user to register_session | any | identity is taken from the JWT, not the body — no forgery possible | register_session |
| 32 | Duplicate active refresh_token_hash insert (constraint) | service | **rejected** by unique partial index | sessions_refresh_token_hash_active_key (0002) |

## Revocation model — documented limitation

Supabase Auth provides no per-device admin token revocation API (only
global signOut revoking ALL of a user's refresh tokens). NazeVer
therefore enforces revocation at the refresh choke point
(`refresh_session` refuses revoked sessions) and at client restore.
The revoked device's existing access token remains valid only until
its short-lived expiry (design §5). A determined client bypassing the
app's API surface and calling Supabase Auth directly could refresh
until that choke point is the only refresh path used by the app;
documented per TASK-007 instructions as the safest architecture in
scope, without weakening existing RLS or the authorization model.
