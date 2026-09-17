package com.nazeworks.nazever.domain.model

import kotlinx.datetime.Instant

/**
 * Representasi user yang sudah lolos authentication (Supabase Auth).
 * Tidak pernah membawa credential mentah (password/token) — token
 * disimpan terpisah di lapisan security, bukan di objek domain ini.
 */
data class User(
    val id: UserId,
    val displayName: String,
    val avatarUrl: String?,
    val createdAt: Instant
)

@JvmInline
value class UserId(val value: String)

/**
 * PairSpace = "private pair space" setelah dua user berhasil pairing.
 * Sengaja immutable dari sisi client: mutasi (ganti member, dsb) tidak
 * ada — hanya CREATED dan (opsional) UNPAIRED via server-side action.
 */
data class PairSpace(
    val id: PairSpaceId,
    val userAId: UserId,
    val userBId: UserId,
    val createdAt: Instant,
    val status: PairSpaceStatus
)

@JvmInline
value class PairSpaceId(val value: String)

enum class PairSpaceStatus {
    ACTIVE,
    UNPAIRED
}

/**
 * Helper: dari sudut pandang `selfId`, siapa partner-nya di pair ini.
 * Dipakai di banyak tempat (chat header, shared calendar, dst) supaya
 * tidak ada modul lain yang perlu tahu skema userA/userB secara langsung.
 */
fun PairSpace.partnerOf(selfId: UserId): UserId =
    if (selfId == userAId) userBId else userAId
