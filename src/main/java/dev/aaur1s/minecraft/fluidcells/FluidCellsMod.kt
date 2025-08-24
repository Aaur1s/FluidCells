package dev.aaur1s.minecraft.fluidcells

import dev.aaur1s.minecraft.fluidcells.item.BasicFluidCellItem
import dev.aaur1s.minecraft.fluidcells.item.UniversalBasicFluidCellItem
import net.minecraft.item.Item
import net.minecraft.util.ResourceLocation
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.RegistryEvent
import net.minecraftforge.event.RegistryEvent.MissingMappings
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext

const val MOD_ID = "fluidcells"

@Mod(MOD_ID)
class FluidCellsMod {
    init {
        MinecraftForge.EVENT_BUS.register(this)
        FMLJavaModLoadingContext.get().modEventBus.register(this)
    }

    @SubscribeEvent
    fun registerItems(event: RegistryEvent.Register<Item>) {
        event.registry.register(BasicFluidCellItem)
        event.registry.register(UniversalBasicFluidCellItem)
    }

    @SubscribeEvent
    fun onMissingItemMappings(event: MissingMappings<Item>) {
        for (mapping in event.allMappings) {

            // minimal fix for 1.0 -> 2.0 migration
            val oldFluidCellIds = listOf(
                ResourceLocation(MOD_ID, "fluid_cell"),
                ResourceLocation(MOD_ID, "filled_fluid_cell")
            )

            if (mapping.key in oldFluidCellIds) {
                mapping.remap(BasicFluidCellItem)
            }
        }
    }
}