import { HttpError, jsonResponse, requireCaller, serviceClient } from "../_shared/client.ts";

// Phase 1: implementasi sederhana — unpair langsung oleh salah satu pihak,
// karena tabel `pair_unpair_requests` (untuk alur "kedua user harus
// konfirmasi") baru dibuat di phase Privacy Center. Didokumentasikan di
// docs/ARCHITECTURE.md sebagai known-limitation sementara, BUKAN dibuat
// pura-pura sudah lengkap.
Deno.serve(async (req) => {
  try {
    const user = await requireCaller(req);
    const supabase = serviceClient();

    const { data: pair, error } = await supabase
      .from("pairs")
      .select("id")
      .eq("status", "ACTIVE")
      .or(`user_a_id.eq.${user.id},user_b_id.eq.${user.id}`)
      .maybeSingle();

    if (error || !pair) throw new HttpError(404, "Tidak ada pair space aktif");

    const { error: updateError } = await supabase
      .from("pairs")
      .update({ status: "UNPAIRED" })
      .eq("id", pair.id);

    if (updateError) throw new HttpError(500, "Gagal memproses unpair");

    return jsonResponse({ status: "UNPAIRED" });
  } catch (err) {
    const status = err instanceof HttpError ? err.status : 500;
    return jsonResponse({ error: (err as Error).message }, status);
  }
});
