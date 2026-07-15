package com.gentlemanstore.feature.order.presentation

import com.gentlemanstore.core.util.Resource
import com.gentlemanstore.feature.order.data.dto.OrderResponse
import com.gentlemanstore.feature.order.data.dto.PagedOrderResponse
import com.gentlemanstore.feature.order.domain.OrderRepository
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
class MyOrdersViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var orderRepository: OrderRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        orderRepository = mockk()
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

    @Test
    fun `prvi ulazak odmah trazi PENDING ordere sa servera`() = runTest(testDispatcher) {
        coEvery { orderRepository.getMyOrders(0, any(), "PENDING") } returns
                Resource.Success(page(listOf(order(1, "PENDING"))))

        val viewModel = MyOrdersViewModel(orderRepository)
        advanceUntilIdle()

        coVerify(exactly = 1) { orderRepository.getMyOrders(0, any(), "PENDING") }
        val state = viewModel.uiState.value
        assertEquals("PENDING", state.selectedStatus)
        assertEquals(listOf(1L), state.orders.map { it.id })
        assertFalse(state.isLoading)
    }

    @Test
    fun `promena filtera resetuje paginaciju i trazi novi status`() = runTest(testDispatcher) {
        coEvery { orderRepository.getMyOrders(0, any(), "PENDING") } returns
                Resource.Success(page(listOf(order(1, "PENDING")), last = false))
        coEvery { orderRepository.getMyOrders(0, any(), "DELIVERED") } returns
                Resource.Success(page(listOf(order(7, "DELIVERED"))))

        val viewModel = MyOrdersViewModel(orderRepository)
        advanceUntilIdle()

        viewModel.onStatusFilter("DELIVERED")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("DELIVERED", state.selectedStatus)
        assertEquals(0, state.currentPage)
        assertTrue(state.isLastPage)
        assertEquals(listOf(7L), state.orders.map { it.id })
    }

    @Test
    fun `spori stari request ne prepisuje rezultat novijeg filtera`() = runTest(testDispatcher) {
        coEvery { orderRepository.getMyOrders(0, any(), "PENDING") } coAnswers {
            delay(1_000)
            Resource.Success(page(listOf(order(1, "PENDING"))))
        }
        coEvery { orderRepository.getMyOrders(0, any(), null) } coAnswers {
            delay(10)
            Resource.Success(page(listOf(order(3, "SHIPPED"))))
        }

        val viewModel = MyOrdersViewModel(orderRepository)
        advanceTimeBy(50) // PENDING request u toku

        viewModel.onStatusFilter(null) // "ALL"
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(null, state.selectedStatus)
        assertEquals(listOf(3L), state.orders.map { it.id })
        assertFalse(state.isLoading)
    }

    @Test
    fun `neuspesan request ne ostavlja ekran u loading stanju i cuva error`() = runTest(testDispatcher) {
        coEvery { orderRepository.getMyOrders(0, any(), "PENDING") } returns
                Resource.Error("Network error")

        val viewModel = MyOrdersViewModel(orderRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.error)
    }

    @Test
    fun `klik na vec selektovan status ne salje novi request`() = runTest(testDispatcher) {
        coEvery { orderRepository.getMyOrders(0, any(), "PENDING") } returns
                Resource.Success(page(listOf(order(1, "PENDING"))))

        val viewModel = MyOrdersViewModel(orderRepository)
        advanceUntilIdle()

        viewModel.onStatusFilter("PENDING")
        viewModel.onStatusFilter("PENDING")
        advanceUntilIdle()

        coVerify(exactly = 1) { orderRepository.getMyOrders(any(), any(), any()) }
    }

    @Test
    fun `brza promena statusa salje tacno jedan request po statusu bez duplikata`() = runTest(testDispatcher) {
        coEvery { orderRepository.getMyOrders(0, any(), "PENDING") } coAnswers {
            delay(1_000)
            Resource.Success(page(listOf(order(1, "PENDING"))))
        }
        coEvery { orderRepository.getMyOrders(0, any(), "DELIVERED") } coAnswers {
            delay(500)
            Resource.Success(page(listOf(order(2, "DELIVERED"))))
        }
        coEvery { orderRepository.getMyOrders(0, any(), "SHIPPED") } coAnswers {
            delay(10)
            Resource.Success(page(listOf(order(3, "SHIPPED"))))
        }

        val viewModel = MyOrdersViewModel(orderRepository)
        advanceTimeBy(50) // PENDING request u toku
        viewModel.onStatusFilter("DELIVERED")
        advanceTimeBy(50) // DELIVERED request u toku
        viewModel.onStatusFilter("SHIPPED")
        advanceUntilIdle()

        coVerify(exactly = 1) { orderRepository.getMyOrders(0, any(), "PENDING") }
        coVerify(exactly = 1) { orderRepository.getMyOrders(0, any(), "DELIVERED") }
        coVerify(exactly = 1) { orderRepository.getMyOrders(0, any(), "SHIPPED") }

        val state = viewModel.uiState.value
        assertEquals("SHIPPED", state.selectedStatus)
        assertEquals(listOf(3L), state.orders.map { it.id })
        assertFalse(state.isLoading)
    }

    @Test
    fun `load-more salje tacno jedan request i ignorise duple pozive dok traje`() = runTest(testDispatcher) {
        val firstPage = (1L..20L).map { order(it, "PENDING") }
        coEvery { orderRepository.getMyOrders(0, any(), "PENDING") } returns
                Resource.Success(page(firstPage, last = false))
        coEvery { orderRepository.getMyOrders(1, any(), "PENDING") } coAnswers {
            delay(100)
            Resource.Success(page(listOf(order(21, "PENDING"))))
        }

        val viewModel = MyOrdersViewModel(orderRepository)
        advanceUntilIdle()

        // Višestruki UI trigger (scroll jitter) — sme proći samo jedan zahtev
        viewModel.loadMoreOrders()
        viewModel.loadMoreOrders()
        viewModel.loadMoreOrders()
        advanceUntilIdle()

        coVerify(exactly = 1) { orderRepository.getMyOrders(1, any(), "PENDING") }
        assertEquals(21, viewModel.uiState.value.orders.size)
        assertEquals(1, viewModel.uiState.value.currentPage)
    }

    @Test
    fun `load-more se ne pokrece na poslednjoj stranici`() = runTest(testDispatcher) {
        coEvery { orderRepository.getMyOrders(0, any(), "PENDING") } returns
                Resource.Success(page(listOf(order(1, "PENDING")), last = true))

        val viewModel = MyOrdersViewModel(orderRepository)
        advanceUntilIdle()

        viewModel.loadMoreOrders()
        advanceUntilIdle()

        coVerify(exactly = 0) { orderRepository.getMyOrders(1, any(), any()) }
    }

    @Test
    fun `neuspesan load-more ne ulazi u retry petlju`() = runTest(testDispatcher) {
        val firstPage = (1L..20L).map { order(it, "PENDING") }
        coEvery { orderRepository.getMyOrders(0, any(), "PENDING") } returns
                Resource.Success(page(firstPage, last = false))
        coEvery { orderRepository.getMyOrders(1, any(), "PENDING") } returns
                Resource.Error("Network error")

        val viewModel = MyOrdersViewModel(orderRepository)
        advanceUntilIdle()

        viewModel.loadMoreOrders()
        advanceUntilIdle()
        // UI trigger (promena veličine liste zbog spinner-a) pokušava ponovo —
        // error stanje mora da blokira automatski retry
        viewModel.loadMoreOrders()
        viewModel.loadMoreOrders()
        advanceUntilIdle()

        coVerify(exactly = 1) { orderRepository.getMyOrders(1, any(), "PENDING") }
        assertNotNull(viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoadingMore)
    }

    @Test
    fun `refresh salje jedan kontrolisan request i cisti error`() = runTest(testDispatcher) {
        coEvery { orderRepository.getMyOrders(0, any(), "PENDING") } returns
                Resource.Error("Network error")

        val viewModel = MyOrdersViewModel(orderRepository)
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.error)

        coEvery { orderRepository.getMyOrders(0, any(), "PENDING") } returns
                Resource.Success(page(listOf(order(1, "PENDING"))))

        viewModel.loadOrders()
        advanceUntilIdle()

        coVerify(exactly = 2) { orderRepository.getMyOrders(0, any(), "PENDING") }
        assertEquals(null, viewModel.uiState.value.error)
        assertEquals(listOf(1L), viewModel.uiState.value.orders.map { it.id })
    }
}
