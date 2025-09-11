package dev.aaur1s.minecraft.fluidcells.api.item.handler

import dev.aaur1s.minecraft.fluidcells.util.*
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.util.ActionResult
import net.minecraft.util.ActionResultType
import net.minecraft.util.Direction
import net.minecraft.util.Hand
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.util.LazyOptional
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fluids.FluidUtil
import net.minecraftforge.fluids.capability.CapabilityFluidHandler
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack

class SimpleFluidHandlerItemStackHandler(
    val itemStack: ItemStack,
    val containerSizeMb: Int,
    val isFractional: Boolean
) : FluidHandlerItemStack(itemStack, containerSizeMb), InteractionHandler<FluidStack, FluidAction> {
    private val otherHandlers = mutableSetOf<InteractionHandler<*, *>>()
    override val isActive get() = fluid.isNotEmpty
    override val isActiveOthers get() = otherHandlers.all { it.isActive }

    override fun addOtherHandlers(vararg handler: InteractionHandler<*, *>) {
        otherHandlers.addAll(handler)
    }

    override fun <T : FluidStack> fillIt(stack: T, action: FluidAction): T {
        @Suppress("UNCHECKED_CAST")
        return stack.copy().apply { amount = fill(stack, action) } as T
    }

    override fun <T : FluidStack> drainIt(stack: T, action: FluidAction): T {
        @Suppress("UNCHECKED_CAST")
        return drain(stack, action) as T
    }

    override fun interactionLogic(
        hand: Hand,
        world: World,
        blockPos: BlockPos,
        direction: Direction,
        itemStack: ItemStack,
        player: PlayerEntity
    ): ActionResult<ItemStack> {
        val original = itemStack
        val originalCount = original.count
        val singleItemStack = original.copy().apply { count = 1 }

        val blockState = world.getBlockState(blockPos)
        val block = blockState.block
        val blockFluidHandler = block.fluidCapability(world, blockPos, direction)

        val singleFluidStack = singleItemStack.fluidStack

        if (blockFluidHandler == null && singleFluidStack.isEmpty) return ActionResult.fail(itemStack)

        val actionResult = if (blockFluidHandler != null && !player.isShiftKeyDown) {
            if (singleFluidStack.isNotEmpty) {
                val successInsert = FluidUtil.tryFluidTransfer(blockFluidHandler, this, Int.MAX_VALUE, true).isNotEmpty
                if (successInsert) {
                    ActionResult.success(itemStack)
                } else {
                    val successExtract = FluidUtil.tryFluidTransfer(this, blockFluidHandler, Int.MAX_VALUE, true).isNotEmpty
                    if (successExtract) ActionResult.success(itemStack) else ActionResult.fail(itemStack)
                }
            } else {
                val successExtract = FluidUtil.tryFluidTransfer(this, blockFluidHandler, Int.MAX_VALUE, true).isNotEmpty
                if (successExtract) ActionResult.success(itemStack) else ActionResult.fail(itemStack)
            }
        } else {
            if (singleFluidStack.isNotEmpty) {
                val placePos = blockPos.relative(direction)
                val placeBlockState = world.getBlockState(placePos)
                val placeBlock = placeBlockState.block

                val placeBlockFluidHandler = placeBlock.fluidCapability(world, placePos, direction.opposite)

                if (placeBlockFluidHandler != null && !player.isShiftKeyDown) {
                    val possibleExtract = FluidUtil.tryFluidTransfer(this, placeBlockFluidHandler, Int.MAX_VALUE, false).isNotEmpty
                    if (possibleExtract) {
                        val successExtract = FluidUtil.tryFluidTransfer(this, placeBlockFluidHandler, Int.MAX_VALUE, true).isNotEmpty
                        if (successExtract) ActionResult.success(itemStack) else ActionResult.fail(itemStack)
                    } else if (placeBlock.extractPlacedFluid(world, placePos).isEmpty) {
                        val successPlace = FluidUtil.tryPlaceFluid(player, world, hand, placePos, this, singleFluidStack)
                        if (successPlace) ActionResult.success(itemStack) else ActionResult.fail(itemStack)
                    } else {
                        ActionResult.fail(itemStack)
                    }
                } else {
                    val successPlace = FluidUtil.tryPlaceFluid(player, world, hand, placePos, this, singleFluidStack)
                    if (successPlace) ActionResult.success(itemStack) else ActionResult.fail(itemStack)
                }
            } else {
                ActionResult.fail(itemStack)
            }
        }

        if (actionResult.result == ActionResultType.SUCCESS) {
            val resultingSingle = singleItemStack.simpleFluidCapability?.container ?: singleItemStack

            if (originalCount > 1) {
                original.shrink(1)
                player.setItemInHand(hand, original)

                if (!player.inventory.add(resultingSingle)) {
                    player.drop(resultingSingle, true)
                }
            } else {
                player.setItemInHand(hand, resultingSingle)
            }
        }

        return actionResult
    }

    override fun fill(resource: FluidStack, doFill: FluidAction): Int {
        if (isActiveOthers) return 0

        if (isFractional) return super.fill(resource, doFill)

        val resourceCopy = resource.copy()

        val fluid = fluid

        if (fluid.isNotEmpty) return 0

        if (resourceCopy.isEmpty) return 0

        if (resourceCopy.amount > containerSizeMb) resourceCopy.amount = containerSizeMb

        if (resourceCopy.amount != containerSizeMb) return 0

        return super.fill(resourceCopy, doFill)
    }

    override fun drain(maxDrain: Int, doDrain: FluidAction): FluidStack {
        if (isActiveOthers) return FluidStack.EMPTY

        if (isFractional) return super.drain(maxDrain, doDrain)

        val resource = super.drain(maxDrain, FluidAction.SIMULATE)
        val resourceCopy = resource.copy()
        val fluid = fluid

        if (fluid.isEmpty) return FluidStack.EMPTY

        if (resourceCopy.isEmpty) return FluidStack.EMPTY

        if (resourceCopy.amount > containerSizeMb) resourceCopy.amount = containerSizeMb

        if (resourceCopy.amount != containerSizeMb) return FluidStack.EMPTY

        return super.drain(resourceCopy.amount, doDrain)
    }

    override fun drain(resource: FluidStack, doDrain: FluidAction): FluidStack {
        if (isActiveOthers) return FluidStack.EMPTY

        if (isFractional) return super.drain(resource, doDrain)

        val resourceCopy = resource.copy()
        val fluid = fluid

        if (fluid.isEmpty) return FluidStack.EMPTY

        if (resourceCopy.isEmpty) return FluidStack.EMPTY

        if (resourceCopy.amount > containerSizeMb) resourceCopy.amount = containerSizeMb

        if (resourceCopy.amount != containerSizeMb) return FluidStack.EMPTY

        return super.drain(resource, doDrain)
    }

    override fun <T> getCapability(capability: Capability<T>, facing: Direction?): LazyOptional<T> {
        return CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY?.orEmpty<T>(capability, LazyOptional.of { this }) ?: LazyOptional.empty()
    }
}