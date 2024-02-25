package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.HotmData
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class HotmFeatures {

    val config get() = SkyHanniMod.feature.mining.hotmConfig

    fun isEnabled() = LorenzUtils.inSkyBlock && HotmData.inInventory

    @SubscribeEvent
    fun onRender(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!isEnabled()) return
        if (!config.highlightEnabledPerks) return
        HotmData.entries.forEach { entry ->
            val color = if (!entry.isUnlocked) LorenzColor.DARK_GRAY
            else if (entry.enabled) LorenzColor.GREEN else LorenzColor.RED
            entry.slot?.highlight(color)
        }
    }

    @SubscribeEvent
    fun onRenderTip(event: RenderItemTipEvent) {
        if (!isEnabled()) return
        handleLevelStackSize(event)
        handleTokenStackSize(event)
    }

    private fun handleLevelStackSize(event: RenderItemTipEvent) {
        if (!config.levelStackSize) return
        HotmData.entries.firstOrNull() {
            event.stack == it.slot?.stack
        }?.let {
            event.stackTip = it.activeLevel.takeIf { it != 0 }?.toString() ?: ""
        }
    }

    private fun handleTokenStackSize(event: RenderItemTipEvent) {
        if (!config.tokenStackSize) return
        if (event.stack != HotmData.heartItem) return
        event.stackTip = HotmData.availableTokens.takeIf { it != 0 }?.let { "§b$it" } ?: ""
    }

}