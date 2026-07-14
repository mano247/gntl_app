package com.gentlemanstore.feature.product.presentation

import com.gentlemanstore.core.util.ErrorType
import com.gentlemanstore.core.util.Resource
import com.gentlemanstore.feature.product.data.dto.PagedProductResponse
import com.gentlemanstore.feature.product.data.dto.ProductResponse
import com.gentlemanstore.feature.product.domain.ProductRepository
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Employee Products: ACTIVE / DELETED / ALL filter, restore, paginacija i
 * zastita od race condition-a pri promeni filtera.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProductViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var productRepository: ProductRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        productRepository = mockk()
        coEvery { productRepository.getCategories() } returns Resource.Error("not under test")
        // Default (customer/ACTIVE) zahtev - status se NE salje
        coEvery {
            productRepository.getProducts(page = 0, category = null, search = null, status = null)
        } returns Resource.Success(page(listOf(product(1), product(2))))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun product(id: Long, deleted: Boolean = false) = ProductResponse(
        id = id,
        sku = "SKU$id",
        name = "Product $id",
        description = "Desc",
        price = 1000.0,
        categoryName = "Suits",
        sizes = emptyList(),
        imageUrls = emptyList(),
        tags = emptyList(),
        discountPercentage = null,
        deleted = deleted
    )

    private fun page(products: List<ProductResponse>, last: Boolean = true) = PagedProductResponse(
        content = products,
        totalPages = 1,
        totalElements = products.size.toLong(),
        last = last,
        first = true,
        number = 0,
        size = 20,
        numberOfElements = products.size
    )

    private fun createViewModel() = ProductViewModel(productRepository)

    @Test
    fun `podrazumevani ACTIVE filter ne salje status parametar (customer katalog nepromenjen)`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        coVerify { productRepository.getProducts(page = 0, category = null, search = null, status = null) }
        assertEquals("ACTIVE", viewModel.listUiState.value.statusFilter)
        assertEquals(listOf(1L, 2L), viewModel.listUiState.value.products.map { it.id })
    }

    @Test
    fun `DELETED filter resetuje stranicu, cisti listu i salje status serveru`() = runTest(testDispatcher) {
        coEvery {
            productRepository.getProducts(page = 0, category = null, search = null, status = "DELETED")
        } returns Resource.Success(page(listOf(product(9, deleted = true))))

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onStatusFilterChange("DELETED")
        advanceUntilIdle()

        val state = viewModel.listUiState.value
        assertEquals("DELETED", state.statusFilter)
        assertEquals(0, state.currentPage)
        assertEquals(listOf(9L), state.products.map { it.id })
        assertTrue(state.products.all { it.deleted })
    }

    @Test
    fun `isti filter ne pokrece novo ucitavanje`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onStatusFilterChange("ACTIVE")
        advanceUntilIdle()

        coVerify(exactly = 1) {
            productRepository.getProducts(page = 0, category = null, search = null, status = null)
        }
    }

    @Test
    fun `spori stari zahtev ne prepisuje rezultat novijeg filtera`() = runTest(testDispatcher) {
        coEvery {
            productRepository.getProducts(page = 0, category = null, search = null, status = null)
        } coAnswers {
            delay(1_000)
            Resource.Success(page(listOf(product(1))))
        }
        coEvery {
            productRepository.getProducts(page = 0, category = null, search = null, status = "DELETED")
        } coAnswers {
            delay(10)
            Resource.Success(page(listOf(product(9, deleted = true))))
        }

        val viewModel = createViewModel()
        advanceTimeBy(50) // ACTIVE zahtev visi u delay-u

        viewModel.onStatusFilterChange("DELETED")
        advanceUntilIdle()

        val state = viewModel.listUiState.value
        assertEquals(listOf(9L), state.products.map { it.id })
        assertFalse(state.isLoading)
    }

    @Test
    fun `filter radi zajedno sa pretragom i paginacijom`() = runTest(testDispatcher) {
        coEvery {
            productRepository.getProducts(page = 0, category = null, search = "suit", status = "DELETED")
        } returns Resource.Success(page(listOf(product(5, deleted = true)), last = false))
        coEvery {
            productRepository.getProducts(page = 1, category = null, search = "suit", status = "DELETED")
        } returns Resource.Success(page(listOf(product(6, deleted = true)), last = true))
        coEvery {
            productRepository.getProducts(page = 0, category = null, search = "suit", status = null)
        } returns Resource.Success(page(emptyList()))

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onSearchQueryChange("suit")
        advanceUntilIdle()
        viewModel.onStatusFilterChange("DELETED")
        advanceUntilIdle()
        viewModel.loadMoreProducts()
        advanceUntilIdle()

        val state = viewModel.listUiState.value
        assertEquals(listOf(5L, 6L), state.products.map { it.id })
        assertEquals(1, state.currentPage)
        assertTrue(state.isLastPage)
    }

    // ---------- restore ----------

    @Test
    fun `restore u DELETED prikazu uklanja proizvod iz liste`() = runTest(testDispatcher) {
        coEvery {
            productRepository.getProducts(page = 0, category = null, search = null, status = "DELETED")
        } returns Resource.Success(page(listOf(product(9, deleted = true))))
        coEvery { productRepository.restoreProduct(9) } returns Resource.Success(product(9, deleted = false))

        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onStatusFilterChange("DELETED")
        advanceUntilIdle()

        viewModel.restoreProduct(9)
        advanceUntilIdle()

        assertTrue(viewModel.listUiState.value.products.none { it.id == 9L })
        assertNull(viewModel.mutationUiState.value.restoringId)
        coVerify(exactly = 1) { productRepository.restoreProduct(9) }
    }

    @Test
    fun `restore u ALL prikazu oznacava proizvod kao aktivan`() = runTest(testDispatcher) {
        coEvery {
            productRepository.getProducts(page = 0, category = null, search = null, status = "ALL")
        } returns Resource.Success(page(listOf(product(1), product(9, deleted = true))))
        coEvery { productRepository.restoreProduct(9) } returns Resource.Success(product(9, deleted = false))

        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onStatusFilterChange("ALL")
        advanceUntilIdle()

        viewModel.restoreProduct(9)
        advanceUntilIdle()

        val restored = viewModel.listUiState.value.products.first { it.id == 9L }
        assertFalse("proizvod je vracen kao aktivan, ne kreira se nov", restored.deleted)
        assertEquals("SKU9", restored.sku)
    }

    @Test
    fun `backend greska pri restore prikazuje konkretnu poruku`() = runTest(testDispatcher) {
        coEvery {
            productRepository.getProducts(page = 0, category = null, search = null, status = "DELETED")
        } returns Resource.Success(page(listOf(product(9, deleted = true))))
        coEvery { productRepository.restoreProduct(9) } returns
                Resource.Error("Product is not deleted", ErrorType.BAD_REQUEST)

        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onStatusFilterChange("DELETED")
        advanceUntilIdle()

        viewModel.restoreProduct(9)
        advanceUntilIdle()

        assertEquals("Product is not deleted", viewModel.mutationUiState.value.error)
        assertNull(viewModel.mutationUiState.value.restoringId)
        assertTrue("proizvod ostaje u listi", viewModel.listUiState.value.products.any { it.id == 9L })
    }
}
