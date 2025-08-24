package dev.aaur1s.minecraft.fluidcells.item

import dev.aaur1s.minecraft.fluidcells.MOD_ID
import dev.aaur1s.minecraft.fluidcells.api.item.FluidCellItemBase
import net.minecraft.util.ResourceLocation

object BasicFluidCellItem : FluidCellItemBase(containerSizeMb = 1000, isFractional = false) {
    init { registryName = ResourceLocation(MOD_ID, "basic_fluid_cell") }
}