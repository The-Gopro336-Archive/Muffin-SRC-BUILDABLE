package me.han.muffin.client.utils.extensions.mc.block

import net.minecraft.block.Block
import net.minecraft.init.Blocks
import net.minecraft.item.Item

val Block.item: Item get() = Item.getItemFromBlock(this)
val Block.id: Int get() = Block.getIdFromBlock(this)

val shulkerList: Set<Block> = hashSetOf(
    Blocks.WHITE_SHULKER_BOX,
    Blocks.ORANGE_SHULKER_BOX,
    Blocks.MAGENTA_SHULKER_BOX,
    Blocks.LIGHT_BLUE_SHULKER_BOX,
    Blocks.YELLOW_SHULKER_BOX,
    Blocks.LIME_SHULKER_BOX,
    Blocks.PINK_SHULKER_BOX,
    Blocks.GRAY_SHULKER_BOX,
    Blocks.SILVER_SHULKER_BOX,
    Blocks.CYAN_SHULKER_BOX,
    Blocks.PURPLE_SHULKER_BOX,
    Blocks.BLUE_SHULKER_BOX,
    Blocks.BROWN_SHULKER_BOX,
    Blocks.GREEN_SHULKER_BOX,
    Blocks.RED_SHULKER_BOX,
    Blocks.BLACK_SHULKER_BOX
)

val nonSolidList: Set<Block> = hashSetOf(
    Blocks.AIR,
    Blocks.WATER,
    Blocks.FLOWING_WATER,
    Blocks.LAVA,
    Blocks.FLOWING_LAVA,
    Blocks.FIRE,
    Blocks.GRASS,
    Blocks.VINE,
    Blocks.SNOW,
    Blocks.TALLGRASS
)

val blockBlacklist: Set<Block> = hashSetOf(
    Blocks.ENDER_CHEST,
    Blocks.CHEST,
    Blocks.TRAPPED_CHEST,
    Blocks.CRAFTING_TABLE,
    Blocks.ANVIL,
    Blocks.BREWING_STAND,
    Blocks.HOPPER,
    Blocks.DROPPER,
    Blocks.DISPENSER,
    Blocks.TRAPDOOR,
    Blocks.ENCHANTING_TABLE,
    Blocks.BEACON
).apply {
    addAll(shulkerList)
}

val rightClickableBlock: Set<Block> = hashSetOf(
    Blocks.IRON_TRAPDOOR,
    Blocks.LEVER,
    Blocks.NOTEBLOCK,
    Blocks.JUKEBOX,
    Blocks.BEACON,
    Blocks.BED,
    Blocks.FURNACE,
    Blocks.CAKE,
    Blocks.DRAGON_EGG,
    Blocks.COMMAND_BLOCK,
    Blocks.CHAIN_COMMAND_BLOCK,
    Blocks.REPEATING_COMMAND_BLOCK,
    Blocks.WOODEN_BUTTON,
    Blocks.STONE_BUTTON,
    Blocks.POWERED_COMPARATOR,
    Blocks.UNPOWERED_COMPARATOR,
    Blocks.POWERED_REPEATER,
    Blocks.UNPOWERED_REPEATER,
    Blocks.OAK_FENCE_GATE,
    Blocks.SPRUCE_FENCE_GATE,
    Blocks.BIRCH_FENCE_GATE,
    Blocks.JUNGLE_FENCE_GATE,
    Blocks.DARK_OAK_FENCE_GATE,
    Blocks.ACACIA_FENCE_GATE,
    Blocks.OAK_DOOR,
    Blocks.SPRUCE_DOOR,
    Blocks.BIRCH_DOOR,
    Blocks.JUNGLE_DOOR,
    Blocks.ACACIA_DOOR,
    Blocks.DARK_OAK_DOOR,
).apply {
    addAll(blockBlacklist)
    addAll(shulkerList)
}