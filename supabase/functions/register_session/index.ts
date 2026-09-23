// NazeVer Edge Function: register_session (TASK-007, FR-01.6, SR-01)
//
// Registers the calling device and stores a SHA-256 HASH of the refresh
// token so sessions can be revoked from another device.
//
// Auth: requires a valid caller JWT (Authorization: Bearer <access
// token>); deploy with JWT verification ENABLED (default). The caller
// identity is resolved from the JWT via /auth/v1/user — client-supplied
// user ids are never trusted.
//
// Secrets: SUPABASE_SERVICE_ROLE_KEY comes from the Supabase
// environment only. It is never logged or returned. The refresh token
// is hashed before storage and never logged.

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
      device_name?: string;
      platform?: string;
      refresh_token?: string;
      device_id?: string;
    } | null;
    if (!body || !body.refresh_token || !body.device_name) {
      return json({ error: "invalid_request" }, 400);
    }
    if (body.device_name.length > 64) return json({ error: "invalid_request" }, 400);
    if (body.device_id && !UUID_RE.test(body.device_id)) {
      return json({ error: "invalid_request" }, 400);
    }

    const refreshHash = await sha256Hex(body.refresh_token);

    // 3. Upsert the device — only if it belongs to the caller.
    let deviceId: string | null = body.device_id ?? null;
    if (deviceId) {
      const upd = await fetch(
        SUPABASE_URL + "/rest/v1/devices?id=eq." + deviceId + "&user_id=eq." + userId,
        {
          method: "PATCH",
          headers: { ...serviceHeaders, "Prefer": "return=representation" },
          body: JSON.stringify({
            device_name: body.device_name,
            last_active_at: new Date().toISOString(),
          }),
        },
      );
      const rows = await upd.json();
      if (!Array.isArray(rows) || rows.length === 0) deviceId = null;
    }
    if (!deviceId) {
      const ins = await fetch(SUPABASE_URL + "/rest/v1/devices", {
        method: "POST",
        headers: { ...serviceHeaders, "Prefer": "return=representation" },
        body: JSON.stringify({
          user_id: userId,
          device_name: body.device_name,
          platform: body.platform ?? "android",
        }),
      });
      const rows = await ins.json();
      if (!Array.isArray(rows) || rows.length === 0) {
        return json({ error: "device_insert_failed" }, 500);
      }
      deviceId = rows[0].id;
    }

    // 4. Create the session row (service role bypasses RLS by design).
    const sessIns = await fetch(SUPABASE_URL + "/rest/v1/sessions", {
      method: "POST",
      headers: { ...serviceHeaders, "Prefer": "return=representation" },
      body: JSON.stringify({
        user_id: userId,
        device_id: deviceId,
        refresh_token_hash: refreshHash,
      }),
    });
    const sessRows = await sessIns.json();
    if (!Array.isArray(sessRows) || sessRows.length === 0) {
      return json({ error: "session_insert_failed" }, 500);
    }

    return json({ session_id: sessRows[0].id, device_id: deviceId }, 200);
  } catch (_e) {
    // Never log tokens or request bodies.
    return json({ error: "internal_error" }, 500);
  }
});
