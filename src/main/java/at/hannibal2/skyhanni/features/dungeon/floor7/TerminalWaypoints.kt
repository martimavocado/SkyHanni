package at.hannibal2.skyhanni.features.dungeon.floor7

import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.test.GriffinUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.entity.player.EntityPlayer
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent


class TerminalWaypoints {
    private val goldorTerminalPattern by RepoPattern.pattern(
        "dungeon.f7.goldor.terminalcomplete",
        "§.(?<playerName>\\w+)§r§a (?:activated|completed) a (?<type>lever|terminal|device)! \\(§r§c(?<currentTerminal>\\d)§r§a/(?<total>\\d)\\)"
    )

    @SubscribeEvent
    fun onWorld(event: LorenzRenderWorldEvent) {
        TerminalInfo.entries.filter { it.highlight }.forEach {
            event.drawWaypointFilled(it.location, LorenzColor.GREEN.toColor())
            event.drawDynamicText(it.location, it.text, 1.0)
        }
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        goldorTerminalPattern.matchMatcher(event.message){
            val playerName = group("playerName")
//             println("matched $playerName")
            val playerEntity = EntityUtils.getAllEntities().filter { it is EntityPlayer }.firstOrNull{ it.name == playerName } ?: return
//             println("found $playerEntity")
            val terminal = TerminalInfo.getClosestTerminal(playerEntity.position.toLorenzVec())
//             println("$terminal is closest")
            terminal?.highlight = false
//             println(terminal?.highlight)
        }
    }
}