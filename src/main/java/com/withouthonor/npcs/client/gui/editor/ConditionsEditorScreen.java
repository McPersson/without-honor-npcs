package com.withouthonor.npcs.client.gui.editor;

import com.withouthonor.npcs.client.gui.ScaledScreen;
import com.withouthonor.npcs.client.gui.ScrollDrag;
import com.withouthonor.npcs.client.gui.SelectableEditBox;
import com.withouthonor.npcs.client.gui.VanillaUIHelper;
import com.withouthonor.npcs.common.dialogue.DialogueChoice;
import com.withouthonor.npcs.common.dialogue.condition.Conditions;
import com.withouthonor.npcs.common.dialogue.condition.DialogueCondition;
import com.withouthonor.npcs.common.dialogue.condition.ItemsCondition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class ConditionsEditorScreen extends ScaledScreen {

    private static final int PAD = 8;
    private static final int HEADER_H = 22;
    private static final int LEFT_W = 130;
    private static final int ROW_H = 13;
    private static final int COL_STEP = 78;

    private record TypeInfo(String type, String label, String description, String detail) {
    }

    private static final List<TypeInfo> TYPES = List.of(
            new TypeInfo("items", "wh_npcs.ui.cond.type.items.label",
                    "wh_npcs.ui.cond.type.items.desc", "wh_npcs.ui.cond.type.items.detail"),
            new TypeInfo("flag", "wh_npcs.ui.cond.type.flag.label",
                    "wh_npcs.ui.cond.type.flag.desc", "wh_npcs.ui.cond.type.flag.detail"),
            new TypeInfo("held_item", "wh_npcs.ui.cond.type.held_item.label",
                    "wh_npcs.ui.cond.type.held_item.desc", "wh_npcs.ui.cond.type.held_item.detail"),
            new TypeInfo("permission", "wh_npcs.ui.cond.type.permission.label",
                    "wh_npcs.ui.cond.type.permission.desc", "wh_npcs.ui.cond.type.permission.detail"),
            new TypeInfo("random", "wh_npcs.ui.cond.type.random.label",
                    "wh_npcs.ui.cond.type.random.desc", "wh_npcs.ui.cond.type.random.detail"),
            new TypeInfo("reputation", "wh_npcs.ui.cond.type.reputation.label",
                    "wh_npcs.ui.cond.type.reputation.desc", "wh_npcs.ui.cond.type.reputation.detail"),
            new TypeInfo("score", "wh_npcs.ui.cond.type.score.label",
                    "wh_npcs.ui.cond.type.score.desc", "wh_npcs.ui.cond.type.score.detail"),
            new TypeInfo("player", "wh_npcs.ui.cond.type.player.label",
                    "wh_npcs.ui.cond.type.player.desc", "wh_npcs.ui.cond.type.player.detail"),
            new TypeInfo("var_equals", "wh_npcs.ui.cond.type.var_equals.label",
                    "wh_npcs.ui.cond.type.var_equals.desc", "wh_npcs.ui.cond.type.var_equals.detail"),
            new TypeInfo("weather", "wh_npcs.ui.cond.type.weather.label",
                    "wh_npcs.ui.cond.type.weather.desc", "wh_npcs.ui.cond.type.weather.detail"),
            new TypeInfo("time", "wh_npcs.ui.cond.type.time.label",
                    "wh_npcs.ui.cond.type.time.desc", "wh_npcs.ui.cond.type.time.detail"),
            new TypeInfo("player_state", "wh_npcs.ui.cond.type.player_state.label",
                    "wh_npcs.ui.cond.type.player_state.desc", "wh_npcs.ui.cond.type.player_state.detail"),
            new TypeInfo("npc_health", "wh_npcs.ui.cond.type.npc_health.label",
                    "wh_npcs.ui.cond.type.npc_health.desc", "wh_npcs.ui.cond.type.npc_health.detail"),
            new TypeInfo("player_level", "wh_npcs.ui.cond.type.player_level.label",
                    "wh_npcs.ui.cond.type.player_level.desc", "wh_npcs.ui.cond.type.player_level.detail"));

    private final Screen parent;

    @Nullable
    private final DialogueChoice choice;
    private final List<DialogueCondition> conditions;

    private int selected = -1;
    private boolean typePicker;
    private int listScroll;
    private final ScrollDrag scrollbars = new ScrollDrag();

    private EditBox hintBox;

    private EditBox flagBox;
    private boolean flagValue = true;

    private EditBox varNameBox;
    private EditBox varValueBox;
    private boolean varIgnoreCase = true;

    private EditBox namesBox;

    private EditBox numberBox;

    private EditBox objectiveBox;
    private EditBox minBox;
    private EditBox maxBox;

    private EditBox factionBox;
    @Nullable
    private String factionTip;

    private EditBox timeMinBox;
    private EditBox timeMaxBox;
    private EditBox hpMinBox;
    private EditBox hpMaxBox;
    private EditBox foodMinBox;
    private EditBox foodMaxBox;
    private EditBox nhMinBox;
    private EditBox nhMaxBox;
    private EditBox plMinBox;
    private EditBox plMaxBox;
    private String weatherState = "clear";
    private String effectMode = "off";
    @Nullable
    private String editorTooltip;
    private final java.util.List<ResourceLocation> draftEffects = new java.util.ArrayList<>();
    private boolean invertDraft;

    private final ResourceLocation[] slotItem = new ResourceLocation[ItemsCondition.MAX_SLOTS];
    private final CompoundTag[] slotNbt = new CompoundTag[ItemsCondition.MAX_SLOTS];
    private final ItemsCondition.NbtMode[] slotMode = new ItemsCondition.NbtMode[ItemsCondition.MAX_SLOTS];
    private final int[] slotCount = new int[ItemsCondition.MAX_SLOTS];
    private final EditBox[] countBoxes = new EditBox[ItemsCondition.MAX_SLOTS];
    private final EditBox[] tagBoxes = new EditBox[ItemsCondition.MAX_SLOTS];
    private boolean draftAll = true;
    private boolean draftConsume;
    private int activeSlot;

    private ItemStack carried = ItemStack.EMPTY;

    // числовые поля с драг-скрабом и кнопками −/+
    private final java.util.List<com.withouthonor.npcs.client.gui.NumberScrub> scrubs = new java.util.ArrayList<>();

    @Nullable
    private ResourceLocation heldItemId;

    private int winX, winY, winW, winH;
    private int leftX, leftY, leftH;
    private int rightX, rightW, mainY;
    private int bottomY;

    private ConditionsEditorScreen(Screen parent, List<DialogueCondition> conditions,
                                   @Nullable DialogueChoice choice) {
        super(Component.translatable("wh_npcs.ui.conditions.title"));
        this.parent = parent;
        this.choice = choice;
        this.conditions = conditions;
    }

    public static void open(Screen parent, DialogueChoice choice) {
        show(new ConditionsEditorScreen(parent, choice.getConditions(), choice));
    }

    public static void openForConditions(Screen parent, List<DialogueCondition> conditions) {
        show(new ConditionsEditorScreen(parent, conditions, null));
    }

    private static void show(ConditionsEditorScreen screen) {
        if (!screen.conditions.isEmpty()) {
            screen.selected = 0;
            screen.loadDraft();
        }
        Minecraft.getInstance().setScreen(screen);
    }

    @Nullable
    private DialogueCondition current() {
        return selected >= 0 && selected < conditions.size() ? conditions.get(selected) : null;
    }

    private int colX(int i) {
        return rightX + 4 + i * COL_STEP;
    }

    /** Базовая линия контента правой панели — под заголовком и разделителем. */
    private int edY() {
        return mainY + 20;
    }

    /** База раскладки «Предметы» — на 12px выше edY(), локальный заголовок убран. */
    private int itemsY() {
        return edY() - 12;
    }

    private int slotRowY() {
        return itemsY() + 26;
    }

    private int countY() {
        return itemsY() + 50;
    }

    private int modeY() {
        return itemsY() + 70;
    }

    private int tagY() {
        return itemsY() + 92;
    }

    private int togglesY() {
        return itemsY() + 120;
    }

    private int invGridY() {
        return itemsY() + 162;
    }

    private int heldGridY() {
        return edY() + 56;
    }

    @Override
    protected void init() {
        recalc();
        clearWidgets();
        scrubs.clear();

        if (choice != null) {
            hintBox = addRenderableWidget(new SelectableEditBox(font, winX + PAD + 92, winY + HEADER_H + 4,
                    winW - PAD * 2 - 92, 16, Component.translatable("wh_npcs.ui.conditions.hint")));
            hintBox.setMaxLength(120);
            hintBox.setValue(EditorCodes.toEditor(choice.getLockedHint() != null ? choice.getLockedHint() : ""));
            hintBox.setHint(Component.translatable("wh_npcs.ui.conditions.hint_placeholder"));
            hintBox.setResponder(value -> choice.setLockedHint(
                    value.isBlank() ? null : EditorCodes.fromEditor(value)));
        }

        DialogueCondition condition = unwrap(current());
        if (condition == null) {
            return;
        }
        switch (condition.type()) {
            case "flag" -> {
                flagBox = addRenderableWidget(new SelectableEditBox(font, rightX + 60, edY() + 14, 160, 16,
                        Component.translatable("wh_npcs.ui.cond.type.flag.label")));
                flagBox.setMaxLength(64);
                flagBox.setValue(((Conditions.Flag) condition).flag());
                flagBox.setResponder(v -> writeBack());
            }
            case "var_equals" -> {
                Conditions.VarEquals ve = (Conditions.VarEquals) condition;
                varNameBox = addRenderableWidget(new SelectableEditBox(font, rightX + 84, edY() + 14, 146, 16,
                        Component.translatable("wh_npcs.ui.cond.type.var_equals.label")));
                varNameBox.setMaxLength(32);
                varNameBox.setValue(ve.name());
                varNameBox.setHint(Component.translatable("wh_npcs.ui.conditions.var_name_hint"));
                varNameBox.setResponder(v -> writeBack());
                varValueBox = addRenderableWidget(new SelectableEditBox(font, rightX + 84, edY() + 42, 146, 16,
                        Component.translatable("wh_npcs.ui.conditions.var_value")));
                varValueBox.setMaxLength(64);
                varValueBox.setValue(ve.value());
                varValueBox.setResponder(v -> writeBack());
            }
            case "player" -> {
                namesBox = addRenderableWidget(new SelectableEditBox(font, rightX, edY() + 28, 230, 16,
                        Component.translatable("wh_npcs.ui.conditions.names")));
                namesBox.setMaxLength(200);
                namesBox.setValue(String.join(", ", ((Conditions.PlayerName) condition).namesLower()));
                namesBox.setHint(Component.literal("dev, alex, steve"));
                namesBox.setResponder(v -> writeBack());
            }
            case "permission", "random" -> {
                boolean perm = condition.type().equals("permission");
                // сдвиг вправо на 15px — слева от поля кнопка «−» скраба
                numberBox = addRenderableWidget(new SelectableEditBox(font, rightX + 105, edY() + 14, 50, 16,
                        Component.translatable("wh_npcs.ui.conditions.number")));
                numberBox.setMaxLength(3);
                numberBox.setValue(perm
                        ? String.valueOf(((Conditions.Permission) condition).level())
                        : String.valueOf(((Conditions.Random) condition).chancePercent()));
                numberBox.setResponder(v -> writeBack());
                scrubs.add(perm
                        ? new com.withouthonor.npcs.client.gui.NumberScrub(numberBox, 0, 4, 1, 0.05F,
                        "2", true, () -> setFocused(null))
                        : new com.withouthonor.npcs.client.gui.NumberScrub(numberBox, 1, 100, 5, 0.5F,
                        "50", true, () -> setFocused(null)));
            }
            case "reputation" -> {
                Conditions.Reputation reputation = (Conditions.Reputation) condition;
                factionBox = addRenderableWidget(new SelectableEditBox(font, rightX + 80, edY() + 14, 150, 16,
                        Component.translatable("wh_npcs.ui.cond.type.reputation.label")));
                factionBox.setMaxLength(32);
                factionBox.setValue(reputation.faction());
                factionBox.setHint(Component.translatable("wh_npcs.ui.conditions.faction_hint"));
                StringBuilder available = new StringBuilder(
                        Component.translatable("wh_npcs.ui.conditions.faction_tip_head").getString());
                if (com.withouthonor.npcs.client.ClientFactions.all().isEmpty()) {
                    available.append("\n").append(
                            Component.translatable("wh_npcs.ui.conditions.faction_tip_empty").getString());
                } else {
                    available.append("\n").append(
                            Component.translatable("wh_npcs.ui.conditions.faction_tip_available").getString());
                    for (var info : com.withouthonor.npcs.client.ClientFactions.all()) {
                        available.append("\n§b").append(info.id()).append(" §8— §7").append(info.name());
                    }
                }
                factionTip = available.toString();
                factionBox.setResponder(v -> writeBack());
                // сдвиг вправо на 15px — слева от полей кнопки «−» скраба
                minBox = addRenderableWidget(new SelectableEditBox(font, rightX + 55, edY() + 42, 50, 16,
                        Component.translatable("wh_npcs.ui.conditions.min")));
                minBox.setValue(reputation.min() != null ? String.valueOf(reputation.min()) : "");
                minBox.setResponder(v -> writeBack());
                maxBox = addRenderableWidget(new SelectableEditBox(font, rightX + 159, edY() + 42, 50, 16,
                        Component.translatable("wh_npcs.ui.conditions.max")));
                maxBox.setValue(reputation.max() != null ? String.valueOf(reputation.max()) : "");
                maxBox.setResponder(v -> writeBack());
                scrubs.add(new com.withouthonor.npcs.client.gui.NumberScrub(minBox, -10000, 10000, 5, 1F,
                        "", true, () -> setFocused(null)));
                scrubs.add(new com.withouthonor.npcs.client.gui.NumberScrub(maxBox, -10000, 10000, 5, 1F,
                        "", true, () -> setFocused(null)));
            }
            case "score" -> {
                Conditions.Score score = (Conditions.Score) condition;
                objectiveBox = addRenderableWidget(new SelectableEditBox(font, rightX + 110, edY() + 14, 150, 16,
                        Component.literal("Objective")));
                objectiveBox.setMaxLength(40);
                objectiveBox.setValue(score.objective());
                objectiveBox.setHint(Component.literal("quest_progress"));
                objectiveBox.setResponder(v -> writeBack());
                // сдвиг вправо на 15px — слева от полей кнопки «−» скраба
                minBox = addRenderableWidget(new SelectableEditBox(font, rightX + 55, edY() + 42, 50, 16,
                        Component.translatable("wh_npcs.ui.conditions.min")));
                minBox.setValue(score.min() != null ? String.valueOf(score.min()) : "");
                minBox.setResponder(v -> writeBack());
                maxBox = addRenderableWidget(new SelectableEditBox(font, rightX + 159, edY() + 42, 50, 16,
                        Component.translatable("wh_npcs.ui.conditions.max")));
                maxBox.setValue(score.max() != null ? String.valueOf(score.max()) : "");
                maxBox.setResponder(v -> writeBack());
                scrubs.add(new com.withouthonor.npcs.client.gui.NumberScrub(minBox, -1000000, 1000000, 1, 1F,
                        "", true, () -> setFocused(null)));
                scrubs.add(new com.withouthonor.npcs.client.gui.NumberScrub(maxBox, -1000000, 1000000, 1, 1F,
                        "", true, () -> setFocused(null)));
            }
            case "items" -> {
                for (int i = 0; i < ItemsCondition.MAX_SLOTS; i++) {
                    // поле уже + сдвиг на 15px — по бокам кнопки −/+ скраба внутри колонки
                    countBoxes[i] = addRenderableWidget(new SelectableEditBox(font, colX(i) + 15, countY(), 40, 14,
                            Component.translatable("wh_npcs.ui.conditions.count")));
                    countBoxes[i].setMaxLength(4);
                    countBoxes[i].setValue(String.valueOf(Math.max(1, slotCount[i])));
                    final int idx = i;
                    countBoxes[i].setResponder(v -> {
                        try {
                            slotCount[idx] = Math.max(1, Integer.parseInt(v.trim()));
                            writeBack();
                        } catch (NumberFormatException ignored) {
                        }
                    });
                    scrubs.add(new com.withouthonor.npcs.client.gui.NumberScrub(countBoxes[i], 1, 999, 1, 0.2F,
                            "1", true, () -> setFocused(null)));
                    tagBoxes[i] = addRenderableWidget(new SelectableEditBox(font, colX(i), tagY(), 70, 14,
                            Component.translatable("wh_npcs.ui.conditions.tag")));
                    tagBoxes[i].setMaxLength(80);
                    tagBoxes[i].setHint(Component.literal("forge:ores"));
                    tagBoxes[i].setResponder(v -> writeBack());
                }
            }
            case "time" -> {
                Conditions.Time t = (Conditions.Time) condition;
                timeMinBox = timeField(rightX + 24, edY() + 14, t.minTicks());
                timeMaxBox = timeField(rightX + 104, edY() + 14, t.maxTicks());
            }
            case "player_state" -> {
                Conditions.PlayerState ps = (Conditions.PlayerState) condition;
                // раздвинуто — между полями и метками кнопки −/+ скраба
                hpMinBox = numField(rightX + 40, edY() + 14, ps.hpMin());
                hpMaxBox = numField(rightX + 125, edY() + 14, ps.hpMax());
                foodMinBox = numField(rightX + 56, edY() + 42, ps.foodMin());
                foodMaxBox = numField(rightX + 141, edY() + 42, ps.foodMax());
                scrubs.add(new com.withouthonor.npcs.client.gui.NumberScrub(hpMinBox, 0, 1024, 1, 0.2F,
                        "", true, () -> setFocused(null)));
                scrubs.add(new com.withouthonor.npcs.client.gui.NumberScrub(hpMaxBox, 0, 1024, 1, 0.2F,
                        "", true, () -> setFocused(null)));
                scrubs.add(new com.withouthonor.npcs.client.gui.NumberScrub(foodMinBox, 0, 20, 1, 0.2F,
                        "", true, () -> setFocused(null)));
                scrubs.add(new com.withouthonor.npcs.client.gui.NumberScrub(foodMaxBox, 0, 20, 1, 0.2F,
                        "", true, () -> setFocused(null)));
            }
            case "npc_health" -> {
                Conditions.NpcHealth nh = (Conditions.NpcHealth) condition;
                // сдвиг вправо на 15px — слева от полей кнопки «−» скраба;
                // max ещё правее, чтобы «До:» не липла к «−»
                nhMinBox = numField(rightX + 55, edY() + 14, nh.minPct());
                nhMaxBox = numField(rightX + 185, edY() + 14, nh.maxPct());
                scrubs.add(new com.withouthonor.npcs.client.gui.NumberScrub(nhMinBox, 0, 100, 5, 0.5F,
                        "", true, () -> setFocused(null)));
                scrubs.add(new com.withouthonor.npcs.client.gui.NumberScrub(nhMaxBox, 0, 100, 5, 0.5F,
                        "", true, () -> setFocused(null)));
            }
            case "player_level" -> {
                Conditions.PlayerLevel pl = (Conditions.PlayerLevel) condition;
                // сдвиг вправо на 15px — слева от полей кнопки «−» скраба;
                // max ещё правее, чтобы «До:» не липла к «−»
                plMinBox = numField(rightX + 55, edY() + 14, pl.min());
                plMaxBox = numField(rightX + 185, edY() + 14, pl.max());
                scrubs.add(new com.withouthonor.npcs.client.gui.NumberScrub(plMinBox, 0, 10000, 1, 0.2F,
                        "", true, () -> setFocused(null)));
                scrubs.add(new com.withouthonor.npcs.client.gui.NumberScrub(plMaxBox, 0, 10000, 1, 0.2F,
                        "", true, () -> setFocused(null)));
            }
            default -> {
            }
        }
    }

    private static DialogueCondition unwrap(DialogueCondition c) {
        return c instanceof Conditions.Inverted inv ? inv.inner() : c;
    }

    private EditBox numField(int x, int y, Integer value) {
        EditBox b = addRenderableWidget(new SelectableEditBox(font, x, y, 40, 16, Component.empty()));
        b.setMaxLength(5);
        b.setValue(value != null ? String.valueOf(value) : "");
        b.setResponder(v -> writeBack());
        return b;
    }

    private EditBox timeField(int x, int y, int ticks) {
        EditBox b = addRenderableWidget(new SelectableEditBox(font, x, y, 48, 16, Component.empty()));
        b.setMaxLength(5);
        b.setValue(ticksToHhmm(ticks));
        b.setResponder(v -> writeBack());
        return b;
    }

    private static String ticksToHhmm(int ticks) {
        int t = ((ticks % 24000) + 24000) % 24000;
        int hour = (t / 1000 + 6) % 24;
        int minute = (t % 1000) * 60 / 1000;
        return String.format("%02d:%02d", hour, minute);
    }

    private static int hhmmToTicks(String s, int def) {
        try {
            String v = s.trim();
            int c = v.indexOf(':');
            int h = c < 0 ? Integer.parseInt(v.trim()) : Integer.parseInt(v.substring(0, c).trim());
            int m = c < 0 ? 0 : Integer.parseInt(v.substring(c + 1).trim());
            int t = ((h - 6 + 24) % 24) * 1000 + m * 1000 / 60;
            return ((t % 24000) + 24000) % 24000;
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static Integer parseOpt(EditBox box) {
        if (box == null || box.getValue().trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(box.getValue().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static final int WIN_W = 520;
    private static final int WIN_H = 330;

    @Override
    protected int designW() {
        return WIN_W;
    }

    @Override
    protected int designH() {
        return WIN_H;
    }

    private void recalc() {
        winW = WIN_W;
        winH = WIN_H;
        winX = (width - winW) / 2;
        winY = (height - winH) / 2;
        leftX = winX + PAD;
        leftY = winY + HEADER_H + 26;
        bottomY = winY + winH - PAD - 20;
        leftH = bottomY - 6 - leftY;
        rightX = winX + PAD + LEFT_W + PAD;
        rightW = winX + winW - PAD - rightX;
        mainY = leftY;
    }

    private void loadDraft() {
        invertDraft = current() instanceof Conditions.Inverted;
        DialogueCondition condition = unwrap(current());
        carried = ItemStack.EMPTY;
        for (int i = 0; i < ItemsCondition.MAX_SLOTS; i++) {
            slotItem[i] = null;
            slotNbt[i] = null;
            slotMode[i] = ItemsCondition.NbtMode.IGNORE;
            slotCount[i] = 1;
        }
        if (condition instanceof ItemsCondition items) {
            draftAll = items.isRequireAll();
            draftConsume = items.isConsume();
            List<ItemsCondition.Slot> slots = items.getSlots();
            for (int i = 0; i < Math.min(ItemsCondition.MAX_SLOTS, slots.size()); i++) {
                ItemsCondition.Slot slot = slots.get(i);
                slotItem[i] = slot.itemId();
                slotNbt[i] = slot.nbt();
                slotMode[i] = slot.nbtMode();
                slotCount[i] = slot.count();
                if (slot.tagId() != null && tagBoxes[i] != null) {
                    tagBoxes[i].setValue(slot.tagId().toString());
                }
            }
            activeSlot = 0;
        } else if (condition instanceof Conditions.HeldItem held) {
            heldItemId = held.itemId();
        } else if (condition instanceof Conditions.Flag flag) {
            flagValue = flag.value();
        } else if (condition instanceof Conditions.VarEquals ve) {
            varIgnoreCase = ve.ignoreCase();
        } else if (condition instanceof Conditions.Weather w) {
            weatherState = w.state();
        } else if (condition instanceof Conditions.PlayerState ps) {
            effectMode = ps.effectMode();
            draftEffects.clear();
            draftEffects.addAll(ps.effects());
        }
    }

    private void writeBack() {
        DialogueCondition condition = current();
        if (condition == null) {
            return;
        }
        try {
            switch (condition.type()) {
                case "flag" -> store(new Conditions.Flag(
                        flagBox.getValue().isBlank() ? "flag" : flagBox.getValue().trim(), flagValue));
                case "var_equals" -> store(new Conditions.VarEquals(
                        varNameBox.getValue().isBlank() ? "var" : varNameBox.getValue().trim(),
                        varValueBox.getValue().trim(), varIgnoreCase));
                case "player" -> {
                    java.util.Set<String> names = new java.util.HashSet<>();
                    for (String n : namesBox.getValue().split(",")) {
                        if (!n.isBlank()) {
                            names.add(n.trim().toLowerCase(java.util.Locale.ROOT));
                        }
                    }
                    if (!names.isEmpty()) {
                        store(new Conditions.PlayerName(names));
                    }
                }
                case "permission" -> store(new Conditions.Permission(parseInt(numberBox, 2)));
                case "random" -> store(new Conditions.Random(
                        Math.max(1, Math.min(100, parseInt(numberBox, 50)))));
                case "score" -> store(new Conditions.Score(
                        objectiveBox.getValue().trim(),
                        minBox.getValue().isBlank() ? null : parseInt(minBox, 0),
                        maxBox.getValue().isBlank() ? null : parseInt(maxBox, 0)));
                case "reputation" -> store(new Conditions.Reputation(
                        factionBox.getValue().trim(),
                        minBox.getValue().isBlank() ? null : parseInt(minBox, 0),
                        maxBox.getValue().isBlank() ? null : parseInt(maxBox, 0)));
                case "held_item" -> {
                    if (heldItemId != null) {
                        store(new Conditions.HeldItem(heldItemId));
                    }
                }
                case "items" -> {
                    List<ItemsCondition.Slot> slots = new ArrayList<>();
                    for (int i = 0; i < ItemsCondition.MAX_SLOTS; i++) {
                        ResourceLocation tag = null;
                        if (slotMode[i] == ItemsCondition.NbtMode.TAG && tagBoxes[i] != null
                                && !tagBoxes[i].getValue().isBlank()) {
                            tag = ResourceLocation.tryParse(tagBoxes[i].getValue().trim());
                        }
                        boolean filled = slotMode[i] == ItemsCondition.NbtMode.TAG ? tag != null : slotItem[i] != null;
                        if (filled) {
                            slots.add(new ItemsCondition.Slot(slotItem[i], tag, Math.max(1, slotCount[i]),
                                    slotMode[i], slotMode[i] == ItemsCondition.NbtMode.EXACT ? slotNbt[i] : null));
                        }
                    }
                    if (!slots.isEmpty()) {
                        store(new ItemsCondition(slots, draftAll, draftConsume));
                    }
                }
                case "weather" -> store(new Conditions.Weather(weatherState));
                case "time" -> store(new Conditions.Time(
                        hhmmToTicks(timeMinBox.getValue(), 0), hhmmToTicks(timeMaxBox.getValue(), 24000)));
                case "player_state" -> store(new Conditions.PlayerState(
                        parseOpt(hpMinBox), parseOpt(hpMaxBox), parseOpt(foodMinBox), parseOpt(foodMaxBox),
                        new ArrayList<>(draftEffects), effectMode));
                case "npc_health" -> store(new Conditions.NpcHealth(
                        clampPct(parseOpt(nhMinBox)), clampPct(parseOpt(nhMaxBox))));
                case "player_level" -> store(new Conditions.PlayerLevel(
                        clampLevel(parseOpt(plMinBox)), clampLevel(parseOpt(plMaxBox))));
                default -> {
                }
            }
        } catch (Exception ignored) {

        }
    }

    private void store(DialogueCondition base) {
        conditions.set(selected, invertDraft ? new Conditions.Inverted(base) : base);
    }

    private static int parseInt(EditBox box, int def) {
        try {
            return Integer.parseInt(box.getValue().trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    /** Проценты 0..100 включительно; null-граница не проверяется. */
    @Nullable
    private static Integer clampPct(@Nullable Integer v) {
        return v == null ? null : Math.max(0, Math.min(100, v));
    }

    /** Уровни опыта 0..10000 включительно; null-граница не проверяется. */
    @Nullable
    private static Integer clampLevel(@Nullable Integer v) {
        return v == null ? null : Math.max(0, Math.min(10000, v));
    }

    private void addCondition(TypeInfo type) {
        DialogueCondition created = switch (type.type()) {
            case "items" -> new ItemsCondition(List.of(new ItemsCondition.Slot(
                    ResourceLocation.fromNamespaceAndPath("minecraft", "diamond"), null, 3,
                    ItemsCondition.NbtMode.IGNORE, null)), true, false);
            case "flag" -> new Conditions.Flag("my_flag", true);
            case "var_equals" -> new Conditions.VarEquals("answer", "", true);
            case "player" -> new Conditions.PlayerName(java.util.Set.of("steve"));
            case "held_item" -> new Conditions.HeldItem(ResourceLocation.fromNamespaceAndPath("minecraft", "paper"));
            case "permission" -> new Conditions.Permission(2);
            case "random" -> new Conditions.Random(50);
            case "reputation" -> new Conditions.Reputation(
                    com.withouthonor.npcs.client.ClientFactions.all().isEmpty() ? "example"
                            : com.withouthonor.npcs.client.ClientFactions.all().get(0).id(),
                    20, null);
            case "weather" -> new Conditions.Weather("rain");
            case "time" -> new Conditions.Time(0, 12000);
            case "player_state" -> new Conditions.PlayerState(null, null, null, null,
                    new ArrayList<>(), "off");
            case "npc_health" -> new Conditions.NpcHealth(null, 50);
            case "player_level" -> new Conditions.PlayerLevel(5, null);
            case "score" -> new Conditions.Score("objective", null, null);
            default -> new Conditions.Score("objective", null, null);
        };
        conditions.add(created);
        selected = conditions.size() - 1;
        loadDraft();
        init(minecraft, width, height);
    }

    @Override
    protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        recalc();
        scrollbars.beginFrame();
        editorTooltip = null;
        if (hintBox != null) {
            hintBox.visible = !typePicker;
        }
        if (factionBox != null) {
            factionBox.visible = !typePicker;
        }
        VanillaUIHelper.drawWindow(g, winX, winY, winW, winH);

        g.fill(winX + 2, winY + 2, winX + winW - 2, winY + HEADER_H - 2, VanillaUIHelper.BG_HEADER);
        g.drawString(font, Component.translatable(choice != null
                        ? "wh_npcs.ui.conditions.title_choice" : "wh_npcs.ui.conditions.title_entry").getString(),
                winX + PAD, winY + 7, VanillaUIHelper.TEXT_YELLOW, false);
        String headerHint = Component.translatable("wh_npcs.ui.conditions.header_and").getString();
        g.drawString(font, headerHint, winX + winW - PAD - font.width(headerHint), winY + 7,
                VanillaUIHelper.TEXT_DARK_GRAY, false);

        if (choice != null) {
            g.drawString(font, Component.translatable("wh_npcs.ui.conditions.hint_label").getString(),
                    winX + PAD, winY + HEADER_H + 8, VanillaUIHelper.TEXT_GRAY, false);
            if (hintBox != null && hintBox.visible && isOver(mouseX, mouseY,
                    hintBox.getX(), hintBox.getY(), hintBox.getWidth(), hintBox.getHeight())) {
                editorTooltip = Component.translatable("wh_npcs.ui.conditions.hint_tip").getString();
            }
        }

        renderConditionList(g, mouseX, mouseY);
        renderEditor(g, mouseX, mouseY);

        g.drawString(font, Component.translatable("wh_npcs.ui.conditions.apply_note").getString(),
                winX + PAD, bottomY + 5, VanillaUIHelper.TEXT_DARK_GRAY, false);
        drawSmall(g, Component.translatable("wh_npcs.ui.common.done").getString(),
                winX + winW - PAD - 80, bottomY, 80, mouseX, mouseY, VanillaUIHelper.TEXT_GREEN);
    }

    @Override
    protected void renderOverlay(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (typePicker) {
            renderTypePicker(g, mouseX, mouseY);
        }

        if (!carried.isEmpty()) {
            g.pose().pushPose();
            g.pose().translate(0, 0, 400);
            g.renderItem(carried, mouseX - 8, mouseY - 8);
            g.renderItemDecorations(font, carried, mouseX - 8, mouseY - 8);
            g.pose().popPose();
        }
        if (editorTooltip != null && !typePicker && carried.isEmpty()) {
            multilineTooltip(g, editorTooltip, mouseX, mouseY);
        }
    }

    private int visibleConditionRows() {
        return Math.max(1, (leftH - 26) / ROW_H);
    }

    private void renderConditionList(GuiGraphics g, int mouseX, int mouseY) {
        VanillaUIHelper.drawContentPanel(g, leftX, leftY, LEFT_W, leftH);
        int visible = visibleConditionRows();
        listScroll = Math.max(0, Math.min(listScroll, Math.max(0, conditions.size() - visible)));
        int y = leftY + 4;
        for (int i = listScroll; i < Math.min(conditions.size(), listScroll + visible); i++) {
            boolean isSelected = i == selected;
            boolean hovered = isOver(mouseX, mouseY, leftX + 2, y, LEFT_W - 4, ROW_H);
            if (isSelected || hovered) {
                g.fill(leftX + 2, y, leftX + LEFT_W - 2, y + ROW_H,
                        isSelected ? VanillaUIHelper.BG_SELECTED : VanillaUIHelper.BG_HOVERED);
            }
            boolean inverted = conditions.get(i) instanceof Conditions.Inverted;
            g.drawString(font, font.plainSubstrByWidth(summary(conditions.get(i)), LEFT_W - 34),
                    leftX + 4, y + 2, isSelected ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_WHITE, false);
            if (hovered || inverted) {
                boolean invHover = isOver(mouseX, mouseY, leftX + LEFT_W - 30, y, 12, ROW_H);
                g.drawString(font, "¬", leftX + LEFT_W - 28, y + 2,
                        inverted ? VanillaUIHelper.TEXT_GOLD : (invHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_DARK_GRAY), false);
                if (invHover) {
                    editorTooltip = Component.translatable("wh_npcs.ui.conditions.invert_tip").getString();
                }
            }
            if (hovered) {
                g.drawString(font, "×", leftX + LEFT_W - 12, y + 2, VanillaUIHelper.TEXT_RED, false);
            }
            y += ROW_H;
        }
        VanillaUIHelper.drawScrollbar(g, leftX + LEFT_W - 6, leftY + 3, visible * ROW_H,
                conditions.size(), visible, listScroll, scrollbars, v -> listScroll = v);
        drawSmall(g, Component.translatable("wh_npcs.ui.conditions.add").getString(),
                leftX + 2, leftY + leftH - 20, LEFT_W - 4, mouseX, mouseY,
                VanillaUIHelper.TEXT_GREEN);
    }

    public static String summary(DialogueCondition condition) {
        if (condition instanceof Conditions.Inverted inv) {
            return "§c¬ §r" + summary(inv.inner());
        }
        if (condition instanceof ItemsCondition items) {
            return Component.translatable("wh_npcs.ui.conditions.sum_items", items.getSlots().size()).getString()
                    + (items.isConsume()
                    ? " " + Component.translatable("wh_npcs.ui.conditions.sum_items_consume").getString() : "");
        }
        if (condition instanceof Conditions.Flag flag) {
            return Component.translatable("wh_npcs.ui.conditions.sum_flag", flag.flag()).getString()
                    + (flag.value() ? ""
                    : " " + Component.translatable("wh_npcs.ui.conditions.sum_flag_no").getString());
        }
        if (condition instanceof Conditions.VarEquals ve) {
            return Component.translatable("wh_npcs.ui.conditions.sum_var", ve.name(), ve.value()).getString();
        }
        if (condition instanceof Conditions.HeldItem held) {
            return Component.translatable("wh_npcs.ui.conditions.sum_held", held.itemId().getPath()).getString();
        }
        if (condition instanceof Conditions.PlayerName player) {
            return Component.translatable("wh_npcs.ui.conditions.sum_players",
                    player.namesLower().size()).getString();
        }
        if (condition instanceof Conditions.Permission permission) {
            return Component.translatable("wh_npcs.ui.conditions.sum_permission",
                    permission.level()).getString();
        }
        if (condition instanceof Conditions.Random random) {
            return Component.translatable("wh_npcs.ui.conditions.sum_random",
                    random.chancePercent()).getString();
        }
        if (condition instanceof Conditions.Score score) {
            return Component.translatable("wh_npcs.ui.conditions.sum_score", score.objective()).getString();
        }
        if (condition instanceof Conditions.Reputation reputation) {
            return Component.translatable("wh_npcs.ui.conditions.sum_reputation",
                    reputation.faction()).getString();
        }
        if (condition instanceof Conditions.Weather w) {
            return Component.translatable("wh_npcs.ui.conditions.sum_weather",
                    Component.translatable("wh_npcs.ui.conditions.weather_" + w.state()).getString()).getString();
        }
        if (condition instanceof Conditions.Time t) {
            return Component.translatable("wh_npcs.ui.conditions.sum_time",
                    ticksToHhmm(t.minTicks()), ticksToHhmm(t.maxTicks())).getString();
        }
        if (condition instanceof Conditions.PlayerState) {
            return Component.translatable("wh_npcs.ui.conditions.sum_player_state").getString();
        }
        if (condition instanceof Conditions.NpcHealth nh) {
            return Component.translatable("wh_npcs.ui.conditions.sum_npc_health",
                    nh.minPct() != null ? nh.minPct() : 0,
                    nh.maxPct() != null ? nh.maxPct() : 100).getString();
        }
        if (condition instanceof Conditions.PlayerLevel pl) {
            return Component.translatable("wh_npcs.ui.conditions.sum_player_level",
                    pl.min() != null ? pl.min() : 0,
                    pl.max() != null ? pl.max() : "∞").getString();
        }
        return condition.type();
    }

    private void renderEditor(GuiGraphics g, int mouseX, int mouseY) {
        DialogueCondition condition = current();
        if (condition == null) {
            g.drawCenteredString(font, Component.translatable("wh_npcs.ui.conditions.empty_hint").getString(),
                    rightX + rightW / 2, mainY + 40, VanillaUIHelper.TEXT_STATUS);
            return;
        }
        // панель + заголовок типа условия (верх вровень с левым списком)
        VanillaUIHelper.drawContentPanel(g, rightX - 4, mainY, rightW, bottomY - 6 - mainY);
        TypeInfo info = null;
        for (TypeInfo t : TYPES) {
            if (t.type().equals(condition.type())) {
                info = t;
                break;
            }
        }
        int hx = rightX;
        int headMaxW = rightW - 8;
        if (invertDraft) {
            String not = Component.translatable("wh_npcs.ui.conditions.header_not").getString() + " ";
            g.drawString(font, not, hx, mainY + 5, VanillaUIHelper.TEXT_GOLD, false);
            hx += font.width(not);
        }
        String headLabel = font.plainSubstrByWidth(info != null
                ? Component.translatable(info.label()).getString() : condition.type(), headMaxW - (hx - rightX));
        g.drawString(font, headLabel, hx, mainY + 5, VanillaUIHelper.TEXT_AQUA, false);
        hx += font.width(headLabel);
        if (info != null) {
            String headDesc = font.plainSubstrByWidth(
                    " — " + Component.translatable(info.description()).getString(), headMaxW - (hx - rightX));
            g.drawString(font, headDesc, hx, mainY + 5, VanillaUIHelper.TEXT_DARK_GRAY, false);
        }
        VanillaUIHelper.drawSeparator(g, rightX, mainY + 16, rightW - 8);
        switch (condition.type()) {
            case "flag" -> {
                g.drawString(font, Component.translatable("wh_npcs.ui.conditions.flag_label").getString(),
                        rightX, edY() + 18, VanillaUIHelper.TEXT_GRAY, false);
                drawSmall(g, Component.translatable(flagValue
                                ? "wh_npcs.ui.conditions.flag_set" : "wh_npcs.ui.conditions.flag_unset").getString(),
                        rightX, edY() + 42, 160, mouseX, mouseY, VanillaUIHelper.TEXT_AQUA);
            }
            case "var_equals" -> {
                g.drawString(font, Component.translatable("wh_npcs.ui.conditions.var_label").getString(),
                        rightX, edY() + 18, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.conditions.var_equals_label").getString(),
                        rightX, edY() + 46, VanillaUIHelper.TEXT_GRAY, false);
                drawSmall(g, Component.translatable(varIgnoreCase
                                ? "wh_npcs.ui.conditions.case_ignore"
                                : "wh_npcs.ui.conditions.case_strict").getString(),
                        rightX, edY() + 70, 160, mouseX, mouseY, VanillaUIHelper.TEXT_AQUA);
            }
            case "player" -> {
                g.drawString(font, Component.translatable("wh_npcs.ui.conditions.player_label").getString(),
                        rightX, edY() + 14, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.conditions.player_note").getString(),
                        rightX, edY() + 50, VanillaUIHelper.TEXT_WHITE, false);
            }
            case "permission" -> g.drawString(font,
                    Component.translatable("wh_npcs.ui.conditions.permission_label").getString(),
                    rightX, edY() + 18, VanillaUIHelper.TEXT_GRAY, false);
            case "random" -> {
                g.drawString(font, Component.translatable("wh_npcs.ui.conditions.random_label").getString(),
                        rightX, edY() + 18, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, "%", rightX + 174, edY() + 18, VanillaUIHelper.TEXT_GRAY, false);
            }
            case "reputation" -> {
                g.drawString(font, Component.translatable("wh_npcs.ui.conditions.faction_label").getString(),
                        rightX, edY() + 18, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.conditions.from").getString(),
                        rightX + 16, edY() + 46, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.conditions.to").getString(),
                        rightX + 126, edY() + 46, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.conditions.empty_bound").getString(),
                        rightX, edY() + 68, VanillaUIHelper.TEXT_DARK_GRAY, false);
                if (factionBox != null && factionBox.visible && factionTip != null && isOver(mouseX, mouseY,
                        factionBox.getX(), factionBox.getY(), factionBox.getWidth(), factionBox.getHeight())) {
                    editorTooltip = factionTip;
                }
            }
            case "score" -> {
                g.drawString(font, Component.translatable("wh_npcs.ui.conditions.objective").getString(),
                        rightX, edY() + 18, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.conditions.from").getString(),
                        rightX + 16, edY() + 46, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.conditions.to").getString(),
                        rightX + 126, edY() + 46, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.conditions.empty_field").getString(),
                        rightX, edY() + 68, VanillaUIHelper.TEXT_DARK_GRAY, false);
            }
            case "held_item" -> {
                g.drawString(font, Component.translatable("wh_npcs.ui.conditions.held_label").getString(),
                        rightX, edY() + 4, VanillaUIHelper.TEXT_GRAY, false);
                VanillaUIHelper.drawItemSlot(g, rightX + 4, edY() + 18, true);
                if (heldItemId != null) {
                    renderGhostItem(g, heldItemId, null, rightX + 5, edY() + 19);
                }
                VanillaUIHelper.drawSeparator(g, rightX, heldGridY() - 14, rightW - 8);
                g.drawString(font, Component.translatable("wh_npcs.ui.conditions.inv_pick").getString(),
                        rightX, heldGridY() - 10, VanillaUIHelper.TEXT_GRAY, false);
                renderInventoryGrid(g, mouseX, mouseY, heldGridY() + 2);
            }
            case "items" -> renderItemsEditor(g, mouseX, mouseY);
            case "weather" -> {
                g.drawString(font, Component.translatable("wh_npcs.ui.conditions.weather_label").getString(),
                        rightX, edY() + 4, VanillaUIHelper.TEXT_GRAY, false);
                String[] st = {"clear", "rain", "thunder"};
                for (int i = 0; i < 3; i++) {
                    boolean sel = weatherState.equals(st[i]);
                    drawSmall(g, (sel ? "✔ " : "")
                                    + Component.translatable("wh_npcs.ui.conditions.weather_" + st[i]).getString(),
                            rightX + i * 84, edY() + 20, 80, mouseX, mouseY,
                            sel ? VanillaUIHelper.TEXT_GOLD : VanillaUIHelper.TEXT_AQUA);
                }
            }
            case "time" -> {
                g.drawString(font, Component.translatable("wh_npcs.ui.conditions.time_from").getString(),
                        rightX, edY() + 18, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.conditions.time_to").getString(),
                        rightX + 80, edY() + 18, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.conditions.time_hint").getString(),
                        rightX, edY() + 44, VanillaUIHelper.TEXT_WHITE, false);
                if (timeMinBox != null && isOver(mouseX, mouseY, timeMinBox.getX(), timeMinBox.getY(),
                        timeMinBox.getWidth(), timeMinBox.getHeight())) {
                    int t = hhmmToTicks(timeMinBox.getValue(), 0);
                    editorTooltip = Component.translatable("wh_npcs.ui.schedule.tip.time", ticksToHhmm(t), t).getString();
                }
                if (timeMaxBox != null && isOver(mouseX, mouseY, timeMaxBox.getX(), timeMaxBox.getY(),
                        timeMaxBox.getWidth(), timeMaxBox.getHeight())) {
                    int t = hhmmToTicks(timeMaxBox.getValue(), 24000);
                    editorTooltip = Component.translatable("wh_npcs.ui.schedule.tip.time", ticksToHhmm(t), t).getString();
                }
            }
            case "player_state" -> {
                g.drawString(font, "§7HP:", rightX, edY() + 18, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, "–", rightX + 101, edY() + 18, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.conditions.food_label").getString(),
                        rightX, edY() + 46, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, "–", rightX + 116, edY() + 46, VanillaUIHelper.TEXT_GRAY, false);
                if (isOver(mouseX, mouseY, rightX, edY() + 16, 30, 12)) {
                    editorTooltip = Component.translatable("wh_npcs.ui.conditions.hp_tip").getString();
                }
                if (isOver(mouseX, mouseY, rightX, edY() + 44, 48, 12)) {
                    editorTooltip = Component.translatable("wh_npcs.ui.conditions.food_tip").getString();
                }
                drawSmall(g, Component.translatable("wh_npcs.ui.conditions.effmode_" + effectMode).getString(),
                        rightX, edY() + 70, 180, mouseX, mouseY, VanillaUIHelper.TEXT_AQUA);
                if (effectMode.equals("any") || effectMode.equals("all")) {
                    drawSmall(g, Component.translatable("wh_npcs.ui.conditions.effects_btn", draftEffects.size()).getString(),
                            rightX + 186, edY() + 70, 120, mouseX, mouseY, VanillaUIHelper.TEXT_AQUA);
                }
                g.drawString(font, Component.translatable("wh_npcs.ui.conditions.empty_bound").getString(),
                        rightX, edY() + 96, VanillaUIHelper.TEXT_DARK_GRAY, false);
            }
            case "npc_health" -> {
                g.drawString(font, Component.translatable("wh_npcs.ui.conditions.from").getString(),
                        rightX + 16, edY() + 18, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, "%", rightX + 114, edY() + 18, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.conditions.to").getString(),
                        rightX + 146, edY() + 18, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, "%", rightX + 244, edY() + 18, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.conditions.empty_bound").getString(),
                        rightX, edY() + 40, VanillaUIHelper.TEXT_DARK_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.conditions.npc_hp_note").getString(),
                        rightX, edY() + 56, VanillaUIHelper.TEXT_WHITE, false);
            }
            case "player_level" -> {
                g.drawString(font, Component.translatable("wh_npcs.ui.conditions.from").getString(),
                        rightX + 16, edY() + 18, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.conditions.level_unit").getString(),
                        rightX + 114, edY() + 18, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.conditions.to").getString(),
                        rightX + 146, edY() + 18, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.conditions.level_unit").getString(),
                        rightX + 244, edY() + 18, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.conditions.empty_bound").getString(),
                        rightX, edY() + 40, VanillaUIHelper.TEXT_DARK_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.conditions.player_level_note").getString(),
                        rightX, edY() + 56, VanillaUIHelper.TEXT_WHITE, false);
            }
            default -> g.drawString(font,
                    Component.translatable("wh_npcs.ui.conditions.type_later", condition.type()).getString(),
                    rightX, edY() + 18, VanillaUIHelper.TEXT_STATUS, false);
        }
        if (!typePicker) {
            for (var s : scrubs) {
                s.render(g, font, mouseX, mouseY);
            }
        }
    }

    private void renderItemsEditor(GuiGraphics g, int mouseX, int mouseY) {
        for (int i = 0; i < ItemsCondition.MAX_SLOTS; i++) {
            int x = colX(i);

            g.drawCenteredString(font,
                    Component.translatable("wh_npcs.ui.conditions.slot_n", i + 1).getString(), x + 35, itemsY() + 14,
                    i == activeSlot ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_DARK_GRAY);

            int slotX = x + 26;
            VanillaUIHelper.drawItemSlot(g, slotX, slotRowY(), i == activeSlot);
            if (i == activeSlot) {
                VanillaUIHelper.drawRaisedFrame(g, slotX - 2, slotRowY() - 2, 22, 22);
            }
            if (slotMode[i] == ItemsCondition.NbtMode.TAG) {
                g.drawCenteredString(font, "#", slotX + 9, slotRowY() + 5, VanillaUIHelper.TEXT_AQUA);
            } else if (slotItem[i] != null) {
                renderGhostItem(g, slotItem[i], slotNbt[i], slotX + 1, slotRowY() + 1);
            }

            String modeLabel = switch (slotMode[i]) {
                case IGNORE -> Component.translatable("wh_npcs.ui.conditions.nbt_ignore").getString();
                case EXACT -> Component.translatable("wh_npcs.ui.conditions.nbt_exact").getString();
                case TAG -> Component.translatable("wh_npcs.ui.conditions.tag").getString();
            };
            drawSmall(g, modeLabel, x, modeY(), 70, mouseX, mouseY, VanillaUIHelper.TEXT_WHITE);
            if (tagBoxes[i] != null) {
                tagBoxes[i].visible = slotMode[i] == ItemsCondition.NbtMode.TAG;
            }
        }
        VanillaUIHelper.drawSeparator(g, rightX, togglesY() - 8, rightW - 8);
        drawSmall(g, Component.translatable(draftAll
                        ? "wh_npcs.ui.conditions.all_slots" : "wh_npcs.ui.conditions.any_slot").getString(),
                rightX + 4, togglesY(), 112, mouseX, mouseY, VanillaUIHelper.TEXT_AQUA);
        drawSmall(g, Component.translatable(draftConsume
                        ? "wh_npcs.ui.conditions.consume_yes"
                        : "wh_npcs.ui.conditions.consume_no").getString(), rightX + 126, togglesY(), 112,
                mouseX, mouseY, draftConsume ? VanillaUIHelper.TEXT_GOLD : VanillaUIHelper.TEXT_WHITE);
        VanillaUIHelper.drawSeparator(g, rightX, invGridY() - 16, rightW - 8);
        g.drawString(font, Component.translatable("wh_npcs.ui.conditions.inv_take").getString(),
                rightX, invGridY() - 12, VanillaUIHelper.TEXT_GRAY, false);
        renderInventoryGrid(g, mouseX, mouseY, invGridY());
    }

    private void renderInventoryGrid(GuiGraphics g, int mouseX, int mouseY, int gridY) {
        if (minecraft == null || minecraft.player == null) {
            return;
        }
        for (int i = 0; i < 36; i++) {
            int x = rightX + 4 + (i % 9) * 18;
            int sy = gridY + (i / 9) * 18;
            boolean hovered = isOver(mouseX, mouseY, x, sy, 18, 18);
            VanillaUIHelper.drawItemSlot(g, x, sy, hovered);
            ItemStack stack = minecraft.player.getInventory().items.get(i);
            if (!stack.isEmpty()) {
                g.renderItem(stack, x + 1, sy + 1);
                g.renderItemDecorations(font, stack, x + 1, sy + 1);
                if (hovered) {
                    g.renderTooltip(font, stack, mouseX, mouseY);
                }
            }
        }
    }

    private void renderGhostItem(GuiGraphics g, ResourceLocation itemId, @Nullable CompoundTag nbt, int x, int y) {
        ItemStack stack = ghostStack(itemId, nbt);
        if (!stack.isEmpty()) {
            g.renderItem(stack, x, y);
        }
    }

    private static ItemStack ghostStack(@Nullable ResourceLocation itemId, @Nullable CompoundTag nbt) {
        Item item = itemId != null ? ForgeRegistries.ITEMS.getValue(itemId) : null;
        if (item == null) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(item);
        if (nbt != null) {
            stack.setTag(nbt.copy());
        }
        return stack;
    }

    private void renderTypePicker(GuiGraphics g, int mouseX, int mouseY) {

        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        g.fill(winX, winY, winX + winW, winY + winH, VanillaUIHelper.BG_DARK);
        int rows = (TYPES.size() + 1) / 2;
        int bw = 512;
        int bh = 32 + rows * 26;
        int bx = (width - bw) / 2;
        int by = (height - bh) / 2;
        int colGap = 6;
        int colW = (bw - 12 - colGap) / 2;
        VanillaUIHelper.drawWindow(g, bx, by, bw, bh);
        g.drawCenteredString(font, Component.translatable("wh_npcs.ui.conditions.type_picker").getString(),
                bx + bw / 2, by + 9, VanillaUIHelper.TEXT_YELLOW);
        String hoveredDetail = null;
        for (int i = 0; i < TYPES.size(); i++) {
            TypeInfo type = TYPES.get(i);
            int col = i / rows;
            int x = bx + 6 + col * (colW + colGap);
            int y = by + 26 + (i % rows) * 26;
            boolean infoHover = isOver(mouseX, mouseY, x + colW - 22, y + 4, 16, 16);
            boolean hovered = !infoHover && isOver(mouseX, mouseY, x, y, colW, 24);
            VanillaUIHelper.drawButton(g, x, y, colW, 24, hovered);
            g.drawString(font, font.plainSubstrByWidth(
                    Component.translatable(type.label()).getString(), colW - 30), x + 6, y + 4,
                    hovered ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_WHITE, false);
            g.drawString(font, font.plainSubstrByWidth(
                            Component.translatable(type.description()).getString(), colW - 30), x + 6, y + 14,
                    VanillaUIHelper.TEXT_DARK_GRAY, false);
            g.drawCenteredString(font, "?", x + colW - 14, y + 8,
                    infoHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_AQUA);
            if (infoHover) {
                hoveredDetail = Component.translatable(type.detail()).getString();
            }
        }
        if (hoveredDetail != null) {
            multilineTooltip(g, hoveredDetail, mouseX, mouseY);
        }
        g.pose().popPose();
    }

    private void multilineTooltip(GuiGraphics g, String text, int x, int y) {
        List<Component> lines = new ArrayList<>();
        for (String line : text.split("\n")) {
            lines.add(Component.literal(line));
        }
        queueTooltip(lines);
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
        recalc();
        if (typePicker) {
            if (button == 0) {
                int rows = (TYPES.size() + 1) / 2;
                int bw = 512;
                int bx = (width - bw) / 2;
                int by = (height - (32 + rows * 26)) / 2;
                int colGap = 6;
                int colW = (bw - 12 - colGap) / 2;
                for (int i = 0; i < TYPES.size(); i++) {
                    int col = i / rows;
                    int x = bx + 6 + col * (colW + colGap);
                    int rowY = by + 26 + (i % rows) * 26;
                    if (isOver(mouseX, mouseY, x + colW - 22, rowY + 4, 16, 16)) {
                        return true;
                    }
                    if (isOver(mouseX, mouseY, x, rowY, colW, 24)) {
                        typePicker = false;
                        addCondition(TYPES.get(i));
                        return true;
                    }
                }
                typePicker = false;
            }
            return true;
        }
        if (button == 0) {
            if (scrollbars.click(mouseX, mouseY)) {
                return true;
            }
            for (var s : scrubs) {
                if (s.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
            }
            int visible = visibleConditionRows();
            int y = leftY + 4;
            for (int i = listScroll; i < Math.min(conditions.size(), listScroll + visible); i++) {
                if (isOver(mouseX, mouseY, leftX + 2, y, LEFT_W - 4, ROW_H)) {
                    if (isOver(mouseX, mouseY, leftX + LEFT_W - 16, y, 14, ROW_H)) {
                        conditions.remove(i);
                        selected = conditions.isEmpty() ? -1 : Math.min(selected, conditions.size() - 1);
                        loadDraft();
                        init(minecraft, width, height);
                    } else if (isOver(mouseX, mouseY, leftX + LEFT_W - 30, y, 12, ROW_H)) {
                        DialogueCondition c = conditions.get(i);
                        boolean nowInv = !(c instanceof Conditions.Inverted);
                        conditions.set(i, nowInv ? new Conditions.Inverted(unwrap(c)) : unwrap(c));
                        if (i == selected) {
                            invertDraft = nowInv;
                        }
                    } else if (i != selected) {
                        selected = i;
                        loadDraft();
                        init(minecraft, width, height);
                    }
                    return true;
                }
                y += ROW_H;
            }
            if (isOver(mouseX, mouseY, leftX + 2, leftY + leftH - 20, LEFT_W - 4, 18)) {
                typePicker = true;
                return true;
            }
            if (isOver(mouseX, mouseY, winX + winW - PAD - 80, bottomY, 80, 18)) {
                onClose();
                return true;
            }
            if (handleEditorClick(mouseX, mouseY, false)) {
                return true;
            }
            if (!carried.isEmpty()) {
                carried = ItemStack.EMPTY;
                return true;
            }
        } else if (button == 1) {
            for (var s : scrubs) {
                if (s.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
            }
            if (handleEditorClick(mouseX, mouseY, true)) {
                return true;
            }
            if (!carried.isEmpty()) {
                carried = ItemStack.EMPTY;
                return true;
            }
        }
        return superMouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected boolean mouseScrolledScaled(double mouseX, double mouseY, double delta) {
        recalc();
        if (!typePicker && isOver(mouseX, mouseY, leftX, leftY, LEFT_W, leftH)) {
            listScroll = Math.max(0, Math.min(listScroll - (int) Math.signum(delta),
                    Math.max(0, conditions.size() - visibleConditionRows())));
            return true;
        }
        return superMouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    protected boolean mouseDraggedScaled(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (scrollbars.drag(mouseY)) {
            return true;
        }
        for (var s : scrubs) {
            if (s.mouseDragged(mouseX, mouseY, button)) {
                return true;
            }
        }
        return superMouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    protected boolean mouseReleasedScaled(double mouseX, double mouseY, int button) {
        scrollbars.release();
        for (var s : scrubs) {
            s.mouseReleased();
        }
        return superMouseReleased(mouseX, mouseY, button);
    }

    private boolean handleEditorClick(double mouseX, double mouseY, boolean rightClick) {
        DialogueCondition condition = current();
        if (condition == null) {
            return false;
        }
        switch (condition.type()) {
            case "flag" -> {
                if (!rightClick && isOver(mouseX, mouseY, rightX, edY() + 42, 160, 18)) {
                    flagValue = !flagValue;
                    writeBack();
                    return true;
                }
            }
            case "var_equals" -> {
                if (!rightClick && isOver(mouseX, mouseY, rightX, edY() + 70, 160, 18)) {
                    varIgnoreCase = !varIgnoreCase;
                    writeBack();
                    return true;
                }
            }
            case "held_item" -> {
                return handleInventoryClick(mouseX, mouseY, heldGridY() + 2, rightClick);
            }
            case "items" -> {
                for (int i = 0; i < ItemsCondition.MAX_SLOTS; i++) {
                    int slotX = colX(i) + 26;
                    if (isOver(mouseX, mouseY, slotX, slotRowY(), 18, 18)) {
                        activeSlot = i;
                        if (rightClick) {
                            slotItem[i] = null;
                            slotNbt[i] = null;
                            writeBack();
                        } else if (!carried.isEmpty()) {

                            slotItem[i] = ForgeRegistries.ITEMS.getKey(carried.getItem());
                            slotNbt[i] = carried.getTag() != null ? carried.getTag().copy() : null;
                            if (slotMode[i] == ItemsCondition.NbtMode.TAG) {
                                slotMode[i] = ItemsCondition.NbtMode.IGNORE;
                            }
                            carried = ItemStack.EMPTY;
                            writeBack();
                        } else if (slotItem[i] != null) {

                            carried = ghostStack(slotItem[i], slotNbt[i]);
                            slotItem[i] = null;
                            slotNbt[i] = null;
                            writeBack();
                        }
                        return true;
                    }
                    if (!rightClick && isOver(mouseX, mouseY, colX(i), modeY(), 70, 18)) {
                        slotMode[i] = ItemsCondition.NbtMode.values()[(slotMode[i].ordinal() + 1) % 3];
                        writeBack();
                        return true;
                    }
                }
                if (!rightClick && isOver(mouseX, mouseY, rightX + 4, togglesY(), 112, 18)) {
                    draftAll = !draftAll;
                    writeBack();
                    return true;
                }
                if (!rightClick && isOver(mouseX, mouseY, rightX + 126, togglesY(), 112, 18)) {
                    draftConsume = !draftConsume;
                    writeBack();
                    return true;
                }
                return handleInventoryClick(mouseX, mouseY, invGridY(), rightClick);
            }
            case "weather" -> {
                if (!rightClick) {
                    String[] st = {"clear", "rain", "thunder"};
                    for (int i = 0; i < 3; i++) {
                        if (isOver(mouseX, mouseY, rightX + i * 84, edY() + 20, 80, 18)) {
                            weatherState = st[i];
                            writeBack();
                            return true;
                        }
                    }
                }
            }
            case "player_state" -> {
                if (!rightClick && isOver(mouseX, mouseY, rightX, edY() + 70, 180, 18)) {
                    String[] modes = {"off", "any", "all", "any_effect", "no_effect"};
                    int idx = 0;
                    for (int i = 0; i < modes.length; i++) {
                        if (modes[i].equals(effectMode)) {
                            idx = i;
                        }
                    }
                    effectMode = modes[(idx + 1) % modes.length];
                    writeBack();
                    return true;
                }
                if (!rightClick && (effectMode.equals("any") || effectMode.equals("all"))
                        && isOver(mouseX, mouseY, rightX + 186, edY() + 70, 120, 18) && minecraft != null) {
                    minecraft.setScreen(new EffectPickerScreen(this, draftEffects, this::writeBack));
                    return true;
                }
            }
            default -> {
            }
        }
        return false;
    }

    private boolean handleInventoryClick(double mouseX, double mouseY, int gridY, boolean rightClick) {
        if (rightClick || minecraft == null || minecraft.player == null) {
            return false;
        }
        for (int i = 0; i < 36; i++) {
            int x = rightX + 4 + (i % 9) * 18;
            int sy = gridY + (i / 9) * 18;
            if (isOver(mouseX, mouseY, x, sy, 18, 18)) {
                ItemStack stack = minecraft.player.getInventory().items.get(i);
                if (stack.isEmpty()) {
                    return true;
                }
                DialogueCondition condition = current();
                if (condition != null && condition.type().equals("held_item")) {
                    heldItemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
                    writeBack();
                } else {

                    carried = stack.copy();
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (typePicker && keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            typePicker = false;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        writeBack();
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
