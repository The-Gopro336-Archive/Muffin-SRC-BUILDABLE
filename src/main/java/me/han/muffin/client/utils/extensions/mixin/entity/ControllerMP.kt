package me.han.muffin.client.utils.extensions.mixin.entity

import me.han.muffin.client.imixin.entity.IPlayerControllerMP
import net.minecraft.client.multiplayer.PlayerControllerMP

var PlayerControllerMP.curBlockDamageMP: Float
    get() = (this as IPlayerControllerMP).curBlockDamageMP
    set(value) {
        (this as IPlayerControllerMP).curBlockDamageMP = value
    }

var PlayerControllerMP.blockHitDelay: Int
    get() = (this as IPlayerControllerMP).blockHitDelay
    set(value) {
        (this as IPlayerControllerMP).blockHitDelay = value
    }

val PlayerControllerMP.currentPlayerItem: Int
    get() = (this as IPlayerControllerMP).currentPlayerItem

var PlayerControllerMP.isPlayerHittingBlock: Boolean
    get() = this.isHittingBlock
    set(value) {
        (this as IPlayerControllerMP).setHittingBlock(value)
    }

fun PlayerControllerMP.syncCurrentPlayItem() = (this as IPlayerControllerMP).syncCurrentPlayItemVoid()