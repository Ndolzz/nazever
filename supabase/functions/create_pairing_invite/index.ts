import {
  generatePairingToken,
  HttpError,
  jsonResponse,
  requireCaller,
  serviceClient,
  sha256Hex,
} from "../_shared/client.ts";

const INVITE_TTL_MINUTES = 10;
const MAX_PENDING_INVITES_PER_USER = 3; // anti-spam sederhana

Deno.serve(async (req) => {
  try {
    const user = await requireCaller(req);
    const supabase = serviceClient();

    // Kalau user sudah punya pair aktif, tolak — satu user cuma boleh satu pair space.
    const { data: existingPair } = await supabase
      .from("pairs")
      .select("id")
      .eq("status", "ACTIVE")
      .or(`user_a_id.eq.${user.id},user_b_id.eq.${user.id}`)
      .maybeSingle();
    if (existingPair) {
      throw new HttpError(409, "Kamu sudah punya private pair space aktif");
    }

    const { count } = await supabase
      .from("pairing_invites")
      .select("id", { count: "exact", head: true })
      .eq("inviter_id", user.id)
      .eq("status", "PENDING");
    if ((count ?? 0) >= MAX_PENDING_INVITES_PER_USER) {
      throw new HttpError(429, "Terlalu banyak invite pending, revoke salah satu dulu");
    }

    const rawToken = generatePairingToken();
    const tokenHash = await sha256Hex(rawToken);
    const expiresAt = new Date(Date.now() + INVITE_TTL_MINUTES * 60_000).toISOString();

    const { data: invite, error } = await supabase
      .from("pairing_invites")
      .insert({
        inviter_id: user.id,
        token_hash: tokenHash,
        status: "PENDING",
        expires_at: expiresAt,
      })
      .select("id, inviter_id, status, expires_at, created_at")
      .single();

    if (error || !invite) {
      throw new HttpError(500, "Gagal membuat invite");
    }

    return jsonResponse({
      invite,
      raw_token: rawToken,
      // Deep link dibuka oleh app pasangan; token TIDAK dikirim lewat backend
      // aplikasi ini — pengguna membagikannya sendiri (QR / share sheet OS).
      deep_link: `nazever://pair?token=${rawToken}`,
    });
  } catch (err) {
    const status = err instanceof HttpError ? err.status : 500;
    return jsonResponse({ error: (err as Error).message }, status);
  }
});
