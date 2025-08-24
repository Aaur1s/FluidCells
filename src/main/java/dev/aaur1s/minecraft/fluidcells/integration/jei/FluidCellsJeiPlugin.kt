package dev.aaur1s.minecraft.fluidcells.integration.jei

import dev.aaur1s.minecraft.fluidcells.api.item.FluidCellItemBase
import mezz.jei.api.IModPlugin
import mezz.jei.api.JeiPlugin
import mezz.jei.api.registration.ISubtypeRegistration
import net.minecraft.util.ResourceLocation
import net.minecraftforge.registries.ForgeRegistries

@JeiPlugin
@Suppress("unused")
class FluidCellsJeiPlugin : IModPlugin {
    override fun getPluginUid() = ResourceLocation("fluidcells", "jei")

    override fun registerItemSubtypes(registration: ISubtypeRegistration) {
        val nonFractionalFluidCells =
            ForgeRegistries.ITEMS
                .filterIsInstance<FluidCellItemBase>()
                .filter { !it.isFractional }

        registration.useNbtForSubtypes(*nonFractionalFluidCells.toTypedArray())
    }
}