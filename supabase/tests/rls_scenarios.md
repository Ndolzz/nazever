# NazeVer — Database Security Test Scenarios (TASK-003)

Reference: requirements.md §3 (SR-02), design.md §15 (Testing Design),
tasks.md TASK-011 (full RLS authorization test suite — SQL execution of these
scenarios belongs to that task, or to CI once a Supabase CLI environment is
configured).

These scenarios document the REQUIRED authorization behavior of the schema in
`supabase/migrations/0001_initial_schema.sql`. They must all pass before any
feature work on top of the database is considered done.

Setup: users A and B are paired into couple X; user C is in couple Y
(or unpaired). All requests run with the `authenticated` role and Supabase JWT
of the indicated user. `service_role` requests bypass RLS and are used only by
edge functions.

## Required scenarios

| # | Scenario | Actor | Expected | Covered policy |
|---|---|---|---|---|
| 1 | A reads couple X | A | **allowed** | couples_select |
| 2 | C reads couple X (knows its UUID) | C | **denied** (0 rows) — IDOR blocked (SR-02.2) | couples_select |
| 3 | A reads conversation of couple X | A | **allowed** | conversations_select |
| 4 | C reads conversation of couple X | C | **denied** | conversations_select |
| 5 | A inserts message as A in couple X | A | **allowed** | messages_insert |
| 6 | A inserts message with sender_id = B | A | **denied** — no sender forgery (SR-02.2) | messages_insert |
| 7 | A inserts message with conversation of couple Y | A | **denied** — cross-couple injection blocked | messages_insert |
| 8 | A reads messages of couple Y | A | **denied** | messages_select |
| 9 | Unauthenticated (anon) reads any private table | anon | **denied** (all policies are `to authenticated`) | all |
| 10 | C reads pairing_request between A and B | C | **denied** | pairing_requests_select |
| 11 | A inserts into couple_members for couple Y | A | **denied** — membership writes are edge-function/service_role only | (no insert policy) |
| 12 | Third user joins couple X directly | any authenticated | **denied** by RLS + `enforce_max_two_couple_members` trigger (FR-02.8) | (no insert policy) |
| 13 | A updates B's privacy_settings | A | **denied** | privacy_settings_update |
| 14 | A updates profile of B (display name) | A | **denied** | profiles_update |
| 15 | A reads profile of B (partner) | A | **allowed** | profiles_select (is_couple_partner) |
| 16 | A hard-deletes a message | A | **denied** — no delete policy; deletes are tombstones (design §6) | (no delete policy) |
| 17 | Duplicate idempotency key insert (retry) | A | **rejected** by unique(couple_id, idempotency_key) (requirements §5) | constraint |
| 18 | A reads B's sessions/devices | A | **denied** | sessions/devices policies |

## How to run (when a test environment exists)

With Supabase CLI:

```bash
supabase start
supabase db reset          # applies migrations 0001.. deterministically
supabase test db           # pgTAP tests (to be added in TASK-011)
```

Manual psql check (impersonation):

```sql
select set_config('role', 'authenticated', true);
select set_config('request.jwt.claims', claims_of_user_a, true);
-- then run each scenario's query and assert row counts / errors
```

No secret is required for any of the above; they run against a local
ephemeral database only.
