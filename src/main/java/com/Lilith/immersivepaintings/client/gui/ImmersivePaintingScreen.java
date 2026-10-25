package com.Lilith.immersivepaintings.client.gui;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import com.Lilith.immersivepaintings.ClientPaintingManager;
import com.Lilith.immersivepaintings.Main;
import com.Lilith.immersivepaintings.Painting;
import com.Lilith.immersivepaintings.client.gui.widget.CheckboxWidget;
import com.Lilith.immersivepaintings.client.gui.widget.IntegerSliderWidget;
import com.Lilith.immersivepaintings.client.gui.widget.PaintingWidget;
import com.Lilith.immersivepaintings.client.gui.widget.PercentageSliderWidget;
import com.Lilith.immersivepaintings.client.gui.widget.TexturedButtonWidget;
import com.Lilith.immersivepaintings.entity.ImmersivePaintingEntity;
import com.Lilith.immersivepaintings.network.LazyNetworkManager;
import com.Lilith.immersivepaintings.network.NetworkHandler;
import com.Lilith.immersivepaintings.network.payload.c2s.ImageUploadPayload;
import com.Lilith.immersivepaintings.network.payload.c2s.PaintingDeletePayload;
import com.Lilith.immersivepaintings.network.payload.c2s.PaintingEditPayload;
import com.Lilith.immersivepaintings.network.payload.c2s.PaintingFlagPayload;
import com.Lilith.immersivepaintings.network.payload.c2s.PaintingRegisterPayload;
import com.Lilith.immersivepaintings.registration.Configs;
import com.Lilith.immersivepaintings.resources.FrameLoader;
import com.Lilith.immersivepaintings.util.ImageManipulations;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Painting selection and upload GUI, ported from the upstream 1.21 screen. */
@SideOnly(Side.CLIENT)
public class ImmersivePaintingScreen extends GuiScreen {

    private static final int SCREENSHOTS_PER_PAGE = 5;

    final int minResolution;
    final int maxResolution;
    final boolean showOtherPlayersPaintings;
    final int uploadPermissionLevel;

    public final ImmersivePaintingEntity entity;

    private static String filteredString = "";
    private static int filteredResolution = 32;
    private static int filteredWidth;
    private static int filteredHeight;
    private final List<ResourceLocation> filteredPaintings = new ArrayList<>();

    private int selectionPage;
    private Page page;

    private GuiButton pageWidget;
    private GuiButton decreaseDepthButton;
    private GuiButton increaseDepthButton;

    private final Map<ResourceLocation, PaintingWidget> paintingWidgets = new HashMap<>();
    private final Map<GuiButton, List<String>> tooltips = new HashMap<>();
    private final Map<Integer, Runnable> actions = new HashMap<>();
    private int nextButtonId;

    private final List<GuiTextField> textFields = new ArrayList<>();
    private GuiTextField urlField;
    private GuiTextField nameField;
    private GuiTextField searchField;
    private GuiTextField widthFilterField;
    private GuiTextField heightFilterField;
    private String lastSearch = "";

    private BufferedImage currentImage;
    private static int currentImagePixelZoomCache = -1;
    private String currentImageName;
    private PixelatorSettings settings;
    private BufferedImage pixelatedImage;

    private ResourceLocation currentImageTexture;
    private int currentImageTextureWidth;
    private int currentImageTextureHeight;
    private ResourceLocation pixelatedTexture;
    private int pixelatedTextureWidth;
    private int pixelatedTextureHeight;
    private int tempTextureCounter;

    private List<File> screenshots = new ArrayList<>();
    private int screenshotPage;

    private ResourceLocation selectedPainting;
    private Page editReturnPage;
    private String error;
    private boolean shouldReProcess;
    private static volatile boolean shouldUpload;

    private final ExecutorService service = Executors.newSingleThreadExecutor();

    public ImmersivePaintingScreen(int entityId, int minResolution, int maxResolution,
        boolean showOtherPlayersPaintings, int uploadPermissionLevel) {
        super();

        this.minResolution = minResolution;
        this.maxResolution = maxResolution;
        this.showOtherPlayersPaintings = showOtherPlayersPaintings && Configs.CLIENT.showOtherPlayersPaintings;
        this.uploadPermissionLevel = uploadPermissionLevel;

        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.theWorld != null
            && minecraft.theWorld.getEntityByID(entityId) instanceof ImmersivePaintingEntity) {
            entity = (ImmersivePaintingEntity) minecraft.theWorld.getEntityByID(entityId);
        } else {
            entity = null;
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void initGui() {
        super.initGui();

        if (entity == null) {
            mc.displayGuiScreen(null);
            return;
        }

        if (page == null) {
            setPage(Page.DATAPACKS);
        } else {
            refreshPage();
        }

        File directory = new File(mc.mcDataDir, "screenshots");
        File[] files = directory.listFiles(
            file -> file.getName()
                .endsWith(".png"));
        if (files != null) {
            screenshots = new ArrayList<>(Arrays.asList(files));
        }
    }

    // ------------------------------------------------------------------
    // Rendering
    // ------------------------------------------------------------------

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        if (page == Page.FRAME) {
            decreaseDepthButton.enabled = entity.getDepthOffset() > -ImmersivePaintingEntity.MAX_DEPTH_OFFSET;
            increaseDepthButton.enabled = entity.getDepthOffset() < ImmersivePaintingEntity.MAX_DEPTH_OFFSET;
        }

        switch (page) {
            case NEW:
                drawGradientRect(
                    width / 2 - 115,
                    height / 2 - 68,
                    width / 2 + 115,
                    height / 2 - 41,
                    0x50000000,
                    0x50000000);
                break;
            case EDIT:
                drawGradientRect(
                    width / 2 - 160,
                    height / 2 - 60,
                    width / 2 + 160,
                    height / 2 + 60,
                    0x88000000,
                    0x88000000);
                break;
            case DELETE:
            case ADMIN_DELETE:
                drawGradientRect(
                    width / 2 - 160,
                    height / 2 - 50,
                    width / 2 + 160,
                    height / 2 + 50,
                    0x88000000,
                    0x88000000);
                break;
        }

        super.drawScreen(mouseX, mouseY, partialTicks);

        for (GuiTextField field : textFields) {
            field.drawTextBox();
        }

        switch (page) {
            case NEW: {
                List<String> wrapped = wrap(StatCollector.translateToLocal("immersivepaintingsnh.gui.drop"), 220);
                int y = height / 2 - 40 - wrapped.size() * 12;
                for (String text : wrapped) {
                    drawCenteredString(fontRendererObj, text, width / 2, y, 0xFFFFFFFF);
                    y += 12;
                }
                break;
            }
            case CREATE: {
                if (shouldReProcess && currentImage != null) {
                    service.submit(() -> pixelateImage());
                    shouldReProcess = false;
                }

                if (shouldUpload && pixelatedImage != null) {
                    pixelatedTexture = uploadImage(pixelatedImage, pixelatedTexture);
                    pixelatedTextureWidth = pixelatedImage.getWidth();
                    pixelatedTextureHeight = pixelatedImage.getHeight();
                    shouldUpload = false;
                }

                if (pixelatedTexture != null) {
                    int maxWidth = 190;
                    int maxHeight = 135;
                    float scale = Math
                        .min((float) maxWidth / pixelatedTextureWidth, (float) maxHeight / pixelatedTextureHeight);
                    drawTexture(
                        pixelatedTexture,
                        (int) (width / 2.0F - pixelatedTextureWidth * scale / 2.0F),
                        (int) (height / 2.0F - pixelatedTextureHeight * scale / 2.0F),
                        pixelatedTextureWidth,
                        pixelatedTextureHeight,
                        scale);
                }
                break;
            }
            case FRAME:
                drawCenteredString(
                    fontRendererObj,
                    StatCollector
                        .translateToLocalFormatted("immersivepaintingsnh.gui.depth_offset", entity.getDepthOffset()),
                    decreaseDepthButton.xPosition + 60,
                    decreaseDepthButton.yPosition + 6,
                    0xFFFFFF);
                break;
            case EDIT:
                ClientPaintingManager.getPainting(selectedPainting)
                    .ifPresent(
                        painting -> drawCenteredString(
                            fontRendererObj,
                            fontRendererObj.trimStringToWidth(painting.name(), 300),
                            width / 2,
                            height / 2 - 45,
                            0xFFFFFF));
                break;
            case DELETE: {
                List<String> wrapped = wrap(
                    StatCollector.translateToLocal("immersivepaintingsnh.gui.confirm_deletion"),
                    300);
                int y = height / 2 - 35;
                for (String text : wrapped) {
                    drawCenteredString(fontRendererObj, text, width / 2, y, 0xFFFFFF);
                    y += 15;
                }
                break;
            }
            case ADMIN_DELETE: {
                List<String> wrapped = wrap(
                    StatCollector.translateToLocal("immersivepaintingsnh.gui.confirm_admin_deletion"),
                    300);
                int y = height / 2 - 35;
                for (String text : wrapped) {
                    drawCenteredString(fontRendererObj, text, width / 2, y, 0xFFFFFF);
                    y += 15;
                }
                break;
            }
            case LOADING:
                drawCenteredString(
                    fontRendererObj,
                    StatCollector.translateToLocalFormatted(
                        "immersivepaintingsnh.gui.upload",
                        (int) Math.ceil(LazyNetworkManager.getRemainingTime())),
                    width / 2,
                    height / 2,
                    0xFFFFFFFF);
                break;
            default:
                break;
        }

        if (error != null) {
            drawCenteredString(fontRendererObj, error, width / 2, height / 2, 0xFFFF0000);
        }

        tooltips.clear();
        for (Map.Entry<GuiButton, List<String>> entry : new HashMap<>(screenTooltips()).entrySet()) {
            GuiButton button = entry.getKey();
            if (isHovered(button, mouseX, mouseY) && !entry.getValue()
                .isEmpty()) {
                drawHoveringText(entry.getValue(), mouseX, mouseY, fontRendererObj);
                break;
            }
        }
    }

    private Map<GuiButton, List<String>> screenTooltips() {
        return tooltips;
    }

    private boolean isHovered(GuiButton button, int mouseX, int mouseY) {
        return button.visible && mouseX >= button.xPosition
            && mouseY >= button.yPosition
            && mouseX < button.xPosition + button.width
            && mouseY < button.yPosition + button.height;
    }

    private void drawTexture(ResourceLocation identifier, int x, int y, int textureWidth, int textureHeight,
        float scale) {
        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, 0.0F);
        GL11.glScalef(scale, scale, 1.0F);
        mc.getTextureManager()
            .bindTexture(identifier);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        Gui.func_152125_a(
            0,
            0,
            0.0F,
            0.0F,
            textureWidth,
            textureHeight,
            textureWidth,
            textureHeight,
            textureWidth,
            textureHeight);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    private ResourceLocation uploadImage(BufferedImage image, ResourceLocation previous) {
        if (previous != null) {
            mc.getTextureManager()
                .deleteTexture(previous);
        }

        ResourceLocation identifier = new ResourceLocation(Main.DOMAIN, "gui/temp_" + tempTextureCounter++);
        mc.getTextureManager()
            .loadTexture(identifier, new DynamicTexture(image));
        return identifier;
    }

    // ------------------------------------------------------------------
    // Widget building
    // ------------------------------------------------------------------

    private GuiButton addButton(String label, int x, int y, int width, int height, Runnable action) {
        int id = nextButtonId++;
        GuiButton button = new GuiButton(id, x, y, width, height, label);
        actions.put(id, action);
        buttonList.add(button);
        return button;
    }

    private GuiTextField addTextField(int x, int y, int width, int height, String initial) {
        GuiTextField field = new GuiTextField(fontRendererObj, x, y, width, height);
        field.setMaxStringLength(256);
        field.setText(initial);
        textFields.add(field);
        return field;
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        Runnable action = actions.get(button.id);
        if (action != null) action.run();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        for (GuiTextField field : textFields) {
            field.mouseClicked(mouseX, mouseY, mouseButton);
        }

        if (mouseButton == 0) {
            for (GuiTextField field : textFields) {
                field.setFocused(false);
            }
            for (GuiTextField field : textFields) {
                if (mouseX >= field.xPosition && mouseX <= field.xPosition + field.width
                    && mouseY >= field.yPosition
                    && mouseY <= field.yPosition + field.height) {
                    field.setFocused(true);
                }
            }
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);

        // Right clicking a painting opens its edit page.
        if (mouseButton == 1) {
            for (PaintingWidget widget : paintingWidgets.values()) {
                if (isHovered(widget, mouseX, mouseY)) {
                    widget.pressRight();
                    break;
                }
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        for (GuiTextField field : textFields) {
            if (field.isFocused() && field.textboxKeyTyped(typedChar, keyCode)) {
                return;
            }
        }

        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();

        for (GuiTextField field : textFields) {
            field.updateCursorCounter();
        }

        if (searchField != null && !searchField.getText()
            .equals(lastSearch)) {
            lastSearch = searchField.getText();
            filteredString = lastSearch;
            updateSearch();
        }

        if (widthFilterField != null) {
            filteredWidth = parseFilter(widthFilterField.getText(), 0);
        }
        if (heightFilterField != null) {
            filteredHeight = parseFilter(heightFilterField.getText(), 0);
        }
    }

    private static int parseFilter(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        for (int i = 0; i < buttonList.size(); i++) {
            GuiButton button = buttonList.get(i);
            if (button instanceof com.Lilith.immersivepaintings.client.gui.widget.ExtendedSliderWidget) {
                ((com.Lilith.immersivepaintings.client.gui.widget.ExtendedSliderWidget<?>) button)
                    .mouseDragged(mc, mouseX, mouseY);
            }
        }
    }

    @Override
    protected void mouseMovedOrUp(int mouseX, int mouseY, int state) {
        super.mouseMovedOrUp(mouseX, mouseY, state);
        for (int i = 0; i < buttonList.size(); i++) {
            GuiButton button = buttonList.get(i);
            if (button instanceof com.Lilith.immersivepaintings.client.gui.widget.ExtendedSliderWidget) {
                ((com.Lilith.immersivepaintings.client.gui.widget.ExtendedSliderWidget<?>) button)
                    .mouseReleased(mouseX, mouseY);
            }
        }
    }

    @Override
    public void onGuiClosed() {
        if (currentImageTexture != null) mc.getTextureManager()
            .deleteTexture(currentImageTexture);
        if (pixelatedTexture != null) mc.getTextureManager()
            .deleteTexture(pixelatedTexture);
        service.shutdownNow();
    }

    public void updateWidget(ResourceLocation identifier) {
        if (paintingWidgets.containsKey(identifier)) {
            ClientPaintingManager.getPainting(identifier)
                .ifPresent(
                    painting -> paintingWidgets.get(identifier)
                        .update(
                            ClientPaintingManager.getImageIdentifier(identifier, Painting.Size.THUMBNAIL),
                            painting.width(),
                            painting.height()));
        }
    }

    public void refreshPage() {
        setPage(page);
    }

    public void clearError() {
        error = null;
    }

    public void setError(String text) {
        error = text;
    }

    public void setPage(Page page) {
        Page previousPage = this.page;
        this.page = page;
        if (page != previousPage && isPaintingSelectionPage(page)) {
            filteredResolution = page == Page.DATAPACKS ? 32 : 0;
        }

        rebuild();

        if (isPaintingSelectionPage(page)) {
            updateSearch();
        }
    }

    private static boolean isPaintingSelectionPage(Page page) {
        return page == Page.DATAPACKS || page == Page.PLAYERS || page == Page.YOURS;
    }

    private void rebuild() {
        buttonList.clear();
        actions.clear();
        textFields.clear();
        paintingWidgets.clear();
        tooltips.clear();
        urlField = null;
        nameField = null;
        searchField = null;
        widthFilterField = null;
        heightFilterField = null;

        if (page != Page.CREATE) {
            List<Page> buttons = new LinkedList<>();
            buttons.add(Page.YOURS);
            buttons.add(Page.DATAPACKS);
            if (showOtherPlayersPaintings || isOp()) buttons.add(Page.PLAYERS);
            if (canUpload()) buttons.add(Page.NEW);
            buttons.add(Page.FRAME);

            int x = width / 2 - 200;
            int w = 400 / buttons.size();
            for (final Page target : buttons) {
                String pageName = target == Page.FRAME && entity.isGraffiti() ? "depth"
                    : target.name()
                        .toLowerCase(Locale.ROOT);
                GuiButton button = addButton(
                    StatCollector.translateToLocal("immersivepaintingsnh.gui.page." + pageName),
                    x,
                    height / 2 - 90 - 22,
                    w,
                    20,
                    () -> setPage(target));
                button.enabled = target != this.page;
                x += w;
            }
        }

        switch (page) {
            case NEW:
                rebuildNewPage();
                break;
            case CREATE:
                rebuildCreatePage();
                break;
            case YOURS:
            case DATAPACKS:
            case PLAYERS:
                rebuildSelectionPage();
                break;
            case FRAME:
                rebuildFramePage();
                break;
            case EDIT:
                rebuildEditPage();
                break;
            case DELETE:
                addButton(
                    StatCollector.translateToLocal("immersivepaintingsnh.gui.cancel"),
                    width / 2 - 105,
                    height / 2 + 20,
                    100,
                    20,
                    () -> setPage(Page.EDIT));
                addButton(
                    StatCollector.translateToLocal("immersivepaintingsnh.gui.delete"),
                    width / 2 + 5,
                    height / 2 + 20,
                    100,
                    20,
                    () -> {
                        NetworkHandler.Client.sendToServer(new PaintingDeletePayload(selectedPainting, false));
                        setPage(Page.YOURS);
                    });
                break;
            case ADMIN_DELETE:
                addButton(
                    StatCollector.translateToLocal("immersivepaintingsnh.gui.cancel"),
                    width / 2 - 115,
                    height / 2 + 10,
                    70,
                    20,
                    () -> setPage(Page.EDIT));
                addButton(
                    StatCollector.translateToLocal("immersivepaintingsnh.gui.delete"),
                    width / 2 - 40,
                    height / 2 + 10,
                    70,
                    20,
                    () -> {
                        NetworkHandler.Client.sendToServer(new PaintingDeletePayload(selectedPainting, false));
                        setPage(Page.PLAYERS);
                    });
                addButton(
                    StatCollector.translateToLocal("immersivepaintingsnh.gui.delete_all"),
                    width / 2 + 35,
                    height / 2 + 10,
                    70,
                    20,
                    () -> {
                        NetworkHandler.Client.sendToServer(new PaintingDeletePayload(selectedPainting, true));
                        setPage(Page.PLAYERS);
                    });
                break;
            default:
                break;
        }
    }

    private void rebuildNewPage() {
        urlField = addTextField(width / 2 - 90, height / 2 - 38, 180, 16, "");

        addButton(
            StatCollector.translateToLocal("immersivepaintingsnh.gui.load"),
            width / 2 - 50,
            height / 2 - 15,
            100,
            20,
            () -> loadImage(urlField.getText()));

        rebuildScreenshots();

        addButton("<<", width / 2 - 65, height / 2 + 70, 30, 20, () -> setScreenshotPage(screenshotPage - 1));

        pageWidget = addButton("", width / 2 - 35, height / 2 + 70, 70, 20, () -> {});
        addButton(">>", width / 2 + 35, height / 2 + 70, 30, 20, () -> setScreenshotPage(screenshotPage + 1));

        setScreenshotPage(screenshotPage);
    }

    private void rebuildCreatePage() {
        nameField = addTextField(
            width / 2 - 90,
            height / 2 - 100,
            180,
            20,
            currentImageName == null ? "" : currentImageName);

        int y = height / 2 - 60;

        buttonList.add(
            new IntegerSliderWidget(
                nextButtonId++,
                width / 2 - 200,
                y,
                100,
                20,
                "immersivepaintingsnh.gui.width",
                settings.width,
                1,
                16,
                value -> {
                    settings.width = value;
                    shouldReProcess = true;
                }));
        y += 22;

        buttonList.add(
            new IntegerSliderWidget(
                nextButtonId++,
                width / 2 - 200,
                y,
                100,
                20,
                "immersivepaintingsnh.gui.height",
                settings.height,
                1,
                16,
                value -> {
                    settings.height = value;
                    shouldReProcess = true;
                }));
        y += 22;

        final int x = width / 2 - 200;
        final GuiButton resolutionWidget = addButton(String.valueOf(settings.resolution), x + 25, y, 50, 20, () -> {});
        tooltip(resolutionWidget, "immersivepaintingsnh.gui.tooltip.resolution");

        addButton("<", x, y, 25, 20, () -> {
            settings.resolution = Math.max(minResolution, settings.resolution / 2);
            if (settings.pixelArt) {
                adaptToPixelArt();
                refreshPage();
            }
            shouldReProcess = true;
            resolutionWidget.displayString = String.valueOf(settings.resolution);
        });
        addButton(">", x + 75, y, 25, 20, () -> {
            settings.resolution = Math.min(maxResolution, settings.resolution * 2);
            if (settings.pixelArt) {
                adaptToPixelArt();
                refreshPage();
            }
            shouldReProcess = true;
            resolutionWidget.displayString = String.valueOf(settings.resolution);
        });
        tooltip(resolutionWidget, "immersivepaintingsnh.gui.tooltip.resolution");

        y += 32;

        IntegerSliderWidget colorSlider = new IntegerSliderWidget(
            nextButtonId++,
            x,
            y,
            100,
            20,
            "immersivepaintingsnh.gui.colors",
            settings.colors,
            2,
            25,
            value -> {
                settings.colors = value;
                shouldReProcess = true;
            });
        colorSlider.enabled = !settings.pixelArt;
        buttonList.add(colorSlider);
        y += 22;

        PercentageSliderWidget ditherSlider = new PercentageSliderWidget(
            nextButtonId++,
            x,
            y,
            100,
            20,
            "immersivepaintingsnh.gui.dither",
            settings.dither,
            value -> {
                settings.dither = value;
                shouldReProcess = true;
            });
        ditherSlider.enabled = !settings.pixelArt;
        buttonList.add(ditherSlider);

        y = height / 2 - 50;

        buttonList.add(
            new CheckboxWidget(
                nextButtonId++,
                width / 2 + 100,
                y,
                100,
                20,
                StatCollector.translateToLocal("immersivepaintingsnh.gui.pixelart"),
                settings.pixelArt,
                value -> {
                    settings.pixelArt = value;
                    adaptToPixelArt();
                    refreshPage();
                    shouldReProcess = true;
                }));
        y += 22;

        buttonList.add(
            new CheckboxWidget(
                nextButtonId++,
                width / 2 + 100,
                y,
                100,
                20,
                StatCollector.translateToLocal("immersivepaintingsnh.gui.hide"),
                settings.hidden,
                value -> settings.hidden = value));
        y += 22;

        buttonList.add(
            new CheckboxWidget(
                nextButtonId++,
                width / 2 + 100,
                y,
                100,
                20,
                StatCollector.translateToLocal("immersivepaintingsnh.gui.nsfw"),
                settings.nsfw,
                value -> settings.nsfw = value));
        y += 22;

        buttonList.add(
            new PercentageSliderWidget(
                nextButtonId++,
                width / 2 + 100,
                y,
                100,
                20,
                "immersivepaintingsnh.gui.x_offset",
                settings.offsetX,
                value -> {
                    settings.offsetX = value;
                    shouldReProcess = true;
                }));
        y += 22;

        buttonList.add(
            new PercentageSliderWidget(
                nextButtonId++,
                width / 2 + 100,
                y,
                100,
                20,
                "immersivepaintingsnh.gui.y_offset",
                settings.offsetY,
                value -> {
                    settings.offsetY = value;
                    shouldReProcess = true;
                }));
        y += 22;

        PercentageSliderWidget zoomSlider = new PercentageSliderWidget(
            nextButtonId++,
            width / 2 + 100,
            y,
            100,
            20,
            "immersivepaintingsnh.gui.zoom",
            settings.zoom,
            entity.isGraffiti() ? 0.5D : 1.0D,
            entity.isGraffiti() ? 1.5D : 3.0D,
            value -> {
                settings.zoom = value;
                shouldReProcess = true;
            });
        zoomSlider.enabled = !settings.pixelArt;
        buttonList.add(zoomSlider);

        addButton(
            StatCollector.translateToLocal("immersivepaintingsnh.gui.cancel"),
            width / 2 - 85,
            height / 2 + 75,
            80,
            20,
            () -> setPage(Page.NEW));

        addButton(
            StatCollector.translateToLocal("immersivepaintingsnh.gui.save"),
            width / 2 + 5,
            height / 2 + 75,
            80,
            20,
            () -> {
                byte[] encoded;
                try {
                    encoded = ImageManipulations.encode(pixelatedImage);
                } catch (IOException e) {
                    Main.LOGGER.error("could not encode temp image", e);
                    return;
                }

                ImageManipulations.processByteArrayInChunks(
                    encoded,
                    (data, segment, total) -> LazyNetworkManager
                        .sendToServer(new ImageUploadPayload(data, segment, total)));

                EnumSet<Painting.Flag> flags = settings.getFlags();
                if (entity.isGraffiti()) flags.add(Painting.Flag.GRAFFITI);

                LazyNetworkManager.sendToServer(
                    new PaintingRegisterPayload(
                        settings.width,
                        settings.height,
                        settings.resolution,
                        currentImageName,
                        flags));

                setPage(Page.LOADING);
            });
    }

    private void rebuildSelectionPage() {
        rebuildPaintings();

        addButton("<<", width / 2 - 65, height / 2 + 80, 30, 20, () -> setSelectionPage(selectionPage - 1));
        pageWidget = addButton("", width / 2 - 35, height / 2 + 80, 70, 20, () -> {});
        addButton(">>", width / 2 + 35, height / 2 + 80, 30, 20, () -> setSelectionPage(selectionPage + 1));

        setSelectionPage(selectionPage);

        searchField = addTextField(width / 2 - 65, height / 2 - 88, 130, 16, filteredString);
        lastSearch = filteredString;

        int x = width / 2 - 200 + 12;
        final GuiButton resolutionWidget = addButton(
            String.valueOf(filteredResolution),
            x + 58,
            height / 2 - 90,
            25,
            20,
            () -> {});
        tooltip(resolutionWidget, "immersivepaintingsnh.gui.tooltip.filter_resolution");

        final GuiButton allWidget = addButton(
            StatCollector.translateToLocal("immersivepaintingsnh.gui.filter_all"),
            x,
            height / 2 - 90,
            50,
            20,
            () -> {
                filteredResolution = 0;
                resolutionWidget.displayString = "0";
                updateSearch();
            });
        tooltip(allWidget, "immersivepaintingsnh.gui.tooltip.filter_resolution");

        addButton("<", x + 33, height / 2 - 90, 25, 20, () -> {
            filteredResolution = filteredResolution == 0 ? 32 : Math.max(minResolution, filteredResolution / 2);
            resolutionWidget.displayString = String.valueOf(filteredResolution);
            updateSearch();
        });
        addButton(">", x + 83, height / 2 - 90, 25, 20, () -> {
            filteredResolution = filteredResolution == 0 ? 32 : Math.min(maxResolution, filteredResolution * 2);
            resolutionWidget.displayString = String.valueOf(filteredResolution);
            updateSearch();
        });

        widthFilterField = addTextField(
            width / 2 + 80,
            height / 2 - 88,
            40,
            16,
            filteredWidth == 0 ? "" : String.valueOf(filteredWidth));
        heightFilterField = addTextField(
            width / 2 + 120,
            height / 2 - 88,
            40,
            16,
            filteredHeight == 0 ? "" : String.valueOf(filteredHeight));
    }

    private void rebuildFramePage() {
        int y = height / 2 - 80;
        List<ResourceLocation> frames = new ArrayList<>();
        if (!entity.isGraffiti()) {
            for (FrameLoader.Frame value : FrameLoader.frames.values()) {
                if (!frames.contains(value.frame())) frames.add(value.frame());
            }
            frames.sort(Comparator.comparing(ResourceLocation::toString));
        }

        for (final ResourceLocation frame : frames) {
            GuiButton button = addButton(
                StatCollector.translateToLocal("immersivepaintingsnh.frame." + identifierToTranslation(frame)),
                width / 2 - 200,
                y,
                100,
                20,
                () -> {
                    List<ResourceLocation> materials = getMaterialsList(frame);
                    ResourceLocation material = materials.isEmpty() ? Main.locate("textures/block/frame/simple/oak.png")
                        : materials.get(0);

                    entity.setFrame(frame);
                    entity.setMaterial(material);

                    Map<PaintingEditPayload.Option, String> options = new HashMap<>();
                    options.put(PaintingEditPayload.Option.FRAME, frame.toString());
                    options.put(PaintingEditPayload.Option.MATERIAL, material.toString());
                    NetworkHandler.Client.sendToServer(new PaintingEditPayload(entity.getEntityId(), options));
                    setPage(Page.FRAME);
                });
            button.enabled = !frame.equals(entity.getFrame());
            y += 25;
        }

        int py = 0;
        int px = 0;
        List<ResourceLocation> materials = entity.isGraffiti() ? new ArrayList<>()
            : getMaterialsList(entity.getFrame());
        final List<TexturedButtonWidget> materialButtons = new LinkedList<>();
        for (final ResourceLocation material : materials) {
            ResourceLocation guiTexture = new ResourceLocation(
                material.getResourceDomain(),
                material.getResourcePath()
                    .replace("/block/", "/gui/"));
            TexturedButtonWidget widget = new TexturedButtonWidget(
                nextButtonId++,
                width / 2 - 80 + px * 65,
                height / 2 - 80 + py * 20,
                64,
                16,
                guiTexture,
                64,
                32,
                "",
                () -> {
                    entity.setMaterial(material);
                    Map<PaintingEditPayload.Option, String> options = new HashMap<>();
                    options.put(PaintingEditPayload.Option.MATERIAL, material.toString());
                    NetworkHandler.Client.sendToServer(new PaintingEditPayload(entity.getEntityId(), options));
                    for (TexturedButtonWidget button : materialButtons) {
                        button.enabled = true;
                    }
                });
            widget.enabled = !material.equals(entity.getMaterial());
            materialButtons.add(widget);
            buttonList.add(widget);
            tooltips.put(
                widget,
                Collections.singletonList(
                    StatCollector
                        .translateToLocal("immersivepaintingsnh.material." + identifierToTranslation(material))));

            px++;
            if (px > 3) {
                px = 0;
                py++;
            }
        }

        addButton(
            StatCollector.translateToLocal("immersivepaintingsnh.gui.done"),
            width / 2 - 50,
            height / 2 + 70,
            100,
            20,
            this::onGuiClosedAction);

        int depthX = width / 2 + (entity.isGraffiti() ? -60 : 60);
        int depthY = height / 2 + (entity.isGraffiti() ? -10 : 70);

        decreaseDepthButton = addButton("<", depthX, depthY, 20, 20, () -> changeDepthOffset(-1));
        tooltip(decreaseDepthButton, "immersivepaintingsnh.gui.tooltip.depth_in");
        increaseDepthButton = addButton(">", depthX + 100, depthY, 20, 20, () -> changeDepthOffset(1));
        tooltip(increaseDepthButton, "immersivepaintingsnh.gui.tooltip.depth_out");
    }

    private void onGuiClosedAction() {
        mc.displayGuiScreen(null);
    }

    private void rebuildEditPage() {
        Optional<Painting> painting = ClientPaintingManager.getPainting(selectedPainting);
        if (!painting.isPresent()) {
            setPage(editReturnPage);
            return;
        }

        buttonList.add(
            new CheckboxWidget(
                nextButtonId++,
                width / 2 - 100,
                height / 2 - 20,
                120,
                20,
                StatCollector.translateToLocal("immersivepaintingsnh.gui.show"),
                !painting.get()
                    .has(Painting.Flag.HIDDEN),
                value -> NetworkHandler.Client
                    .sendToServer(new PaintingFlagPayload(selectedPainting, Painting.Flag.HIDDEN, !value))));

        buttonList.add(
            new CheckboxWidget(
                nextButtonId++,
                width / 2 - 100,
                height / 2 + 4,
                120,
                20,
                StatCollector.translateToLocal("immersivepaintingsnh.gui.nsfw"),
                painting.get()
                    .has(Painting.Flag.NSFW),
                value -> NetworkHandler.Client
                    .sendToServer(new PaintingFlagPayload(selectedPainting, Painting.Flag.NSFW, value))));

        addButton(
            StatCollector.translateToLocal("immersivepaintingsnh.gui.back"),
            width / 2 - 105,
            height / 2 + 32,
            100,
            20,
            () -> setPage(editReturnPage));

        addButton(
            StatCollector.translateToLocal("immersivepaintingsnh.gui.delete"),
            width / 2 + 5,
            height / 2 + 32,
            100,
            20,
            () -> setPage(editReturnPage == Page.YOURS ? Page.DELETE : Page.ADMIN_DELETE));
    }

    private void tooltip(GuiButton button, String translationKey) {
        tooltips.put(button, Collections.singletonList(StatCollector.translateToLocal(translationKey)));
    }

    private List<ResourceLocation> getMaterialsList(ResourceLocation frame) {
        List<ResourceLocation> materials = new ArrayList<>();
        for (FrameLoader.Frame value : FrameLoader.frames.values()) {
            if (value.frame()
                .equals(frame)) {
                materials.add(value.material());
            }
        }
        materials.sort(Comparator.comparing(ResourceLocation::toString));
        return materials;
    }

    // ------------------------------------------------------------------
    // Painting grid
    // ------------------------------------------------------------------

    private void rebuildPaintings() {
        for (PaintingWidget widget : paintingWidgets.values()) {
            buttonList.remove(widget);
        }
        paintingWidgets.clear();

        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 8; x++) {
                int index = y * 8 + x + selectionPage * 24;
                if (index < 0 || index >= filteredPaintings.size()) break;

                final ResourceLocation identifier = filteredPaintings.get(index);

                List<String> tooltip = new LinkedList<>();
                Optional<Painting> painting = ClientPaintingManager.getPainting(identifier);
                if (painting.isPresent()) {
                    Painting value = painting.get();
                    tooltip.add(value.name());
                    tooltip.add(
                        StatCollector.translateToLocalFormatted("immersivepaintingsnh.gui.by_author", value.author()));
                    tooltip.add(
                        StatCollector.translateToLocalFormatted(
                            "immersivepaintingsnh.gui.resolution",
                            value.width(),
                            value.height(),
                            value.resolution()));

                    if (page == Page.YOURS && value.has(Painting.Flag.HIDDEN)) {
                        tooltip.add(StatCollector.translateToLocal("immersivepaintingsnh.gui.hidden"));
                    }
                    if (page == Page.YOURS && value.has(Painting.Flag.NSFW)) {
                        tooltip.add(StatCollector.translateToLocal("immersivepaintingsnh.gui.nsfw"));
                    }
                    if (page == Page.YOURS || page == Page.PLAYERS && isOp()) {
                        tooltip.add(StatCollector.translateToLocal("immersivepaintingsnh.gui.right_click_to_modify"));
                    }
                }

                final PaintingWidget widget = new PaintingWidget(
                    nextButtonId++,
                    (int) (width / 2.0 + (x - 3.5) * 48) - 24,
                    height / 2 - 66 + y * 48,
                    46,
                    46,
                    () -> {
                        Map<PaintingEditPayload.Option, String> options = new HashMap<>();
                        options.put(PaintingEditPayload.Option.MOTIVE, identifier.toString());
                        NetworkHandler.Client.sendToServer(new PaintingEditPayload(entity.getEntityId(), options));
                        if (entity.isGraffiti()) {
                            onGuiClosedAction();
                        } else {
                            setPage(Page.FRAME);
                        }
                    },
                    () -> {
                        if (page == Page.YOURS || page == Page.PLAYERS && isOp()) {
                            selectedPainting = identifier;
                            editReturnPage = page;
                            setPage(Page.EDIT);
                        }
                    });

                paintingWidgets.put(identifier, widget);
                buttonList.add(widget);
                tooltips.put(widget, tooltip);
                updateWidget(identifier);
            }
        }
    }

    private void rebuildScreenshots() {
        for (PaintingWidget widget : paintingWidgets.values()) {
            buttonList.remove(widget);
        }
        paintingWidgets.clear();

        for (int x = 0; x < SCREENSHOTS_PER_PAGE; x++) {
            int index = x + screenshotPage * SCREENSHOTS_PER_PAGE;
            if (index < 0 || index >= screenshots.size()) break;

            final File file = screenshots.get(index);
            final ResourceLocation identifier = Main.locate("gui/screenshot_" + x);

            PaintingWidget widget = new PaintingWidget(
                nextButtonId++,
                width / 2 + (x - SCREENSHOTS_PER_PAGE / 2) * 68 - 32,
                height / 2 + 15,
                64,
                48,
                () -> {
                    BufferedImage image = widgetImage(identifier);
                    if (image != null) {
                        currentImage = image;
                        currentImagePixelZoomCache = -1;
                        currentImageName = file.getName();
                        settings = new PixelatorSettings(currentImage, minResolution, maxResolution);
                        setPage(Page.CREATE);
                        pixelateImage();
                    }
                },
                () -> {});
            widget.visible = true;
            paintingWidgets.put(identifier, widget);
            buttonList.add(widget);
            tooltips.put(widget, Collections.singletonList(file.getName()));

            service.submit(() -> {
                BufferedImage image = loadImage(file.getPath(), identifier);
                if (image != null) {
                    mc.func_152344_a(() -> widget.update(identifier, image));
                }
            });
        }
    }

    private BufferedImage widgetImage(ResourceLocation identifier) {
        PaintingWidget widget = paintingWidgets.get(identifier);
        return widget == null ? null : widget.getImage();
    }

    // ------------------------------------------------------------------
    // Search / pagination
    // ------------------------------------------------------------------

    private void updateSearch() {
        filteredPaintings.clear();

        UUID uuid = mc.thePlayer == null ? null : mc.thePlayer.getUniqueID();
        List<Map.Entry<ResourceLocation, Painting>> entries = new ArrayList<>(
            ClientPaintingManager.getPaintings()
                .entrySet());
        entries.sort(
            Comparator.comparing(
                entry -> entry.getValue()
                    .name()));

        for (Map.Entry<ResourceLocation, Painting> entry : entries) {
            Painting painting = entry.getValue();
            boolean matchesPage = (page == Page.YOURS && !painting.is(Painting.Type.DATAPACK)
                && painting.authorUUID()
                    .equals(uuid))
                || (page == Page.PLAYERS && !painting.is(Painting.Type.DATAPACK)
                    && (!painting.has(Painting.Flag.HIDDEN) || isOp())
                    && (!painting.has(Painting.Flag.NSFW) || Configs.CLIENT.showNSFWPaintings || isOp()))
                || (page == Page.DATAPACKS && painting.is(Painting.Type.DATAPACK));

            if (!matchesPage) continue;
            if (painting.has(Painting.Flag.GRAFFITI) != entity.isGraffiti()) continue;
            if (!String.valueOf(entry.getKey())
                .contains(filteredString)) continue;
            if (filteredResolution != 0 && painting.resolution() != filteredResolution) continue;
            if (filteredWidth != 0 && painting.width() != filteredWidth) continue;
            if (filteredHeight != 0 && painting.height() != filteredHeight) continue;

            filteredPaintings.add(entry.getKey());
        }

        setSelectionPage(selectionPage);
    }

    private void setSelectionPage(int page) {
        int maxPages = (int) Math.ceil(filteredPaintings.size() / 24.0D);
        selectionPage = Math.min(maxPages - 1, Math.max(0, page));
        rebuildPaintings();
        if (pageWidget != null) {
            pageWidget.displayString = (selectionPage + 1) + " / " + Math.max(1, maxPages);
        }
    }

    private void setScreenshotPage(int page) {
        int maxPages = Math.max(1, (int) Math.ceil((double) screenshots.size() / SCREENSHOTS_PER_PAGE));
        int oldPage = screenshotPage;
        screenshotPage = Math.min(maxPages - 1, Math.max(0, page));
        if (oldPage != screenshotPage) {
            rebuildScreenshots();
        }
        if (pageWidget != null) {
            pageWidget.displayString = (screenshotPage + 1) + " / " + maxPages;
        }
    }

    private void changeDepthOffset(int delta) {
        entity.setDepthOffset(entity.getDepthOffset() + delta);
        Map<PaintingEditPayload.Option, String> options = new HashMap<>();
        options.put(PaintingEditPayload.Option.DEPTH_OFFSET, Integer.toString(entity.getDepthOffset()));
        NetworkHandler.Client.sendToServer(new PaintingEditPayload(entity.getEntityId(), options));
    }

    private boolean isOp() {
        if (mc.thePlayer == null) return false;
        if (mc.isIntegratedServerRunning()) {
            MinecraftServer server = MinecraftServer.getServer();
            return server != null && server.getConfigurationManager()
                .func_152596_g(mc.thePlayer.getGameProfile());
        }
        return mc.thePlayer.capabilities.isCreativeMode;
    }

    private boolean canUpload() {
        return uploadPermissionLevel <= 0 || isOp();
    }

    // ------------------------------------------------------------------
    // Image loading / pixelating
    // ------------------------------------------------------------------

    private boolean loadImage(String path) {
        currentImage = loadImage(path, Main.locate("gui/temp"));
        currentImagePixelZoomCache = -1;
        if (currentImage != null) {
            currentImageName = toFileName(path);
            settings = new PixelatorSettings(currentImage, minResolution, maxResolution);
            setPage(Page.CREATE);
            pixelateImage();
            return true;
        }
        return false;
    }

    private String toFileName(String path) {
        path = path.replace("\\", "/");
        int lastSlash = path.lastIndexOf('/');
        int lastDot = path.lastIndexOf('.');
        if (lastDot < lastSlash) lastDot = path.length();
        return path.substring(lastSlash + 1, lastDot);
    }

    private BufferedImage loadImage(String path, ResourceLocation identifier) {
        try (InputStream stream = path.startsWith("http://") || path.startsWith("https://") ? openUrlStream(path)
            : new FileInputStream(path)) {
            BufferedImage image = javax.imageio.ImageIO.read(stream);
            if (image != null) {
                preprocessImage(image);
                mc.func_152344_a(() -> {
                    currentImageTexture = uploadImage(image, currentImageTexture);
                    currentImageTextureWidth = image.getWidth();
                    currentImageTextureHeight = image.getHeight();
                });
                return image;
            }
        } catch (IOException e) {
            Main.LOGGER.error("Failed to load image {}", path, e);
        }

        return null;
    }

    private static InputStream openUrlStream(String path) throws IOException {
        URLConnection connection = new URL(path).openConnection();
        connection.setRequestProperty("User-Agent", "ImmersivePaintings/1.0");
        return connection.getInputStream();
    }

    private void preprocessImage(BufferedImage image) {
        clearError();
        if (!entity.isGraffiti()) {
            for (int x = 0; x < image.getWidth(); x++) {
                for (int y = 0; y < image.getHeight(); y++) {
                    int color = image.getRGB(x, y);
                    int alpha = (color >> 24) & 255;
                    if (alpha != 255) {
                        if (error == null) {
                            setError(StatCollector.translateToLocal("immersivepaintingsnh.gui.graffiti_warning"));
                        }
                        color = (255 << 24) | (color & 0x00FFFFFF);
                        image.setRGB(x, y, color);
                    }
                }
            }
        }
    }

    private static int getCurrentImagePixelZoomCache(BufferedImage image) {
        if (currentImagePixelZoomCache < 0) {
            currentImagePixelZoomCache = ImageManipulations.scanForPixelArtMultiple(image);
        }
        return currentImagePixelZoomCache;
    }

    private void adaptToPixelArt() {
        double zoom = getCurrentImagePixelZoomCache(currentImage);
        settings.width = ImageManipulations
            .clampInt((int) (currentImage.getWidth() / zoom / settings.resolution), 1, 16);
        settings.height = ImageManipulations
            .clampInt((int) (currentImage.getHeight() / zoom / settings.resolution), 1, 16);
    }

    private void pixelateImage() {
        pixelatedImage = pixelateImage(currentImage, settings);
        shouldUpload = true;
    }

    public static BufferedImage pixelateImage(BufferedImage currentImage, PixelatorSettings settings) {
        BufferedImage result = new BufferedImage(
            settings.resolution * settings.width,
            settings.resolution * settings.height,
            BufferedImage.TYPE_INT_ARGB);

        float zoom;
        if (settings.pixelArt) {
            zoom = getCurrentImagePixelZoomCache(currentImage);
        } else {
            float fx = (float) currentImage.getWidth() / result.getWidth();
            float fy = (float) currentImage.getHeight() / result.getHeight();
            zoom = (float) (Math.min(fx, fy) / settings.zoom);
        }

        int ox = (int) ((currentImage.getWidth() - result.getWidth() * zoom) * settings.offsetX);
        int oy = (int) ((currentImage.getHeight() - result.getHeight() * zoom) * settings.offsetY);
        if (settings.pixelArt) {
            ox = ox / ((int) zoom) * ((int) zoom);
            oy = oy / ((int) zoom) * ((int) zoom);
        }

        ImageManipulations.resize(result, currentImage, zoom, ox, oy);

        if (settings.dither > 0 && !settings.pixelArt) {
            if (settings.colors > 1) {
                ImageManipulations.dither(result, settings.dither / settings.colors);
            } else {
                ImageManipulations.dither(result, settings.dither / 16.0D);
            }
        }

        if (settings.colors > 1 && !settings.pixelArt) {
            ImageManipulations.reduceColors(result, settings.colors);
        }

        return result;
    }

    public static String identifierToTranslation(ResourceLocation location) {
        String path = location.getResourcePath();
        String lastSplit = path.substring(path.lastIndexOf('/') + 1);
        int index = lastSplit.lastIndexOf('.');
        return index < 0 ? lastSplit : lastSplit.substring(0, index);
    }

    public static List<String> wrap(String text, int maxWidth) {
        return Minecraft.getMinecraft().fontRenderer.listFormattedStringToWidth(text, maxWidth);
    }

    public static String consolidate(List<String> textList) {
        if (textList == null || textList.isEmpty()) return "";
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < textList.size(); i++) {
            if (i > 0) builder.append('\n');
            builder.append(textList.get(i));
        }
        return builder.toString();
    }

    public enum Page {
        YOURS,
        DATAPACKS,
        PLAYERS,
        NEW,
        CREATE,
        FRAME,
        EDIT,
        DELETE,
        ADMIN_DELETE,
        LOADING
    }

    public static final class PixelatorSettings {

        public double dither;
        public int colors;
        public int resolution;
        public int width;
        public int height;
        public double offsetX;
        public double offsetY;
        public double zoom;
        public boolean pixelArt;
        public boolean hidden = true;
        public boolean nsfw;

        public PixelatorSettings(double dither, int colors, int resolution, int width, int height, double offsetX,
            double offsetY, double zoom, boolean pixelArt) {
            this.dither = dither;
            this.colors = colors;
            this.resolution = resolution;
            this.width = width;
            this.height = height;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.zoom = zoom;
            this.pixelArt = pixelArt;
        }

        PixelatorSettings(BufferedImage currentImage, int minResolution, int maxResolution) {
            this(0, 10, ImageManipulations.clampInt(64, minResolution, maxResolution), 1, 1, 0.5, 0.5, 1, false);

            double target = currentImage.getWidth() / (double) currentImage.getHeight();
            double bestScore = 100;

            double diagonalLength = Math.sqrt(
                currentImage.getWidth() * currentImage.getWidth()
                    + currentImage.getHeight() * currentImage.getHeight());
            double dw = currentImage.getWidth() / diagonalLength;
            double dh = currentImage.getHeight() / diagonalLength;
            for (double diagonal = 3.0F; diagonal < 6.0D; diagonal += target) {
                int pw = (int) Math.ceil(dw * diagonal);
                int ph = (int) Math.ceil(dh * diagonal);
                double score = Math.abs(pw / (double) ph - target) * Math.sqrt(5 + width + height);
                if (score < bestScore) {
                    width = ImageManipulations.clampInt(pw, 1, 16);
                    height = ImageManipulations.clampInt(ph, 1, 16);
                    bestScore = score;
                }
            }
        }

        public EnumSet<Painting.Flag> getFlags() {
            EnumSet<Painting.Flag> flags = EnumSet.noneOf(Painting.Flag.class);
            if (nsfw) flags.add(Painting.Flag.NSFW);
            if (hidden) flags.add(Painting.Flag.HIDDEN);
            return flags;
        }
    }
}
