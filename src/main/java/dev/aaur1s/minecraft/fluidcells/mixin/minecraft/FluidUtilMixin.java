package dev.aaur1s.minecraft.fluidcells.mixin.minecraft;

import dev.aaur1s.minecraft.fluidcells.api.item.FluidCellItemBase;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Mixin(value = FluidUtil.class, remap = false)
public abstract class FluidUtilMixin {
    /// Fixes Forge bug with dupe
    /// @author Aaur1s
    /// @reason can't get doDrain argument from lambda context
    @Overwrite
    @Nonnull
    public static FluidActionResult tryEmptyContainer(@Nonnull ItemStack container, IFluidHandler fluidDestination, int maxAmount, @Nullable PlayerEntity player, boolean doDrain) {
        ItemStack containerCopy = ItemHandlerHelper.copyStackWithSize(container, 1); // do not modify the input
        return FluidUtil.getFluidHandler(containerCopy)
                .map(containerFluidHandler -> {

                    // We are acting on a COPY of the stack, so performing changes is acceptable even if we are simulating.
                    // Edit(Aaur1s): It's NOT fine, fluidDestination isn't copy so real transfer on simulate is dupe
                    FluidStack transfer = FluidUtil.tryFluidTransfer(fluidDestination, containerFluidHandler, maxAmount, doDrain);
                    if (transfer.isEmpty())
                        return FluidActionResult.FAILURE;

                    if (doDrain && player != null)
                    {
                        SoundEvent soundevent = transfer.getFluid().getAttributes().getEmptySound(transfer);
                        player.level.playSound(null, player.getX(), player.getY() + 0.5, player.getZ(), soundevent, SoundCategory.BLOCKS, 1.0F, 1.0F);
                    }

                    ItemStack resultContainer = containerFluidHandler.getContainer();
                    return new FluidActionResult(resultContainer);
                })
                .orElse(FluidActionResult.FAILURE);
    }
}
