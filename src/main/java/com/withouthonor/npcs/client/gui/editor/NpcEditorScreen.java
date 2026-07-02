package com.withouthonor.npcs.client.gui.editor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.withouthonor.npcs.client.gui.ScaledScreen;
import com.withouthonor.npcs.client.gui.ScrollDrag;
import com.withouthonor.npcs.client.gui.VanillaUIHelper;
import com.withouthonor.npcs.common.dialogue.EntryPoint;
import com.withouthonor.npcs.network.NetworkHandler;
import com.withouthonor.npcs.network.SaveProfilePacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NpcEditorScreen extends ScaledScreen {

    private static final int PAD = 10;
    private static final int HEADER_H = 24;
    private static final int TAB_H = 20;
    private static final String[] TAB_KEYS = {"wh_npcs.ui.npc.tab_profile", "wh_npcs.ui.npc.tab_dialogues",
            "wh_npcs.ui.npc.tab_entries", "wh_npcs.ui.npc.tab_items", "wh_npcs.ui.npc.tab_factions",
            "wh_npcs.ui.npc.tab_combat", "wh_npcs.ui.npc.tab_behavior", "wh_npcs.ui.npc.tab_pose"};

    private static String tr(String key) {
        return Component.translatable(key).getString();
    }

    private static String tr(String key, Object... args) {
        return Component.translatable(key, args).getString();
    }

    private final JsonObject profileJson;
    private final List<com.withouthonor.npcs.network.EditorDataPacket.DialogueSummary> dialogues;
    private int activeTab = 0;

    private com.withouthonor.npcs.client.gui.RichTextEditor nameBox;
    private com.withouthonor.npcs.client.gui.RichTextEditor titleBox;

    private EditBox newDialogueBox;
    private String renamingDialogue;
    private EditBox dlgSearchBox;
    @Nullable
    private String selectedDialogue;
    private int dialogueScroll;

    private final ScrollDrag scrollbars = new ScrollDrag();
    private long lastDialogueClickTime;
    private boolean dlgFavTab;
    private boolean dlgMineTab;
    private boolean dlgSortDesc;
    private static final int DLG_ROW_H = 13;

    @Nullable
    private String confirmTitle;
    private String confirmValue = "";
    private List<String> confirmWarn = List.of();
    private String confirmOkKey = "wh_npcs.ui.common.delete";
    private boolean confirmIrreversible = true;
    @Nullable
    private Runnable confirmAction;

    private static final int FAC_LIST_W = 112;
    @Nullable
    private String selectedFaction;
    private boolean factionsRequested;
    private boolean facDirty;
    private String draftFacName = "";
    private String draftFacPenalty = "0";

    private final java.util.LinkedHashSet<String> draftHostile = new java.util.LinkedHashSet<>();
    private int facColor = 0xFF55FFFF;
    private final List<String> tierNames = new ArrayList<>();
    private final List<Integer> tierMins = new ArrayList<>();
    private final List<Integer> tierColors = new ArrayList<>();

    private final List<Float> tierPrices = new ArrayList<>();
    private EditBox facNameBox;
    private EditBox facPenaltyBox;
    private EditBox newFactionBox;
    private final List<EditBox> tierNameBoxes = new ArrayList<>();
    private final List<EditBox> tierMinBoxes = new ArrayList<>();

    private boolean palettePicker;
    private int paletteTarget = -1;

    private boolean bossColorPicker;

    private boolean factionPicker;
    private int factionPickerScroll;

    private boolean hostilePicker;
    private int hostilePickerScroll;

    static final String[] PRESET_IDS = {"passive", "melee", "shield", "bow", "potion"};
    static final String[] PRESET_NAME_KEYS = {"wh_npcs.ui.npc.preset_passive", "wh_npcs.ui.npc.preset_melee",
            "wh_npcs.ui.npc.preset_shield", "wh_npcs.ui.npc.preset_bow", "wh_npcs.ui.npc.preset_potion"};

    static final String[] MOBTYPE_IDS = {"undefined", "undead", "arthropod", "water", "illager"};
    static final String[] MOBTYPE_NAME_KEYS = {"wh_npcs.ui.npc.mobtype_normal", "wh_npcs.ui.npc.mobtype_undead",
            "wh_npcs.ui.npc.mobtype_arthropod", "wh_npcs.ui.npc.mobtype_aquatic", "wh_npcs.ui.npc.mobtype_illager"};

    static final String[] PRESET_DESC_KEYS = {"wh_npcs.ui.npc.preset_desc_passive",
            "wh_npcs.ui.npc.preset_desc_melee", "wh_npcs.ui.npc.preset_desc_shield",
            "wh_npcs.ui.npc.preset_desc_bow", "wh_npcs.ui.npc.preset_desc_potion"};
    private final net.minecraft.resources.ResourceLocation[] dropItem =
            new net.minecraft.resources.ResourceLocation[9];
    private final net.minecraft.nbt.CompoundTag[] dropNbt = new net.minecraft.nbt.CompoundTag[9];
    private final int[] dropCount = new int[9];
    private final int[] dropChance = new int[9];

    private int chanceEditSlot = -1;
    @Nullable
    private EditBox chanceEditBox;
    private EditBox hpBox;
    private EditBox dmgBox;
    private EditBox armorBox;
    private EditBox kbBox;
    private EditBox speedBox;
    private EditBox xpMinBox;
    private EditBox xpMaxBox;
    private EditBox regenBox;
    private EditBox totemChargesBox;

    private final EditBox[] rotBox = new EditBox[3];
    private final EditBox[] posBox = new EditBox[3];
    private final EditBox[] scaleBox = new EditBox[3];
    @Nullable
    private EditBox scaleUniBox;

    @Nullable
    private com.withouthonor.npcs.common.entity.CompanionEntity posePreviewNpc;
    private float poseAngle = 180.0F;
    private long poseLastMs;
    private int poseFrozenTick;
    private boolean poseRotDrag;
    private boolean posePaused = true;

    @Nullable
    private EditBox scrubBox;
    private double scrubStartX;
    private float scrubStartVal, scrubSens, scrubMin, scrubMax;
    private boolean scrubbing;

    private static final int REP_W = 310;
    private static final int REP_H = 214;
    private static final int REP_VISIBLE = 9;
    private boolean repPopup;
    private boolean repLoading;
    private int repScroll;
    private String repSearch = "";
    private List<com.withouthonor.npcs.network.FactionPackets.RepEntry> repEntries = List.of();

    private List<com.withouthonor.npcs.network.FactionPackets.RepEntry> repFiltered() {
        if (repSearch.isEmpty()) {
            return repEntries;
        }
        String q = repSearch.toLowerCase(java.util.Locale.ROOT);
        List<com.withouthonor.npcs.network.FactionPackets.RepEntry> out = new ArrayList<>();
        for (var e : repEntries) {
            if (e.name().toLowerCase(java.util.Locale.ROOT).contains(q)) {
                out.add(e);
            }
        }
        return out;
    }

    private final List<EntryPoint> entryPoints = new ArrayList<>();
    private final List<EditBox> entryBoxes = new ArrayList<>();
    private int entryScroll;
    private static final int ENTRY_ROW_H = 22;
    private static final int ENTRY_VISIBLE = 5;
    private static final net.minecraft.resources.ResourceLocation INDICATOR_ATLAS =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("wh_npcs", "textures/entity/emotes.png");

    private static final String[] EQUIP_LABEL_KEYS = {"wh_npcs.ui.npc.equip_head", "wh_npcs.ui.npc.equip_chest",
            "wh_npcs.ui.npc.equip_legs", "wh_npcs.ui.npc.equip_boots", "wh_npcs.ui.npc.equip_mainhand",
            "wh_npcs.ui.npc.equip_offhand"};
    @Nullable
    private final com.withouthonor.npcs.common.entity.CompanionEntity npc;

    private final int siblingCount;
    private final net.minecraft.world.item.ItemStack[] funcEquip = new net.minecraft.world.item.ItemStack[6];
    private final net.minecraft.world.item.ItemStack[] cosmEquip = new net.minecraft.world.item.ItemStack[6];

    private net.minecraft.world.item.ItemStack arrowEquip = net.minecraft.world.item.ItemStack.EMPTY;
    private boolean hideArmor;
    private boolean hideMainhand;
    private boolean hideOffhand;

    private net.minecraft.world.item.ItemStack carried = net.minecraft.world.item.ItemStack.EMPTY;

    private boolean carriedFromNpc;

    private int carriedInvSlot = -1;

    private int winX, winY, winW, winH;
    private int contentX, contentY, contentW, contentH;

    private NpcEditorScreen(JsonObject profileJson,
                            List<com.withouthonor.npcs.network.EditorDataPacket.DialogueSummary> dialogues,
                            @Nullable com.withouthonor.npcs.common.entity.CompanionEntity npc,
                            int siblingCount) {
        super(Component.translatable("wh_npcs.ui.npc.title"));
        this.profileJson = profileJson;
        this.dialogues = dialogues;
        this.npc = npc;
        this.siblingCount = siblingCount;
        var slots = com.withouthonor.npcs.network.SaveEquipmentPacket.SLOTS;
        for (int i = 0; i < 6; i++) {
            funcEquip[i] = npc != null ? npc.getFunctionalItem(slots[i]).copy()
                    : net.minecraft.world.item.ItemStack.EMPTY;
            cosmEquip[i] = npc != null ? npc.getCosmeticItem(slots[i]).copy()
                    : net.minecraft.world.item.ItemStack.EMPTY;
        }
        arrowEquip = npc != null ? npc.getArrowItem().copy() : net.minecraft.world.item.ItemStack.EMPTY;
        hideArmor = npc != null && npc.isHideArmor();
        hideMainhand = npc != null && npc.isHideMainhand();
        hideOffhand = npc != null && npc.isHideOffhand();
        loadDropsFromJson();
        if (profileJson.has("entry_points")) {
            for (JsonElement e : profileJson.getAsJsonArray("entry_points")) {
                try {
                    entryPoints.add(EntryPoint.fromJson(e.getAsJsonObject()));
                } catch (Exception ignored) {

                }
            }
        }
    }

    public static void acceptRepData(String factionId,
                                     List<com.withouthonor.npcs.network.FactionPackets.RepEntry> entries) {
        if (Minecraft.getInstance().screen instanceof NpcEditorScreen screen
                && factionId.equals(screen.selectedFaction)) {
            screen.repEntries = entries;
            screen.repLoading = false;
        }
    }

    public static void acceptDialogueList(
            List<com.withouthonor.npcs.network.EditorDataPacket.DialogueSummary> list) {
        if (Minecraft.getInstance().screen instanceof NpcEditorScreen screen) {
            screen.dialogues.clear();
            screen.dialogues.addAll(list);
            screen.dialogueScroll = 0;
        }
    }

    public static void open(String profileJson,
                            List<com.withouthonor.npcs.network.EditorDataPacket.DialogueSummary> dialogues,
                            int entityId, int siblingCount) {
        JsonObject json = JsonParser.parseString(profileJson).getAsJsonObject();
        com.withouthonor.npcs.common.entity.CompanionEntity npc = null;
        var level = Minecraft.getInstance().level;
        if (level != null && entityId >= 0
                && level.getEntity(entityId) instanceof com.withouthonor.npcs.common.entity.CompanionEntity c) {
            npc = c;
        }
        Minecraft.getInstance().setScreen(new NpcEditorScreen(json, dialogues, npc, siblingCount));
    }

    private java.util.Set<String> dialogueIdSet() {
        java.util.Set<String> ids = new java.util.HashSet<>();
        dialogues.forEach(d -> ids.add(d.id()));
        return ids;
    }

    @Override
    protected void init() {
        recalc();
        clearWidgets();
        carried = net.minecraft.world.item.ItemStack.EMPTY;
        carriedFromNpc = false;
        carriedInvSlot = -1;
        chanceEditSlot = -1;
        chanceEditBox = null;
        if (activeTab == 0) {
            int x = contentX + 110;
            int w = Math.min(220, contentW - 120);

            String name = str("name", "Companion");
            String legacyColor = str("name_color", "");
            if (!name.contains("§") && !legacyColor.isEmpty()) {
                ChatFormatting color = ChatFormatting.getByName(legacyColor);
                if (color != null && color.getChar() != 0) {
                    name = "§" + color.getChar() + name;
                }
            }
            nameBox = addRenderableWidget(new com.withouthonor.npcs.client.gui.RichTextEditor(
                    font, x, contentY + 12, w, 18).singleLine());
            nameBox.setValue(name);

            titleBox = addRenderableWidget(new com.withouthonor.npcs.client.gui.RichTextEditor(
                    font, x, contentY + 40, w - 22, 18).singleLine().colorOnly());
            titleBox.setValue(str("title", ""));

        } else if (activeTab == 1) {
            String oldSearch = dlgSearchBox != null ? dlgSearchBox.getValue() : "";
            dlgSearchBox = addRenderableWidget(new com.withouthonor.npcs.client.gui.SelectableEditBox(font,
                    contentX + 6, contentY + 3, 150, 16, Component.translatable("wh_npcs.ui.npc.dlg_search")));
            dlgSearchBox.setMaxLength(64);
            dlgSearchBox.setValue(oldSearch);
            dlgSearchBox.setHint(Component.translatable("wh_npcs.ui.npc.dlg_search_hint"));
            dlgSearchBox.setResponder(v -> dialogueScroll = 0);
            newDialogueBox = addRenderableWidget(new com.withouthonor.npcs.client.gui.SelectableEditBox(font,
                    contentX + 8, contentY + contentH - 30, contentW - 106, 18,
                    Component.translatable("wh_npcs.ui.npc.dlg_new_id")));
            newDialogueBox.setMaxLength(64);
            newDialogueBox.setHint(Component.translatable("wh_npcs.ui.npc.dlg_new_id_hint"));

            newDialogueBox.setResponder(v -> {
                String s = v.trim();
                newDialogueBox.setTextColor(s.isEmpty() || s.matches("[a-z0-9_]{1,64}") ? 0xE0E0E0 : 0xFF5555);
            });
        } else if (activeTab == 4) {
            if (!factionsRequested) {
                factionsRequested = true;
                NetworkHandler.sendToServer(new com.withouthonor.npcs.network.FactionPackets.Request());
            }
            buildFactionWidgets();
        } else if (activeTab == 5) {
            buildCombatWidgets();
        } else if (activeTab == 2) {
            entryBoxes.clear();
            entryScroll = Math.max(0, Math.min(entryScroll, Math.max(0, entryPoints.size() - ENTRY_VISIBLE)));
            int visible = Math.min(ENTRY_VISIBLE, entryPoints.size());
            for (int row = 0; row < visible; row++) {
                EntryPoint entry = entryPoints.get(entryScroll + row);
                EditBox box = addRenderableWidget(new com.withouthonor.npcs.client.gui.SelectableEditBox(font,
                        contentX + 58, entryRowY(row) + 1, contentW - 58 - 144, 16,
                        Component.translatable("wh_npcs.ui.npc.entry_dialogue")));
                box.setMaxLength(64);
                box.setValue(entry.getDialogueId());
                box.setHint(Component.translatable("wh_npcs.ui.npc.entry_dialogue_hint"));
                entryBoxes.add(box);
            }
        } else if (activeTab == 7) {
            buildPoseWidgets();
        }
    }

    private int entryRowY(int row) {
        return contentY + 14 + row * ENTRY_ROW_H;
    }

    private float numF(String key, float def) {
        return profileJson.has(key) ? profileJson.get(key).getAsFloat() : def;
    }

    private int numI(String key, int def) {
        return profileJson.has(key) ? profileJson.get(key).getAsInt() : def;
    }

    private int combatRightX() {
        return contentX + 232;
    }

    private int tabGap() {
        int total = 0;
        for (String key : TAB_KEYS) {
            total += font.width(tr(key)) + 16;
        }
        return Math.max(2, (winW - PAD * 2 - total) / (TAB_KEYS.length - 1));
    }

    private EditBox combatBox(int x, int y, int w, String key, float def, float min, float max) {
        EditBox box = addRenderableWidget(new com.withouthonor.npcs.client.gui.SelectableEditBox(
                font, x, y, w, 16, Component.literal(key)));
        box.setMaxLength(6);
        float value = numF(key, def);
        box.setValue(value == (int) value ? String.valueOf((int) value) : String.valueOf(value));
        box.setResponder(v -> {
            try {
                float parsed = Math.max(min, Math.min(max, Float.parseFloat(v.trim().replace(',', '.'))));
                profileJson.addProperty(key, parsed);
            } catch (NumberFormatException ignored) {
            }
        });
        return box;
    }

    private void buildCombatWidgets() {
        int fx = contentX + 8;
        hpBox = combatBox(fx + 50, contentY + 18, 40, "max_health", 20.0F, 1.0F, 1024.0F);
        hpBox.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                Component.translatable("wh_npcs.ui.npc.tip_health")));
        dmgBox = combatBox(fx + 150, contentY + 18, 40, "attack_damage", 3.0F, 0.0F, 1024.0F);
        dmgBox.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                Component.translatable("wh_npcs.ui.npc.tip_damage")));
        armorBox = combatBox(fx + 50, contentY + 40, 40, "armor", 0.0F, 0.0F, 30.0F);
        armorBox.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                Component.translatable("wh_npcs.ui.npc.tip_armor")));
        kbBox = combatBox(fx + 150, contentY + 40, 40, "kb_resistance", 0.0F, 0.0F, 1.0F);
        kbBox.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                Component.translatable("wh_npcs.ui.npc.tip_kb")));
        speedBox = combatBox(fx + 50, contentY + 62, 40, "move_speed", 0.25F, 0.0F, 1.0F);
        speedBox.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                Component.translatable("wh_npcs.ui.npc.tip_speed")));
        regenBox = combatBox(fx + 150, contentY + 62, 40, "regen_per_second", 0.0F, 0.0F, 100.0F);
        regenBox.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                Component.translatable("wh_npcs.ui.npc.tip_regen")));
        totemChargesBox = combatBox(fx + 376, contentY + 205, 30, "totem_charges", 0.0F, 0.0F, 99.0F);
        int rx = combatRightX();
        xpMinBox = combatBox(rx + 64, contentY + 147, 34, "death_xp_min", 0.0F, 0.0F, 10000.0F);
        xpMaxBox = combatBox(rx + 124, contentY + 147, 34, "death_xp_max", 0.0F, 0.0F, 10000.0F);
        xpMaxBox.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                Component.translatable("wh_npcs.ui.npc.tip_xp")));
    }

    private static final String[] AXIS = {"x", "y", "z"};
    private static final String[] AXIS_LABELS = {"X", "Y", "Z"};

    private static final int POSE_PREV_W = 150;

    private static final int POSE_BTN_W = 12, POSE_BOX_W = 36, POSE_CTRL_H = 18;
    private static final int POSE_ROT_DY = 46, POSE_POS_DY = 76,
            POSE_UNI_DY = 112, POSE_SCALE_DY = 138, POSE_RESETALL_DY = 176, POSE_VIEW_DY = 200,
            POSE_PARTS_DY = 246;

    private int posePrevX() {
        return contentX + 4;
    }

    private int poseRx() {
        return contentX + POSE_PREV_W + 16;
    }

    private int poseCellX(int i) {
        return poseRx() + 56 + i * 76;
    }

    private int poseBoxX(int i) {
        return poseCellX(i) + POSE_BTN_W + 4;
    }

    private int posePlusX(int i) {
        return poseBoxX(i) + POSE_BOX_W + 4;
    }

    private static String fmtNum(float v) {
        return v == (int) v ? String.valueOf((int) v) : String.valueOf(v);
    }

    private static float poseDefault(String prefix) {
        return prefix.equals("scale_") ? 1F : 0F;
    }

    private void buildPoseWidgets() {
        for (int i = 0; i < 3; i++) {
            rotBox[i] = poseBox(poseBoxX(i), contentY + POSE_ROT_DY, "rot_" + AXIS[i], 0F, -180F, 180F);
            posBox[i] = poseBox(poseBoxX(i), contentY + POSE_POS_DY, "pos_" + AXIS[i], 0F, -3F, 3F);
            scaleBox[i] = poseBox(poseBoxX(i), contentY + POSE_SCALE_DY, "scale_" + AXIS[i], 1F, 0.1F, 4F);
        }
        EditBox uni = addRenderableWidget(new com.withouthonor.npcs.client.gui.SelectableEditBox(
                font, poseBoxX(0), contentY + POSE_UNI_DY, POSE_BOX_W, POSE_CTRL_H, Component.literal("uniform")));
        uni.setMaxLength(7);
        uni.setHint(Component.literal("1.0"));
        uni.setResponder(this::applyUniformScale);
        scaleUniBox = uni;
    }

    private EditBox poseBox(int x, int y, String key, float def, float min, float max) {
        EditBox box = addRenderableWidget(new com.withouthonor.npcs.client.gui.SelectableEditBox(
                font, x, y, POSE_BOX_W, POSE_CTRL_H, Component.literal(key)));
        box.setMaxLength(7);
        box.setValue(fmtNum(numF(key, def)));
        box.setResponder(v -> {
            String s = v.trim().replace(',', '.');
            if (s.isEmpty()) {
                profileJson.remove(key);
                pushPosePreview();
                return;
            }
            try {
                float p = Math.max(min, Math.min(max, Float.parseFloat(s)));
                if (p == def) {
                    profileJson.remove(key);
                } else {
                    profileJson.addProperty(key, p);
                }
            } catch (NumberFormatException ignored) {
                return;
            }
            pushPosePreview();
        });
        return box;
    }

    private void applyUniformScale(String v) {
        String s = v.trim().replace(',', '.');
        if (s.isEmpty()) {
            return;
        }
        float p;
        try {
            p = Math.max(0.1F, Math.min(4.0F, Float.parseFloat(s)));
        } catch (NumberFormatException ignored) {
            return;
        }
        for (int i = 0; i < 3; i++) {
            if (scaleBox[i] != null) {
                scaleBox[i].setValue(fmtNum(p));
            }
        }
    }

    private float currentUniformScale() {
        if (scaleUniBox != null) {
            try {
                return Float.parseFloat(scaleUniBox.getValue().trim().replace(',', '.'));
            } catch (NumberFormatException ignored) {

            }
        }
        return numF("scale_x", 1F);
    }

    private void setUniformScale(float v) {
        float p = Math.max(0.1F, Math.min(4.0F, Math.round(v * 100F) / 100F));
        if (scaleUniBox != null) {
            scaleUniBox.setValue(fmtNum(p));
        }
    }

    private void pushPosePreview() {
        if (npc != null) {
            npc.setRenderTransformClient(
                    numF("rot_x", 0), numF("rot_y", 0), numF("rot_z", 0),
                    numF("pos_x", 0), numF("pos_y", 0), numF("pos_z", 0),
                    numF("scale_x", 1), numF("scale_y", 1), numF("scale_z", 1));
        }
    }

    private void renderPoseTab(GuiGraphics g, int mouseX, int mouseY) {
        renderPosePreview(g, mouseX, mouseY, posePrevX(), contentY, POSE_PREV_W, contentH - 8);

        int rx = poseRx();
        g.drawString(font, tr("wh_npcs.ui.npc.pose_transform"), rx, contentY + 2, VanillaUIHelper.TEXT_YELLOW, false);
        g.drawString(font, tr("wh_npcs.ui.npc.pose_render_only"), rx, contentY + 14, VanillaUIHelper.TEXT_WHITE, false);
        for (int i = 0; i < 3; i++) {
            g.drawCenteredString(font, AXIS_LABELS[i], poseBoxX(i) + POSE_BOX_W / 2,
                    contentY + 34, VanillaUIHelper.TEXT_DARK_GRAY);
        }
        poseRow(g, tr("wh_npcs.ui.npc.pose_rotation"), contentY + POSE_ROT_DY, mouseX, mouseY);
        poseRow(g, tr("wh_npcs.ui.npc.pose_position"), contentY + POSE_POS_DY, mouseX, mouseY);
        g.drawString(font, tr("wh_npcs.ui.npc.pose_scale"), rx, contentY + POSE_UNI_DY + 5, VanillaUIHelper.TEXT_GRAY, false);
        poseStepper(g, poseCellX(0), contentY + POSE_UNI_DY, mouseX, mouseY);
        poseRow(g, tr("wh_npcs.ui.npc.pose_per_axis"), contentY + POSE_SCALE_DY, mouseX, mouseY);

        boolean allHover = isOver(mouseX, mouseY, rx, contentY + POSE_RESETALL_DY, 130, 18);
        VanillaUIHelper.drawButton(g, rx, contentY + POSE_RESETALL_DY, 130, 18, allHover);
        g.drawCenteredString(font, tr("wh_npcs.ui.npc.pose_reset_all"), rx + 65, contentY + POSE_RESETALL_DY + 5,
                allHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_WHITE);

        int vy = contentY + POSE_VIEW_DY;
        boolean rotHover = isOver(mouseX, mouseY, rx, vy, 142, 18);
        VanillaUIHelper.drawButton(g, rx, vy, 142, 18, rotHover);
        g.drawCenteredString(font, "↗ " + tr("wh_npcs.ui.npc.pose_world_rot"), rx + 71, vy + 5,
                rotHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_AQUA);
        boolean posHover = isOver(mouseX, mouseY, rx + 148, vy, 142, 18);
        VanillaUIHelper.drawButton(g, rx + 148, vy, 142, 18, posHover);
        g.drawCenteredString(font, "↗ " + tr("wh_npcs.ui.npc.pose_world_pos"), rx + 148 + 71, vy + 5,
                posHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_AQUA);
        g.drawString(font, tr("wh_npcs.ui.npc.pose_drag_hint"),
                rx, contentY + POSE_VIEW_DY + 24, VanillaUIHelper.TEXT_WHITE, false);

        int py = contentY + POSE_PARTS_DY;
        boolean partsHover = isOver(mouseX, mouseY, rx, py, 178, 18);
        VanillaUIHelper.drawButton(g, rx, py, 178, 18, partsHover);
        g.drawCenteredString(font, tr("wh_npcs.ui.npc.pose_parts"), rx + 89, py + 5,
                partsHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_AQUA);
        boolean emoLoaded = com.withouthonor.npcs.compat.Compat.emotecraftLoaded();
        boolean emoHover = isOver(mouseX, mouseY, rx + 184, py, 106, 18);
        VanillaUIHelper.drawButton(g, rx + 184, py, 106, 18, emoHover && emoLoaded);
        g.drawCenteredString(font, tr("wh_npcs.ui.npc.pose_emotes"), rx + 184 + 53, py + 5,
                !emoLoaded ? VanillaUIHelper.TEXT_DARK_GRAY
                        : (emoHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_AQUA));
        if (emoHover && !emoLoaded) {
            multilineTooltip(g, tr("wh_npcs.ui.npc.pose_emotes_missing"), mouseX, mouseY);
        }
    }

    private void poseRow(GuiGraphics g, String label, int y, int mouseX, int mouseY) {
        g.drawString(font, label, poseRx(), y + 5, VanillaUIHelper.TEXT_GRAY, false);
        for (int i = 0; i < 3; i++) {
            poseStepper(g, poseCellX(i), y, mouseX, mouseY);
        }
    }

    private void poseStepper(GuiGraphics g, int cellX, int y, int mouseX, int mouseY) {
        int minusX = cellX, plusX = cellX + POSE_BTN_W + 4 + POSE_BOX_W + 4;
        boolean mh = isOver(mouseX, mouseY, minusX, y, POSE_BTN_W, POSE_CTRL_H);
        VanillaUIHelper.drawButton(g, minusX, y, POSE_BTN_W, POSE_CTRL_H, mh);
        g.drawCenteredString(font, "-", minusX + POSE_BTN_W / 2, y + 5,
                mh ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_WHITE);
        boolean ph = isOver(mouseX, mouseY, plusX, y, POSE_BTN_W, POSE_CTRL_H);
        VanillaUIHelper.drawButton(g, plusX, y, POSE_BTN_W, POSE_CTRL_H, ph);
        g.drawCenteredString(font, "+", plusX + POSE_BTN_W / 2, y + 5,
                ph ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_WHITE);
    }

    @Nullable
    private com.withouthonor.npcs.common.entity.CompanionEntity posePreview() {
        if (posePreviewNpc == null && minecraft != null && minecraft.level != null) {
            posePreviewNpc = com.withouthonor.npcs.common.registry.ModEntities.COMPANION.get()
                    .create(minecraft.level);
        }
        return posePreviewNpc;
    }

    private void renderPosePreview(GuiGraphics g, int mouseX, int mouseY, int x, int y, int w, int h) {
        VanillaUIHelper.drawContentPanel(g, x, y, w, h);
        com.withouthonor.npcs.common.entity.CompanionEntity p = posePreview();
        if (p == null || minecraft == null) {
            g.drawCenteredString(font, tr("wh_npcs.ui.npc.no_preview"), x + w / 2, y + h / 2 - 4, VanillaUIHelper.TEXT_WHITE);
            return;
        }
        p.setSkinName(str("skin_player_name", ""));
        p.setRenderTransformClient(
                numF("rot_x", 0), numF("rot_y", 0), numF("rot_z", 0),
                numF("pos_x", 0), numF("pos_y", 0), numF("pos_z", 0),
                numF("scale_x", 1), numF("scale_y", 1), numF("scale_z", 1));
        long now = System.currentTimeMillis();
        if (!poseRotDrag && !posePaused) {
            poseAngle = (poseAngle + (now - poseLastMs) * 0.04F) % 360.0F;
        }
        poseLastMs = now;
        if (minecraft.level != null) {
            if (!posePaused) {
                poseFrozenTick = (int) minecraft.level.getGameTime();
            }
            p.tickCount = poseFrozenTick;
        }
        float scale = (h - 60.0F) / 2.4F;
        ScaledScreen.enableScissor(g, x + 2, y + 2, x + w - 2, y + h - 2);
        poseRenderRotating(g, p, x + w / 2, y + h / 2 + (int) (scale * 0.92F), scale, poseAngle);
        g.disableScissor();

        int pbx = x + w - 20, pby = y + 4;
        boolean ph = isOver(mouseX, mouseY, pbx, pby, 16, 16);
        VanillaUIHelper.drawButton(g, pbx, pby, 16, 16, ph);
        int ic = ph ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_GRAY;
        if (posePaused) {
            g.fill(pbx + 6, pby + 4, pbx + 7, pby + 12, ic);
            g.fill(pbx + 7, pby + 5, pbx + 9, pby + 11, ic);
            g.fill(pbx + 9, pby + 6, pbx + 11, pby + 10, ic);
        } else {
            g.fill(pbx + 5, pby + 4, pbx + 7, pby + 12, ic);
            g.fill(pbx + 9, pby + 4, pbx + 11, pby + 12, ic);
        }
    }

    private void poseRenderRotating(GuiGraphics g,
                                    com.withouthonor.npcs.common.entity.CompanionEntity e,
                                    int x, int y, float scale, float angle) {
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

    private boolean handlePoseClick(double mx, double my, int button) {

        if (button == 0 && isOver(mx, my, posePrevX() + POSE_PREV_W - 20, contentY + 4, 16, 16)) {
            posePaused = !posePaused;
            return true;
        }

        if (button == 0 && isOver(mx, my, posePrevX(), contentY, POSE_PREV_W, contentH - 8)) {
            poseRotDrag = true;
            return true;
        }
        for (int i = 0; i < 3; i++) {
            if (poseAxisClick(mx, my, button, i, "rot_", rotBox[i], contentY + POSE_ROT_DY, 1.0F, 5F, -180F, 180F)) {
                return true;
            }
            if (poseAxisClick(mx, my, button, i, "pos_", posBox[i], contentY + POSE_POS_DY, 0.02F, 0.1F, -3F, 3F)) {
                return true;
            }
            if (poseAxisClick(mx, my, button, i, "scale_", scaleBox[i], contentY + POSE_SCALE_DY, 0.02F, 0.1F, 0.1F, 4F)) {
                return true;
            }
        }
        if (poseUniformClick(mx, my, button)) {
            return true;
        }
        if (button == 0 && isOver(mx, my, poseRx(), contentY + POSE_RESETALL_DY, 130, 18)) {
            resetAllPose();
            return true;
        }

        int vy = contentY + POSE_VIEW_DY;
        if (button == 0 && isOver(mx, my, poseRx(), vy, 142, 18)) {
            openQuickAdjust(false);
            return true;
        }
        if (button == 0 && isOver(mx, my, poseRx() + 148, vy, 142, 18)) {
            openQuickAdjust(true);
            return true;
        }
        if (button == 0 && isOver(mx, my, poseRx(), contentY + POSE_PARTS_DY, 178, 18)) {
            if (minecraft != null) {
                minecraft.setScreen(new PoseEditorScreen(this, profileJson, npc));
            }
            return true;
        }
        if (button == 0 && isOver(mx, my, poseRx() + 184, contentY + POSE_PARTS_DY, 106, 18)) {
            if (com.withouthonor.npcs.compat.Compat.emotecraftLoaded() && minecraft != null) {
                minecraft.setScreen(new EmotecraftScreen(this, profileJson, npc));
            }
            return true;
        }
        return false;
    }

    private void openQuickAdjust(boolean positionMode) {
        if (minecraft != null) {
            minecraft.setScreen(new PoseQuickAdjustScreen(this, profileJson, npc, positionMode));
        }
    }

    private boolean poseAxisClick(double mx, double my, int button, int i, String prefix,
                                  @Nullable EditBox box, int y, float sens, float step, float min, float max) {
        String key = prefix + AXIS[i];
        float def = poseDefault(prefix);
        int minusX = poseCellX(i), boxX = poseBoxX(i), plusX = posePlusX(i);
        if (button == 0 && isOver(mx, my, minusX, y, POSE_BTN_W, POSE_CTRL_H)) {
            adjustPose(box, numF(key, def), -step, min, max);
            return true;
        }
        if (button == 0 && isOver(mx, my, plusX, y, POSE_BTN_W, POSE_CTRL_H)) {
            adjustPose(box, numF(key, def), step, min, max);
            return true;
        }
        if (button == 1 && isOver(mx, my, boxX, y, POSE_BOX_W, POSE_CTRL_H)) {
            resetPose(box, key, def);
            return true;
        }
        if (button == 0 && isOver(mx, my, boxX, y, POSE_BOX_W, POSE_CTRL_H)) {
            beginScrub(box, numF(key, def), mx, sens, min, max);
            return false;
        }
        return false;
    }

    private boolean poseUniformClick(double mx, double my, int button) {
        int y = contentY + POSE_UNI_DY;
        int minusX = poseCellX(0), boxX = poseBoxX(0), plusX = posePlusX(0);
        float cur = currentUniformScale();
        if (button == 0 && isOver(mx, my, minusX, y, POSE_BTN_W, POSE_CTRL_H)) {
            setUniformScale(cur - 0.1F);
            return true;
        }
        if (button == 0 && isOver(mx, my, plusX, y, POSE_BTN_W, POSE_CTRL_H)) {
            setUniformScale(cur + 0.1F);
            return true;
        }
        if (button == 1 && isOver(mx, my, boxX, y, POSE_BOX_W, POSE_CTRL_H)) {
            if (scaleUniBox != null) {
                scaleUniBox.setValue("");
            }
            for (int i = 0; i < 3; i++) {
                resetPose(scaleBox[i], "scale_" + AXIS[i], 1F);
            }
            return true;
        }
        if (button == 0 && isOver(mx, my, boxX, y, POSE_BOX_W, POSE_CTRL_H)) {
            beginScrub(scaleUniBox, cur, mx, 0.02F, 0.1F, 4F);
            return false;
        }
        return false;
    }

    private void beginScrub(@Nullable EditBox box, float startVal, double mx,
                            float sens, float min, float max) {
        scrubBox = box;
        scrubStartVal = startVal;
        scrubStartX = mx;
        scrubSens = sens;
        scrubMin = min;
        scrubMax = max;
        scrubbing = false;
    }

    private void adjustPose(@Nullable EditBox box, float base, float delta, float min, float max) {
        if (box == null) {
            return;
        }
        float v = Math.max(min, Math.min(max, base + delta));
        v = Math.round(v * 100F) / 100F;
        box.setValue(fmtNum(v));
    }

    private void resetPose(@Nullable EditBox box, String key, float def) {
        if (box != null) {
            box.setValue(fmtNum(def));
        } else {
            profileJson.remove(key);
            pushPosePreview();
        }
    }

    private void resetAllPose() {
        for (String k : new String[]{"rot_x", "rot_y", "rot_z",
                "pos_x", "pos_y", "pos_z", "scale_x", "scale_y", "scale_z"}) {
            profileJson.remove(k);
        }
        if (scaleUniBox != null) {
            scaleUniBox.setValue("");
        }
        init(minecraft, width, height);
        pushPosePreview();
    }

    private void loadDropsFromJson() {
        for (int i = 0; i < 9; i++) {
            dropItem[i] = null;
            dropNbt[i] = null;
            dropCount[i] = 1;
            dropChance[i] = 100;
        }
        if (!profileJson.has("drops")) {
            return;
        }
        int i = 0;
        for (JsonElement e : profileJson.getAsJsonArray("drops")) {
            if (i >= 9) {
                break;
            }
            try {
                var entry = com.withouthonor.npcs.common.profile.CompanionProfile.DropEntry
                        .fromJson(e.getAsJsonObject());
                dropItem[i] = entry.item().itemId();
                dropNbt[i] = entry.item().nbt();
                dropCount[i] = entry.item().count();
                dropChance[i] = entry.chancePercent();
                i++;
            } catch (Exception ignored) {
            }
        }
    }

    private net.minecraft.world.item.ItemStack dropStack(int slot) {
        if (dropItem[slot] == null) {
            return net.minecraft.world.item.ItemStack.EMPTY;
        }
        var item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(dropItem[slot]);
        if (item == null) {
            return net.minecraft.world.item.ItemStack.EMPTY;
        }
        var stack = new net.minecraft.world.item.ItemStack(item, Math.max(1, dropCount[slot]));
        if (dropNbt[slot] != null) {
            stack.setTag(dropNbt[slot].copy());
        }
        return stack;
    }

    private void writeDropsToJson() {
        JsonArray array = new JsonArray();
        for (int i = 0; i < 9; i++) {
            if (dropItem[i] != null) {
                array.add(new com.withouthonor.npcs.common.profile.CompanionProfile.DropEntry(
                        new com.withouthonor.npcs.common.dialogue.action.Actions.ItemSpec(
                                dropItem[i], Math.max(1, dropCount[i]), dropNbt[i]),
                        dropChance[i]).toJson());
            }
        }
        if (array.isEmpty()) {
            profileJson.remove("drops");
        } else {
            profileJson.add("drops", array);
        }
    }

    private void renderBehaviorTab(GuiGraphics g, int mouseX, int mouseY) {
        String tooltip = null;
        int c1 = contentX + 8;
        int c2 = contentX + 210;
        g.drawString(font, tr("wh_npcs.ui.npc.behavior_follow_header"), c1, contentY + 4, VanillaUIHelper.TEXT_GRAY, false);

        String t;
        t = behaviorToggle(g, c1, contentY + 22, "follow_run", tr("wh_npcs.ui.npc.bh_run"), mouseX, mouseY,
                tr("wh_npcs.ui.npc.bh_run_tip"));
        if (t != null) tooltip = t;
        t = behaviorToggle(g, c2, contentY + 22, "follow_teleport", tr("wh_npcs.ui.npc.bh_tp"), mouseX, mouseY,
                tr("wh_npcs.ui.npc.bh_tp_tip"));
        if (t != null) tooltip = t;
        t = behaviorToggle(g, c1, contentY + 42, "follow_teleport_oos", tr("wh_npcs.ui.npc.bh_tp_oos"), mouseX, mouseY,
                tr("wh_npcs.ui.npc.bh_tp_oos_tip"));
        if (t != null) tooltip = t;
        t = behaviorToggle(g, c2, contentY + 42, "follow_match_speed", tr("wh_npcs.ui.npc.bh_match_speed"), mouseX, mouseY,
                tr("wh_npcs.ui.npc.bh_match_speed_tip"));
        if (t != null) tooltip = t;
        t = behaviorToggle(g, c1, contentY + 62, "open_doors", tr("wh_npcs.ui.npc.bh_doors"), mouseX, mouseY,
                tr("wh_npcs.ui.npc.bh_doors_tip"));
        if (t != null) tooltip = t;
        t = behaviorToggle(g, c2, contentY + 62, "avoid_danger", tr("wh_npcs.ui.npc.bh_avoid"), mouseX, mouseY,
                tr("wh_npcs.ui.npc.bh_avoid_tip"));
        if (t != null) tooltip = t;
        t = behaviorToggle(g, c1, contentY + 82, "teleport_fx", tr("wh_npcs.ui.npc.bh_tp_fx"), mouseX, mouseY,
                tr("wh_npcs.ui.npc.bh_tp_fx_tip"));
        if (t != null) tooltip = t;
        t = behaviorToggle(g, c2, contentY + 82, "group_spacing", tr("wh_npcs.ui.npc.bh_spacing"), mouseX, mouseY,
                tr("wh_npcs.ui.npc.bh_spacing_tip"));
        if (t != null) tooltip = t;

        g.drawString(font, tr("wh_npcs.ui.npc.bh_distance"), c1, contentY + 110, VanillaUIHelper.TEXT_GRAY, false);
        String dist = profileJson.has("follow_distance")
                ? profileJson.get("follow_distance").getAsString() : "normal";
        String distLabel = switch (dist) {
            case "close" -> tr("wh_npcs.ui.npc.bh_dist_close");
            case "far" -> tr("wh_npcs.ui.npc.bh_dist_far");
            default -> tr("wh_npcs.ui.npc.bh_dist_normal");
        };
        boolean distHover = isOver(mouseX, mouseY, c1 + 66, contentY + 106, 96, 18);
        VanillaUIHelper.drawButton(g, c1 + 66, contentY + 106, 96, 18, distHover);
        g.drawCenteredString(font, distLabel + " ▾", c1 + 66 + 48, contentY + 111,
                distHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_AQUA);
        if (distHover) {
            tooltip = tr("wh_npcs.ui.npc.bh_distance_tip");
        }

        g.drawString(font, tr("wh_npcs.ui.npc.bh_applies_note"),
                c1, contentY + 134, VanillaUIHelper.TEXT_WHITE, false);

        g.drawString(font, tr("wh_npcs.ui.npc.bh_combat_header"), c1, contentY + 156, VanillaUIHelper.TEXT_GRAY, false);
        t = behaviorToggle(g, c1, contentY + 174, "pursue_attacker", tr("wh_npcs.ui.npc.bh_pursue"), mouseX, mouseY,
                tr("wh_npcs.ui.npc.bh_pursue_tip"));
        if (t != null) tooltip = t;
        t = behaviorToggleOff(g, c2, contentY + 174, "hold_position", tr("wh_npcs.ui.npc.bh_hold"), mouseX, mouseY,
                tr("wh_npcs.ui.npc.bh_hold_tip"));
        if (t != null) tooltip = t;

        boolean schedHover = isOver(mouseX, mouseY, c1, contentY + 200, 160, 18);
        VanillaUIHelper.drawButton(g, c1, contentY + 200, 160, 18, schedHover);
        g.drawCenteredString(font, tr("wh_npcs.ui.npc.bh_schedule"), c1 + 80, contentY + 205,
                schedHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_AQUA);
        if (schedHover) {
            tooltip = tr("wh_npcs.ui.npc.bh_schedule_tip");
        }

        boolean traitsHover = isOver(mouseX, mouseY, c2, contentY + 200, 160, 18);
        VanillaUIHelper.drawButton(g, c2, contentY + 200, 160, 18, traitsHover);
        g.drawCenteredString(font, tr("wh_npcs.ui.npc.bh_traits"), c2 + 80, contentY + 205,
                traitsHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_AQUA);
        if (traitsHover) {
            tooltip = tr("wh_npcs.ui.npc.bh_traits_tip");
        }

        boolean reactHover = isOver(mouseX, mouseY, c1, contentY + 222, 160, 18);
        VanillaUIHelper.drawButton(g, c1, contentY + 222, 160, 18, reactHover);
        g.drawCenteredString(font, tr("wh_npcs.ui.npc.bh_reactions"), c1 + 80, contentY + 227,
                reactHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_AQUA);
        if (reactHover) {
            tooltip = tr("wh_npcs.ui.npc.bh_reactions_tip");
        }

        boolean flagsHover = isOver(mouseX, mouseY, c2, contentY + 222, 160, 18);
        VanillaUIHelper.drawButton(g, c2, contentY + 222, 160, 18, flagsHover);
        g.drawCenteredString(font, tr("wh_npcs.ui.npc.bh_flags"), c2 + 80, contentY + 227,
                flagsHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_AQUA);
        if (flagsHover) {
            tooltip = tr("wh_npcs.ui.npc.bh_flags_tip");
        }
        if (tooltip != null) {
            multilineTooltip(g, tooltip, mouseX, mouseY);
        }
    }

    private String behaviorToggle(GuiGraphics g, int x, int y, String key, String label,
                                  int mouseX, int mouseY, String tip) {
        boolean on = !profileJson.has(key) || profileJson.get(key).getAsBoolean();
        boolean boxHover = isOver(mouseX, mouseY, x, y, 12, 12);
        VanillaUIHelper.drawButton(g, x, y, 12, 12, boxHover);
        if (on) {
            VanillaUIHelper.drawCheck(g, x + 1, y + 2, VanillaUIHelper.TEXT_GREEN);
        }
        g.drawString(font, label, x + 16, y + 2, VanillaUIHelper.TEXT_GRAY, false);
        boolean labelHover = isOver(mouseX, mouseY, x + 16, y, Math.min(170, font.width(label) + 6), 12);
        return boxHover || labelHover ? tip : null;
    }

    private boolean behaviorToggleClick(double mx, double my, int x, int y, String key) {
        if (isOver(mx, my, x, y, 12, 12)) {
            boolean on = !profileJson.has(key) || profileJson.get(key).getAsBoolean();
            profileJson.addProperty(key, !on);
            return true;
        }
        return false;
    }

    private String behaviorToggleOff(GuiGraphics g, int x, int y, String key, String label,
                                     int mouseX, int mouseY, String tip) {
        boolean on = profileJson.has(key) && profileJson.get(key).getAsBoolean();
        boolean boxHover = isOver(mouseX, mouseY, x, y, 12, 12);
        VanillaUIHelper.drawButton(g, x, y, 12, 12, boxHover);
        if (on) {
            VanillaUIHelper.drawCheck(g, x + 1, y + 2, VanillaUIHelper.TEXT_GREEN);
        }
        g.drawString(font, label, x + 16, y + 2, VanillaUIHelper.TEXT_GRAY, false);
        boolean labelHover = isOver(mouseX, mouseY, x + 16, y, Math.min(170, font.width(label) + 6), 12);
        return boxHover || labelHover ? tip : null;
    }

    private boolean behaviorToggleClickOff(double mx, double my, int x, int y, String key) {
        if (isOver(mx, my, x, y, 12, 12)) {
            if (profileJson.has(key) && profileJson.get(key).getAsBoolean()) {
                profileJson.remove(key);
            } else {
                profileJson.addProperty(key, true);
            }
            return true;
        }
        return false;
    }

    private boolean handleBehaviorClick(double mx, double my, int button) {
        if (button != 0) {
            return false;
        }
        int c1 = contentX + 8;
        int c2 = contentX + 210;
        if (behaviorToggleClick(mx, my, c1, contentY + 22, "follow_run")) return true;
        if (behaviorToggleClick(mx, my, c2, contentY + 22, "follow_teleport")) return true;
        if (behaviorToggleClick(mx, my, c1, contentY + 42, "follow_teleport_oos")) return true;
        if (behaviorToggleClick(mx, my, c2, contentY + 42, "follow_match_speed")) return true;
        if (behaviorToggleClick(mx, my, c1, contentY + 62, "open_doors")) return true;
        if (behaviorToggleClick(mx, my, c2, contentY + 62, "avoid_danger")) return true;
        if (behaviorToggleClick(mx, my, c1, contentY + 82, "teleport_fx")) return true;
        if (behaviorToggleClick(mx, my, c2, contentY + 82, "group_spacing")) return true;
        if (behaviorToggleClick(mx, my, c1, contentY + 174, "pursue_attacker")) return true;
        if (behaviorToggleClickOff(mx, my, c2, contentY + 174, "hold_position")) return true;
        if (isOver(mx, my, c1 + 66, contentY + 106, 96, 18)) {
            String dist = profileJson.has("follow_distance")
                    ? profileJson.get("follow_distance").getAsString() : "normal";
            String next = switch (dist) {
                case "close" -> "normal";
                case "normal" -> "far";
                default -> "close";
            };
            profileJson.addProperty("follow_distance", next);
            return true;
        }
        if (isOver(mx, my, c1, contentY + 200, 160, 18)) {
            if (minecraft != null) {
                minecraft.setScreen(new ScheduleScreen(this, profileJson, npc));
            }
            return true;
        }
        if (isOver(mx, my, c2, contentY + 200, 160, 18)) {
            if (minecraft != null) {
                minecraft.setScreen(new BehaviorTraitsScreen(this, profileJson));
            }
            return true;
        }
        if (isOver(mx, my, c1, contentY + 222, 160, 18)) {
            if (minecraft != null) {
                minecraft.setScreen(new ReactionsScreen(this, profileJson));
            }
            return true;
        }
        if (isOver(mx, my, c2, contentY + 222, 160, 18)) {
            if (minecraft != null) {
                minecraft.setScreen(new FlagsScreen(this));
            }
            return true;
        }
        return false;
    }

    private void renderCombatTab(GuiGraphics g, int mouseX, int mouseY) {
        String tooltip = null;
        int fx = contentX + 8;
        int rx = combatRightX();
        boolean ef = com.withouthonor.npcs.compat.Compat.epicFightLoaded();

        g.fill(contentX + 222, contentY + 2, contentX + 223, contentY + 184, 0xFF373737);

        g.drawString(font, tr("wh_npcs.ui.npc.combat_sec_stats"), fx, contentY + 2, VanillaUIHelper.TEXT_GRAY, false);
        VanillaUIHelper.drawSeparator(g, fx, contentY + 12, 206);

        drawHeart(g, fx, contentY + 21, 0xFFFF5555);
        g.drawString(font, tr("wh_npcs.ui.npc.combat_hp"), fx + 11, contentY + 21, VanillaUIHelper.TEXT_GRAY, false);
        drawSwordIcon(g, fx + 104, contentY + 21, 0xFFCCCCCC);
        g.drawString(font, tr("wh_npcs.ui.npc.combat_damage"), fx + 115, contentY + 21, VanillaUIHelper.TEXT_GRAY, false);
        drawChestplateIcon(g, fx, contentY + 43, 0xFF7FB2F0);
        g.drawString(font, tr("wh_npcs.ui.npc.combat_armor"), fx + 11, contentY + 43, VanillaUIHelper.TEXT_GRAY, false);
        drawKnockbackIcon(g, fx + 104, contentY + 43, 0xFFD8A657);
        g.drawString(font, tr("wh_npcs.ui.npc.combat_kb"), fx + 115, contentY + 43, VanillaUIHelper.TEXT_GRAY, false);
        drawBootIcon(g, fx, contentY + 65, 0xFFB08968);
        g.drawString(font, tr("wh_npcs.ui.npc.combat_speed"), fx + 11, contentY + 65, VanillaUIHelper.TEXT_GRAY, false);
        drawHeart(g, fx + 104, contentY + 65, 0xFF55FF55);
        g.drawString(font, tr("wh_npcs.ui.npc.combat_regen"), fx + 115, contentY + 65, VanillaUIHelper.TEXT_GRAY, false);

        g.drawString(font, tr("wh_npcs.ui.npc.combat_sec_behavior"), fx, contentY + 86, VanillaUIHelper.TEXT_GRAY, false);
        VanillaUIHelper.drawSeparator(g, fx, contentY + 96, 206);

        g.drawString(font, tr("wh_npcs.ui.npc.combat_style"), fx, contentY + 107, VanillaUIHelper.TEXT_GRAY, false);
        String preset = profileJson.has("combat_preset")
                ? profileJson.get("combat_preset").getAsString() : "passive";
        int presetIdx = presetIndex(preset);
        boolean presetHover = isOver(mouseX, mouseY, fx + 52, contentY + 102, 158, 18);
        VanillaUIHelper.drawButton(g, fx + 52, contentY + 102, 158, 18, presetHover);
        g.drawCenteredString(font, tr(PRESET_NAME_KEYS[presetIdx]) + " ▾", fx + 131, contentY + 107,
                presetHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_AQUA);
        if (presetHover) {
            tooltip = tr("wh_npcs.ui.npc.combat_style_tip");
        }

        String mt = str("mob_type", "undefined");
        int mtIdx = 0;
        for (int i = 0; i < MOBTYPE_IDS.length; i++) {
            if (MOBTYPE_IDS[i].equals(mt)) {
                mtIdx = i;
            }
        }
        g.drawString(font, tr("wh_npcs.ui.npc.combat_type"), fx, contentY + 129, VanillaUIHelper.TEXT_GRAY, false);
        boolean mtHover = isOver(mouseX, mouseY, fx + 52, contentY + 124, 158, 18);
        VanillaUIHelper.drawButton(g, fx + 52, contentY + 124, 158, 18, mtHover);
        g.drawCenteredString(font, tr(MOBTYPE_NAME_KEYS[mtIdx]) + " ▾", fx + 131, contentY + 129,
                mtHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_AQUA);
        if (mtHover) {
            tooltip = tr("wh_npcs.ui.npc.combat_type_tip");
        }

        int chkY = contentY + 148;
        if (ef) {
            boolean efMode = "epicfight".equals(str("combat_system", "vanilla"));
            g.drawString(font, tr("wh_npcs.ui.npc.combat_system"), fx, contentY + 151, VanillaUIHelper.TEXT_GRAY, false);
            boolean sysHover = isOver(mouseX, mouseY, fx + 52, contentY + 146, 158, 18);
            VanillaUIHelper.drawButton(g, fx + 52, contentY + 146, 158, 18, sysHover || efMode);
            g.drawCenteredString(font, tr(efMode ? "wh_npcs.ui.npc.combat_system.ef"
                            : "wh_npcs.ui.npc.combat_system.vanilla") + " ▾", fx + 131, contentY + 151,
                    efMode ? VanillaUIHelper.TEXT_YELLOW
                            : (sysHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_AQUA));
            if (sysHover) {
                tooltip = tr("wh_npcs.ui.npc.combat_system_tip");
            }
            chkY = contentY + 170;
        }

        boolean attackable = profileJson.has("attackable") && profileJson.get("attackable").getAsBoolean();
        boolean atkHover = isOver(mouseX, mouseY, fx, chkY, 12, 12);
        VanillaUIHelper.drawButton(g, fx, chkY, 12, 12, atkHover);
        if (attackable) {
            VanillaUIHelper.drawCheck(g, fx + 1, chkY + 2, VanillaUIHelper.TEXT_GREEN);
        }
        g.drawString(font, tr("wh_npcs.ui.npc.combat_attackable"),
                fx + 16, chkY + 2, VanillaUIHelper.TEXT_GRAY, false);
        if (atkHover || isOver(mouseX, mouseY, fx + 16, chkY, 84, 12)) {
            tooltip = tr("wh_npcs.ui.npc.attackable_tip");
        }
        boolean leap = profileJson.has("leap_at_target") && profileJson.get("leap_at_target").getAsBoolean();
        boolean leapHover = isOver(mouseX, mouseY, fx + 104, chkY, 12, 12);
        VanillaUIHelper.drawButton(g, fx + 104, chkY, 12, 12, leapHover);
        if (leap) {
            VanillaUIHelper.drawCheck(g, fx + 105, chkY + 2, VanillaUIHelper.TEXT_GREEN);
        }
        g.drawString(font, tr("wh_npcs.ui.npc.combat_leap"), fx + 120, chkY + 2, VanillaUIHelper.TEXT_GRAY, false);
        if (leapHover || isOver(mouseX, mouseY, fx + 120, chkY, 90, 12)) {
            tooltip = tr("wh_npcs.ui.npc.combat_leap_tip");
        }

        g.drawString(font, tr("wh_npcs.ui.npc.combat_sec_gear"), rx, contentY + 2, VanillaUIHelper.TEXT_GRAY, false);
        VanillaUIHelper.drawSeparator(g, rx, contentY + 12, 226);

        boolean beltHover = isOver(mouseX, mouseY, rx, contentY + 18, 200, 18);
        VanillaUIHelper.drawButton(g, rx, contentY + 18, 200, 18, beltHover);
        g.drawCenteredString(font, tr("wh_npcs.ui.npc.btn_potion_belt"), rx + 100, contentY + 23,
                beltHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_AQUA);
        if (beltHover) {
            tooltip = tr("wh_npcs.ui.npc.potion_belt_tip");
        }
        boolean natHover = isOver(mouseX, mouseY, rx, contentY + 40, 200, 18);
        VanillaUIHelper.drawButton(g, rx, contentY + 40, 200, 18, natHover);
        g.drawCenteredString(font, tr("wh_npcs.ui.npc.btn_nature"), rx + 100, contentY + 45,
                natHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_AQUA);
        if (natHover) {
            tooltip = tr("wh_npcs.ui.npc.nature_tip");
        }
        boolean resHover = isOver(mouseX, mouseY, rx, contentY + 62, 200, 18);
        VanillaUIHelper.drawButton(g, rx, contentY + 62, 200, 18, resHover);
        g.drawCenteredString(font, tr("wh_npcs.ui.npc.btn_resistances"), rx + 100, contentY + 67,
                resHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_AQUA);
        if (resHover) {
            tooltip = tr("wh_npcs.ui.npc.resistances_tip");
        }

        g.drawString(font, tr("wh_npcs.ui.npc.combat_sec_death"), rx, contentY + 86, VanillaUIHelper.TEXT_GRAY, false);
        VanillaUIHelper.drawSeparator(g, rx, contentY + 96, 226);

        g.drawString(font, tr("wh_npcs.ui.npc.combat_outcome"), rx, contentY + 107, VanillaUIHelper.TEXT_GRAY, false);
        String death = profileJson.has("death_behavior")
                ? profileJson.get("death_behavior").getAsString() : "respawn";
        boolean respawn = !"vanish".equals(death);
        boolean deathHover = isOver(mouseX, mouseY, rx + 52, contentY + 102, 130, 18);
        VanillaUIHelper.drawButton(g, rx + 52, contentY + 102, 130, 18, deathHover);
        g.drawCenteredString(font, respawn ? tr("wh_npcs.ui.npc.death_respawn") : tr("wh_npcs.ui.npc.death_vanish"),
                rx + 117, contentY + 107,
                deathHover ? VanillaUIHelper.TEXT_YELLOW
                        : (respawn ? VanillaUIHelper.TEXT_GREEN : VanillaUIHelper.TEXT_RED));
        if (deathHover) {
            tooltip = tr("wh_npcs.ui.npc.death_tip");
        }
        if (respawn) {
            boolean respHover = isOver(mouseX, mouseY, rx, contentY + 124, 200, 18);
            VanillaUIHelper.drawButton(g, rx, contentY + 124, 200, 18, respHover);
            g.drawCenteredString(font, tr("wh_npcs.ui.npc.btn_respawn"), rx + 100, contentY + 129,
                    respHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_AQUA);
            if (respHover) {
                tooltip = tr("wh_npcs.ui.npc.respawn_tip");
            }
        }
        drawXpOrb(g, rx, contentY + 149, 0xFF7FCB1B);
        g.drawString(font, tr("wh_npcs.ui.npc.combat_xp"), rx + 12, contentY + 150, VanillaUIHelper.TEXT_GRAY, false);
        g.drawString(font, tr("wh_npcs.ui.npc.combat_xp_from"), rx + 48, contentY + 150, VanillaUIHelper.TEXT_GRAY, false);
        g.drawString(font, tr("wh_npcs.ui.npc.combat_xp_to"), rx + 104, contentY + 150, VanillaUIHelper.TEXT_GRAY, false);

        int sy = contentY + 186;
        g.fill(fx, sy, fx + 450, sy + 1, 0xFF373737);
        g.drawString(font, tr("wh_npcs.ui.npc.combat_sec_display"), fx, sy + 6, VanillaUIHelper.TEXT_GRAY, false);

        int dy = sy + 22;
        boolean barOn = profileJson.has("bossbar_enabled") && profileJson.get("bossbar_enabled").getAsBoolean();
        boolean barHover = isOver(mouseX, mouseY, fx, dy, 12, 12);
        VanillaUIHelper.drawButton(g, fx, dy, 12, 12, barHover);
        if (barOn) {
            VanillaUIHelper.drawCheck(g, fx + 1, dy + 2, VanillaUIHelper.TEXT_GREEN);
        }
        g.drawString(font, tr("wh_npcs.ui.npc.combat_hpbar"), fx + 16, dy + 2, VanillaUIHelper.TEXT_GRAY, false);
        if (barOn) {
            String col = str("bossbar_color", "red");
            boolean colH = isOver(mouseX, mouseY, fx + 92, dy - 2, 16, 16);
            g.fill(fx + 92, dy - 2, fx + 108, dy + 14, barColorRgb(col));
            VanillaUIHelper.drawInsetFrame(g, fx + 92, dy - 2, 16, 16);
            if (colH) {
                tooltip = tr("wh_npcs.ui.npc.bar_color_tip");
            }
            int rad = profileJson.has("bossbar_radius") ? profileJson.get("bossbar_radius").getAsInt() : 32;
            boolean radH = isOver(mouseX, mouseY, fx + 114, dy - 2, 50, 16);
            VanillaUIHelper.drawButton(g, fx + 114, dy - 2, 50, 16, radH);
            g.drawCenteredString(font, "R: " + rad, fx + 139, dy + 2,
                    radH ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_AQUA);
        }
        if (barHover || isOver(mouseX, mouseY, fx + 16, dy, 76, 12)) {
            tooltip = tr("wh_npcs.ui.npc.combat_hpbar_tip");
        }

        boolean tRender = profileJson.has("totem_render") && profileJson.get("totem_render").getAsBoolean();
        boolean tHover = isOver(mouseX, mouseY, fx + 230, dy, 12, 12);
        VanillaUIHelper.drawButton(g, fx + 230, dy, 12, 12, tHover);
        if (tRender) {
            VanillaUIHelper.drawCheck(g, fx + 231, dy + 2, VanillaUIHelper.TEXT_GREEN);
        }
        g.drawString(font, tr("wh_npcs.ui.behavior.totem_render"), fx + 246, dy + 2, VanillaUIHelper.TEXT_GRAY, false);
        g.drawString(font, tr("wh_npcs.ui.npc.combat_charges"), fx + 330, dy + 2, VanillaUIHelper.TEXT_GRAY, false);
        if (tHover || isOver(mouseX, mouseY, fx + 246, dy, 62, 12)) {
            tooltip = tr("wh_npcs.ui.behavior.tip.totem");
        }

        if (npc != null) {
            int ay = sy + 54;
            boolean healHover = isOver(mouseX, mouseY, fx, ay, 100, 18);
            VanillaUIHelper.drawButton(g, fx, ay, 100, 18, healHover);
            g.drawCenteredString(font, tr("wh_npcs.ui.combat_preset.heal"), fx + 50, ay + 5,
                    healHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_GREEN);
            boolean clrHover = isOver(mouseX, mouseY, fx + 108, ay, 130, 18);
            VanillaUIHelper.drawButton(g, fx + 108, ay, 130, 18, clrHover);
            g.drawCenteredString(font, tr("wh_npcs.ui.combat_preset.clear_effects"), fx + 173, ay + 5,
                    clrHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_WHITE);
        }

        if (tooltip != null && confirmTitle == null) {
            multilineTooltip(g, tooltip, mouseX, mouseY);
        }
    }

    private static void drawSwordIcon(GuiGraphics g, int x, int y, int c) {
        g.fill(x + 5, y, x + 7, y + 2, c);
        g.fill(x + 4, y + 1, x + 6, y + 3, c);
        g.fill(x + 3, y + 2, x + 5, y + 4, c);
        g.fill(x + 2, y + 3, x + 4, y + 5, c);
        g.fill(x + 1, y + 5, x + 3, y + 6, c);
        g.fill(x, y + 6, x + 2, y + 8, c);
    }

    private static void drawChestplateIcon(GuiGraphics g, int x, int y, int c) {
        g.fill(x, y, x + 2, y + 3, c);
        g.fill(x + 6, y, x + 8, y + 3, c);
        g.fill(x + 2, y + 1, x + 6, y + 2, c);
        g.fill(x + 1, y + 3, x + 7, y + 8, c);
    }

    private static void drawKnockbackIcon(GuiGraphics g, int x, int y, int c) {
        g.fill(x, y + 3, x + 2, y + 5, c);
        g.fill(x + 3, y + 3, x + 6, y + 5, c);
        g.fill(x + 5, y + 1, x + 7, y + 3, c);
        g.fill(x + 5, y + 5, x + 7, y + 7, c);
    }

    private static void drawBootIcon(GuiGraphics g, int x, int y, int c) {
        g.fill(x + 1, y, x + 4, y + 5, c);
        g.fill(x + 1, y + 5, x + 7, y + 8, c);
    }

    private static void drawXpOrb(GuiGraphics g, int x, int y, int c) {
        g.fill(x + 3, y, x + 6, y + 1, c);
        g.fill(x + 1, y + 1, x + 8, y + 2, c);
        g.fill(x, y + 2, x + 9, y + 5, c);
        g.fill(x + 1, y + 5, x + 8, y + 6, c);
        g.fill(x + 3, y + 6, x + 6, y + 7, c);
        g.fill(x + 2, y + 2, x + 4, y + 4, 0xFFFFFFFF);
    }

    private static int presetIndex(String preset) {
        for (int i = 0; i < PRESET_IDS.length; i++) {
            if (PRESET_IDS[i].equals(preset)) {
                return i;
            }
        }
        return 0;
    }

    private void startChanceEdit(int slot) {
        commitChanceEdit();
        chanceEditSlot = slot;
        chanceEditBox = addRenderableWidget(new com.withouthonor.npcs.client.gui.SelectableEditBox(
                font, dropSlotX(slot) - 7, dropSlotY() + 16, 32, 12, Component.translatable("wh_npcs.ui.npc.drop_chance")));
        chanceEditBox.setMaxLength(3);
        chanceEditBox.setValue(String.valueOf(dropChance[slot]));
        setFocused(chanceEditBox);
    }

    private void commitChanceEdit() {
        if (chanceEditBox != null && chanceEditSlot >= 0) {
            try {
                dropChance[chanceEditSlot] = Math.max(1, Math.min(100,
                        Integer.parseInt(chanceEditBox.getValue().trim())));
                writeDropsToJson();
            } catch (NumberFormatException ignored) {
            }
        }
        cancelChanceEdit();
    }

    private void cancelChanceEdit() {
        if (chanceEditBox != null) {
            removeWidget(chanceEditBox);
        }
        chanceEditBox = null;
        chanceEditSlot = -1;
    }

    private static final String[] BAR_COLORS = {"pink", "blue", "red", "green", "yellow", "purple", "white"};

    private static int barColorRgb(String c) {
        return switch (c) {
            case "yellow" -> 0xFFFFE555;
            case "green" -> 0xFF55FF55;
            case "blue" -> 0xFF5599FF;
            case "purple" -> 0xFFAA55FF;
            case "pink" -> 0xFFFF6EC7;
            case "white" -> 0xFFFFFFFF;
            default -> 0xFFFF5555;
        };
    }

    private boolean handleCombatClick(double mouseX, double mouseY, int button) {
        recalc();
        if (button != 0) {
            return false;
        }
        int fx = contentX + 8;
        int rx = combatRightX();
        boolean ef = com.withouthonor.npcs.compat.Compat.epicFightLoaded();

        if (isOver(mouseX, mouseY, fx + 52, contentY + 102, 158, 18)) {
            if (minecraft != null) {
                minecraft.setScreen(new CombatPresetScreen(this, profileJson));
            }
            return true;
        }
        if (isOver(mouseX, mouseY, fx + 52, contentY + 124, 158, 18)) {
            String mt = str("mob_type", "undefined");
            int idx = 0;
            for (int i = 0; i < MOBTYPE_IDS.length; i++) {
                if (MOBTYPE_IDS[i].equals(mt)) {
                    idx = i;
                }
            }
            String next = MOBTYPE_IDS[(idx + 1) % MOBTYPE_IDS.length];
            if ("undefined".equals(next)) {
                profileJson.remove("mob_type");
            } else {
                profileJson.addProperty("mob_type", next);
            }
            return true;
        }
        int chkY = contentY + 148;
        if (ef) {
            if (isOver(mouseX, mouseY, fx + 52, contentY + 146, 158, 18)) {
                if ("epicfight".equals(str("combat_system", "vanilla"))) {
                    profileJson.remove("combat_system");
                } else {
                    profileJson.addProperty("combat_system", "epicfight");
                }
                return true;
            }
            chkY = contentY + 170;
        }
        if (isOver(mouseX, mouseY, fx, chkY, 12, 12)) {
            if (profileJson.has("attackable") && profileJson.get("attackable").getAsBoolean()) {
                profileJson.remove("attackable");
            } else {
                profileJson.addProperty("attackable", true);
            }
            return true;
        }
        if (isOver(mouseX, mouseY, fx + 104, chkY, 12, 12)) {
            if (profileJson.has("leap_at_target") && profileJson.get("leap_at_target").getAsBoolean()) {
                profileJson.remove("leap_at_target");
            } else {
                profileJson.addProperty("leap_at_target", true);
            }
            return true;
        }

        if (isOver(mouseX, mouseY, rx, contentY + 18, 200, 18)) {
            if (minecraft != null) {
                minecraft.setScreen(new PotionBeltScreen(this, profileJson));
            }
            return true;
        }
        if (isOver(mouseX, mouseY, rx, contentY + 40, 200, 18)) {
            if (minecraft != null) {
                minecraft.setScreen(new CreatureNatureScreen(this, profileJson));
            }
            return true;
        }
        if (isOver(mouseX, mouseY, rx, contentY + 62, 200, 18)) {
            if (minecraft != null) {
                minecraft.setScreen(new ResistancesScreen(this, profileJson));
            }
            return true;
        }

        if (isOver(mouseX, mouseY, rx + 52, contentY + 102, 130, 18)) {
            String death = profileJson.has("death_behavior")
                    ? profileJson.get("death_behavior").getAsString() : "respawn";
            profileJson.addProperty("death_behavior", "vanish".equals(death) ? "respawn" : "vanish");
            return true;
        }
        boolean respawn = !"vanish".equals(profileJson.has("death_behavior")
                ? profileJson.get("death_behavior").getAsString() : "respawn");
        if (respawn && isOver(mouseX, mouseY, rx, contentY + 124, 200, 18)) {
            if (minecraft != null) {
                minecraft.setScreen(new RespawnScreen(this, profileJson));
            }
            return true;
        }

        int dy = contentY + 208;
        if (isOver(mouseX, mouseY, fx, dy, 12, 12)) {
            if (profileJson.has("bossbar_enabled") && profileJson.get("bossbar_enabled").getAsBoolean()) {
                profileJson.remove("bossbar_enabled");
            } else {
                profileJson.addProperty("bossbar_enabled", true);
            }
            return true;
        }
        boolean barOn = profileJson.has("bossbar_enabled") && profileJson.get("bossbar_enabled").getAsBoolean();
        if (barOn && isOver(mouseX, mouseY, fx + 92, dy - 2, 16, 16)) {
            bossColorPicker = true;
            return true;
        }
        if (barOn && isOver(mouseX, mouseY, fx + 114, dy - 2, 50, 16)) {
            int rad = profileJson.has("bossbar_radius") ? profileJson.get("bossbar_radius").getAsInt() : 32;
            int[] opts = {16, 32, 48, 64};
            int idx = 0;
            for (int i = 0; i < opts.length; i++) {
                if (opts[i] == rad) {
                    idx = i;
                }
            }
            profileJson.addProperty("bossbar_radius", opts[(idx + 1) % opts.length]);
            return true;
        }
        if (isOver(mouseX, mouseY, fx + 230, dy, 12, 12)) {
            if (profileJson.has("totem_render") && profileJson.get("totem_render").getAsBoolean()) {
                profileJson.remove("totem_render");
            } else {
                profileJson.addProperty("totem_render", true);
            }
            return true;
        }
        if (npc != null) {
            int ay = contentY + 240;
            if (isOver(mouseX, mouseY, fx, ay, 100, 18)) {
                com.withouthonor.npcs.network.NetworkHandler.sendToServer(
                        new com.withouthonor.npcs.network.NpcAdminActionPacket(
                                npc.getId(), com.withouthonor.npcs.network.NpcAdminActionPacket.HEAL));
                return true;
            }
            if (isOver(mouseX, mouseY, fx + 108, ay, 130, 18)) {
                com.withouthonor.npcs.network.NetworkHandler.sendToServer(
                        new com.withouthonor.npcs.network.NpcAdminActionPacket(
                                npc.getId(), com.withouthonor.npcs.network.NpcAdminActionPacket.CLEAR_EFFECTS));
                return true;
            }
        }
        return false;
    }

    private void putCarriedToDrop(int slot, boolean consume) {
        dropItem[slot] = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(carried.getItem());
        dropNbt[slot] = carried.getTag() != null ? carried.getTag().copy() : null;
        dropCount[slot] = Math.max(1, Math.min(64, carried.getCount()));
        if (consume) {
            carried = net.minecraft.world.item.ItemStack.EMPTY;
        }
    }

    private void depositOneToDrop(int slot) {
        net.minecraft.resources.ResourceLocation id =
                net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(carried.getItem());
        net.minecraft.nbt.CompoundTag tag = carried.getTag();
        if (dropItem[slot] == null) {
            dropItem[slot] = id;
            dropNbt[slot] = tag != null ? tag.copy() : null;
            dropCount[slot] = 1;
        } else if (id != null && id.equals(dropItem[slot])
                && java.util.Objects.equals(dropNbt[slot], tag)) {
            if (dropCount[slot] >= 64) {
                return;
            }
            dropCount[slot]++;
        } else {
            return;
        }
        carried.shrink(1);
        if (carried.isEmpty()) {
            carried = net.minecraft.world.item.ItemStack.EMPTY;
            carriedInvSlot = -1;
        }
    }

    private String renderDropGrid(GuiGraphics g, int mouseX, int mouseY) {
        String tooltip = null;
        g.drawString(font, tr("wh_npcs.ui.npc.drops_label"), contentX + 8, dropSlotY() - 11, VanillaUIHelper.TEXT_GRAY, false);
        for (int i = 0; i < 9; i++) {
            int x = dropSlotX(i);
            int sy = dropSlotY();
            boolean slotHover = isOver(mouseX, mouseY, x, sy, 18, 18);
            VanillaUIHelper.drawItemSlot(g, x, sy, slotHover);
            var stack = dropStack(i);
            if (!stack.isEmpty()) {
                g.renderItem(stack, x + 1, sy + 1);
                g.renderItemDecorations(font, stack, x + 1, sy + 1);
                if (i != chanceEditSlot) {
                    String pct = dropChance[i] + "%";
                    g.pose().pushPose();
                    g.pose().translate(x + 9 - font.width(pct) * 0.35F, sy + 20, 0);
                    g.pose().scale(0.7F, 0.7F, 1.0F);
                    boolean pctHover = isOver(mouseX, mouseY, x, sy + 19, 18, 8);
                    g.drawString(font, pct, 0, 0,
                            pctHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_GOLD, false);
                    g.pose().popPose();
                    if (pctHover) {
                        tooltip = tr("wh_npcs.ui.npc.drop_chance_tip");
                    }
                }
                if (slotHover && carried.isEmpty()) {
                    g.renderTooltip(font, stack, mouseX, mouseY);
                }
            }
        }
        g.drawString(font, tr("wh_npcs.ui.npc.drops_hint"),
                contentX + 8, dropSlotY() + 30, VanillaUIHelper.TEXT_WHITE, false);
        return tooltip;
    }

    private boolean handleDropGridClick(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (int i = 0; i < 9; i++) {
                if (dropItem[i] != null && isOver(mouseX, mouseY, dropSlotX(i), dropSlotY() + 19, 18, 9)) {
                    startChanceEdit(i);
                    return true;
                }
            }
        }
        for (int i = 0; i < 9; i++) {
            if (isOver(mouseX, mouseY, dropSlotX(i), dropSlotY(), 18, 18)) {
                if (button == 1) {
                    if (carried.isEmpty()) {
                        dropItem[i] = null;
                        dropNbt[i] = null;
                        dropCount[i] = 1;
                    } else {
                        depositOneToDrop(i);
                    }
                    writeDropsToJson();
                } else if (button == 0) {
                    net.minecraft.world.item.ItemStack old = dropStack(i);
                    boolean had = dropItem[i] != null;
                    if (!carried.isEmpty()) {
                        putCarriedToDrop(i, true);
                    } else if (had) {
                        dropItem[i] = null;
                        dropNbt[i] = null;
                        dropCount[i] = 1;
                    }
                    carried = had ? old : net.minecraft.world.item.ItemStack.EMPTY;
                    carriedFromNpc = had;
                    carriedInvSlot = -1;
                    writeDropsToJson();
                }
                return true;
            }
        }
        return false;
    }

    private int facEdX() {
        return contentX + FAC_LIST_W + 10;
    }

    private int facEdRight() {
        return contentX + contentW - 2;
    }

    private int facTierRowY(int t) {
        return contentY + 114 + t * 22;
    }

    private void buildFactionWidgets() {
        tierNameBoxes.clear();
        tierMinBoxes.clear();
        String oldNew = newFactionBox != null ? newFactionBox.getValue() : "";
        newFactionBox = addRenderableWidget(new com.withouthonor.npcs.client.gui.SelectableEditBox(font,
                contentX + 4, contentY + contentH - 46, FAC_LIST_W - 6, 16, Component.translatable("wh_npcs.ui.npc.fac_name")));
        newFactionBox.setMaxLength(48);
        newFactionBox.setValue(oldNew);
        newFactionBox.setHint(Component.translatable("wh_npcs.ui.npc.fac_name_hint"));
        if (selectedFaction == null) {
            return;
        }
        int fx = facEdX();
        facNameBox = addRenderableWidget(new com.withouthonor.npcs.client.gui.SelectableEditBox(font,
                fx + 64, contentY + 2, 114, 18, Component.translatable("wh_npcs.ui.npc.fac_label_name")));
        facNameBox.setMaxLength(48);
        facNameBox.setValue(draftFacName);
        facNameBox.setResponder(v -> {
            draftFacName = v;
            facDirty = true;
        });
        facPenaltyBox = addRenderableWidget(new com.withouthonor.npcs.client.gui.SelectableEditBox(font,
                fx + 64, contentY + 26, 38, 18, Component.translatable("wh_npcs.ui.npc.fac_penalty")));
        facPenaltyBox.setMaxLength(4);
        facPenaltyBox.setValue(draftFacPenalty);
        facPenaltyBox.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                Component.translatable("wh_npcs.ui.npc.fac_penalty_tip")));
        facPenaltyBox.setResponder(v -> {
            draftFacPenalty = v;
            facDirty = true;
        });

        for (int t = 0; t < tierNames.size(); t++) {
            final int tier = t;
            int y = facTierRowY(t);
            EditBox name = addRenderableWidget(new com.withouthonor.npcs.client.gui.SelectableEditBox(font,
                    fx + 20, y, 104, 18, Component.translatable("wh_npcs.ui.npc.fac_tier")));
            name.setMaxLength(24);
            name.setValue(tierNames.get(t));
            name.setResponder(v -> {
                tierNames.set(tier, v);
                facDirty = true;
            });
            tierNameBoxes.add(name);
            EditBox min = addRenderableWidget(new com.withouthonor.npcs.client.gui.SelectableEditBox(font,
                    fx + 132, y, 46, 18, Component.translatable("wh_npcs.ui.npc.fac_threshold")));
            min.setMaxLength(6);
            min.setValue(t == 0 ? "" : String.valueOf(tierMins.get(t)));
            min.visible = t > 0;
            min.setResponder(v -> {
                try {
                    tierMins.set(tier, Integer.parseInt(v.trim()));
                    facDirty = true;
                } catch (NumberFormatException ignored) {
                }
            });
            tierMinBoxes.add(min);
            EditBox price = addRenderableWidget(new com.withouthonor.npcs.client.gui.SelectableEditBox(font,
                    fx + 194, y, 40, 18, Component.translatable("wh_npcs.ui.npc.fac_price")));
            price.setMaxLength(4);
            price.setValue(String.valueOf(tierPrices.get(t)));
            price.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                    Component.translatable("wh_npcs.ui.npc.fac_price_tip")));
            price.setResponder(v -> {
                try {
                    tierPrices.set(tier, Math.max(0.0F, Float.parseFloat(v.trim().replace(',', '.'))));
                    facDirty = true;
                } catch (NumberFormatException ignored) {
                }
            });
        }
    }

    private void loadFactionDraft(com.withouthonor.npcs.common.reputation.Faction faction) {
        selectedFaction = faction.getId();
        draftFacName = faction.getName();
        draftFacPenalty = String.valueOf(faction.getKillPenalty());
        draftHostile.clear();
        draftHostile.addAll(faction.getHostileTo());
        facColor = faction.getColor();
        tierNames.clear();
        tierMins.clear();
        tierColors.clear();
        tierPrices.clear();
        for (var tier : faction.getTiers()) {
            tierNames.add(tier.name());
            tierMins.add(tier.min());
            tierColors.add(tier.color());
            tierPrices.add(tier.priceMult());
        }
        facDirty = false;
        init(minecraft, width, height);
    }

    private com.withouthonor.npcs.common.reputation.Faction buildFactionFromDraft() {
        List<com.withouthonor.npcs.common.reputation.Faction.Tier> tiers = new ArrayList<>();
        for (int t = 0; t < tierNames.size(); t++) {
            String name = tierNames.get(t).isBlank() ? tr("wh_npcs.ui.npc.fac_tier_n", (t + 1)) : tierNames.get(t).trim();
            tiers.add(new com.withouthonor.npcs.common.reputation.Faction.Tier(
                    name, t == 0 ? Integer.MIN_VALUE : tierMins.get(t), tierColors.get(t), tierPrices.get(t)));
        }
        int penalty = 0;
        try {
            penalty = Math.max(0, Integer.parseInt(draftFacPenalty.trim()));
        } catch (NumberFormatException ignored) {
        }
        List<String> hostileTo = new ArrayList<>();
        for (String id : draftHostile) {
            if (!id.isEmpty() && !id.equals(selectedFaction)) {
                hostileTo.add(id);
            }
        }
        return new com.withouthonor.npcs.common.reputation.Faction(selectedFaction,
                draftFacName.isBlank() ? selectedFaction : draftFacName.trim(), facColor, penalty, tiers,
                hostileTo);
    }

    private void flushFaction() {
        if (facDirty && selectedFaction != null) {
            NetworkHandler.sendToServer(new com.withouthonor.npcs.network.FactionPackets.Save(
                    buildFactionFromDraft().toJson()));
            facDirty = false;
        }
    }

    private static final int[] PALETTE = buildPalette();

    private static int[] buildPalette() {
        List<Integer> colors = new ArrayList<>();
        for (ChatFormatting formatting : ChatFormatting.values()) {
            if (formatting.isColor() && formatting.getColor() != null) {
                colors.add(0xFF000000 | formatting.getColor());
            }
        }
        int[] result = new int[colors.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = colors.get(i);
        }
        return result;
    }

    private static final String CYR = "абвгдеёжзийклмнопрстуфхцчшщъыьэюя";
    private static final String[] LAT = {"a", "b", "v", "g", "d", "e", "e", "zh", "z", "i", "y", "k", "l",
            "m", "n", "o", "p", "r", "s", "t", "u", "f", "h", "c", "ch", "sh", "sch", "", "y", "", "e", "yu", "ya"};

    static String slugify(String input) {
        StringBuilder sb = new StringBuilder();
        for (char ch : input.toLowerCase(java.util.Locale.ROOT).toCharArray()) {
            if ((ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9')) {
                sb.append(ch);
            } else if (ch == ' ' || ch == '-' || ch == '_') {
                if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '_') {
                    sb.append('_');
                }
            } else {
                int idx = CYR.indexOf(ch);
                if (idx >= 0) {
                    sb.append(LAT[idx]);
                }
            }
        }
        while (sb.length() > 0 && sb.charAt(sb.length() - 1) == '_') {
            sb.setLength(sb.length() - 1);
        }
        return sb.length() > 28 ? sb.substring(0, 28) : sb.toString();
    }

    private boolean factionExists(String id) {
        for (var entry : com.withouthonor.npcs.client.ClientFactions.full()) {
            if (entry.faction().getId().equals(id)) {
                return true;
            }
        }
        return false;
    }

    private List<com.withouthonor.npcs.client.ClientFactions.Full> factionList() {
        var prefs = com.withouthonor.npcs.client.ClientPrefs.get();
        List<com.withouthonor.npcs.client.ClientFactions.Full> pinned = new ArrayList<>();
        List<com.withouthonor.npcs.client.ClientFactions.Full> rest = new ArrayList<>();
        for (var entry : com.withouthonor.npcs.client.ClientFactions.full()) {
            (prefs.isPinnedFaction(entry.faction().getId()) ? pinned : rest).add(entry);
        }
        java.util.Comparator<com.withouthonor.npcs.client.ClientFactions.Full> byName =
                java.util.Comparator.comparing(e -> e.faction().getName(), String.CASE_INSENSITIVE_ORDER);
        pinned.sort(byName);
        rest.sort(byName);
        pinned.addAll(rest);
        return pinned;
    }

    private String str(String key, String def) {
        return profileJson.has(key) && !profileJson.get(key).isJsonNull()
                ? profileJson.get(key).getAsString() : def;
    }

    @Override
    protected int designW() {
        return 486;
    }

    @Override
    protected int designH() {
        return 360;
    }

    private void recalc() {
        winW = 486;
        winH = 360;
        winX = (width - winW) / 2;
        winY = (height - winH) / 2;
        contentX = winX + PAD;
        contentY = winY + HEADER_H + TAB_H + 8;
        contentW = winW - PAD * 2;
        contentH = winY + winH - PAD - 26 - contentY;
    }

    @Override
    protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        recalc();
        scrollbars.beginFrame();

        boolean modalOpen = confirmTitle != null || palettePicker || bossColorPicker || factionPicker || repPopup
                || hostilePicker;
        if (modalOpen) {
            mouseX = -10000;
            mouseY = -10000;
        }
        VanillaUIHelper.drawWindow(g, winX, winY, winW, winH);

        g.fill(winX + 2, winY + 2, winX + winW - 2, winY + HEADER_H - 2, VanillaUIHelper.BG_HEADER);
        drawNameGlow(g, winX, winY, winW, winH, nameGlowColor());
        String previewName = nameBox != null && !nameBox.getValue().isBlank()
                ? nameBox.getValue() : str("name", "NPC");
        g.drawString(font, previewName, winX + PAD, winY + 8, VanillaUIHelper.TEXT_YELLOW, false);
        String shortId = str("id", "????????").substring(0, 8);
        g.drawString(font, "id: " + shortId, winX + winW - PAD - font.width("id: " + shortId), winY + 8,
                VanillaUIHelper.TEXT_DARK_GRAY, false);

        int tabX = winX + PAD;
        int tabGap = tabGap();
        for (int i = 0; i < TAB_KEYS.length; i++) {
            String tabLabel = tr(TAB_KEYS[i]);
            int tw = font.width(tabLabel) + 16;
            boolean hovered = isOver(mouseX, mouseY, tabX, winY + HEADER_H, tw, TAB_H);
            VanillaUIHelper.drawTab(g, tabX, winY + HEADER_H, tw, TAB_H, activeTab == i, hovered);
            g.drawString(font, tabLabel, tabX + 8, winY + HEADER_H + 6,
                    activeTab == i ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_WHITE, false);
            tabX += tw + tabGap;
        }

        VanillaUIHelper.drawContentPanel(g, contentX, contentY - 4, contentW, contentH);

        if (activeTab == 0) {
            g.drawString(font, tr("wh_npcs.ui.npc.field_name"), contentX + 8, contentY + 17, VanillaUIHelper.TEXT_GRAY, false);
            g.drawString(font, tr("wh_npcs.ui.npc.field_title"), contentX + 8, contentY + 45, VanillaUIHelper.TEXT_GRAY, false);

            int fieldX = contentX + 110;
            int fieldW = Math.min(220, contentW - 120);
            boolean showTitle = profileJson.has("show_title") && profileJson.get("show_title").getAsBoolean();
            int eyeX = fieldX + fieldW - 18;
            boolean eyeHover = isOver(mouseX, mouseY, eyeX, contentY + 40, 18, 18);
            VanillaUIHelper.drawButton(g, eyeX, contentY + 40, 18, 18, eyeHover);
            drawEye(g, eyeX + 4, contentY + 46, showTitle ? VanillaUIHelper.TEXT_YELLOW : 0xFF707070, !showTitle);
            if (eyeHover) {
                multilineTooltip(g, tr("wh_npcs.ui.npc.title_eye_tip_head") + "\n"
                        + (showTitle ? tr("wh_npcs.ui.npc.title_eye_on")
                                     : tr("wh_npcs.ui.npc.title_eye_off"))
                        + "\n\n" + tr("wh_npcs.ui.npc.title_eye_tip_color"), mouseX, mouseY);
            }
            g.drawString(font, tr("wh_npcs.ui.npc.field_skin"), contentX + 8, contentY + 73, VanillaUIHelper.TEXT_GRAY, false);
            String skinSpec = str("skin_player_name", "");
            drawSkinHead(g, skinSpec, contentX + 110, contentY + 70);
            g.drawString(font, font.plainSubstrByWidth(
                            skinSpec.isEmpty() ? tr("wh_npcs.ui.npc.skin_default") : "§b" + shortSkinLabel(skinSpec),
                            contentW - 212),
                    contentX + 126, contentY + 73, VanillaUIHelper.TEXT_WHITE, false);
            boolean skinHover = isOver(mouseX, mouseY, disguiseBtnX(), contentY + 68, 80, 18);
            VanillaUIHelper.drawButton(g, disguiseBtnX(), contentY + 68, 80, 18, skinHover);
            g.drawCenteredString(font, tr("wh_npcs.ui.npc.btn_library"), disguiseBtnX() + 40, contentY + 73,
                    skinHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_WHITE);
            g.drawString(font, tr("wh_npcs.ui.npc.field_voice"), contentX + 8, contentY + 101, VanillaUIHelper.TEXT_GRAY, false);
            String voice = str("voice_sound", "");
            if (voice.isEmpty()) {
                g.drawString(font, tr("wh_npcs.ui.npc.voice_none"), contentX + 110, contentY + 101, VanillaUIHelper.TEXT_WHITE, false);
            } else {
                drawNote(g, contentX + 111, contentY + 99, VanillaUIHelper.TEXT_AQUA);
                String vname = voice.contains(":") ? voice.substring(voice.indexOf(':') + 1) : voice;
                g.drawString(font, font.plainSubstrByWidth("§b" + vname, contentW - 212),
                        contentX + 122, contentY + 101, VanillaUIHelper.TEXT_WHITE, false);
            }
            boolean voiceHover = isOver(mouseX, mouseY, disguiseBtnX(), contentY + 96, 80, 18);
            VanillaUIHelper.drawButton(g, disguiseBtnX(), contentY + 96, 80, 18, voiceHover);
            g.drawCenteredString(font, tr("wh_npcs.ui.npc.btn_change"), disguiseBtnX() + 40, contentY + 101,
                    voiceHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_WHITE);
            if (voiceHover) {
                multilineTooltip(g, tr("wh_npcs.ui.npc.voice_tip"), mouseX, mouseY);
            }

            g.drawString(font, tr("wh_npcs.ui.npc.field_disguise"), contentX + 8, contentY + 129, VanillaUIHelper.TEXT_GRAY, false);
            String disguise = str("disguise", "");
            g.drawString(font, font.plainSubstrByWidth(
                            disguise.isEmpty() ? tr("wh_npcs.ui.npc.disguise_none") : "§b" + disguise, contentW - 196),
                    contentX + 110, contentY + 129, VanillaUIHelper.TEXT_WHITE, false);
            boolean pickHover = isOver(mouseX, mouseY, disguiseBtnX(), contentY + 124, 80, 18);
            VanillaUIHelper.drawButton(g, disguiseBtnX(), contentY + 124, 80, 18, pickHover);
            g.drawCenteredString(font, tr("wh_npcs.ui.npc.btn_change"), disguiseBtnX() + 40, contentY + 129,
                    pickHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_WHITE);
            if (pickHover) {
                multilineTooltip(g, tr("wh_npcs.ui.npc.disguise_tip"), mouseX, mouseY);
            }

            g.drawString(font, tr("wh_npcs.ui.npc.field_faction"), contentX + 8, contentY + 157, VanillaUIHelper.TEXT_GRAY, false);
            String factionId = str("faction", "");
            var factionInfo = factionId.isEmpty() ? null
                    : com.withouthonor.npcs.client.ClientFactions.byId(factionId);
            String factionLabel = factionId.isEmpty() ? tr("wh_npcs.ui.npc.faction_none")
                    : factionInfo != null ? factionInfo.name() : "§c" + tr("wh_npcs.ui.npc.faction_missing", factionId);
            g.drawString(font, font.plainSubstrByWidth(factionLabel, contentW - 196),
                    contentX + 110, contentY + 157,
                    factionInfo != null ? factionInfo.color() : VanillaUIHelper.TEXT_WHITE, false);
            boolean facHover = isOver(mouseX, mouseY, disguiseBtnX(), contentY + 152, 80, 18);
            VanillaUIHelper.drawButton(g, disguiseBtnX(), contentY + 152, 80, 18, facHover);
            g.drawCenteredString(font, tr("wh_npcs.ui.npc.btn_switch"), disguiseBtnX() + 40, contentY + 157,
                    facHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_WHITE);
            if (facHover) {
                multilineTooltip(g, tr("wh_npcs.ui.npc.faction_tip"), mouseX, mouseY);
            }

            g.drawString(font, tr("wh_npcs.ui.npc.field_trade"), contentX + 8, contentY + 185, VanillaUIHelper.TEXT_GRAY, false);
            int offerCount = profileJson.has("offers") ? profileJson.getAsJsonArray("offers").size() : 0;
            g.drawString(font, offerCount > 0 ? "§b" + tr("wh_npcs.ui.npc.trade_offers", offerCount) : tr("wh_npcs.ui.npc.value_none"),
                    contentX + 110, contentY + 185, VanillaUIHelper.TEXT_WHITE, false);
            boolean tradeHover = isOver(mouseX, mouseY, disguiseBtnX(), contentY + 180, 80, 18);
            VanillaUIHelper.drawButton(g, disguiseBtnX(), contentY + 180, 80, 18, tradeHover);
            g.drawCenteredString(font, tr("wh_npcs.ui.npc.btn_offers"), disguiseBtnX() + 40, contentY + 185,
                    tradeHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_WHITE);
            if (tradeHover) {
                multilineTooltip(g, tr("wh_npcs.ui.npc.trade_tip"), mouseX, mouseY);
            }

            g.drawString(font, tr("wh_npcs.ui.npc.field_lines"), contentX + 8, contentY + 213, VanillaUIHelper.TEXT_GRAY, false);
            int phraseCount = 0;
            for (String pk : new String[]{"ambient_phrases", "combat_phrases", "interact_phrases",
                    "death_phrases", "kill_phrases"}) {
                if (profileJson.has(pk) && profileJson.get(pk).isJsonArray()) {
                    phraseCount += profileJson.getAsJsonArray(pk).size();
                }
            }
            boolean bubblesOff = profileJson.has("bubbles_enabled")
                    && !profileJson.get("bubbles_enabled").getAsBoolean();
            boolean toChat = profileJson.has("bubbles_to_chat")
                    && profileJson.get("bubbles_to_chat").getAsBoolean();
            boolean muted = bubblesOff && !toChat;
            g.drawString(font, muted ? tr("wh_npcs.ui.npc.lines_off")
                            : (phraseCount > 0 ? "§b" + tr("wh_npcs.ui.npc.lines_count", phraseCount) : tr("wh_npcs.ui.npc.value_none")),
                    contentX + 110, contentY + 213, VanillaUIHelper.TEXT_WHITE, false);
            boolean phrasesHover = isOver(mouseX, mouseY, disguiseBtnX(), contentY + 208, 80, 18);
            VanillaUIHelper.drawButton(g, disguiseBtnX(), contentY + 208, 80, 18, phrasesHover);
            g.drawCenteredString(font, tr("wh_npcs.ui.npc.btn_change"), disguiseBtnX() + 40, contentY + 213,
                    phrasesHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_WHITE);
            if (phrasesHover) {
                multilineTooltip(g, tr("wh_npcs.ui.npc.lines_tip"), mouseX, mouseY);
            }
            g.drawString(font, tr("wh_npcs.ui.npc.name_format_note"),
                    contentX + 8, contentY + 246, VanillaUIHelper.TEXT_WHITE, false);
        } else if (activeTab == 1) {
            renderDialoguesTab(g, mouseX, mouseY);
        } else if (activeTab == 2) {
            renderEntryPointsTab(g, mouseX, mouseY);
        } else if (activeTab == 3) {
            renderEquipmentTab(g, mouseX, mouseY);
        } else if (activeTab == 4) {
            renderFactionsTab(g, mouseX, mouseY);
        } else if (activeTab == 5) {
            renderCombatTab(g, mouseX, mouseY);
        } else if (activeTab == 6) {
            renderBehaviorTab(g, mouseX, mouseY);
        } else {
            renderPoseTab(g, mouseX, mouseY);
        }

        boolean saveHover = isOver(mouseX, mouseY, saveX(), buttonsY(), 90, 20);
        boolean cancelHover = isOver(mouseX, mouseY, saveX() + 96, buttonsY(), 90, 20);
        VanillaUIHelper.drawButton(g, saveX(), buttonsY(), 90, 20, saveHover);
        VanillaUIHelper.drawButton(g, saveX() + 96, buttonsY(), 90, 20, cancelHover);
        g.drawCenteredString(font, tr("wh_npcs.ui.common.save"), saveX() + 45, buttonsY() + 6,
                saveHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_GREEN);
        g.drawCenteredString(font, tr("wh_npcs.ui.common.cancel"), saveX() + 96 + 45, buttonsY() + 6,
                cancelHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_WHITE);

        if (npc != null) {
            int delX = winX + PAD;
            int expX = winX + PAD + 24;
            int impX = winX + PAD + 48;
            int cliX = winX + PAD + 72;
            boolean mp = minecraft != null && !minecraft.hasSingleplayerServer();
            boolean delHover = isOver(mouseX, mouseY, delX, buttonsY(), 20, 20);
            boolean expHover = isOver(mouseX, mouseY, expX, buttonsY(), 20, 20);
            boolean impHover = isOver(mouseX, mouseY, impX, buttonsY(), 20, 20);
            boolean cliHover = mp && isOver(mouseX, mouseY, cliX, buttonsY(), 20, 20);
            VanillaUIHelper.drawButton(g, delX, buttonsY(), 20, 20, delHover);
            VanillaUIHelper.drawButton(g, expX, buttonsY(), 20, 20, expHover);
            VanillaUIHelper.drawButton(g, impX, buttonsY(), 20, 20, impHover);
            drawTrashIcon(g, delX + 6, buttonsY() + 5, delHover ? VanillaUIHelper.TEXT_YELLOW : 0xFFFF5555);
            drawExportIcon(g, expX + 6, buttonsY() + 5, expHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_AQUA);
            drawImportIcon(g, impX + 6, buttonsY() + 5, impHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_AQUA);
            if (mp) {
                VanillaUIHelper.drawButton(g, cliX, buttonsY(), 20, 20, cliHover);
                drawExportIcon(g, cliX + 6, buttonsY() + 5, cliHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_GREEN);
            }
            if (confirmTitle == null && delHover) {
                multilineTooltip(g, tr("wh_npcs.ui.npc.btn_delete_npc"), mouseX, mouseY);
            } else if (confirmTitle == null && expHover) {
                multilineTooltip(g, tr(mp ? "wh_npcs.ui.npc.export_server_tip" : "wh_npcs.ui.npc.export_tip"), mouseX, mouseY);
            } else if (confirmTitle == null && impHover) {
                multilineTooltip(g, tr("wh_npcs.ui.npc.import_tip"), mouseX, mouseY);
            } else if (confirmTitle == null && cliHover) {
                multilineTooltip(g, tr("wh_npcs.ui.npc.export_client_tip"), mouseX, mouseY);
            }
        }

        String themeLabel = switch (VanillaUIHelper.theme()) {
            case VANILLA -> tr("wh_npcs.ui.npc.theme_vanilla");
            case COFFEE -> tr("wh_npcs.ui.npc.theme_coffee");
            default -> tr("wh_npcs.ui.npc.theme_dark");
        };
        int themeX = saveX() - 104;
        boolean themeHover = isOver(mouseX, mouseY, themeX, buttonsY(), 96, 20);
        VanillaUIHelper.drawButton(g, themeX, buttonsY(), 96, 20, themeHover);
        g.drawCenteredString(font, themeLabel,
                themeX + 48, buttonsY() + 6,
                themeHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_AQUA);
        if (confirmTitle == null && themeHover) {
            multilineTooltip(g, tr("wh_npcs.ui.npc.theme_tip"), mouseX, mouseY);
        }
    }

    @Override
    protected void renderOverlay(GuiGraphics g, int mouseX, int mouseY, float partialTick) {

        if (confirmTitle != null) {
            renderConfirmPopup(g, mouseX, mouseY);
        }
        if (palettePicker) {
            renderPalettePicker(g, mouseX, mouseY);
        }
        if (bossColorPicker) {
            renderBossColorPicker(g, mouseX, mouseY);
        }
        if (factionPicker) {
            renderFactionPicker(g, mouseX, mouseY);
        }
        if (repPopup) {
            renderRepPopup(g, mouseX, mouseY);
        }
        if (hostilePicker) {
            renderHostilePicker(g, mouseX, mouseY);
        }
    }

    private void renderFactionPicker(GuiGraphics g, int mouseX, int mouseY) {
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        g.fill(winX, winY, winX + winW, winY + winH, 0xA0000000);
        var list = factionList();
        int rows = 1 + list.size();
        int visible = Math.min(rows, 14);
        factionPickerScroll = Math.max(0, Math.min(factionPickerScroll, rows - visible));
        int bw = 200;
        int bh = 30 + visible * 13;
        int bx = (width - bw) / 2;
        int by = (height - bh) / 2;
        VanillaUIHelper.drawWindow(g, bx, by, bw, bh);
        g.drawCenteredString(font, tr("wh_npcs.ui.npc.faction_picker_title"), bx + bw / 2, by + 8, VanillaUIHelper.TEXT_YELLOW);
        String current = str("faction", "");
        int y = by + 22;
        for (int i = factionPickerScroll; i < Math.min(rows, factionPickerScroll + visible); i++) {
            boolean hovered = isOver(mouseX, mouseY, bx + 4, y, bw - 8, 13);
            boolean isCurrent = i == 0 ? current.isEmpty()
                    : list.get(i - 1).faction().getId().equals(current);
            if (hovered || isCurrent) {
                g.fill(bx + 4, y, bx + bw - 4, y + 13,
                        isCurrent ? VanillaUIHelper.BG_SELECTED : VanillaUIHelper.BG_HOVERED);
            }
            if (i == 0) {
                g.drawString(font, tr("wh_npcs.ui.npc.faction_none"), bx + 8, y + 2, VanillaUIHelper.TEXT_WHITE, false);
            } else {
                var faction = list.get(i - 1).faction();
                g.drawString(font, font.plainSubstrByWidth(faction.getName(), bw - 16),
                        bx + 8, y + 2, faction.getColor(), false);
            }
            y += 13;
        }
        if (rows > visible) {
            VanillaUIHelper.drawScrollbar(g, bx + bw - 8, by + 22, visible * 13, rows, visible,
                    factionPickerScroll, scrollbars, v -> factionPickerScroll = v);
        }
        g.pose().popPose();
    }

    @Nullable
    private com.withouthonor.npcs.common.reputation.Faction selectedFactionFull() {
        for (var entry : com.withouthonor.npcs.client.ClientFactions.full()) {
            if (entry.faction().getId().equals(selectedFaction)) {
                return entry.faction();
            }
        }
        return null;
    }

    private void renderRepPopup(GuiGraphics g, int mouseX, int mouseY) {
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        g.fill(winX, winY, winX + winW, winY + winH, 0xA0000000);
        int bx = (width - REP_W) / 2;
        int by = (height - REP_H) / 2;
        VanillaUIHelper.drawWindow(g, bx, by, REP_W, REP_H);
        var faction = selectedFactionFull();
        g.drawString(font, tr("wh_npcs.ui.npc.rep_title", (faction != null ? faction.getName() : selectedFaction)),
                bx + 8, by + 8, VanillaUIHelper.TEXT_YELLOW, false);
        String tooltip = null;
        if (repLoading) {
            g.drawCenteredString(font, tr("wh_npcs.ui.npc.loading"), bx + REP_W / 2, by + REP_H / 2, VanillaUIHelper.TEXT_STATUS);
        } else if (repEntries.isEmpty()) {
            g.drawCenteredString(font, tr("wh_npcs.ui.npc.rep_empty"),
                    bx + REP_W / 2, by + REP_H / 2, VanillaUIHelper.TEXT_WHITE);
        }

        int searchY = by + 22;
        boolean searchHover = isOver(mouseX, mouseY, bx + 8, searchY, REP_W - 16, 14);
        g.fill(bx + 8, searchY, bx + REP_W - 8, searchY + 14,
                searchHover ? VanillaUIHelper.BG_HOVERED : 0xFF000000);
        VanillaUIHelper.drawInsetFrame(g, bx + 8, searchY, REP_W - 16, 14);
        g.drawString(font, repSearch.isEmpty() ? tr("wh_npcs.ui.npc.rep_search_hint") : repSearch,
                bx + 12, searchY + 3, VanillaUIHelper.TEXT_WHITE, false);

        var shown = repFiltered();
        int visible = REP_VISIBLE - 1;
        int listTop = by + 40;
        repScroll = Math.max(0, Math.min(repScroll, Math.max(0, shown.size() - visible)));
        if (!repLoading && !repEntries.isEmpty() && shown.isEmpty()) {
            g.drawCenteredString(font, tr("wh_npcs.ui.npc.nothing_found"), bx + REP_W / 2, by + REP_H / 2, VanillaUIHelper.TEXT_WHITE);
        }
        int y = listTop;
        for (int i = repScroll; i < Math.min(shown.size(), repScroll + visible); i++) {
            var entry = shown.get(i);
            boolean rowHover = isOver(mouseX, mouseY, bx + 4, y, REP_W - 8, 16);
            if (rowHover) {
                g.fill(bx + 4, y, bx + REP_W - 4, y + 16, VanillaUIHelper.BG_HOVERED);
            }
            drawAuthorHead(g, entry.name(), bx + 8, y + 4);
            g.drawString(font, font.plainSubstrByWidth(entry.name(), 96), bx + 20, y + 4,
                    VanillaUIHelper.TEXT_WHITE, false);
            var tier = faction != null ? faction.tierFor(entry.value()) : null;
            String value = String.valueOf(entry.value());
            g.drawString(font, value, bx + 172 - font.width(value), y + 4,
                    tier != null ? tier.color() : VanillaUIHelper.TEXT_WHITE, false);
            if (tier != null && isOver(mouseX, mouseY, bx + 130, y + 2, 44, 12)) {
                tooltip = "§e" + tier.name();
            }

            boolean minusHover = isOver(mouseX, mouseY, bx + 182, y + 1, 14, 14);
            boolean plusHover = isOver(mouseX, mouseY, bx + 200, y + 1, 14, 14);
            boolean zeroHover = isOver(mouseX, mouseY, bx + 218, y + 1, 14, 14);
            VanillaUIHelper.drawButton(g, bx + 182, y + 1, 14, 14, minusHover);
            VanillaUIHelper.drawButton(g, bx + 200, y + 1, 14, 14, plusHover);
            VanillaUIHelper.drawButton(g, bx + 218, y + 1, 14, 14, zeroHover);
            g.drawCenteredString(font, "-", bx + 189, y + 4,
                    minusHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_RED);
            g.drawCenteredString(font, "+", bx + 207, y + 4,
                    plusHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_GREEN);
            g.drawCenteredString(font, "0", bx + 225, y + 4,
                    zeroHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_DARK_GRAY);
            if (zeroHover) {
                tooltip = tr("wh_npcs.ui.npc.rep_reset_zero");
            }
            y += 16;
        }
        VanillaUIHelper.drawScrollbar(g, bx + REP_W - 8, listTop, visible * 16,
                shown.size(), visible, repScroll, scrollbars, v -> repScroll = v);
        g.drawString(font, tr("wh_npcs.ui.npc.rep_footer"), bx + 8, by + REP_H - 21, VanillaUIHelper.TEXT_WHITE, false);
        boolean closeHover = isOver(mouseX, mouseY, bx + REP_W - 70, by + REP_H - 26, 62, 18);
        VanillaUIHelper.drawButton(g, bx + REP_W - 70, by + REP_H - 26, 62, 18, closeHover);
        g.drawCenteredString(font, tr("wh_npcs.ui.common.close"), bx + REP_W - 39, by + REP_H - 21,
                closeHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_WHITE);
        if (tooltip != null) {
            multilineTooltip(g, tooltip, mouseX, mouseY);
        }
        g.pose().popPose();
    }

    private boolean handleRepPopupClick(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return true;
        }
        recalc();
        int bx = (width - REP_W) / 2;
        int by = (height - REP_H) / 2;
        if (isOver(mouseX, mouseY, bx + REP_W - 70, by + REP_H - 26, 62, 18)) {
            repPopup = false;
            return true;
        }
        int step = hasShiftDown() ? 10 : 1;
        var shown = repFiltered();
        int visible = REP_VISIBLE - 1;
        int y = by + 40;
        for (int i = repScroll; i < Math.min(shown.size(), repScroll + visible); i++) {
            var entry = shown.get(i);
            Integer newValue = null;
            if (isOver(mouseX, mouseY, bx + 182, y + 1, 14, 14)) {
                newValue = entry.value() - step;
            } else if (isOver(mouseX, mouseY, bx + 200, y + 1, 14, 14)) {
                newValue = entry.value() + step;
            } else if (isOver(mouseX, mouseY, bx + 218, y + 1, 14, 14)) {
                newValue = 0;
            }
            if (newValue != null) {

                for (int k = 0; k < repEntries.size(); k++) {
                    if (repEntries.get(k).id().equals(entry.id())) {
                        repEntries.set(k, new com.withouthonor.npcs.network.FactionPackets.RepEntry(
                                entry.id(), entry.name(), newValue));
                        break;
                    }
                }
                NetworkHandler.sendToServer(new com.withouthonor.npcs.network.FactionPackets.RepSet(
                        selectedFaction, entry.id(), newValue));
                return true;
            }
            y += 16;
        }
        return true;
    }

    private void renderPalettePicker(GuiGraphics g, int mouseX, int mouseY) {
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        g.fill(winX, winY, winX + winW, winY + winH, 0xA0000000);
        int cols = 8;
        int step = 20;
        int bw = cols * step + 16;
        int bh = 2 * step + 34;
        int bx = (width - bw) / 2;
        int by = (height - bh) / 2;
        VanillaUIHelper.drawWindow(g, bx, by, bw, bh);
        g.drawCenteredString(font, paletteTarget < 0 ? tr("wh_npcs.ui.npc.fac_color_title") : tr("wh_npcs.ui.npc.tier_color_title"),
                bx + bw / 2, by + 8, VanillaUIHelper.TEXT_YELLOW);
        for (int i = 0; i < PALETTE.length; i++) {
            int x = bx + 8 + (i % cols) * step;
            int y = by + 22 + (i / cols) * step;
            boolean hovered = isOver(mouseX, mouseY, x, y, 16, 16);
            g.fill(x, y, x + 16, y + 16, PALETTE[i]);
            if (hovered) {
                VanillaUIHelper.drawRaisedFrame(g, x - 1, y - 1, 18, 18);
            } else {
                VanillaUIHelper.drawInsetFrame(g, x, y, 16, 16);
            }
        }
        g.pose().popPose();
    }

    private void renderBossColorPicker(GuiGraphics g, int mouseX, int mouseY) {
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        g.fill(winX, winY, winX + winW, winY + winH, 0xA0000000);
        int step = 20;
        int bw = BAR_COLORS.length * step + 16;
        int bh = step + 34;
        int bx = (width - bw) / 2;
        int by = (height - bh) / 2;
        VanillaUIHelper.drawWindow(g, bx, by, bw, bh);
        g.drawCenteredString(font, tr("wh_npcs.ui.npc.bar_color_title"), bx + bw / 2, by + 8, VanillaUIHelper.TEXT_YELLOW);
        for (int i = 0; i < BAR_COLORS.length; i++) {
            int x = bx + 8 + i * step;
            int y = by + 22;
            boolean hovered = isOver(mouseX, mouseY, x, y, 16, 16);
            g.fill(x, y, x + 16, y + 16, barColorRgb(BAR_COLORS[i]));
            if (hovered) {
                VanillaUIHelper.drawRaisedFrame(g, x - 1, y - 1, 18, 18);
            } else {
                VanillaUIHelper.drawInsetFrame(g, x, y, 16, 16);
            }
        }
        g.pose().popPose();
    }

    private int confirmDlgH() {
        return 86 + (confirmWarn.isEmpty() ? 0 : Math.min(confirmWarn.size(), 5) * 10 + 2);
    }

    private void clearConfirm() {
        confirmTitle = null;
        confirmAction = null;
        confirmOkKey = "wh_npcs.ui.common.delete";
        confirmIrreversible = true;
    }

    private void askExport(String titleKey, Runnable action) {
        confirmTitle = tr(titleKey);
        confirmValue = profileJson.has("name") ? profileJson.get("name").getAsString() : "";
        confirmWarn = List.of();
        confirmOkKey = "wh_npcs.ui.npc.export_do";
        confirmIrreversible = false;
        confirmAction = action;
    }

    private void renderConfirmPopup(GuiGraphics g, int mouseX, int mouseY) {

        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        g.fill(winX, winY, winX + winW, winY + winH, 0xA0000000);
        int w = 260;
        int h = confirmDlgH();
        int x = winX + (winW - w) / 2;
        int y = winY + (winH - h) / 2;
        VanillaUIHelper.drawWindow(g, x, y, w, h);
        g.drawCenteredString(font, confirmTitle, x + w / 2, y + 10, VanillaUIHelper.TEXT_YELLOW);
        g.drawCenteredString(font, "§b" + font.plainSubstrByWidth(confirmValue, w - 20),
                x + w / 2, y + 26, VanillaUIHelper.TEXT_WHITE);
        int line = y + 40;
        for (int i = 0; i < Math.min(confirmWarn.size(), 5); i++) {
            g.drawCenteredString(font, font.plainSubstrByWidth(confirmWarn.get(i), w - 24),
                    x + w / 2, line, VanillaUIHelper.TEXT_WHITE);
            line += 10;
        }
        if (confirmIrreversible) {
            g.drawCenteredString(font, tr("wh_npcs.ui.npc.irreversible"), x + w / 2, line + 2, VanillaUIHelper.TEXT_WHITE);
        }
        boolean delHover = isOver(mouseX, mouseY, x + w / 2 - 86, y + h - 28, 80, 18);
        boolean cancelHover = isOver(mouseX, mouseY, x + w / 2 + 6, y + h - 28, 80, 18);
        VanillaUIHelper.drawButton(g, x + w / 2 - 86, y + h - 28, 80, 18, delHover);
        VanillaUIHelper.drawButton(g, x + w / 2 + 6, y + h - 28, 80, 18, cancelHover);
        g.drawCenteredString(font, tr(confirmOkKey), x + w / 2 - 46, y + h - 23,
                delHover ? VanillaUIHelper.TEXT_YELLOW
                        : (confirmIrreversible ? 0xFFFF5555 : VanillaUIHelper.TEXT_GREEN));
        g.drawCenteredString(font, tr("wh_npcs.ui.common.cancel"), x + w / 2 + 46, y + h - 23,
                cancelHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_WHITE);
        g.pose().popPose();
    }

    private int funcColX() {
        return contentX + 64;
    }

    private int cosmColX() {
        return contentX + 92;
    }

    private int invGridX() {
        return contentX + 140;
    }

    private int invGridY() {
        return contentY + 26;
    }

    private int equipRowY(int row) {
        return contentY + 26 + row * 22;
    }

    private int dropSlotX(int i) {
        return contentX + 8 + i * 20;
    }

    private int dropSlotY() {
        return contentY + 210;
    }

    private net.minecraft.world.item.ItemStack equipStack(int idx) {
        return idx < 6 ? funcEquip[idx] : cosmEquip[idx - 6];
    }

    private void setEquipStack(int idx, net.minecraft.world.item.ItemStack stack) {
        if (idx < 6) {
            funcEquip[idx] = stack;
        } else {
            cosmEquip[idx - 6] = stack;
        }
    }

    private void renderEquipmentTab(GuiGraphics g, int mouseX, int mouseY) {
        if (npc == null) {
            g.drawString(font, tr("wh_npcs.ui.npc.items_unavailable"),
                    contentX + 8, contentY + 20, VanillaUIHelper.TEXT_STATUS, false);
            g.drawString(font, tr("wh_npcs.ui.npc.items_unavailable_hint"),
                    contentX + 8, contentY + 34, VanillaUIHelper.TEXT_WHITE, false);
            return;
        }

        String pendingTip = null;
        net.minecraft.world.item.ItemStack pendingStack = net.minecraft.world.item.ItemStack.EMPTY;

        g.drawCenteredString(font, tr("wh_npcs.ui.npc.equip_func_col"), funcColX() + 9, contentY + 10, VanillaUIHelper.TEXT_GRAY);
        g.drawCenteredString(font, tr("wh_npcs.ui.npc.equip_cosm_col"), cosmColX() + 9, contentY + 10, VanillaUIHelper.TEXT_AQUA);
        if (isOver(mouseX, mouseY, funcColX(), contentY + 8, 18, 12)) {
            pendingTip = tr("wh_npcs.ui.npc.equip_func_tip");
        } else if (isOver(mouseX, mouseY, cosmColX(), contentY + 8, 18, 12)) {
            pendingTip = tr("wh_npcs.ui.npc.equip_cosm_tip");
        }
        for (int r = 0; r < 6; r++) {
            int y = equipRowY(r);
            g.drawString(font, tr(EQUIP_LABEL_KEYS[r]), contentX + 4, y + 5, VanillaUIHelper.TEXT_GRAY, false);
            net.minecraft.world.item.ItemStack f = renderEquipSlot(g, mouseX, mouseY, funcColX(), y, r);
            net.minecraft.world.item.ItemStack c = renderEquipSlot(g, mouseX, mouseY, cosmColX(), y, r + 6);
            if (!f.isEmpty()) {
                pendingStack = f;
            }
            if (!c.isEmpty()) {
                pendingStack = c;
            }
        }

        int arrowY = equipRowY(6);
        g.drawString(font, tr("wh_npcs.ui.npc.equip_arrows"), contentX + 4, arrowY + 5, VanillaUIHelper.TEXT_GRAY, false);
        boolean arrowHover = isOver(mouseX, mouseY, funcColX(), arrowY, 18, 18);
        VanillaUIHelper.drawItemSlot(g, funcColX(), arrowY, arrowHover);
        if (!arrowEquip.isEmpty()) {
            g.renderItem(arrowEquip, funcColX() + 1, arrowY + 1);
            g.renderItemDecorations(font, arrowEquip, funcColX() + 1, arrowY + 1);
            if (arrowHover && carried.isEmpty()) {
                pendingStack = arrowEquip;
            }
        } else if (arrowHover && carried.isEmpty()) {
            pendingTip = tr("wh_npcs.ui.npc.equip_arrows_tip");
        }

        g.drawString(font, tr("wh_npcs.ui.npc.inventory_take"),
                invGridX(), contentY + 12, VanillaUIHelper.TEXT_GRAY, false);
        if (minecraft != null && minecraft.player != null) {
            for (int i = 0; i < 36; i++) {
                int x = invGridX() + (i % 9) * 18;
                int sy = invGridY() + (i / 9) * 18;
                boolean hovered = isOver(mouseX, mouseY, x, sy, 18, 18);
                VanillaUIHelper.drawItemSlot(g, x, sy, hovered);
                net.minecraft.world.item.ItemStack stack = minecraft.player.getInventory().items.get(i);

                boolean liftedFromHere = i == carriedInvSlot && !carried.isEmpty() && !carriedFromNpc;
                if (!stack.isEmpty() && !liftedFromHere) {
                    g.renderItem(stack, x + 1, sy + 1);
                    g.renderItemDecorations(font, stack, x + 1, sy + 1);
                    if (hovered && carried.isEmpty()) {
                        pendingStack = stack;
                    }
                }
            }
        }

        int ty = invGridY() + 78;
        String vt;
        vt = drawVisToggle(g, ty, hideArmor, tr("wh_npcs.ui.npc.vis_armor"), mouseX, mouseY,
                tr("wh_npcs.ui.npc.vis_armor_tip"));
        if (vt != null) {
            pendingTip = vt;
        }
        vt = drawVisToggle(g, ty + 19, hideMainhand, tr("wh_npcs.ui.npc.vis_mainhand"), mouseX, mouseY,
                tr("wh_npcs.ui.npc.vis_mainhand_tip"));
        if (vt != null) {
            pendingTip = vt;
        }
        vt = drawVisToggle(g, ty + 38, hideOffhand, tr("wh_npcs.ui.npc.vis_offhand"), mouseX, mouseY,
                tr("wh_npcs.ui.npc.vis_offhand_tip"));
        if (vt != null) {
            pendingTip = vt;
        }
        g.drawString(font, tr("wh_npcs.ui.npc.equip_rmb_note"),
                invGridX(), ty + 60, VanillaUIHelper.TEXT_WHITE, false);

        boolean curLoaded = com.withouthonor.npcs.compat.Compat.curiosLoaded();
        boolean curHover = isOver(mouseX, mouseY, invGridX() + 104, ty, 100, 18);
        VanillaUIHelper.drawButton(g, invGridX() + 104, ty, 100, 18, curHover && curLoaded);
        g.drawCenteredString(font, tr("wh_npcs.ui.npc.btn_accessories"), invGridX() + 154, ty + 5,
                !curLoaded ? VanillaUIHelper.TEXT_DARK_GRAY
                        : (curHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_AQUA));
        if (curHover) {
            pendingTip = curLoaded
                    ? tr("wh_npcs.ui.npc.accessories_tip")
                    : tr("wh_npcs.ui.npc.accessories_tip_missing");
        }

        VanillaUIHelper.drawSeparator(g, contentX + 4, dropSlotY() - 20, contentW - 12);
        String dropTip = renderDropGrid(g, mouseX, mouseY);
        if (dropTip != null) {
            pendingTip = dropTip;
        }

        if (!carried.isEmpty()) {
            g.pose().pushPose();
            g.pose().translate(0, 0, 400);
            g.renderItem(carried, mouseX - 8, mouseY - 8);
            g.renderItemDecorations(font, carried, mouseX - 8, mouseY - 8);
            g.pose().popPose();
        }

        if (!pendingStack.isEmpty()) {
            g.renderTooltip(font, pendingStack, mouseX, mouseY);
        } else if (pendingTip != null) {
            multilineTooltip(g, pendingTip, mouseX, mouseY);
        }
    }

    private net.minecraft.world.item.ItemStack renderEquipSlot(GuiGraphics g, int mouseX, int mouseY, int x, int y, int idx) {
        boolean hovered = isOver(mouseX, mouseY, x, y, 18, 18);
        VanillaUIHelper.drawItemSlot(g, x, y, hovered);
        net.minecraft.world.item.ItemStack stack = equipStack(idx);
        if (!stack.isEmpty()) {
            g.renderItem(stack, x + 1, y + 1);
            g.renderItemDecorations(font, stack, x + 1, y + 1);
            if (hovered && carried.isEmpty()) {
                return stack;
            }
        }
        return net.minecraft.world.item.ItemStack.EMPTY;
    }

    private boolean handleEquipmentClick(double mouseX, double mouseY, int button) {
        if (npc == null) {
            return false;
        }
        recalc();

        if (chanceEditBox != null && !chanceEditBox.isMouseOver(mouseX, mouseY)) {
            commitChanceEdit();
        }

        if (button == 0 && com.withouthonor.npcs.compat.Compat.curiosLoaded()
                && isOver(mouseX, mouseY, invGridX() + 104, invGridY() + 78, 100, 18)) {
            if (minecraft != null) {
                minecraft.setScreen(new CuriosEditorScreen(this, npc.getId()));
            }
            return true;
        }
        if (handleDropGridClick(mouseX, mouseY, button)) {
            return true;
        }
        for (int r = 0; r < 6; r++) {
            int y = equipRowY(r);
            for (int col = 0; col < 2; col++) {
                int idx = r + col * 6;
                if (isOver(mouseX, mouseY, col == 0 ? funcColX() : cosmColX(), y, 18, 18)) {
                    if (button == 1) {

                        setEquipStack(idx, carried.isEmpty()
                                ? net.minecraft.world.item.ItemStack.EMPTY : carried.copy());
                    } else if (button == 0) {

                        net.minecraft.world.item.ItemStack inSlot = equipStack(idx);
                        setEquipStack(idx, carried);
                        carried = inSlot;
                        carriedFromNpc = !inSlot.isEmpty();
                        carriedInvSlot = -1;
                    }
                    return true;
                }
            }
        }

        int arrowY = equipRowY(6);
        if (isOver(mouseX, mouseY, funcColX(), arrowY, 18, 18)) {
            if (button == 1) {
                arrowEquip = carried.isEmpty()
                        ? net.minecraft.world.item.ItemStack.EMPTY : carried.copy();
            } else if (button == 0) {
                net.minecraft.world.item.ItemStack inSlot = arrowEquip;
                arrowEquip = carried;
                carried = inSlot;
                carriedFromNpc = !inSlot.isEmpty();
                carriedInvSlot = -1;
            }
            return true;
        }
        if (button == 0 && minecraft != null && minecraft.player != null) {
            for (int i = 0; i < 36; i++) {
                int x = invGridX() + (i % 9) * 18;
                int sy = invGridY() + (i / 9) * 18;
                if (isOver(mouseX, mouseY, x, sy, 18, 18)) {
                    if (!carried.isEmpty()) {
                        var items = minecraft.player.getInventory().items;
                        if (carriedFromNpc) {

                            NetworkHandler.sendToServer(
                                    new com.withouthonor.npcs.network.EditorGiveItemPacket(carried, i));
                            if (items.get(i).isEmpty()) {
                                items.set(i, carried.copy());
                            }
                        } else if (carriedInvSlot >= 0 && carriedInvSlot != i) {

                            NetworkHandler.sendToServer(
                                    new com.withouthonor.npcs.network.EditorMoveItemPacket(carriedInvSlot, i));
                            net.minecraft.world.item.ItemStack moved = items.get(carriedInvSlot);
                            items.set(carriedInvSlot, items.get(i));
                            items.set(i, moved);
                        }

                        carried = net.minecraft.world.item.ItemStack.EMPTY;
                        carriedFromNpc = false;
                        carriedInvSlot = -1;
                    } else {
                        net.minecraft.world.item.ItemStack stack = minecraft.player.getInventory().items.get(i);
                        carried = stack.copy();
                        carriedFromNpc = false;
                        carriedInvSlot = stack.isEmpty() ? -1 : i;
                    }
                    return true;
                }
            }
            int ty = invGridY() + 78;
            if (isOver(mouseX, mouseY, invGridX(), ty, 18, 18)) {
                hideArmor = !hideArmor;
                return true;
            }
            if (isOver(mouseX, mouseY, invGridX(), ty + 19, 18, 18)) {
                hideMainhand = !hideMainhand;
                return true;
            }
            if (isOver(mouseX, mouseY, invGridX(), ty + 38, 18, 18)) {
                hideOffhand = !hideOffhand;
                return true;
            }
        }

        if (!carried.isEmpty()) {
            carried = net.minecraft.world.item.ItemStack.EMPTY;
            carriedFromNpc = false;
            carriedInvSlot = -1;
            return true;
        }
        return false;
    }

    private void renderFactionsTab(GuiGraphics g, int mouseX, int mouseY) {
        String tooltip = null;
        var list = factionList();
        var prefs = com.withouthonor.npcs.client.ClientPrefs.get();

        g.fill(contentX + FAC_LIST_W + 2, contentY + 2, contentX + FAC_LIST_W + 3, contentY + contentH - 12, 0xFF373737);

        if (list.isEmpty()) {
            g.drawString(font, factionsRequested ? tr("wh_npcs.ui.npc.fac_empty1") : tr("wh_npcs.ui.npc.loading"),
                    contentX + 6, contentY + 6, VanillaUIHelper.TEXT_WHITE, false);
            g.drawString(font, tr("wh_npcs.ui.npc.fac_empty2"), contentX + 6, contentY + 18, VanillaUIHelper.TEXT_WHITE, false);
        }
        int maxRows = (contentH - 56) / 13;
        int y = contentY + 2;
        for (int i = 0; i < Math.min(list.size(), maxRows); i++) {
            var faction = list.get(i).faction();
            boolean isSelected = faction.getId().equals(selectedFaction);
            boolean hovered = isOver(mouseX, mouseY, contentX + 2, y, FAC_LIST_W - 2, 13);
            boolean isPinned = prefs.isPinnedFaction(faction.getId());
            if (isSelected || hovered) {
                g.fill(contentX + 2, y, contentX + FAC_LIST_W, y + 13,
                        isSelected ? VanillaUIHelper.BG_SELECTED : VanillaUIHelper.BG_HOVERED);
            }
            String facName = faction.getName();
            boolean facCut = font.width(facName) > FAC_LIST_W - 36;
            g.drawString(font, facCut
                            ? font.plainSubstrByWidth(facName, FAC_LIST_W - 36 - font.width("...")) + "..."
                            : facName,
                    contentX + 6, y + 2, faction.getColor(), false);
            if (hovered && facCut) {
                tooltip = facName;
            }
            if (hovered || isPinned) {
                boolean pinHover = isOver(mouseX, mouseY, contentX + FAC_LIST_W - 26, y + 1, 10, 10);
                drawPin(g, contentX + FAC_LIST_W - 26, y + 2, isPinned ? VanillaUIHelper.TEXT_GOLD
                        : (pinHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_DARK_GRAY));
                if (pinHover) {
                    tooltip = tr("wh_npcs.ui.npc.fac_pin");
                }
            }
            if (hovered && minecraft != null && minecraft.player != null && minecraft.player.hasPermissions(3)) {
                boolean delHover = isOver(mouseX, mouseY, contentX + FAC_LIST_W - 12, y + 1, 10, 10);
                g.drawString(font, "✕", contentX + FAC_LIST_W - 12, y + 2,
                        delHover ? 0xFFFF5555 : VanillaUIHelper.TEXT_GRAY, false);
                if (delHover) {
                    tooltip = tr("wh_npcs.ui.npc.fac_delete_tip");
                }
            }
            y += 13;
        }
        boolean createHover = isOver(mouseX, mouseY, contentX + 4, contentY + contentH - 26, FAC_LIST_W - 6, 18);
        VanillaUIHelper.drawButton(g, contentX + 4, contentY + contentH - 26, FAC_LIST_W - 6, 18, createHover);
        g.drawCenteredString(font, tr("wh_npcs.ui.npc.btn_create"), contentX + 4 + (FAC_LIST_W - 6) / 2, contentY + contentH - 21,
                createHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_GREEN);
        if (createHover) {
            tooltip = tr("wh_npcs.ui.npc.fac_create_tip");
        }

        int fx = facEdX();
        if (selectedFaction == null) {
            g.drawString(font, tr("wh_npcs.ui.npc.fac_intro1"), fx, contentY + 6, VanillaUIHelper.TEXT_WHITE, false);
            g.drawString(font, tr("wh_npcs.ui.npc.fac_intro2"), fx, contentY + 22, VanillaUIHelper.TEXT_WHITE, false);
            g.drawString(font, tr("wh_npcs.ui.npc.fac_intro3"), fx, contentY + 34, VanillaUIHelper.TEXT_WHITE, false);
            g.drawString(font, tr("wh_npcs.ui.npc.fac_intro4"), fx, contentY + 46, VanillaUIHelper.TEXT_WHITE, false);
        } else {
            int ix = fx + 64;
            int edRight = facEdRight();
            int right = edRight - 6;

            g.drawString(font, tr("wh_npcs.ui.npc.fac_label_name"), fx, contentY + 7, VanillaUIHelper.TEXT_GRAY, false);

            boolean colorHover = isOver(mouseX, mouseY, fx + 184, contentY + 3, 16, 16);
            g.fill(fx + 184, contentY + 3, fx + 200, contentY + 19, facColor);
            VanillaUIHelper.drawInsetFrame(g, fx + 184, contentY + 3, 16, 16);
            if (colorHover) {
                tooltip = tr("wh_npcs.ui.npc.fac_color_tip");
            }
            boolean playersHover = isOver(mouseX, mouseY, right - 80, contentY + 2, 80, 18);
            VanillaUIHelper.drawButton(g, right - 80, contentY + 2, 80, 18, playersHover);
            g.drawCenteredString(font, tr("wh_npcs.ui.npc.btn_players"), right - 40, contentY + 7,
                    playersHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_AQUA);
            if (playersHover) {
                tooltip = tr("wh_npcs.ui.npc.fac_players_tip");
            }

            g.drawString(font, tr("wh_npcs.ui.npc.fac_penalty_label"), fx, contentY + 31, VanillaUIHelper.TEXT_GRAY, false);
            g.drawString(font, tr("wh_npcs.ui.npc.fac_per_kill"), fx + 110, contentY + 31, VanillaUIHelper.TEXT_DARK_GRAY, false);
            String usedBy = tr("wh_npcs.ui.npc.fac_used_by", usedByOf(selectedFaction));
            g.drawString(font, usedBy, right - font.width(usedBy), contentY + 31,
                    VanillaUIHelper.TEXT_DARK_GRAY, false);

            g.drawString(font, tr("wh_npcs.ui.npc.fac_enmity"), fx, contentY + 55, VanillaUIHelper.TEXT_GRAY, false);
            int hsX = ix;
            int hsY = contentY + 50;
            int hsW = right - ix;
            boolean hsHover = isOver(mouseX, mouseY, hsX, hsY, hsW, 18);
            g.fill(hsX, hsY, hsX + hsW, hsY + 18, hsHover ? VanillaUIHelper.BG_HOVERED : 0xFF000000);
            VanillaUIHelper.drawInsetFrame(g, hsX, hsY, hsW, 18);
            g.drawString(font, "▾", hsX + hsW - 12, hsY + 5,
                    hsHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_GRAY, false);
            String summary = draftHostile.isEmpty() ? tr("wh_npcs.ui.npc.fac_enmity_empty") : hostileSummary();
            g.drawString(font, font.plainSubstrByWidth(summary, hsW - 18), hsX + 4, hsY + 5,
                    VanillaUIHelper.TEXT_WHITE, false);
            if (hsHover) {
                tooltip = tr("wh_npcs.ui.npc.fac_enmity_tip");
            }

            VanillaUIHelper.drawSeparator(g, fx, contentY + 78, right - fx);
            g.drawString(font, tr("wh_npcs.ui.npc.fac_tiers"), fx, contentY + 84, VanillaUIHelper.TEXT_GRAY, false);
            g.drawString(font, tr("wh_npcs.ui.npc.fac_tiers_sub"), fx + 34, contentY + 84,
                    VanillaUIHelper.TEXT_DARK_GRAY, false);
            g.drawString(font, tr("wh_npcs.ui.npc.fac_col_name"), fx + 20, contentY + 100, VanillaUIHelper.TEXT_DARK_GRAY, false);
            g.drawCenteredString(font, tr("wh_npcs.ui.npc.fac_col_threshold"), fx + 155, contentY + 100, VanillaUIHelper.TEXT_DARK_GRAY);
            g.drawCenteredString(font, tr("wh_npcs.ui.npc.fac_col_price"), fx + 214, contentY + 100, VanillaUIHelper.TEXT_DARK_GRAY);
            for (int t = 0; t < tierNames.size(); t++) {
                int ty = facTierRowY(t);
                boolean tcHover = isOver(mouseX, mouseY, fx, ty + 2, 14, 14);
                g.fill(fx, ty + 2, fx + 14, ty + 16, tierColors.get(t));
                VanillaUIHelper.drawInsetFrame(g, fx, ty + 2, 14, 14);
                if (tcHover) {
                    tooltip = tr("wh_npcs.ui.npc.tier_color_tip");
                }
                if (t == 0) {
                    String floor = tierNames.size() > 1 ? tr("wh_npcs.ui.npc.tier_below", tierMins.get(1)) : tr("wh_npcs.ui.npc.tier_any");
                    g.drawCenteredString(font, floor, fx + 155, ty + 5, VanillaUIHelper.TEXT_DARK_GRAY);
                    if (isOver(mouseX, mouseY, fx + 132, ty, 46, 18)) {
                        tooltip = tr("wh_npcs.ui.npc.tier_bottom_tip");
                    }
                }
                if (tierNames.size() > 1) {
                    boolean delHover = isOver(mouseX, mouseY, fx + 250, ty + 4, 10, 12);
                    g.drawString(font, "✕", fx + 250, ty + 5,
                            delHover ? 0xFFFF5555 : VanillaUIHelper.TEXT_DARK_GRAY, false);
                }
            }
            if (tierNames.size() < 6) {
                int addY = facTierRowY(tierNames.size()) + 2;
                boolean addHover = isOver(mouseX, mouseY, fx, addY, 70, 16);
                VanillaUIHelper.drawButton(g, fx, addY, 70, 16, addHover);
                g.drawCenteredString(font, tr("wh_npcs.ui.npc.fac_add_tier"), fx + 35, addY + 4,
                        addHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_GREEN);
            }
        }
        if (tooltip != null && confirmTitle == null) {
            multilineTooltip(g, tooltip, mouseX, mouseY);
        }
    }

    private int usedByOf(String factionId) {
        for (var entry : com.withouthonor.npcs.client.ClientFactions.full()) {
            if (entry.faction().getId().equals(factionId)) {
                return entry.usedBy();
            }
        }
        return 0;
    }

    private String hostileSummary() {
        List<String> names = new ArrayList<>();
        for (String id : draftHostile) {
            String name = id;
            for (var info : com.withouthonor.npcs.client.ClientFactions.all()) {
                if (info.id().equals(id)) {
                    name = info.name();
                    break;
                }
            }
            names.add(name);
        }
        return String.join(", ", names);
    }

    private List<com.withouthonor.npcs.client.ClientFactions.Full> hostileCandidates() {
        List<com.withouthonor.npcs.client.ClientFactions.Full> out = new ArrayList<>();
        for (var entry : factionList()) {
            if (!entry.faction().getId().equals(selectedFaction)) {
                out.add(entry);
            }
        }
        return out;
    }

    private static final int HOSTILE_W = 220;

    private void renderHostilePicker(GuiGraphics g, int mouseX, int mouseY) {
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        g.fill(winX, winY, winX + winW, winY + winH, 0xA0000000);
        var others = hostileCandidates();
        int rows = others.size();
        int visible = Math.min(Math.max(rows, 1), 12);
        hostilePickerScroll = Math.max(0, Math.min(hostilePickerScroll, Math.max(0, rows - visible)));
        int bh = 52 + visible * 14;
        int bx = (width - HOSTILE_W) / 2;
        int by = (height - bh) / 2;
        VanillaUIHelper.drawWindow(g, bx, by, HOSTILE_W, bh);
        g.drawCenteredString(font, tr("wh_npcs.ui.npc.hostile_title"), bx + HOSTILE_W / 2, by + 8, VanillaUIHelper.TEXT_YELLOW);
        if (others.isEmpty()) {
            g.drawCenteredString(font, tr("wh_npcs.ui.npc.hostile_empty"), bx + HOSTILE_W / 2, by + bh / 2, VanillaUIHelper.TEXT_WHITE);
        }
        int y = by + 24;
        for (int i = hostilePickerScroll; i < Math.min(rows, hostilePickerScroll + visible); i++) {
            var faction = others.get(i).faction();
            boolean checked = draftHostile.contains(faction.getId());
            boolean hovered = isOver(mouseX, mouseY, bx + 4, y, HOSTILE_W - 8, 14);
            if (hovered) {
                g.fill(bx + 4, y, bx + HOSTILE_W - 4, y + 14, VanillaUIHelper.BG_HOVERED);
            }
            VanillaUIHelper.drawButton(g, bx + 8, y + 1, 12, 12, hovered);
            if (checked) {
                VanillaUIHelper.drawCheck(g, bx + 9, y + 3, VanillaUIHelper.TEXT_GREEN);
            }
            g.drawString(font, font.plainSubstrByWidth(faction.getName(), HOSTILE_W - 40),
                    bx + 26, y + 3, faction.getColor(), false);
            y += 14;
        }
        if (rows > visible) {
            VanillaUIHelper.drawScrollbar(g, bx + HOSTILE_W - 8, by + 24, visible * 14, rows, visible,
                    hostilePickerScroll, scrollbars, v -> hostilePickerScroll = v);
        }
        boolean doneHover = isOver(mouseX, mouseY, bx + HOSTILE_W / 2 - 32, by + bh - 22, 64, 16);
        VanillaUIHelper.drawButton(g, bx + HOSTILE_W / 2 - 32, by + bh - 22, 64, 16, doneHover);
        g.drawCenteredString(font, tr("wh_npcs.ui.common.done"), bx + HOSTILE_W / 2, by + bh - 18,
                doneHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_GREEN);
        g.pose().popPose();
    }

    private boolean handleHostilePickerClick(double mouseX, double mouseY) {
        var others = hostileCandidates();
        int rows = others.size();
        int visible = Math.min(Math.max(rows, 1), 12);
        int bh = 52 + visible * 14;
        int bx = (width - HOSTILE_W) / 2;
        int by = (height - bh) / 2;
        if (isOver(mouseX, mouseY, bx + HOSTILE_W / 2 - 32, by + bh - 22, 64, 16)
                || !isOver(mouseX, mouseY, bx, by, HOSTILE_W, bh)) {
            hostilePicker = false;
            return true;
        }
        int y = by + 24;
        for (int i = hostilePickerScroll; i < Math.min(rows, hostilePickerScroll + visible); i++) {
            if (isOver(mouseX, mouseY, bx + 4, y, HOSTILE_W - 8, 14)) {
                String id = others.get(i).faction().getId();
                if (!draftHostile.add(id)) {
                    draftHostile.remove(id);
                }
                facDirty = true;
                return true;
            }
            y += 14;
        }
        return true;
    }

    private boolean handleFactionsClick(double mouseX, double mouseY, int button) {
        recalc();
        var list = factionList();
        var prefs = com.withouthonor.npcs.client.ClientPrefs.get();
        if (button == 0) {

            int maxRows = (contentH - 56) / 13;
            int y = contentY + 2;
            for (int i = 0; i < Math.min(list.size(), maxRows); i++) {
                var entry = list.get(i);
                var faction = entry.faction();
                if (isOver(mouseX, mouseY, contentX + 2, y, FAC_LIST_W - 2, 13)) {
                    if (isOver(mouseX, mouseY, contentX + FAC_LIST_W - 26, y + 1, 10, 10)) {
                        prefs.togglePinnedFaction(faction.getId());
                        return true;
                    }
                    if (minecraft != null && minecraft.player != null && minecraft.player.hasPermissions(3)
                            && isOver(mouseX, mouseY, contentX + FAC_LIST_W - 12, y + 1, 10, 10)) {
                        confirmTitle = tr("wh_npcs.ui.npc.confirm_delete_faction");
                        confirmValue = faction.getName() + " (" + faction.getId() + ")";
                        List<String> warn = new ArrayList<>();
                        if (entry.usedBy() > 0) {
                            warn.add(tr("wh_npcs.ui.npc.confirm_fac_used", entry.usedBy()));
                        }
                        warn.add(tr("wh_npcs.ui.npc.confirm_fac_rep"));
                        confirmWarn = warn;
                        String id = faction.getId();
                        confirmAction = () -> {
                            NetworkHandler.sendToServer(
                                    new com.withouthonor.npcs.network.FactionPackets.Delete(id));
                            if (id.equals(selectedFaction)) {
                                selectedFaction = null;
                                facDirty = false;
                            }
                            init(minecraft, width, height);
                        };
                        return true;
                    }
                    if (!faction.getId().equals(selectedFaction)) {
                        flushFaction();
                        loadFactionDraft(faction);
                    }
                    return true;
                }
                y += 13;
            }

            if (isOver(mouseX, mouseY, contentX + 4, contentY + contentH - 26, FAC_LIST_W - 6, 18)) {
                String name = newFactionBox.getValue().trim();
                if (!name.isEmpty()) {
                    String base = slugify(name);
                    if (base.isEmpty()) {
                        base = "faction";
                    }
                    String id = base;
                    int n = 2;
                    while (factionExists(id)) {
                        id = base + "_" + n++;
                    }
                    flushFaction();
                    var faction = new com.withouthonor.npcs.common.reputation.Faction(
                            id, name, 0xFF55FFFF, 0,
                            com.withouthonor.npcs.common.reputation.Faction.DEFAULT_TIERS);
                    NetworkHandler.sendToServer(
                            new com.withouthonor.npcs.network.FactionPackets.Save(faction.toJson()));
                    newFactionBox.setValue("");
                    loadFactionDraft(faction);
                }
                return true;
            }
        }
        if (selectedFaction == null || button != 0) {
            return false;
        }
        int fx = facEdX();
        int right = facEdRight() - 6;

        if (isOver(mouseX, mouseY, right - 80, contentY + 2, 80, 18)) {
            repPopup = true;
            repLoading = true;
            repScroll = 0;
            repSearch = "";
            repEntries = new ArrayList<>();
            NetworkHandler.sendToServer(
                    new com.withouthonor.npcs.network.FactionPackets.RepRequest(selectedFaction));
            return true;
        }

        if (isOver(mouseX, mouseY, fx + 184, contentY + 3, 16, 16)) {
            palettePicker = true;
            paletteTarget = -1;
            return true;
        }

        if (isOver(mouseX, mouseY, fx + 64, contentY + 50, right - (fx + 64), 18)) {
            hostilePicker = true;
            hostilePickerScroll = 0;
            return true;
        }

        for (int t = 0; t < tierNames.size(); t++) {
            int ty = facTierRowY(t);
            if (isOver(mouseX, mouseY, fx, ty + 2, 14, 14)) {
                palettePicker = true;
                paletteTarget = t;
                return true;
            }
            if (button == 0 && tierNames.size() > 1 && isOver(mouseX, mouseY, fx + 250, ty + 4, 10, 12)) {
                tierNames.remove(t);
                tierMins.remove(t);
                tierColors.remove(t);
                tierPrices.remove(t);
                if (!tierMins.isEmpty()) {
                    tierMins.set(0, Integer.MIN_VALUE);
                }
                facDirty = true;
                init(minecraft, width, height);
                return true;
            }
        }
        if (button == 0 && tierNames.size() < 6
                && isOver(mouseX, mouseY, fx, facTierRowY(tierNames.size()) + 2, 70, 16)) {
            int lastMin = tierMins.isEmpty() || tierMins.get(tierMins.size() - 1) == Integer.MIN_VALUE
                    ? 0 : tierMins.get(tierMins.size() - 1);
            tierNames.add(tr("wh_npcs.ui.npc.fac_tier_n", (tierNames.size() + 1)));
            tierMins.add(lastMin + 20);
            tierColors.add(0xFFAAAAAA);
            tierPrices.add(1.0F);
            facDirty = true;
            init(minecraft, width, height);
            return true;
        }
        return false;
    }

    private List<com.withouthonor.npcs.network.EditorDataPacket.DialogueSummary> displayedDialogues() {
        var prefs = com.withouthonor.npcs.client.ClientPrefs.get();
        String query = dlgSearchBox != null
                ? dlgSearchBox.getValue().toLowerCase(java.util.Locale.ROOT).trim() : "";
        List<com.withouthonor.npcs.network.EditorDataPacket.DialogueSummary> pinned = new ArrayList<>();
        List<com.withouthonor.npcs.network.EditorDataPacket.DialogueSummary> rest = new ArrayList<>();
        for (var summary : dialogues) {
            if (!query.isEmpty()
                    && !summary.id().toLowerCase(java.util.Locale.ROOT).contains(query)
                    && !summary.author().toLowerCase(java.util.Locale.ROOT).contains(query)) {
                continue;
            }
            if (dlgFavTab && !prefs.isFavoriteDialogue(summary.id())) {
                continue;
            }
            if (dlgMineTab && !summary.author().equalsIgnoreCase(myName())) {
                continue;
            }
            (prefs.isPinnedDialogue(summary.id()) ? pinned : rest).add(summary);
        }
        java.util.Comparator<com.withouthonor.npcs.network.EditorDataPacket.DialogueSummary> cmp =
                java.util.Comparator.comparing(s -> s.id().toLowerCase(java.util.Locale.ROOT));
        if (dlgSortDesc) {
            cmp = cmp.reversed();
        }
        pinned.sort(cmp);
        rest.sort(cmp);
        List<com.withouthonor.npcs.network.EditorDataPacket.DialogueSummary> result = new ArrayList<>(pinned);
        result.addAll(rest);
        return result;
    }

    private void renderDialoguesTab(GuiGraphics g, int mouseX, int mouseY) {
        var prefs = com.withouthonor.npcs.client.ClientPrefs.get();

        drawMiniBtn(g, tr("wh_npcs.ui.npc.dlg_tab_all"), dlgTabAllX(), contentY + 3, 34, mouseX, mouseY,
                !dlgFavTab ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_WHITE);
        boolean favHover = isOver(mouseX, mouseY, dlgTabFavX(), contentY + 3, 28, 16);
        VanillaUIHelper.drawButton(g, dlgTabFavX(), contentY + 3, 28, 16, favHover || dlgFavTab);
        drawHeart(g, dlgTabFavX() + 10, contentY + 8, dlgFavTab ? 0xFFFF5555 : VanillaUIHelper.TEXT_GRAY);

        boolean mineHover = isOver(mouseX, mouseY, dlgMineX(), contentY + 3, 16, 16);
        VanillaUIHelper.drawButton(g, dlgMineX(), contentY + 3, 16, 16, mineHover || dlgMineTab);
        drawPlayerHead(g, myName(), dlgMineX() + 4, contentY + 7, dlgMineTab ? 0xFFFFFFFF : 0xFF888888);
        if (mineHover) {
            multilineTooltip(g, tr("wh_npcs.ui.npc.dlg_mine_tip"), mouseX, mouseY);
        }

        drawMiniBtn(g, dlgSortDesc ? tr("wh_npcs.ui.npc.sort_za") : tr("wh_npcs.ui.npc.sort_az"), dlgSortX(), contentY + 3, 34, mouseX, mouseY,
                VanillaUIHelper.TEXT_AQUA);

        boolean refHover = isOver(mouseX, mouseY, dlgRefreshX(), contentY + 3, 16, 16);
        VanillaUIHelper.drawButton(g, dlgRefreshX(), contentY + 3, 16, 16, refHover);
        drawRefreshIcon(g, dlgRefreshX() + 3, contentY + 7, refHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_AQUA);
        if (refHover) {
            multilineTooltip(g, tr("wh_npcs.ui.npc.dlg_refresh_tip"), mouseX, mouseY);
        }

        boolean impHover = isOver(mouseX, mouseY, dlgImportX(), contentY + 3, 16, 16);
        VanillaUIHelper.drawButton(g, dlgImportX(), contentY + 3, 16, 16, impHover);
        drawImportIcon(g, dlgImportX() + 4, contentY + 7, impHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_AQUA);
        if (impHover) {
            multilineTooltip(g, tr("wh_npcs.ui.npc.dlg_import_tip"), mouseX, mouseY);
        }

        boolean hasSel = selectedDialogue != null && !selectedDialogue.isEmpty();
        boolean expHover = isOver(mouseX, mouseY, dlgExportX(), contentY + 3, 16, 16);
        VanillaUIHelper.drawButton(g, dlgExportX(), contentY + 3, 16, 16, expHover);
        int expColor = hasSel
                ? (expHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_GREEN)
                : disabledBtnIcon();
        if (lightButtons()) {
            drawExportIcon(g, dlgExportX() + 5, contentY + 8, 0xFF3F3F3F);
        }
        drawExportIcon(g, dlgExportX() + 4, contentY + 7, expColor);
        if (expHover) {
            multilineTooltip(g, tr("wh_npcs.ui.npc.dlg_export_tip"), mouseX, mouseY);
        }

        boolean renaming = renamingDialogue != null;
        boolean renHover = isOver(mouseX, mouseY, dlgRenameX(), contentY + 3, 16, 16);
        VanillaUIHelper.drawButton(g, dlgRenameX(), contentY + 3, 16, 16, renHover || renaming);
        g.drawCenteredString(font, "✎", dlgRenameX() + 8, contentY + 7,
                renaming ? VanillaUIHelper.TEXT_YELLOW
                        : (hasSel ? (renHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_GREEN)
                                : disabledBtnIcon()));
        if (renHover) {
            multilineTooltip(g, tr("wh_npcs.ui.npc.dlg_rename_tip"), mouseX, mouseY);
        }

        boolean portraitOn = profileJson.has("portrait_show") && profileJson.get("portrait_show").getAsBoolean()
                && profileJson.has("portrait_image");
        drawMiniBtn(g, tr("wh_npcs.ui.npc.btn_portrait"), portraitBtnX(), contentY + 3, 90, mouseX, mouseY,
                portraitOn ? VanillaUIHelper.TEXT_GREEN : VanillaUIHelper.TEXT_AQUA);
        if (isOver(mouseX, mouseY, portraitBtnX(), contentY + 3, 90, 16)) {
            multilineTooltip(g, tr("wh_npcs.ui.npc.portrait_tip") + "\n"
                    + (portraitOn ? tr("wh_npcs.ui.npc.portrait_on") : tr("wh_npcs.ui.npc.portrait_off")), mouseX, mouseY);
        }

        var list = displayedDialogues();
        if (list.isEmpty()) {
            g.drawCenteredString(font, dlgFavTab
                            ? tr("wh_npcs.ui.npc.dlg_empty_fav")
                            : tr("wh_npcs.ui.npc.nothing_found"), contentX + contentW / 2,
                    contentY + contentH / 2 - 4, VanillaUIHelper.TEXT_WHITE);
        }
        int visibleRows = dialogueListRows();
        dialogueScroll = Math.max(0, Math.min(dialogueScroll, Math.max(0, list.size() - visibleRows)));
        String tooltip = null;
        int y = dlgListTop();
        for (int i = dialogueScroll; i < Math.min(list.size(), dialogueScroll + visibleRows); i++) {
            var summary = list.get(i);
            boolean selected = summary.id().equals(selectedDialogue);
            boolean hovered = isOver(mouseX, mouseY, contentX + 4, y, contentW - 8, DLG_ROW_H);
            boolean isPinned = prefs.isPinnedDialogue(summary.id());
            boolean isFavorite = prefs.isFavoriteDialogue(summary.id());
            if (selected || hovered) {
                g.fill(contentX + 4, y, contentX + contentW - 4, y + DLG_ROW_H,
                        selected ? VanillaUIHelper.BG_SELECTED : VanillaUIHelper.BG_HOVERED);
            }
            int x = contentX + 8;
            if (isPinned) {
                drawPin(g, x, y + 2, VanillaUIHelper.TEXT_GOLD);
                x += 11;
            }

            if (!summary.author().isEmpty()) {
                boolean headHover = isOver(mouseX, mouseY, x, y + 2, 8, 8);
                drawAuthorHead(g, summary.author(), x, y + 2);
                if (headHover) {
                    tooltip = tr("wh_npcs.ui.npc.dlg_author", summary.author());
                }
                x += 12;
            }
            g.drawString(font, font.plainSubstrByWidth(summary.id(), dlgIconsX() - x - 50), x, y + 2,
                    selected ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_WHITE, false);
            String nodes = tr("wh_npcs.ui.npc.dlg_nodes", summary.nodes());
            g.drawString(font, nodes, dlgIconsX() - 6 - font.width(nodes), y + 2,
                    VanillaUIHelper.TEXT_DARK_GRAY, false);

            if (!summary.usedBy().isEmpty()) {
                boolean useHover = isOver(mouseX, mouseY, dlgIconsX(), y + 2, 9, 9);
                drawNpcIcon(g, dlgIconsX(), y + 2,
                        useHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_GREEN);
                if (useHover) {
                    StringBuilder sb = new StringBuilder(tr("wh_npcs.ui.npc.dlg_used_by"));
                    summary.usedBy().forEach(n -> sb.append("\n§7• ").append(n));
                    tooltip = sb.toString();
                }
            }
            if (hovered) {
                boolean copyHover = isOver(mouseX, mouseY, dlgIconsX() + 14, y + 1, 12, 11);
                drawCopy(g, dlgIconsX() + 14, y + 2,
                        copyHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_GRAY);
                if (copyHover) {
                    tooltip = tr("wh_npcs.ui.npc.dlg_copy_id");
                }
                boolean heartHover = isOver(mouseX, mouseY, dlgIconsX() + 30, y + 2, 10, 9);
                drawHeart(g, dlgIconsX() + 30, y + 2, isFavorite ? 0xFFFF5555
                        : (heartHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_DARK_GRAY));
                boolean pinHover = isOver(mouseX, mouseY, dlgIconsX() + 44, y + 1, 10, 10);
                drawPin(g, dlgIconsX() + 44, y + 2, isPinned ? VanillaUIHelper.TEXT_GOLD
                        : (pinHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_DARK_GRAY));
                boolean openHover = isOver(mouseX, mouseY, dlgIconsX() + 58, y, 14, DLG_ROW_H);
                g.drawString(font, "↗", dlgIconsX() + 60, y + 2,
                        openHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_AQUA, false);
                if (openHover) {
                    tooltip = tr("wh_npcs.ui.npc.dlg_open");
                }
                if (canDeleteDialogue(summary)) {
                    boolean delHover = isOver(mouseX, mouseY, dlgIconsX() + 74, y + 1, 10, 10);
                    g.drawString(font, "✕", dlgIconsX() + 74, y + 2,
                            delHover ? 0xFFFF5555 : VanillaUIHelper.TEXT_GRAY, false);
                    if (delHover) {
                        tooltip = tr("wh_npcs.ui.npc.dlg_delete_tip");
                    }
                }
            } else if (isFavorite) {
                drawHeart(g, dlgIconsX() + 30, y + 2, 0xFFFF5555);
            }
            y += DLG_ROW_H;
        }
        VanillaUIHelper.drawScrollbar(g, contentX + contentW - 8, dlgListTop(),
                visibleRows * DLG_ROW_H, list.size(), visibleRows, dialogueScroll,
                scrollbars, v -> dialogueScroll = v);

        boolean createHover = isOver(mouseX, mouseY, createBtnX(), newRowY(), 80, 18);
        VanillaUIHelper.drawButton(g, createBtnX(), newRowY(), 80, 18, createHover);
        boolean renameMode = renamingDialogue != null;
        g.drawCenteredString(font, tr(renameMode ? "wh_npcs.ui.npc.btn_rename" : "wh_npcs.ui.npc.btn_create"),
                createBtnX() + 40, newRowY() + 5,
                createHover ? VanillaUIHelper.TEXT_YELLOW
                        : (renameMode ? VanillaUIHelper.TEXT_GOLD : VanillaUIHelper.TEXT_GREEN));
        if (tooltip != null && confirmTitle == null) {
            multilineTooltip(g, tooltip, mouseX, mouseY);
        }
    }

    private int dlgListTop() {
        return contentY + 24;
    }

    private int dlgTabAllX() {
        return contentX + 162;
    }

    private int dlgTabFavX() {
        return dlgTabAllX() + 38;
    }

    private int dlgMineX() {
        return dlgTabFavX() + 32;
    }

    private int dlgSortX() {
        return dlgMineX() + 20;
    }

    private String myName() {
        return minecraft != null && minecraft.player != null
                ? minecraft.player.getGameProfile().getName() : "";
    }

    private void drawPlayerHead(GuiGraphics g, String name, int x, int y, int fallback) {
        com.withouthonor.npcs.client.cache.ClientSkinCache.Skin sk =
                com.withouthonor.npcs.client.cache.ClientSkinCache.getInstance().get(name);
        if (sk != null) {
            g.blit(sk.location(), x, y, 8, 8, 8.0F, 8.0F, 8, 8, 64, 64);
            g.blit(sk.location(), x, y, 8, 8, 40.0F, 8.0F, 8, 8, 64, 64);
        } else {
            g.fill(x, y, x + 8, y + 8, fallback);
        }
    }

    private int dlgRefreshX() {
        return dlgSortX() + 38;
    }

    private int dlgImportX() {
        return dlgRefreshX() + 20;
    }

    private int dlgExportX() {
        return dlgImportX() + 20;
    }

    private int dlgRenameX() {
        return dlgExportX() + 20;
    }

    private int portraitBtnX() {
        return contentX + contentW - 98;
    }

    private int dlgIconsX() {
        return contentX + contentW - 100;
    }

    private boolean canDeleteDialogue(com.withouthonor.npcs.network.EditorDataPacket.DialogueSummary summary) {
        if (minecraft == null || minecraft.player == null) {
            return false;
        }
        return minecraft.player.hasPermissions(3)
                || summary.author().equalsIgnoreCase(minecraft.player.getGameProfile().getName());
    }

    private int dialogueListRows() {
        return (contentH - 66) / DLG_ROW_H;
    }

    private void drawAuthorHead(GuiGraphics g, String author, int x, int y) {
        var skin = com.withouthonor.npcs.client.cache.ClientSkinCache.getInstance().get(author);
        if (skin != null) {
            g.blit(skin.location(), x, y, 8, 8, 8.0F, 8.0F, 8, 8, 64, 64);
            g.blit(skin.location(), x, y, 8, 8, 40.0F, 8.0F, 8, 8, 64, 64);
        } else {
            g.fill(x, y, x + 8, y + 8, 0xFF6E5037);
            g.fill(x + 2, y + 4, x + 3, y + 5, 0xFF2B1F14);
            g.fill(x + 5, y + 4, x + 6, y + 5, 0xFF2B1F14);
        }
    }

    private void drawSkinHead(GuiGraphics g, String spec, int x, int y) {
        net.minecraft.resources.ResourceLocation tex;
        if (spec.isEmpty()) {
            tex = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                    "wh_npcs", "textures/entity/companion/default.png");
        } else {
            var skin = com.withouthonor.npcs.client.cache.ClientSkinCache.getInstance().get(spec);
            tex = skin != null ? skin.location() : null;
        }
        if (tex != null) {
            g.blit(tex, x, y, 12, 12, 8.0F, 8.0F, 8, 8, 64, 64);
            g.blit(tex, x, y, 12, 12, 40.0F, 8.0F, 8, 8, 64, 64);
        } else {
            g.fill(x, y, x + 12, y + 12, 0xFF6E5037);
            g.fill(x + 3, y + 6, x + 5, y + 8, 0xFF2B1F14);
            g.fill(x + 7, y + 6, x + 9, y + 8, 0xFF2B1F14);
        }
    }

    @Nullable
    private String drawVisToggle(GuiGraphics g, int y, boolean hidden, String label,
                               int mouseX, int mouseY, String tooltip) {
        boolean hover = isOver(mouseX, mouseY, invGridX(), y, 18, 18);
        VanillaUIHelper.drawButton(g, invGridX(), y, 18, 18, hover);
        drawEye(g, invGridX() + 4, y + 6, hidden ? 0xFF707070 : VanillaUIHelper.TEXT_YELLOW, hidden);
        g.drawString(font, label + ": " + (hidden ? tr("wh_npcs.ui.npc.vis_hidden") : tr("wh_npcs.ui.npc.vis_shown")), invGridX() + 24, y + 5,
                VanillaUIHelper.TEXT_GRAY, false);
        return hover ? tooltip : null;
    }

    private static void drawNote(GuiGraphics g, int x, int y, int c) {
        g.fill(x + 4, y, x + 5, y + 6, c);
        g.fill(x + 5, y, x + 8, y + 1, c);
        g.fill(x + 6, y + 1, x + 8, y + 3, c);
        g.fill(x + 1, y + 5, x + 5, y + 8, c);
    }

    private static void drawEye(GuiGraphics g, int x, int y, int c, boolean closed) {
        if (closed) {

            g.fill(x + 2, y + 2, x + 8, y + 3, c);
            g.fill(x + 1, y + 1, x + 2, y + 2, c);
            g.fill(x + 8, y + 1, x + 9, y + 2, c);
            g.fill(x + 3, y + 3, x + 4, y + 5, c);
            g.fill(x + 6, y + 3, x + 7, y + 5, c);
        } else {
            g.fill(x + 3, y, x + 7, y + 1, c);
            g.fill(x + 1, y + 1, x + 9, y + 4, c);
            g.fill(x + 3, y + 4, x + 7, y + 5, c);
            g.fill(x + 4, y + 1, x + 6, y + 4, 0xFF202020);
        }
    }

    private static void drawNpcIcon(GuiGraphics g, int x, int y, int c) {
        g.fill(x + 2, y, x + 7, y + 4, c);
        g.fill(x, y + 5, x + 9, y + 9, c);
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

    private static void drawCopy(GuiGraphics g, int x, int y, int c) {
        g.fill(x, y, x + 6, y + 1, c);
        g.fill(x, y, x + 1, y + 6, c);
        g.fill(x + 2, y + 2, x + 8, y + 3, c);
        g.fill(x + 2, y + 7, x + 8, y + 8, c);
        g.fill(x + 2, y + 2, x + 3, y + 8, c);
        g.fill(x + 7, y + 2, x + 8, y + 8, c);
    }

    private static int disabledBtnIcon() {
        return VanillaUIHelper.theme() == VanillaUIHelper.Theme.VANILLA
                ? 0xFFFFFFFF : VanillaUIHelper.TEXT_DARK_GRAY;
    }

    private static boolean lightButtons() {
        return VanillaUIHelper.theme() == VanillaUIHelper.Theme.VANILLA;
    }

    private static void drawExportIcon(GuiGraphics g, int x, int y, int c) {
        g.fill(x + 3, y, x + 5, y + 6, c);
        g.fill(x + 1, y + 2, x + 7, y + 3, c);
        g.fill(x + 2, y + 1, x + 6, y + 2, c);
        g.fill(x, y + 7, x + 8, y + 8, c);
    }

    private static void drawTrashIcon(GuiGraphics g, int x, int y, int c) {
        g.fill(x + 2, y, x + 6, y + 1, c);
        g.fill(x, y + 1, x + 8, y + 2, c);
        g.fill(x + 1, y + 3, x + 7, y + 8, c);
        g.fill(x + 3, y + 4, x + 4, y + 7, 0xFF1A1A1A);
        g.fill(x + 5, y + 4, x + 6, y + 7, 0xFF1A1A1A);
    }

    private static void drawRefreshIcon(GuiGraphics g, int x, int y, int c) {
        g.fill(x + 2, y + 1, x + 7, y + 2, c);
        g.fill(x + 1, y + 2, x + 2, y + 7, c);
        g.fill(x + 2, y + 7, x + 7, y + 8, c);
        g.fill(x + 7, y + 4, x + 8, y + 7, c);
        g.fill(x + 6, y, x + 9, y + 1, c);
        g.fill(x + 7, y + 1, x + 9, y + 2, c);
        g.fill(x + 8, y + 2, x + 9, y + 3, c);
    }

    private static void drawImportIcon(GuiGraphics g, int x, int y, int c) {
        g.fill(x + 3, y, x + 5, y + 4, c);
        g.fill(x + 1, y + 3, x + 7, y + 4, c);
        g.fill(x + 2, y + 4, x + 6, y + 5, c);
        g.fill(x + 3, y + 5, x + 5, y + 6, c);
        g.fill(x, y + 7, x + 8, y + 8, c);
    }

    public int editedEntityId() {
        return npc != null ? npc.getId() : -1;
    }

    private void multilineTooltip(GuiGraphics g, String text, int x, int y) {
        List<Component> lines = new ArrayList<>();
        for (String line : text.split("\n")) {
            lines.add(Component.literal(line));
        }
        queueTooltip(lines);
    }

    private void drawMiniBtn(GuiGraphics g, String label, int x, int y, int w,
                             int mouseX, int mouseY, int color) {
        boolean hovered = isOver(mouseX, mouseY, x, y, w, 16);
        VanillaUIHelper.drawButton(g, x, y, w, 16, hovered);
        g.drawCenteredString(font, label, x + w / 2, y + 4,
                hovered ? VanillaUIHelper.TEXT_YELLOW : color);
    }

    private int newRowY() {
        return contentY + contentH - 30;
    }

    private int createBtnX() {
        return contentX + contentW - 90;
    }

    private void renderEntryPointsTab(GuiGraphics g, int mouseX, int mouseY) {
        String indTooltip = null;
        g.drawString(font, tr("wh_npcs.ui.npc.entry_header"),
                contentX + 6, contentY + 2, VanillaUIHelper.TEXT_DARK_GRAY, false);
        if (entryPoints.isEmpty()) {
            g.drawCenteredString(font, tr("wh_npcs.ui.npc.entry_empty"),
                    contentX + contentW / 2, contentY + contentH / 2 - 4, VanillaUIHelper.TEXT_STATUS);
        }

        java.util.Set<String> knownIds = dialogueIdSet();
        for (EditBox box : entryBoxes) {
            box.setTextColor(knownIds.contains(box.getValue().trim()) ? 0x55FFFF : 0xFF5555);
        }
        int visible = Math.min(ENTRY_VISIBLE, entryPoints.size());
        for (int row = 0; row < visible; row++) {
            int idx = entryScroll + row;
            EntryPoint entry = entryPoints.get(idx);
            int y = entryRowY(row);
            g.drawString(font, (idx + 1) + ".", contentX + 6, y + 5, VanillaUIHelper.TEXT_GRAY, false);
            drawArrow(g, contentX + 22, y + 1, true, mouseX, mouseY, idx > 0);
            drawArrow(g, contentX + 38, y + 1, false, mouseX, mouseY, idx < entryPoints.size() - 1);

            int indX = contentX + contentW - 140;
            com.withouthonor.npcs.common.dialogue.EmoteIcon ind = entry.getIndicator();
            boolean indHover = isOver(mouseX, mouseY, indX, y + 1, 18, 16);
            VanillaUIHelper.drawButton(g, indX, y + 1, 18, 16, indHover);
            if (ind == null) {
                g.drawCenteredString(font, "—", indX + 9, y + 5, VanillaUIHelper.TEXT_DARK_GRAY);
            } else {
                g.blit(INDICATOR_ATLAS, indX + 1, y + 1, 16, 16, ind.atlasIndex() * 16f, 0f, 16, 16,
                        com.withouthonor.npcs.common.dialogue.EmoteIcon.COUNT * 16, 16);
            }
            if (indHover) {
                indTooltip = tr("wh_npcs.ui.npc.entry_indicator_tip");
            }
            int n = entry.getConditions().size();
            drawMini(g, n > 0 ? tr("wh_npcs.ui.npc.entry_cond_n", n) : tr("wh_npcs.ui.npc.entry_cond"), contentX + contentW - 118, y + 1, 48, mouseX, mouseY,
                    n > 0 ? VanillaUIHelper.TEXT_GOLD : VanillaUIHelper.TEXT_WHITE);
            drawMini(g, "×", contentX + contentW - 64, y + 1, 18, mouseX, mouseY, VanillaUIHelper.TEXT_RED);
        }
        VanillaUIHelper.drawScrollbar(g, contentX + contentW - 40, contentY + 14,
                ENTRY_VISIBLE * ENTRY_ROW_H - 6, entryPoints.size(), visible, entryScroll,
                scrollbars, v -> entryScroll = v);
        drawMini(g, tr("wh_npcs.ui.npc.entry_add"), contentX + 6, contentY + contentH - 26, 110, mouseX, mouseY,
                VanillaUIHelper.TEXT_GREEN);

        boolean indEnabled = profileJson.has("indicators_enabled")
                && profileJson.get("indicators_enabled").getAsBoolean();
        int cbX = contentX + contentW - 20;
        int tgY = contentY + contentH - 24;
        boolean cbHover = isOver(mouseX, mouseY, cbX, tgY, 12, 12);
        String lbl = tr("wh_npcs.ui.npc.entry_indicators");
        g.drawString(font, lbl, cbX - 6 - font.width(lbl), tgY + 2, VanillaUIHelper.TEXT_GRAY, false);
        VanillaUIHelper.drawButton(g, cbX, tgY, 12, 12, cbHover);
        if (indEnabled) {
            VanillaUIHelper.drawCheck(g, cbX + 1, tgY + 2, VanillaUIHelper.TEXT_GREEN);
        }
        if (cbHover) {
            indTooltip = tr("wh_npcs.ui.npc.entry_indicators_tip");
        }
        if (indTooltip != null) {
            multilineTooltip(g, indTooltip, mouseX, mouseY);
        }
    }

    private void drawArrow(GuiGraphics g, int x, int y, boolean up, int mouseX, int mouseY, boolean enabled) {
        boolean hovered = enabled && isOver(mouseX, mouseY, x, y, 14, 16);
        VanillaUIHelper.drawButton(g, x, y, 14, 16, hovered);
        g.drawCenteredString(font, up ? "˄" : "˅", x + 7, y + 4,
                enabled ? (hovered ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_WHITE)
                        : VanillaUIHelper.TEXT_DARK_GRAY);
    }

    private void drawMini(GuiGraphics g, String label, int x, int y, int w, int mouseX, int mouseY, int color) {
        boolean hovered = isOver(mouseX, mouseY, x, y, w, 16);
        VanillaUIHelper.drawButton(g, x, y, w, 16, hovered);
        g.drawCenteredString(font, label, x + w / 2, y + 4,
                hovered ? VanillaUIHelper.TEXT_YELLOW : color);
    }

    private void collectEntryRows() {
        for (int row = 0; row < entryBoxes.size(); row++) {
            int idx = entryScroll + row;
            if (idx < entryPoints.size()) {
                entryPoints.get(idx).setDialogueId(entryBoxes.get(row).getValue().trim());
            }
        }
    }

    private void openDialogue(String id) {
        NetworkHandler.sendToServer(new com.withouthonor.npcs.network.RequestDialoguePacket(id));
    }

    private void createDialogue() {
        String id = newDialogueBox.getValue().trim();
        if (!id.matches("[a-z0-9_]{1,64}")) {
            newDialogueBox.setTextColor(0xFF5555);
            return;
        }
        applyFieldsToJson();
        if (dialogueIdSet().contains(id)) {
            openDialogue(id);
            return;
        }

        DialogueEditorScreen.openNew(id, this);
    }

    private void performRename() {
        if (renamingDialogue == null) {
            return;
        }
        String oldId = renamingDialogue;
        String newId = newDialogueBox.getValue().trim();
        if (!newId.matches("[a-z0-9_]{1,64}") || newId.equals(oldId) || dialogueIdSet().contains(newId)) {
            newDialogueBox.setTextColor(0xFF5555);
            return;
        }
        com.withouthonor.npcs.network.NetworkHandler.sendToServer(
                new com.withouthonor.npcs.network.RenameDialoguePacket(oldId, newId));
        for (int i = 0; i < dialogues.size(); i++) {
            var s = dialogues.get(i);
            if (s.id().equals(oldId)) {
                dialogues.set(i, new com.withouthonor.npcs.network.EditorDataPacket.DialogueSummary(
                        newId, s.author(), s.nodes(), s.usedBy()));
            }
        }
        if (oldId.equals(selectedDialogue)) {
            selectedDialogue = newId;
        }
        renamingDialogue = null;
        newDialogueBox.setValue("");
    }

    void onDialogueSaved(String id, int nodeCount) {
        for (int i = 0; i < dialogues.size(); i++) {
            var summary = dialogues.get(i);
            if (summary.id().equals(id)) {
                dialogues.set(i, new com.withouthonor.npcs.network.EditorDataPacket.DialogueSummary(
                        id, summary.author(), nodeCount, summary.usedBy()));
                return;
            }
        }
        String self = minecraft != null && minecraft.player != null
                ? minecraft.player.getGameProfile().getName() : "";
        dialogues.add(new com.withouthonor.npcs.network.EditorDataPacket.DialogueSummary(
                id, self, nodeCount, List.of()));
    }

    private int disguiseBtnX() {
        return contentX + contentW - 92;
    }

    private String shortSkinLabel(String spec) {
        var defaultSkin = com.withouthonor.npcs.common.skin.DefaultSkins.bySpec(spec);
        if (defaultSkin != null) {
            return tr("wh_npcs.ui.npc.skin_default_suffix", defaultSkin.displayName());
        }
        String lower = spec.toLowerCase(java.util.Locale.ROOT);
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            int slash = spec.lastIndexOf('/');
            return "URL: ..." + (slash >= 0 && slash < spec.length() - 1 ? spec.substring(slash + 1) : spec);
        }
        return spec;
    }

    private void setSkinFromLibrary(@Nullable String spec) {
        if (spec == null || spec.isBlank()) {
            profileJson.remove("skin_player_name");
        } else {
            profileJson.addProperty("skin_player_name", spec);
        }
    }

    private void setDisguiseFromPicker(@Nullable String disguise) {
        if (disguise == null) {
            profileJson.remove("disguise");
        } else {
            profileJson.addProperty("disguise", disguise);
        }
    }

    private void setVoiceFromPicker(@Nullable String id, float pitch) {
        if (id == null || id.isBlank()) {
            profileJson.remove("voice_sound");
            profileJson.remove("voice_pitch");
        } else {
            profileJson.addProperty("voice_sound", id);
            profileJson.addProperty("voice_pitch", Math.max(0.5F, Math.min(2.0F, pitch)));
        }
    }

    private int saveX() {
        return winX + winW - PAD - 186;
    }

    private int buttonsY() {
        return winY + winH - PAD - 20;
    }

    private int nameGlowColor() {
        if (nameBox != null) {
            return nameBox.firstColor();
        }
        String n = str("name", "");
        for (int i = 0; i + 1 < n.length(); i++) {
            if (n.charAt(i) == '§') {
                ChatFormatting f = ChatFormatting.getByCode(Character.toLowerCase(n.charAt(i + 1)));
                if (f != null && f.isColor() && f.getColor() != null) {
                    return f.getColor();
                }
            }
        }
        return -1;
    }

    private void drawNameGlow(GuiGraphics g, int x, int y, int w, int h, int rgb) {
        if (rgb < 0 || w <= 0 || h <= 0) {
            return;
        }
        int r = (rgb >> 16) & 0xFF;
        int gr = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        int aTL = 0x17;
        g.flush();
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        com.mojang.blaze3d.systems.RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionColorShader);
        org.joml.Matrix4f m = g.pose().last().pose();
        com.mojang.blaze3d.vertex.BufferBuilder bb = com.mojang.blaze3d.vertex.Tesselator.getInstance().getBuilder();
        bb.begin(com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS,
                com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_COLOR);
        bb.vertex(m, x, y, 0).color(r, gr, b, aTL).endVertex();
        bb.vertex(m, x, y + h, 0).color(r, gr, b, 0).endVertex();
        bb.vertex(m, x + w, y + h, 0).color(r, gr, b, 0).endVertex();
        bb.vertex(m, x + w, y, 0).color(r, gr, b, 0).endVertex();
        com.mojang.blaze3d.vertex.Tesselator.getInstance().end();
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
    }

    @Override
    protected boolean mouseClickedScaled(double mouseX, double mouseY, int button) {
        if (scrollbars.click(mouseX, mouseY)) {
            return true;
        }

        if (repPopup) {
            return handleRepPopupClick(mouseX, mouseY, button);
        }

        if (hostilePicker) {
            if (button == 0) {
                recalc();
                return handleHostilePickerClick(mouseX, mouseY);
            }
            return true;
        }

        if (factionPicker) {
            if (button == 0) {
                recalc();
                var list = factionList();
                int rows = 1 + list.size();
                int visible = Math.min(rows, 14);
                int bw = 200;
                int bh = 30 + visible * 13;
                int bx = (width - bw) / 2;
                int by = (height - bh) / 2;
                int y = by + 22;
                for (int i = factionPickerScroll; i < Math.min(rows, factionPickerScroll + visible); i++) {
                    if (isOver(mouseX, mouseY, bx + 4, y, bw - 8, 13)) {
                        if (i == 0) {
                            profileJson.remove("faction");
                        } else {
                            profileJson.addProperty("faction", list.get(i - 1).faction().getId());
                        }
                        factionPicker = false;
                        return true;
                    }
                    y += 13;
                }
                factionPicker = false;
            }
            return true;
        }

        if (palettePicker) {
            if (button == 0) {
                recalc();
                int cols = 8;
                int step = 20;
                int bw = cols * step + 16;
                int bx = (width - bw) / 2;
                int by = (height - (2 * step + 34)) / 2;
                for (int i = 0; i < PALETTE.length; i++) {
                    int x = bx + 8 + (i % cols) * step;
                    int y = by + 22 + (i / cols) * step;
                    if (isOver(mouseX, mouseY, x, y, 16, 16)) {
                        if (paletteTarget < 0) {
                            facColor = PALETTE[i];
                        } else if (paletteTarget < tierColors.size()) {
                            tierColors.set(paletteTarget, PALETTE[i]);
                        }
                        facDirty = true;
                        palettePicker = false;
                        return true;
                    }
                }
                palettePicker = false;
            }
            return true;
        }

        if (bossColorPicker) {
            if (button == 0) {
                recalc();
                int step = 20;
                int bw = BAR_COLORS.length * step + 16;
                int bx = (width - bw) / 2;
                int by = (height - (step + 34)) / 2;
                for (int i = 0; i < BAR_COLORS.length; i++) {
                    int x = bx + 8 + i * step;
                    int y = by + 22;
                    if (isOver(mouseX, mouseY, x, y, 16, 16)) {
                        profileJson.addProperty("bossbar_color", BAR_COLORS[i]);
                        bossColorPicker = false;
                        return true;
                    }
                }
                bossColorPicker = false;
            }
            return true;
        }

        if (confirmTitle != null) {
            if (button == 0) {
                recalc();
                int w = 260;
                int h = confirmDlgH();
                int x = winX + (winW - w) / 2;
                int y = winY + (winH - h) / 2;
                boolean okClick = isOver(mouseX, mouseY, x + w / 2 - 86, y + h - 28, 80, 18);
                boolean cancelClick = isOver(mouseX, mouseY, x + w / 2 + 6, y + h - 28, 80, 18);
                if (okClick || cancelClick) {
                    if (okClick && confirmAction != null) {
                        confirmAction.run();
                    }
                    clearConfirm();
                }
            }
            return true;
        }
        if (activeTab == 3 && handleEquipmentClick(mouseX, mouseY, button)) {
            return true;
        }
        if (activeTab == 4 && handleFactionsClick(mouseX, mouseY, button)) {
            return true;
        }
        if (activeTab == 5 && handleCombatClick(mouseX, mouseY, button)) {
            return true;
        }
        if (activeTab == 6 && handleBehaviorClick(mouseX, mouseY, button)) {
            return true;
        }
        if (activeTab == 7 && handlePoseClick(mouseX, mouseY, button)) {
            return true;
        }
        if (button == 0) {
            recalc();

            int tabX = winX + PAD;
            int tabGap = tabGap();
            for (int i = 0; i < TAB_KEYS.length; i++) {
                int tw = font.width(tr(TAB_KEYS[i])) + 16;
                if (isOver(mouseX, mouseY, tabX, winY + HEADER_H, tw, TAB_H)) {
                    if (i != activeTab) {
                        applyFieldsToJson();
                        flushFaction();
                        activeTab = i;
                        init(minecraft, width, height);
                    }
                    return true;
                }
                tabX += tw + tabGap;
            }
            if (activeTab == 0) {
                int fieldX = contentX + 110;
                int fieldW = Math.min(220, contentW - 120);
                if (isOver(mouseX, mouseY, fieldX + fieldW - 18, contentY + 40, 18, 18)) {
                    boolean cur = profileJson.has("show_title") && profileJson.get("show_title").getAsBoolean();
                    profileJson.addProperty("show_title", !cur);
                    return true;
                }
                if (isOver(mouseX, mouseY, disguiseBtnX(), contentY + 68, 80, 18)) {
                    applyFieldsToJson();
                    if (minecraft != null) {
                        minecraft.setScreen(new SkinLibraryScreen(this,
                                str("skin_player_name", ""), this::setSkinFromLibrary));
                    }
                    return true;
                }
                if (isOver(mouseX, mouseY, disguiseBtnX(), contentY + 96, 80, 18)) {
                    applyFieldsToJson();
                    if (minecraft != null) {
                        float p = profileJson.has("voice_pitch") ? profileJson.get("voice_pitch").getAsFloat() : 1.0F;
                        minecraft.setScreen(new VoicePickerScreen(this, str("voice_sound", ""), p,
                                this::setVoiceFromPicker));
                    }
                    return true;
                }
                if (isOver(mouseX, mouseY, disguiseBtnX(), contentY + 124, 80, 18)) {
                    applyFieldsToJson();
                    if (minecraft != null) {
                        minecraft.setScreen(new DisguisePickerScreen(this, str("disguise", "").isEmpty()
                                ? null : str("disguise", ""), this::setDisguiseFromPicker));
                    }
                    return true;
                }
                if (isOver(mouseX, mouseY, disguiseBtnX(), contentY + 180, 80, 18)) {
                    applyFieldsToJson();
                    if (minecraft != null) {
                        minecraft.setScreen(new OffersEditorScreen(this, profileJson));
                    }
                    return true;
                }
                if (isOver(mouseX, mouseY, disguiseBtnX(), contentY + 208, 80, 18)) {
                    applyFieldsToJson();
                    if (minecraft != null) {
                        minecraft.setScreen(new PhrasesEditorScreen(this, profileJson));
                    }
                    return true;
                }
                if (isOver(mouseX, mouseY, disguiseBtnX(), contentY + 152, 80, 18)) {
                    factionPicker = true;
                    factionPickerScroll = 0;
                    return true;
                }
            }
            if (activeTab == 1) {
                if (isOver(mouseX, mouseY, dlgTabAllX(), contentY + 3, 34, 16)) {
                    dlgFavTab = false;
                    dlgMineTab = false;
                    dialogueScroll = 0;
                    return true;
                }
                if (isOver(mouseX, mouseY, dlgTabFavX(), contentY + 3, 28, 16)) {
                    dlgFavTab = true;
                    dialogueScroll = 0;
                    return true;
                }
                if (isOver(mouseX, mouseY, dlgMineX(), contentY + 3, 16, 16)) {
                    dlgMineTab = !dlgMineTab;
                    dialogueScroll = 0;
                    return true;
                }
                if (isOver(mouseX, mouseY, dlgSortX(), contentY + 3, 34, 16)) {
                    dlgSortDesc = !dlgSortDesc;
                    return true;
                }
                if (isOver(mouseX, mouseY, dlgRefreshX(), contentY + 3, 16, 16)) {
                    com.withouthonor.npcs.network.NetworkHandler.sendToServer(
                            new com.withouthonor.npcs.network.RefreshDialoguesPacket());
                    return true;
                }
                if (isOver(mouseX, mouseY, dlgImportX(), contentY + 3, 16, 16)) {
                    com.withouthonor.npcs.client.ClientLocalFiles.browseJson(bytes ->
                            com.withouthonor.npcs.network.NetworkHandler.sendToServer(
                                    new com.withouthonor.npcs.network.ClientImportPacket(
                                            com.withouthonor.npcs.network.ClientImportPacket.KIND_DIALOGUE, -1, bytes)));
                    return true;
                }
                if (isOver(mouseX, mouseY, dlgExportX(), contentY + 3, 16, 16)) {
                    if (selectedDialogue != null && !selectedDialogue.isEmpty()) {
                        com.withouthonor.npcs.network.NetworkHandler.sendToServer(
                                new com.withouthonor.npcs.network.RequestDialogueBundle(
                                        java.util.List.of(selectedDialogue)));
                    } else if (minecraft != null && minecraft.player != null) {
                        minecraft.player.displayClientMessage(
                                Component.translatable("wh_npcs.msg.dialogue.export_select"), true);
                    }
                    return true;
                }
                if (isOver(mouseX, mouseY, dlgRenameX(), contentY + 3, 16, 16)) {
                    if (renamingDialogue != null) {
                        renamingDialogue = null;
                    } else if (selectedDialogue != null && !selectedDialogue.isEmpty()) {
                        renamingDialogue = selectedDialogue;
                        newDialogueBox.setValue(selectedDialogue);
                        setFocused(newDialogueBox);
                        newDialogueBox.setFocused(true);
                    } else if (minecraft != null && minecraft.player != null) {
                        minecraft.player.displayClientMessage(
                                Component.translatable("wh_npcs.msg.dialogue.export_select"), true);
                    }
                    return true;
                }
                if (isOver(mouseX, mouseY, portraitBtnX(), contentY + 3, 90, 16) && minecraft != null) {
                    minecraft.setScreen(new PortraitPickerScreen(this, profileJson));
                    return true;
                }
                var prefs = com.withouthonor.npcs.client.ClientPrefs.get();
                var list = displayedDialogues();
                int y = dlgListTop();
                int visibleRows = dialogueListRows();
                for (int i = dialogueScroll; i < Math.min(list.size(), dialogueScroll + visibleRows); i++) {
                    if (isOver(mouseX, mouseY, contentX + 4, y, contentW - 8, DLG_ROW_H)) {
                        String id = list.get(i).id();
                        long now = System.currentTimeMillis();
                        if (isOver(mouseX, mouseY, dlgIconsX() + 74, y + 1, 10, 10)
                                && canDeleteDialogue(list.get(i))) {
                            confirmTitle = tr("wh_npcs.ui.npc.confirm_delete_dialogue");
                            confirmValue = id;
                            List<String> warn = new ArrayList<>();
                            if (!list.get(i).usedBy().isEmpty()) {
                                warn.add(tr("wh_npcs.ui.npc.confirm_dlg_used"));
                                list.get(i).usedBy().forEach(n -> warn.add("§7• " + n));
                            }
                            confirmWarn = warn;
                            confirmAction = () -> {
                                dialogues.removeIf(d -> d.id().equals(id));
                                if (id.equals(selectedDialogue)) {
                                    selectedDialogue = null;
                                }
                                NetworkHandler.sendToServer(
                                        new com.withouthonor.npcs.network.DeleteDialoguePacket(id));
                            };
                            return true;
                        }
                        if (isOver(mouseX, mouseY, dlgIconsX() + 14, y + 1, 12, 11)) {
                            if (minecraft != null) {
                                minecraft.keyboardHandler.setClipboard(id);
                            }
                        } else if (isOver(mouseX, mouseY, dlgIconsX() + 30, y + 2, 10, 9)) {
                            prefs.toggleFavoriteDialogue(id);
                        } else if (isOver(mouseX, mouseY, dlgIconsX() + 44, y + 1, 10, 10)) {
                            prefs.togglePinnedDialogue(id);
                        } else if (isOver(mouseX, mouseY, dlgIconsX() + 58, y, 14, DLG_ROW_H)) {
                            openDialogue(id);
                        } else if (id.equals(selectedDialogue) && now - lastDialogueClickTime < 350) {
                            openDialogue(id);
                        } else {
                            selectedDialogue = id;
                            renamingDialogue = null;
                        }
                        lastDialogueClickTime = now;
                        return true;
                    }
                    y += DLG_ROW_H;
                }
                if (isOver(mouseX, mouseY, createBtnX(), newRowY(), 80, 18)) {
                    if (renamingDialogue != null) {
                        performRename();
                    } else {
                        createDialogue();
                    }
                    return true;
                }
            }
            if (activeTab == 2 && handleEntryPointsClick(mouseX, mouseY)) {
                return true;
            }
            if (npc != null && isOver(mouseX, mouseY, winX + PAD, buttonsY(), 20, 20)) {
                String nm = net.minecraft.ChatFormatting.stripFormatting(str("name", "NPC"));
                confirmTitle = tr("wh_npcs.ui.npc.confirm_delete_npc");
                confirmValue = nm == null || nm.isBlank() ? "NPC" : nm;

                java.util.List<String> warn = new ArrayList<>();
                if (siblingCount > 0) {
                    warn.add(tr(siblingCount == 1 ? "wh_npcs.ui.npc.confirm_npc_siblings_one"
                            : "wh_npcs.ui.npc.confirm_npc_siblings_many", siblingCount));
                }
                confirmWarn = warn;
                java.util.UUID delUuid = npc.getUUID();
                confirmAction = () -> {
                    NetworkHandler.sendToServer(
                            new com.withouthonor.npcs.network.DeleteCompanionPacket(delUuid));
                    onClose();
                };
                return true;
            }
            if (npc != null && isOver(mouseX, mouseY, winX + PAD + 24, buttonsY(), 20, 20)) {
                askExport("wh_npcs.ui.npc.confirm_export", () -> {
                    applyFieldsToJson();
                    flushFaction();
                    NetworkHandler.sendToServer(
                            new com.withouthonor.npcs.network.ProfileSharePackets.Export(profileJson));
                });
                return true;
            }
            if (npc != null && minecraft != null && !minecraft.hasSingleplayerServer()
                    && isOver(mouseX, mouseY, winX + PAD + 72, buttonsY(), 20, 20)) {
                askExport("wh_npcs.ui.npc.confirm_export_client", () -> {
                    applyFieldsToJson();
                    flushFaction();
                    try {
                        String pname = profileJson.has("name") ? profileJson.get("name").getAsString() : "profile";
                        String id8 = profileJson.has("id")
                                ? profileJson.get("id").getAsString().replace("-", "").substring(0, 8) : "";
                        com.withouthonor.npcs.client.ClientLocalFiles.writeProfile(
                                pname + "_" + id8, profileJson.toString());
                        com.withouthonor.npcs.client.ClientLocalFiles.openFolder(false);
                    } catch (Exception ignored) {
                    }
                    java.util.List<String> dlgIds = new ArrayList<>();
                    if (profileJson.has("entry_points")) {
                        for (JsonElement e : profileJson.getAsJsonArray("entry_points")) {
                            JsonObject o = e.getAsJsonObject();
                            if (o.has("dialogue")) {
                                String d = o.get("dialogue").getAsString();
                                if (!d.isEmpty() && !dlgIds.contains(d)) {
                                    dlgIds.add(d);
                                }
                            }
                        }
                    }
                    if (!dlgIds.isEmpty()) {
                        NetworkHandler.sendToServer(
                                new com.withouthonor.npcs.network.RequestDialogueBundle(dlgIds));
                    }
                });
                return true;
            }
            if (npc != null && isOver(mouseX, mouseY, winX + PAD + 48, buttonsY(), 20, 20)) {

                NetworkHandler.sendToServer(
                        new com.withouthonor.npcs.network.ProfileSharePackets.RequestList());
                return true;
            }
            if (isOver(mouseX, mouseY, saveX(), buttonsY(), 90, 20)) {
                save();
                return true;
            }
            if (isOver(mouseX, mouseY, saveX() + 96, buttonsY(), 90, 20)) {
                onClose();
                return true;
            }
            if (isOver(mouseX, mouseY, saveX() - 104, buttonsY(), 96, 20)) {
                VanillaUIHelper.Theme next = switch (VanillaUIHelper.theme()) {
                    case DARK -> VanillaUIHelper.Theme.VANILLA;
                    case VANILLA -> VanillaUIHelper.Theme.COFFEE;
                    default -> VanillaUIHelper.Theme.DARK;
                };
                VanillaUIHelper.setTheme(next);
                com.withouthonor.npcs.client.ClientPrefs.get().setUiTheme(
                        switch (next) {
                            case VANILLA -> "vanilla";
                            case COFFEE -> "coffee";
                            default -> "dark";
                        });
                return true;
            }
        }
        return superMouseClicked(mouseX, mouseY, button);
    }

    private void applyFieldsToJson() {
        if (nameBox != null) {
            String name = nameBox.getValue().isBlank() ? "Companion" : nameBox.getValue().trim();
            profileJson.addProperty("name", name);
            profileJson.addProperty("title", titleBox.getValue().trim());

            profileJson.remove("name_color");
        }
        collectEntryRows();
        JsonArray entries = new JsonArray();
        for (EntryPoint entry : entryPoints) {
            if (!entry.getDialogueId().isBlank()) {
                entries.add(entry.toJson());
            }
        }
        if (entries.isEmpty()) {
            profileJson.remove("entry_points");
        } else {
            profileJson.add("entry_points", entries);
        }
    }

    private boolean handleEntryPointsClick(double mouseX, double mouseY) {
        int visible = Math.min(ENTRY_VISIBLE, entryPoints.size());
        for (int row = 0; row < visible; row++) {
            int idx = entryScroll + row;
            int y = entryRowY(row);
            if (isOver(mouseX, mouseY, contentX + 22, y + 1, 14, 16) && idx > 0) {
                collectEntryRows();
                java.util.Collections.swap(entryPoints, idx, idx - 1);
                init(minecraft, width, height);
                return true;
            }
            if (isOver(mouseX, mouseY, contentX + 38, y + 1, 14, 16) && idx < entryPoints.size() - 1) {
                collectEntryRows();
                java.util.Collections.swap(entryPoints, idx, idx + 1);
                init(minecraft, width, height);
                return true;
            }
            if (isOver(mouseX, mouseY, contentX + contentW - 140, y + 1, 18, 16)) {
                com.withouthonor.npcs.common.dialogue.EmoteIcon cur = entryPoints.get(idx).getIndicator();
                entryPoints.get(idx).setIndicator(
                        cur == null ? com.withouthonor.npcs.common.dialogue.EmoteIcon.EXCLAIM
                                : (cur == com.withouthonor.npcs.common.dialogue.EmoteIcon.EXCLAIM
                                        ? com.withouthonor.npcs.common.dialogue.EmoteIcon.QUESTION : null));
                return true;
            }
            if (isOver(mouseX, mouseY, contentX + contentW - 118, y + 1, 48, 16)) {
                collectEntryRows();
                ConditionsEditorScreen.openForConditions(this, entryPoints.get(idx).getConditions());
                return true;
            }
            if (isOver(mouseX, mouseY, contentX + contentW - 64, y + 1, 18, 16)) {
                collectEntryRows();
                entryPoints.remove(idx);
                init(minecraft, width, height);
                return true;
            }
        }
        if (isOver(mouseX, mouseY, contentX + 6, contentY + contentH - 26, 110, 16)) {
            collectEntryRows();
            entryPoints.add(new EntryPoint(selectedDialogue != null ? selectedDialogue : ""));
            entryScroll = Math.max(0, entryPoints.size() - ENTRY_VISIBLE);
            init(minecraft, width, height);
            return true;
        }
        if (isOver(mouseX, mouseY, contentX + contentW - 20, contentY + contentH - 24, 12, 12)) {
            boolean cur = profileJson.has("indicators_enabled")
                    && profileJson.get("indicators_enabled").getAsBoolean();
            profileJson.addProperty("indicators_enabled", !cur);
            return true;
        }
        return false;
    }

    private boolean saved;

    @Override
    public void onClose() {

        if (!saved && npc != null) {
            npc.revertRenderTransformPreview();
        }
        super.onClose();
    }

    private void save() {
        saved = true;
        applyFieldsToJson();
        flushFaction();
        NetworkHandler.sendToServer(new SaveProfilePacket(profileJson));
        if (npc != null) {
            NetworkHandler.sendToServer(new com.withouthonor.npcs.network.SaveEquipmentPacket(
                    npc.getId(), funcEquip, cosmEquip, hideArmor, hideMainhand, hideOffhand, arrowEquip));
        }
        onClose();
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {

        if (repPopup) {
            if (chr >= ' ' && chr != 127 && repSearch.length() < 32) {
                repSearch += chr;
                repScroll = 0;
            }
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (chanceEditBox != null && chanceEditBox.isFocused()) {
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER
                    || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) {
                commitChanceEdit();
                return true;
            }
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
                cancelChanceEdit();
                return true;
            }
        }
        if (repPopup && keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            repPopup = false;
            return true;
        }
        if (repPopup && keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE) {
            if (!repSearch.isEmpty()) {
                repSearch = repSearch.substring(0, repSearch.length() - 1);
                repScroll = 0;
            }
            return true;
        }
        if (palettePicker && keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            palettePicker = false;
            return true;
        }
        if (bossColorPicker && keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            bossColorPicker = false;
            return true;
        }
        if (factionPicker && keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            factionPicker = false;
            return true;
        }
        if (confirmTitle != null && keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            clearConfirm();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected boolean mouseDraggedScaled(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (scrollbars.drag(mouseY)) {
            return true;
        }
        if (poseRotDrag && button == 0) {
            poseAngle = (poseAngle + (float) dragX * 1.5F) % 360.0F;
            return true;
        }
        if (scrubBox != null && button == 0) {
            double delta = mouseX - scrubStartX;
            if (Math.abs(delta) > 3) {
                scrubbing = true;
                scrubBox.setFocused(false);
                setFocused(null);
            }
            if (scrubbing) {
                adjustPose(scrubBox, scrubStartVal, (float) (delta * scrubSens), scrubMin, scrubMax);
            }
            return true;
        }
        return superMouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    protected boolean mouseReleasedScaled(double mouseX, double mouseY, int button) {
        scrollbars.release();
        poseRotDrag = false;
        scrubBox = null;
        scrubbing = false;
        return superMouseReleased(mouseX, mouseY, button);
    }

    @Override
    protected boolean mouseScrolledScaled(double mouseX, double mouseY, double delta) {
        if (repPopup) {
            repScroll -= (int) Math.signum(delta);
            return true;
        }
        if (factionPicker) {
            factionPickerScroll -= (int) Math.signum(delta);
            return true;
        }
        if (confirmTitle != null || palettePicker || bossColorPicker) {
            return true;
        }

        if (activeTab == 3) {
            for (int i = 0; i < 9; i++) {
                if (dropItem[i] == null) {
                    continue;
                }
                if (isOver(mouseX, mouseY, dropSlotX(i), dropSlotY(), 18, 18)) {
                    int step = (hasShiftDown() ? 8 : 1) * (int) Math.signum(delta);
                    dropCount[i] = Math.max(1, Math.min(64, dropCount[i] + step));
                    writeDropsToJson();
                    return true;
                }
                if (i != chanceEditSlot && isOver(mouseX, mouseY, dropSlotX(i), dropSlotY() + 19, 18, 9)) {
                    int step = (hasShiftDown() ? 1 : 5) * (int) Math.signum(delta);
                    dropChance[i] = Math.max(1, Math.min(100, dropChance[i] + step));
                    writeDropsToJson();
                    return true;
                }
            }
        }
        if (activeTab == 1) {
            dialogueScroll -= (int) Math.signum(delta);
            return true;
        }
        if (activeTab == 2 && entryPoints.size() > ENTRY_VISIBLE) {
            collectEntryRows();
            entryScroll = Math.max(0, Math.min(entryScroll - (int) Math.signum(delta),
                    entryPoints.size() - ENTRY_VISIBLE));
            init(minecraft, width, height);
            return true;
        }
        return superMouseScrolled(mouseX, mouseY, delta);
    }

    private static boolean isOver(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
