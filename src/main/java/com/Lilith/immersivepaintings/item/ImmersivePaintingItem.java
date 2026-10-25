package com.Lilith.immersivepaintings.item;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.Lilith.immersivepaintings.Main;
import com.Lilith.immersivepaintings.entity.ImmersivePaintingEntity;

/** Places a painting entity on the clicked block face. */
public class ImmersivePaintingItem extends Item {

    private final String displayNameKey;

    public ImmersivePaintingItem(String name) {
        this.displayNameKey = "item." + Main.DOMAIN + "." + name;
        setUnlocalizedName(Main.DOMAIN + "." + name);
        setTextureName(Main.DOMAIN + ":" + name);
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        return StatCollector.translateToLocal(displayNameKey);
    }

    protected Class<? extends ImmersivePaintingEntity> getEntityClass() {
        return ImmersivePaintingEntity.class;
    }

    protected ImmersivePaintingEntity createEntity(World world) {
        try {
            return getEntityClass().getConstructor(World.class)
                .newInstance(world);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to create painting entity", e);
        }
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ) {
        ForgeDirection direction = ForgeDirection.getOrientation(side);
        int attachmentX = x + direction.offsetX;
        int attachmentY = y + direction.offsetY;
        int attachmentZ = z + direction.offsetZ;

        if (!player.canPlayerEdit(attachmentX, attachmentY, attachmentZ, side, stack)) return false;

        int rotation = 0;
        if (direction == ForgeDirection.UP || direction == ForgeDirection.DOWN) {
            rotation = Math.floorMod((int) Math.floor(player.rotationYaw / 90.0F + 2.5) * 90, 360);
        }

        ImmersivePaintingEntity entity = createEntity(world);
        entity.placeAt(attachmentX, attachmentY, attachmentZ, side, rotation);

        if (entity.survives()) {
            if (!world.isRemote) {
                entity.playPlacementSound();
                world.spawnEntityInWorld(entity);
            }

            --stack.stackSize;
            return true;
        }

        return false;
    }
}
