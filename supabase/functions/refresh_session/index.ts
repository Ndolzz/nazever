// NazeVer Edge Function: refresh_session (TASK-007, SR-01)
//
// Server-side refresh CHOKE POINT: enforces session revocation before
// minting new tokens. If the refresh token's hash matches no ACTIVE
// (non-revoked) session row, the request is refused with 401 and the
// client clears its local SecureSessionStore.
//
// Auth: deploy with JWT verification DISABLED (--no-verify-jwt): by
// design the access token is already expired when refreshing, so
// authentication here is POSSESSION of the refresh token whose hash
// matches an ACTIVE session row.
//
// Secrets: SUPABASE_SERVICE_ROLE_KEY lives in the Supabase environment
// only. Refresh tokens are hashed before storage and never logged.

const SUPABASE_URL = Deno.env.get("SUPABASE_URL") ?? "";
const SERVICE_ROLE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
const ANON_KEY = Deno.env.get("SUPABASE_ANON_KEY") ?? "";

function json(body: Record<string, unknown>, status: number): Response {
  return new Response(JSON.stringify(body), {
    status: status,
    headers: { "Content-Type": "application/json" },
  });
}

async function sha256Hex(value: string): Promise<string> {
  const data = new TextEncoder().encode(value);
  const digest = await crypto.subtle.digest("SHA-256", data);
  const bytes = new Uint8Array(digest);
  let out = "";
  for (const b of bytes) {
    out += b.toString(16).padStart(2, "0");
  }
  return out;
}

Deno.serve(async (req: Request): Promise<Response> => {
  if (req.method !== "POST") return json({ error: "method_not_allowed" }, 405);
  if (!SUPABASE_URL || !SERVICE_ROLE_KEY || !ANON_KEY) {
    return json({ error: "server_misconfigured" }, 500);
  }

  const serviceHeaders = {
    "apikey": SERVICE_ROLE_KEY,
    "Authorization": "Bearer " + SERVICE_ROLE_KEY,
    "Content-Type": "application/json",
  };

  try {
    const body = await req.json().catch(() => null) as {
      refresh_token?: string;
    } | null;
    if (!body || !body.refresh_token) return json({ error: "invalid_request" }, 400);

    const refreshHash = await sha256Hex(body.refresh_token);

    // 1. Only ACTIVE (non-revoked) sessions may refresh.
    const q = await fetch(
      SUPABASE_URL +
        "/rest/v1/sessions?refresh_token_hash=eq." + refreshHash +
        "&revoked_at=is.null&select=id,user_id",
      { headers: serviceHeaders },
    );
    const rows = await q.json();
    if (!Array.isArray(rows) || rows.length === 0) {
      // Revoked, unknown, or already-rotated token: refuse.
      return json({ error: "session_revoked" }, 401);
    }
    const sessionId: string = rows[0].id;

    // 2. Forward to the official Supabase Auth refresh grant.
    const tokenRes = await fetch(
      SUPABASE_URL + "/auth/v1/token?grant_type=refresh_token",
      {
        method: "POST",
        headers: { "apikey": ANON_KEY, "Content-Type": "application/json" },
        body: JSON.stringify({ refresh_token: body.refresh_token }),
      },
    );
    if (!tokenRes.ok) {
      // Supabase refused (expired/reused token): treat as revoked so the
      // client drops the local session.
      return json({ error: "refresh_failed" }, 401);
    }
    const tokens = await tokenRes.json();

    // 3. Rotate the stored hash to the new refresh token (Supabase
    //    rotates refresh tokens on every refresh).
    const newRefresh: string | undefined = tokens?.refresh_token;
    if (newRefresh) {
      const newHash = await sha256Hex(newRefresh);
      await fetch(SUPABASE_URL + "/rest/v1/sessions?id=eq." + sessionId, {
        method: "PATCH",
        headers: serviceHeaders,
        body: JSON.stringify({ refresh_token_hash: newHash }),
      });
    }

    return new Response(JSON.stringify(tokens), {
      status: 200,
      headers: { "Content-Type": "application/json" },
    });
  } catch (_e) {
    // Never log tokens or request bodies.
    return json({ error: "internal_error" }, 500);
  }
});
