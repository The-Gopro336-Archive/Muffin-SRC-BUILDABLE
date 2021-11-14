package me.han.muffin.client.imixin.entity

interface IPlayerControllerMP {

    var curBlockDamageMP: Float
    var blockHitDelay: Int

    val currentPlayerItem: Int

    fun setHittingBlock(isHittingBlock: Boolean)
    fun syncCurrentPlayItemVoid()

}