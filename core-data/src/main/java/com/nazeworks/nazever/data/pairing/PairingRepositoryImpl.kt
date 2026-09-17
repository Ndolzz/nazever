package com.nazeworks.nazever.data.pairing

import com.nazeworks.nazever.data.remote.ConfirmPairingRequestDto
import com.nazeworks.nazever.data.remote.ConsumeInviteRequestDto
import com.nazeworks.nazever.data.remote.CreateInviteResponseDto
import com.nazeworks.nazever.data.remote.PairSpaceDto
import com.nazeworks.nazever.domain.model.PairSpace
import com.nazeworks.nazever.domain.model.PairSpaceId
import com.nazeworks.nazever.domain.model.PairSpaceStatus
import com.nazeworks.nazever.domain.model.PairingInvite
import com.nazeworks.nazever.domain.model.PairingInviteCreated
import com.nazeworks.nazever.domain.model.PairingInviteId
import com.nazeworks.nazever.domain.model.PairingInviteStatus
import com.nazeworks.nazever.domain.model.UserId
import com.nazeworks.nazever.domain.repository.PairingRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.datetime.Instant

class PairingRepositoryImpl(
    private val supabase: SupabaseClient,
    private val currentUserIdProvider: () -> UserId?
) : PairingRepository {

    // `pairs` punya RLS: SELECT hanya boleh oleh user_a_id / user_b_id itu sendiri
    // (lihat supabase/schema.sql). Query ini otomatis kefilter oleh RLS di server,
    // jadi filter user id di sini hanya untuk kejelasan intent, bukan satu-satunya
    // lapisan keamanan.
    override val activePairSpace: Flow<PairSpace?>
        get() = supabase.realtime.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "pairs"
        }.filterIsInstance<PostgresAction>()
            .map { fetchActivePairSpaceOnce() }

    private suspend fun fetchActivePairSpaceOnce(): PairSpace? {
        val userId = currentUserIdProvider() ?: return null
        val dto = supabase.postgrest["pairs"]
            .select(columns = Columns.ALL) {
                filter {
                    eq("status", PairSpaceStatus.ACTIVE.name)
                    or {
                        eq("user_a_id", userId.value)
                        eq("user_b_id", userId.value)
                    }
                }
                limit(1)
            }
            .decodeSingleOrNull<PairSpaceDto>() ?: return null
        return dto.toDomain()
    }

    override suspend fun createInvite(): Result<PairingInviteCreated> = runCatching {
        // Token mentah DIBUAT DI SERVER (Edge Function), bukan di client,
        // supaya client tidak pernah bertanggung jawab atas kualitas
        // randomness kriptografis maupun penyimpanan hash-nya.
        val response = supabase.functions.invoke("create_pairing_invite")
            .body<CreateInviteResponseDto>()

        PairingInviteCreated(
            invite = PairingInvite(
                id = PairingInviteId(response.invite.id),
                inviterId = UserId(response.invite.inviterId),
                status = PairingInviteStatus.valueOf(response.invite.status),
                expiresAt = Instant.parse(response.invite.expiresAt),
                createdAt = Instant.parse(response.invite.createdAt)
            ),
            rawToken = response.rawToken,
            deepLink = response.deepLink
        )
    }

    override suspend fun revokeInvite(inviteId: String): Result<Unit> = runCatching {
        supabase.functions.invoke("revoke_pairing_invite") {
            setBody(mapOf("invite_id" to inviteId))
        }
        Unit
    }

    override suspend fun consumeInvite(rawToken: String): Result<Unit> = runCatching {
        // Server yang menolak jika: token invalid, expired, sudah dipakai,
        // atau inviter == diri sendiri. Client tidak melakukan validasi apa pun
        // selain memastikan input tidak kosong, karena validasi keamanan
        // sesungguhnya wajib di server.
        require(rawToken.isNotBlank()) { "Token tidak boleh kosong" }
        supabase.functions.invoke("consume_pairing_invite") {
            setBody(ConsumeInviteRequestDto(token = rawToken))
        }
        Unit
    }

    override suspend fun confirmPairing(inviteId: String, accept: Boolean): Result<Unit> = runCatching {
        supabase.functions.invoke("confirm_pairing") {
            setBody(ConfirmPairingRequestDto(inviteId = inviteId, accept = accept))
        }
        Unit
    }

    override fun observeIncomingPairingRequests(): Flow<List<String>> =
        supabase.realtime.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "pairing_invites"
        }.mapNotNull { action ->
            // Hanya invite yang statusnya AWAITING_CONFIRMATION dan inviter-nya
            // adalah user saat ini yang relevan ditampilkan sebagai permintaan masuk.
            val status = action.record["status"]?.toString()
            val inviterId = action.record["inviter_id"]?.toString()
            val selfId = currentUserIdProvider()?.value
            if (status == PairingInviteStatus.AWAITING_CONFIRMATION.name && inviterId == selfId) {
                listOfNotNull(action.record["id"]?.toString())
            } else null
        }

    override suspend fun requestUnpair(): Result<Unit> = runCatching {
        supabase.functions.invoke("request_unpair")
        Unit
    }

    private fun PairSpaceDto.toDomain() = PairSpace(
        id = PairSpaceId(id),
        userAId = UserId(userAId),
        userBId = UserId(userBId),
        createdAt = Instant.parse(createdAt),
        status = PairSpaceStatus.valueOf(status)
    )
}
