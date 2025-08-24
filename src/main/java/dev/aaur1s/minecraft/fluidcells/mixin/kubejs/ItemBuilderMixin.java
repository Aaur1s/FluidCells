package dev.aaur1s.minecraft.fluidcells.mixin.kubejs;

import dev.aaur1s.minecraft.fluidcells.integration.kubejs.ItemBuilderFluidCellExtension;
import dev.latvian.kubejs.item.ItemBuilder;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * I really hate it, but in kubejs 1.16.5 there's no other clean way of doing this
 */
@Mixin(value = ItemBuilder.class, remap = false)
public abstract class ItemBuilderMixin implements ItemBuilderFluidCellExtension {
    @Unique public Integer sizeMb = null;
    @Unique public Boolean isFractional = null;
    @Unique public ResourceLocation fluidMaskName = get$this().id;

    @Override
    public int getSizeMb() {
        if (sizeMb == null) throw new IllegalStateException("sizeMb not set");
        return sizeMb;
    }

    @Override
    public boolean isFractional() {
        if (isFractional == null) throw new IllegalStateException("isFractional not set");
        return isFractional;
    }

    @Override
    public @NotNull ItemBuilder sizeMb(int sizeMb) {
        this.sizeMb = sizeMb;
        return get$this();
    }

    @Override
    @NotNull
    public ItemBuilder isFractional(boolean isFractional) {
        this.isFractional = isFractional;
        return get$this();
    }

    @Override
    public @NotNull ResourceLocation getFluidMaskName() {
        return fluidMaskName;
    }

    @Override
    @NotNull
    public ItemBuilder fluidMaskName(@NotNull ResourceLocation fluidMaskName) {
        this.fluidMaskName = fluidMaskName;
        return get$this();
    }

    @Override
    @NotNull
    public ItemBuilder get$this() {
        return (ItemBuilder)(Object)this;
    }
}
