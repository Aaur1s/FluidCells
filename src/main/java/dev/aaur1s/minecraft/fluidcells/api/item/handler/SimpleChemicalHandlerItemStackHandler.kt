package dev.aaur1s.minecraft.fluidcells.api.item.handler

import dev.aaur1s.minecraft.fluidcells.api.item.FluidCellItemBase
import dev.aaur1s.minecraft.fluidcells.util.*
import mekanism.api.IContentsListener
import mekanism.api.chemical.*
import mekanism.api.chemical.gas.Gas
import mekanism.api.chemical.gas.GasStack
import mekanism.api.chemical.gas.IGasHandler
import mekanism.api.chemical.gas.IGasTank
import mekanism.api.chemical.infuse.IInfusionHandler
import mekanism.api.chemical.infuse.IInfusionTank
import mekanism.api.chemical.infuse.InfuseType
import mekanism.api.chemical.infuse.InfusionStack
import mekanism.api.chemical.merged.MergedChemicalTank
import mekanism.api.chemical.pigment.IPigmentHandler
import mekanism.api.chemical.pigment.IPigmentTank
import mekanism.api.chemical.pigment.Pigment
import mekanism.api.chemical.pigment.PigmentStack
import mekanism.api.chemical.slurry.ISlurryHandler
import mekanism.api.chemical.slurry.ISlurryTank
import mekanism.api.chemical.slurry.Slurry
import mekanism.api.chemical.slurry.SlurryStack
import mekanism.api.inventory.AutomationType
import mekanism.common.capabilities.Capabilities.GAS_HANDLER_CAPABILITY
import mekanism.common.capabilities.Capabilities.INFUSION_HANDLER_CAPABILITY
import mekanism.common.capabilities.Capabilities.PIGMENT_HANDLER_CAPABILITY
import mekanism.common.capabilities.Capabilities.SLURRY_HANDLER_CAPABILITY
import mekanism.common.capabilities.DynamicHandler.InteractPredicate
import mekanism.common.capabilities.chemical.dynamic.DynamicChemicalHandler
import mekanism.common.capabilities.chemical.dynamic.DynamicChemicalHandler.*
import mekanism.common.capabilities.chemical.variable.RateLimitChemicalTank.*
import mekanism.common.capabilities.merged.MergedTank
import mekanism.common.capabilities.merged.MergedTankContentsHandler
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundNBT
import net.minecraft.nbt.INBT
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.ActionResult
import net.minecraft.util.ActionResultType
import net.minecraft.util.Direction
import net.minecraft.util.Hand
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.CapabilityInject
import net.minecraftforge.common.capabilities.CapabilityManager
import net.minecraftforge.common.capabilities.ICapabilityProvider
import net.minecraftforge.common.util.LazyOptional
import kotlin.collections.addAll
import mekanism.api.Action as MekanismAction

class SimpleChemicalHandlerItemStackHandler(
    val itemStack: ItemStack,
    val containerSizeMb: Int,
    val isFractional: Boolean,
) : MergedTankContentsHandler<MergedChemicalTank>(), InteractionHandler<ChemicalStack<*>, MekanismAction>, ICapabilityProvider {
    private val otherHandlers = mutableSetOf<InteractionHandler<*, *>>()
    override val isActive get() = mergedTank.allTanks.any { !it.isEmpty }
    override val isActiveOthers get() = otherHandlers.all { it.isActive }
    val chemicalStack: ChemicalStack<*>? get() = mergedTank.allTanks.find { !it.isEmpty }?.stack

    override fun addOtherHandlers(vararg handler: InteractionHandler<*, *>) {
        otherHandlers.addAll(handler)
    }

    init {
        this.mergedTank = MergedTank.create(
            object : SimpleChemicalTank<Gas, GasStack>(
                object : DynamicGasHandler(
                    { this.gasTanks },
                    InteractPredicate.ALWAYS_TRUE,
                    InteractPredicate.ALWAYS_TRUE,
                    { this.onContentsChanged("GasTanks", this.gasTanks) }
                ) {
                    override fun insertChemical(
                        tank: Int,
                        stack: GasStack,
                        side: Direction?,
                        action: MekanismAction
                    ): GasStack {
                        if (tank != 0) return emptyStack

                        if (isActiveOthers) return stack.emptyStack

                        if (isFractional) return super.insertChemical(tank, stack, side, action)

                        val stackCopy = stack.copy()

                        if (isActive) return stack.emptyStack

                        if (stackCopy.isEmpty) return stack.emptyStack

                        if (stackCopy.amount > containerSizeMb) stackCopy.amount = containerSizeMb.toLong()

                        if (stackCopy.amount != containerSizeMb.toLong()) return stack.emptyStack

                        return super.insertChemical(tank, stack, side, action)
                    }

                    override fun extractChemical(
                        tank: Int,
                        amount: Long,
                        side: Direction?,
                        action: MekanismAction
                    ): GasStack {
                        if (tank != 0) return emptyStack

                        if (isActiveOthers) return emptyStack

                        if (isFractional) return super.extractChemical(tank, amount, side, action)

                        if (!isActive) return emptyStack

                        var newAmount = amount

                        if (newAmount == 0L) return emptyStack

                        if (newAmount > containerSizeMb) newAmount = containerSizeMb.toLong()

                        if (newAmount != containerSizeMb.toLong()) return emptyStack

                        return super.extractChemical(tank, newAmount, side, action)
                    }
                }.also { this.gasHandler = it }
            ), IGasTank, IGasHandler {},

            object : SimpleChemicalTank<InfuseType, InfusionStack>(
                DynamicInfusionHandler(
                    { this.infusionTanks },
                    InteractPredicate.ALWAYS_TRUE,
                    InteractPredicate.ALWAYS_TRUE,
                    { this.onContentsChanged("InfusionTanks", this.infusionTanks) }
                ).also { this.infusionHandler = it }
            ), IInfusionTank, IInfusionHandler {},

            object : SimpleChemicalTank<Pigment, PigmentStack>(
                DynamicPigmentHandler(
                    { this.pigmentTanks },
                    InteractPredicate.ALWAYS_TRUE,
                    InteractPredicate.ALWAYS_TRUE,
                    { this.onContentsChanged("PigmentTanks", this.pigmentTanks) }
                ).also { this.pigmentHandler = it }
            ), IPigmentTank, IPigmentHandler {},

            object : SimpleChemicalTank<Slurry, SlurryStack>(
                DynamicSlurryHandler(
                    { this.slurryTanks },
                    InteractPredicate.ALWAYS_TRUE,
                    InteractPredicate.ALWAYS_TRUE,
                    { this.onContentsChanged("SlurryTanks", this.slurryTanks) }
                ).also { this.slurryHandler = it }
            ), ISlurryTank, ISlurryHandler {},
        )

        this.gasTanks = listOf(mergedTank.gasTank)
        this.infusionTanks = listOf(mergedTank.infusionTank)
        this.pigmentTanks = listOf(mergedTank.pigmentTank)
        this.slurryTanks = listOf(mergedTank.slurryTank)
    }

    override fun <T : ChemicalStack<*>> fillIt(
        stack: T,
        action: MekanismAction
    ): T {
        val result = mergedTank.getTankForType(ChemicalType.getTypeFor(stack.type)).insert(stack.unsafeCast(), action, AutomationType.MANUAL).unsafeCast<T>()

        this.onContentsChanged(result)

        return result
    }

    override fun <T : ChemicalStack<*>> drainIt(
        stack: T,
        action: MekanismAction
    ): T {
        val result = mergedTank.getTankForType(ChemicalType.getTypeFor(stack.type)).extract(stack.amount, action, AutomationType.MANUAL).unsafeCast<T>()

        this.onContentsChanged(result)

        return result
    }

    private fun onContentsChanged(stack: ChemicalStack<*>) = when (stack) {
        is GasStack -> onContentsChanged("GasTanks", this.gasTanks)
        is InfusionStack -> onContentsChanged("InfusionTanks", this.infusionTanks)
        is PigmentStack -> onContentsChanged("PigmentTanks", this.pigmentTanks)
        is SlurryStack -> onContentsChanged("SlurryTanks", this.slurryTanks)
        else -> Unit
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> Any.unsafeCast(): T = this as T

    override fun getStack(): ItemStack = itemStack

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

        val blockEntity = world.getBlockEntity(blockPos)
        val blockGasHandler = blockEntity?.gasCapability
        val blockInfuseHandler = blockEntity?.infuseCapability
        val blockPigmentHandler = blockEntity?.pigmentCapability
        val blockSlurryHandler = blockEntity?.slurryCapability

        val hasBlockHandler = blockGasHandler != null || blockInfuseHandler != null || blockPigmentHandler != null || blockSlurryHandler != null

        val singleChemicalStack = chemicalStack

        if (!hasBlockHandler && singleChemicalStack?.isEmpty == true) return ActionResult.fail(itemStack)

        val actionResult = if (hasBlockHandler && !player.isShiftKeyDown) {
            if (singleChemicalStack?.isNotEmpty == true) {
                val successInsert = tryInsertIntoBlock(blockEntity)
                if (successInsert) {
                    ActionResult.success(itemStack)
                } else {
                    val successExtract = tryExtractFromBlock(blockEntity)
                    if (successExtract) ActionResult.success(itemStack) else ActionResult.fail(itemStack)
                }
            } else {
                val successExtract = tryExtractFromBlock(blockEntity)
                if (successExtract) ActionResult.success(itemStack) else ActionResult.fail(itemStack)
            }
        } else {
            // chemicals not in-world
            ActionResult.fail(itemStack)
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

    private fun tryInsertIntoBlock(blockEntity: TileEntity?): Boolean {
        val blockHandlers = blockEntity.chemicalHandlers()

        fun execute(blockHandler: IChemicalHandler<*, ChemicalStack<*>>?): Boolean {
            blockHandler ?: return false
            val chemicalStack = chemicalStack ?: return false
            val inserted = blockHandler.insertChemical(chemicalStack, MekanismAction.SIMULATE) ?: return false
            if (inserted.isEmpty) return false

            val drained = drainIt(inserted, MekanismAction.SIMULATE)
            if (drained.amount > 0) {
                val actualInserted = blockHandler.insertChemical(drained, MekanismAction.EXECUTE)
                drainIt(actualInserted, MekanismAction.EXECUTE)
                return true
            }

            return false
        }

        return blockHandlers.any(::execute)
    }

    private fun tryExtractFromBlock(blockEntity: TileEntity?): Boolean {
        val blockHandlers = blockEntity.chemicalHandlers()

        fun execute(blockHandler: IChemicalHandler<*, ChemicalStack<*>>?): Boolean {
            blockHandler ?: return false
            val extracted = blockHandler.extractChemical(Long.MAX_VALUE, MekanismAction.SIMULATE) ?: return false
            if (extracted.isEmpty) return false

            val filled = fillIt(extracted, MekanismAction.SIMULATE)
            if (filled.amount > 0) {
                val actualExtracted = blockHandler.extractChemical(filled, MekanismAction.EXECUTE)
                fillIt(actualExtracted, MekanismAction.EXECUTE)
                return true
            }

            return false
        }

        return blockHandlers.any(::execute)
    }

    private fun TileEntity?.chemicalHandlers() = listOf<IChemicalHandler<*, ChemicalStack<*>>?>(
        this?.gasCapability?.unsafeCast(),
        this?.infuseCapability?.unsafeCast(),
        this?.pigmentCapability?.unsafeCast(),
        this?.slurryCapability?.unsafeCast(),
    )

    override fun init() {
        super.init()
    }

    override fun <T : Any> getCapability(
        cap: Capability<T>,
        side: Direction?
    ): LazyOptional<T> = when (cap) {
        CAPABILITY -> this
        GAS_HANDLER_CAPABILITY -> this.gasHandler
        INFUSION_HANDLER_CAPABILITY -> this.infusionHandler
        PIGMENT_HANDLER_CAPABILITY -> this.pigmentHandler
        SLURRY_HANDLER_CAPABILITY -> this.slurryHandler
        else -> null
    }.let { it?.let { LazyOptional.of { it }.cast() } ?: LazyOptional.empty() }

    private abstract inner class SimpleChemicalTank<CHEMICAL : Chemical<CHEMICAL>, STACK : ChemicalStack<CHEMICAL>>(
        listener: IContentsListener
    ) : BasicChemicalTank<CHEMICAL, STACK>(
        containerSizeMb.toLong(),
        { _, _ -> true },
        { _, _ -> true },
        { true },
        null,
        listener
    ) {
//        override fun insertChemical(tank: Int, stack: STACK, action: MekanismAction): STACK {
//            if (tank != 0) return emptyStack
//            return insert(stack, action, AutomationType.MANUAL)
//        }
//
//        override fun insert(stack: STACK, action: MekanismAction, automationType: AutomationType): STACK {
//            if (isActiveOthers) return stack.emptyStack
//
//            if (isFractional) return super.insert(stack, action, automationType)
//
//            val stackCopy = stack.copy()
//
//            if (isActive) return stack.emptyStack
//
//            if (stackCopy.isEmpty) return stack.emptyStack
//
//            if (stackCopy.amount > containerSizeMb) stackCopy.amount = containerSizeMb.toLong()
//
//            if (stackCopy.amount != containerSizeMb.toLong()) return stack.emptyStack
//
//            return super.insert(stack, action, automationType)
//        }
//
//        override fun extractChemical(tank: Int, amount: Long, action: MekanismAction): STACK {
//            if (tank != 0) return emptyStack
//            return extract(amount, action, AutomationType.MANUAL)
//        }
//
//        override fun extract(amount: Long, action: MekanismAction, automationType: AutomationType): STACK {
//            if (isActiveOthers) return stack.emptyStack
//
//            if (isFractional) return super.extract(stack.amount, action, automationType)
//
//            val stackCopy = stack.copy()
//
//            if (!isActive) return stack.emptyStack
//
//            if (stackCopy.isEmpty) return stack.emptyStack
//
//            if (stackCopy.amount > containerSizeMb) stackCopy.amount = containerSizeMb.toLong()
//
//            if (stackCopy.amount != containerSizeMb.toLong()) return stack.emptyStack
//
//            return super.extract(stack.amount, action, automationType)
//        }
    }

    companion object {
        @CapabilityInject(SimpleChemicalHandlerItemStackHandler::class)
        var CAPABILITY: Capability<SimpleChemicalHandlerItemStackHandler>? = null

        init {
            CapabilityManager.INSTANCE.register(
                SimpleChemicalHandlerItemStackHandler::class.java,
                object : Capability.IStorage<SimpleChemicalHandlerItemStackHandler> {
                    override fun writeNBT(
                        capability: Capability<SimpleChemicalHandlerItemStackHandler>,
                        instance: SimpleChemicalHandlerItemStackHandler,
                        side: Direction
                    ) = CompoundNBT()

                    override fun readNBT(
                        capability: Capability<SimpleChemicalHandlerItemStackHandler>,
                        instance: SimpleChemicalHandlerItemStackHandler,
                        side: Direction,
                        nbt: INBT
                    ) = Unit

                },
                { SimpleChemicalHandlerItemStackHandler(object : FluidCellItemBase(1, false) {}.defaultInstance, 1, false) }
            )
        }
    }
}