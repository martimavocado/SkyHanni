package at.hannibal2.skyhanni.features.misc.customscoreboard

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.MaxwellAPI
import at.hannibal2.skyhanni.data.MayorElection
import at.hannibal2.skyhanni.data.PartyAPI
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.data.SlayerAPI
import at.hannibal2.skyhanni.features.misc.customscoreboard.CustomScoreboardUtils.getTitleAndFooterAlignment
import at.hannibal2.skyhanni.mixins.hooks.replaceString
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.LorenzUtils.inDungeons
import at.hannibal2.skyhanni.utils.LorenzUtils.nextAfter
import at.hannibal2.skyhanni.utils.RenderUtils.AlignmentEnum
import at.hannibal2.skyhanni.utils.StringUtils.firstLetterUppercase
import at.hannibal2.skyhanni.utils.TimeUnit
import at.hannibal2.skyhanni.utils.TimeUtils
import at.hannibal2.skyhanni.utils.TimeUtils.formatted
import io.github.moulberry.notenoughupdates.util.SkyBlockTime
import java.util.function.Supplier

private val config get() = SkyHanniMod.feature.gui.customScoreboard

// Stats / Numbers
var purse = "0"
var motes = "0"
var bank = "0"
var bits = "0"
var copper = "0"
var gems = "0"
var location = "None"
var lobbyCode = "None"
var heat = "0"
var mithrilPowder = "0"
var gemstonePowder = "0"
var extraLines = listOf<String>()

val extraObjectiveLines = listOf("§7(§e", "§f Mages", "§f Barbarians")
var amountOfExtraLines = 0

enum class ScoreboardElements(
    private val displayPair: Supplier<List<Pair<String, AlignmentEnum>>>,
    private val showWhen: () -> Boolean,
    private val configLine: String
) {
    TITLE({ getTitleDisplayPair() }, { true }, "§6§lSKYBLOCK"),
    PROFILE({ getProfileDisplayPair() }, { true }, "§7♲ Blueberry"),
    PURSE({ getPurseDisplayPair() }, { getPurseShowWhen() }, "Purse: §652,763,737"),
    MOTES({ getMotesDisplayPair() }, { getMotesShowWhen() }, "Motes: §d64,647"),
    BANK({ getBankDisplayPair() }, { getBankShowWhen() }, "Bank: §6249M"),
    BITS({ getBitsDisplayPair() }, { getBitsShowWhen() }, "Bits: §b59,264"),
    COPPER({ getCopperDisplayPair() }, { getCopperShowWhen() }, "Copper: §c23,495"),
    GEMS({ getGemsDisplayPair() }, { getGemsShowWhen() }, "Gems: §a57,873"),
    HEAT({ getHeatDisplayPair() }, { getHeatShowWhen() }, "Heat: §c♨ 0"),
    EMPTY_LINE({ getEmptyLineDisplayPair() }, { true }, ""),
    ISLAND({ getIslandDisplayPair() }, { true }, "§7㋖ §aHub"),
    LOCATION({ getLocationDisplayPair() }, { true }, "§7⏣ §bVillage"),
    VISITING({ getVisitDisplayPair() }, { getVisitShowWhen() }, " §a✌ §7(§a1§7/6)"),
    DATE({ getDateDisplayPair() }, { true }, "Late Summer 11th"),
    TIME({ getTimeDisplayPair() }, { true }, "§710:40pm"),
    LOBBY_CODE({ getLobbyDisplayPair() }, { true }, "§8m77CK"),
    POWER({ getPowerDisplayPair() }, { getPowerShowWhen() }, "Power: Sighted"),
    COOKIE({ getCookieDisplayPair() }, { getCookieShowWhen() }, "§d§lCookie Buff\n §f3days, 17hours"),
    EMPTY_LINE2({ getEmptyLineDisplayPair() }, { true }, ""),
    OBJECTIVE({ getObjectiveDisplayPair() }, { true }, "Objective:\n§eUpdate SkyHanni"),
    SLAYER(
        { getSlayerDisplayPair() },
        { getSlayerShowWhen() },
        "§cSlayer\n §7- §cVoidgloom Seraph III\n §7- §e12§7/§c120 §7Kills"
    ),
    EMPTY_LINE3({ getEmptyLineDisplayPair() }, { true }, ""),
    POWDER(
        { getPowderDisplayPair() },
        { getPowderShowWhen() },
        "§9§lPowder\n §7- §fMithril: §254,646\n §7- §fGemstone: §d51,234"
    ),
    EVENTS({ getEventsDisplayPair() }, { getEventsShowWhen() }, "§7Wide Range of Events\n§7(too much for this here)"),
    MAYOR(
        { getMayorDisplayPair() },
        { getMayorShowWhen() },
        "§2Diana:\n §7- §eLucky!\n §7- §eMythological Ritual\n §7- §ePet XP Buff"
    ),
    PARTY(
        { getPartyDisplayPair() },
        { getPartyShowWhen() },
        "§9§lParty (4):\n §7- §fhannibal2\n §7- §fMoulberry\n §7- §fVahvl\n §7- §fJ10a1n15"
    ),
    FOOTER({ getFooterDisplayPair() }, { true }, "§ewww.hypixel.net"),
    EXTRA({ getExtraDisplayPair() }, { getExtraShowWhen() }, "§7Extra lines the mod is not detecting"),
    ;

    override fun toString(): String {
        return configLine
    }

    fun getPair(): List<Pair<String, AlignmentEnum>> {
        return try {
            displayPair.get()
        } catch (e: NoSuchElementException) {
            listOf("<hidden>" to AlignmentEnum.LEFT)
        }
    }

    fun isVisible(): Boolean {
        if (!config.informationFilteringConfig.hideIrrelevantLines) return true
        return showWhen()
    }
}


private fun getTitleDisplayPair() = when (config.displayConfig.useHypixelTitleAnimation) {
    true -> listOf(ScoreboardData.objectiveTitle to getTitleAndFooterAlignment())
    false -> listOf(config.displayConfig.customTitle.get().toString().replace("&", "§") to getTitleAndFooterAlignment())
}

private fun getProfileDisplayPair() =
    listOf(CustomScoreboardUtils.getProfileTypeSymbol() + HypixelData.profileName.firstLetterUppercase() to AlignmentEnum.LEFT)

private fun getPurseDisplayPair() = when {
    config.informationFilteringConfig.hideEmptyLines && purse == "0" -> listOf("<hidden>")
    config.displayConfig.displayNumbersFirst -> listOf("§6$purse Purse")
    else -> listOf("Purse: §6$purse")
}.map { it to AlignmentEnum.LEFT }

private fun getPurseShowWhen() = !listOf(IslandType.THE_RIFT).contains(HypixelData.skyBlockIsland)

private fun getMotesDisplayPair() = when {
    config.informationFilteringConfig.hideEmptyLines && motes == "0" -> listOf("<hidden>")
    config.displayConfig.displayNumbersFirst -> listOf("§d$motes Motes")
    else -> listOf("Motes: §d$motes")
}.map { it to AlignmentEnum.LEFT }

private fun getMotesShowWhen() = listOf(IslandType.THE_RIFT).contains(HypixelData.skyBlockIsland)

private fun getBankDisplayPair() = when {
    config.informationFilteringConfig.hideEmptyLines && bank == "0" -> listOf("<hidden>")
    config.displayConfig.displayNumbersFirst -> listOf("§6$bank Bank")
    else -> listOf("Bank: §6$bank")
}.map { it to AlignmentEnum.LEFT }

private fun getBankShowWhen() = !listOf(IslandType.THE_RIFT).contains(HypixelData.skyBlockIsland)

private fun getBitsDisplayPair() = when {
    config.informationFilteringConfig.hideEmptyLines && bits == "0" -> listOf("<hidden>")
    config.displayConfig.displayNumbersFirst -> listOf("§b$bits Bits")
    else -> listOf("Bits: §b$bits")
}.map { it to AlignmentEnum.LEFT }

private fun getBitsShowWhen() = !listOf(IslandType.THE_RIFT, IslandType.CATACOMBS).contains(HypixelData.skyBlockIsland)

private fun getCopperDisplayPair() = when {
    config.informationFilteringConfig.hideEmptyLines && copper == "0" -> listOf("<hidden>")
    config.displayConfig.displayNumbersFirst -> listOf("§c$copper Copper")
    else -> listOf("Copper: §c$copper")
}.map { it to AlignmentEnum.LEFT }

private fun getCopperShowWhen() = listOf(IslandType.GARDEN).contains(HypixelData.skyBlockIsland)

private fun getGemsDisplayPair() = when {
    config.informationFilteringConfig.hideEmptyLines && gems == "0" -> listOf("<hidden>")
    config.displayConfig.displayNumbersFirst -> listOf("§a$gems Gems")
    else -> listOf("Gems: §a$gems")
}.map { it to AlignmentEnum.LEFT }

private fun getGemsShowWhen() = !listOf(IslandType.THE_RIFT, IslandType.CATACOMBS).contains(HypixelData.skyBlockIsland)

private fun getHeatDisplayPair() = when {
    config.informationFilteringConfig.hideEmptyLines && heat == "§c♨ 0" -> listOf("<hidden>")
    config.displayConfig.displayNumbersFirst -> listOf(if (heat == "§c♨ 0") "§c♨ 0 Heat" else "$heat Heat")
    else -> listOf(if (heat == "§c♨ 0") "Heat: §c♨ 0" else "Heat: $heat")
}.map { it to AlignmentEnum.LEFT }

private fun getHeatShowWhen() = listOf(IslandType.CRYSTAL_HOLLOWS).contains(HypixelData.skyBlockIsland)

private fun getEmptyLineDisplayPair() = listOf("<empty>" to AlignmentEnum.LEFT)

private fun getIslandDisplayPair() =
    listOf("§7㋖ §a" + HypixelData.skyBlockIsland.toString().split("_")
        .joinToString(" ") { it.firstLetterUppercase() } to AlignmentEnum.LEFT)

private fun getLocationDisplayPair() =
    listOf((replaceString(location)?.trim() ?: "<hidden>") to AlignmentEnum.LEFT)


private fun getVisitDisplayPair() =
    listOf(
        (ScoreboardData.sidebarLinesFormatted.firstOrNull { it.startsWith(" §a✌ §") }?.trim()
            ?: "<hidden>") to AlignmentEnum.LEFT
    )

private fun getVisitShowWhen() = ScoreboardData.sidebarLinesFormatted.any { it.startsWith(" §a✌ §") }

private fun getDateDisplayPair() =
    listOf(SkyBlockTime.now().formatted(yearElement = false, hoursAndMinutesElement = false) to AlignmentEnum.LEFT)


private fun getTimeDisplayPair(): List<Pair<String, AlignmentEnum>> {
    val symbols = listOf("☔", "⚡", "§e☀", "§b☽")
    return if (ScoreboardData.sidebarLinesFormatted.any { line -> symbols.any { line.contains(it) } }) {
        listOf(ScoreboardData.sidebarLinesFormatted.first { line -> symbols.any { line.contains(it) } }
            .trim() to AlignmentEnum.LEFT)
    } else {
        listOf(
            "§7" + SkyBlockTime.now()
                .formatted(dayAndMonthElement = false, yearElement = false) to AlignmentEnum.LEFT
        )
    }
}

private fun getLobbyDisplayPair() = listOf("§8$lobbyCode" to AlignmentEnum.LEFT)

private fun getPowerDisplayPair() = when (MaxwellAPI.currentPower == null) {
    true -> listOf("§c§lPlease visit Maxwell!" to AlignmentEnum.LEFT)
    false ->
        when (config.displayConfig.displayNumbersFirst) {
            true -> listOf("${MaxwellAPI.currentPower?.power} Power" to AlignmentEnum.LEFT)
            false -> listOf("Power: ${MaxwellAPI.currentPower?.power}" to AlignmentEnum.LEFT)
        }
}

private fun getPowerShowWhen() = !listOf(IslandType.THE_RIFT).contains(HypixelData.skyBlockIsland)

private fun getCookieDisplayPair(): List<Pair<String, AlignmentEnum>> {
    val timeLine = CustomScoreboardUtils.getTablistFooter().split("\n")
        .nextAfter("§d§lCookie Buff") ?: "<hidden>"

    return listOf(
        "§d§lCookie Buff" to AlignmentEnum.LEFT
    ) + when (timeLine.contains("Not active")) {
        true -> listOf(" §7- §cNot active" to AlignmentEnum.LEFT)
        false -> listOf(" §7- §e${timeLine.substringAfter("§d§lCookie Buff").trim()}" to AlignmentEnum.LEFT)
    }
}

private fun getCookieShowWhen() = when (config.informationFilteringConfig.hideEmptyLines) {
    true -> CustomScoreboardUtils.getTablistFooter().split("\n").any {
        CustomScoreboardUtils.getTablistFooter().split("\n").nextAfter("§d§lCookie Buff")?.contains(it)
            ?: false
    }

    false -> true
}

private fun getObjectiveDisplayPair(): List<Pair<String, AlignmentEnum>> {
    val objective = mutableListOf<String>()

    objective += ScoreboardData.sidebarLinesFormatted.first { it.startsWith("Objective") }

    objective += ScoreboardData.sidebarLinesFormatted.nextAfter(objective[0]) ?: "<hidden>"

    if (extraObjectiveLines.any {
            ScoreboardData.sidebarLinesFormatted.nextAfter(objective[0], 2)?.contains(it) == true
        }) {
        objective += ScoreboardData.sidebarLinesFormatted.nextAfter(objective[0], 2).toString()
            .replace(")", "§7)")
    }

    return objective.map { it to AlignmentEnum.LEFT }
}

private fun getSlayerDisplayPair(): List<Pair<String, AlignmentEnum>> {
    return listOf(
        (if (SlayerAPI.hasActiveSlayerQuest()) "§cSlayer" else "<hidden>") to AlignmentEnum.LEFT
    ) + (
        " §7- §e${SlayerAPI.latestSlayerCategory.trim()}" to AlignmentEnum.LEFT
        ) + (
        " §7- §e${SlayerAPI.latestSlayerProgress.trim()}" to AlignmentEnum.LEFT
        )
}

private fun getSlayerShowWhen() = listOf(
    IslandType.HUB,
    IslandType.SPIDER_DEN,
    IslandType.THE_PARK,
    IslandType.THE_END,
    IslandType.CRIMSON_ISLE,
    IslandType.THE_RIFT
).contains(HypixelData.skyBlockIsland)

private fun getPowderDisplayPair() = when (config.displayConfig.displayNumbersFirst) {
    true -> listOf("§9§lPowder") + (" §7- §2$mithrilPowder Mithril") + (" §7- §d$gemstonePowder Gemstone")
    false -> listOf("§9§lPowder") + (" §7- §fMithril: §2$mithrilPowder") + (" §7- §fGemstone: §d$gemstonePowder")
}.map { it to AlignmentEnum.LEFT }

private fun getPowderShowWhen() =
    listOf(IslandType.CRYSTAL_HOLLOWS, IslandType.DWARVEN_MINES).contains(HypixelData.skyBlockIsland)

private fun getEventsDisplayPair() = Events.getEvent().flatMap { it.getLines().map { i -> i to AlignmentEnum.LEFT } }

private fun getEventsShowWhen() = Events.getEvent().isNotEmpty()

private fun getMayorDisplayPair(): List<Pair<String, AlignmentEnum>> {
    return listOf(
        (MayorElection.currentCandidate?.name?.let { CustomScoreboardUtils.mayorNameToColorCode(it) }
            ?: "<hidden>") +
            (if (config.showTimeTillNextMayor) {
                "§7 (§e${TimeUtils.formatDuration(MayorElection.timeTillNextMayor, TimeUnit.DAY)}§7)"
            } else {
                ""
            }) to AlignmentEnum.LEFT
    ) + (if (config.showMayorPerks) {
        MayorElection.currentCandidate?.perks?.map { " §7- §e${it.name}" to AlignmentEnum.LEFT } ?: emptyList()
    } else {
        emptyList()
    })
}

private fun getMayorShowWhen() =
    !listOf(IslandType.THE_RIFT).contains(HypixelData.skyBlockIsland) && MayorElection.currentCandidate != null

private fun getPartyDisplayPair() =
    if (PartyAPI.partyMembers.isEmpty() && config.informationFilteringConfig.hideEmptyLines) {
        listOf("<hidden>" to AlignmentEnum.LEFT)
    } else {
        val title =
            if (PartyAPI.partyMembers.isEmpty()) "§9§lParty" else "§9§lParty (${PartyAPI.partyMembers.size})"
        val partyList = PartyAPI.partyMembers
            .take(config.partyConfig.maxPartyList.get())
            .map {
                " §7- §7$it"
            }
            .toTypedArray()
        listOf(title, *partyList).map { it to AlignmentEnum.LEFT }
    }

private fun getPartyShowWhen() = when (inDungeons) {
    true -> false // Hidden bc the scoreboard lines already exist
    false -> when (config.partyConfig.showPartyEverywhere) {
        true -> true
        false -> listOf(
            IslandType.DUNGEON_HUB,
            IslandType.KUUDRA_ARENA,
            IslandType.CRIMSON_ISLE
        ).contains(HypixelData.skyBlockIsland)
    }
}

private fun getFooterDisplayPair(): List<Pair<String, AlignmentEnum>> {


    return listOf(config.displayConfig.customFooter.get().toString().replace("&", "§") to getTitleAndFooterAlignment())
}

private fun getExtraDisplayPair(): List<Pair<String, AlignmentEnum>> {
    if (amountOfExtraLines != extraLines.size && config.unknownLinesWarning) {
        ErrorManager.logErrorWithData(
            CustomScoreboardUtils.UndetectedScoreboardLines("CustomScoreboard detected unknown lines"),
            "CustomScoreboard detected unknown lines",
            "extraLines" to extraLines
        )
        amountOfExtraLines = extraLines.size
    }

    return listOf("§cUndetected Lines:" to AlignmentEnum.LEFT) + extraLines.map { it to AlignmentEnum.LEFT }
}

private fun getExtraShowWhen(): Boolean {
    if (extraLines.isNotEmpty()) {
        amountOfExtraLines = 0
    }
    return extraLines.isNotEmpty()
}