package com.nazeworks.nazever.presentation.pairing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nazeworks.nazever.domain.model.PairSpace
import com.nazeworks.nazever.domain.model.PairingInviteCreated
import com.nazeworks.nazever.domain.repository.PairingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PairingUiState(
    val activePairSpace: PairSpace? = null,
    val currentInvite: PairingInviteCreated? = null,
    val incomingRequestInviteIds: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class PairingViewModel @Inject constructor(
    private val pairingRepository: PairingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PairingUiState())
    val uiState: StateFlow<PairingUiState> = _uiState.asStateFlow()

    init {
        pairingRepository.activePairSpace
            .onEach { pairSpace -> _uiState.update { it.copy(activePairSpace = pairSpace) } }
            .launchIn(viewModelScope)

        pairingRepository.observeIncomingPairingRequests()
            .onEach { ids -> _uiState.update { it.copy(incomingRequestInviteIds = ids) } }
            .launchIn(viewModelScope)
    }

    /** User A menekan "Undang Pasangan". */
    fun createInvite() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = pairingRepository.createInvite()
            _uiState.update {
                result.fold(
                    onSuccess = { invite -> it.copy(isLoading = false, currentInvite = invite) },
                    onFailure = { e -> it.copy(isLoading = false, errorMessage = e.message) }
                )
            }
        }
    }

    fun revokeCurrentInvite() {
        val inviteId = _uiState.value.currentInvite?.invite?.id?.value ?: return
        viewModelScope.launch {
            pairingRepository.revokeInvite(inviteId)
            _uiState.update { it.copy(currentInvite = null) }
        }
    }

    /** User B membuka deep link `nazever://pair?token=...` -> token diekstrak lalu dipanggil di sini. */
    fun consumeInvite(rawToken: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = pairingRepository.consumeInvite(rawToken)
            _uiState.update {
                result.fold(
                    onSuccess = { it.copy(isLoading = false) },
                    onFailure = { e -> it.copy(isLoading = false, errorMessage = e.message) }
                )
            }
        }
    }

    /** User A menekan Approve/Reject pada permintaan pairing yang masuk. */
    fun confirmPairing(inviteId: String, accept: Boolean) {
        viewModelScope.launch {
            pairingRepository.confirmPairing(inviteId, accept)
        }
    }

    private fun MutableStateFlow<PairingUiState>.update(transform: (PairingUiState) -> PairingUiState) {
        value = transform(value)
    }
}
