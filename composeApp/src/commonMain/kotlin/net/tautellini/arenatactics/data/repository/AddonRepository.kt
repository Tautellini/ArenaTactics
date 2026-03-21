package net.tautellini.arenatactics.data.repository

import arenatactics.composeapp.generated.resources.Res
import net.tautellini.arenatactics.data.model.Addon

internal fun parseAddons(jsonString: String): List<Addon> =
    appJson.decodeFromString(jsonString)

class AddonRepository {
    suspend fun getAll(): List<Addon> {
        val bytes = Res.readBytes("files/addons.json")
        return parseAddons(bytes.decodeToString())
    }

    suspend fun getById(id: String): Addon? = getAll().find { it.id == id }
}
