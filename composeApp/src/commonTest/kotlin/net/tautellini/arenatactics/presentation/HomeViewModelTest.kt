package net.tautellini.arenatactics.presentation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.tautellini.arenatactics.data.model.Addon
import net.tautellini.arenatactics.data.model.GameMode
import net.tautellini.arenatactics.data.repository.AddonRepository
import net.tautellini.arenatactics.data.repository.GameModeRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

// ─── Fake repositories ───────────────────────────────────────────────────────

private val ADDON_TBC = Addon(
    id = "tbc_anniversary",
    name = "TBC Anniversary",
    description = "The Burning Crusade",
    iconName = "inv_misc_tournaments_noticebc",
    specPoolId = "tbc_specs",
    classPoolId = "tbc_classes",
    hasData = true
)

private val GAMEMODE_2V2 = GameMode(
    id = "tbc_anniversary_2v2",
    name = "2v2",
    description = "2v2 arena",
    teamSize = 2,
    addonId = "tbc_anniversary",
    compositionSetId = "tbc_anniversary_2v2_comps",
    iconName = "achievement_arena_2v2_7",
    hasData = true
)

private class FakeAddonRepository(
    private val result: Result<List<Addon>>
) : AddonRepository() {
    override suspend fun getAll(): List<Addon> = result.getOrThrow()
}

private class FakeGameModeRepository(
    private val result: Result<List<GameMode>>
) : GameModeRepository() {
    override suspend fun getByAddon(addonId: String): List<GameMode> = result.getOrThrow()
}

// ─── Tests ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // 1. Initial state transitions Loading → Success
    @Test
    fun initialStateTransitionsToSuccess() = runTest {
        val vm = HomeViewModel(
            addonRepository = FakeAddonRepository(Result.success(listOf(ADDON_TBC))),
            gameModeRepository = FakeGameModeRepository(Result.success(listOf(GAMEMODE_2V2)))
        )

        val state = vm.state.value
        assertIs<HomeState.Success>(state)
        assertEquals(listOf(ADDON_TBC), state.addons)
        assertIs<GameModeRowState.Idle>(state.gameModeRow)
    }

    // 2. loadGameModes transitions gameModeRow Loading → Ready
    @Test
    fun loadGameModesTransitionsToReady() = runTest {
        val vm = HomeViewModel(
            addonRepository = FakeAddonRepository(Result.success(listOf(ADDON_TBC))),
            gameModeRepository = FakeGameModeRepository(Result.success(listOf(GAMEMODE_2V2)))
        )

        vm.loadGameModes("tbc_anniversary")

        val state = vm.state.value
        assertIs<HomeState.Success>(state)
        val row = state.gameModeRow
        assertIs<GameModeRowState.Ready>(row)
        assertEquals(listOf(GAMEMODE_2V2), row.modes)
    }

    // 3. loadGameModes with same addonId when already Ready — no-op
    @Test
    fun loadGameModesNoOpWhenAlreadyReadySameAddon() = runTest {
        var fetchCount = 0
        val fakeGameModeRepo = object : GameModeRepository() {
            override suspend fun getByAddon(addonId: String): List<GameMode> {
                fetchCount++
                return listOf(GAMEMODE_2V2)
            }
        }
        val vm = HomeViewModel(
            addonRepository = FakeAddonRepository(Result.success(listOf(ADDON_TBC))),
            gameModeRepository = fakeGameModeRepo
        )

        vm.loadGameModes("tbc_anniversary")
        assertEquals(1, fetchCount)

        vm.loadGameModes("tbc_anniversary")
        assertEquals(1, fetchCount, "Should not re-fetch for same addonId when already Ready")
    }

    // 4. loadGameModes with different addonId when already Ready — reloads
    @Test
    fun loadGameModesReloadsForDifferentAddon() = runTest {
        var fetchCount = 0
        val fakeGameModeRepo = object : GameModeRepository() {
            override suspend fun getByAddon(addonId: String): List<GameMode> {
                fetchCount++
                return listOf(GAMEMODE_2V2)
            }
        }
        val vm = HomeViewModel(
            addonRepository = FakeAddonRepository(Result.success(listOf(ADDON_TBC))),
            gameModeRepository = fakeGameModeRepo
        )

        vm.loadGameModes("tbc_anniversary")
        assertEquals(1, fetchCount)

        vm.loadGameModes("other_addon")
        assertEquals(2, fetchCount, "Should re-fetch for different addonId")

        val state = vm.state.value
        assertIs<HomeState.Success>(state)
        assertIs<GameModeRowState.Ready>(state.gameModeRow)
    }

    // 5. resetGameModes sets gameModeRow to Idle and clears lastLoadedAddonId
    @Test
    fun resetGameModesSetsIdle() = runTest {
        val vm = HomeViewModel(
            addonRepository = FakeAddonRepository(Result.success(listOf(ADDON_TBC))),
            gameModeRepository = FakeGameModeRepository(Result.success(listOf(GAMEMODE_2V2)))
        )

        vm.loadGameModes("tbc_anniversary")
        assertIs<GameModeRowState.Ready>((vm.state.value as HomeState.Success).gameModeRow)

        vm.resetGameModes()

        val state = vm.state.value
        assertIs<HomeState.Success>(state)
        assertIs<GameModeRowState.Idle>(state.gameModeRow)
    }

    // 5b. After resetGameModes, same addonId should reload (lastLoadedAddonId was cleared)
    @Test
    fun afterResetSameAddonReloads() = runTest {
        var fetchCount = 0
        val fakeGameModeRepo = object : GameModeRepository() {
            override suspend fun getByAddon(addonId: String): List<GameMode> {
                fetchCount++
                return listOf(GAMEMODE_2V2)
            }
        }
        val vm = HomeViewModel(
            addonRepository = FakeAddonRepository(Result.success(listOf(ADDON_TBC))),
            gameModeRepository = fakeGameModeRepo
        )

        vm.loadGameModes("tbc_anniversary")
        assertEquals(1, fetchCount)

        vm.resetGameModes()
        vm.loadGameModes("tbc_anniversary")
        assertEquals(2, fetchCount, "Should reload after reset even for same addonId")
    }

    // 6. Error state when addon load fails
    @Test
    fun addonLoadFailureSetsErrorState() = runTest {
        val vm = HomeViewModel(
            addonRepository = FakeAddonRepository(Result.failure(RuntimeException("Network error"))),
            gameModeRepository = FakeGameModeRepository(Result.success(listOf(GAMEMODE_2V2)))
        )

        val state = vm.state.value
        assertIs<HomeState.Error>(state)
        assertEquals("Network error", state.message)
    }

    // 7. Error state when game mode load fails
    @Test
    fun gameModeLoadFailureSetsErrorRow() = runTest {
        val vm = HomeViewModel(
            addonRepository = FakeAddonRepository(Result.success(listOf(ADDON_TBC))),
            gameModeRepository = FakeGameModeRepository(Result.failure(RuntimeException("Game mode error")))
        )

        vm.loadGameModes("tbc_anniversary")

        val state = vm.state.value
        assertIs<HomeState.Success>(state)
        val row = state.gameModeRow
        assertIs<GameModeRowState.Error>(row)
        assertEquals("Game mode error", row.message)
    }

    // 8. After a failed loadGameModes, same addonId can be retried (lastLoadedAddonId not set on error)
    @Test
    fun loadGameModesCanRetryAfterError() = runTest {
        var callCount = 0
        val fakeGameModeRepo = object : GameModeRepository() {
            override suspend fun getByAddon(addonId: String): List<GameMode> {
                callCount++
                if (callCount == 1) throw RuntimeException("Transient error")
                return listOf(GAMEMODE_2V2)
            }
        }
        val vm = HomeViewModel(
            addonRepository = FakeAddonRepository(Result.success(listOf(ADDON_TBC))),
            gameModeRepository = fakeGameModeRepo
        )

        // First call: fails
        vm.loadGameModes("tbc_anniversary")
        assertIs<GameModeRowState.Error>((vm.state.value as HomeState.Success).gameModeRow)

        // Second call with same addonId: should succeed because lastLoadedAddonId was not set after error
        vm.loadGameModes("tbc_anniversary")
        val state = vm.state.value
        assertIs<HomeState.Success>(state)
        assertIs<GameModeRowState.Ready>(state.gameModeRow)
        assertEquals(listOf(GAMEMODE_2V2), state.gameModeRow.modes)
    }
}
