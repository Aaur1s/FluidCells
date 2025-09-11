package dev.aaur1s.minecraft.fluidcells.util

import dev.aaur1s.minecraft.fluidcells.api.item.handler.SimpleFluidHandlerItemStackHandler
import net.minecraft.block.Block
import net.minecraft.block.IBucketPickupHandler
import net.minecraft.fluid.Fluid
import net.minecraft.fluid.Fluids
import net.minecraft.item.ItemStack
import net.minecraft.util.Direction
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fluids.FluidUtil
import net.minecraftforge.fluids.IFluidBlock
import net.minecraftforge.fluids.capability.CapabilityFluidHandler
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction
import net.minecraftforge.fluids.capability.wrappers.BucketPickupHandlerWrapper
import net.minecraftforge.fluids.capability.wrappers.FluidBlockWrapper


fun Block.fluidCapability(world: World, pos: BlockPos, direction: Direction) = when (this) {
    is IFluidBlock -> FluidBlockWrapper(this, world, pos)
    is IBucketPickupHandler -> BucketPickupHandlerWrapper(this, world, pos)
    else -> FluidUtil.getFluidHandler(world, pos, direction).resolve().orElse(null)
}

fun Block.extractPlacedFluid(world: World, pos: BlockPos) = when (this) {
    is IFluidBlock -> this.drain(world, pos, FluidAction.SIMULATE).fluid
    is IBucketPickupHandler -> {
        val blockState = world.getBlockState(pos)
        val liquid = this.takeLiquid(world, pos, blockState)
        world.setBlock(pos, blockState, 0)
        liquid
    }
    else -> Fluids.EMPTY
}

val ItemStack.simpleFluidCapability: SimpleFluidHandlerItemStackHandler? get() = CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY?.let { getCapability(it).resolve().orElse(null) } as? SimpleFluidHandlerItemStackHandler

val ItemStack.fluidStack: FluidStack
    get() = simpleFluidCapability?.fluid ?: FluidStack.EMPTY

val ItemStack.fluid: Fluid
    get() = fluidStack.fluid

val Fluid.isEmpty get() = this.registryName == Fluids.EMPTY.registryName
val Fluid.isNotEmpty get() = !this.isEmpty

val FluidStack.isNotEmpty get() = !this.isEmpty