package dev.aaur1s.minecraft.fluidcells.util

import dev.aaur1s.minecraft.fluidcells.api.item.handler.SimpleChemicalHandlerItemStackHandler
import dev.aaur1s.minecraft.fluidcells.api.item.handler.SimpleChemicalHandlerItemStackHandler.Companion.CAPABILITY
import mekanism.api.chemical.Chemical
import mekanism.api.chemical.ChemicalStack
import mekanism.api.chemical.gas.Gas
import mekanism.api.chemical.gas.GasStack
import mekanism.api.chemical.gas.IGasHandler
import mekanism.api.chemical.infuse.IInfusionHandler
import mekanism.api.chemical.infuse.InfuseType
import mekanism.api.chemical.infuse.InfusionStack
import mekanism.api.chemical.pigment.IPigmentHandler
import mekanism.api.chemical.pigment.Pigment
import mekanism.api.chemical.pigment.PigmentStack
import mekanism.api.chemical.slurry.ISlurryHandler
import mekanism.api.chemical.slurry.Slurry
import mekanism.api.chemical.slurry.SlurryStack
import mekanism.client.render.MekanismRenderer
import mekanism.common.capabilities.Capabilities.GAS_HANDLER_CAPABILITY
import mekanism.common.capabilities.Capabilities.INFUSION_HANDLER_CAPABILITY
import mekanism.common.capabilities.Capabilities.PIGMENT_HANDLER_CAPABILITY
import mekanism.common.capabilities.Capabilities.SLURRY_HANDLER_CAPABILITY
import mekanism.common.registration.WrappedDeferredRegister
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.fml.RegistryObject
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.IForgeRegistryEntry
import mekanism.api.Action as MekanismAction

val TileEntity.gasCapability: IGasHandler? get() = GAS_HANDLER_CAPABILITY?.let { getCapability(it).resolve().orElse(null) }
val ItemStack.gasCapability: IGasHandler? get() = GAS_HANDLER_CAPABILITY?.let { getCapability(it).resolve().orElse(null) }
val ItemStack.gasStack: GasStack get() = gasCapability?.extractChemical(Long.MAX_VALUE, MekanismAction.SIMULATE) ?: GasStack.EMPTY
val ItemStack.gas: Gas get() = gasStack.type

val TileEntity.infuseCapability: IInfusionHandler? get() = INFUSION_HANDLER_CAPABILITY?.let { getCapability(it).resolve().orElse(null) }
val ItemStack.infuseCapability: IInfusionHandler? get() = INFUSION_HANDLER_CAPABILITY?.let { getCapability(it).resolve().orElse(null) }
val ItemStack.infuseStack: InfusionStack get() = infuseCapability?.extractChemical(Long.MAX_VALUE, MekanismAction.SIMULATE) ?: InfusionStack.EMPTY
val ItemStack.infuse: InfuseType get() = infuseStack.type

val TileEntity.pigmentCapability: IPigmentHandler? get() = PIGMENT_HANDLER_CAPABILITY?.let { getCapability(it).resolve().orElse(null) }
val ItemStack.pigmentCapability: IPigmentHandler? get() = PIGMENT_HANDLER_CAPABILITY?.let { getCapability(it).resolve().orElse(null) }
val ItemStack.pigmentStack: PigmentStack get() = pigmentCapability?.extractChemical(Long.MAX_VALUE, MekanismAction.SIMULATE) ?: PigmentStack.EMPTY
val ItemStack.pigment: Pigment get() = pigmentStack.type

val TileEntity.slurryCapability: ISlurryHandler? get() = SLURRY_HANDLER_CAPABILITY?.let { getCapability(it).resolve().orElse(null) }
val ItemStack.slurryCapability: ISlurryHandler? get() = SLURRY_HANDLER_CAPABILITY?.let { getCapability(it).resolve().orElse(null) }
val ItemStack.slurryStack: SlurryStack get() = slurryCapability?.extractChemical(Long.MAX_VALUE, MekanismAction.SIMULATE) ?: SlurryStack.EMPTY
val ItemStack.slurry: Slurry get() = slurryStack.type

val ItemStack.simpleChemicalCapability: SimpleChemicalHandlerItemStackHandler? get() = CAPABILITY?.let { getCapability(it).resolve().orElse(null) }
val ItemStack.chemicalStack: ChemicalStack<*>? get() = simpleChemicalCapability?.chemicalStack
val ItemStack.chemical: Chemical<*>? get() = chemicalStack?.type

@Suppress("UNCHECKED_CAST")
val <T : ChemicalStack<*>> T.emptyStack get() = when (this) {
    is GasStack -> GasStack.EMPTY
    is InfusionStack -> InfusionStack.EMPTY
    is PigmentStack -> PigmentStack.EMPTY
    is SlurryStack -> SlurryStack.EMPTY
    else -> error("Unknown Chemical Type: ${this.type.javaClass.name}")
} as T

val Chemical<*>.isEmpty get() = this.isEmptyType
val Chemical<*>.isNotEmpty get() = !this.isEmpty

val ChemicalStack<*>.isNotEmpty get() = !this.isEmpty

val <T : IForgeRegistryEntry<T>> WrappedDeferredRegister<T>.forgeRegistry: DeferredRegister<T> get() {
    val internalProperty = WrappedDeferredRegister::class.java.getDeclaredField("internal").also { it.isAccessible = true }
    @Suppress("UNCHECKED_CAST")
    return internalProperty.get(this) as DeferredRegister<T>
}

val <T : IForgeRegistryEntry<T>> WrappedDeferredRegister<T>.entries: Set<RegistryObject<T>> get() = forgeRegistry.entries.toSet()

val Chemical<*>.textureAtlasSprite: TextureAtlasSprite get() = MekanismRenderer.getChemicalTexture(this)