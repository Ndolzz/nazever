import { HttpError, jsonResponse, requireCaller, serviceClient } from "../_shared/client.ts";

Deno.serve(async (req) => {
  try {
    const user = await requireCaller(req);
    const { invite_id, accept } = await req.json();
    if (!invite_id || typeof accept !== "boolean") {
      throw new HttpError(400, "invite_id dan accept wajib diisi");
    }

    const supabase = serviceClient();
    const { data: invite, error } = await supabase
      .from("pairing_invites")
      .select("*")
      .eq("id", invite_id)
      .single();

    if (error || !invite) {
      throw new HttpError(404, "Invite tidak ditemukan");
    }
    // Hanya inviter asli yang boleh confirm/reject — bukan sembarang user.
    if (invite.inviter_id !== user.id) {
      throw new HttpError(403, "Kamu tidak berhak konfirmasi invite ini");
    }
    if (invite.status !== "AWAITING_CONFIRMATION") {
      throw new HttpError(409, "Invite tidak sedang menunggu konfirmasi");
    }

    if (!accept) {
      await supabase
        .from("pairing_invites")
        .update({ status: "REVOKED" })
        .eq("id", invite.id);
      return jsonResponse({ status: "REJECTED" });
    }

    // rpc transaksional: buat row `pairs` + set invite ACTIVE dalam satu
    // transaksi database, supaya tidak mungkin ada state invite=ACTIVE
    // tanpa pair yang benar-benar terbentuk (atau sebaliknya).
    const { data, error: rpcError } = await supabase.rpc("finalize_pairing", {
      p_invite_id: invite.id,
      p_user_a: invite.inviter_id,
      p_user_b: invite.consumed_by,
    });

    if (rpcError) {
      throw new HttpError(500, `Gagal finalisasi pairing: ${rpcError.message}`);
    }

    return jsonResponse({ status: "ACTIVE", pair: data });
  } catch (err) {
    const status = err instanceof HttpError ? err.status : 500;
    return jsonResponse({ error: (err as Error).message }, status);
  }
});
