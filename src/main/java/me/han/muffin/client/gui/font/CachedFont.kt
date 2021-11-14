package me.han.muffin.client.gui.font

import org.lwjgl.opengl.GL11.glDeleteLists

data class CachedFont(val displayList: Int, var lastUsage: Long, var deleted: Boolean = false) {
    protected fun finalize() {
        if (!deleted) {
            glDeleteLists(displayList, 1)
        }
    }
}