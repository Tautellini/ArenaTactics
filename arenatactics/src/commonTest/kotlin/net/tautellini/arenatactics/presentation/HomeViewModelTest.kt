package net.tautellini.arenatactics.presentation

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.tautellini.models.arenatactics.Addon
import net.tautellini.models.arenatactics.GameMode
import net.tautellini.arenatactics.data.api.ArenaApi
import net.tautellini.arenatactics.data.repository.AddonRepository
import net.tautellini.arenatactics.data.repository.GameModeRepository
import net.tautellini.arenatactics.data.repository.LadderRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

private val ADDON_TBC = Addon(
    id = "tbc_anniversary",
    name = "TBC Anniversary",
    shortName = "TBC",
    description = "The Burning Crusade",
    accentColor = "#4FC978",
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

private fun createMockApi(
    addons: List<Addon> = listOf(ADDON_TBC),
    gameModes: List<GameMode> = listOf(GAMEMODE_2V2)
): ArenaApi {
    val mockEngine = MockEngine { request ->
        val path = request.url.encodedPath
        when {
            path.endsWith("/addons") -> respond(
                Json.encodeToString(addons),
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
            path.endsWith("/game-modes") -> respond(
                Json.encodeToString(gameModes),
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
            path.contains("/ladder/") && path.endsWith("/index") -> respond(
                "[]",
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
            else -> respond("[]", headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))
        }
    }
    val client = HttpClient(mockEngine) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }
    return ArenaApi(client)
}

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

    @Test
    fun initialStateTransitionsToSuccess() = runTest {
        val api = createMockApi()
        val vm = HomeViewModel(
            ladderRepository = LadderRepository(api),
            addonRepository = AddonRepository(api),
            gameModeRepository = GameModeRepository(api)
        )

        val state = vm.state.value
        assertIs<HomeState.Success>(state)
        assertEquals(listOf(ADDON_TBC), state.addons)
        assertIs<GameModeRowState.Idle>(state.gameModeRow)
    }

    @Test
    fun loadGameModesTransitionsToReady() = runTest {
        val api = createMockApi()
        val vm = HomeViewModel(
            ladderRepository = LadderRepository(api),
            addonRepository = AddonRepository(api),
            gameModeRepository = GameModeRepository(api)
        )

        vm.loadGameModes("tbc_anniversary")

        val state = vm.state.value
        assertIs<HomeState.Success>(state)
        val row = state.gameModeRow
        assertIs<GameModeRowState.Ready>(row)
        assertEquals(listOf(GAMEMODE_2V2), row.modes)
    }
}
