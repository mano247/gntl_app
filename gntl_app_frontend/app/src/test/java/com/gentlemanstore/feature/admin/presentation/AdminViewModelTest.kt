package com.gentlemanstore.feature.admin.presentation

import com.gentlemanstore.core.util.ErrorType
import com.gentlemanstore.core.util.Resource
import com.gentlemanstore.feature.admin.data.dto.PagedUserResponse
import com.gentlemanstore.feature.admin.data.dto.UserListResponse
import com.gentlemanstore.feature.admin.domain.AdminRepository
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AdminViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var adminRepository: AdminRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        adminRepository = mockk()
        coEvery { adminRepository.getAllUsers(0, false) } returns Resource.Success(
            page(listOf(user(1, "CUSTOMER"), user(2, "EMPLOYEE")))
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun user(id: Long, role: String) = UserListResponse(
        id = id,
        email = "user$id@test.com",
        firstName = "First$id",
        lastName = "Last$id",
        phoneNumber = null,
        createdAt = "2026-07-14T00:00:00",
        addresses = null,
        role = role,
        deleted = false
    )

    private fun page(users: List<UserListResponse>) = PagedUserResponse(
        content = users,
        totalPages = 1,
        totalElements = users.size.toLong(),
        last = true,
        first = true,
        number = 0,
        size = 20,
        numberOfElements = users.size
    )

    private fun createViewModel(): AdminViewModel {
        val viewModel = AdminViewModel(adminRepository)
        return viewModel
    }

    @Test
    fun `uspesna promena role azurira korisnika u listi i daje success poruku`() = runTest(testDispatcher) {
        coEvery { adminRepository.changeUserRole(1, "MANAGER") } returns
                Resource.Success(user(1, "MANAGER"))

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.changeUserRole(1, "MANAGER")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("MANAGER", state.users.first { it.id == 1L }.role)
        assertEquals("Role changed successfully!", state.successMessage)
        assertNull(state.updatingUserId)
        coVerify(exactly = 1) { adminRepository.changeUserRole(1, "MANAGER") }
    }

    @Test
    fun `ista rola se ne salje na server`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.changeUserRole(1, "CUSTOMER")
        advanceUntilIdle()

        coVerify(exactly = 0) { adminRepository.changeUserRole(any(), any()) }
        assertEquals("User already has the CUSTOMER role", viewModel.uiState.value.error)
    }

    @Test
    fun `dupli submit se ignorise dok je prethodna promena u toku`() = runTest(testDispatcher) {
        coEvery { adminRepository.changeUserRole(1, "MANAGER") } coAnswers {
            delay(1_000)
            Resource.Success(user(1, "MANAGER"))
        }

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.changeUserRole(1, "MANAGER")
        advanceTimeBy(50)
        viewModel.changeUserRole(1, "MANAGER") // dupli tap
        advanceUntilIdle()

        coVerify(exactly = 1) { adminRepository.changeUserRole(1, "MANAGER") }
    }

    @Test
    fun `backend greska prikazuje konkretnu poruku i ne menja listu`() = runTest(testDispatcher) {
        coEvery { adminRepository.changeUserRole(1, "MANAGER") } returns
                Resource.Error("Role not found", ErrorType.NOT_FOUND)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.changeUserRole(1, "MANAGER")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Role not found", state.error)
        assertEquals("CUSTOMER", state.users.first { it.id == 1L }.role)
        assertNull(state.updatingUserId)
    }

    @Test
    fun `forbidden greska se prikazuje kroz error poruku`() = runTest(testDispatcher) {
        coEvery { adminRepository.changeUserRole(2, "ADMIN") } returns
                Resource.Error("You don't have permission to do this.", ErrorType.FORBIDDEN)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.changeUserRole(2, "ADMIN")
        advanceUntilIdle()

        assertEquals("You don't have permission to do this.", viewModel.uiState.value.error)
    }
}
