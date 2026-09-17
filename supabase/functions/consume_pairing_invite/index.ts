import {
  HttpError,
  jsonResponse,
  requireCaller,
  serviceClient,
  sha256Hex,
} from "../_shared/client.ts";

Deno.serve(async (req) => {
  try {
    const user = await requireCaller(req);
    const { token } = await req.json();
    if (!token || typeof token !== "string") {
      throw new HttpError(400, "Token wajib diisi");
    }

    const supabase = serviceClient();
    const tokenHash = await sha256Hex(token);

    const { data: invite, error } = await supabase
      .from("pairing_invites")
      .select("*")
      .eq("token_hash", tokenHash)
      .single();

    if (error || !invite) {
      // Pesan generik — tidak membocorkan apakah token "hampir benar".
      throw new HttpError(404, "Invite tidak ditemukan atau sudah tidak berlaku");
    }

    if (invite.status !== "PENDING") {
      throw new HttpError(409, "Invite sudah dipakai, dibatalkan, atau kedaluwarsa");
    }
    if (new Date(invite.expires_at).getTime() < Date.now()) {
      await supabase.from("pairing_invites").update({ status: "EXPIRED" }).eq("id", invite.id);
      throw new HttpError(410, "Invite sudah kedaluwarsa");
    }
    if (invite.inviter_id === user.id) {
      throw new HttpError(400, "Tidak bisa pairing dengan diri sendiri");
    }
    if (invite.attempt_count >= invite.max_attempts) {
      throw new HttpError(429, "Invite ini sudah terlalu sering dicoba");
    }

    // Bukan langsung ACTIVE — inviter (User A) harus approve eksplisit di
    // perangkatnya sendiri, supaya link yang bocor/screenshot tidak otomatis
    // mem-pairing siapa pun yang menemukannya.
    const { error: updateError } = await supabase
      .from("pairing_invites")
      .update({
        status: "AWAITING_CONFIRMATION",
        consumed_by: user.id,
        attempt_count: invite.attempt_count + 1,
      })
      .eq("id", invite.id)
      .eq("status", "PENDING"); // guard: hindari race condition dua consumer bersamaan

    if (updateError) {
      throw new HttpError(500, "Gagal memproses invite");
    }

    // TODO(phase realtime-notifications): trigger push notification ke User A
    // lewat FCM/notification channel supaya dia tahu ada permintaan pairing masuk,
    // selain realtime Postgres change yang sudah otomatis ke-emit ke client-nya.

    return jsonResponse({ status: "AWAITING_CONFIRMATION", inviteId: invite.id });
  } catch (err) {
    const status = err instanceof HttpError ? err.status : 500;
    return jsonResponse({ error: (err as Error).message }, status);
  }
});
