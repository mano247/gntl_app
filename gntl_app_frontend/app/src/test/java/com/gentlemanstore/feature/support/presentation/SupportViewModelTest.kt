package com.gentlemanstore.feature.support.presentation

import com.gentlemanstore.core.network.BadgeWebSocketManager
import com.gentlemanstore.core.network.ChatWebSocketManager
import com.gentlemanstore.core.util.ErrorType
import com.gentlemanstore.core.util.Resource
import com.gentlemanstore.data.datastore.TokenDataStore
import com.gentlemanstore.feature.support.data.dto.PagedTicketResponse
import com.gentlemanstore.feature.support.data.dto.SupportTicketResponse
import com.gentlemanstore.feature.support.data.dto.UnreadUpdateEvent
import com.gentlemanstore.feature.support.domain.SupportRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Customer Support: unread badge po status kategorijama + brisanje tiketa.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SupportViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var supportRepository: SupportRepository
    private lateinit var tokenDataStore: TokenDataStore
    private lateinit var chatWebSocketManager: ChatWebSocketManager
    private lateinit var badgeWebSocketManager: BadgeWebSocketManager

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        supportRepository = mockk()
        tokenDataStore = mockk()
        chatWebSocketManager = mockk(relaxed = true)
        badgeWebSocketManager = mockk(relaxed = true)

        every { tokenDataStore.token } returns flowOf("jwt-token")
        every { tokenDataStore.userId } returns flowOf("10")
        every { tokenDataStore.userRole } returns flowOf("ROLE_CUSTOMER")

        coEvery { supportRepository.getMyTickets(page = 0) } returns Resource.Success(
            page(listOf(ticket(1, "OPEN", unread = 2), ticket(2, "RESOLVED", unread = 1)))
        )
        coEvery { supportRepository.getUnreadCount(1) } returns Resource.Success(2)
        coEvery { supportRepository.getUnreadCount(2) } returns Resource.Success(1)
        coEvery { supportRepository.getMyUnreadSummary() } returns Resource.Success(
            mapOf("OPEN" to 2, "IN_PROGRESS" to 0, "RESOLVED" to 1, "CLOSED" to 0)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun ticket(id: Long, status: String, unread: Int = 0) = SupportTicketResponse(
        id = id,
        subject = "Ticket $id",
        status = status,
        createdAt = "2026-07-14T00:00:00",
        userEmail = "customer@test.com",
        sessionId = id * 100,
        unreadCount = unread
    )

    private fun page(tickets: List<SupportTicketResponse>) = PagedTicketResponse(
        content = tickets,
        totalPages = 1,
        totalElements = tickets.size.toLong(),
        last = true,
        first = true,
        number = 0,
        size = 20,
        numberOfElements = tickets.size
    )

    private fun createViewModel() = SupportViewModel(
        supportRepository = supportRepository,
        tokenDataStore = tokenDataStore,
        chatWebSocketManager = chatWebSocketManager,
        badgeWebSocketManager = badgeWebSocketManager
    )

    // ---------- unread badge po kategorijama ----------

    @Test
    fun `login ucitava tikete i unread summary po kategorijama`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.supportUiState.value
        assertEquals(2, state.tickets.size)
        assertEquals(2, state.unreadByStatus["OPEN"])
        assertEquals(1, state.unreadByStatus["RESOLVED"])
        assertEquals(0, state.unreadByStatus["IN_PROGRESS"])
        assertEquals("ALL badge = ukupan zbir", 3, state.unreadByStatus.values.sum())
    }

    @Test
    fun `refresh ekrana ponovo povlaci summary i zadrzava tacno stanje`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        // korisnik je u medjuvremenu procitao poruke tiketa 1
        coEvery { supportRepository.getUnreadCount(1) } returns Resource.Success(0)
        coEvery { supportRepository.getMyUnreadSummary() } returns Resource.Success(
            mapOf("OPEN" to 0, "IN_PROGRESS" to 0, "RESOLVED" to 1, "CLOSED" to 0)
        )

        viewModel.loadMyTickets()
        advanceUntilIdle()

        val state = viewModel.supportUiState.value
        assertEquals("badge nestaje kada padne na 0", 0, state.unreadByStatus["OPEN"])
        assertEquals(1, state.unreadByStatus.values.sum())
    }

    @Test
    fun `websocket unread event odmah azurira karticu i badge kategorije`() = runTest(testDispatcher) {
        val eventSlot = slot<(UnreadUpdateEvent) -> Unit>()
        every {
            badgeWebSocketManager.subscribe(
                topic = "/topic/user/10/unread",
                type = UnreadUpdateEvent::class.java,
                onEvent = capture(eventSlot),
                onResync = any()
            )
        } returns Unit

        val viewModel = createViewModel()
        advanceUntilIdle()

        coEvery { supportRepository.getMyUnreadSummary() } returns Resource.Success(
            mapOf("OPEN" to 5, "IN_PROGRESS" to 0, "RESOLVED" to 1, "CLOSED" to 0)
        )
        eventSlot.captured.invoke(UnreadUpdateEvent(ticketId = 1, sessionId = 100, unreadCount = 5))
        advanceUntilIdle()

        val state = viewModel.supportUiState.value
        assertEquals(5, state.tickets.first { it.id == 1L }.unreadCount)
        assertEquals(5, state.unreadByStatus["OPEN"])
    }

    // ---------- brisanje tiketa (customer) ----------

    @Test
    fun `uspesno brisanje uklanja tiket i osvezava badge kategorije`() = runTest(testDispatcher) {
        coEvery { supportRepository.deleteTicket(1) } returns Resource.Success(Unit)

        val viewModel = createViewModel()
        advanceUntilIdle()

        coEvery { supportRepository.getMyUnreadSummary() } returns Resource.Success(
            mapOf("OPEN" to 0, "IN_PROGRESS" to 0, "RESOLVED" to 1, "CLOSED" to 0)
        )
        viewModel.deleteTicket(1)
        advanceUntilIdle()

        val state = viewModel.supportUiState.value
        assertTrue(state.tickets.none { it.id == 1L })
        assertEquals("Ticket deleted successfully!", state.successMessage)
        assertEquals(0, state.unreadByStatus["OPEN"])
    }

    @Test
    fun `greska pri brisanju prikazuje backend poruku i vraca listu`() = runTest(testDispatcher) {
        coEvery { supportRepository.deleteTicket(1) } returns
                Resource.Error("Ticket not found", ErrorType.NOT_FOUND)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.deleteTicket(1)
        advanceUntilIdle()

        val state = viewModel.supportUiState.value
        assertEquals("Ticket not found", state.error)
        assertTrue("lista je ponovo ucitana sa servera", state.tickets.any { it.id == 1L })
        coVerify(atLeast = 2) { supportRepository.getMyTickets(page = 0) }
    }
}
