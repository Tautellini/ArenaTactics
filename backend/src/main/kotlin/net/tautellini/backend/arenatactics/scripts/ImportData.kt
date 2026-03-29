package net.tautellini.backend.arenatactics.scripts

import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.FirestoreOptions
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import net.tautellini.backend.arenatactics.services.toFirestore
import net.tautellini.models.arenatactics.*
import java.io.File

private val json = Json { ignoreUnknownKeys = true }
private const val BATCH_LIMIT = 500

private val RESOURCES = "composeApp/src/commonMain/composeResources/files"

fun main() {
    val databaseId = System.getenv("FIRESTORE_DATABASE") ?: "tautellini"
    val db = FirestoreOptions.newBuilder()
        .setDatabaseId(databaseId)
        .build()
        .service
    val root = db.collection("projects").document("arenatactics")

    println("=== ArenaTactics Firestore Import ===")
    println("Project: ${db.options.projectId}")
    println()

    importAddons(root, db)
    importGameModes(root, db)
    importSpecPools(root, db)
    importClassPools(root, db)
    importCompositionSets(root, db)
    importMatchups(root, db)
    importGear(root, db)
    importLadderData(root, db)
    importSpecMeta(root, db)

    println()
    println("=== Import complete ===")
}

private fun importAddons(root: com.google.cloud.firestore.DocumentReference, db: Firestore) {
    print("Importing addons... ")
    val addons = json.decodeFromString<List<Addon>>(readResource("addons.json"))
    addons.forEach { addon ->
        root.collection("addons").document(addon.id).set(toFirestore(addon)).get()
    }
    println("${addons.size} addons")
}

private fun importGameModes(root: com.google.cloud.firestore.DocumentReference, db: Firestore) {
    print("Importing game modes... ")
    val modes = json.decodeFromString<List<GameMode>>(readResource("game_modes.json"))
    modes.forEach { mode ->
        root.collection("game_modes").document(mode.id).set(toFirestore(mode)).get()
    }
    println("${modes.size} game modes")
}

private fun importSpecPools(root: com.google.cloud.firestore.DocumentReference, db: Firestore) {
    val poolFiles = File("$RESOURCES/spec_pools").listFiles()?.filter { it.extension == "json" } ?: return
    poolFiles.forEach { file ->
        val poolId = file.nameWithoutExtension
        print("Importing spec pool '$poolId'... ")
        val specs = json.decodeFromString<List<WowSpec>>(file.readText())
        // Create the pool document first
        root.collection("spec_pools").document(poolId).set(mapOf("id" to poolId)).get()
        specs.forEach { spec ->
            root.collection("spec_pools").document(poolId).collection("specs")
                .document(spec.id).set(toFirestore(spec)).get()
        }
        println("${specs.size} specs")
    }
}

private fun importClassPools(root: com.google.cloud.firestore.DocumentReference, db: Firestore) {
    val poolFiles = File("$RESOURCES/class_pools").listFiles()?.filter { it.extension == "json" } ?: return
    poolFiles.forEach { file ->
        val poolId = file.nameWithoutExtension
        print("Importing class pool '$poolId'... ")
        val classes = json.decodeFromString<List<WowClass>>(file.readText())
        root.collection("class_pools").document(poolId).set(mapOf("id" to poolId)).get()
        classes.forEach { cls ->
            root.collection("class_pools").document(poolId).collection("classes")
                .document(cls.id).set(toFirestore(cls)).get()
        }
        println("${classes.size} classes")
    }
}

private fun importCompositionSets(root: com.google.cloud.firestore.DocumentReference, db: Firestore) {
    val setFiles = File("$RESOURCES/composition_sets").listFiles()?.filter { it.extension == "json" } ?: return
    setFiles.forEach { file ->
        val setId = file.nameWithoutExtension
        print("Importing composition set '$setId'... ")
        val compositions = json.decodeFromString<List<Composition>>(file.readText())
        root.collection("composition_sets").document(setId).set(mapOf("id" to setId)).get()
        compositions.forEach { comp ->
            root.collection("composition_sets").document(setId).collection("compositions")
                .document(comp.id).set(toFirestore(comp)).get()
        }
        println("${compositions.size} compositions")
    }
}

private fun importMatchups(root: com.google.cloud.firestore.DocumentReference, db: Firestore) {
    val matchupFiles = File("$RESOURCES/matchups").listFiles()?.filter { it.extension == "json" } ?: return
    matchupFiles.forEach { file ->
        val compositionId = file.nameWithoutExtension.removePrefix("matchups_")
        print("Importing matchups for '$compositionId'... ")
        val matchups = json.decodeFromString<List<Matchup>>(file.readText())
        root.collection("matchups").document(compositionId).set(mapOf("id" to compositionId)).get()
        matchups.forEach { matchup ->
            root.collection("matchups").document(compositionId).collection("entries")
                .document(matchup.id).set(toFirestore(matchup)).get()
        }
        println("${matchups.size} matchups")
    }
}

private fun importGear(root: com.google.cloud.firestore.DocumentReference, db: Firestore) {
    val gearFiles = File("$RESOURCES/gear").listFiles()?.filter { it.extension == "json" } ?: return
    print("Importing gear... ")
    var count = 0
    gearFiles.forEach { file ->
        val docId = file.nameWithoutExtension.removePrefix("gear_") // e.g. "mage_phase1"
        val gearPhase = json.decodeFromString<GearPhase>(file.readText())
        root.collection("gear").document(docId).set(toFirestore(gearPhase)).get()
        count++
    }
    println("$count gear phases")
}

private fun importLadderData(root: com.google.cloud.firestore.DocumentReference, db: Firestore) {
    val ladderDir = File("$RESOURCES/ladder")
    val addonDirs = ladderDir.listFiles()?.filter { it.isDirectory } ?: return

    addonDirs.forEach { addonDir ->
        val addonId = addonDir.name
        println("Importing ladder data for '$addonId'...")

        // Create ladder document
        root.collection("ladder").document(addonId).set(mapOf("id" to addonId)).get()

        // Index
        val indexFile = File(addonDir, "index.json")
        if (indexFile.exists()) {
            val indices = json.decodeFromString<List<LadderIndex>>(indexFile.readText())
            indices.forEach { idx ->
                root.collection("ladder").document(addonId).collection("index")
                    .document("${idx.region}_${idx.bracket}").set(toFirestore(idx)).get()
            }
            println("  ${indices.size} index entries")
        }

        // Snapshots (e.g. us_2v2.json, eu_3v3.json)
        val snapshotFiles = addonDir.listFiles()?.filter {
            it.extension == "json" && it.name.matches(Regex("(us|eu)_(2v2|3v3|5v5)\\.json"))
        } ?: emptyList()
        snapshotFiles.forEach { file ->
            val docId = file.nameWithoutExtension // e.g. "us_2v2"
            print("  Importing snapshot '$docId'... ")
            val snapshot = json.decodeFromString<LadderSnapshot>(file.readText())
            root.collection("ladder").document(addonId).collection("snapshots")
                .document(docId).set(toFirestore(snapshot)).get()
            println("${snapshot.topEntries.size} entries")
        }

        // Player profiles (e.g. players_us.json)
        val playerFiles = addonDir.listFiles()?.filter {
            it.name.startsWith("players_") && it.extension == "json"
        } ?: emptyList()
        playerFiles.forEach { file ->
            val region = file.nameWithoutExtension.removePrefix("players_")
            print("  Importing players '$region'... ")
            val players = json.decodeFromString<Map<String, PlayerProfile>>(file.readText())
            var count = 0
            var batch = db.batch()
            var batchCount = 0
            players.forEach { (characterId, profile) ->
                val docRef = root.collection("ladder").document(addonId).collection("players")
                    .document("${region}_$characterId")
                batch.set(docRef, toFirestore(profile))
                batchCount++
                count++
                if (batchCount >= BATCH_LIMIT) {
                    batch.commit().get()
                    batch = db.batch()
                    batchCount = 0
                }
            }
            if (batchCount > 0) {
                batch.commit().get()
            }
            println("$count players")
        }

        // Items (e.g. items_us.json)
        val itemFiles = addonDir.listFiles()?.filter {
            it.name.startsWith("items_") && it.extension == "json"
        } ?: emptyList()
        itemFiles.forEach { file ->
            val region = file.nameWithoutExtension.removePrefix("items_")
            print("  Importing items '$region'... ")
            val items = json.decodeFromString<Map<String, ItemTooltipData>>(file.readText())
            var count = 0
            var batch = db.batch()
            var batchCount = 0
            items.forEach { (itemId, item) ->
                val docRef = root.collection("ladder").document(addonId).collection("items")
                    .document("${region}_$itemId")
                batch.set(docRef, toFirestore(item))
                batchCount++
                count++
                if (batchCount >= BATCH_LIMIT) {
                    batch.commit().get()
                    batch = db.batch()
                    batchCount = 0
                }
            }
            if (batchCount > 0) {
                batch.commit().get()
            }
            println("$count items")
        }
    }
}

private fun importSpecMeta(root: com.google.cloud.firestore.DocumentReference, db: Firestore) {
    println("Computing and importing SpecMeta...")
    val specFile = File("$RESOURCES/spec_pools/tbc.json")
    if (!specFile.exists()) return
    val specs = json.decodeFromString<List<WowSpec>>(specFile.readText())

    // Load all player profiles
    val allPlayers = mutableListOf<PlayerProfile>()
    val ladderDir = File("$RESOURCES/ladder")
    ladderDir.listFiles()?.filter { it.isDirectory }?.forEach { addonDir ->
        addonDir.listFiles()?.filter { it.name.startsWith("players_") && it.extension == "json" }?.forEach { file ->
            val players = json.decodeFromString<Map<String, PlayerProfile>>(file.readText())
            allPlayers.addAll(players.values)
        }
    }

    var count = 0
    specs.forEach { spec ->
        val matching = allPlayers.filter { it.specId == spec.id }
        if (matching.isEmpty()) return@forEach

        val meta = computeSpecMetaServer(spec.id, matching)
        root.collection("spec_meta").document(spec.id).set(toFirestore(meta)).get()
        count++
        println("  ${spec.id}: ${meta.sampleSize} players")
    }
    println("$count spec metas")
}

private val SLOT_ORDER = listOf(
    "HEAD", "NECK", "SHOULDER", "BACK", "CHEST", "WRIST",
    "HANDS", "WAIST", "LEGS", "FEET",
    "FINGER_1", "FINGER_2", "TRINKET_1", "TRINKET_2",
    "MAIN_HAND", "OFF_HAND", "RANGED"
)

private fun computeSpecMetaServer(specId: String, players: List<PlayerProfile>): SpecMeta {
    val total = players.size
    if (total == 0) return SpecMeta(specId, 0, emptyList(), emptyList())

    val slotItems = mutableMapOf<String, MutableList<Pair<Int, String>>>()
    val slotQualities = mutableMapOf<Pair<String, Int>, String?>()
    val slotEnchants = mutableMapOf<String, MutableList<String>>()

    for (player in players) {
        for (item in player.equipment) {
            slotItems.getOrPut(item.slot) { mutableListOf() }.add(item.itemId to item.name)
            slotQualities[item.slot to item.itemId] = item.quality
            if (!item.enchant.isNullOrEmpty()) {
                slotEnchants.getOrPut(item.slot) { mutableListOf() }.add(item.enchant!!)
            }
        }
    }

    val slotBreakdowns = SLOT_ORDER.mapNotNull { slot ->
        val items = slotItems[slot] ?: return@mapNotNull null
        val itemCounts = items.groupingBy { it }.eachCount()
            .entries.sortedByDescending { it.value }
            .take(10)
            .map { (pair, count) ->
                ItemUsage(pair.first, pair.second, slotQualities[slot to pair.first], count, (count * 1000L / total) / 10.0)
            }
        val enchants = slotEnchants[slot]?.let { list ->
            list.groupingBy { it }.eachCount()
                .entries.sortedByDescending { it.value }
                .take(5)
                .map { (name, count) -> EnchantUsage(name, count, (count * 1000L / total) / 10.0) }
        } ?: emptyList()
        SlotBreakdown(slot, itemCounts, enchants)
    }

    data class BuildData(val trees: List<TalentTreePair>, val count: Int, val talents: List<TalentSelection>)
    val buildCounts = mutableMapOf<String, BuildData>()
    for (player in players) {
        val activeGroup = player.talentGroups.firstOrNull { it.isActive } ?: continue
        val trees = activeGroup.specializations.map { TalentTreePair(it.treeName, it.spentPoints) }
        val label = trees.joinToString(" / ") { "${it.treeName} (${it.spentPoints})" }
        val existing = buildCounts[label]
        if (existing != null) {
            buildCounts[label] = existing.copy(count = existing.count + 1)
        } else {
            val allTalents = activeGroup.specializations.flatMap { it.talents }
            buildCounts[label] = BuildData(trees, 1, allTalents)
        }
    }

    val talentBuilds = buildCounts.entries
        .sortedByDescending { it.value.count }
        .take(5)
        .map { (label, data) ->
            TalentBuildEntry(label, data.trees, data.count, (data.count * 1000L / total) / 10.0, data.talents)
        }

    return SpecMeta(specId, total, slotBreakdowns, talentBuilds)
}

private fun readResource(path: String): String = File("$RESOURCES/$path").readText()
