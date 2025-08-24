package dev.aaur1s.minecraft.fluidcells.item

import dev.aaur1s.minecraft.fluidcells.MOD_ID
import dev.aaur1s.minecraft.fluidcells.api.item.FluidCellItemBase
import net.minecraft.util.ResourceLocation

object UniversalBasicFluidCellItem : FluidCellItemBase(containerSizeMb = 1000, isFractional = true) {
    init { registryName = ResourceLocation(MOD_ID, "universal_basic_fluid_cell") }
}