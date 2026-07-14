package com.gentlemanstore.feature.employee.presentation

import com.gentlemanstore.core.network.BadgeWebSocketManager
import com.gentlemanstore.core.util.ErrorType
import com.gentlemanstore.core.util.Resource
import com.gentlemanstore.feature.employee.domain.EmployeeRepository
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
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Employee Support Tickets: unread badge po kategorijama + uklanjanje tiketa
 * (staff archive).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EmployeeTicketsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var employeeRepository: EmployeeRepository
    private lateinit var supportRepository: SupportRepository
    private lateinit var badgeWebSocketManager: BadgeWebSocketManager

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        employeeRepository = mockk()
        supportRepository = mockk()
        badgeWebSocketManager = mockk(relaxed = true)
        // Orderi nisu predmet ovih testova
        coEvery { employeeRepository.getAllOrders(any(), any()) } returns Resource.Error("not under test")
        coEvery { employeeRepository.getAllTickets(0) } returns Resource.Success(
            page(listOf(ticket(1, "OPEN", unread = 2), ticket(2, "RESOLVED", unread = 0)))
        )
        coEvery { supportRepository.getUnreadCount(1) } returns Resource.Success(2)
        coEvery { supportRepository.getUnreadCount(2) } returns Resource.Success(0)
        coEvery { supportRepository.getStaffUnreadSummary() } returns Resource.Success(
            mapOf("OPEN" to 2, "IN_PROGRESS" to 0, "RESOLVED" to 0, "CLOSED" to 0)
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

    private fun createViewModel() = EmployeeViewModel(
        employeeRepository = employeeRepository,
        supportRepository = supportRepository,
        badgeWebSocketManager = badgeWebSocketManager
    )

    // ---------- unread badge po kategorijama ----------

    @Test
    fun `ucitavanje tiketa povlaci i unread summary po statusima`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.ticketsUiState.value
        assertEquals(2, state.unreadByStatus["OPEN"])
        assertEquals(0, state.unreadByStatus["RESOLVED"])
        assertEquals(2, state.unreadByStatus.values.sum())
        coVerify(atLeast = 1) { supportRepository.getStaffUnreadSummary() }
    }

    @Test
    fun `websocket unread event azurira tiket i osvezava summary`() = runTest(testDispatcher) {
        val eventSlot = slot<(UnreadUpdateEvent) -> Unit>()
        every {
            badgeWebSocketManager.subscribe(
                topic = "/topic/employee/unread",
                type = UnreadUpdateEvent::class.java,
                onEvent = capture(eventSlot),
                onResync = any()
            )
        } returns Unit

        coEvery { supportRepository.getStaffUnreadSummary() } returns Resource.Success(
            mapOf("OPEN" to 2, "IN_PROGRESS" to 0, "RESOLVED" to 0, "CLOSED" to 0)
        ) andThen Resource.Success(
            mapOf("OPEN" to 3, "IN_PROGRESS" to 0, "RESOLVED" to 0, "CLOSED" to 0)
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        eventSlot.captured.invoke(UnreadUpdateEvent(ticketId = 1, sessionId = 100, unreadCount = 3))
        advanceUntilIdle()

        val state = viewModel.ticketsUiState.value
        assertEquals(3, state.tickets.first { it.id == 1L }.unreadCount)
        assertEquals(3, state.unreadByStatus["OPEN"])
    }

    @Test
    fun `promena statusa tiketa osvezava summary kategorija`() = runTest(testDispatcher) {
        coEvery { employeeRepository.updateTicketStatus(1, "RESOLVED") } returns
                Resource.Success(ticket(1, "RESOLVED", unread = 2))

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.updateTicketStatus(1, "RESOLVED")
        advanceUntilIdle()

        assertEquals("RESOLVED", viewModel.ticketsUiState.value.tickets.first { it.id == 1L }.status)
        // init + posle promene statusa
        coVerify(atLeast = 2) { supportRepository.getStaffUnreadSummary() }
    }

    // ---------- uklanjanje tiketa (staff archive) ----------

    @Test
    fun `uspesno uklanjanje sklanja tiket iz liste i osvezava badge`() = runTest(testDispatcher) {
        coEvery { supportRepository.archiveTicket(1) } returns Resource.Success(Unit)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.archiveTicket(1)
        advanceUntilIdle()

        val state = viewModel.ticketsUiState.value
        assertTrue(state.tickets.none { it.id == 1L })
        assertEquals("Ticket removed from list", state.successMessage)
        assertNull(state.archivingTicketId)
        coVerify(exactly = 1) { supportRepository.archiveTicket(1) }
        coVerify(atLeast = 2) { supportRepository.getStaffUnreadSummary() }
    }

    @Test
    fun `backend greska pri uklanjanju prikazuje poruku i vraca listu sa servera`() = runTest(testDispatcher) {
        coEvery { supportRepository.archiveTicket(1) } returns
                Resource.Error("Ticket not found", ErrorType.NOT_FOUND)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.archiveTicket(1)
        advanceUntilIdle()

        val state = viewModel.ticketsUiState.value
        assertEquals("Ticket not found", state.error)
        assertNull(state.archivingTicketId)
        // lista je ponovo ucitana sa servera (tiket 1 i dalje postoji)
        assertTrue(state.tickets.any { it.id == 1L })
    }

    @Test
    fun `dupli zahtev za uklanjanje se ignorise dok je prvi u toku`() = runTest(testDispatcher) {
        coEvery { supportRepository.archiveTicket(1) } coAnswers {
            kotlinx.coroutines.delay(1_000)
            Resource.Success(Unit)
        }

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.archiveTicket(1)
        advanceTimeBy(50)
        viewModel.archiveTicket(1)
        advanceUntilIdle()

        coVerify(exactly = 1) { supportRepository.archiveTicket(1) }
    }
}
