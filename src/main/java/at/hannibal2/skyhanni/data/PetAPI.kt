package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.WidgetUpdateEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object PetAPI {
    private val patternGroup = RepoPattern.group("misc.pet")
    private val petMenuPattern by patternGroup.pattern(
        "menu.title",
        "Pets(?: \\(\\d+/\\d+\\) )?",
    )

    private var rawPetName: String? = null
    private var petEquipped = false

    private var petName: String? = null
    private var petRarity: LorenzRarity? = null
    private var petHasSkin: Boolean? = null
    private var petItem: NEUInternalName? = null

    private var petLevel: Int? = null
    private var petXP: Double? = null

    /**
     * REGEX-TEST: §e⭐ §7[Lvl 200] §6Golden Dragon§d ✦
     * REGEX-TEST: ⭐ [Lvl 100] Black Cat ✦
     */
    private val petItemName by patternGroup.pattern(
        "item.name",
        "(?<favorite>(?:§.)*⭐ )?(?:§.)*\\[Lvl (?<level>\\d+)] (?<name>.*)",
    )
    private val neuRepoPetItemName by patternGroup.pattern(
        "item.name.neu.format",
        "(?:§f§f)?§7\\[Lvl (?:1➡(?:100|200)|\\{LVL})] (?<name>.*)",
    )

    /**
     * REGEX-TEST:  §r§7[Lvl 100] §r§dEndermite
     * REGEX-TEST:  §r§7[Lvl 200] §r§8[§r§6108§r§8§r§4✦§r§8] §r§6Golden Dragon
     * REGEX-TEST:  §r§7[Lvl 100] §r§dBlack Cat§r§d ✦
     */
    private val petWidget by patternGroup.pattern(
        "widget.pet",
        "^ §r§7\\[Lvl (?<level>\\d+)](?: (?:§.)+\\[(?:§.)+(?<overflow>\\d+)(?:§.)+✦(?:§.)+])? §r§(?<rarity>.)(?<name>[\\w ]+)(?:§r(?<skin>§. ✦))?\$",
    )

    /**
     * REGEX-TEST:  §r§7No pet selected
     * REGEX-TEST:  §r§6Washed-up Souvenir
     * REGEX-TEST:  §r§9Dwarf Turtle Shelmet
     */
    private val widgetString by patternGroup.pattern(
        "widget.string",
        "^ §r§.(?<string>[\\w -]+)\$",
    )

    /**
     * REGEX-TEST:  §r§b§lMAX LEVEL
     * REGEX-TEST:  §r§6+§r§e21,248,020.7 XP
     * REGEX-TEST:  §r§e15,986.6§r§6/§r§e29k XP §r§6(53.6%)
     */
    private val xpWidget by patternGroup.pattern(
        "widget.xp",
        "^ §r§.(?:§l(?<max>MAX LEVEL)|\\+§r§e(?<overflowxp>[\\d,.]+) XP|(?<currentXP>[\\d,.]+)§r§6/§r§e(?<maxXP>[\\d.km]+) XP §r§6\\((?<percentage>[\\d.%]+)\\))$",
    )

    private val ignoredPetStrings = listOf(
        "Archer",
        "Berserk",
        "Mage",
        "Tank",
        "Healer",
        "➡",
    )

    fun isPetMenu(inventoryTitle: String): Boolean = petMenuPattern.matches(inventoryTitle)

    // Contains color code + name and for older SkyHanni users maybe also the pet level
    var currentPet: String?
        get() = ProfileStorageData.profileSpecific?.currentPet?.takeIf { it.isNotEmpty() }
        set(value) {
            ProfileStorageData.profileSpecific?.currentPet = value
        }

    fun isCurrentPet(petName: String): Boolean = currentPet?.contains(petName) ?: false

    fun getCleanName(nameWithLevel: String): String? {
        petItemName.matchMatcher(nameWithLevel) {
            return group("name")
        }
        neuRepoPetItemName.matchMatcher(nameWithLevel) {
            return group("name")
        }

        return null
    }

    fun getPetLevel(nameWithLevel: String): Int? = petItemName.matchMatcher(nameWithLevel) {
        group("level").toInt()
    }

    fun hasPetName(name: String): Boolean = petItemName.matches(name) && !ignoredPetStrings.any { name.contains(it) }

    @SubscribeEvent
    fun onTabUpdate(event: WidgetUpdateEvent) {
        if (!event.isWidget(TabWidget.PET)) return

        val newPetLine = event.lines.getOrNull(1) ?: return
        if (newPetLine == rawPetName) return

        rawPetName = newPetLine

        widgetString.matchMatcher(newPetLine) {
            val string = group("string")
            if (string == "No pet selected") {
                removePet()
                return
            }
            petItem = NEUInternalName.fromItemNameOrNull(string)
        }

        petWidget.matchMatcher(newPetLine) {
            petEquipped = true
            petName = group("name")
            petRarity = LorenzRarity.getByColorCode(group("rarity")[0])
            petLevel = group("level").toInt()
            petHasSkin = group("skin") != null
            return
        }

        xpWidget.matchMatcher(newPetLine) {
            //i don't feel like doing this right now
        }
    }

    private fun removePet() {
        petName = null
        petLevel = null
        petRarity = null
        petHasSkin = null
        petEquipped = false
        petItem = null
    }

    @SubscribeEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("PetAPI")
        event.addIrrelevant {
            add("petName: '$petName'")
            add("petRarity: '$petRarity'")
            add("petLevel: '$petLevel'")
            add("petHasSkin: '$petHasSkin'")
            add("petEquipped: '$petEquipped'")
            add("petItem: '$petItem'")
        }
    }
}
