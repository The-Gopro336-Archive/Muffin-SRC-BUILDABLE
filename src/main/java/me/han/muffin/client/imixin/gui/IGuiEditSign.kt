package me.han.muffin.client.imixin.gui

import net.minecraft.client.gui.inventory.GuiEditSign
import net.minecraft.tileentity.TileEntitySign

interface IGuiEditSign {
    val tileSign: TileEntitySign
    val editLine: Int

    val GuiEditSign.tileSign: TileEntitySign get() = (this as IGuiEditSign).tileSign
}