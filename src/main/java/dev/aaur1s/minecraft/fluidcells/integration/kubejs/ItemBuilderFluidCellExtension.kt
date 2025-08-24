package dev.aaur1s.minecraft.fluidcells.integration.kubejs

import dev.latvian.kubejs.item.ItemBuilder
import net.minecraft.util.ResourceLocation

interface ItemBuilderFluidCellExtension {
    val sizeMb: Int
    fun sizeMb(sizeMb: Int): ItemBuilder

    val isFractional: Boolean
    fun isFractional(isFractional: Boolean): ItemBuilder

    val fluidMaskName: ResourceLocation
    fun fluidMaskName(fluidMaskName: ResourceLocation): ItemBuilder

    val `$this`: ItemBuilder
}