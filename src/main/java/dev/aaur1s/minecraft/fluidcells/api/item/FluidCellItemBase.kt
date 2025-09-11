package dev.aaur1s.minecraft.fluidcells.api.item

import dev.aaur1s.minecraft.fluidcells.api.item.handler.InteractionHandler
import dev.aaur1s.minecraft.fluidcells.api.item.handler.SimpleChemicalHandlerItemStackHandler
import dev.aaur1s.minecraft.fluidcells.api.item.handler.SimpleFluidHandlerItemStackHandler
import dev.aaur1s.minecraft.fluidcells.util.*
import mekanism.api.Action as MekanismAction
import mekanism.common.registries.MekanismGases
import mekanism.common.registries.MekanismInfuseTypes
import mekanism.common.registries.MekanismPigments
import mekanism.common.registries.MekanismSlurries
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.entity.player.PlayerEntity
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
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.ICapabilityProvider
import net.minecraftforge.common.util.LazyOptional
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction
import net.minecraftforge.fml.ModList
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries

open class FluidCellItemBase(
    val containerSizeMb: Int,
    val isFractional: Boolean,
    itemProperties: Properties = Properties().stacksTo(64).tab(ItemGroup.TAB_MISC)
) : Item(itemProperties) {

    override fun appendHoverText(
        itemStack: ItemStack,
        world: World?,
        lines: MutableList<ITextComponent>,
        tooltipFlag: ITooltipFlag
    ) {
        val fluidStack = itemStack.fluidStack
        val amount = if (fluidStack.isEmpty && ModList.get().isLoaded("mekanism")) itemStack.chemicalStack?.amount ?: 0 else fluidStack.amount
        lines.add(1, StringTextComponent("$amount/$containerSizeMb mb"))
    }

    override fun getName(itemStack: ItemStack): ITextComponent =
        TranslationTextComponent(itemStack.contentTranslationKey())
            .append(" ")
            .append(super.getName(itemStack))


    private fun ItemStack.contentTranslationKey(): String {
        val fluid = fluid
        if (fluid.isNotEmpty) return fluid.attributes.translationKey

        if (ModList.get().isLoaded("mekanism")) {
            val chemical = simpleChemicalCapability?.chemicalStack
            if (chemical != null) return chemical.translationKey
        }

        return "fluid.fluidcells.empty"
    }


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
        val interactors = mutableListOf<InteractionHandler<*, *>>()
        val fluidCapability = itemStack.simpleFluidCapability

        if (ModList.get().isLoaded("mekanism")) {
            val chemicalCapability = itemStack.simpleChemicalCapability
            if (chemicalCapability != null) interactors += chemicalCapability
        }
        if (fluidCapability != null) interactors += fluidCapability

        interactors.firstOrNull() ?: error("Couldn't get transfer capability")

        var result: ActionResult<ItemStack> = ActionResult.pass(ItemStack.EMPTY)
        for (interactor in interactors) {
            result = interactor.interactionLogic(hand, world, blockPos, direction, itemStack, player)
            if (result.result == ActionResultType.SUCCESS) break
        }
        return result
    }

    override fun fillItemCategory(itemGroup: ItemGroup, items: NonNullList<ItemStack>) {
        if (!allowdedIn(itemGroup)) return

        items.add(defaultInstance)

        if (isFractional) return

        val addedFluids = mutableSetOf<ResourceLocation>()

        for (rawFluid in ForgeRegistries.FLUIDS) {
            val fluid = (rawFluid.bucket as? BucketItem)?.fluid ?: continue

            if (fluid.isEmpty) continue

            val itemStack = defaultInstance
            initCapabilities(itemStack, null)

            val capability = itemStack.simpleFluidCapability
            capability?.fill(FluidStack(fluid, containerSizeMb), FluidAction.EXECUTE)

            val addingRegistryName = fluid.registryName

            if (addingRegistryName != null && addingRegistryName !in addedFluids) {
                items.add(itemStack)
                addedFluids.add(addingRegistryName)
            }
        }

        if (ModList.get().isLoaded("mekanism")) {
            val addedGases = mutableSetOf<ResourceLocation>()

            for (gas in MekanismGases.GASES.entries) {
                val gas = gas.get()

                if (gas.isEmpty) continue

                val itemStack = defaultInstance
                initCapabilities(itemStack, null)

                val capability = itemStack.gasCapability
                capability?.insertChemical(gas.getStack(containerSizeMb.toLong()), MekanismAction.EXECUTE)

                val addingRegistryName = gas.registryName

                if (addingRegistryName != null && addingRegistryName !in addedGases) {
                    items.add(itemStack)
                    addedGases.add(addingRegistryName)
                }
            }

            val addedInfuses = mutableSetOf<ResourceLocation>()

            for (infuse in MekanismInfuseTypes.INFUSE_TYPES.entries) {
                val infuse = infuse.get()

                if (infuse.isEmpty) continue

                val itemStack = defaultInstance
                initCapabilities(itemStack, null)

                val capability = itemStack.infuseCapability
                capability?.insertChemical(infuse.getStack(containerSizeMb.toLong()), MekanismAction.EXECUTE)

                val addingRegistryName = infuse.registryName

                if (addingRegistryName != null && addingRegistryName !in addedInfuses) {
                    items.add(itemStack)
                    addedInfuses.add(addingRegistryName)
                }
            }

            val addedPigments = mutableSetOf<ResourceLocation>()

            for (pigment in MekanismPigments.PIGMENTS.entries) {
                val pigment = pigment.get()

                if (pigment.isEmpty) continue

                val itemStack = defaultInstance
                initCapabilities(itemStack, null)

                val capability = itemStack.pigmentCapability
                capability?.insertChemical(pigment.getStack(containerSizeMb.toLong()), MekanismAction.EXECUTE)

                val addingRegistryName = pigment.registryName

                if (addingRegistryName != null && addingRegistryName !in addedPigments) {
                    items.add(itemStack)
                    addedPigments.add(addingRegistryName)
                }
            }

            val addedSlurries = mutableSetOf<ResourceLocation>()

            for (slurry in MekanismSlurries.SLURRIES.entries) {
                val slurry = slurry.get()

                if (slurry.isEmpty) continue

                val itemStack = defaultInstance
                initCapabilities(itemStack, null)

                val capability = itemStack.slurryCapability
                capability?.insertChemical(slurry.getStack(containerSizeMb.toLong()), MekanismAction.EXECUTE)

                val addingRegistryName = slurry.registryName

                if (addingRegistryName != null && addingRegistryName !in addedSlurries) {
                    items.add(itemStack)
                    addedSlurries.add(addingRegistryName)
                }
            }
        }
    }

    final override fun initCapabilities(stack: ItemStack, nbt: CompoundNBT?): ICapabilityProvider {
        val providers = mutableListOf<ICapabilityProvider>()

        val simpleFluidCapability = SimpleFluidHandlerItemStackHandler(stack, containerSizeMb, isFractional)
        providers.add(simpleFluidCapability)

        if (ModList.get().isLoaded("mekanism")) {
            val simpleChemicalCapability = SimpleChemicalHandlerItemStackHandler(stack, containerSizeMb, isFractional)
            providers.add(simpleChemicalCapability)
            simpleFluidCapability.addOtherHandlers(simpleChemicalCapability)
            simpleChemicalCapability.addOtherHandlers(simpleFluidCapability)
        }

        return object : ICapabilityProvider {
            override fun <T : Any> getCapability(
                cap: Capability<T>,
                side: Direction?
            ): LazyOptional<T> = providers
                .map {
                    it.getCapability(cap, side)
                }
                .firstOrNull { it.isPresent }
                ?: LazyOptional.empty()
        }
    }
}
