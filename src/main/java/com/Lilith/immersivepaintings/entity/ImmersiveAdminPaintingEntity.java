package com.Lilith.immersivepaintings.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

import com.Lilith.immersivepaintings.registration.Items;
import com.Lilith.immersivepaintings.util.Permissions;

/**
 * Placed admin painting. Only players with {@link Permissions#ADMIN_LEVEL} may open the editor (right click)
 * or break it (left click); everyone else gets a localized chat warning and nothing happens.
 *
 * <p>
 * Unlike a normal painting it does not need a support block: {@link #survives()} always returns true, so the
 * painting keeps floating when the block behind it is removed instead of dropping off.
 */
public class ImmersiveAdminPaintingEntity extends ImmersivePaintingEntity {

    public ImmersiveAdminPaintingEntity(World world) {
        super(world);
    }

    @Override
    public Item getItem() {
        return Items.ADMIN_PAINTING;
    }

    @Override
    public boolean isAdminOnly() {
        return true;
    }

    /** Admin paintings float, so breaking the support block never removes them (and never drops them). */
    @Override
    public boolean survives() {
        return true;
    }

    @Override
    public boolean interactFirst(EntityPlayer player) {
        if (!worldObj.isRemote && !Permissions.isAdmin(player)) {
            Permissions.sendDenied(player);
            return true;
        }

        return super.interactFirst(player);
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        Entity attacker = source.getEntity();
        if (attacker instanceof EntityPlayer && !Permissions.isAdmin((EntityPlayer) attacker)) {
            if (!worldObj.isRemote) Permissions.sendDenied((EntityPlayer) attacker);
            return false;
        }

        return super.attackEntityFrom(source, amount);
    }
}
