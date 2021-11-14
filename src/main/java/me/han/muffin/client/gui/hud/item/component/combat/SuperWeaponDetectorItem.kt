package me.han.muffin.client.gui.hud.item.component.combat

import me.han.muffin.client.Muffin
import me.han.muffin.client.core.Globals
import me.han.muffin.client.gui.hud.item.HudItem
import me.han.muffin.client.manager.managers.FriendManager
import me.han.muffin.client.utils.color.ColourUtils
import me.han.muffin.client.utils.entity.WeaponUtils
import net.minecraft.entity.player.EntityPlayer

object SuperWeaponDetectorItem: HudItem("SuperWeaponDetector", HudCategory.Combat, 300, 15) {
    private val holdingSet = LinkedHashSet<EntityPlayer>()

    override fun updateTicking() {
        super.updateTicking()

        Globals.mc.world.playerEntities
            .filter { it != Globals.mc.player && !FriendManager.isFriend(it.name) && WeaponUtils.isSuperWeapon(it.heldItemMainhand) && !holdingSet.contains(it) }
            .sortedBy { Globals.mc.player.getDistance(it) }
            .forEach {
                holdingSet.add(it)
            }

        holdingSet.removeIf { !WeaponUtils.isSuperWeapon(it.heldItemMainhand) }
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        super.drawScreen(mouseX, mouseY, partialTicks)

        var posY = y.toFloat()

        holdingSet.forEach {
            val placeHolder = "${it.name} is holding 32k."
            Muffin.getInstance().fontManager.drawStringWithShadow(placeHolder, x.toFloat(), posY, ColourUtils.toRGBA(190, 10, 10, 245))
            posY += Muffin.getInstance().fontManager.stringHeight
        }

    }


}