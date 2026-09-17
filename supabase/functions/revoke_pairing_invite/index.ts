import { HttpError, jsonResponse, requireCaller, serviceClient } from "../_shared/client.ts";

Deno.serve(async (req) => {
  try {
    const user = await requireCaller(req);
    const { invite_id } = await req.json();
    if (!invite_id) throw new HttpError(400, "invite_id wajib diisi");

    const supabase = serviceClient();
    const { error } = await supabase
      .from("pairing_invites")
      .update({ status: "REVOKED" })
      .eq("id", invite_id)
      .eq("inviter_id", user.id) // guard: hanya pemilik invite yang boleh revoke
      .in("status", ["PENDING", "AWAITING_CONFIRMATION"]);

    if (error) throw new HttpError(500, "Gagal revoke invite");
    return jsonResponse({ status: "REVOKED" });
  } catch (err) {
    const status = err instanceof HttpError ? err.status : 500;
    return jsonResponse({ error: (err as Error).message }, status);
  }
});
