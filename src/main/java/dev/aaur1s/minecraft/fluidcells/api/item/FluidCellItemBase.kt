package dev.aaur1s.minecraft.fluidcells.api.item

import net.minecraft.block.Block
import net.minecraft.block.IBucketPickupHandler
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.fluid.Fluid
import net.minecraft.fluid.Fluids
import net.minecraft.item.BucketItem
import net.minecraft.item.Item
import net.minecraft.item.ItemGroup
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundNBT
import net.minecraft.util.ActionResult
import net.minecraft.util.ActionResultType
import net.minecraft.util.Direction
import net.minecraft.util.Hand
import net.minecraft.util.NonNullList
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.RayTraceContext.FluidMode
import net.minecraft.util.math.RayTraceResult.Type.ENTITY
import net.minecraft.util.math.RayTraceResult.Type.MISS
import net.minecraft.util.text.ITextComponent
import net.minecraft.util.text.StringTextComponent
import net.minecraft.util.text.TranslationTextComponent
import net.minecraft.world.World
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fluids.FluidUtil
import net.minecraftforge.fluids.IFluidBlock
import net.minecraftforge.fluids.capability.CapabilityFluidHandler
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack
import net.minecraftforge.fluids.capability.wrappers.BucketPickupHandlerWrapper
import net.minecraftforge.fluids.capability.wrappers.FluidBlockWrapper
import net.minecraftforge.registries.ForgeRegistries

open class FluidCellItemBase(
    val containerSizeMb: Int,
    val isFractional: Boolean,
    itemProperties: Properties = Properties().stacksTo(64).tab(ItemGroup.TAB_MISC)
) : Item(itemProperties) {

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

    val ItemStack.fluidCapability: FluidHandlerItemStack?
        get() = CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY?.let {
            getCapability(it).resolve().orElse(null) as? FluidHandlerItemStack
        }

    val ItemStack.fluidStack: FluidStack
        get() = fluidCapability?.fluid ?: FluidStack.EMPTY

    val ItemStack.fluid: Fluid
        get() = fluidStack.fluid

    val Fluid.isEmpty get() = this.registryName == Fluids.EMPTY.registryName
    val Fluid.isNotEmpty get() = !this.isEmpty

    val FluidStack.isNotEmpty get() = !this.isEmpty

    override fun appendHoverText(
        itemStack: ItemStack,
        world: World?,
        lines: MutableList<ITextComponent>,
        tooltipFlag: ITooltipFlag
    ) {
        lines.add(1, StringTextComponent("${itemStack.fluidStack.amount}/$containerSizeMb mb"))
    }

    override fun getName(itemStack: ItemStack): ITextComponent =
        itemStack.fluid.run {
            if (isEmpty) TranslationTextComponent("fluid.fluidcells.empty")
            else TranslationTextComponent(attributes.translationKey)
        }.append(" ").append(super.getName(itemStack))


    override fun use(world: World, player: PlayerEntity, hand: Hand): ActionResult<ItemStack> {
        val itemInHand = player.getItemInHand(hand)
        val fluid = itemInHand.fluid
        val rayTrace = getPlayerPOVHitResult(
            world,
            player,
            when (fluid) {
                Fluids.EMPTY -> FluidMode.SOURCE_ONLY
                else -> FluidMode.NONE
            }
        )

        val pos = rayTrace.blockPos

        val mayInteract = world.mayInteract(player, pos)
        val mayUse = player.mayUseItemAt(pos, rayTrace.direction, itemInHand)

        val actionResult = when (rayTrace.type) {
            MISS, ENTITY -> ActionResult.pass(itemInHand)
            else if (!mayInteract || !mayUse) -> ActionResult.fail(itemInHand)
            else -> interactionLogic(hand, world, pos, rayTrace.direction, itemInHand, player)
        }

        return actionResult
    }

    private fun interactionLogic(
        hand: Hand,
        world: World,
        blockPos: BlockPos,
        direction: Direction,
        itemStack: ItemStack,
        player: PlayerEntity,
    ): ActionResult<ItemStack> {
        val original = itemStack
        val originalCount = original.count

        val singleItemStack = original.copy().apply { count = 1 }

        val itemFluidHandler = singleItemStack.fluidCapability ?: return ActionResult.fail(itemStack)

        val blockState = world.getBlockState(blockPos)
        val block = blockState.block
        val blockFluidHandler = block.fluidCapability(world, blockPos, direction)

        val singleFluidStack = singleItemStack.fluidStack

        if (blockFluidHandler == null && singleFluidStack.isEmpty) return ActionResult.fail(itemStack)

        val actionResult = if (blockFluidHandler != null && !player.isShiftKeyDown) {
            if (singleFluidStack.isNotEmpty) {
                val successInsert = FluidUtil.tryFluidTransfer(blockFluidHandler, itemFluidHandler, Int.MAX_VALUE, true).isNotEmpty
                if (successInsert) {
                    ActionResult.success(itemStack)
                } else {
                    val successExtract = FluidUtil.tryFluidTransfer(itemFluidHandler, blockFluidHandler, Int.MAX_VALUE, true).isNotEmpty
                    if (successExtract) ActionResult.success(itemStack) else ActionResult.fail(itemStack)
                }
            } else {
                val successExtract = FluidUtil.tryFluidTransfer(itemFluidHandler, blockFluidHandler, Int.MAX_VALUE, true).isNotEmpty
                if (successExtract) ActionResult.success(itemStack) else ActionResult.fail(itemStack)
            }
        } else {
            if (singleFluidStack.isNotEmpty) {
                val placePos = blockPos.relative(direction)
                val placeBlockState = world.getBlockState(placePos)
                val placeBlock = placeBlockState.block

                val placeBlockFluidHandler = placeBlock.fluidCapability(world, placePos, direction.opposite)

                if (placeBlockFluidHandler != null && !player.isShiftKeyDown) {
                    val possibleExtract = FluidUtil.tryFluidTransfer(itemFluidHandler, placeBlockFluidHandler, Int.MAX_VALUE, false).isNotEmpty
                    if (possibleExtract) {
                        val successExtract = FluidUtil.tryFluidTransfer(itemFluidHandler, placeBlockFluidHandler, Int.MAX_VALUE, true).isNotEmpty
                        if (successExtract) ActionResult.success(itemStack) else ActionResult.fail(itemStack)
                    } else if (placeBlock.extractPlacedFluid(world, placePos).isEmpty) {
                        val successPlace = FluidUtil.tryPlaceFluid(player, world, hand, placePos, itemFluidHandler, singleFluidStack)
                        if (successPlace) ActionResult.success(itemStack) else ActionResult.fail(itemStack)
                    } else {
                        ActionResult.fail(itemStack)
                    }
                } else {
                    val successPlace = FluidUtil.tryPlaceFluid(player, world, hand, placePos, itemFluidHandler, singleFluidStack)
                    if (successPlace) ActionResult.success(itemStack) else ActionResult.fail(itemStack)
                }
            } else {
                ActionResult.fail(itemStack)
            }
        }

        if (actionResult.result == ActionResultType.SUCCESS) {
            val resultingSingle = singleItemStack.fluidCapability?.container ?: singleItemStack

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

    override fun fillItemCategory(itemGroup: ItemGroup, items: NonNullList<ItemStack>) {
        if (!allowdedIn(itemGroup)) return

        items.add(defaultInstance)

        val addedFluids = mutableSetOf<ResourceLocation>()

        for (rawFluid in ForgeRegistries.FLUIDS) {
            val fluid = (rawFluid.bucket as? BucketItem)?.fluid ?: continue

            val itemStack = defaultInstance
            val capability = initCapabilities(itemStack, null)
            capability.fill(FluidStack(fluid, containerSizeMb), FluidAction.EXECUTE)

            val addingRegistryName = fluid.registryName

            if (addingRegistryName != null && addingRegistryName !in addedFluids) {
                items.add(itemStack)
                addedFluids.add(addingRegistryName)
            }
        }
    }

    override fun initCapabilities(stack: ItemStack, nbt: CompoundNBT?) =
        object : FluidHandlerItemStack(stack, containerSizeMb) {
            override fun fill(resource: FluidStack, doFill: FluidAction): Int {
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
                if (isFractional) return super.drain(resource, doDrain)

                val resourceCopy = resource.copy()
                val fluid = fluid

                if (fluid.isEmpty) return FluidStack.EMPTY

                if (resourceCopy.isEmpty) return FluidStack.EMPTY

                if (resourceCopy.amount > containerSizeMb) resourceCopy.amount = containerSizeMb

                if (resourceCopy.amount != containerSizeMb) return FluidStack.EMPTY

                return super.drain(resource, doDrain)
            }
        }
}
