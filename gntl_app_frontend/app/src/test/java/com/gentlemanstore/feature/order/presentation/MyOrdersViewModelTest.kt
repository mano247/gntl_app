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
}
