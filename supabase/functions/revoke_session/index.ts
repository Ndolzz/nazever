// NazeVer Edge Function: revoke_session (TASK-007, FR-01.6, SR-01)
//
// Device A logs out device B: marks one of the CALLER'S OWN sessions
// as revoked. The caller identity comes from the JWT (Authorization:
// Bearer <access token>) — deploy with JWT verification ENABLED.
//
// Guarantees:
//   * Unauthenticated callers are rejected (401).
//   * A caller can only revoke sessions whose user_id equals the JWT
//     user id (ownership checked server-side, 403 otherwise — the
//     response does not leak whether the target exists).
//   * Idempotent: revoking an already-revoked session returns 200.
//
// Secrets: SUPABASE_SERVICE_ROLE_KEY lives in the Supabase environment
// only; it is never logged or returned.

const SUPABASE_URL = Deno.env.get("SUPABASE_URL") ?? "";
const SERVICE_ROLE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
const ANON_KEY = Deno.env.get("SUPABASE_ANON_KEY") ?? "";

function json(body: Record<string, unknown>, status: number): Response {
  return new Response(JSON.stringify(body), {
    status: status,
    headers: { "Content-Type": "application/json" },
  });
}

const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

Deno.serve(async (req: Request): Promise<Response> => {
  if (req.method !== "POST") return json({ error: "method_not_allowed" }, 405);
  const authHeader = req.headers.get("Authorization") ?? "";
  if (!authHeader.startsWith("Bearer ")) return json({ error: "unauthorized" }, 401);
  if (!SUPABASE_URL || !SERVICE_ROLE_KEY || !ANON_KEY) {
    return json({ error: "server_misconfigured" }, 500);
  }

  const serviceHeaders = {
    "apikey": SERVICE_ROLE_KEY,
    "Authorization": "Bearer " + SERVICE_ROLE_KEY,
    "Content-Type": "application/json",
  };

  try {
    // 1. Identify the caller from the authenticated context (JWT).
    const userRes = await fetch(SUPABASE_URL + "/auth/v1/user", {
      headers: { "Authorization": authHeader, "apikey": ANON_KEY },
    });
    if (!userRes.ok) return json({ error: "unauthorized" }, 401);
    const user = await userRes.json();
    const userId: string = user?.id ?? "";
    if (!userId) return json({ error: "unauthorized" }, 401);

    // 2. Validate input.
    const body = await req.json().catch(() => null) as {
      session_id?: string;
    } | null;
    if (!body || !body.session_id || !UUID_RE.test(body.session_id)) {
      return json({ error: "invalid_request" }, 400);
    }

    // 3. Ownership check in one query (no existence leak to other users).
    const q = await fetch(
      SUPABASE_URL + "/rest/v1/sessions?id=eq." + body.session_id +
        "&user_id=eq." + userId + "&select=id",
      { headers: serviceHeaders },
    );
    const rows = await q.json();
    if (!Array.isArray(rows) || rows.length === 0) {
      return json({ error: "not_found_or_forbidden" }, 403);
    }

    // 4. Idempotent revoke: only touches rows that are still active.
    await fetch(
      SUPABASE_URL + "/rest/v1/sessions?id=eq." + body.session_id +
        "&revoked_at=is.null",
      {
        method: "PATCH",
        headers: serviceHeaders,
        body: JSON.stringify({ revoked_at: new Date().toISOString() }),
      },
    );

    return json({ revoked: true }, 200);
  } catch (_e) {
    // Never log tokens or request bodies.
    return json({ error: "internal_error" }, 500);
  }
});
