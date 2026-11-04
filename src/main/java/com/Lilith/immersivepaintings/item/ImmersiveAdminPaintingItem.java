package com.Lilith.immersivepaintings.item;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import com.Lilith.immersivepaintings.entity.ImmersiveAdminPaintingEntity;
import com.Lilith.immersivepaintings.entity.ImmersivePaintingEntity;
import com.Lilith.immersivepaintings.util.Permissions;

/**
 * Operator only painting. Placing, editing and breaking the placed painting are all gated on the
 * {@link Permissions#ADMIN_LEVEL} operator check, which is enforced on the server so a modified client
 * cannot bypass the GUI.
 */
public class ImmersiveAdminPaintingItem extends ImmersivePaintingItem {

    public ImmersiveAdminPaintingItem(String name) {
        super(name);
    }

    @Override
    protected Class<? extends ImmersivePaintingEntity> getEntityClass() {
        return ImmersiveAdminPaintingEntity.class;
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ) {
        // The client never predicts this placement: it cannot know the operator flag, and the server sends the
        // entity a tick later anyway. Non operators get the localized popup instead of a painting.
        if (world.isRemote) return false;

        if (!Permissions.isAdmin(player)) {
            Permissions.sendDenied(player);
            return false;
        }

        return super.onItemUse(stack, player, world, x, y, z, side, hitX, hitY, hitZ);
    }
}
