// Helper dipakai semua Edge Function di bawah ini.
// Dijalankan di runtime Deno milik Supabase Edge Functions (free tier).

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

/**
 * Service-role client — HANYA ada di server (Edge Function runtime),
 * tidak pernah dikirim ke APK. Dipakai untuk operasi yang butuh
 * melewati RLS secara terkontrol (mis. menulis ke `pairs` setelah
 * validasi token pairing berhasil).
 */
export function serviceClient() {
  const url = Deno.env.get("SUPABASE_URL")!;
  const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
  return createClient(url, serviceRoleKey, {
    auth: { autoRefreshToken: false, persistSession: false },
  });
}

/** Ambil & verifikasi user yang memanggil function ini dari header Authorization. */
export async function requireCaller(req: Request) {
  const authHeader = req.headers.get("Authorization") ?? "";
  const jwt = authHeader.replace("Bearer ", "");
  if (!jwt) {
    throw new HttpError(401, "Missing Authorization header");
  }
  const anonClient = createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_ANON_KEY")!,
  );
  const { data, error } = await anonClient.auth.getUser(jwt);
  if (error || !data.user) {
    throw new HttpError(401, "Invalid or expired session");
  }
  return data.user;
}

export class HttpError extends Error {
  constructor(public status: number, message: string) {
    super(message);
  }
}

/** Hash token pairing dengan SHA-256 sebelum disimpan/dicocokkan — token mentah tidak pernah masuk DB. */
export async function sha256Hex(input: string): Promise<string> {
  const data = new TextEncoder().encode(input);
  const digest = await crypto.subtle.digest("SHA-256", data);
  return Array.from(new Uint8Array(digest))
    .map((b) => b.toString(16).padStart(2, "0"))
    .join("");
}

/** Token acak 128-bit, encoded base32-ish (Crockford, tanpa karakter ambigu) untuk QR/deep link. */
export function generatePairingToken(): string {
  const bytes = new Uint8Array(16);
  crypto.getRandomValues(bytes);
  const alphabet = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"; // Crockford Base32
  let bits = 0n;
  for (const b of bytes) bits = (bits << 8n) | BigInt(b);
  let out = "";
  let totalBits = bytes.length * 8;
  while (totalBits > 0) {
    const shift = totalBits >= 5 ? totalBits - 5 : 0;
    const chunk = Number((bits >> BigInt(shift)) & 0x1fn);
    out += alphabet[chunk];
    totalBits -= 5;
  }
  return out;
}

export function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}
