package dev.aaur1s.minecraft.fluidcells.mixin.minecraft;

import dev.aaur1s.minecraft.fluidcells.api.item.FluidCellItemBase;
import net.minecraft.item.ItemStack;
import net.minecraft.server.management.PlayerInteractionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PlayerInteractionManager.class)
public abstract class PlayerInteractionManagerMixin {
    @Redirect(method = "useItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;setCount(I)V"))
    private void useItem$fluidcells$removeCreativeOnlyShenanigans(ItemStack instance, int count) {
        if (instance.getItem() instanceof FluidCellItemBase) return;
        instance.setCount(count);
    }

    @Redirect(method = "useItemOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;setCount(I)V"))
    private void useItemOn$fluidcells$removeCreativeOnlyShenanigans(ItemStack instance, int count) {
        if (instance.getItem() instanceof FluidCellItemBase) return;
        instance.setCount(count);
    }
}
