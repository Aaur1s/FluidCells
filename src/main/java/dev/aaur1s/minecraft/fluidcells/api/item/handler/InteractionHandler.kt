package dev.aaur1s.minecraft.fluidcells.api.item.handler

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.util.ActionResult
import net.minecraft.util.Direction
import net.minecraft.util.Hand
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

interface InteractionHandler<STACK, ACTION> {
    val isActive: Boolean
    val isActiveOthers: Boolean
    fun addOtherHandlers(vararg handler: InteractionHandler<*, *>)
    fun <T : STACK> fillIt(stack: T, action: ACTION): T
    fun <T : STACK> drainIt(stack: T, action: ACTION): T

    fun interactionLogic(
        hand: Hand,
        world: World,
        blockPos: BlockPos,
        direction: Direction,
        itemStack: ItemStack,
        player: PlayerEntity,
    ): ActionResult<ItemStack>
}