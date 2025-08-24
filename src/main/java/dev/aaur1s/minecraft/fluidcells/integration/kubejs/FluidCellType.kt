package dev.aaur1s.minecraft.fluidcells.integration.kubejs

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import dev.aaur1s.minecraft.fluidcells.api.item.FluidCellItemBase
import dev.latvian.kubejs.generator.AssetJsonGenerator
import dev.latvian.kubejs.item.ItemBuilder
import dev.latvian.kubejs.item.custom.ItemType
import net.minecraft.item.Item
import net.minecraft.util.ResourceLocation

object FluidCellType : ItemType(FluidCellsKubeJsPlugin.FLUIDCELL_TYPE) {

    override fun createItem(builder: ItemBuilder): Item {
        builder as? ItemBuilderFluidCellExtension ?: error("ItemBuilder mixin not applied")

        return FluidCellItemBase(
            builder.sizeMb,
            builder.isFractional,
            builder.createItemProperties()
        )
    }

    override fun generateAssets(builder: ItemBuilder, generator: AssetJsonGenerator) {
        builder as? ItemBuilderFluidCellExtension ?: error("ItemBuilder mixin not applied")

        val id = builder.id

        val model = JsonObject().apply {
            this["loader"] = "forge:bucket"
            this["fluid"] = "minecraft:empty"
            this["parent"] = if (!builder.parentModel.isEmpty()) {
                builder.parentModel
            } else {
                "forge:item/default"
            }

            this["textures"] = JsonObject().apply {
                this["base"] = "${id.namespace}:item/${id.path}"
                this["fluid"] = "${builder.fluidMaskName.namespace}:item/mask/${builder.fluidMaskName.path}"
                this["cover"] = "${id.namespace}:item/${id.path}"
            }
        }

        generator.json(ResourceLocation(id.namespace, "models/item/${id.path}"), model)
    }


    private operator fun JsonObject.set(fieldName: String, value: JsonElement) {
        this.add(fieldName, value)
    }

    private operator fun JsonObject.set(fieldName: String, value: String) {
        this.addProperty(fieldName, value)
    }
}