package com.Lilith.immersivepaintings.entity;

import java.util.List;
import java.util.Optional;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.Lilith.immersivepaintings.Main;
import com.Lilith.immersivepaintings.Painting;
import com.Lilith.immersivepaintings.ServerPaintingManager;
import com.Lilith.immersivepaintings.config.CommonConfig;
import com.Lilith.immersivepaintings.network.NetworkHandler;
import com.Lilith.immersivepaintings.network.payload.s2c.OpenGuiPayload;
import com.Lilith.immersivepaintings.registration.Configs;
import com.Lilith.immersivepaintings.registration.Items;
import com.Lilith.immersivepaintings.util.Permissions;

/**
 * Hanging painting/graffiti entity. Extends the plain {@link Entity} instead of EntityHanging so
 * floor and ceiling placements with a free roll angle are supported like upstream.
 */
public class ImmersivePaintingEntity extends Entity {

    public static final int MAX_DEPTH_OFFSET = 16;

    private static final int HIT_WINDOW_TICKS = 10;
    private static final byte EVENT_WOBBLE = 22;

    // DataWatcher ids, identical on both sides.
    private static final int DATA_MOTIVE = 0;
    private static final int DATA_FRAME = 1;
    private static final int DATA_MATERIAL = 2;
    private static final int DATA_WIDTH = 3;
    private static final int DATA_HEIGHT = 4;
    private static final int DATA_DEPTH = 5;
    private static final int DATA_DIRECTION = 6;
    private static final int DATA_ROTATION = 7;

    private static final String NONE = Main.DOMAIN + ":none";

    public int hangingDirection = ForgeDirection.SOUTH.ordinal();
    public int rotation;
    public int tileX;
    public int tileY;
    public int tileZ;

    private int tickCounter;
    private long wobbleUntil;

    public ImmersivePaintingEntity(World world) {
        super(world);
        this.yOffset = 0.0F;
        setSize(0.5F, 0.5F);
        noClip = true;
    }

    @Override
    protected void entityInit() {
        dataWatcher.addObject(DATA_MOTIVE, NONE);
        dataWatcher.addObject(DATA_FRAME, NONE);
        dataWatcher.addObject(DATA_MATERIAL, NONE);
        dataWatcher.addObject(DATA_WIDTH, 1);
        dataWatcher.addObject(DATA_HEIGHT, 1);
        dataWatcher.addObject(DATA_DEPTH, 0);
        dataWatcher.addObject(DATA_DIRECTION, (byte) ForgeDirection.SOUTH.ordinal());
        dataWatcher.addObject(DATA_ROTATION, 0);
    }

    // ------------------------------------------------------------------
    // Placement and geometry
    // ------------------------------------------------------------------

    public void setDirection(int direction, int roll) {
        this.hangingDirection = clampDirection(direction);
        this.rotation = normalizeRotation(roll);

        if (!worldObj.isRemote) {
            dataWatcher.updateObject(DATA_DIRECTION, (byte) hangingDirection);
            dataWatcher.updateObject(DATA_ROTATION, rotation);
        }

        updateRotationFromDirection();
        recalculateBoundingBox();
    }

    /** Places the painting in front of the given block face and sets its centre position. */
    public void placeAt(int x, int y, int z, int direction, int roll) {
        this.tileX = x;
        this.tileY = y;
        this.tileZ = z;
        setDirection(direction, roll);

        double[][] matrix = currentRotationMatrix();
        double[] normal = multiply(matrix, 0, 0, 1);
        double[] tangentX = multiply(matrix, 1, 0, 0);
        double[] tangentY = multiply(matrix, 0, 1, 0);
        double dx = offsetForPaintingSize(getPaintingWidth());
        double dy = offsetForPaintingSize(getPaintingHeight());

        setPosition(
            x + 0.5D - normal[0] * 0.46875D + tangentX[0] * dx + tangentY[0] * dy,
            y + 0.5D - normal[1] * 0.46875D + tangentX[1] * dx + tangentY[1] * dy,
            z + 0.5D - normal[2] * 0.46875D + tangentX[2] * dx + tangentY[2] * dy);
        recalculateBoundingBox();
    }

    private static double offsetForPaintingSize(int size) {
        return size % 2 == 0 ? 0.5D : 0.0D;
    }

    private static int clampDirection(int direction) {
        return direction < 0 || direction > 5 ? ForgeDirection.SOUTH.ordinal() : direction;
    }

    private static int normalizeRotation(int roll) {
        int result = roll % 360;
        return result < 0 ? result + 360 : result;
    }

    private void updateRotationFromDirection() {
        ForgeDirection direction = ForgeDirection.getOrientation(hangingDirection);
        if (direction == ForgeDirection.UP) {
            rotationYaw = rotation;
            rotationPitch = 90.0F;
        } else if (direction == ForgeDirection.DOWN) {
            rotationYaw = rotation;
            rotationPitch = -90.0F;
        } else {
            rotationYaw = horizontalYaw(direction);
            rotationPitch = 0.0F;
        }
        prevRotationYaw = rotationYaw;
        prevRotationPitch = rotationPitch;
    }

    /** Same yaw mapping as 1.21 {@code Direction#get2DDataValue() * 90}. */
    private static float horizontalYaw(ForgeDirection direction) {
        switch (direction) {
            case SOUTH:
                return 0.0F;
            case WEST:
                return 90.0F;
            case NORTH:
                return 180.0F;
            case EAST:
                return 270.0F;
            default:
                return 0.0F;
        }
    }

    /** Rotation matrix matching the GL transform order used by the renderer. */
    private static double[][] rotationMatrixFor(float yaw, float pitch) {
        double yawRadians = Math.toRadians(-yaw);
        double pitchRadians = Math.toRadians(-pitch);

        double cosYaw = Math.cos(yawRadians);
        double sinYaw = Math.sin(yawRadians);
        double cosPitch = Math.cos(pitchRadians);
        double sinPitch = Math.sin(pitchRadians);

        // R = Ry(-yaw) * Rx(-pitch)
        double[][] ry = { { cosYaw, 0, sinYaw }, { 0, 1, 0 }, { -sinYaw, 0, cosYaw } };
        double[][] rx = { { 1, 0, 0 }, { 0, cosPitch, -sinPitch }, { 0, sinPitch, cosPitch } };
        return multiply(ry, rx);
    }

    private double[][] currentRotationMatrix() {
        float pitch = 0.0F;
        if (hangingDirection == ForgeDirection.UP.ordinal()) pitch = 90.0F;
        else if (hangingDirection == ForgeDirection.DOWN.ordinal()) pitch = -90.0F;
        return rotationMatrixFor(rotationYaw, pitch);
    }

    public void recalculateBoundingBox() {
        double[][] matrix = currentRotationMatrix();
        double width = getPaintingWidth();
        double height = getPaintingHeight();

        double[] axisX = multiply(matrix, 1, 0, 0);
        double[] axisY = multiply(matrix, 0, 1, 0);
        double[] normal = multiply(matrix, 0, 0, 1);

        double extentX = Math.abs(axisX[0]) * width / 2.0D + Math.abs(axisY[0]) * height / 2.0D
            + Math.abs(normal[0]) * 0.03125D;
        double extentY = Math.abs(axisX[1]) * width / 2.0D + Math.abs(axisY[1]) * height / 2.0D
            + Math.abs(normal[1]) * 0.03125D;
        double extentZ = Math.abs(axisX[2]) * width / 2.0D + Math.abs(axisY[2]) * height / 2.0D
            + Math.abs(normal[2]) * 0.03125D;

        boundingBox
            .setBounds(posX - extentX, posY - extentY, posZ - extentZ, posX + extentX, posY + extentY, posZ + extentZ);
    }

    public boolean survives() {
        if (worldObj == null) return false;

        if (Configs.COMMON.testIfSpaceEmpty && !worldObj.getCollidingBoundingBoxes(this, boundingBox)
            .isEmpty()) {
            return false;
        }

        ForgeDirection direction = ForgeDirection.getOrientation(hangingDirection);
        int blockX = tileX - direction.offsetX;
        int blockY = tileY - direction.offsetY;
        int blockZ = tileZ - direction.offsetZ;

        if (!worldObj.getBlock(blockX, blockY, blockZ)
            .getMaterial()
            .isSolid()) {
            return false;
        }

        @SuppressWarnings("unchecked")
        List<Entity> entities = worldObj.getEntitiesWithinAABBExcludingEntity(this, boundingBox);
        for (Entity entity : entities) {
            if (entity instanceof ImmersivePaintingEntity
                && ((ImmersivePaintingEntity) entity).hangingDirection == hangingDirection) {
                return false;
            }
        }

        return true;
    }

    // ------------------------------------------------------------------
    // Entity lifecycle
    // ------------------------------------------------------------------

    @Override
    public void onUpdate() {
        super.onUpdate();

        if (!worldObj.isRemote && tickCounter++ >= 100) {
            tickCounter = 0;
            if (!isDead && !survives()) {
                setDead();
                dropSelf();
            }
        }
    }

    @Override
    public void func_145781_i(int dataId) {
        super.func_145781_i(dataId);

        if (dataId == DATA_MOTIVE && !worldObj.isRemote) {
            MinecraftServer server = MinecraftServer.getServer();
            if (server != null) {
                Optional<Painting> painting = ServerPaintingManager.getPainting(server, getMotive());
                if (painting.isPresent()) {
                    setPaintingSize(
                        painting.get()
                            .width(),
                        painting.get()
                            .height());
                }
            }
        }

        if (dataId == DATA_DIRECTION || dataId == DATA_ROTATION) {
            updateRotationFromDirection();
        }

        if (dataId == DATA_MOTIVE || dataId == DATA_WIDTH
            || dataId == DATA_HEIGHT
            || dataId == DATA_DEPTH
            || dataId == DATA_DIRECTION
            || dataId == DATA_ROTATION) {
            recalculateBoundingBox();
        }
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (isEntityInvulnerable()) return false;
        if (Configs.COMMON.canOnlyBeDestroyedByPlayer && !(source.getEntity() instanceof EntityPlayer)) return false;

        if (!worldObj.isRemote) {
            long time = worldObj.getTotalWorldTime();
            if (time < wobbleUntil) {
                setDead();
                dropSelf();
                return true;
            }

            wobbleUntil = time + HIT_WINDOW_TICKS;
            worldObj.setEntityState(this, EVENT_WOBBLE);
        }

        return true;
    }

    @Override
    public void handleHealthUpdate(byte event) {
        if (event == EVENT_WOBBLE) {
            wobbleUntil = worldObj.getTotalWorldTime() + HIT_WINDOW_TICKS;
        } else {
            super.handleHealthUpdate(event);
        }
    }

    public float getWobble(float partialTicks) {
        float remaining = (float) (wobbleUntil - worldObj.getTotalWorldTime()) - partialTicks;
        if (remaining <= 0.0F) return 0.0F;
        return MathHelper.sin(remaining * ((float) Math.PI / 2.0F)) * remaining * 0.2F;
    }

    @Override
    public boolean interactFirst(EntityPlayer player) {
        if (!worldObj.isRemote && !Permissions.isAdventure(player)) {
            CommonConfig config = Configs.COMMON;
            NetworkHandler.sendToClient(
                (net.minecraft.entity.player.EntityPlayerMP) player,
                new OpenGuiPayload(
                    OpenGuiPayload.GuiType.EDITOR,
                    getEntityId(),
                    config.minPaintingResolution,
                    config.maxPaintingResolution,
                    config.showOtherPlayersPaintings,
                    config.uploadPermissionLevel));
        }
        return true;
    }

    @Override
    public boolean canBeCollidedWith() {
        return Configs.COMMON.paintingsHaveCollision;
    }

    @Override
    public void moveEntity(double x, double y, double z) {
        if (Configs.COMMON.canOnlyBeDestroyedByPlayer) return;
        super.moveEntity(x, y, z);
    }

    @Override
    public void addVelocity(double x, double y, double z) {
        if (Configs.COMMON.canOnlyBeDestroyedByPlayer) return;
        super.addVelocity(x, y, z);
    }

    @Override
    public ItemStack getPickedResult(net.minecraft.util.MovingObjectPosition target) {
        return new ItemStack(getItem());
    }

    protected void dropSelf() {
        if (!worldObj.getGameRules()
            .getGameRuleBooleanValue("doTileDrops")) return;

        playSound("dig.wood", 1.0F, 1.0F);
        EntityItem item = entityDropItem(new ItemStack(getItem()), 0.0F);
        if (item != null) {
            item.delayBeforeCanPickup = 10;
        }
    }

    public void playPlacementSound() {
        playSound("step.wood", 1.0F, 1.0F);
    }

    // ------------------------------------------------------------------
    // NBT
    // ------------------------------------------------------------------

    @Override
    protected void writeEntityToNBT(NBTTagCompound nbt) {
        nbt.setString("Motive", getMotiveString());
        nbt.setString("Frame", getFrameString());
        nbt.setString("Material", getMaterialString());
        nbt.setByte("Facing", (byte) hangingDirection);
        nbt.setInteger("Rotation", rotation);
        nbt.setInteger("DepthOffset", getDepthOffset());
        nbt.setInteger("TileX", tileX);
        nbt.setInteger("TileY", tileY);
        nbt.setInteger("TileZ", tileZ);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound nbt) {
        setFrameString(nbt.getString("Frame"));
        setMaterialString(nbt.getString("Material"));
        hangingDirection = clampDirection(nbt.getByte("Facing"));
        rotation = normalizeRotation(nbt.getInteger("Rotation"));
        tileX = nbt.getInteger("TileX");
        tileY = nbt.getInteger("TileY");
        tileZ = nbt.getInteger("TileZ");
        setDepthOffset(nbt.getInteger("DepthOffset"));
        setMotiveString(nbt.getString("Motive"));
        updateRotationFromDirection();
        recalculateBoundingBox();
    }

    // ------------------------------------------------------------------
    // Synced values
    // ------------------------------------------------------------------

    public Item getItem() {
        return Items.PAINTING;
    }

    public boolean isGraffiti() {
        return false;
    }

    public boolean isGlowing() {
        return false;
    }

    public int getDepthOffset() {
        return dataWatcher.getWatchableObjectInt(DATA_DEPTH);
    }

    public void setDepthOffset(int offset) {
        dataWatcher.updateObject(DATA_DEPTH, MathHelper.clamp_int(offset, -MAX_DEPTH_OFFSET, MAX_DEPTH_OFFSET));
    }

    public int getPaintingWidth() {
        return Math.max(1, dataWatcher.getWatchableObjectInt(DATA_WIDTH));
    }

    public int getPaintingHeight() {
        return Math.max(1, dataWatcher.getWatchableObjectInt(DATA_HEIGHT));
    }

    private void setPaintingSize(int width, int height) {
        dataWatcher.updateObject(DATA_WIDTH, Math.max(1, width));
        dataWatcher.updateObject(DATA_HEIGHT, Math.max(1, height));
    }

    public ResourceLocation getMotive() {
        return parseLocation(getMotiveString());
    }

    public void setMotive(ResourceLocation motive) {
        setMotiveString(motive == null ? NONE : motive.toString());
    }

    public ResourceLocation getFrame() {
        return parseLocation(getFrameString());
    }

    public void setFrame(ResourceLocation frame) {
        setFrameString(frame == null ? NONE : frame.toString());
    }

    public ResourceLocation getMaterial() {
        return parseLocation(getMaterialString());
    }

    public void setMaterial(ResourceLocation material) {
        setMaterialString(material == null ? NONE : material.toString());
    }

    private String getMotiveString() {
        return dataWatcher.getWatchableObjectString(DATA_MOTIVE);
    }

    private void setMotiveString(String value) {
        dataWatcher.updateObject(DATA_MOTIVE, value == null || value.isEmpty() ? NONE : value);
    }

    private String getFrameString() {
        return dataWatcher.getWatchableObjectString(DATA_FRAME);
    }

    private void setFrameString(String value) {
        dataWatcher.updateObject(DATA_FRAME, value == null || value.isEmpty() ? NONE : value);
    }

    private String getMaterialString() {
        return dataWatcher.getWatchableObjectString(DATA_MATERIAL);
    }

    private void setMaterialString(String value) {
        dataWatcher.updateObject(DATA_MATERIAL, value == null || value.isEmpty() ? NONE : value);
    }

    private static ResourceLocation parseLocation(String value) {
        if (value == null || value.isEmpty()) return new ResourceLocation(NONE);
        return new ResourceLocation(value);
    }

    // ------------------------------------------------------------------
    // Small vector helpers
    // ------------------------------------------------------------------

    private static double[][] multiply(double[][] a, double[][] b) {
        double[][] result = new double[3][3];
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                double sum = 0.0D;
                for (int k = 0; k < 3; k++) {
                    sum += a[row][k] * b[k][column];
                }
                result[row][column] = sum;
            }
        }
        return result;
    }

    private static double[] multiply(double[][] matrix, double x, double y, double z) {
        return new double[] { matrix[0][0] * x + matrix[0][1] * y + matrix[0][2] * z,
            matrix[1][0] * x + matrix[1][1] * y + matrix[1][2] * z,
            matrix[2][0] * x + matrix[2][1] * y + matrix[2][2] * z };
    }
}
