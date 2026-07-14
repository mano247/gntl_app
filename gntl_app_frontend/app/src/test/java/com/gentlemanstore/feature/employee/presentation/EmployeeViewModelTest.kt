package com.gentlemanstore.feature.employee.presentation

import com.gentlemanstore.core.network.BadgeWebSocketManager
import com.gentlemanstore.core.util.Resource
import com.gentlemanstore.feature.employee.domain.EmployeeRepository
import com.gentlemanstore.feature.order.data.dto.OrderResponse
import com.gentlemanstore.feature.order.data.dto.PagedOrderResponse
import com.gentlemanstore.feature.support.domain.SupportRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class EmployeeViewModelTest {

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
        // Tiketi nisu predmet ovih testova — vrati grešku da se preskoči unread sync.
        coEvery { employeeRepository.getAllTickets(any()) } returns Resource.Error("not under test")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun order(id: Long, status: String) = OrderResponse(
        id = id,
        totalPrice = BigDecimal.TEN,
        status = status,
        createdAt = "2026-07-14T00:00:00",
        items = emptyList(),
        loyaltyDiscount = null,
        finalPrice = null,
        promoDiscount = null
    )

    private fun page(orders: List<OrderResponse>, last: Boolean = true) = PagedOrderResponse(
        content = orders,
        totalPages = 1,
        totalElements = orders.size.toLong(),
        last = last,
        first = true,
        number = 0,
        size = 20,
        numberOfElements = orders.size
    )

    private fun createViewModel() = EmployeeViewModel(
        employeeRepository = employeeRepository,
        supportRepository = supportRepository,
        badgeWebSocketManager = badgeWebSocketManager
    )

    @Test
    fun `prvi ulazak odmah trazi PENDING ordere sa servera`() = runTest(testDispatcher) {
        coEvery { employeeRepository.getAllOrders(0, "PENDING") } returns
                Resource.Success(page(listOf(order(1, "PENDING"), order(2, "PENDING"))))

        val viewModel = createViewModel()
        advanceUntilIdle()

        coVerify(exactly = 1) { employeeRepository.getAllOrders(0, "PENDING") }
        val state = viewModel.ordersUiState.value
        assertEquals("PENDING", state.selectedStatus)
        assertEquals(listOf(1L, 2L), state.orders.map { it.id })
        assertFalse(state.isLoading)
    }

    @Test
    fun `promena filtera resetuje paginaciju i trazi novi status`() = runTest(testDispatcher) {
        coEvery { employeeRepository.getAllOrders(0, "PENDING") } returns
                Resource.Success(page(listOf(order(1, "PENDING")), last = false))
        coEvery { employeeRepository.getAllOrders(0, "CONFIRMED") } returns
                Resource.Success(page(listOf(order(9, "CONFIRMED"))))

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onOrderStatusFilter("CONFIRMED")
        advanceUntilIdle()

        coVerify(exactly = 1) { employeeRepository.getAllOrders(0, "CONFIRMED") }
        val state = viewModel.ordersUiState.value
        assertEquals("CONFIRMED", state.selectedStatus)
        assertEquals(0, state.currentPage)
        assertTrue(state.isLastPage)
        assertEquals(listOf(9L), state.orders.map { it.id })
    }

    @Test
    fun `spori stari request ne prepisuje rezultat novijeg filtera`() = runTest(testDispatcher) {
        coEvery { employeeRepository.getAllOrders(0, "PENDING") } coAnswers {
            delay(1_000)
            Resource.Success(page(listOf(order(1, "PENDING"))))
        }
        coEvery { employeeRepository.getAllOrders(0, "CONFIRMED") } coAnswers {
            delay(10)
            Resource.Success(page(listOf(order(9, "CONFIRMED"))))
        }

        val viewModel = createViewModel()
        advanceTimeBy(50) // PENDING request je u toku (visi u delay-u)

        viewModel.onOrderStatusFilter("CONFIRMED")
        advanceUntilIdle()

        val state = viewModel.ordersUiState.value
        assertEquals("CONFIRMED", state.selectedStatus)
        assertEquals(listOf(9L), state.orders.map { it.id })
        assertFalse(state.isLoading)
    }

    @Test
    fun `neuspesan request ne ostavlja ekran u loading stanju i cuva error`() = runTest(testDispatcher) {
        coEvery { employeeRepository.getAllOrders(0, "PENDING") } returns
                Resource.Error("Network error")

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.ordersUiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.error)
        assertEquals("Network error", state.error)
    }

    @Test
    fun `prazan rezultat daje prazno stanje sa poslednjom stranicom`() = runTest(testDispatcher) {
        coEvery { employeeRepository.getAllOrders(0, "PENDING") } returns
                Resource.Success(page(emptyList()))

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.ordersUiState.value
        assertTrue(state.orders.isEmpty())
        assertTrue(state.isLastPage)
        assertFalse(state.isLoading)
    }

    @Test
    fun `loadMoreOrders trazi sledecu stranicu sa istim statusom`() = runTest(testDispatcher) {
        coEvery { employeeRepository.getAllOrders(0, "PENDING") } returns
                Resource.Success(page(listOf(order(1, "PENDING")), last = false))
        coEvery { employeeRepository.getAllOrders(1, "PENDING") } returns
                Resource.Success(page(listOf(order(2, "PENDING")), last = true))

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.loadMoreOrders()
        advanceUntilIdle()

        coVerify(exactly = 1) { employeeRepository.getAllOrders(1, "PENDING") }
        val state = viewModel.ordersUiState.value
        assertEquals(listOf(1L, 2L), state.orders.map { it.id })
        assertEquals(1, state.currentPage)
        assertTrue(state.isLastPage)
    }
}
