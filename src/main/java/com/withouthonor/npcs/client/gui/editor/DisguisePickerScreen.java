package com.withouthonor.npcs.client.gui.editor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.withouthonor.npcs.client.gui.ScaledScreen;
import com.withouthonor.npcs.client.gui.ScrollDrag;
import com.withouthonor.npcs.client.gui.SelectableEditBox;
import com.withouthonor.npcs.client.gui.VanillaUIHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Quaternionf;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class DisguisePickerScreen extends ScaledScreen {

    private static final int PAD = 8;
    private static final int HEADER_H = 22;
    private static final int LIST_W = 210;
    private static final int ROW_H = 12;

    private final Screen parent;

    private final Consumer<String> onPicked;

    private EditBox searchBox;
    private final List<ResourceLocation> allTypes = new ArrayList<>();
    private final List<ResourceLocation> filtered = new ArrayList<>();
    @Nullable
    private ResourceLocation selected;
    private int scroll;
    private final ScrollDrag scrollbars = new ScrollDrag();

    @Nullable
    private Entity previewEntity;
    @Nullable
    private ResourceLocation previewFor;
    private boolean previewBroken;

    private float previewAngle = 200.0F;
    private long lastFrameMs = System.currentTimeMillis();
    private boolean paused;
    private boolean rotDragging;
    private int frozenTick;

    private int winX, winY, winW, winH;
    private int listX, listY, listH;
    private int prevX, prevY, prevW, prevH;
    private int bottomY;

    public DisguisePickerScreen(Screen parent, @Nullable String current, Consumer<String> onPicked) {
        super(Component.translatable("wh_npcs.ui.disguise.title"));
        this.parent = parent;
        this.onPicked = onPicked;
        for (ResourceLocation key : ForgeRegistries.ENTITY_TYPES.getKeys()) {
            EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(key);

            if (type != null && net.minecraft.world.entity.ai.attributes.DefaultAttributes.hasSupplier(type)) {
                allTypes.add(key);
            }
        }
        allTypes.sort((a, b) -> a.toString().compareTo(b.toString()));
        if (current != null) {
            selected = ResourceLocation.tryParse(current);
        }
        applyFilter("");
    }

    private void applyFilter(String query) {
        filtered.clear();
        String q = query.toLowerCase(Locale.ROOT).trim();
        for (ResourceLocation key : allTypes) {
            if (q.isEmpty() || key.toString().contains(q) || displayName(key).toLowerCase(Locale.ROOT).contains(q)) {
                filtered.add(key);
            }
        }
        scroll = 0;
    }

    private String displayName(ResourceLocation key) {
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(key);
        return type != null ? type.getDescription().getString() : key.getPath();
    }

    @Override
    protected void init() {
        recalc();
        clearWidgets();
        String old = searchBox != null ? searchBox.getValue() : "";
        searchBox = addRenderableWidget(new SelectableEditBox(font, listX, winY + HEADER_H + 4, LIST_W, 16,
                Component.translatable("wh_npcs.ui.disguise.search")));
        searchBox.setMaxLength(64);
        searchBox.setValue(old);
        searchBox.setHint(Component.translatable("wh_npcs.ui.disguise.search_hint"));
        searchBox.setResponder(this::applyFilter);
        setFocused(searchBox);
    }

    @Override
    protected int designW() {
        return 470;
    }

    @Override
    protected int designH() {
        return 300;
    }

    private void recalc() {
        winW = 470;
        winH = 300;
        winX = (width - winW) / 2;
        winY = (height - winH) / 2;
        bottomY = winY + winH - PAD - 20;
        listX = winX + PAD;
        listY = winY + HEADER_H + 24;
        listH = bottomY - 22 - listY;
        prevX = listX + LIST_W + PAD;
        prevY = winY + HEADER_H + 4;
        prevW = winX + winW - PAD - prevX;
        prevH = bottomY - 22 - prevY;
    }

    @Override
    protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        recalc();
        scrollbars.beginFrame();
        VanillaUIHelper.drawWindow(g, winX, winY, winW, winH);
        g.fill(winX + 2, winY + 2, winX + winW - 2, winY + HEADER_H - 2, VanillaUIHelper.BG_HEADER);
        g.drawString(font, Component.translatable("wh_npcs.ui.disguise.header").getString(), winX + PAD, winY + 7, VanillaUIHelper.TEXT_YELLOW, false);
        String count = filtered.size() + " / " + allTypes.size();
        g.drawString(font, count, winX + winW - PAD - font.width(count), winY + 7,
                VanillaUIHelper.TEXT_DARK_GRAY, false);

        renderList(g, mouseX, mouseY);
        renderPreview(g, partialTick);

        g.drawString(font, Component.translatable("wh_npcs.ui.disguise.anim_note").getString(),
                winX + PAD, bottomY - 14, VanillaUIHelper.TEXT_DARK_GRAY, false);
        drawSmall(g, Component.translatable("wh_npcs.ui.disguise.choose").getString(), winX + winW - PAD - 226, bottomY, 70, mouseX, mouseY,
                selected != null ? VanillaUIHelper.TEXT_GREEN : VanillaUIHelper.TEXT_DARK_GRAY);
        drawSmall(g, Component.translatable("wh_npcs.ui.disguise.no_disguise").getString(), winX + winW - PAD - 150, bottomY, 100, mouseX, mouseY,
                VanillaUIHelper.TEXT_AQUA);
        drawSmall(g, Component.translatable("wh_npcs.ui.common.cancel").getString(), winX + winW - PAD - 44, bottomY, 44, mouseX, mouseY, VanillaUIHelper.TEXT_WHITE);
    }

    private void renderList(GuiGraphics g, int mouseX, int mouseY) {
        VanillaUIHelper.drawContentPanel(g, listX, listY, LIST_W, listH);
        int visibleRows = (listH - 8) / ROW_H;
        scroll = Math.max(0, Math.min(scroll, Math.max(0, filtered.size() - visibleRows)));
        int y = listY + 4;
        for (int i = scroll; i < Math.min(filtered.size(), scroll + visibleRows); i++) {
            ResourceLocation key = filtered.get(i);
            boolean isSelected = key.equals(selected);
            boolean hovered = isOver(mouseX, mouseY, listX + 2, y, LIST_W - 4, ROW_H);
            if (isSelected || hovered) {
                g.fill(listX + 2, y, listX + LIST_W - 2, y + ROW_H,
                        isSelected ? VanillaUIHelper.BG_SELECTED : VanillaUIHelper.BG_HOVERED);
            }
            String label = displayName(key) + " §8" + key;
            g.drawString(font, font.plainSubstrByWidth(label, LIST_W - 14), listX + 5, y + 2,
                    isSelected ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_WHITE, false);
            y += ROW_H;
        }
        VanillaUIHelper.drawScrollbar(g, listX + LIST_W - 6, listY + 3, listH - 6,
                filtered.size(), visibleRows, scroll, scrollbars, v -> scroll = v);
    }

    private void renderPreview(GuiGraphics g, float partialTick) {
        VanillaUIHelper.drawContentPanel(g, prevX, prevY, prevW, prevH);
        if (selected == null) {
            g.drawCenteredString(font, Component.translatable("wh_npcs.ui.disguise.pick_left").getString(), prevX + prevW / 2, prevY + prevH / 2 - 4,
                    VanillaUIHelper.TEXT_STATUS);
            return;
        }
        Entity entity = previewEntity();
        if (entity == null) {
            g.drawCenteredString(font, previewBroken ? Component.translatable("wh_npcs.ui.disguise.preview_fail").getString() : "...",
                    prevX + prevW / 2, prevY + prevH / 2 - 4, VanillaUIHelper.TEXT_WHITE);
            return;
        }

        g.drawString(font, font.plainSubstrByWidth(displayName(selected), prevW - 32),
                prevX + 6, prevY + 5, VanillaUIHelper.TEXT_GRAY, false);
        String dims = String.format("%.1f × %.1f", entity.getBbWidth(), entity.getBbHeight());
        g.drawString(font, dims, prevX + 6, prevY + prevH - 12, VanillaUIHelper.TEXT_DARK_GRAY, false);

        long now = System.currentTimeMillis();
        if (!paused && !rotDragging) {
            previewAngle = (previewAngle + (now - lastFrameMs) * 0.04F) % 360.0F;
        }
        lastFrameMs = now;

        if (minecraft != null && minecraft.level != null) {
            if (!paused) {
                frozenTick = (int) minecraft.level.getGameTime();
            }
            entity.tickCount = frozenTick;
        }
        float size = Math.max(entity.getBbWidth(), entity.getBbHeight());
        float scale = Math.min(56.0F, (prevH - 50.0F) / Math.max(0.5F, size));

        ScaledScreen.enableScissor(g, prevX + 2, prevY + 2, prevX + prevW - 2, prevY + prevH - 2);
        renderRotatingEntity(g, entity, prevX + prevW / 2,
                prevY + prevH / 2 + (int) (entity.getBbHeight() * scale / 2), scale, previewAngle);
        g.disableScissor();

        boolean pauseHover = isOver(lastMouseX, lastMouseY, pauseBtnX(), pauseBtnY(), 16, 16);
        VanillaUIHelper.drawButton(g, pauseBtnX(), pauseBtnY(), 16, 16, pauseHover);
        int ic = pauseHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_GRAY;
        if (paused) {

            g.fill(pauseBtnX() + 6, pauseBtnY() + 4, pauseBtnX() + 7, pauseBtnY() + 12, ic);
            g.fill(pauseBtnX() + 7, pauseBtnY() + 5, pauseBtnX() + 9, pauseBtnY() + 11, ic);
            g.fill(pauseBtnX() + 9, pauseBtnY() + 6, pauseBtnX() + 11, pauseBtnY() + 10, ic);
        } else {

            g.fill(pauseBtnX() + 5, pauseBtnY() + 4, pauseBtnX() + 7, pauseBtnY() + 12, ic);
            g.fill(pauseBtnX() + 9, pauseBtnY() + 4, pauseBtnX() + 11, pauseBtnY() + 12, ic);
        }
    }

    private int pauseBtnX() {
        return prevX + prevW - 20;
    }

    private int pauseBtnY() {
        return prevY + 4;
    }

    private int lastMouseX, lastMouseY;

    @Nullable
    private Entity previewEntity() {
        if (selected == null || minecraft == null || minecraft.level == null) {
            return null;
        }
        if (selected.equals(previewFor)) {
            return previewEntity;
        }
        previewFor = selected;
        previewBroken = false;
        previewEntity = null;
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(selected);
        if (type != null) {
            try {
                previewEntity = type.create(minecraft.level);
            } catch (Exception e) {
                previewBroken = true;
            }
        }
        if (previewEntity == null) {
            previewBroken = true;
        }
        return previewEntity;
    }

    private void renderRotatingEntity(GuiGraphics g, Entity entity, int x, int y, float scale, float angleDeg) {
        if (entity instanceof LivingEntity living) {
            living.yBodyRot = angleDeg;
            living.yBodyRotO = angleDeg;
            living.yHeadRot = angleDeg;
            living.yHeadRotO = angleDeg;
        }
        entity.setYRot(angleDeg);
        entity.yRotO = angleDeg;
        entity.setXRot(0);
        entity.xRotO = 0;

        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(x, y, 100);
        pose.scale(scale, scale, -scale);
        pose.mulPose(new Quaternionf().rotateZ((float) Math.PI));
        pose.mulPose(new Quaternionf().rotateX((float) Math.toRadians(-15)));

        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        dispatcher.setRenderShadow(false);
        MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();
        try {
            dispatcher.render(entity, 0, 0, 0, 0, 1.0F, pose, buffer, 0xF000F0);
        } catch (Exception e) {
            previewBroken = true;
            previewEntity = null;
        }
        buffer.endBatch();
        dispatcher.setRenderShadow(true);
        pose.popPose();
    }

    private void drawSmall(GuiGraphics g, String label, int x, int y, int w,
                           int mouseX, int mouseY, int color) {
        boolean hovered = isOver(mouseX, mouseY, x, y, w, 18);
        VanillaUIHelper.drawButton(g, x, y, w, 18, hovered);
        g.drawCenteredString(font, font.plainSubstrByWidth(label, w - 6), x + w / 2, y + 5,
                hovered ? VanillaUIHelper.TEXT_YELLOW : color);
    }

    @Override
    protected boolean mouseClickedScaled(double mouseX, double mouseY, int button) {
        if (button == 0) {
            recalc();
            if (scrollbars.click(mouseX, mouseY)) {
                return true;
            }

            if (selected != null && isOver(mouseX, mouseY, pauseBtnX(), pauseBtnY(), 16, 16)) {
                paused = !paused;
                return true;
            }

            if (selected != null && isOver(mouseX, mouseY, prevX, prevY, prevW, prevH)) {
                rotDragging = true;
                return true;
            }
            int visibleRows = (listH - 8) / ROW_H;
            int y = listY + 4;
            for (int i = scroll; i < Math.min(filtered.size(), scroll + visibleRows); i++) {
                if (isOver(mouseX, mouseY, listX + 2, y, LIST_W - 4, ROW_H)) {
                    selected = filtered.get(i);
                    return true;
                }
                y += ROW_H;
            }
            if (isOver(mouseX, mouseY, winX + winW - PAD - 226, bottomY, 70, 18) && selected != null) {
                onPicked.accept(selected.toString());
                onClose();
                return true;
            }
            if (isOver(mouseX, mouseY, winX + winW - PAD - 150, bottomY, 100, 18)) {
                onPicked.accept(null);
                onClose();
                return true;
            }
            if (isOver(mouseX, mouseY, winX + winW - PAD - 44, bottomY, 44, 18)) {
                onClose();
                return true;
            }
        }
        return superMouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected boolean mouseDraggedScaled(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (scrollbars.drag(mouseY)) {
            return true;
        }
        if (rotDragging && button == 0) {
            previewAngle = (previewAngle + (float) dragX * 1.5F) % 360.0F;
            return true;
        }
        return superMouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    protected boolean mouseReleasedScaled(double mouseX, double mouseY, int button) {
        scrollbars.release();
        if (rotDragging && button == 0) {
            rotDragging = false;
            return true;
        }
        return superMouseReleased(mouseX, mouseY, button);
    }

    @Override
    protected boolean mouseScrolledScaled(double mouseX, double mouseY, double delta) {
        recalc();
        if (isOver(mouseX, mouseY, listX, listY, LIST_W, listH)) {
            scroll -= (int) Math.signum(delta) * 3;
            return true;
        }
        return superMouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    private static boolean isOver(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
