package dev.aaur1s.minecraft.fluidcells.integration.kubejs

import dev.latvian.kubejs.KubeJSPlugin
import dev.latvian.kubejs.item.custom.ItemTypes

class FluidCellsKubeJsPlugin : KubeJSPlugin() {

    override fun init() {
        ItemTypes.register(FluidCellType)
    }

    companion object {
        const val FLUIDCELL_TYPE = "fluidcells:custom"
    }
}