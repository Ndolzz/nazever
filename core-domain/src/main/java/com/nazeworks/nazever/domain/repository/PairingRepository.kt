package com.nazeworks.nazever.domain.repository

import com.nazeworks.nazever.domain.model.PairSpace
import com.nazeworks.nazever.domain.model.PairingInviteCreated
import kotlinx.coroutines.flow.Flow

interface PairingRepository {

    /** Emit PairSpace aktif milik user saat ini, atau null jika belum pairing. */
    val activePairSpace: Flow<PairSpace?>

    /** Dipanggil User A. Memanggil Edge Function `create_pairing_invite`. */
    suspend fun createInvite(): Result<PairingInviteCreated>

    /** Dipanggil User A jika ingin membatalkan invite sebelum dipakai/expired. */
    suspend fun revokeInvite(inviteId: String): Result<Unit>

    /**
     * Dipanggil User B setelah membuka deep link / scan QR.
     * Server akan menaruh status invite ke AWAITING_CONFIRMATION,
     * BUKAN langsung membuat PairSpace aktif.
     */
    suspend fun consumeInvite(rawToken: String): Result<Unit>

    /**
     * Dipanggil User A untuk approve/reject permintaan pairing yang
     * masuk (hasil dari consumeInvite oleh User B).
     */
    suspend fun confirmPairing(inviteId: String, accept: Boolean): Result<Unit>

    /** Observasi realtime: ada permintaan pairing baru yang menunggu konfirmasi User A. */
    fun observeIncomingPairingRequests(): Flow<List<String>>

    /** Memutus pairing yang sudah aktif (kedua user harus konfirmasi terpisah, lihat docs/ARCHITECTURE.md). */
    suspend fun requestUnpair(): Result<Unit>
}
