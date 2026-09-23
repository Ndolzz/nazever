-- ============================================================================
-- NazeVer — TASK-007: Devices & Sessions session management (FR-01.6, SR-01)
--
-- The devices & sessions tables were created in 0001_initial_schema.sql
-- (design §3) together with their RLS policies:
--   * devices:  select/insert/update/delete — strictly user_id = auth.uid()
--   * sessions: SELECT only — user_id = auth.uid()
--
-- This migration deliberately does NOT add any client
-- INSERT/UPDATE/DELETE policy on public.sessions: all session writes go
-- through the Edge Functions (register_session / refresh_session /
-- revoke_session), which use the service-role key. A client can
-- therefore never forge, alter, or revoke a session row — not even its
-- own — except through the audited server-side paths.
--
-- Revocation model (documented per TASK-007):
--   * revoke_session marks a session row revoked_at = now(). It is
--     idempotent and only works on the caller's own sessions.
--   * refresh_session is the server-side refresh choke point: it
--     refuses to mint new tokens for a revoked (or missing) session
--     row, so a revoked device cannot refresh and its client clears
--     the local SecureSessionStore on the next restore/refresh.
--   * Supabase Auth has no per-device admin token revocation; the
--     revoked device's access token stays valid only until its
--     short-lived expiry (design §5: access tokens are short-lived).
-- ============================================================================

create index if not exists sessions_device_id_idx
  on public.sessions (device_id);

-- One ACTIVE session per refresh token: refresh tokens are unique and
-- rotated on every refresh (see refresh_session edge function).
create unique index if not exists sessions_refresh_token_hash_active_key
  on public.sessions (refresh_token_hash)
  where revoked_at is null;

create index if not exists sessions_user_active_idx
  on public.sessions (user_id, revoked_at);

comment on table public.sessions is
  'Per-device auth sessions (TASK-007). refresh_token_hash only — tokens are never stored. Revocation: revoked_at set by the revoke_session edge function; enforced at refresh by the refresh_session edge function.';
