package com.edgeburnmedia.puppyprotector.item;

import com.edgeburnmedia.puppyprotector.PuppyProtector;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.edgeburnmedia.puppyprotector.PuppyProtector.MODID;
import static com.edgeburnmedia.puppyprotector.PuppyProtector.isProtectedWithNbt;

public class ProtectingWandItem extends Item {
    public ProtectingWandItem() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public @NotNull InteractionResult interactLivingEntity(@NotNull ItemStack pStack, @NotNull Player pPlayer, @NotNull LivingEntity pInteractionTarget, @NotNull InteractionHand pUsedHand) {
        if (!pPlayer.getLevel().isClientSide()) {
            if (!isProtectedWithNbt(pInteractionTarget)) {
                pInteractionTarget.getPersistentData().putBoolean(MODID + ":is_protected", true);
                pPlayer.displayClientMessage(Component.translatable("puppyprotector.entity_protected", PuppyProtector.getEntityDescription(pInteractionTarget)).withStyle(ChatFormatting.GREEN), true);

            } else {
                pPlayer.displayClientMessage(Component.translatable("puppyprotector.entity_already_protected", PuppyProtector.getEntityDescription(pInteractionTarget)).withStyle(ChatFormatting.RED), true);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.sidedSuccess(pPlayer.getLevel().isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        super.appendHoverText(pStack, pLevel, pTooltipComponents, pIsAdvanced);
        pTooltipComponents.add(Component.translatable("item.puppyprotector.protecting_wand.desc"));
    }
}
