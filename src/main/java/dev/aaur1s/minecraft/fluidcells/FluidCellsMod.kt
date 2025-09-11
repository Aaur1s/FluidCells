package dev.aaur1s.minecraft.fluidcells

import dev.aaur1s.minecraft.fluidcells.client.FluidCellModel
import dev.aaur1s.minecraft.fluidcells.item.BasicFluidCellItem
import dev.aaur1s.minecraft.fluidcells.item.UniversalBasicFluidCellItem
import net.minecraft.item.Item
import net.minecraft.util.ResourceLocation
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.ModelRegistryEvent
import net.minecraftforge.client.model.ModelLoaderRegistry
import net.minecraftforge.event.RegistryEvent
import net.minecraftforge.event.RegistryEvent.MissingMappings
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.Mod.EventBusSubscriber

const val MOD_ID = "fluidcells"

@Mod(MOD_ID)
class FluidCellsMod {

    @EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = [Dist.CLIENT])
    object Client {
        @JvmStatic
        @SubscribeEvent
        fun clientSetup(event: ModelRegistryEvent) {
            ModelLoaderRegistry.registerLoader(ResourceLocation(MOD_ID, "substance_container"), FluidCellModel.Loader)
        }
    }

    @EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = [Dist.CLIENT, Dist.DEDICATED_SERVER])
    object Common {
        @JvmStatic
        @SubscribeEvent
        fun registerItems(event: RegistryEvent.Register<Item>) {
            event.registry.register(BasicFluidCellItem)
            event.registry.register(UniversalBasicFluidCellItem)
        }

        @JvmStatic
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
}