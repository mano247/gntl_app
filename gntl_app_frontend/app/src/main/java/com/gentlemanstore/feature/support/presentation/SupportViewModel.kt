package com.gentlemanstore.feature.support.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gentlemanstore.core.network.BadgeWebSocketManager
import com.gentlemanstore.core.network.ChatWebSocketManager
import com.gentlemanstore.core.util.Resource
import com.gentlemanstore.data.datastore.TokenDataStore
import com.gentlemanstore.feature.support.data.dto.*
import com.gentlemanstore.feature.support.domain.SupportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

data class SupportUiState(
    val isLoading: Boolean = false,
    val tickets: List<SupportTicketResponse> = emptyList(),
    val error: String? = null,
    val isLastPage: Boolean = false,
    val currentPage: Int = 0,
    val successMessage: String? = null,
    val selectedStatus: String? = "OPEN",
    // Unread brojaci po statusu tiketa (backend summary nad svim tiketima,
    // ne nad trenutnom stranicom) - badge na status filter chipovima
    val unreadByStatus: Map<String, Int> = emptyMap()
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
    private val tokenDataStore: TokenDataStore,
    private val chatWebSocketManager: ChatWebSocketManager,
    private val badgeWebSocketManager: BadgeWebSocketManager
) : ViewModel() {

    private val _supportUiState = MutableStateFlow(SupportUiState())
    val supportUiState: StateFlow<SupportUiState> = _supportUiState.asStateFlow()

    private val _botFlowUiState = MutableStateFlow(BotFlowUiState())
    val botFlowUiState: StateFlow<BotFlowUiState> = _botFlowUiState.asStateFlow()

    private val _chatUiState = MutableStateFlow(ChatUiState())
    val chatUiState: StateFlow<ChatUiState> = _chatUiState.asStateFlow()

    private var unreadTopic: String? = null

    val currentRole = tokenDataStore.userRole
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "CUSTOMER")

    init {
        // Ovaj ViewModel se kreira pri startu aplikacije (activity scope, zbog
        // badge-a u bottom bar-u), tj. pre logina. Zato se inicijalno učitavanje
        // i badge subscription vezuju za pojavu tokena: na login se jednom
        // učita lista pa se otvori WebSocket subscription; na logout se odjavi.
        viewModelScope.launch {
            tokenDataStore.token
                .map { it != null }
                .distinctUntilChanged()
                .collect { loggedIn ->
                    if (loggedIn) {
                        loadMyTicketsAndUnread()
                        tokenDataStore.userId.first()?.toLongOrNull()
                            ?.let { subscribeToUnreadUpdates(it) }
                    } else {
                        unsubscribeFromUnreadUpdates()
                    }
                }
        }
    }

    fun subscribeToUnreadUpdates(userId: Long) {
        val topic = "/topic/user/$userId/unread"
        unreadTopic = topic
        badgeWebSocketManager.subscribe(
            topic = topic,
            type = UnreadUpdateEvent::class.java,
            onEvent = { event ->
                // Server pushuje novi unreadCount — samo prepiši lokalni state,
                // bez REST poziva za samu listu.
                _supportUiState.update { state ->
                    state.copy(tickets = state.tickets.map {
                        if (it.id == event.ticketId) it.copy(unreadCount = event.unreadCount) else it
                    })
                }
                // Badge po kategorijama se preracunava na backendu (tiket na koji
                // se event odnosi ne mora biti u trenutno ucitanoj stranici).
                loadUnreadSummary()
            },
            onResync = {
                // Prekid ili reconnect WebSocket-a — jednokratna REST sinhronizacija.
                loadMyTickets()
            }
        )
    }

    fun unsubscribeFromUnreadUpdates() {
        unreadTopic?.let { badgeWebSocketManager.unsubscribe(it) }
        unreadTopic = null
    }

    private suspend fun loadMyTicketsAndUnread() {
        if (_supportUiState.value.tickets.isEmpty()) {
            _supportUiState.value = _supportUiState.value.copy(isLoading = true)
        }
        when (val result = supportRepository.getMyTickets(page = 0)) {
            is Resource.Success -> {
                _supportUiState.value = _supportUiState.value.copy(
                    isLoading = false,
                    tickets = result.data.content,
                    currentPage = 0,
                    isLastPage = result.data.last,
                    error = null
                )
                loadUnreadCountsSync()
                loadUnreadSummarySync()
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

    private suspend fun loadUnreadSummarySync() {
        when (val result = supportRepository.getMyUnreadSummary()) {
            is Resource.Success -> {
                _supportUiState.update { it.copy(unreadByStatus = result.data) }
            }
            else -> Unit
        }
    }

    fun loadUnreadSummary() {
        viewModelScope.launch {
            loadUnreadSummarySync()
        }
    }

    private suspend fun loadUnreadCountsSync() {
        val tickets = _supportUiState.value.tickets
        tickets.forEach { ticket ->
            when (val result = supportRepository.getUnreadCount(ticket.id)) {
                is Resource.Success -> {
                    _supportUiState.value = _supportUiState.value.copy(
                        tickets = _supportUiState.value.tickets.map {
                            if (it.id == ticket.id) it.copy(unreadCount = result.data) else it
                        }
                    )
                }
                else -> Unit
            }
        }
    }

    fun loadMyTickets() {
        viewModelScope.launch {
            loadMyTicketsAndUnread()
        }
    }

    fun startBotFlow() {
        viewModelScope.launch {
            _botFlowUiState.value = BotFlowUiState(isLoading = true)
            when (val result = supportRepository.getBotQuestions()) {
                is Resource.Success -> {
                    val sortedQuestions = result.data.sortedBy { it.orderIndex }
                    _botFlowUiState.value = BotFlowUiState(isLoading = false, questions = sortedQuestions)
                }
                is Resource.Error -> {
                    _botFlowUiState.value = BotFlowUiState(isLoading = false, error = result.message)
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
            _botFlowUiState.value = state.copy(answers = updatedAnswers, currentAnswer = "", isCreatingTicket = true)
            createTicketAndSaveAnswers(updatedAnswers, state.questions)
        } else {
            _botFlowUiState.value = state.copy(
                answers = updatedAnswers,
                currentQuestionIndex = state.currentQuestionIndex + 1,
                currentAnswer = ""
            )
        }
    }

    private fun createTicketAndSaveAnswers(answers: Map<Long, String>, questions: List<BotQuestionResponse>) {
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

    fun loadMessages(sessionId: Long) {
        viewModelScope.launch {
            _chatUiState.value = _chatUiState.value.copy(isLoading = true)
            when (val result = supportRepository.getMessages(sessionId)) {
                is Resource.Success -> {
                    _chatUiState.value = _chatUiState.value.copy(
                        isLoading = false,
                        messages = result.data.sortedBy { it.sentAt },
                        error = null
                    )
                }
                is Resource.Error -> {
                    _chatUiState.value = _chatUiState.value.copy(isLoading = false, error = result.message)
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun connectWebSocket(sessionId: Long) {
        chatWebSocketManager.connect(
            sessionId = sessionId,
            onMessage = { message ->
                // Sopstvene poruke se takođe vraćaju kroz broadcast — dedup po id-u.
                _chatUiState.update { state ->
                    if (state.messages.any { it.id == message.id }) state
                    else state.copy(messages = (state.messages + message).sortedBy { it.sentAt })
                }
            },
            onError = { error ->
                _chatUiState.update { it.copy(error = error) }
            }
        )
    }

    fun disconnectWebSocket() {
        chatWebSocketManager.disconnect()
    }

    fun onMessageChange(message: String) {
        _chatUiState.value = _chatUiState.value.copy(currentMessage = message)
    }

    fun deleteTicket(ticketId: Long) {
        viewModelScope.launch {
            _supportUiState.value = _supportUiState.value.copy(
                tickets = _supportUiState.value.tickets.filter { it.id != ticketId }
            )
            when (val result = supportRepository.deleteTicket(ticketId)) {
                is Resource.Success -> {
                    _supportUiState.value = _supportUiState.value.copy(
                        successMessage = "Ticket deleted successfully!"
                    )
                    // Obrisan tiket vise ne ulazi u unread brojace kategorija
                    loadUnreadSummary()
                }
                is Resource.Error -> {
                    // Rollback optimistickog uklanjanja (reload sa servera),
                    // pa tek onda konkretna backend poruka - obrnut redosled bi
                    // obrisao poruku pre nego sto je UI prikaze.
                    loadMyTicketsAndUnread()
                    _supportUiState.value = _supportUiState.value.copy(
                        error = result.message
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

            _chatUiState.value = _chatUiState.value.copy(currentMessage = "")

            // Primarno slanje kroz WebSocket — poruka se vraća kroz broadcast
            // na /topic/chat/{sessionId} i tada ulazi u listu.
            if (chatWebSocketManager.sendMessage(message, sender)) {
                markMessagesAsRead(sessionId)
                return@launch
            }

            // Fallback na REST ako WebSocket konekcija nije aktivna
            _chatUiState.value = _chatUiState.value.copy(isSending = true)
            when (val result = supportRepository.sendMessage(sessionId, message, sender)) {
                is Resource.Success -> {
                    _chatUiState.update { state ->
                        val messages =
                            if (state.messages.any { it.id == result.data.id }) state.messages
                            else (state.messages + result.data).sortedBy { it.sentAt }
                        state.copy(isSending = false, messages = messages)
                    }
                    markMessagesAsRead(sessionId)
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

    val filteredTickets: StateFlow<List<SupportTicketResponse>> = _supportUiState
        .map { state ->
            if (state.selectedStatus == null) state.tickets
            else state.tickets.filter { it.status == state.selectedStatus }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onStatusFilter(status: String?) {
        _supportUiState.value = _supportUiState.value.copy(selectedStatus = status)
    }

    fun loadUnreadCounts() {
        viewModelScope.launch {
            loadUnreadCountsSync()
        }
    }

    fun markMessagesAsRead(sessionId: Long) {
        viewModelScope.launch {
            supportRepository.markMessagesAsRead(sessionId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        disconnectWebSocket()
        unsubscribeFromUnreadUpdates()
    }
}