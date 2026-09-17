package com.nazeworks.nazever.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Row `pairing_invites` — hanya field yang boleh dibaca client (lihat supabase/schema.sql untuk RLS). */
@Serializable
data class PairingInviteDto(
    val id: String,
    @SerialName("inviter_id") val inviterId: String,
    val status: String,
    @SerialName("expires_at") val expiresAt: String,
    @SerialName("created_at") val createdAt: String
)

/** Response dari Edge Function `create_pairing_invite`. */
@Serializable
data class CreateInviteResponseDto(
    val invite: PairingInviteDto,
    @SerialName("raw_token") val rawToken: String,
    @SerialName("deep_link") val deepLink: String
)

/** Request body untuk Edge Function `consume_pairing_invite`. */
@Serializable
data class ConsumeInviteRequestDto(
    val token: String
)

/** Request body untuk Edge Function `confirm_pairing`. */
@Serializable
data class ConfirmPairingRequestDto(
    @SerialName("invite_id") val inviteId: String,
    val accept: Boolean
)

/** Row `pairs` — dua user + status. */
@Serializable
data class PairSpaceDto(
    val id: String,
    @SerialName("user_a_id") val userAId: String,
    @SerialName("user_b_id") val userBId: String,
    val status: String,
    @SerialName("created_at") val createdAt: String
)
