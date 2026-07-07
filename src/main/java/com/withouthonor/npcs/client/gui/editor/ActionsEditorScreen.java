package com.withouthonor.npcs.client.gui.editor;

import com.withouthonor.npcs.client.ClientFactions;
import com.withouthonor.npcs.client.gui.ScaledScreen;
import com.withouthonor.npcs.client.gui.ScrollDrag;
import com.withouthonor.npcs.client.gui.SelectableEditBox;
import com.withouthonor.npcs.client.gui.VanillaUIHelper;
import com.withouthonor.npcs.common.dialogue.DialogueChoice;
import com.withouthonor.npcs.common.dialogue.EmoteIcon;
import com.withouthonor.npcs.common.dialogue.action.Actions;
import com.withouthonor.npcs.common.dialogue.action.DialogueAction;
import com.withouthonor.npcs.common.dialogue.action.MonologueLine;
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

public class ActionsEditorScreen extends ScaledScreen {

    private static final int PAD = 8;
    private static final int HEADER_H = 22;
    private static final int LEFT_W = 130;
    private static final int ROW_H = 13;
    private static final int COL_STEP = 78;
    private static final ResourceLocation EMOTE_ATLAS =
            ResourceLocation.fromNamespaceAndPath("wh_npcs", "textures/entity/emotes.png");

    private record TypeInfo(String type, String label, String description, String detail) {
    }

    private static final List<TypeInfo> TYPES = List.of(
            new TypeInfo("give_item", "wh_npcs.ui.act.type.give_item.label",
                    "wh_npcs.ui.act.type.give_item.desc", "wh_npcs.ui.act.type.give_item.detail"),
            new TypeInfo("take_item", "wh_npcs.ui.act.type.take_item.label",
                    "wh_npcs.ui.act.type.take_item.desc", "wh_npcs.ui.act.type.take_item.detail"),
            new TypeInfo("set_flag", "wh_npcs.ui.act.type.set_flag.label",
                    "wh_npcs.ui.act.type.set_flag.desc", "wh_npcs.ui.act.type.set_flag.detail"),
            new TypeInfo("reputation", "wh_npcs.ui.act.type.reputation.label",
                    "wh_npcs.ui.act.type.reputation.desc", "wh_npcs.ui.act.type.reputation.detail"),
            new TypeInfo("say", "wh_npcs.ui.act.type.say.label",
                    "wh_npcs.ui.act.type.say.desc", "wh_npcs.ui.act.type.say.detail"),
            new TypeInfo("emote", "wh_npcs.ui.act.type.emote.label",
                    "wh_npcs.ui.act.type.emote.desc", "wh_npcs.ui.act.type.emote.detail"),
            new TypeInfo("emotecraft_emote", "wh_npcs.ui.act.type.emotecraft_emote.label",
                    "wh_npcs.ui.act.type.emotecraft_emote.desc", "wh_npcs.ui.act.type.emotecraft_emote.detail"),
            new TypeInfo("stop_emotecraft_emote", "wh_npcs.ui.act.type.stop_emotecraft_emote.label",
                    "wh_npcs.ui.act.type.stop_emotecraft_emote.desc", "wh_npcs.ui.act.type.stop_emotecraft_emote.detail"),
            new TypeInfo("open_trade", "wh_npcs.ui.act.type.open_trade.label",
                    "wh_npcs.ui.act.type.open_trade.desc", "wh_npcs.ui.act.type.open_trade.detail"),
            new TypeInfo("run_command", "wh_npcs.ui.act.type.run_command.label",
                    "wh_npcs.ui.act.type.run_command.desc", "wh_npcs.ui.act.type.run_command.detail"),
            new TypeInfo("sound", "wh_npcs.ui.act.type.sound.label",
                    "wh_npcs.ui.act.type.sound.desc", "wh_npcs.ui.act.type.sound.detail"),
            new TypeInfo("title", "wh_npcs.ui.act.type.title.label",
                    "wh_npcs.ui.act.type.title.desc", "wh_npcs.ui.act.type.title.detail"),
            new TypeInfo("monologue", "wh_npcs.ui.act.type.monologue.label",
                    "wh_npcs.ui.act.type.monologue.desc", "wh_npcs.ui.act.type.monologue.detail"),
            new TypeInfo("stop_music", "wh_npcs.ui.act.type.stop_music.label",
                    "wh_npcs.ui.act.type.stop_music.desc", "wh_npcs.ui.act.type.stop_music.detail"),
            new TypeInfo("follow", "wh_npcs.ui.act.type.follow.label",
                    "wh_npcs.ui.act.type.follow.desc", "wh_npcs.ui.act.type.follow.detail"),
            new TypeInfo("stop_follow", "wh_npcs.ui.act.type.stop_follow.label",
                    "wh_npcs.ui.act.type.stop_follow.desc", "wh_npcs.ui.act.type.stop_follow.detail"),
            new TypeInfo("follow_wait", "wh_npcs.ui.act.type.follow_wait.label",
                    "wh_npcs.ui.act.type.follow_wait.desc", "wh_npcs.ui.act.type.follow_wait.detail"),
            new TypeInfo("effect", "wh_npcs.ui.act.type.effect.label",
                    "wh_npcs.ui.act.type.effect.desc", "wh_npcs.ui.act.type.effect.detail"),
            new TypeInfo("transform", "wh_npcs.ui.act.type.transform.label",
                    "wh_npcs.ui.act.type.transform.desc", "wh_npcs.ui.act.type.transform.detail"),
            new TypeInfo("attack_player", "wh_npcs.ui.act.type.attack_player.label",
                    "wh_npcs.ui.act.type.attack_player.desc", "wh_npcs.ui.act.type.attack_player.detail"),
            new TypeInfo("edit_profile", "wh_npcs.ui.act.type.edit_profile.label",
                    "wh_npcs.ui.act.type.edit_profile.desc", "wh_npcs.ui.act.type.edit_profile.detail"),
            new TypeInfo("combat_stats", "wh_npcs.ui.act.type.combat_stats.label",
                    "wh_npcs.ui.act.type.combat_stats.desc", "wh_npcs.ui.act.type.combat_stats.detail"));

    private final Screen parent;
    private final List<DialogueAction> actions;

    private int selected = -1;
    private boolean typePicker;
    private int listScroll;
    private final ScrollDrag scrollbars = new ScrollDrag();

    private static final int ACT_SLOTS = 4;

    private final ResourceLocation[] slotItem = new ResourceLocation[ACT_SLOTS];
    private final CompoundTag[] slotNbt = new CompoundTag[ACT_SLOTS];
    private final int[] slotCount = new int[ACT_SLOTS];
    private final EditBox[] countBoxes = new EditBox[ACT_SLOTS];
    private int activeSlot;

    // числовые поля с драг-скрабом и кнопками −/+
    private final java.util.List<com.withouthonor.npcs.client.gui.NumberScrub> scrubs = new java.util.ArrayList<>();

    private EditBox flagBox;
    private boolean flagValue = true;

    private EditBox factionBox;
    private EditBox deltaBox;
    @Nullable
    private String factionTip;

    private EditBox commandBox;

    // «Реплика» — дублирование в чат + КД (0 = без КД) с областью npc/player
    private boolean sayToChat;
    private String sayCdScope = "npc";
    private int sayCooldownSec;
    @Nullable
    private EditBox sayCdBox;

    @Nullable
    private ResourceLocation soundDraft;
    private EditBox volumeBox;
    private EditBox pitchBox;

    private EditBox titleBox;
    private EditBox subtitleBox;
    private EditBox actionbarBox;

    private EmoteIcon emoteDraft = EmoteIcon.EXCLAIM;

    private EditBox emoteIdBox;
    private String emoteDraftId = "";
    private String emoteDraftName = "";
    private String emoteDraftAuthor = "";
    // Режим повтора эмоции: once (по умолчанию) / always / cooldown
    private static final String[] EMOTE_MODE_IDS = {"once", "always", "cooldown"};
    private String emoteModeDraft = "once";
    private int emoteCdDraft = 60;
    @Nullable
    private EditBox emoteCdBox;

    private String effectMode = "apply";
    private boolean effectRemoveAll;
    private List<Actions.EffectSpec> effectSpecs = new ArrayList<>();

    // «Преображение» — файл экспорта и флаг снаряжения
    private String transformFile = "";
    private boolean transformEquipment;

    // «Атаковать игрока» — со-фракционники, все игроки рядом и общий радиус
    private boolean attackAllies;
    private boolean atkAllPlayers;
    private int attackRadius = 16;
    @Nullable
    private EditBox radiusBox;

    // «Изменить профиль» — пустое поле = не менять; RichTextEditor даёт тулбар
    // стилей/цветов по выделению — как у имени/титула во вкладке «Профиль»
    private com.withouthonor.npcs.client.gui.RichTextEditor epNameBox;
    private com.withouthonor.npcs.client.gui.RichTextEditor epTitleBox;
    private String epSkin = "";
    private String epFaction = "";
    @Nullable
    private String epFactionTip;

    // «Боевые параметры» — пустое поле/галочка = не менять
    private static final String[] CS_PRESET_IDS = {"", "passive", "melee", "shield", "bow", "potion"};
    private static final String[] CS_TARGET_IDS =
            {"players", "monsters", "animals", "villagers", "npcs", "factions"};
    private String csPreset = "";
    private boolean csTargetsEnabled;
    private final boolean[] csTargets = new boolean[CS_TARGET_IDS.length];
    private boolean csHeal;
    private EditBox csHpBox;
    private EditBox csDamageBox;
    private EditBox csArmorBox;

    private EditBox monoNameBox;
    private EditBox monoPortraitBox;
    private com.withouthonor.npcs.client.gui.RichTextEditor monoTextEditor;
    private boolean monoLock = true;
    private List<MonologueLine> monoLines = new ArrayList<>();
    private int monoPage;
    private static final int MONO_BTN_W = 18;
    private static final int MONO_BTN_H = 18;

    private int winX, winY, winW, winH;
    private int leftX, leftY, leftH;
    private int rightX, rightW, mainY;
    private int bottomY;

    private ActionsEditorScreen(Screen parent, List<DialogueAction> actions) {
        super(Component.translatable("wh_npcs.ui.actions.window_title"));
        this.parent = parent;
        this.actions = actions;
    }

    public static void open(Screen parent, DialogueChoice choice) {
        open(parent, choice.getActions());
    }

    public static void open(Screen parent, List<DialogueAction> actions) {
        ActionsEditorScreen screen = new ActionsEditorScreen(parent, actions);
        if (!screen.actions.isEmpty()) {
            screen.selected = 0;
            screen.loadDraft();
        }
        Minecraft.getInstance().setScreen(screen);
    }

    @Nullable
    private DialogueAction current() {
        return selected >= 0 && selected < actions.size() ? actions.get(selected) : null;
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

    private int invGridY() {
        return itemsY() + 96;
    }

    private int emoteCellX(int i) {
        return rightX + 4 + i * 26;
    }

    private int emoteRowY() {
        return edY() + 30;
    }

    private int monoNavY() {
        return edY() + 2;
    }

    private int monoPrevX() {
        return rightX + 96;
    }

    private int monoNextX() {
        return rightX + 118;
    }

    private int monoAddX() {
        return rightX + 140;
    }

    private int monoDelX() {
        return rightX + 162;
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
        leftY = winY + HEADER_H + 6;
        bottomY = winY + winH - PAD - 20;
        leftH = bottomY - 6 - leftY;
        rightX = winX + PAD + LEFT_W + PAD;
        rightW = winX + winW - PAD - rightX;
        mainY = leftY;
    }

    @Override
    protected void init() {
        recalc();
        clearWidgets();
        scrubs.clear();
        DialogueAction action = current();
        if (action == null) {
            return;
        }
        switch (action.type()) {
            case "give_item", "take_item" -> {
                for (int i = 0; i < ACT_SLOTS; i++) {
                    final int slot = i;
                    // поле уже + сдвиг на 15px — по бокам кнопки −/+ скраба внутри колонки
                    countBoxes[i] = addRenderableWidget(new SelectableEditBox(font, colX(i) + 15, countY(), 40, 14,
                            Component.translatable("wh_npcs.ui.actions.count_hint")));
                    countBoxes[i].setMaxLength(4);
                    countBoxes[i].setValue(String.valueOf(Math.max(1, slotCount[i])));
                    countBoxes[i].setResponder(v -> {
                        slotCount[slot] = parseInt(countBoxes[slot], 1);
                        writeBack();
                    });
                    scrubs.add(new com.withouthonor.npcs.client.gui.NumberScrub(countBoxes[i], 1, 999, 1, 0.2F,
                            "1", true, () -> setFocused(null)));
                }
            }
            case "set_flag" -> {
                Actions.SetFlag setFlag = (Actions.SetFlag) action;
                flagBox = addRenderableWidget(new SelectableEditBox(font, rightX + 60, edY() + 14, 160, 16,
                        Component.translatable("wh_npcs.ui.actions.flag_field")));
                flagBox.setMaxLength(64);
                flagBox.setValue(setFlag.flag());
                flagBox.setResponder(v -> writeBack());
            }
            case "reputation" -> {
                Actions.Reputation reputation = (Actions.Reputation) action;
                factionBox = addRenderableWidget(new SelectableEditBox(font, rightX + 80, edY() + 14, 150, 16,
                        Component.translatable("wh_npcs.ui.actions.faction_field")));
                factionBox.setMaxLength(32);
                factionBox.setValue(reputation.faction());
                factionBox.setHint(Component.translatable("wh_npcs.ui.actions.faction_hint"));
                StringBuilder available = new StringBuilder(
                        Component.translatable("wh_npcs.ui.actions.faction_tip_title").getString());
                if (ClientFactions.all().isEmpty()) {
                    available.append(Component.translatable("wh_npcs.ui.actions.faction_tip_empty").getString());
                } else {
                    available.append(Component.translatable("wh_npcs.ui.actions.faction_tip_available").getString());
                    for (var info : ClientFactions.all()) {
                        available.append("\n§b").append(info.id()).append(" §8— §7").append(info.name());
                    }
                }
                factionTip = available.toString();
                factionBox.setResponder(v -> writeBack());
                deltaBox = addRenderableWidget(new SelectableEditBox(font, rightX + 80, edY() + 42, 60, 16,
                        Component.translatable("wh_npcs.ui.actions.delta_field")));
                deltaBox.setMaxLength(6);
                deltaBox.setValue(String.valueOf(reputation.delta()));
                deltaBox.setResponder(v -> writeBack());
            }
            case "say" -> {
                Actions.Say say = (Actions.Say) action;
                commandBox = addRenderableWidget(new SelectableEditBox(font, rightX, edY() + 28, rightW - 8, 16,
                        Component.translatable("wh_npcs.ui.actions.say_field")));
                commandBox.setMaxLength(200);
                commandBox.setValue(EditorCodes.toEditor(say.text()));
                commandBox.setHint(Component.translatable("wh_npcs.ui.actions.say_hint"));
                commandBox.setResponder(v -> writeBack());
                // КД повтора: 0 = без КД; сдвиг вправо на 15px — слева от поля кнопка «−» скраба
                sayCdBox = addRenderableWidget(new SelectableEditBox(font, rightX + 55, edY() + 100, 40, 16,
                        Component.translatable("wh_npcs.ui.actions.say_cd_label")));
                sayCdBox.setMaxLength(4);
                sayCdBox.setValue(String.valueOf(sayCooldownSec));
                sayCdBox.setResponder(v -> {
                    sayCooldownSec = Math.max(0, Math.min(3600, parseInt(sayCdBox, 0)));
                    writeBack();
                });
                scrubs.add(new com.withouthonor.npcs.client.gui.NumberScrub(sayCdBox, 0, 3600, 5, 0.5F,
                        "0", true, () -> setFocused(null)));
            }
            case "run_command" -> {
                Actions.RunCommand command = (Actions.RunCommand) action;
                commandBox = addRenderableWidget(new SelectableEditBox(font, rightX, edY() + 28, rightW - 8, 16,
                        Component.translatable("wh_npcs.ui.actions.command_field")));
                commandBox.setMaxLength(256);
                commandBox.setValue(command.command());
                commandBox.setHint(Component.translatable("wh_npcs.ui.actions.command_hint"));
                commandBox.setResponder(v -> writeBack());
            }
            case "emotecraft_emote" -> {
                emoteIdBox = addRenderableWidget(new SelectableEditBox(font, rightX, edY() + 28, rightW - 8, 16,
                        Component.translatable("wh_npcs.ui.actions.emote_field")));
                emoteIdBox.setMaxLength(128);
                emoteIdBox.setValue(emoteDraftId);
                emoteIdBox.setHint(Component.translatable("wh_npcs.ui.actions.emotecraft_hint"));
                emoteIdBox.setResponder(v -> writeBack());
                // Поле секунд КД — только в режиме «По КД» (пересоздание через init, как у attack_player)
                if ("cooldown".equals(emoteModeDraft)) {
                    emoteCdBox = addRenderableWidget(new SelectableEditBox(font, rightX + 210, edY() + 108, 40, 16,
                            Component.translatable("wh_npcs.ui.actions.emote_cd_label")));
                    emoteCdBox.setMaxLength(4);
                    emoteCdBox.setValue(String.valueOf(emoteCdDraft));
                    emoteCdBox.setResponder(v -> {
                        emoteCdDraft = Math.max(1, Math.min(3600, parseInt(emoteCdBox, 60)));
                        writeBack();
                    });
                    scrubs.add(new com.withouthonor.npcs.client.gui.NumberScrub(emoteCdBox, 1, 3600, 5, 0.5F,
                            "60", true, () -> setFocused(null)));
                } else {
                    emoteCdBox = null;
                }
            }
            case "sound" -> {
                Actions.Sound sound = (Actions.Sound) action;
                soundDraft = sound.soundId();
                volumeBox = addRenderableWidget(new SelectableEditBox(font, rightX + 80, edY() + 46, 44, 16,
                        Component.translatable("wh_npcs.ui.actions.volume_field")));
                volumeBox.setMaxLength(4);
                volumeBox.setValue(com.withouthonor.npcs.client.gui.NumberScrub.fmt(sound.volume(), false));
                volumeBox.setResponder(v -> writeBack());
                scrubs.add(new com.withouthonor.npcs.client.gui.NumberScrub(
                        volumeBox, 0.1F, 2.0F, 0.1F, 0.01F, "1", false, () -> setFocused(null)));
                pitchBox = addRenderableWidget(new SelectableEditBox(font, rightX + 210, edY() + 46, 44, 16,
                        Component.translatable("wh_npcs.ui.actions.pitch_field")));
                pitchBox.setMaxLength(4);
                pitchBox.setValue(com.withouthonor.npcs.client.gui.NumberScrub.fmt(sound.pitch(), false));
                pitchBox.setResponder(v -> writeBack());
                scrubs.add(new com.withouthonor.npcs.client.gui.NumberScrub(
                        pitchBox, 0.5F, 2.0F, 0.1F, 0.01F, "1", false, () -> setFocused(null)));
            }
            case "title" -> {
                Actions.Title title = (Actions.Title) action;
                titleBox = addRenderableWidget(new SelectableEditBox(font, rightX + 80, edY() + 14, 150, 16,
                        Component.translatable("wh_npcs.ui.actions.title_field")));
                titleBox.setMaxLength(80);
                titleBox.setValue(EditorCodes.toEditor(title.title() != null ? title.title() : ""));
                titleBox.setResponder(v -> writeBack());
                subtitleBox = addRenderableWidget(new SelectableEditBox(font, rightX + 80, edY() + 42, 150, 16,
                        Component.translatable("wh_npcs.ui.actions.subtitle_field")));
                subtitleBox.setMaxLength(120);
                subtitleBox.setValue(EditorCodes.toEditor(title.subtitle() != null ? title.subtitle() : ""));
                subtitleBox.setResponder(v -> writeBack());
                actionbarBox = addRenderableWidget(new SelectableEditBox(font, rightX + 80, edY() + 70, 150, 16,
                        Component.translatable("wh_npcs.ui.actions.actionbar_field")));
                actionbarBox.setMaxLength(120);
                actionbarBox.setValue(EditorCodes.toEditor(title.actionbar() != null ? title.actionbar() : ""));
                actionbarBox.setResponder(v -> writeBack());
            }
            case "monologue" -> {
                ensureMonoLines();
                MonologueLine line = monoLines.get(monoPage);
                int monoFieldX = rightX + 60;
                monoNameBox = addRenderableWidget(new SelectableEditBox(font, monoFieldX, edY() + 24,
                        rightW - 72, 16, Component.translatable("wh_npcs.ui.actions.mono_name_field")));
                monoNameBox.setMaxLength(64);
                monoNameBox.setValue(EditorCodes.toEditor(line.name()));
                monoNameBox.setHint(Component.translatable("wh_npcs.ui.actions.mono_name_hint"));
                monoNameBox.setResponder(v -> writeBack());
                monoPortraitBox = addRenderableWidget(new SelectableEditBox(font, monoFieldX, edY() + 46,
                        rightW - 72, 16, Component.translatable("wh_npcs.ui.actions.mono_portrait_field")));
                monoPortraitBox.setMaxLength(48);
                monoPortraitBox.setValue(line.portrait());
                monoPortraitBox.setHint(Component.translatable("wh_npcs.ui.actions.mono_portrait_hint"));
                monoPortraitBox.setResponder(v -> writeBack());
                monoTextEditor = addRenderableWidget(new com.withouthonor.npcs.client.gui.RichTextEditor(
                        font, rightX, edY() + 84, rightW - 12, 56));
                monoTextEditor.setValue(line.text());
            }
            case "attack_player" -> {
                // радиус общий: созыв союзников и поиск игроков вокруг инициатора
                if (attackAllies || atkAllPlayers) {
                    // сдвиг вправо на 15px — слева от поля кнопка «−» скраба
                    radiusBox = addRenderableWidget(new SelectableEditBox(font, rightX + 75, edY() + 54, 40, 16,
                            Component.translatable("wh_npcs.ui.actions.atk_radius")));
                    radiusBox.setMaxLength(2);
                    radiusBox.setValue(String.valueOf(attackRadius));
                    radiusBox.setResponder(v -> {
                        attackRadius = Math.max(4, Math.min(48, parseInt(radiusBox, 16)));
                        writeBack();
                    });
                    scrubs.add(new com.withouthonor.npcs.client.gui.NumberScrub(radiusBox, 4, 48, 2, 0.2F,
                            "16", true, () -> setFocused(null)));
                } else {
                    radiusBox = null;
                }
            }
            case "edit_profile" -> {
                Actions.EditProfile ep = (Actions.EditProfile) action;
                // редакторы с тулбаром стилей по выделению — зеркально вкладке «Профиль» NpcEditorScreen:
                // имя — полный набор стилей+цветов, титул — только цвет; значения хранятся с §-кодами
                epNameBox = addRenderableWidget(new com.withouthonor.npcs.client.gui.RichTextEditor(
                        font, rightX + 60, edY() + 2, 168, 18).singleLine());
                epNameBox.setValue(ep.name());
                epNameBox.setHint(Component.translatable("wh_npcs.ui.actions.keep_value"));
                epTitleBox = addRenderableWidget(new com.withouthonor.npcs.client.gui.RichTextEditor(
                        font, rightX + 60, edY() + 26, 168, 18).singleLine().colorOnly());
                epTitleBox.setValue(ep.title());
                epTitleBox.setHint(Component.translatable("wh_npcs.ui.actions.keep_value"));
                // подсказка со списком фракций — как у действия «Репутация»
                StringBuilder available = new StringBuilder(
                        Component.translatable("wh_npcs.ui.actions.faction_tip_title").getString());
                if (ClientFactions.all().isEmpty()) {
                    available.append(Component.translatable("wh_npcs.ui.actions.faction_tip_empty").getString());
                } else {
                    available.append(Component.translatable("wh_npcs.ui.actions.faction_tip_available").getString());
                    for (var info : ClientFactions.all()) {
                        available.append("\n§b").append(info.id()).append(" §8— §7").append(info.name());
                    }
                }
                epFactionTip = available.toString();
            }
            case "combat_stats" -> {
                Actions.CombatStats cs = (Actions.CombatStats) action;
                // поля со сдвигом — по бокам кнопки −/+ скраба; пусто = не менять
                csHpBox = csField(rightX + 50, edY() + 88, cs.hp());
                scrubs.add(new com.withouthonor.npcs.client.gui.NumberScrub(csHpBox, 1, 1024, 5, 0.5F,
                        "", true, () -> setFocused(null)));
                csDamageBox = csField(rightX + 165, edY() + 88, cs.damage());
                scrubs.add(new com.withouthonor.npcs.client.gui.NumberScrub(csDamageBox, 0, 1024, 1, 0.2F,
                        "", true, () -> setFocused(null)));
                csArmorBox = csField(rightX + 290, edY() + 88, cs.armor());
                scrubs.add(new com.withouthonor.npcs.client.gui.NumberScrub(csArmorBox, 0, 30, 1, 0.2F,
                        "", true, () -> setFocused(null)));
            }
            default -> {
            }
        }
    }

    /** Поле боевого параметра: пусто = «не менять». */
    private EditBox csField(int x, int y, @Nullable Float value) {
        EditBox b = addRenderableWidget(new SelectableEditBox(font, x, y, 40, 16, Component.empty()));
        b.setMaxLength(6);
        b.setValue(value != null ? com.withouthonor.npcs.client.gui.NumberScrub.fmt(value, false) : "");
        b.setResponder(v -> writeBack());
        return b;
    }

    private static String presetLabel(String id) {
        return id.isEmpty()
                ? Component.translatable("wh_npcs.ui.actions.keep_value").getString()
                : Component.translatable("wh_npcs.ui.npc.preset_" + id).getString();
    }

    private void loadDraft() {
        for (int i = 0; i < ACT_SLOTS; i++) {
            slotItem[i] = null;
            slotNbt[i] = null;
            slotCount[i] = 1;
        }
        activeSlot = 0;
        sayToChat = false;
        sayCdScope = "npc";
        sayCooldownSec = 0;
        emoteModeDraft = "once";
        emoteCdDraft = 60;
        DialogueAction action = current();
        if (action instanceof Actions.GiveItem give) {
            for (int i = 0; i < Math.min(ACT_SLOTS, give.items().size()); i++) {
                Actions.ItemSpec spec = give.items().get(i);
                slotItem[i] = spec.itemId();
                slotNbt[i] = spec.nbt();
                slotCount[i] = spec.count();
            }
        } else if (action instanceof Actions.TakeItem take) {
            for (int i = 0; i < Math.min(ACT_SLOTS, take.slots().size()); i++) {
                ItemsCondition.Slot slot = take.slots().get(i);
                slotItem[i] = slot.itemId();
                slotNbt[i] = slot.nbt();
                slotCount[i] = slot.count();
            }
        } else if (action instanceof Actions.SetFlag setFlag) {
            flagValue = setFlag.value();
        } else if (action instanceof Actions.Emote emote) {
            emoteDraft = emote.icon();
        } else if (action instanceof Actions.EmotecraftEmote em) {
            emoteDraftId = em.emoteId();
            emoteDraftName = em.emoteName();
            emoteDraftAuthor = em.emoteAuthor();
            emoteModeDraft = em.mode();
            emoteCdDraft = em.cooldownSec();
        } else if (action instanceof Actions.Say say) {
            sayToChat = say.toChat();
            sayCdScope = say.cdScope();
            sayCooldownSec = say.cooldownSec();
        } else if (action instanceof Actions.Effect eff) {
            effectMode = eff.mode();
            effectRemoveAll = eff.removeAll();
            effectSpecs = new ArrayList<>(eff.effects());
        } else if (action instanceof Actions.Sound snd) {
            soundDraft = snd.soundId();
        }
        transformFile = "";
        transformEquipment = false;
        attackAllies = false;
        atkAllPlayers = false;
        attackRadius = 16;
        epSkin = "";
        epFaction = "";
        csPreset = "";
        csTargetsEnabled = false;
        java.util.Arrays.fill(csTargets, false);
        csHeal = false;
        if (action instanceof Actions.Transform tr) {
            transformFile = tr.file();
            transformEquipment = tr.equipment();
        } else if (action instanceof Actions.AttackPlayer ap) {
            attackAllies = ap.allies();
            attackRadius = ap.alliesRadius();
            atkAllPlayers = ap.allPlayers();
        } else if (action instanceof Actions.EditProfile ep) {
            epSkin = ep.skin();
            epFaction = ep.faction();
        } else if (action instanceof Actions.CombatStats cs) {
            csPreset = cs.preset();
            csHeal = cs.heal();
            csTargetsEnabled = !cs.aggroTargets().isBlank();
            for (String t : cs.aggroTargets().split(",")) {
                for (int i = 0; i < CS_TARGET_IDS.length; i++) {
                    if (CS_TARGET_IDS[i].equals(t.trim())) {
                        csTargets[i] = true;
                    }
                }
            }
        }
        monoLines = new ArrayList<>();
        monoPage = 0;
        if (action instanceof Actions.Monologue mono) {
            monoLines.addAll(mono.lines());
            monoLock = mono.lockControl();
        }
    }

    private void ensureMonoLines() {
        if (monoLines.isEmpty() && current() instanceof Actions.Monologue mono) {
            monoLines = new ArrayList<>(mono.lines());
            monoLock = mono.lockControl();
        }
        if (monoLines.isEmpty()) {
            monoLines.add(new MonologueLine("{npc}", "",
                    Component.translatable("wh_npcs.ui.actions.mono_default_text").getString()));
        }
        monoPage = Math.max(0, Math.min(monoPage, monoLines.size() - 1));
    }

    private void monoGoto(int newPage) {
        writeBack();
        monoPage = Math.max(0, Math.min(newPage, monoLines.size() - 1));
        init(minecraft, width, height);
    }

    private void monoAddPage() {
        writeBack();
        MonologueLine cur = monoLines.get(monoPage);
        monoLines.add(monoPage + 1, new MonologueLine(cur.name(), cur.portrait(), ""));
        monoPage++;
        actions.set(selected, new Actions.Monologue(List.copyOf(monoLines), monoLock));
        init(minecraft, width, height);
    }

    private void monoDeletePage() {
        if (monoLines.size() <= 1) {
            return;
        }
        monoLines.remove(monoPage);
        if (monoPage >= monoLines.size()) {
            monoPage = monoLines.size() - 1;
        }
        actions.set(selected, new Actions.Monologue(List.copyOf(monoLines), monoLock));
        init(minecraft, width, height);
    }

    private void writeBack() {
        DialogueAction action = current();
        if (action == null) {
            return;
        }
        try {
            switch (action.type()) {
                case "give_item" -> {
                    List<Actions.ItemSpec> items = new ArrayList<>();
                    for (int i = 0; i < ACT_SLOTS; i++) {
                        if (slotItem[i] != null) {
                            items.add(new Actions.ItemSpec(slotItem[i], Math.max(1, slotCount[i]), slotNbt[i]));
                        }
                    }
                    if (!items.isEmpty()) {
                        actions.set(selected, new Actions.GiveItem(List.copyOf(items)));
                    }
                }
                case "take_item" -> {
                    List<ItemsCondition.Slot> slots = new ArrayList<>();
                    for (int i = 0; i < ACT_SLOTS; i++) {
                        if (slotItem[i] != null) {
                            slots.add(new ItemsCondition.Slot(slotItem[i], null, Math.max(1, slotCount[i]),
                                    slotNbt[i] != null ? ItemsCondition.NbtMode.EXACT : ItemsCondition.NbtMode.IGNORE,
                                    slotNbt[i]));
                        }
                    }
                    if (!slots.isEmpty()) {
                        actions.set(selected, new Actions.TakeItem(List.copyOf(slots)));
                    }
                }
                case "set_flag" -> actions.set(selected,
                        new Actions.SetFlag(flagBox.getValue().trim(), flagValue));
                case "reputation" -> actions.set(selected, new Actions.Reputation(
                        factionBox.getValue().trim(), parseInt(deltaBox, 0)));
                case "say" -> actions.set(selected,
                        new Actions.Say(EditorCodes.fromEditor(commandBox.getValue().trim()),
                                sayToChat, sayCdScope, Math.max(0, Math.min(3600, sayCooldownSec))));
                case "emote" -> actions.set(selected, new Actions.Emote(emoteDraft));
                case "emotecraft_emote" -> {
                    if (emoteIdBox != null) {
                        String typed = emoteIdBox.getValue().trim();
                        if (!typed.equals(emoteDraftId)) {
                            emoteDraftId = typed;
                            emoteDraftName = "";
                            emoteDraftAuthor = "";
                        }
                    }
                    actions.set(selected, new Actions.EmotecraftEmote(
                            emoteDraftId, emoteDraftName, emoteDraftAuthor,
                            emoteModeDraft, Math.max(1, Math.min(3600, emoteCdDraft))));
                }
                case "stop_emotecraft_emote" -> actions.set(selected, new Actions.StopEmotecraftEmote());
                case "run_command" -> actions.set(selected,
                        new Actions.RunCommand(commandBox.getValue().trim()));
                case "sound" -> {
                    if (soundDraft != null) {
                        actions.set(selected, new Actions.Sound(
                                soundDraft,
                                clamp(parseFloat(volumeBox, 1.0F), 0.1F, 2.0F),
                                clamp(parseFloat(pitchBox, 1.0F), 0.5F, 2.0F)));
                    }
                }
                case "title" -> actions.set(selected, new Actions.Title(
                        blankToNull(titleBox), blankToNull(subtitleBox), blankToNull(actionbarBox)));
                case "monologue" -> {
                    if (monoTextEditor != null && monoPage >= 0 && monoPage < monoLines.size()) {
                        monoLines.set(monoPage, new MonologueLine(
                                EditorCodes.fromEditor(monoNameBox.getValue().trim()),
                                monoPortraitBox.getValue().trim(),
                                monoTextEditor.getValue()));
                    }
                    actions.set(selected, new Actions.Monologue(List.copyOf(monoLines), monoLock));
                }
                case "effect" -> actions.set(selected,
                        new Actions.Effect(effectMode, List.copyOf(effectSpecs), effectRemoveAll));
                case "transform" -> actions.set(selected,
                        new Actions.Transform(transformFile, transformEquipment));
                case "attack_player" -> actions.set(selected, new Actions.AttackPlayer(
                        attackAllies, Math.max(4, Math.min(48, attackRadius)), atkAllPlayers));
                case "edit_profile" -> actions.set(selected, new Actions.EditProfile(
                        // RichTextEditor хранит §-коды напрямую — без конверсии EditorCodes
                        epNameBox != null ? epNameBox.getValue().trim() : "",
                        epTitleBox != null ? epTitleBox.getValue().trim() : "",
                        epSkin, epFaction));
                case "combat_stats" -> {
                    StringBuilder targets = new StringBuilder();
                    if (csTargetsEnabled) {
                        for (int i = 0; i < CS_TARGET_IDS.length; i++) {
                            if (csTargets[i]) {
                                if (targets.length() > 0) {
                                    targets.append(',');
                                }
                                targets.append(CS_TARGET_IDS[i]);
                            }
                        }
                    }
                    actions.set(selected, new Actions.CombatStats(csPreset, targets.toString(),
                            parseOptFloat(csHpBox), parseOptFloat(csDamageBox),
                            parseOptFloat(csArmorBox), csHeal));
                }
                default -> {
                }
            }
        } catch (Exception ignored) {

        }
    }

    @Nullable
    private static String blankToNull(EditBox box) {
        String value = box.getValue().trim();
        return value.isEmpty() ? null : EditorCodes.fromEditor(value);
    }

    private static int parseInt(EditBox box, int def) {
        try {
            return Integer.parseInt(box.getValue().trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    /** Пустое/некорректное поле = null («не менять»). */
    @Nullable
    private static Float parseOptFloat(@Nullable EditBox box) {
        if (box == null || box.getValue().trim().isEmpty()) {
            return null;
        }
        try {
            return Float.parseFloat(box.getValue().trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static float parseFloat(EditBox box, float def) {
        try {
            return Float.parseFloat(box.getValue().trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private void addAction(TypeInfo type) {
        DialogueAction created = switch (type.type()) {
            case "give_item" -> new Actions.GiveItem(List.of(new Actions.ItemSpec(
                    ResourceLocation.fromNamespaceAndPath("minecraft", "diamond"), 1, null)));
            case "take_item" -> new Actions.TakeItem(List.of(new ItemsCondition.Slot(
                    ResourceLocation.fromNamespaceAndPath("minecraft", "diamond"), null, 1,
                    ItemsCondition.NbtMode.IGNORE, null)));
            case "set_flag" -> new Actions.SetFlag("my_flag", true);
            case "reputation" -> new Actions.Reputation(
                    ClientFactions.all().isEmpty() ? "example" : ClientFactions.all().get(0).id(), 5);
            case "say" -> new Actions.Say(Component.translatable("wh_npcs.ui.actions.say_default").getString());
            case "emote" -> new Actions.Emote(EmoteIcon.EXCLAIM);
            case "emotecraft_emote" -> new Actions.EmotecraftEmote("");
            case "stop_emotecraft_emote" -> new Actions.StopEmotecraftEmote();
            case "monologue" -> new Actions.Monologue(
                    List.of(new MonologueLine("{npc}", "",
                            Component.translatable("wh_npcs.ui.actions.mono_default_text").getString())), true);
            case "open_trade" -> new Actions.OpenTrade();
            case "stop_music" -> new Actions.StopMusic();
            case "follow" -> new Actions.Follow();
            case "stop_follow" -> new Actions.StopFollow();
            case "follow_wait" -> new Actions.FollowWait();
            case "effect" -> new Actions.Effect("apply", List.of(), false);
            case "transform" -> new Actions.Transform("", false);
            case "attack_player" -> new Actions.AttackPlayer(false, 16, false);
            case "edit_profile" -> new Actions.EditProfile("", "", "", "");
            case "combat_stats" -> new Actions.CombatStats("", "", null, null, null, false);
            case "run_command" -> new Actions.RunCommand(
                    Component.translatable("wh_npcs.ui.actions.command_hint").getString());
            case "sound" -> new Actions.Sound(
                    ResourceLocation.fromNamespaceAndPath("minecraft", "entity.villager.yes"), 1.0F, 1.0F);
            default -> new Actions.Title(
                    Component.translatable("wh_npcs.ui.actions.title_default").getString(), null, null);
        };
        actions.add(created);
        selected = actions.size() - 1;
        loadDraft();
        init(minecraft, width, height);
    }

    @Override
    protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        recalc();
        scrollbars.beginFrame();
        VanillaUIHelper.drawWindow(g, winX, winY, winW, winH);

        g.fill(winX + 2, winY + 2, winX + winW - 2, winY + HEADER_H - 2, VanillaUIHelper.BG_HEADER);
        g.drawString(font, Component.translatable("wh_npcs.ui.actions.header").getString(),
                winX + PAD, winY + 7, VanillaUIHelper.TEXT_YELLOW, false);
        String headerHint = Component.translatable("wh_npcs.ui.actions.header_hint").getString();
        g.drawString(font, headerHint, winX + winW - PAD - font.width(headerHint), winY + 7,
                VanillaUIHelper.TEXT_DARK_GRAY, false);

        renderActionList(g, mouseX, mouseY);
        renderEditor(g, mouseX, mouseY);

        g.drawString(font, Component.translatable("wh_npcs.ui.actions.apply_note").getString(),
                winX + PAD, bottomY + 5, VanillaUIHelper.TEXT_DARK_GRAY, false);
        drawSmall(g, Component.translatable("wh_npcs.ui.common.done").getString(),
                winX + winW - PAD - 80, bottomY, 80, mouseX, mouseY, VanillaUIHelper.TEXT_GREEN);
    }

    @Override
    protected void renderOverlay(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (typePicker) {
            renderTypePicker(g, mouseX, mouseY);
        }
    }

    private int visibleActionRows() {
        return Math.max(1, (leftH - 26) / ROW_H);
    }

    private void renderActionList(GuiGraphics g, int mouseX, int mouseY) {
        VanillaUIHelper.drawContentPanel(g, leftX, leftY, LEFT_W, leftH);
        int visible = visibleActionRows();
        listScroll = Math.max(0, Math.min(listScroll, Math.max(0, actions.size() - visible)));
        int y = leftY + 4;
        for (int i = listScroll; i < Math.min(actions.size(), listScroll + visible); i++) {
            boolean isSelected = i == selected;
            boolean hovered = isOver(mouseX, mouseY, leftX + 2, y, LEFT_W - 4, ROW_H);
            if (isSelected || hovered) {
                g.fill(leftX + 2, y, leftX + LEFT_W - 2, y + ROW_H,
                        isSelected ? VanillaUIHelper.BG_SELECTED : VanillaUIHelper.BG_HOVERED);
            }
            g.drawString(font, font.plainSubstrByWidth(summary(actions.get(i)), LEFT_W - 22),
                    leftX + 4, y + 2, isSelected ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_WHITE, false);
            if (hovered) {
                g.drawString(font, "×", leftX + LEFT_W - 12, y + 2, VanillaUIHelper.TEXT_RED, false);
            }
            y += ROW_H;
        }
        VanillaUIHelper.drawScrollbar(g, leftX + LEFT_W - 6, leftY + 3, visible * ROW_H,
                actions.size(), visible, listScroll, scrollbars, v -> listScroll = v);
        drawSmall(g, Component.translatable("wh_npcs.ui.actions.add").getString(),
                leftX + 2, leftY + leftH - 20, LEFT_W - 4, mouseX, mouseY,
                VanillaUIHelper.TEXT_GREEN);
    }

    public static String summary(DialogueAction action) {
        if (action instanceof Actions.GiveItem give) {
            return Component.translatable("wh_npcs.ui.actions.sum_give", give.items().size()).getString();
        }
        if (action instanceof Actions.TakeItem take) {
            return Component.translatable("wh_npcs.ui.actions.sum_take", take.slots().size()).getString();
        }
        if (action instanceof Actions.SetFlag flag) {
            return Component.translatable("wh_npcs.ui.actions.sum_flag", flag.flag()).getString()
                    + (flag.value() ? "" : Component.translatable("wh_npcs.ui.actions.sum_flag_unset").getString());
        }
        if (action instanceof Actions.Reputation reputation) {
            return Component.translatable("wh_npcs.ui.actions.sum_reputation", reputation.faction()).getString()
                    + (reputation.delta() >= 0 ? " +" : " −") + Math.abs(reputation.delta());
        }
        if (action instanceof Actions.Say say) {
            return Component.translatable("wh_npcs.ui.actions.sum_say", say.text()).getString();
        }
        if (action instanceof Actions.Emote emote) {
            return Component.translatable("wh_npcs.ui.actions.sum_emote",
                    Component.translatable(emote.icon().label()).getString()).getString();
        }
        if (action instanceof Actions.EmotecraftEmote em) {
            String id = em.emoteId();
            int dot = id.lastIndexOf('.');
            String name = id.isEmpty() ? "—" : (dot > 0 ? id.substring(0, dot) : id);
            return Component.translatable("wh_npcs.ui.actions.sum_emotecraft", name).getString();
        }
        if (action instanceof Actions.StopEmotecraftEmote) {
            return Component.translatable("wh_npcs.ui.actions.sum_stop_emote").getString();
        }
        if (action instanceof Actions.Monologue mono) {
            String body = mono.lines().isEmpty()
                    ? Component.translatable("wh_npcs.ui.actions.sum_monologue_empty").getString()
                    : Component.translatable("wh_npcs.ui.actions.sum_monologue_pages",
                            mono.lines().size(), mono.lines().get(0).text()).getString();
            return Component.translatable("wh_npcs.ui.actions.sum_monologue", body).getString();
        }
        if (action instanceof Actions.OpenTrade) {
            return Component.translatable("wh_npcs.ui.actions.sum_trade").getString();
        }
        if (action instanceof Actions.StopMusic) {
            return Component.translatable("wh_npcs.ui.actions.sum_stop_music").getString();
        }
        if (action instanceof Actions.Follow) {
            return Component.translatable("wh_npcs.ui.actions.sum_follow").getString();
        }
        if (action instanceof Actions.StopFollow) {
            return Component.translatable("wh_npcs.ui.actions.sum_stop_follow").getString();
        }
        if (action instanceof Actions.FollowWait) {
            return Component.translatable("wh_npcs.ui.actions.sum_follow_wait").getString();
        }
        if (action instanceof Actions.RunCommand command) {
            return Component.translatable("wh_npcs.ui.actions.sum_command", command.command()).getString();
        }
        if (action instanceof Actions.Sound sound) {
            return Component.translatable("wh_npcs.ui.actions.sum_sound", sound.soundId().getPath()).getString();
        }
        if (action instanceof Actions.Title) {
            return Component.translatable("wh_npcs.ui.actions.sum_title").getString();
        }
        if (action instanceof Actions.Effect eff) {
            if ("remove".equals(eff.mode())) {
                return eff.removeAll()
                        ? Component.translatable("wh_npcs.ui.actions.sum_effect_remove_all").getString()
                        : Component.translatable("wh_npcs.ui.actions.sum_effect_remove",
                                eff.effects().size()).getString();
            }
            return Component.translatable("wh_npcs.ui.actions.sum_effect_apply",
                    eff.effects().size()).getString();
        }
        if (action instanceof Actions.Transform tr) {
            return Component.translatable("wh_npcs.ui.actions.sum_transform",
                    tr.file().isBlank() ? "—" : tr.file()).getString();
        }
        if (action instanceof Actions.AttackPlayer) {
            return Component.translatable("wh_npcs.ui.actions.sum_attack").getString();
        }
        if (action instanceof Actions.EditProfile ep) {
            List<String> parts = new ArrayList<>();
            if (!ep.name().isBlank()) {
                parts.add(Component.translatable("wh_npcs.ui.actions.sum_f_name").getString());
            }
            if (!ep.title().isBlank()) {
                parts.add(Component.translatable("wh_npcs.ui.actions.sum_f_title").getString());
            }
            if (!ep.skin().isBlank()) {
                parts.add(Component.translatable("wh_npcs.ui.actions.sum_f_skin").getString());
            }
            if (!ep.faction().isBlank()) {
                parts.add(Component.translatable("wh_npcs.ui.actions.sum_f_faction").getString());
            }
            return Component.translatable("wh_npcs.ui.actions.sum_profile", parts.isEmpty()
                    ? Component.translatable("wh_npcs.ui.actions.sum_f_none").getString()
                    : String.join(", ", parts)).getString();
        }
        if (action instanceof Actions.CombatStats cs) {
            List<String> parts = new ArrayList<>();
            if (!cs.preset().isBlank()) {
                parts.add(Component.translatable("wh_npcs.ui.actions.sum_f_preset").getString());
            }
            if (!cs.aggroTargets().isBlank()) {
                parts.add(Component.translatable("wh_npcs.ui.actions.sum_f_targets").getString());
            }
            if (cs.hp() != null) {
                parts.add("HP");
            }
            if (cs.damage() != null) {
                parts.add(Component.translatable("wh_npcs.ui.actions.sum_f_damage").getString());
            }
            if (cs.armor() != null) {
                parts.add(Component.translatable("wh_npcs.ui.actions.sum_f_armor").getString());
            }
            if (cs.heal()) {
                parts.add(Component.translatable("wh_npcs.ui.actions.sum_f_heal").getString());
            }
            return Component.translatable("wh_npcs.ui.actions.sum_combat", parts.isEmpty()
                    ? Component.translatable("wh_npcs.ui.actions.sum_f_none").getString()
                    : String.join(", ", parts)).getString();
        }
        return action.type();
    }

    private void renderEditor(GuiGraphics g, int mouseX, int mouseY) {
        DialogueAction action = current();
        if (action == null) {
            g.drawCenteredString(font, Component.translatable("wh_npcs.ui.actions.empty_hint").getString(),
                    rightX + rightW / 2, mainY + 40, VanillaUIHelper.TEXT_WHITE);
            return;
        }
        // панель + заголовок типа действия (верх вровень с левым списком)
        VanillaUIHelper.drawContentPanel(g, rightX - 4, mainY, rightW, bottomY - 6 - mainY);
        TypeInfo info = null;
        for (TypeInfo t : TYPES) {
            if (t.type().equals(action.type())) {
                info = t;
                break;
            }
        }
        int hx = rightX;
        int headMaxW = rightW - 8;
        String headLabel = font.plainSubstrByWidth(info != null
                ? Component.translatable(info.label()).getString() : action.type(), headMaxW);
        g.drawString(font, headLabel, hx, mainY + 5, VanillaUIHelper.TEXT_AQUA, false);
        hx += font.width(headLabel);
        if (info != null) {
            String headDesc = font.plainSubstrByWidth(
                    " — " + Component.translatable(info.description()).getString(), headMaxW - (hx - rightX));
            g.drawString(font, headDesc, hx, mainY + 5, VanillaUIHelper.TEXT_DARK_GRAY, false);
        }
        VanillaUIHelper.drawSeparator(g, rightX, mainY + 16, rightW - 8);
        switch (action.type()) {
            case "give_item", "take_item" -> renderItemsEditor(g, mouseX, mouseY);
            case "set_flag" -> {
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.flag_label").getString(),
                        rightX, edY() + 18, VanillaUIHelper.TEXT_GRAY, false);
                drawSmall(g, flagValue
                                ? Component.translatable("wh_npcs.ui.actions.flag_set").getString()
                                : Component.translatable("wh_npcs.ui.actions.flag_unset").getString(),
                        rightX, edY() + 42, 160, mouseX, mouseY, VanillaUIHelper.TEXT_AQUA);
            }
            case "reputation" -> {
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.faction_label").getString(),
                        rightX, edY() + 18, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.delta_label").getString(),
                        rightX, edY() + 46, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.delta_hint").getString(),
                        rightX + 148, edY() + 46, VanillaUIHelper.TEXT_DARK_GRAY, false);
                if (factionBox != null && factionTip != null && isOver(mouseX, mouseY,
                        factionBox.getX(), factionBox.getY(), factionBox.getWidth(), factionBox.getHeight())) {
                    multilineTooltip(g, factionTip, mouseX, mouseY);
                }
            }
            case "say" -> {
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.say_caption").getString(),
                        rightX, edY() + 14, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.say_note1").getString(),
                        rightX, edY() + 52, VanillaUIHelper.TEXT_WHITE, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.say_note2").getString(),
                        rightX, edY() + 64, VanillaUIHelper.TEXT_WHITE, false);
                // галочка «дублировать в чат» — как у фраз профиля, радиус 24 блока
                int cy = edY() + 80;
                boolean chHover = isOver(mouseX, mouseY, rightX, cy, 200, 12);
                VanillaUIHelper.drawButton(g, rightX, cy, 12, 12, chHover);
                if (sayToChat) {
                    VanillaUIHelper.drawCheck(g, rightX + 1, cy + 2, VanillaUIHelper.TEXT_GREEN);
                }
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.say_to_chat").getString(),
                        rightX + 18, cy + 2,
                        sayToChat ? VanillaUIHelper.TEXT_WHITE : VanillaUIHelper.TEXT_DARK_GRAY, false);
                if (chHover) {
                    multilineTooltip(g, Component.translatable("wh_npcs.ui.actions.say_to_chat_tip").getString(),
                            mouseX, mouseY);
                }
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.say_cd_label").getString(),
                        rightX, edY() + 104, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.say_cd_unit").getString(),
                        rightX + 113, edY() + 104, VanillaUIHelper.TEXT_DARK_GRAY, false);
                if (isOver(mouseX, mouseY, rightX, edY() + 102, 38, 12)) {
                    multilineTooltip(g, Component.translatable("wh_npcs.ui.actions.say_cd_tip").getString(),
                            mouseX, mouseY);
                }
                if (sayCooldownSec > 0) {
                    // область КД видна только при КД > 0
                    drawSmall(g, Component.translatable("player".equals(sayCdScope)
                                    ? "wh_npcs.ui.actions.say_cd_player"
                                    : "wh_npcs.ui.actions.say_cd_npc").getString(),
                            rightX + 160, edY() + 100, 130, mouseX, mouseY, VanillaUIHelper.TEXT_AQUA);
                    if (isOver(mouseX, mouseY, rightX + 160, edY() + 100, 130, 18)) {
                        multilineTooltip(g, Component.translatable(
                                "wh_npcs.ui.actions.say_cd_scope_tip").getString(), mouseX, mouseY);
                    }
                }
            }
            case "emote" -> {
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.emote_caption").getString(),
                        rightX, edY() + 14, VanillaUIHelper.TEXT_GRAY, false);
                EmoteIcon[] icons = EmoteIcon.values();
                int frames = EmoteIcon.COUNT;
                for (int i = 0; i < icons.length; i++) {
                    int x = emoteCellX(i);
                    int y = emoteRowY();
                    boolean sel = icons[i] == emoteDraft;
                    boolean hovered = isOver(mouseX, mouseY, x, y, 24, 24);
                    if (sel || hovered) {
                        g.fill(x, y, x + 24, y + 24,
                                sel ? VanillaUIHelper.BG_SELECTED : VanillaUIHelper.BG_HOVERED);
                    }
                    if (sel) {
                        VanillaUIHelper.drawRaisedFrame(g, x, y, 24, 24);
                    }
                    g.blit(EMOTE_ATLAS, x + 2, y + 2, 20, 20, i * 16f, 0f, 16, 16, frames * 16, 16);
                    if (hovered) {
                        multilineTooltip(g, "§e" + Component.translatable(icons[i].label()).getString(), mouseX, mouseY);
                    }
                }
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.emote_note").getString(),
                        rightX, emoteRowY() + 32, VanillaUIHelper.TEXT_WHITE, false);
            }
            case "emotecraft_emote" -> {
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.emotecraft_caption").getString(),
                        rightX, edY() + 14, VanillaUIHelper.TEXT_GRAY, false);
                drawSmall(g, Component.translatable("wh_npcs.ui.actions.emotecraft_pick").getString(),
                        rightX, edY() + 50, 150, mouseX, mouseY, VanillaUIHelper.TEXT_AQUA);
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.emotecraft_note1").getString(),
                        rightX, edY() + 76, VanillaUIHelper.TEXT_WHITE, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.emotecraft_note2").getString(),
                        rightX, edY() + 88, VanillaUIHelper.TEXT_WHITE, false);
                // режим повтора: Один раз / Каждый раз / По КД (циклическая кнопка)
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.emote_mode_label").getString(),
                        rightX, edY() + 113, VanillaUIHelper.TEXT_GRAY, false);
                drawSmall(g, Component.translatable("wh_npcs.ui.actions.emote_mode_" + emoteModeDraft).getString(),
                        rightX + 60, edY() + 108, 110, mouseX, mouseY, VanillaUIHelper.TEXT_AQUA);
                if (isOver(mouseX, mouseY, rightX + 60, edY() + 108, 110, 18)) {
                    multilineTooltip(g, Component.translatable(
                            "wh_npcs.ui.actions.emote_mode_tip").getString(), mouseX, mouseY);
                }
                if ("cooldown".equals(emoteModeDraft)) {
                    g.drawString(font, Component.translatable("wh_npcs.ui.actions.say_cd_unit").getString(),
                            rightX + 268, edY() + 113, VanillaUIHelper.TEXT_DARK_GRAY, false);
                }
            }
            case "stop_emotecraft_emote" -> {
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.stop_emote_line1").getString(),
                        rightX, edY() + 14, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.stop_emote_line2").getString(),
                        rightX, edY() + 26, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.stop_emote_note").getString(),
                        rightX, edY() + 46, VanillaUIHelper.TEXT_WHITE, false);
            }
            case "open_trade" -> {
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.trade_line1").getString(),
                        rightX, edY() + 14, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.trade_line2").getString(),
                        rightX, edY() + 26, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.trade_note1").getString(),
                        rightX, edY() + 46, VanillaUIHelper.TEXT_WHITE, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.trade_note2").getString(),
                        rightX, edY() + 58, VanillaUIHelper.TEXT_WHITE, false);
            }
            case "stop_music" -> {
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.stop_music_line1").getString(),
                        rightX, edY() + 14, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.stop_music_line2").getString(),
                        rightX, edY() + 26, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.stop_music_note1").getString(),
                        rightX, edY() + 46, VanillaUIHelper.TEXT_WHITE, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.stop_music_note2").getString(),
                        rightX, edY() + 58, VanillaUIHelper.TEXT_WHITE, false);
            }
            case "follow" -> {
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.follow_line1").getString(),
                        rightX, edY() + 14, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.follow_note1").getString(),
                        rightX, edY() + 40, VanillaUIHelper.TEXT_WHITE, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.follow_note2").getString(),
                        rightX, edY() + 52, VanillaUIHelper.TEXT_WHITE, false);
            }
            case "stop_follow" -> {
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.stop_follow_line1").getString(),
                        rightX, edY() + 14, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.stop_follow_line2").getString(),
                        rightX, edY() + 26, VanillaUIHelper.TEXT_GRAY, false);
            }
            case "follow_wait" -> {
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.follow_wait_line1").getString(),
                        rightX, edY() + 14, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.follow_wait_line2").getString(),
                        rightX, edY() + 26, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.follow_wait_note").getString(),
                        rightX, edY() + 46, VanillaUIHelper.TEXT_WHITE, false);
            }
            case "run_command" -> {
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.command_caption").getString(),
                        rightX, edY() + 14, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.command_note1").getString(),
                        rightX, edY() + 52, VanillaUIHelper.TEXT_WHITE, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.command_note2").getString(),
                        rightX, edY() + 64, VanillaUIHelper.TEXT_WHITE, false);
            }
            case "sound" -> {
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.sound_label").getString(),
                        rightX, edY() + 18, VanillaUIHelper.TEXT_GRAY, false);
                String id = soundDraft != null ? soundDraft.toString() : "—";
                g.drawString(font, font.plainSubstrByWidth(id, rightW - 160),
                        rightX + 44, edY() + 18, VanillaUIHelper.TEXT_AQUA, false);
                if (soundDraft != null && isOver(mouseX, mouseY, rightX + 44, edY() + 14, rightW - 160, 16)
                        && font.width(id) > rightW - 160) {
                    multilineTooltip(g, id, mouseX, mouseY);
                }
                drawSmall(g, Component.translatable("wh_npcs.ui.actions.sound_pick").getString(),
                        rightX + rightW - 106, edY() + 12, 98, mouseX, mouseY, VanillaUIHelper.TEXT_AQUA);
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.volume_label").getString(),
                        rightX, edY() + 50, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.pitch_label").getString(),
                        rightX + 160, edY() + 50, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.sound_scrub_note").getString(),
                        rightX, edY() + 74, VanillaUIHelper.TEXT_DARK_GRAY, false);
            }
            case "title" -> {
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.title_label").getString(),
                        rightX, edY() + 18, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.subtitle_label").getString(),
                        rightX, edY() + 46, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.actionbar_label").getString(),
                        rightX, edY() + 74, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.title_note").getString(),
                        rightX, edY() + 96, VanillaUIHelper.TEXT_WHITE, false);
            }
            case "monologue" -> {

                String navLabel = Component.translatable("wh_npcs.ui.actions.mono_page",
                        monoPage + 1, monoLines.size()).getString();
                g.drawString(font, navLabel, rightX, monoNavY() + 5, VanillaUIHelper.TEXT_GRAY, false);
                drawSmall(g, "<", monoPrevX(), monoNavY(), MONO_BTN_W, mouseX, mouseY, VanillaUIHelper.TEXT_WHITE);
                drawSmall(g, ">", monoNextX(), monoNavY(), MONO_BTN_W, mouseX, mouseY, VanillaUIHelper.TEXT_WHITE);
                drawSmall(g, "+", monoAddX(), monoNavY(), MONO_BTN_W, mouseX, mouseY, VanillaUIHelper.TEXT_GREEN);
                drawSmall(g, "-", monoDelX(), monoNavY(), MONO_BTN_W, mouseX, mouseY, VanillaUIHelper.TEXT_RED);

                g.drawString(font, Component.translatable("wh_npcs.ui.actions.mono_name_label").getString(),
                        rightX, edY() + 28, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.mono_portrait_label").getString(),
                        rightX, edY() + 50, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.mono_text_label").getString(),
                        rightX, edY() + 72, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.mono_add_note").getString(),
                        rightX + 96, edY() + 72, VanillaUIHelper.TEXT_WHITE, false);

                int cy = edY() + 150;
                boolean lockHover = isOver(mouseX, mouseY, rightX, cy, 132, 12);
                VanillaUIHelper.drawButton(g, rightX, cy, 12, 12, lockHover);
                if (monoLock) {
                    VanillaUIHelper.drawCheck(g, rightX + 1, cy + 2, VanillaUIHelper.TEXT_GREEN);
                }
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.mono_lock").getString(),
                        rightX + 18, cy + 2,
                        monoLock ? VanillaUIHelper.TEXT_WHITE : VanillaUIHelper.TEXT_DARK_GRAY, false);
            }
            case "effect" -> {
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.effect_caption").getString(),
                        rightX, edY() + 2, VanillaUIHelper.TEXT_GRAY, false);
                boolean apply = !"remove".equals(effectMode);
                boolean applyHover = isOver(mouseX, mouseY, rightX, edY() + 18, 80, 18);
                VanillaUIHelper.drawButton(g, rightX, edY() + 18, 80, 18, applyHover || apply);
                g.drawCenteredString(font, Component.translatable("wh_npcs.ui.actions.effect_apply").getString(),
                        rightX + 40, edY() + 23, apply ? VanillaUIHelper.TEXT_YELLOW
                                : (applyHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_AQUA));
                boolean removeHover = isOver(mouseX, mouseY, rightX + 86, edY() + 18, 80, 18);
                VanillaUIHelper.drawButton(g, rightX + 86, edY() + 18, 80, 18, removeHover || !apply);
                g.drawCenteredString(font, Component.translatable("wh_npcs.ui.actions.effect_remove").getString(),
                        rightX + 126, edY() + 23, !apply ? VanillaUIHelper.TEXT_YELLOW
                                : (removeHover ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_AQUA));
                if (apply) {
                    drawSmall(g, Component.translatable("wh_npcs.ui.actions.effect_pick").getString(),
                            rightX, edY() + 46, 150, mouseX, mouseY, VanillaUIHelper.TEXT_AQUA);
                    g.drawString(font, Component.translatable("wh_npcs.ui.actions.effect_selected",
                            effectSpecs.size()).getString(), rightX, edY() + 70, VanillaUIHelper.TEXT_WHITE, false);
                    g.drawString(font, Component.translatable("wh_npcs.ui.actions.effect_apply_note").getString(),
                            rightX, edY() + 82, VanillaUIHelper.TEXT_DARK_GRAY, false);
                } else {
                    boolean allHover = isOver(mouseX, mouseY, rightX, edY() + 46, 180, 12);
                    VanillaUIHelper.drawButton(g, rightX, edY() + 46, 12, 12, allHover);
                    if (effectRemoveAll) {
                        g.drawCenteredString(font, "§a✓", rightX + 6, edY() + 48, VanillaUIHelper.TEXT_WHITE);
                    }
                    g.drawString(font, Component.translatable("wh_npcs.ui.actions.effect_remove_all").getString(),
                            rightX + 18, edY() + 48,
                            effectRemoveAll ? VanillaUIHelper.TEXT_WHITE : VanillaUIHelper.TEXT_DARK_GRAY, false);
                    if (!effectRemoveAll) {
                        drawSmall(g, Component.translatable("wh_npcs.ui.actions.effect_pick").getString(),
                                rightX, edY() + 68, 150, mouseX, mouseY, VanillaUIHelper.TEXT_AQUA);
                        g.drawString(font, Component.translatable("wh_npcs.ui.actions.effect_selected",
                                effectSpecs.size()).getString(), rightX, edY() + 92,
                                VanillaUIHelper.TEXT_WHITE, false);
                    } else {
                        g.drawString(font, Component.translatable("wh_npcs.ui.actions.effect_remove_all_note")
                                .getString(), rightX, edY() + 68, VanillaUIHelper.TEXT_DARK_GRAY, false);
                    }
                }
            }
            case "transform" -> {
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.tf_file_label").getString(),
                        rightX, edY() + 2, VanillaUIHelper.TEXT_GRAY, false);
                String file = transformFile.isBlank() ? "—" : transformFile;
                g.drawString(font, font.plainSubstrByWidth(file, rightW - 88),
                        rightX + 80, edY() + 2, VanillaUIHelper.TEXT_AQUA, false);
                if (!transformFile.isBlank() && font.width(file) > rightW - 88
                        && isOver(mouseX, mouseY, rightX + 80, edY() - 2, rightW - 88, 12)) {
                    multilineTooltip(g, file, mouseX, mouseY);
                }
                drawSmall(g, Component.translatable("wh_npcs.ui.actions.tf_pick").getString(),
                        rightX, edY() + 18, 150, mouseX, mouseY, VanillaUIHelper.TEXT_AQUA);
                int cy = edY() + 46;
                boolean eqHover = isOver(mouseX, mouseY, rightX, cy, 200, 12);
                VanillaUIHelper.drawButton(g, rightX, cy, 12, 12, eqHover);
                if (transformEquipment) {
                    VanillaUIHelper.drawCheck(g, rightX + 1, cy + 2, VanillaUIHelper.TEXT_GREEN);
                }
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.tf_equipment").getString(),
                        rightX + 18, cy + 2,
                        transformEquipment ? VanillaUIHelper.TEXT_WHITE : VanillaUIHelper.TEXT_DARK_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.tf_note1").getString(),
                        rightX, edY() + 68, VanillaUIHelper.TEXT_WHITE, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.tf_note2").getString(),
                        rightX, edY() + 80, VanillaUIHelper.TEXT_WHITE, false);
            }
            case "attack_player" -> {
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.atk_caption").getString(),
                        rightX, edY() + 2, VanillaUIHelper.TEXT_GRAY, false);
                int cy = edY() + 20;
                boolean alHover = isOver(mouseX, mouseY, rightX, cy, 200, 12);
                VanillaUIHelper.drawButton(g, rightX, cy, 12, 12, alHover);
                if (attackAllies) {
                    VanillaUIHelper.drawCheck(g, rightX + 1, cy + 2, VanillaUIHelper.TEXT_GREEN);
                }
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.atk_allies").getString(),
                        rightX + 18, cy + 2,
                        attackAllies ? VanillaUIHelper.TEXT_WHITE : VanillaUIHelper.TEXT_DARK_GRAY, false);
                // вторая галочка — атаковать всех игроков в радиусе от инициатора
                int ay = edY() + 36;
                boolean apHover = isOver(mouseX, mouseY, rightX, ay, 200, 12);
                VanillaUIHelper.drawButton(g, rightX, ay, 12, 12, apHover);
                if (atkAllPlayers) {
                    VanillaUIHelper.drawCheck(g, rightX + 1, ay + 2, VanillaUIHelper.TEXT_GREEN);
                }
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.atk_all").getString(),
                        rightX + 18, ay + 2,
                        atkAllPlayers ? VanillaUIHelper.TEXT_WHITE : VanillaUIHelper.TEXT_DARK_GRAY, false);
                if (apHover) {
                    multilineTooltip(g, Component.translatable("wh_npcs.ui.actions.atk_all_tip").getString(),
                            mouseX, mouseY);
                }
                if (attackAllies || atkAllPlayers) {
                    g.drawString(font, Component.translatable("wh_npcs.ui.actions.atk_radius").getString(),
                            rightX + 18, edY() + 58, VanillaUIHelper.TEXT_GRAY, false);
                    g.drawString(font, Component.translatable("wh_npcs.ui.actions.atk_radius_unit").getString(),
                            rightX + 132, edY() + 58, VanillaUIHelper.TEXT_DARK_GRAY, false);
                }
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.atk_note1").getString(),
                        rightX, edY() + 80, VanillaUIHelper.TEXT_WHITE, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.atk_note2").getString(),
                        rightX, edY() + 92, VanillaUIHelper.TEXT_WHITE, false);
            }
            case "edit_profile" -> {
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.ep_name").getString(),
                        rightX, edY() + 6, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.ep_title").getString(),
                        rightX, edY() + 28, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.ep_skin").getString(),
                        rightX, edY() + 50, VanillaUIHelper.TEXT_GRAY, false);
                String skin = epSkin.isBlank()
                        ? Component.translatable("wh_npcs.ui.actions.keep_value").getString() : epSkin;
                g.drawString(font, font.plainSubstrByWidth(skin, rightW - 160),
                        rightX + 60, edY() + 50, VanillaUIHelper.TEXT_AQUA, false);
                drawSmall(g, Component.translatable("wh_npcs.ui.actions.ep_pick_skin").getString(),
                        rightX + rightW - 90, edY() + 46, 82, mouseX, mouseY, VanillaUIHelper.TEXT_AQUA);
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.ep_faction").getString(),
                        rightX, edY() + 74, VanillaUIHelper.TEXT_GRAY, false);
                String fac = epFaction.isBlank()
                        ? Component.translatable("wh_npcs.ui.actions.keep_value").getString() : epFaction;
                drawSmall(g, fac, rightX + 60, edY() + 68, 168, mouseX, mouseY, VanillaUIHelper.TEXT_AQUA);
                if (epFactionTip != null && isOver(mouseX, mouseY, rightX + 60, edY() + 68, 168, 18)) {
                    multilineTooltip(g, epFactionTip, mouseX, mouseY);
                }
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.ep_note").getString(),
                        rightX, edY() + 96, VanillaUIHelper.TEXT_WHITE, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.ep_style_note").getString(),
                        rightX, edY() + 108, VanillaUIHelper.TEXT_WHITE, false);
            }
            case "combat_stats" -> {
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.cs_preset").getString(),
                        rightX, edY() + 7, VanillaUIHelper.TEXT_GRAY, false);
                drawSmall(g, presetLabel(csPreset), rightX + 60, edY() + 2, 130,
                        mouseX, mouseY, VanillaUIHelper.TEXT_AQUA);
                int cy = edY() + 26;
                boolean tgHover = isOver(mouseX, mouseY, rightX, cy, 200, 12);
                VanillaUIHelper.drawButton(g, rightX, cy, 12, 12, tgHover);
                if (csTargetsEnabled) {
                    VanillaUIHelper.drawCheck(g, rightX + 1, cy + 2, VanillaUIHelper.TEXT_GREEN);
                }
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.cs_targets").getString(),
                        rightX + 18, cy + 2,
                        csTargetsEnabled ? VanillaUIHelper.TEXT_WHITE : VanillaUIHelper.TEXT_DARK_GRAY, false);
                if (tgHover) {
                    multilineTooltip(g, Component.translatable("wh_npcs.ui.actions.cs_targets_tip").getString(),
                            mouseX, mouseY);
                }
                if (csTargetsEnabled) {
                    // 2 колонки «кого атаковать» — как в CombatPresetScreen
                    for (int i = 0; i < CS_TARGET_IDS.length; i++) {
                        int cx = rightX + 8 + (i % 2) * 150;
                        int ty = edY() + 42 + (i / 2) * 14;
                        boolean h = isOver(mouseX, mouseY, cx, ty, 140, 12);
                        VanillaUIHelper.drawButton(g, cx, ty, 12, 12, h);
                        if (csTargets[i]) {
                            VanillaUIHelper.drawCheck(g, cx + 1, ty + 2, VanillaUIHelper.TEXT_GREEN);
                        }
                        g.drawString(font, Component.translatable(
                                        "wh_npcs.ui.combat_preset.agg." + CS_TARGET_IDS[i]).getString(),
                                cx + 16, ty + 2,
                                csTargets[i] ? VanillaUIHelper.TEXT_WHITE : VanillaUIHelper.TEXT_DARK_GRAY, false);
                    }
                }
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.cs_hp").getString(),
                        rightX, edY() + 92, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.cs_damage").getString(),
                        rightX + 115, edY() + 92, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.cs_armor").getString(),
                        rightX + 238, edY() + 92, VanillaUIHelper.TEXT_GRAY, false);
                int hy = edY() + 112;
                boolean healHover = isOver(mouseX, mouseY, rightX, hy, 200, 12);
                VanillaUIHelper.drawButton(g, rightX, hy, 12, 12, healHover);
                if (csHeal) {
                    VanillaUIHelper.drawCheck(g, rightX + 1, hy + 2, VanillaUIHelper.TEXT_GREEN);
                }
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.cs_heal").getString(),
                        rightX + 18, hy + 2,
                        csHeal ? VanillaUIHelper.TEXT_WHITE : VanillaUIHelper.TEXT_DARK_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.cs_note").getString(),
                        rightX, edY() + 132, VanillaUIHelper.TEXT_WHITE, false);
            }
            default -> g.drawString(font,
                    Component.translatable("wh_npcs.ui.actions.json_only", action.type()).getString(),
                    rightX, edY() + 18, VanillaUIHelper.TEXT_STATUS, false);
        }
        if (!typePicker) {
            for (var s : scrubs) {
                s.render(g, font, mouseX, mouseY);
            }
        }
    }

    private void renderItemsEditor(GuiGraphics g, int mouseX, int mouseY) {
        for (int i = 0; i < ACT_SLOTS; i++) {
            int x = colX(i);
            g.drawCenteredString(font, Component.translatable("wh_npcs.ui.actions.slot", i + 1).getString(),
                    x + 35, itemsY() + 14,
                    i == activeSlot ? VanillaUIHelper.TEXT_YELLOW : VanillaUIHelper.TEXT_DARK_GRAY);
            int slotX = x + 26;
            VanillaUIHelper.drawItemSlot(g, slotX, slotRowY(), i == activeSlot);
            if (i == activeSlot) {
                VanillaUIHelper.drawRaisedFrame(g, slotX - 2, slotRowY() - 2, 22, 22);
            }
            if (slotItem[i] != null) {
                renderGhostItem(g, slotItem[i], slotNbt[i], slotX + 1, slotRowY() + 1);
                if (slotNbt[i] != null) {
                    g.drawString(font, "*", slotX + 14, slotRowY() - 2, VanillaUIHelper.TEXT_AQUA, false);
                    if (isOver(mouseX, mouseY, slotX + 13, slotRowY() - 3, 8, 8)) {
                        multilineTooltip(g,
                                Component.translatable("wh_npcs.ui.actions.nbt_tip").getString(), mouseX, mouseY);
                    }
                }
            }
        }
        VanillaUIHelper.drawSeparator(g, rightX, invGridY() - 16, rightW - 8);
        g.drawString(font, Component.translatable("wh_npcs.ui.actions.inv_caption").getString(),
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
        Item item = ForgeRegistries.ITEMS.getValue(itemId);
        if (item == null) {
            return;
        }
        ItemStack stack = new ItemStack(item);
        if (nbt != null) {
            stack.setTag(nbt.copy());
        }
        g.renderItem(stack, x, y);
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
        g.drawCenteredString(font, Component.translatable("wh_npcs.ui.actions.type_picker_title").getString(),
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
                        addAction(TYPES.get(i));
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
            // плавающий тулбар RichTextEditor рисуется поверх соседних полей —
            // отдаём ему клик раньше остальных зон (как в DialogueEditorScreen)
            if (getFocused() instanceof com.withouthonor.npcs.client.gui.RichTextEditor rte
                    && rte.clickToolbar(mouseX, mouseY)) {
                return true;
            }
            for (var s : scrubs) {
                if (s.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
            }
            int visible = visibleActionRows();
            int y = leftY + 4;
            for (int i = listScroll; i < Math.min(actions.size(), listScroll + visible); i++) {
                if (isOver(mouseX, mouseY, leftX + 2, y, LEFT_W - 4, ROW_H)) {
                    if (isOver(mouseX, mouseY, leftX + LEFT_W - 16, y, 14, ROW_H)) {
                        actions.remove(i);
                        selected = actions.isEmpty() ? -1 : Math.min(selected, actions.size() - 1);
                        loadDraft();
                        init(minecraft, width, height);
                    } else if (i != selected) {
                        writeBack();
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
        } else if (button == 1) {
            for (var s : scrubs) {
                if (s.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
            }
            if (handleEditorClick(mouseX, mouseY, true)) {
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
                    Math.max(0, actions.size() - visibleActionRows())));
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
        DialogueAction action = current();
        if (action == null) {
            return false;
        }
        switch (action.type()) {
            case "sound" -> {
                if (!rightClick && isOver(mouseX, mouseY, rightX + rightW - 106, edY() + 12, 98, 18)
                        && minecraft != null) {
                    float p = clamp(parseFloat(pitchBox, 1.0F), 0.5F, 2.0F);
                    minecraft.setScreen(new VoicePickerScreen(this,
                            soundDraft != null ? soundDraft.toString() : "", p, (id, pitch) -> {
                        if (id != null && !id.isBlank()) {
                            ResourceLocation parsed = ResourceLocation.tryParse(id);
                            if (parsed != null) {
                                soundDraft = parsed;
                            }
                        }
                        if (pitchBox != null) {
                            pitchBox.setValue(com.withouthonor.npcs.client.gui.NumberScrub.fmt(
                                    clamp(pitch, 0.5F, 2.0F), false));
                        }
                        writeBack();
                    }));
                    return true;
                }
            }
            case "set_flag" -> {
                if (!rightClick && isOver(mouseX, mouseY, rightX, edY() + 42, 160, 18)) {
                    flagValue = !flagValue;
                    writeBack();
                    return true;
                }
            }
            case "say" -> {
                if (!rightClick && isOver(mouseX, mouseY, rightX, edY() + 80, 200, 12)) {
                    sayToChat = !sayToChat;
                    writeBack();
                    return true;
                }
                if (!rightClick && sayCooldownSec > 0
                        && isOver(mouseX, mouseY, rightX + 160, edY() + 100, 130, 18)) {
                    sayCdScope = "player".equals(sayCdScope) ? "npc" : "player";
                    writeBack();
                    return true;
                }
            }
            case "emote" -> {
                if (!rightClick) {
                    EmoteIcon[] icons = EmoteIcon.values();
                    for (int i = 0; i < icons.length; i++) {
                        if (isOver(mouseX, mouseY, emoteCellX(i), emoteRowY(), 24, 24)) {
                            emoteDraft = icons[i];
                            writeBack();
                            return true;
                        }
                    }
                }
            }
            case "emotecraft_emote" -> {
                if (!rightClick && isOver(mouseX, mouseY, rightX, edY() + 50, 150, 18)) {
                    writeBack();
                    if (minecraft != null) {
                        int idx = selected;
                        minecraft.setScreen(EmotecraftScreen.forPicker(this, ref -> {
                            emoteDraftId = ref.id();
                            emoteDraftName = ref.name();
                            emoteDraftAuthor = ref.author();
                            if (idx >= 0 && idx < actions.size()) {
                                actions.set(idx, new Actions.EmotecraftEmote(
                                        emoteDraftId, emoteDraftName, emoteDraftAuthor,
                                        emoteModeDraft, emoteCdDraft));
                            }
                        }));
                    }
                    return true;
                }
                if (!rightClick && isOver(mouseX, mouseY, rightX + 60, edY() + 108, 110, 18)) {
                    // цикл режима повтора; пересоздаём виджеты — поле КД появляется/уходит
                    int cur = 0;
                    for (int i = 0; i < EMOTE_MODE_IDS.length; i++) {
                        if (EMOTE_MODE_IDS[i].equals(emoteModeDraft)) {
                            cur = i;
                        }
                    }
                    emoteModeDraft = EMOTE_MODE_IDS[(cur + 1) % EMOTE_MODE_IDS.length];
                    writeBack();
                    init(minecraft, width, height);
                    return true;
                }
            }
            case "monologue" -> {
                if (!rightClick) {
                    int ny = monoNavY();
                    if (isOver(mouseX, mouseY, monoPrevX(), ny, MONO_BTN_W, MONO_BTN_H)) {
                        monoGoto(monoPage - 1);
                        return true;
                    }
                    if (isOver(mouseX, mouseY, monoNextX(), ny, MONO_BTN_W, MONO_BTN_H)) {
                        monoGoto(monoPage + 1);
                        return true;
                    }
                    if (isOver(mouseX, mouseY, monoAddX(), ny, MONO_BTN_W, MONO_BTN_H)) {
                        monoAddPage();
                        return true;
                    }
                    if (isOver(mouseX, mouseY, monoDelX(), ny, MONO_BTN_W, MONO_BTN_H)) {
                        monoDeletePage();
                        return true;
                    }
                    if (isOver(mouseX, mouseY, rightX, edY() + 150, 132, 12)) {
                        monoLock = !monoLock;
                        writeBack();
                        return true;
                    }
                }
            }
            case "effect" -> {
                if (rightClick) {
                    return false;
                }
                if (isOver(mouseX, mouseY, rightX, edY() + 18, 80, 18)) {
                    effectMode = "apply";
                    writeBack();
                    return true;
                }
                if (isOver(mouseX, mouseY, rightX + 86, edY() + 18, 80, 18)) {
                    effectMode = "remove";
                    writeBack();
                    return true;
                }
                boolean apply = !"remove".equals(effectMode);
                if (apply) {
                    if (isOver(mouseX, mouseY, rightX, edY() + 46, 150, 18)) {
                        writeBack();
                        if (minecraft != null) {
                            minecraft.setScreen(EffectPickerScreen.forApply(this, effectSpecs, this::writeBack));
                        }
                        return true;
                    }
                } else {
                    if (isOver(mouseX, mouseY, rightX, edY() + 46, 180, 12)) {
                        effectRemoveAll = !effectRemoveAll;
                        writeBack();
                        return true;
                    }
                    if (!effectRemoveAll && isOver(mouseX, mouseY, rightX, edY() + 68, 150, 18)) {
                        writeBack();
                        if (minecraft != null) {
                            List<ResourceLocation> ids = new ArrayList<>();
                            for (Actions.EffectSpec spec : effectSpecs) {
                                ids.add(spec.id());
                            }
                            minecraft.setScreen(new EffectPickerScreen(this, ids, () -> {
                                List<Actions.EffectSpec> rebuilt = new ArrayList<>();
                                for (ResourceLocation id : ids) {
                                    rebuilt.add(new Actions.EffectSpec(id, 600, 0));
                                }
                                effectSpecs = rebuilt;
                                writeBack();
                            }));
                        }
                        return true;
                    }
                }
            }
            case "give_item", "take_item" -> {
                for (int i = 0; i < ACT_SLOTS; i++) {
                    if (isOver(mouseX, mouseY, colX(i) + 26, slotRowY(), 18, 18)) {
                        if (rightClick) {
                            slotItem[i] = null;
                            slotNbt[i] = null;
                            writeBack();
                        } else {
                            activeSlot = i;
                        }
                        return true;
                    }
                }
                if (!rightClick && minecraft != null && minecraft.player != null) {
                    for (int i = 0; i < 36; i++) {
                        int x = rightX + 4 + (i % 9) * 18;
                        int sy = invGridY() + (i / 9) * 18;
                        if (isOver(mouseX, mouseY, x, sy, 18, 18)) {
                            ItemStack stack = minecraft.player.getInventory().items.get(i);
                            if (!stack.isEmpty()) {
                                slotItem[activeSlot] = ForgeRegistries.ITEMS.getKey(stack.getItem());
                                slotNbt[activeSlot] = stack.getTag() != null ? stack.getTag().copy() : null;
                                writeBack();
                            }
                            return true;
                        }
                    }
                }
            }
            case "transform" -> {
                if (rightClick) {
                    return false;
                }
                if (isOver(mouseX, mouseY, rightX, edY() + 18, 150, 18)) {
                    writeBack();
                    int idx = selected;
                    // одноразовый колбэк: RequestList → openImport откроет пикер в режиме выбора
                    com.withouthonor.npcs.client.ClientNetHandlers.filePickCallback = name -> {
                        transformFile = name;
                        if (idx >= 0 && idx < actions.size()) {
                            actions.set(idx, new Actions.Transform(name, transformEquipment));
                        }
                    };
                    com.withouthonor.npcs.network.NetworkHandler.sendToServer(
                            new com.withouthonor.npcs.network.ProfileSharePackets.RequestList());
                    return true;
                }
                if (isOver(mouseX, mouseY, rightX, edY() + 46, 200, 12)) {
                    transformEquipment = !transformEquipment;
                    writeBack();
                    return true;
                }
            }
            case "attack_player" -> {
                if (!rightClick && isOver(mouseX, mouseY, rightX, edY() + 20, 200, 12)) {
                    attackAllies = !attackAllies;
                    writeBack();
                    init(minecraft, width, height);
                    return true;
                }
                if (!rightClick && isOver(mouseX, mouseY, rightX, edY() + 36, 200, 12)) {
                    atkAllPlayers = !atkAllPlayers;
                    writeBack();
                    init(minecraft, width, height);
                    return true;
                }
            }
            case "edit_profile" -> {
                if (!rightClick && isOver(mouseX, mouseY, rightX + rightW - 90, edY() + 46, 82, 18)) {
                    writeBack();
                    int idx = selected;
                    if (minecraft != null) {
                        minecraft.setScreen(new SkinLibraryScreen(this, epSkin, spec -> {
                            epSkin = spec == null ? "" : spec;
                            if (idx >= 0 && idx < actions.size()
                                    && actions.get(idx) instanceof Actions.EditProfile ep) {
                                actions.set(idx, new Actions.EditProfile(
                                        ep.name(), ep.title(), epSkin, ep.faction()));
                            }
                        }));
                    }
                    return true;
                }
                if (isOver(mouseX, mouseY, rightX + 60, edY() + 68, 168, 18)) {
                    var all = ClientFactions.all();
                    if (rightClick) {
                        epFaction = "";
                    } else if (!all.isEmpty()) {
                        int cur = -1;
                        for (int i = 0; i < all.size(); i++) {
                            if (all.get(i).id().equals(epFaction)) {
                                cur = i;
                            }
                        }
                        epFaction = cur + 1 >= all.size() ? "" : all.get(cur + 1).id();
                    }
                    writeBack();
                    return true;
                }
            }
            case "combat_stats" -> {
                if (rightClick) {
                    return false;
                }
                if (isOver(mouseX, mouseY, rightX + 60, edY() + 2, 130, 18)) {
                    int cur = 0;
                    for (int i = 0; i < CS_PRESET_IDS.length; i++) {
                        if (CS_PRESET_IDS[i].equals(csPreset)) {
                            cur = i;
                        }
                    }
                    csPreset = CS_PRESET_IDS[(cur + 1) % CS_PRESET_IDS.length];
                    writeBack();
                    return true;
                }
                if (isOver(mouseX, mouseY, rightX, edY() + 26, 200, 12)) {
                    csTargetsEnabled = !csTargetsEnabled;
                    writeBack();
                    return true;
                }
                if (csTargetsEnabled) {
                    for (int i = 0; i < CS_TARGET_IDS.length; i++) {
                        int cx = rightX + 8 + (i % 2) * 150;
                        int ty = edY() + 42 + (i / 2) * 14;
                        if (isOver(mouseX, mouseY, cx, ty, 140, 12)) {
                            csTargets[i] = !csTargets[i];
                            writeBack();
                            return true;
                        }
                    }
                }
                if (isOver(mouseX, mouseY, rightX, edY() + 112, 200, 12)) {
                    csHeal = !csHeal;
                    writeBack();
                    return true;
                }
            }
            default -> {
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
        // Ожидающий колбэк выбора файла не должен пережить экран: иначе следующий
        // ListResult любого другого потока (импорт/спавн) уйдёт в мёртвый экран.
        com.withouthonor.npcs.client.ClientNetHandlers.filePickCallback = null;
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
