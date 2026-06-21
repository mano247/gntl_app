package com.gentlemanstore.feature.support.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gentlemanstore.core.util.Resource
import com.gentlemanstore.data.datastore.TokenDataStore
import com.gentlemanstore.feature.support.data.dto.*
import com.gentlemanstore.feature.support.domain.SupportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn

data class SupportUiState(
    val isLoading: Boolean = false,
    val tickets: List<SupportTicketResponse> = emptyList(),
    val error: String? = null,
    val isLastPage: Boolean = false,
    val currentPage: Int = 0,
    val successMessage: String? = null,
    val selectedStatus: String? = null
)

data class BotFlowUiState(
    val isLoading: Boolean = false,
    val questions: List<BotQuestionResponse> = emptyList(),
    val currentQuestionIndex: Int = 0,
    val answers: Map<Long, String> = emptyMap(),
    val currentAnswer: String = "",
    val ticket: SupportTicketResponse? = null,
    val isCreatingTicket: Boolean = false,
    val isSavingAnswer: Boolean = false,
    val error: String? = null,
    val flowComplete: Boolean = false
)

data class ChatUiState(
    val isLoading: Boolean = false,
    val messages: List<ChatMessageResponse> = emptyList(),
    val currentMessage: String = "",
    val isSending: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SupportViewModel @Inject constructor(
    private val supportRepository: SupportRepository,
    private val tokenDataStore: TokenDataStore
) : ViewModel() {

    private val _supportUiState = MutableStateFlow(SupportUiState())
    val supportUiState: StateFlow<SupportUiState> = _supportUiState.asStateFlow()

    private val _botFlowUiState = MutableStateFlow(BotFlowUiState())
    val botFlowUiState: StateFlow<BotFlowUiState> = _botFlowUiState.asStateFlow()

    private val _chatUiState = MutableStateFlow(ChatUiState())
    val chatUiState: StateFlow<ChatUiState> = _chatUiState.asStateFlow()

    private var pollingJob: Job? = null

    val currentRole = tokenDataStore.userRole
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "CUSTOMER")



    // --- Tickets ---
    fun loadMyTickets() {
        viewModelScope.launch {
            _supportUiState.value = _supportUiState.value.copy(isLoading = true)

            when (val result = supportRepository.getMyTickets(page = 0)) {
                is Resource.Success -> {
                    _supportUiState.value = _supportUiState.value.copy(
                        isLoading = false,
                        tickets = result.data.content,
                        currentPage = 0,
                        isLastPage = result.data.last,
                        error = null
                    )
                }
                is Resource.Error -> {
                    _supportUiState.value = _supportUiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
                is Resource.Loading -> Unit
            }
        }
    }

    // --- Bot Flow ---
    fun startBotFlow() {
        viewModelScope.launch {
            _botFlowUiState.value = BotFlowUiState(isLoading = true)

            when (val result = supportRepository.getBotQuestions()) {
                is Resource.Success -> {
                    val sortedQuestions = result.data.sortedBy { it.orderIndex }
                    _botFlowUiState.value = BotFlowUiState(
                        isLoading = false,
                        questions = sortedQuestions
                    )
                }
                is Resource.Error -> {
                    _botFlowUiState.value = BotFlowUiState(
                        isLoading = false,
                        error = result.message
                    )
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun onBotAnswerChange(answer: String) {
        _botFlowUiState.value = _botFlowUiState.value.copy(currentAnswer = answer)
    }

    fun submitBotAnswer() {
        val state = _botFlowUiState.value
        val currentQuestion = state.questions.getOrNull(state.currentQuestionIndex) ?: return
        val answer = state.currentAnswer.trim()
        if (answer.isBlank()) return

        val updatedAnswers = state.answers + (currentQuestion.id to answer)
        val isLastQuestion = state.currentQuestionIndex >= state.questions.size - 1

        if (isLastQuestion) {
            _botFlowUiState.value = state.copy(
                answers = updatedAnswers,
                currentAnswer = "",
                isCreatingTicket = true
            )
            createTicketAndSaveAnswers(updatedAnswers, state.questions)
        } else {
            _botFlowUiState.value = state.copy(
                answers = updatedAnswers,
                currentQuestionIndex = state.currentQuestionIndex + 1,
                currentAnswer = ""
            )
        }
    }

    private fun createTicketAndSaveAnswers(
        answers: Map<Long, String>,
        questions: List<BotQuestionResponse>
    ) {
        viewModelScope.launch {
            val subject = answers[questions.firstOrNull()?.id] ?: "Support Request"

            when (val ticketResult = supportRepository.createTicket(subject)) {
                is Resource.Success -> {
                    val ticket = ticketResult.data
                    answers.forEach { (questionId, response) ->
                        supportRepository.saveBotResponse(ticket.id, questionId, response)
                    }
                    _botFlowUiState.value = _botFlowUiState.value.copy(
                        isCreatingTicket = false,
                        ticket = ticket,
                        flowComplete = true
                    )
                    loadMyTickets()
                }
                is Resource.Error -> {
                    _botFlowUiState.value = _botFlowUiState.value.copy(
                        isCreatingTicket = false,
                        error = ticketResult.message
                    )
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun resetBotFlow() {
        _botFlowUiState.value = BotFlowUiState()
    }

    // --- Chat ---
    fun loadMessages(sessionId: Long) {
        viewModelScope.launch {
            _chatUiState.value = _chatUiState.value.copy(isLoading = true)

            when (val result = supportRepository.getMessages(sessionId)) {
                is Resource.Success -> {
                    _chatUiState.value = _chatUiState.value.copy(
                        isLoading = false,
                        messages = result.data,
                        error = null
                    )
                }
                is Resource.Error -> {
                    _chatUiState.value = _chatUiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun startPolling(sessionId: Long) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                delay(5000)
                when (val result = supportRepository.getMessages(sessionId)) {
                    is Resource.Success -> {
                        _chatUiState.value = _chatUiState.value.copy(
                            messages = result.data
                        )
                    }
                    else -> Unit
                }
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    fun onMessageChange(message: String) {
        _chatUiState.value = _chatUiState.value.copy(currentMessage = message)
    }

    fun deleteTicket(ticketId: Long) {
        viewModelScope.launch {
            when (supportRepository.deleteTicket(ticketId)) {
                is Resource.Success -> {
                    _supportUiState.value = _supportUiState.value.copy(
                        successMessage = "Ticket deleted successfully!"
                    )
                    delay(300)
                    loadMyTickets()
                }
                is Resource.Error -> {
                    _supportUiState.value = _supportUiState.value.copy(
                        error = "Failed to delete ticket"
                    )
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun clearMessages() {
        _supportUiState.value = _supportUiState.value.copy(error = null, successMessage = null)
    }

    fun sendMessage(sessionId: Long) {
        val message = _chatUiState.value.currentMessage.trim()
        if (message.isBlank()) return

        viewModelScope.launch {
            val role = tokenDataStore.userRole.first() ?: "ROLE_CUSTOMER"
            val sender = if (role.contains("CUSTOMER")) "USER" else "EMPLOYEE"

            _chatUiState.value = _chatUiState.value.copy(
                isSending = true,
                currentMessage = ""
            )

            when (val result = supportRepository.sendMessage(sessionId, message, sender)) {
                is Resource.Success -> {
                    _chatUiState.value = _chatUiState.value.copy(
                        isSending = false,
                        messages = _chatUiState.value.messages + result.data
                    )
                }
                is Resource.Error -> {
                    _chatUiState.value = _chatUiState.value.copy(
                        isSending = false,
                        error = result.message,
                        currentMessage = message
                    )
                }
                is Resource.Loading -> Unit
            }
        }
    }

    val filteredTickets: List<SupportTicketResponse>
        get() = if (_supportUiState.value.selectedStatus == null) {
            _supportUiState.value.tickets
        } else {
            _supportUiState.value.tickets.filter { it.status == _supportUiState.value.selectedStatus }
        }

    fun onStatusFilter(status: String?) {
        _supportUiState.value = _supportUiState.value.copy(selectedStatus = status)
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }
}