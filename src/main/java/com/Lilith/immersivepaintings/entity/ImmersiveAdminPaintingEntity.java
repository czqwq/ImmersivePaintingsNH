package com.Lilith.immersivepaintings.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

import com.Lilith.immersivepaintings.Main;
import com.Lilith.immersivepaintings.registration.Items;
import com.Lilith.immersivepaintings.util.Permissions;

/**
 * Placed admin painting. Only players with {@link Permissions#ADMIN_LEVEL} may open the editor (right click)
 * or break it (left click); everyone else gets a localized chat warning and nothing happens.
 *
 * <p>
 * Unlike a normal painting it does not need a support block: {@link #survives()} always returns true, so the
 * painting keeps floating when the block behind it is removed instead of dropping off.
 *
 * <p>
 * It is also only destructible by the operator's own direct hit: {@link #attackEntityFrom} rejects direct hits from
 * non operators, explosions, projectiles, magic and mobs (Thaumcraft's primal orb used to blow it off the wall), and
 * {@link #setDead()} ignores foreign removal requests such as modded effects calling {@code setDead()} on entities.
 * The server holds the authoritative copy, so a client cannot delete it either.
 */
public class ImmersiveAdminPaintingEntity extends ImmersivePaintingEntity {

    /** Set while this entity removes itself as part of the operator destroy path, see {@link #attackEntityFrom}. */
    private boolean authorizedRemoval;

    private boolean foreignRemovalLogged;

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

    /**
     * Explosions push every entity in range by writing to the motion fields directly (the damage result does not
     * matter), so an admin painting must also be immovable or it would be "blown off the wall" without dying.
     */
    @Override
    public void moveEntity(double x, double y, double z) {}

    @Override
    public void addVelocity(double x, double y, double z) {}

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        Entity attacker = source.getEntity();

        if (!isDirectOperatorHit(source, attacker)) {
            if (!worldObj.isRemote) {
                if (attacker instanceof EntityPlayer) {
                    Permissions.sendDenied((EntityPlayer) attacker);
                } else {
                    Main.LOGGER.debug(
                        "Blocked '{}' damage on operator painting at {}, {}, {}",
                        source.getDamageType(),
                        posX,
                        posY,
                        posZ);
                }
            }
            return false;
        }

        // Only the operator's own hit may flag this entity dead, foreign setDead calls are ignored below.
        authorizedRemoval = true;
        try {
            return super.attackEntityFrom(source, amount);
        } finally {
            authorizedRemoval = false;
        }
    }

    /**
     * Only our own operator destroy path may remove this entity. Modded effects that delete entities directly
     * ({@code entity.setDead()}, {@code world.removeEntity(entity)}, entity cleanup commands) are ignored on the
     * server; the client side keeps the normal removal so destroy packets still despawn it there.
     */
    @Override
    public void setDead() {
        if (!worldObj.isRemote && !authorizedRemoval) {
            if (!foreignRemovalLogged) {
                foreignRemovalLogged = true;
                Main.LOGGER.warn(
                    "Blocked a foreign removal attempt on the operator painting at {}, {}, {} (dim {})",
                    posX,
                    posY,
                    posZ,
                    worldObj.provider.dimensionId);
            }
            return;
        }

        super.setDead();
    }

    /**
     * True only for a direct left click from an operator: arrows, thrown items, explosions (even ones placed by an
     * operator), magic and fire can never break the painting.
     */
    private static boolean isDirectOperatorHit(DamageSource source, Entity attacker) {
        if (!(attacker instanceof EntityPlayer) || !Permissions.isAdmin((EntityPlayer) attacker)) return false;

        return source.getSourceOfDamage() == attacker && !source.isProjectile()
            && !source.isExplosion()
            && !source.isMagicDamage()
            && !source.isFireDamage();
    }
}
