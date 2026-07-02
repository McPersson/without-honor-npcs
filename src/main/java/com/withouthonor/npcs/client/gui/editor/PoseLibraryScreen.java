package com.withouthonor.npcs.client.gui.editor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.withouthonor.npcs.client.ClientPrefs;
import com.withouthonor.npcs.client.cache.ClientSkinCache;
import com.withouthonor.npcs.client.gui.ScaledScreen;
import com.withouthonor.npcs.client.gui.ScrollDrag;
import com.withouthonor.npcs.client.gui.SelectableEditBox;
import com.withouthonor.npcs.client.gui.VanillaUIHelper;
import com.withouthonor.npcs.common.entity.CompanionEntity;
import com.withouthonor.npcs.common.profile.PoseJson;
import com.withouthonor.npcs.network.NetworkHandler;
import com.withouthonor.npcs.network.PoseLibraryPackets;
import com.withouthonor.npcs.network.PoseLibraryPackets.PoseEntry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class PoseLibraryScreen extends ScaledScreen {

    private static final int PAD = 8;
    private static final int HEADER_H = 22;
    private static final int ROW_H = 16;
    private static final int PV_W = 132;

    private final Screen parent;
    private final JsonObject profileJson;
    @Nullable
    private final CompanionEntity npc;

    private List<PoseEntry> poses = new ArrayList<>();
    private final List<PoseEntry> builtins = builtinPoses();

    private final ScrollDrag scrollbars = new ScrollDrag();

    private EditBox searchBox;
    private EditBox saveNameBox;
    private boolean favTab;
    private int sortMode;
    private int scroll;
    @Nullable
    private String confirmDelete;
    @Nullable
    private String renamingPose;
    @Nullable
    private PoseEntry selected;
    private long lastClickMs;
    @Nullable
    private String lastClickName;

    @Nullable
    private CompanionEntity previewNpc;
    private float spin = 180F;
    private long lastMs;
    private int frozenTick;
    private boolean rotDrag, paused = true;

    private int winX, winY, winW, winH;
    private int controlsY, listY, listH, listW;
    private int saveRowY, bottomY, pvX, pvY, pvH;

    @Nullable
    private String hoverTooltip;

    @Nullable
    private static PoseLibraryScreen INSTANCE;

    private static final String[] BEHAVIORS = {"stand", "wander", "sit", "sleep"};
    private static final String[] BEHAVIOR_LABEL_KEYS = {
            "wh_npcs.ui.pose_lib.beh_stand", "wh_npcs.ui.pose_lib.beh_wander",
            "wh_npcs.ui.pose_lib.beh_sit", "wh_npcs.ui.pose_lib.beh_sleep"};
    @Nullable
    private java.util.function.Consumer<PoseEntry> onPickPose;
    @Nullable
    private java.util.function.Consumer<String> onPickBehavior;

    public PoseLibraryScreen(Screen parent, JsonObject profileJson, @Nullable CompanionEntity npc) {
        super(Component.translatable("wh_npcs.ui.pose_lib.title"));
        this.parent = parent;
        this.profileJson = profileJson;
        this.npc = npc;
    }

    public static PoseLibraryScreen forPicker(Screen parent, JsonObject profileJson, @Nullable CompanionEntity npc,
                                              java.util.function.Consumer<PoseEntry> onPickPose,
                                              java.util.function.Consumer<String> onPickBehavior) {
        PoseLibraryScreen s = new PoseLibraryScreen(parent, profileJson, npc);
        s.onPickPose = onPickPose;
        s.onPickBehavior = onPickBehavior;
        return s;
    }

    private boolean pickMode() {
        return onPickPose != null;
    }

    private int behaviorBtnX(int i) {
        return winX + PAD + 44 + i * 60;
    }

    public static void acceptList(List<PoseEntry> updated) {
        if (INSTANCE != null) {
            INSTANCE.poses = updated;
            INSTANCE.confirmDelete = null;
        }
    }

    @Override
    protected void init() {
        INSTANCE = this;
        recalc();
        clearWidgets();
        String old = searchBox != null ? searchBox.getValue() : "";
        searchBox = addRenderableWidget(new SelectableEditBox(font, winX + PAD, controlsY, 140, 16,
                Component.translatable("wh_npcs.ui.pose_lib.search")));
        searchBox.setMaxLength(48);
        searchBox.setValue(old);
        searchBox.setHint(Component.translatable("wh_npcs.ui.pose_lib.search_hint"));
        searchBox.setResponder(v -> scroll = 0);

        if (!pickMode()) {
            String oldName = saveNameBox != null ? saveNameBox.getValue() : "";
            saveNameBox = addRenderableWidget(new SelectableEditBox(font, winX + PAD, saveRowY, 150, 16,
                    Component.translatable("wh_npcs.ui.pose_lib.save_name")));
            saveNameBox.setMaxLength(48);
            saveNameBox.setValue(oldName);
            saveNameBox.setHint(Component.translatable("wh_npcs.ui.pose_lib.save_name_hint"));
        }

        NetworkHandler.sendToServer(new PoseLibraryPackets.RequestList());
    }

    @Override
    protected int designW() {
        return 480;
    }

    @Override
    protected int designH() {
        return 308;
    }

    private void recalc() {
        winW = 480;
        winH = 308;
        winX = (width - winW) / 2;
        winY = (height - winH) / 2;
        bottomY = winY + winH - PAD - 20;
        saveRowY = bottomY - 24;
        controlsY = winY + HEADER_H + 4;
        listY = controlsY + 22 + 12;
        listH = saveRowY - 6 - listY;
        pvX = winX + winW - PAD - PV_W;
        pvY = listY;
        pvH = listH;
        listW = pvX - 8 - (winX + PAD);
    }

    private int tabAllX() {
        return winX + PAD + 146;
    }

    private int tabFavX() {
        return tabAllX() + 32;
    }

    private int sortBtnX() {
        return tabFavX() + 32;
    }

    private int folderBtnX() {
        return winX + PAD + listW - 64;
    }

    private List<PoseEntry> displayed() {
        String q = searchBox != null ? searchBox.getValue().toLowerCase(Locale.ROOT).trim() : "";
        ClientPrefs prefs = ClientPrefs.get();
        List<PoseEntry> head = new ArrayList<>();
        if (!favTab) {
            for (PoseEntry b : builtins) {
                if (q.isEmpty() || b.name().toLowerCase(Locale.ROOT).contains(q)) {
                    head.add(b);
                }
            }
        }
        List<PoseEntry> pinned = new ArrayList<>();
        List<PoseEntry> rest = new ArrayList<>();
        for (PoseEntry f : poses) {
            if (!q.isEmpty() && !f.name().toLowerCase(Locale.ROOT).contains(q)
                    && !f.author().toLowerCase(Locale.ROOT).contains(q)) {
                continue;
            }
            if (favTab && !prefs.isFavoritePose(f.name())) {
                continue;
            }
            (prefs.isPinnedPose(f.name()) ? pinned : rest).add(f);
        }
        Comparator<PoseEntry> cmp = switch (sortMode) {
            case 1 -> Comparator.comparing(PoseEntry::name, String.CASE_INSENSITIVE_ORDER).reversed();
            case 2 -> Comparator.comparingLong(PoseEntry::mtime).reversed();
            default -> Comparator.comparing(PoseEntry::name, String.CASE_INSENSITIVE_ORDER);
        };
        pinned.sort(cmp);
        rest.sort(cmp);
        head.addAll(pinned);
        head.addAll(rest);
        return head;
    }

    private boolean isBuiltin(PoseEntry e) {
        for (PoseEntry b : builtins) {
            if (b.name().equals(e.name())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        recalc();
        hoverTooltip = null;
        scrollbars.beginFrame();
        VanillaUIHelper.drawWindow(g, winX, winY, winW, winH);
        g.fill(winX + 2, winY + 2, winX + winW - 2, winY + HEADER_H - 2, VanillaUIHelper.BG_HEADER);
        g.drawString(font, (pickMode() ? Component.translatable("wh_npcs.ui.pose_lib.title_pick")
                        : Component.translatable("wh_npcs.ui.pose_lib.title")).getString(),
                winX + PAD, winY + 7, VanillaUIHelper.TEXT_YELLOW, false);

        drawMini(g, Component.translatable("wh_npcs.ui.pose_lib.tab_all").getString(), tabAllX(), controlsY, 30, mouseX, mouseY,
                !favTab ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_WHITE);
        boolean favHover = isOver(mouseX, mouseY, tabFavX(), controlsY, 28, 16);
        VanillaUIHelper.drawButton(g, tabFavX(), controlsY, 28, 16, favHover || favTab);
        drawHeart(g, tabFavX() + 10, controlsY + 5, favTab ? 0xFFFF5555 : VanillaUIHelper.TEXT_GRAY);
        String sortLabel = switch (sortMode) {
            case 1 -> Component.translatable("wh_npcs.ui.pose_lib.sort_za").getString();
            case 2 -> Component.translatable("wh_npcs.ui.pose_lib.sort_new").getString();
            default -> Component.translatable("wh_npcs.ui.pose_lib.sort_az").getString();
        };
        drawMini(g, sortLabel, sortBtnX(), controlsY, 36, mouseX, mouseY, VanillaUIHelper.TEXT_AQUA);
        if (isLocalWorld()) {
            drawMini(g, Component.translatable("wh_npcs.ui.pose_lib.folder").getString(), folderBtnX(), controlsY, 64, mouseX, mouseY, VanillaUIHelper.TEXT_GRAY);
        }

        g.drawString(font, Component.translatable("wh_npcs.ui.pose_lib.world_folder").getString(), winX + PAD, controlsY + 20,
                VanillaUIHelper.TEXT_DARK_GRAY, false);

        renderList(g, mouseX, mouseY);
        renderPreview(g, mouseX, mouseY);

        if (pickMode()) {

            g.drawString(font, Component.translatable("wh_npcs.ui.pose_lib.behavior_label").getString(), winX + PAD, saveRowY + 4, VanillaUIHelper.TEXT_GRAY, false);
            for (int i = 0; i < 4; i++) {
                drawBtn(g, Component.translatable(BEHAVIOR_LABEL_KEYS[i]).getString(), behaviorBtnX(i), saveRowY - 1, 56, mouseX, mouseY,
                        VanillaUIHelper.TEXT_AQUA);
            }
        } else if (renamingPose != null) {
            drawBtn(g, Component.translatable("wh_npcs.ui.pose_lib.rename").getString(), winX + PAD + 156, saveRowY - 1, listW - 156, mouseX, mouseY,
                    VanillaUIHelper.TEXT_GOLD);
        } else {
            drawBtn(g, Component.translatable("wh_npcs.ui.pose_lib.save_current").getString(), winX + PAD + 156, saveRowY - 1, listW - 156, mouseX, mouseY,
                    VanillaUIHelper.TEXT_GREEN);
        }
        drawBtn(g, Component.translatable("wh_npcs.ui.common.done").getString(), winX + winW - PAD - 60, bottomY, 60, mouseX, mouseY, VanillaUIHelper.TEXT_WHITE);

        if (confirmDelete == null && isLocalWorld()
                && isOver(mouseX, mouseY, folderBtnX(), controlsY, 64, 16)) {
            hoverTooltip = Component.translatable("wh_npcs.ui.pose_lib.folder_tip").getString();
        }
    }

    @Override
    protected void renderOverlay(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (confirmDelete != null) {
            renderConfirm(g, mouseX, mouseY, Component.translatable("wh_npcs.ui.pose_lib.confirm_delete").getString(), confirmDelete,
                    Component.translatable("wh_npcs.ui.common.delete").getString(), VanillaUIHelper.TEXT_RED);
            return;
        }
        if (hoverTooltip != null) {
            multilineTooltip(g, hoverTooltip, mouseX, mouseY);
        }
    }

    private void renderList(GuiGraphics g, int mouseX, int mouseY) {
        int listX = winX + PAD;
        VanillaUIHelper.drawContentPanel(g, listX, listY, listW, listH);
        List<PoseEntry> shown = displayed();
        if (shown.isEmpty()) {
            g.drawCenteredString(font, (favTab ? Component.translatable("wh_npcs.ui.pose_lib.empty_fav")
                            : Component.translatable("wh_npcs.ui.pose_lib.empty")).getString(),
                    listX + listW / 2, listY + listH / 2 - 4, VanillaUIHelper.TEXT_WHITE);
            return;
        }
        ClientPrefs prefs = ClientPrefs.get();
        boolean canDel = canDelete();
        int visible = (listH - 8) / ROW_H;
        scroll = Math.max(0, Math.min(scroll, Math.max(0, shown.size() - visible)));
        int y = listY + 4;
        for (int i = scroll; i < Math.min(shown.size(), scroll + visible); i++) {
            PoseEntry f = shown.get(i);
            boolean builtin = isBuiltin(f);
            boolean pinned = !builtin && prefs.isPinnedPose(f.name());
            boolean fav = !builtin && prefs.isFavoritePose(f.name());
            boolean isSel = selected != null && selected.name().equals(f.name());
            boolean hovered = isOver(mouseX, mouseY, listX + 2, y, listW - 4, ROW_H);
            if (isSel) {
                g.fill(listX + 2, y, listX + listW - 2, y + ROW_H, VanillaUIHelper.BG_SELECTED);
            } else if (hovered) {
                g.fill(listX + 2, y, listX + listW - 2, y + ROW_H, VanillaUIHelper.BG_HOVERED);
            }
            if (builtin) {
                drawPin(g, listX + 5, y + 4, VanillaUIHelper.TEXT_GOLD);
            } else {
                drawAuthorHead(g, f.author(), listX + 5, y + 4);
            }
            int nameX = listX + 18;
            if (pinned) {
                drawPin(g, nameX, y + 4, VanillaUIHelper.TEXT_GOLD);
                nameX += 10;
            }
            boolean showDelete = hovered && canDel && !builtin;
            boolean showRename = showDelete && !pickMode();
            int rightLimit = heartX(listX) - 8
                    - (showDelete ? (showRename ? 42 : 14) : (builtin ? font.width(Component.translatable("wh_npcs.ui.pose_lib.builtin").getString())
                    : font.width(Component.translatable("wh_npcs.ui.pose_lib.size_kb", f.sizeKb()).getString())));
            int nameColor = builtin ? VanillaUIHelper.TEXT_GOLD
                    : ((hovered || isSel) ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_WHITE);
            g.drawString(font, font.plainSubstrByWidth(f.name(), rightLimit - nameX), nameX, y + 4, nameColor, false);
            if (builtin) {
                String tag = Component.translatable("wh_npcs.ui.pose_lib.builtin").getString();
                g.drawString(font, "§8" + tag, heartX(listX) + 8 - font.width(tag), y + 4,
                        VanillaUIHelper.TEXT_DARK_GRAY, false);
            } else if (showDelete) {
                if (showRename) {
                    boolean renHover = isOver(mouseX, mouseY, renX(listX), y + 3, 10, 10);
                    boolean renaming = f.name().equals(renamingPose);
                    VanillaUIHelper.drawRenameIcon(g, font, renX(listX), y + 4,
                            (renHover || renaming) ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_GRAY);
                    if (renHover && confirmDelete == null) {
                        hoverTooltip = Component.translatable("wh_npcs.ui.pose_lib.rename_tip").getString();
                    }
                }
                boolean delHover = isOver(mouseX, mouseY, delX(listX), y + 3, 10, 10);
                g.drawString(font, "✕", delX(listX), y + 4,
                        delHover ? 0xFFFF5555 : VanillaUIHelper.TEXT_GRAY, false);
            } else {
                String size = Component.translatable("wh_npcs.ui.pose_lib.size_kb", f.sizeKb()).getString();
                g.drawString(font, size, pinX(listX) - 6 - font.width(size), y + 4,
                        VanillaUIHelper.TEXT_DARK_GRAY, false);
            }
            if (!builtin && (hovered || pinned)) {
                drawPin(g, pinX(listX), y + 4, pinned ? VanillaUIHelper.TEXT_GOLD
                        : (isOver(mouseX, mouseY, pinX(listX), y + 3, 10, 10)
                        ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_DARK_GRAY));
            }
            if (!builtin && (hovered || fav)) {
                drawHeart(g, heartX(listX), y + 5, fav ? 0xFFFF5555
                        : (isOver(mouseX, mouseY, heartX(listX), y + 3, 10, 10)
                        ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_DARK_GRAY));
            }
            if (!builtin && !f.author().isEmpty() && confirmDelete == null
                    && isOver(mouseX, mouseY, listX + 5, y + 4, 8, 8)) {
                hoverTooltip = Component.translatable("wh_npcs.ui.pose_lib.author_tip", f.author()).getString();
            }
            y += ROW_H;
        }
        VanillaUIHelper.drawScrollbar(g, listX + listW - 6, listY + 3, listH - 6,
                shown.size(), visible, scroll, scrollbars, v -> scroll = v);
    }

    private void renderPreview(GuiGraphics g, int mouseX, int mouseY) {
        VanillaUIHelper.drawContentPanel(g, pvX, pvY, PV_W, pvH);
        CompanionEntity p = preview();
        if (p == null || minecraft == null) {
            g.drawCenteredString(font, Component.translatable("wh_npcs.ui.pose_lib.no_preview").getString(), pvX + PV_W / 2, pvY + pvH / 2 - 4, VanillaUIHelper.TEXT_WHITE);
        } else if (selected == null) {
            g.drawCenteredString(font, Component.translatable("wh_npcs.ui.pose_lib.pick_pose").getString(), pvX + PV_W / 2, pvY + pvH / 2 - 4, VanillaUIHelper.TEXT_WHITE);
        } else {
            p.setSkinName(str("skin_player_name"));
            applySelectedToPreview(p);
            long now = System.currentTimeMillis();
            if (!rotDrag && !paused) {
                spin = (spin + (now - lastMs) * 0.04F) % 360.0F;
            }
            lastMs = now;
            if (minecraft.level != null) {
                if (!paused) {
                    frozenTick = (int) minecraft.level.getGameTime();
                }
                p.tickCount = frozenTick;
            }
            float scale = (pvH - 70.0F) / 2.2F;
            ScaledScreen.enableScissor(g, pvX + 2, pvY + 2, pvX + PV_W - 2, pvY + pvH - 22);
            renderRotating(g, p, pvX + PV_W / 2, pvY + (pvH - 22) / 2 + (int) (scale * 0.9F), scale, spin);
            g.disableScissor();
            int pbx = pvX + PV_W - 20, pby = pvY + 4;
            boolean ph = isOver(mouseX, mouseY, pbx, pby, 16, 16);
            VanillaUIHelper.drawButton(g, pbx, pby, 16, 16, ph);
            int ic = ph ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_GRAY;
            if (paused) {
                g.fill(pbx + 6, pby + 4, pbx + 7, pby + 12, ic);
                g.fill(pbx + 7, pby + 5, pbx + 9, pby + 11, ic);
                g.fill(pbx + 9, pby + 6, pbx + 11, pby + 10, ic);
            } else {
                g.fill(pbx + 5, pby + 4, pbx + 7, pby + 12, ic);
                g.fill(pbx + 9, pby + 4, pbx + 11, pby + 12, ic);
            }
            g.drawCenteredString(font, font.plainSubstrByWidth(selected.name(), PV_W - 8),
                    pvX + PV_W / 2, pvY + pvH - 16, VanillaUIHelper.TEXT_WHITE);
        }

        boolean can = selected != null;
        boolean applyHover = can && isOver(mouseX, mouseY, pvX, saveRowY - 1, PV_W, 18);
        VanillaUIHelper.drawButton(g, pvX, saveRowY - 1, PV_W, 18, applyHover);
        g.drawCenteredString(font, Component.translatable("wh_npcs.ui.pose_lib.apply").getString(), pvX + PV_W / 2, saveRowY + 4,
                applyHover ? VanillaUIHelper.TEXT_YELLOW : (can ? VanillaUIHelper.TEXT_GREEN : VanillaUIHelper.TEXT_DARK_GRAY));
    }

    @Nullable
    private CompanionEntity preview() {
        if (previewNpc == null && minecraft != null && minecraft.level != null) {
            previewNpc = com.withouthonor.npcs.common.registry.ModEntities.COMPANION.get()
                    .create(minecraft.level);
        }
        return previewNpc;
    }

    private void applySelectedToPreview(CompanionEntity p) {
        if (selected == null) {
            return;
        }
        PoseJson.Pose ps = new PoseJson.Pose();
        parsePose(selected.pose(), ps);
        p.setPoseClient(ps);
        JsonObject t = parseObj(selected.transform());
        p.setRenderTransformClient(
                tf(t, "rot_x", 0F), tf(t, "rot_y", 0F), tf(t, "rot_z", 0F),
                tf(t, "pos_x", 0F), tf(t, "pos_y", 0F), tf(t, "pos_z", 0F),
                tf(t, "scale_x", 1F), tf(t, "scale_y", 1F), tf(t, "scale_z", 1F));
    }

    private void renderRotating(GuiGraphics g, CompanionEntity e, int x, int y, float scale, float angle) {
        e.yBodyRot = angle;
        e.yBodyRotO = angle;
        e.yHeadRot = angle;
        e.yHeadRotO = angle;
        e.setYRot(angle);
        e.yRotO = angle;
        var pose = g.pose();
        pose.pushPose();
        pose.translate(x, y, 100);
        pose.scale(scale, scale, -scale);
        pose.mulPose(new org.joml.Quaternionf().rotateZ((float) Math.PI));
        pose.mulPose(new org.joml.Quaternionf().rotateX((float) Math.toRadians(-12)));
        var dispatcher = minecraft.getEntityRenderDispatcher();
        dispatcher.setRenderShadow(false);
        var buffer = minecraft.renderBuffers().bufferSource();
        try {
            dispatcher.render(e, 0, 0, 0, 0, 1.0F, pose, buffer,
                    net.minecraft.client.renderer.LightTexture.FULL_BRIGHT);
        } catch (Exception ignored) {

        }
        buffer.endBatch();
        dispatcher.setRenderShadow(true);
        pose.popPose();
    }

    private int heartX(int listX) {
        return listX + listW - 16;
    }

    private int pinX(int listX) {
        return listX + listW - 30;
    }

    private int delX(int listX) {
        return listX + listW - 44;
    }

    private int renX(int listX) {
        return listX + listW - 58;
    }

    private void renderConfirm(GuiGraphics g, int mouseX, int mouseY, String title, String value,
                               String okLabel, int okColor) {
        int w = 260;
        int h = 96;
        int x = (width - w) / 2;
        int cy = (height - h) / 2;
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        g.fill(0, 0, width, height, 0xA0000000);
        VanillaUIHelper.drawWindow(g, x, cy, w, h);
        g.drawCenteredString(font, title, x + w / 2, cy + 14, VanillaUIHelper.TEXT_WHITE);
        g.drawCenteredString(font, "§b" + font.plainSubstrByWidth(value, w - 30), x + w / 2, cy + 30, VanillaUIHelper.TEXT_WHITE);
        g.drawCenteredString(font, Component.translatable("wh_npcs.ui.pose_lib.irreversible").getString(), x + w / 2, cy + 48, VanillaUIHelper.TEXT_WHITE);
        drawBtn(g, okLabel, x + w / 2 - 110, cy + h - 28, 104, mouseX, mouseY, okColor);
        drawBtn(g, Component.translatable("wh_npcs.ui.common.cancel").getString(), x + w / 2 + 6, cy + h - 28, 104, mouseX, mouseY, VanillaUIHelper.TEXT_WHITE);
        g.pose().popPose();
    }

    @Override
    protected boolean mouseClickedScaled(double mouseX, double mouseY, int button) {
        recalc();
        if (button != 0) {
            return superMouseClicked(mouseX, mouseY, button);
        }
        if (confirmDelete != null) {
            int w = 260;
            int h = 96;
            int x = (width - w) / 2;
            int cy = (height - h) / 2;
            if (isOver(mouseX, mouseY, x + w / 2 - 110, cy + h - 28, 104, 18)) {
                NetworkHandler.sendToServer(new PoseLibraryPackets.Delete(confirmDelete));
                confirmDelete = null;
                return true;
            }
            if (isOver(mouseX, mouseY, x + w / 2 + 6, cy + h - 28, 104, 18)) {
                confirmDelete = null;
                return true;
            }
            return true;
        }
        if (scrollbars.click(mouseX, mouseY)) {
            return true;
        }

        if (selected != null && isOver(mouseX, mouseY, pvX + PV_W - 20, pvY + 4, 16, 16)) {
            paused = !paused;
            return true;
        }
        if (selected != null && isOver(mouseX, mouseY, pvX, pvY, PV_W, pvH - 22)) {
            rotDrag = true;
            return true;
        }
        if (selected != null && isOver(mouseX, mouseY, pvX, saveRowY - 1, PV_W, 18)) {
            commit(selected);
            return true;
        }
        if (isOver(mouseX, mouseY, tabAllX(), controlsY, 30, 16)) {
            favTab = false;
            scroll = 0;
            return true;
        }
        if (isOver(mouseX, mouseY, tabFavX(), controlsY, 28, 16)) {
            favTab = true;
            scroll = 0;
            return true;
        }
        if (isOver(mouseX, mouseY, sortBtnX(), controlsY, 36, 16)) {
            sortMode = (sortMode + 1) % 3;
            return true;
        }
        if (isLocalWorld() && isOver(mouseX, mouseY, folderBtnX(), controlsY, 64, 16)) {
            openPosesFolder();
            return true;
        }
        int listX = winX + PAD;
        List<PoseEntry> shown = displayed();
        boolean canDel = canDelete();
        int visible = (listH - 8) / ROW_H;
        int y = listY + 4;
        for (int i = scroll; i < Math.min(shown.size(), scroll + visible); i++) {
            PoseEntry f = shown.get(i);
            boolean builtin = isBuiltin(f);
            boolean hovered = isOver(mouseX, mouseY, listX + 2, y, listW - 4, ROW_H);
            if (hovered) {
                if (!builtin && canDel && !pickMode() && isOver(mouseX, mouseY, renX(listX), y + 3, 10, 10)) {
                    if (f.name().equals(renamingPose)) {
                        cancelRename();
                    } else {
                        renamingPose = f.name();
                        if (saveNameBox != null) {
                            saveNameBox.setValue(f.name());
                            saveNameBox.setTextColor(0xE0E0E0);
                            setFocused(saveNameBox);
                            saveNameBox.setFocused(true);
                        }
                    }
                    return true;
                }
                if (!builtin && isOver(mouseX, mouseY, heartX(listX), y + 3, 10, 10)) {
                    ClientPrefs.get().toggleFavoritePose(f.name());
                    return true;
                }
                if (!builtin && isOver(mouseX, mouseY, pinX(listX), y + 3, 10, 10)) {
                    ClientPrefs.get().togglePinnedPose(f.name());
                    return true;
                }
                if (!builtin && canDel && isOver(mouseX, mouseY, delX(listX), y + 3, 10, 10)) {
                    confirmDelete = f.name();
                    return true;
                }

                long now = System.currentTimeMillis();
                if (selected != null && f.name().equals(lastClickName) && now - lastClickMs < 250) {
                    commit(f);
                    return true;
                }
                selected = f;
                lastClickName = f.name();
                lastClickMs = now;
                return true;
            }
            y += ROW_H;
        }
        if (pickMode()) {
            for (int i = 0; i < 4; i++) {
                if (isOver(mouseX, mouseY, behaviorBtnX(i), saveRowY - 1, 56, 18)) {
                    if (onPickBehavior != null) {
                        onPickBehavior.accept(BEHAVIORS[i]);
                    }
                    onClose();
                    return true;
                }
            }
        } else if (isOver(mouseX, mouseY, winX + PAD + 156, saveRowY - 1, listW - 156, 18)) {
            if (renamingPose != null) {
                performRename();
            } else {
                saveCurrent();
            }
            return true;
        }
        if (isOver(mouseX, mouseY, winX + winW - PAD - 60, bottomY, 60, 18)) {
            onClose();
            return true;
        }
        return superMouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected boolean mouseDraggedScaled(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (scrollbars.drag(mouseY)) {
            return true;
        }
        if (rotDrag && button == 0) {
            spin = (spin + (float) dragX * 1.5F) % 360.0F;
            return true;
        }
        return superMouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    protected boolean mouseReleasedScaled(double mouseX, double mouseY, int button) {
        scrollbars.release();
        rotDrag = false;
        return superMouseReleased(mouseX, mouseY, button);
    }

    @Override
    protected boolean mouseScrolledScaled(double mouseX, double mouseY, double delta) {
        recalc();
        if (isOver(mouseX, mouseY, winX + PAD, listY, listW, listH)) {
            scroll -= (int) Math.signum(delta) * 2;
            return true;
        }
        return superMouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int key, int scancode, int mods) {
        if (key == 256 && confirmDelete != null) {
            confirmDelete = null;
            return true;
        }
        if (key == 256 && renamingPose != null) {
            cancelRename();
            return true;
        }
        return super.keyPressed(key, scancode, mods);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public void removed() {
        if (INSTANCE == this) {
            INSTANCE = null;
        }
        super.removed();
    }

    private void saveCurrent() {
        String name = saveNameBox != null ? saveNameBox.getValue().trim() : "";
        if (name.isEmpty()) {
            return;
        }
        PoseJson.Pose ps = new PoseJson.Pose();
        PoseJson.read(profileJson, ps);
        String poseJson = PoseJson.toPoseObject(ps).toString();
        String transformJson = currentTransformObject().toString();
        NetworkHandler.sendToServer(new PoseLibraryPackets.Save(name, poseJson, transformJson));
    }

    private void cancelRename() {
        renamingPose = null;
        if (saveNameBox != null) {
            saveNameBox.setValue("");
            saveNameBox.setTextColor(0xE0E0E0);
        }
    }

    private void performRename() {
        if (renamingPose == null || saveNameBox == null) {
            return;
        }
        String oldName = renamingPose;
        String newName = saveNameBox.getValue().trim();
        if (newName.isEmpty() || newName.equals(oldName)) {
            saveNameBox.setTextColor(0xFF5555);
            return;
        }
        for (PoseEntry f : poses) {
            if (!f.name().equals(oldName) && f.name().equalsIgnoreCase(newName)) {
                saveNameBox.setTextColor(0xFF5555);
                return;
            }
        }
        NetworkHandler.sendToServer(new PoseLibraryPackets.Rename(oldName, newName));
        ClientPrefs.get().renamePose(oldName, newName);
        for (int i = 0; i < poses.size(); i++) {
            PoseEntry f = poses.get(i);
            if (f.name().equals(oldName)) {
                poses.set(i, new PoseEntry(newName, f.author(), f.mtime(), f.sizeKb(), f.pose(), f.transform()));
            }
        }
        if (selected != null && selected.name().equals(oldName)) {
            selected = null;
        }
        cancelRename();
    }

    private void commit(PoseEntry e) {
        if (onPickPose != null) {

            onPickPose.accept(e);
            onClose();
            return;
        }
        PoseJson.Pose ps = new PoseJson.Pose();
        parsePose(e.pose(), ps);
        PoseJson.write(profileJson, ps);
        applyTransform(e.transform());
        if (npc != null) {
            npc.setPoseClient(ps);
            npc.setRenderTransformClient(
                    fp("rot_x", 0F), fp("rot_y", 0F), fp("rot_z", 0F),
                    fp("pos_x", 0F), fp("pos_y", 0F), fp("pos_z", 0F),
                    fp("scale_x", 1F), fp("scale_y", 1F), fp("scale_z", 1F));
        }
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    private static void parsePose(String poseJson, PoseJson.Pose out) {
        out.clear();
        try {
            JsonObject poseObj = JsonParser.parseString(poseJson).getAsJsonObject();
            JsonObject wrapper = new JsonObject();
            wrapper.add("pose", poseObj);
            PoseJson.read(wrapper, out);
        } catch (Exception ignored) {
            out.clear();
        }
    }

    private static JsonObject parseObj(String json) {
        try {
            if (json != null && !json.isBlank()) {
                var el = JsonParser.parseString(json);
                if (el.isJsonObject()) {
                    return el.getAsJsonObject();
                }
            }
        } catch (Exception ignored) {

        }
        return new JsonObject();
    }

    private static float tf(JsonObject t, String key, float def) {
        return t.has(key) && !t.get(key).isJsonNull() ? t.get(key).getAsFloat() : def;
    }

    private JsonObject currentTransformObject() {
        JsonObject t = new JsonObject();
        for (String k : new String[]{"rot_x", "rot_y", "rot_z", "pos_x", "pos_y", "pos_z"}) {
            float v = fp(k, 0F);
            if (v != 0F) {
                t.addProperty(k, v);
            }
        }
        for (String k : new String[]{"scale_x", "scale_y", "scale_z"}) {
            float v = fp(k, 1F);
            if (v != 1F) {
                t.addProperty(k, v);
            }
        }
        return t;
    }

    private void applyTransform(String transformJson) {
        JsonObject t = parseObj(transformJson);
        for (String k : new String[]{"rot_x", "rot_y", "rot_z", "scale_x", "scale_y", "scale_z"}) {
            if (t.has(k) && !t.get(k).isJsonNull()) {
                profileJson.addProperty(k, t.get(k).getAsFloat());
            } else {
                profileJson.remove(k);
            }
        }
    }

    private float fp(String key, float def) {
        return profileJson.has(key) && !profileJson.get(key).isJsonNull()
                ? profileJson.get(key).getAsFloat() : def;
    }

    private String str(String key) {
        return profileJson.has(key) && !profileJson.get(key).isJsonNull()
                ? profileJson.get(key).getAsString() : "";
    }

    public static List<PoseEntry> builtinList() {
        return builtinPoses();
    }

    private static List<PoseEntry> builtinPoses() {
        List<PoseEntry> out = new ArrayList<>();
        out.add(builtin(Component.translatable("wh_npcs.ui.pose_lib.builtin_stand").getString(), new PoseJson.Pose(), ""));

        PoseJson.Pose sit = new PoseJson.Pose();
        sit.freeze = true;
        sit.angles[12] = -80F;
        sit.angles[15] = -80F;
        sit.angles[6] = -15F;
        sit.angles[9] = -15F;
        out.add(builtin(Component.translatable("wh_npcs.ui.pose_lib.builtin_sit").getString(), sit, ""));

        PoseJson.Pose sleep = new PoseJson.Pose();
        sleep.freeze = true;
        sleep.angles[8] = 10F;
        sleep.angles[11] = -10F;
        sleep.bb[0] = 1.6F;
        sleep.bb[1] = 0.6F;

        out.add(builtin(Component.translatable("wh_npcs.ui.pose_lib.builtin_sleep").getString(), sleep, "{\"rot_x\":90.0}"));
        return out;
    }

    private static PoseEntry builtin(String name, PoseJson.Pose pose, String transformJson) {
        String poseJson = PoseJson.toPoseObject(pose).toString();
        return new PoseEntry(name, "", 0L, 0, poseJson, transformJson == null ? "" : transformJson);
    }

    private boolean isLocalWorld() {
        return minecraft != null && minecraft.hasSingleplayerServer();
    }

    private boolean canDelete() {
        return minecraft != null && minecraft.player != null
                && (minecraft.player.hasPermissions(3) || minecraft.hasSingleplayerServer());
    }

    private void openPosesFolder() {
        if (minecraft == null || minecraft.getSingleplayerServer() == null) {
            return;
        }
        java.nio.file.Path dir = minecraft.getSingleplayerServer()
                .getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                .resolve("wh_npcs").resolve("poses");
        try {
            java.nio.file.Files.createDirectories(dir);
        } catch (java.io.IOException ignored) {

        }
        net.minecraft.Util.getPlatform().openFile(dir.toFile());
    }

    private static void drawAuthorHead(GuiGraphics g, String author, int x, int y) {
        ClientSkinCache.Skin skin = author.isEmpty() ? null : ClientSkinCache.getInstance().get(author);
        if (skin != null) {
            g.blit(skin.location(), x, y, 8, 8, 8.0F, 8.0F, 8, 8, 64, 64);
            g.blit(skin.location(), x, y, 8, 8, 40.0F, 8.0F, 8, 8, 64, 64);
        } else {
            g.fill(x, y, x + 8, y + 8, 0xFF6E5037);
            g.fill(x + 2, y + 4, x + 3, y + 5, 0xFF2B1F14);
            g.fill(x + 5, y + 4, x + 6, y + 5, 0xFF2B1F14);
        }
    }

    private void drawMini(GuiGraphics g, String label, int x, int y, int w, int mouseX, int mouseY, int color) {
        boolean hovered = isOver(mouseX, mouseY, x, y, w, 16);
        VanillaUIHelper.drawButton(g, x, y, w, 16, hovered);
        g.drawCenteredString(font, label, x + w / 2, y + 4, hovered ? VanillaUIHelper.TEXT_YELLOW : color);
    }

    private void drawBtn(GuiGraphics g, String label, int x, int y, int w, int mouseX, int mouseY, int color) {
        VanillaUIHelper.drawSmallButton(g, font, label, x, y, w, isOver(mouseX, mouseY, x, y, w, 18), color);
    }

    private static void drawHeart(GuiGraphics g, int x, int y, int c) {
        g.fill(x + 1, y, x + 3, y + 1, c);
        g.fill(x + 4, y, x + 6, y + 1, c);
        g.fill(x, y + 1, x + 7, y + 3, c);
        g.fill(x + 1, y + 3, x + 6, y + 4, c);
        g.fill(x + 2, y + 4, x + 5, y + 5, c);
        g.fill(x + 3, y + 5, x + 4, y + 6, c);
    }

    private static void drawPin(GuiGraphics g, int x, int y, int c) {
        g.fill(x + 1, y, x + 7, y + 4, c);
        g.fill(x + 3, y + 4, x + 5, y + 8, c);
    }

    private void multilineTooltip(GuiGraphics g, String text, int x, int y) {
        List<Component> lines = new ArrayList<>();
        for (String line : text.split("\n")) {
            lines.add(Component.literal(line));
        }
        g.renderComponentTooltip(font, lines, x, y);
    }

    private static boolean isOver(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
