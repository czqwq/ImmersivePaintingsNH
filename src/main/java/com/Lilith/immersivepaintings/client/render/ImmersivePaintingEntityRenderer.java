package com.Lilith.immersivepaintings.client.render;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.Lilith.immersivepaintings.ClientPaintingManager;
import com.Lilith.immersivepaintings.Main;
import com.Lilith.immersivepaintings.Painting;
import com.Lilith.immersivepaintings.entity.ImmersivePaintingEntity;
import com.Lilith.immersivepaintings.registration.Configs;
import com.Lilith.immersivepaintings.resources.FrameLoader;
import com.Lilith.immersivepaintings.resources.ObjectLoader;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import owens.oobjloader.Face;
import owens.oobjloader.FaceVertex;

/** Renders paintings by drawing the bundled OBJ meshes with the 1.7.10 tessellator. */
@SideOnly(Side.CLIENT)
public class ImmersivePaintingEntityRenderer extends Render {

    private static final double GRAFFITI_DEPTH_SCALE = 4.0 / (0.05 * (1 << 24));

    public ImmersivePaintingEntityRenderer() {
        this.shadowSize = 0.0F;
    }

    @Override
    public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTicks) {
        if (!(entity instanceof ImmersivePaintingEntity)) return;
        ImmersivePaintingEntity painting = (ImmersivePaintingEntity) entity;
        if (ClientPaintingManager.arePaintingsHidden()) return;

        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        GL11.glRotatef(-yaw, 0.0F, 1.0F, 0.0F);

        float pitch = painting.prevRotationPitch + (painting.rotationPitch - painting.prevRotationPitch) * partialTicks;
        GL11.glRotatef(-pitch, 1.0F, 0.0F, 0.0F);

        if (painting.isGraffiti()) {
            double distanceSquared = x * x + y * y + z * z;
            double radius = Math.hypot(painting.getPaintingWidth(), painting.getPaintingHeight()) * 0.5;
            double rasterOffset = (Math.sqrt(distanceSquared) + radius) * 2.0e-4;
            double offset = Math.max(rasterOffset, distanceSquared * GRAFFITI_DEPTH_SCALE);
            GL11.glTranslated(0.0D, 0.0D, offset);
        }

        GL11.glRotatef(painting.getWobble(partialTicks), 0.0F, 0.0F, 1.0F);
        GL11.glScalef(0.0625F, 0.0625F, 0.0625F);
        renderPainting(painting, partialTicks);
        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        GL11.glPopMatrix();
    }

    private void renderPainting(ImmersivePaintingEntity entity, float partialTicks) {
        int light = entity.getBrightnessForRender(partialTicks);
        int frameLight = light;
        if (entity.isGlowing()) {
            int block = light % 65536;
            int sky = light / 65536;
            light = packLight((int) (block * 0.25F + 240 * 0.75F), sky);
            frameLight = packLight((int) (block * 0.875F + 32), sky);
        }

        boolean hasFrame = !entity.getFrame()
            .getResourcePath()
            .equals("none");

        int widthPixels = entity.getPaintingWidth() * 16;
        int heightPixels = entity.getPaintingHeight() * 16;

        boolean graffiti = entity.isGraffiti();

        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
        if (graffiti) {
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glDepthMask(false);
        }

        applyLightmap(light);
        bindTexture(getTextureFor(entity));
        renderFaces(
            graffiti ? "objects/graffiti.obj" : "objects/canvas.obj",
            widthPixels,
            heightPixels,
            hasFrame ? 1.0F : 0.0F);

        if (hasFrame) {
            applyLightmap(frameLight);
            bindTexture(entity.getMaterial());
            renderFrame(entity, widthPixels, heightPixels);
        }

        if (graffiti) {
            GL11.glDepthMask(true);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glEnable(GL11.GL_ALPHA_TEST);
        }
    }

    private static int packLight(int block, int sky) {
        return (sky & 0xFFFF) << 16 | (block & 0xFFFF);
    }

    private static void applyLightmap(int light) {
        OpenGlHelper
            .setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float) (light % 65536), (float) (light / 65536));
    }

    private ResourceLocation getTextureFor(ImmersivePaintingEntity entity) {
        Minecraft client = Minecraft.getMinecraft();
        double distance = client.thePlayer == null ? 0.0D : client.thePlayer.getDistanceToEntity(entity);
        double blocksVisible = Math.tan(client.gameSettings.fovSetting / 180.0D * Math.PI / 2.0D) * 2.0D * distance;

        java.util.Optional<Painting> painting = ClientPaintingManager.getPainting(entity.getMotive());
        if (!painting.isPresent()) {
            return ClientPaintingManager.getImageIdentifier(Painting.DEFAULT_IDENTIFIER, Painting.Size.FULL);
        }

        int resolution = painting.get()
            .resolution();
        double pixelDensity = blocksVisible * resolution / client.displayHeight;

        Painting.Size size = pixelDensity > Configs.CLIENT.thumbResolutionThreshold ? Painting.Size.THUMBNAIL
            : pixelDensity > Configs.CLIENT.eighthResolutionThreshold ? Painting.Size.EIGHTH
                : pixelDensity > Configs.CLIENT.quarterResolutionThreshold ? Painting.Size.QUARTER
                    : pixelDensity > Configs.CLIENT.halfResolutionThreshold ? Painting.Size.HALF : Painting.Size.FULL;

        return ClientPaintingManager.getImageIdentifier(entity.getMotive(), size);
    }

    private List<Face> getFaces(ResourceLocation frame, String part) {
        ResourceLocation identifier = new ResourceLocation(
            frame.getResourceDomain(),
            frame.getResourcePath() + "/" + part + ".obj");
        List<Face> faces = ObjectLoader.objects.get(identifier);
        return faces == null ? java.util.Collections.emptyList() : faces;
    }

    private void renderFaces(String name, float width, float height, float margin) {
        List<Face> faces = ObjectLoader.objects.get(Main.locate(name));
        if (faces == null) return;

        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        for (Face face : faces) {
            for (FaceVertex vertex : face.vertices) {
                tessellator.setNormal(vertex.n.x, vertex.n.y, vertex.n.z);
                tessellator.addVertexWithUV(
                    vertex.v.x * (width - margin * 2),
                    vertex.v.y * (height - margin * 2),
                    vertex.v.z * 16.0F - 0.5F,
                    vertex.t.u * (width - margin * 2) / width + margin / width,
                    (1.0F - vertex.t.v) * (height - margin * 2) / height + margin / height);
            }
        }
        tessellator.draw();
    }

    private void renderFrame(ImmersivePaintingEntity entity, float width, float height) {
        ResourceLocation frame = entity.getFrame();

        List<Face> faces = getFaces(frame, "bottom");
        for (int x = 0; x < width / 16; x++) {
            float u = width == 16 ? 0.75F : (x == 0 ? 0.0F : x == width / 16 - 1 ? 0.5F : 0.25F);
            drawFrameFaces(faces, x * 16 - (width - 16) / 2, -(height - 16) / 2, u);
        }

        faces = getFaces(frame, "top");
        for (int x = 0; x < width / 16; x++) {
            float u = width == 16 ? 0.75F : (x == 0 ? 0.0F : x == width / 16 - 1 ? 0.5F : 0.25F);
            drawFrameFaces(faces, x * 16 - (width - 16) / 2, (height - 16) / 2, u);
        }

        faces = getFaces(frame, "right");
        for (int y = 0; y < height / 16; y++) {
            drawFrameFaces(faces, (width - 16) / 2, y * 16 - (height - 16) / 2, 0.25F);
        }

        faces = getFaces(frame, "left");
        for (int y = 0; y < height / 16; y++) {
            drawFrameFaces(faces, -(width - 16) / 2, y * 16 - (height - 16) / 2, 0.25F);
        }
    }

    private void drawFrameFaces(List<Face> faces, float offsetX, float offsetY, float uOffset) {
        if (faces.isEmpty()) return;

        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        for (Face face : faces) {
            for (FaceVertex vertex : face.vertices) {
                tessellator.setNormal(vertex.n.x, vertex.n.y, vertex.n.z);
                tessellator.addVertexWithUV(
                    vertex.v.x + offsetX,
                    vertex.v.y + offsetY,
                    vertex.v.z,
                    vertex.t.u * 0.25F + uOffset,
                    1.0F - vertex.t.v);
            }
        }
        tessellator.draw();
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return Painting.DEFAULT_IDENTIFIER;
    }

    /** Only used when the frame definition is missing, keeps the frame loader referenced. */
    static FrameLoader.Frame fallbackFrame() {
        return new FrameLoader.Frame(
            Main.locate("frame/simple"),
            false,
            Main.locate("textures/block/frame/simple/oak.png"));
    }
}
