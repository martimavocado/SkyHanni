package at.hannibal2.skyhanni.features.commands

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.misc.PartyCommandsConfig
import at.hannibal2.skyhanni.data.FriendAPI
import at.hannibal2.skyhanni.data.PartyAPI
import at.hannibal2.skyhanni.events.PartyChatEvent
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object PartyChatCommands {

    data class PartyChatCommand(
        val names: List<String>,
        val isEnabled: () -> Boolean,
        val requiresPartyLead: Boolean,
        val executable: (PartyChatEvent) -> Unit,
    )

    private val config get() = SkyHanniMod.feature.misc.partyCommands
    private val allPartyCommands = listOf(
        PartyChatCommand(
            listOf("pt", "ptme", "transfer"),
            { config.transferCommand },
            requiresPartyLead = true,
            executable = {
                ChatUtils.sendCommandToServer("party transfer ${it.author}")
            }
        ),
        PartyChatCommand(
            listOf("pw", "warp", "warpus"),
            { config.warpCommand },
            requiresPartyLead = true,
            executable = {
                ChatUtils.sendCommandToServer("party warp")
            }
        ),
    )

    private val indexedPartyChatCommands = buildMap {
        for (command in allPartyCommands) {
            for (name in command.names) {
                put(name.lowercase(), command)
            }
        }
    }

    private fun isTrustedUser(name: String): Boolean {
        val friend = FriendAPI.getAllFriends().find { it.name == name }
        return when (config.defaultRequiredTrustLevel) {
            PartyCommandsConfig.TrustedUser.FRIENDS -> friend != null
            PartyCommandsConfig.TrustedUser.BEST_FRIENDS -> friend?.bestFriend == true
            PartyCommandsConfig.TrustedUser.ANYONE -> true
            PartyCommandsConfig.TrustedUser.NO_ONE -> false
        }
    }

    private fun isBlockedUser(name: String): Boolean {
        val blacklist = config.blacklistedUsers
        return name in blacklist
    }

    private val commandBeginChars = ".!?".toSet()

    @SubscribeEvent
    fun onPartyCommand(event: PartyChatEvent) {
        if (event.text.firstOrNull() !in commandBeginChars)
            return
        val commandLabel = event.text.substring(1).substringBefore(' ')
        val command = indexedPartyChatCommands[commandLabel.lowercase()] ?: return
        if (event.author == LorenzUtils.getPlayerName()) {
            return
        }
        if (!command.isEnabled()) return
        if (command.requiresPartyLead && PartyAPI.partyLeader != LorenzUtils.getPlayerName()) {
            return
        }
        if (isBlockedUser(event.author)) {
            if (config.showIgnoredReminder) ChatUtils.clickableChat("§cIgnoring chat command from ${event.author}. Unignore them using /shignore remove <player> or click here!",
                { blacklistModify(event.author) })
            return
        }
        if (!isTrustedUser(event.author)) {
            if (config.showIgnoredReminder) ChatUtils.chat("§cIgnoring chat command from ${event.author}. Change your party chat command settings or /friend (best) them.")
            return
        }
        command.executable(event)
    }

    fun blacklist(input: Array<String>) {
        if (input.size !in 1..2) {
            ChatUtils.userError("Usage: /shignore <add/remove/list/clear> <name>")
            return
        }
        when (val firstArg = input[0]) {
            "add" -> {
                if (input.size != 2) {
                    ChatUtils.userError("Usage: /shignore <add/remove/list/clear> <name>")
                    return
                }
                if (isBlockedUser(input[1])) {
                    ChatUtils.userError("${input[1]} is already ignored!")
                } else blacklistModify(input[1])
                return
            }
            "remove" -> {
                if (input.size != 2) {
                    ChatUtils.userError("Usage: /shignore <add/remove/list/clear> <name>")
                    return
                }
                if (!isBlockedUser(input[1])) {
                    ChatUtils.userError("${input[1]} isn't ignored!")
                } else blacklistModify(input[1])
                return
            }
            "list" -> {
                if (input.size == 2) {
                    blacklistView(input[1])
                } else blacklistView()
                return
            }
            "clear" -> {
                ChatUtils.clickableChat("Are you sure you want to do this? Click here to confirm.",
                    {
                        config.blacklistedUsers.clear()
                        ChatUtils.chat("Cleared your ignored players list!")
                    })
                return
            }
            else -> {
                blacklistModify(firstArg)
                return
            }
        }
    }

    private fun blacklistModify(player: String) {
        if (player !in config.blacklistedUsers) {
            ChatUtils.chat("§cNow ignoring §b$player§e!")
            config.blacklistedUsers.add(player)
            return
        } else {
            ChatUtils.chat("§aStopped ignoring §b$player§e!")
            config.blacklistedUsers.remove(player)
            return
        }
    }

    private fun blacklistView(player: String? = null) {
        val blacklist = config.blacklistedUsers
        if (player == null) {
            if (blacklist.size > 0) {
                var message = "Ignored player list:"
                if (blacklist.size > 15) {
                    message += "\n§e"
                    blacklist.forEachIndexed { i, it ->
                        message += it
                        if (i < blacklist.size - 1) { message += ", "}
                    }
                } else {
                    blacklist.forEach {
                        message += "\n§e$it"
                    }
                }
                ChatUtils.chat(message)
                return
            }
            ChatUtils.chat("Your ignored players list is empty!")
            return
        } else {
            if (isBlockedUser(player)) {
                ChatUtils.chat("$player §ais §eignored.")
            } else {
                ChatUtils.chat("$player §cisn't §eignored.")
            }
        }
    }
}
