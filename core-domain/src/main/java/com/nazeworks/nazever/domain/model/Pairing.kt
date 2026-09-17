package com.nazeworks.nazever.domain.model

import kotlinx.datetime.Instant

/**
 * SECURE PAIRING DESIGN
 * ---------------------
 * Tidak memakai kode numerik pendek yang mudah ditebak/brute-force.
 * Alurnya:
 *
 *  1. User A (inviter) minta invite baru -> server (Edge Function
 *     `create_pairing_invite`) generate `token` acak 128-bit
 *     (bukan dibuat di client), simpan HASH-nya saja di DB
 *     (`pairing_invites.token_hash`), dan set `expiresAt` singkat
 *     (default 10 menit) serta `maxAttempts`.
 *
 *  2. Token mentah HANYA pernah ada di response sekali itu saja,
 *     dikirim ke User A untuk dibagikan lewat kanal out-of-band
 *     (deep link / QR code yang di-scan langsung, BUKAN dikirim
 *     lewat server chat aplikasi ini karena saat itu belum ada
 *     private space).
 *
 *  3. User B membuka deep link -> app memanggil
 *     `consume_pairing_invite(token)` dengan JWT User B terlampir.
 *     Server hash token yang diterima, cocokkan ke `token_hash`,
 *     cek belum expired/consumed/revoked, cek User B != User A.
 *
 *  4. Supaya link yang bocor tidak otomatis mem-pairing siapa pun,
 *     hasil consume BUKAN langsung ACTIVE: status jadi
 *     `AWAITING_CONFIRMATION`, dan User A menerima notifikasi
 *     realtime untuk approve/reject secara eksplisit di perangkatnya.
 *     PairSpace baru berstatus ACTIVE setelah User A confirm.
 *
 *  5. Semua validasi (expiry, single-use, ownership, confirmation)
 *     dilakukan di server (RLS + Edge Function), tidak pernah
 *     dipercayakan ke client.
 */
data class PairingInvite(
    val id: PairingInviteId,
    val inviterId: UserId,
    val status: PairingInviteStatus,
    val expiresAt: Instant,
    val createdAt: Instant
)

@JvmInline
value class PairingInviteId(val value: String)

enum class PairingInviteStatus {
    PENDING,
    AWAITING_CONFIRMATION,
    ACTIVE,
    EXPIRED,
    REVOKED
}

/**
 * Payload yang dikembalikan server SEKALI SAJA saat invite dibuat.
 * `rawToken` tidak pernah disimpan ulang oleh client (hanya dipakai
 * untuk membentuk deep link / QR, lalu dibuang dari memory).
 */
data class PairingInviteCreated(
    val invite: PairingInvite,
    val rawToken: String,
    val deepLink: String
)
