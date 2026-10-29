package com.mawlee.cointcore.oiia;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Places an {@link OiiaCatEntity} on the clicked face, facing the player. */
public class OiiaCatItem extends Item {
    public OiiaCatItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.cointcore.oiia_cat.use").withStyle(ChatFormatting.GRAY));
        // The model is CC BY 4.0: its author must be credited.
        tooltip.add(Component.translatable("tooltip.cointcore.oiia_cat.credit").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        Vec3 at = Vec3.atBottomCenterOf(pos);
        OiiaCatEntity cat = OiiaCat.ENTITY.get().create(level);
        if (cat == null) {
            return InteractionResult.FAIL;
        }
        float yaw = player == null ? 0.0F : player.getYRot() + 180.0F;
        cat.moveTo(at.x, at.y, at.z, yaw, 0.0F);
        cat.setYHeadRot(yaw);
        if (!level.noCollision(cat, cat.getBoundingBox()) || !level.getEntities(cat, cat.getBoundingBox()).isEmpty()) {
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide) {
            if (player != null) {
                cat.setOwner(player.getUUID());
            }
            level.addFreshEntity(cat);
            context.getItemInHand().consume(1, player);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
