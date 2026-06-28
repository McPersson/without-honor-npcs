package com.withouthonor.npcs.client.gui.editor;

import com.withouthonor.npcs.client.ClientFactions;
import com.withouthonor.npcs.client.gui.ScaledScreen;
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
                    "wh_npcs.ui.act.type.follow_wait.desc", "wh_npcs.ui.act.type.follow_wait.detail"));

    private final Screen parent;
    private final List<DialogueAction> actions;

    private int selected = -1;
    private boolean typePicker;

    private final ResourceLocation[] slotItem = new ResourceLocation[3];
    private final CompoundTag[] slotNbt = new CompoundTag[3];
    private final int[] slotCount = new int[3];
    private final EditBox[] countBoxes = new EditBox[3];
    private int activeSlot;

    private EditBox flagBox;
    private boolean flagValue = true;

    private EditBox factionBox;
    private EditBox deltaBox;

    private EditBox commandBox;

    private EditBox soundBox;
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

    private int slotRowY() {
        return mainY + 26;
    }

    private int countY() {
        return mainY + 50;
    }

    private int invGridY() {
        return mainY + 96;
    }

    private int emoteCellX(int i) {
        return rightX + 4 + i * 26;
    }

    private int emoteRowY() {
        return mainY + 30;
    }

    private int monoNavY() {
        return mainY + 2;
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
        DialogueAction action = current();
        if (action == null) {
            return;
        }
        switch (action.type()) {
            case "give_item", "take_item" -> {
                for (int i = 0; i < 3; i++) {
                    final int slot = i;
                    countBoxes[i] = addRenderableWidget(new SelectableEditBox(font, colX(i), countY(), 48, 16,
                            Component.translatable("wh_npcs.ui.actions.count_hint")));
                    countBoxes[i].setMaxLength(4);
                    countBoxes[i].setValue(String.valueOf(Math.max(1, slotCount[i])));
                    countBoxes[i].setResponder(v -> {
                        slotCount[slot] = parseInt(countBoxes[slot], 1);
                        writeBack();
                    });
                }
            }
            case "set_flag" -> {
                Actions.SetFlag setFlag = (Actions.SetFlag) action;
                flagBox = addRenderableWidget(new SelectableEditBox(font, rightX + 60, mainY + 14, 160, 16,
                        Component.translatable("wh_npcs.ui.actions.flag_field")));
                flagBox.setMaxLength(64);
                flagBox.setValue(setFlag.flag());
                flagBox.setResponder(v -> writeBack());
            }
            case "reputation" -> {
                Actions.Reputation reputation = (Actions.Reputation) action;
                factionBox = addRenderableWidget(new SelectableEditBox(font, rightX + 80, mainY + 14, 150, 16,
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
                factionBox.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                        Component.literal(available.toString())));
                factionBox.setResponder(v -> writeBack());
                deltaBox = addRenderableWidget(new SelectableEditBox(font, rightX + 80, mainY + 42, 60, 16,
                        Component.translatable("wh_npcs.ui.actions.delta_field")));
                deltaBox.setMaxLength(6);
                deltaBox.setValue(String.valueOf(reputation.delta()));
                deltaBox.setResponder(v -> writeBack());
            }
            case "say" -> {
                Actions.Say say = (Actions.Say) action;
                commandBox = addRenderableWidget(new SelectableEditBox(font, rightX, mainY + 28, rightW - 8, 16,
                        Component.translatable("wh_npcs.ui.actions.say_field")));
                commandBox.setMaxLength(200);
                commandBox.setValue(EditorCodes.toEditor(say.text()));
                commandBox.setHint(Component.translatable("wh_npcs.ui.actions.say_hint"));
                commandBox.setResponder(v -> writeBack());
            }
            case "run_command" -> {
                Actions.RunCommand command = (Actions.RunCommand) action;
                commandBox = addRenderableWidget(new SelectableEditBox(font, rightX, mainY + 28, rightW - 8, 16,
                        Component.translatable("wh_npcs.ui.actions.command_field")));
                commandBox.setMaxLength(256);
                commandBox.setValue(command.command());
                commandBox.setHint(Component.translatable("wh_npcs.ui.actions.command_hint"));
                commandBox.setResponder(v -> writeBack());
            }
            case "emotecraft_emote" -> {
                emoteIdBox = addRenderableWidget(new SelectableEditBox(font, rightX, mainY + 28, rightW - 8, 16,
                        Component.translatable("wh_npcs.ui.actions.emote_field")));
                emoteIdBox.setMaxLength(128);
                emoteIdBox.setValue(emoteDraftId);
                emoteIdBox.setHint(Component.translatable("wh_npcs.ui.actions.emotecraft_hint"));
                emoteIdBox.setResponder(v -> writeBack());
            }
            case "sound" -> {
                Actions.Sound sound = (Actions.Sound) action;
                soundBox = addRenderableWidget(new SelectableEditBox(font, rightX + 44, mainY + 14, 186, 16,
                        Component.translatable("wh_npcs.ui.actions.sound_field")));
                soundBox.setMaxLength(120);
                soundBox.setValue(sound.soundId().toString());
                soundBox.setHint(Component.translatable("wh_npcs.ui.actions.sound_hint"));
                soundBox.setResponder(v -> writeBack());
                volumeBox = addRenderableWidget(new SelectableEditBox(font, rightX + 80, mainY + 42, 44, 16,
                        Component.translatable("wh_npcs.ui.actions.volume_field")));
                volumeBox.setMaxLength(4);
                volumeBox.setValue(String.valueOf(sound.volume()));
                volumeBox.setResponder(v -> writeBack());
                pitchBox = addRenderableWidget(new SelectableEditBox(font, rightX + 170, mainY + 42, 44, 16,
                        Component.translatable("wh_npcs.ui.actions.pitch_field")));
                pitchBox.setMaxLength(4);
                pitchBox.setValue(String.valueOf(sound.pitch()));
                pitchBox.setResponder(v -> writeBack());
            }
            case "title" -> {
                Actions.Title title = (Actions.Title) action;
                titleBox = addRenderableWidget(new SelectableEditBox(font, rightX + 80, mainY + 14, 150, 16,
                        Component.translatable("wh_npcs.ui.actions.title_field")));
                titleBox.setMaxLength(80);
                titleBox.setValue(EditorCodes.toEditor(title.title() != null ? title.title() : ""));
                titleBox.setResponder(v -> writeBack());
                subtitleBox = addRenderableWidget(new SelectableEditBox(font, rightX + 80, mainY + 42, 150, 16,
                        Component.translatable("wh_npcs.ui.actions.subtitle_field")));
                subtitleBox.setMaxLength(120);
                subtitleBox.setValue(EditorCodes.toEditor(title.subtitle() != null ? title.subtitle() : ""));
                subtitleBox.setResponder(v -> writeBack());
                actionbarBox = addRenderableWidget(new SelectableEditBox(font, rightX + 80, mainY + 70, 150, 16,
                        Component.translatable("wh_npcs.ui.actions.actionbar_field")));
                actionbarBox.setMaxLength(120);
                actionbarBox.setValue(EditorCodes.toEditor(title.actionbar() != null ? title.actionbar() : ""));
                actionbarBox.setResponder(v -> writeBack());
            }
            case "monologue" -> {
                ensureMonoLines();
                MonologueLine line = monoLines.get(monoPage);
                int monoFieldX = rightX + 60;
                monoNameBox = addRenderableWidget(new SelectableEditBox(font, monoFieldX, mainY + 24,
                        rightW - 64, 16, Component.translatable("wh_npcs.ui.actions.mono_name_field")));
                monoNameBox.setMaxLength(64);
                monoNameBox.setValue(EditorCodes.toEditor(line.name()));
                monoNameBox.setHint(Component.translatable("wh_npcs.ui.actions.mono_name_hint"));
                monoNameBox.setResponder(v -> writeBack());
                monoPortraitBox = addRenderableWidget(new SelectableEditBox(font, monoFieldX, mainY + 46,
                        rightW - 64, 16, Component.translatable("wh_npcs.ui.actions.mono_portrait_field")));
                monoPortraitBox.setMaxLength(48);
                monoPortraitBox.setValue(line.portrait());
                monoPortraitBox.setHint(Component.translatable("wh_npcs.ui.actions.mono_portrait_hint"));
                monoPortraitBox.setResponder(v -> writeBack());
                monoTextEditor = addRenderableWidget(new com.withouthonor.npcs.client.gui.RichTextEditor(
                        font, rightX, mainY + 84, rightW, 56));
                monoTextEditor.setValue(line.text());
            }
            default -> {
            }
        }
    }

    private void loadDraft() {
        for (int i = 0; i < 3; i++) {
            slotItem[i] = null;
            slotNbt[i] = null;
            slotCount[i] = 1;
        }
        activeSlot = 0;
        DialogueAction action = current();
        if (action instanceof Actions.GiveItem give) {
            for (int i = 0; i < Math.min(3, give.items().size()); i++) {
                Actions.ItemSpec spec = give.items().get(i);
                slotItem[i] = spec.itemId();
                slotNbt[i] = spec.nbt();
                slotCount[i] = spec.count();
            }
        } else if (action instanceof Actions.TakeItem take) {
            for (int i = 0; i < Math.min(3, take.slots().size()); i++) {
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
                    for (int i = 0; i < 3; i++) {
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
                    for (int i = 0; i < 3; i++) {
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
                        new Actions.Say(EditorCodes.fromEditor(commandBox.getValue().trim())));
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
                            emoteDraftId, emoteDraftName, emoteDraftAuthor));
                }
                case "stop_emotecraft_emote" -> actions.set(selected, new Actions.StopEmotecraftEmote());
                case "run_command" -> actions.set(selected,
                        new Actions.RunCommand(commandBox.getValue().trim()));
                case "sound" -> actions.set(selected, new Actions.Sound(
                        ResourceLocation.parse(soundBox.getValue().trim()),
                        clamp(parseFloat(volumeBox, 1.0F), 0.1F, 2.0F),
                        clamp(parseFloat(pitchBox, 1.0F), 0.5F, 2.0F)));
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

    private void renderActionList(GuiGraphics g, int mouseX, int mouseY) {
        VanillaUIHelper.drawContentPanel(g, leftX, leftY, LEFT_W, leftH);
        int y = leftY + 4;
        for (int i = 0; i < actions.size(); i++) {
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
        return action.type();
    }

    private void renderEditor(GuiGraphics g, int mouseX, int mouseY) {
        DialogueAction action = current();
        if (action == null) {
            g.drawCenteredString(font, Component.translatable("wh_npcs.ui.actions.empty_hint").getString(),
                    rightX + rightW / 2, mainY + 40, VanillaUIHelper.TEXT_WHITE);
            return;
        }
        switch (action.type()) {
            case "give_item", "take_item" -> renderItemsEditor(g, mouseX, mouseY,
                    action.type().equals("give_item")
                            ? Component.translatable("wh_npcs.ui.actions.give_caption").getString()
                            : Component.translatable("wh_npcs.ui.actions.take_caption").getString());
            case "set_flag" -> {
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.flag_label").getString(),
                        rightX, mainY + 18, VanillaUIHelper.TEXT_GRAY, false);
                drawSmall(g, flagValue
                                ? Component.translatable("wh_npcs.ui.actions.flag_set").getString()
                                : Component.translatable("wh_npcs.ui.actions.flag_unset").getString(),
                        rightX, mainY + 42, 160, mouseX, mouseY, VanillaUIHelper.TEXT_AQUA);
            }
            case "reputation" -> {
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.faction_label").getString(),
                        rightX, mainY + 18, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.delta_label").getString(),
                        rightX, mainY + 46, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.delta_hint").getString(),
                        rightX + 148, mainY + 46, VanillaUIHelper.TEXT_DARK_GRAY, false);
            }
            case "say" -> {
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.say_caption").getString(),
                        rightX, mainY + 14, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.say_note1").getString(),
                        rightX, mainY + 52, VanillaUIHelper.TEXT_WHITE, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.say_note2").getString(),
                        rightX, mainY + 64, VanillaUIHelper.TEXT_WHITE, false);
            }
            case "emote" -> {
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.emote_caption").getString(),
                        rightX, mainY + 14, VanillaUIHelper.TEXT_GRAY, false);
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
                        rightX, mainY + 14, VanillaUIHelper.TEXT_GRAY, false);
                drawSmall(g, Component.translatable("wh_npcs.ui.actions.emotecraft_pick").getString(),
                        rightX, mainY + 50, 150, mouseX, mouseY, VanillaUIHelper.TEXT_AQUA);
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.emotecraft_note1").getString(),
                        rightX, mainY + 76, VanillaUIHelper.TEXT_WHITE, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.emotecraft_note2").getString(),
                        rightX, mainY + 88, VanillaUIHelper.TEXT_WHITE, false);
            }
            case "stop_emotecraft_emote" -> {
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.stop_emote_line1").getString(),
                        rightX, mainY + 14, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.stop_emote_line2").getString(),
                        rightX, mainY + 26, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.stop_emote_note").getString(),
                        rightX, mainY + 46, VanillaUIHelper.TEXT_WHITE, false);
            }
            case "open_trade" -> {
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.trade_line1").getString(),
                        rightX, mainY + 14, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.trade_line2").getString(),
                        rightX, mainY + 26, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.trade_note1").getString(),
                        rightX, mainY + 46, VanillaUIHelper.TEXT_WHITE, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.trade_note2").getString(),
                        rightX, mainY + 58, VanillaUIHelper.TEXT_WHITE, false);
            }
            case "stop_music" -> {
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.stop_music_line1").getString(),
                        rightX, mainY + 14, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.stop_music_line2").getString(),
                        rightX, mainY + 26, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.stop_music_note1").getString(),
                        rightX, mainY + 46, VanillaUIHelper.TEXT_WHITE, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.stop_music_note2").getString(),
                        rightX, mainY + 58, VanillaUIHelper.TEXT_WHITE, false);
            }
            case "follow" -> {
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.follow_line1").getString(),
                        rightX, mainY + 14, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.follow_note1").getString(),
                        rightX, mainY + 40, VanillaUIHelper.TEXT_WHITE, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.follow_note2").getString(),
                        rightX, mainY + 52, VanillaUIHelper.TEXT_WHITE, false);
            }
            case "stop_follow" -> {
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.stop_follow_line1").getString(),
                        rightX, mainY + 14, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.stop_follow_line2").getString(),
                        rightX, mainY + 26, VanillaUIHelper.TEXT_GRAY, false);
            }
            case "follow_wait" -> {
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.follow_wait_line1").getString(),
                        rightX, mainY + 14, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.follow_wait_line2").getString(),
                        rightX, mainY + 26, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.follow_wait_note").getString(),
                        rightX, mainY + 46, VanillaUIHelper.TEXT_WHITE, false);
            }
            case "run_command" -> {
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.command_caption").getString(),
                        rightX, mainY + 14, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.command_note1").getString(),
                        rightX, mainY + 52, VanillaUIHelper.TEXT_WHITE, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.command_note2").getString(),
                        rightX, mainY + 64, VanillaUIHelper.TEXT_WHITE, false);
            }
            case "sound" -> {
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.sound_label").getString(),
                        rightX, mainY + 18, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.volume_label").getString(),
                        rightX + 24, mainY + 46, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.pitch_label").getString(),
                        rightX + 146, mainY + 46, VanillaUIHelper.TEXT_GRAY, false);
            }
            case "title" -> {
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.title_label").getString(),
                        rightX, mainY + 18, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.subtitle_label").getString(),
                        rightX, mainY + 46, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.actionbar_label").getString(),
                        rightX, mainY + 74, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.title_note").getString(),
                        rightX, mainY + 96, VanillaUIHelper.TEXT_WHITE, false);
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
                        rightX, mainY + 28, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.mono_portrait_label").getString(),
                        rightX, mainY + 50, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.mono_text_label").getString(),
                        rightX, mainY + 72, VanillaUIHelper.TEXT_GRAY, false);
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.mono_add_note").getString(),
                        rightX + 96, mainY + 72, VanillaUIHelper.TEXT_WHITE, false);

                int cy = mainY + 150;
                boolean lockHover = isOver(mouseX, mouseY, rightX, cy, 132, 12);
                VanillaUIHelper.drawButton(g, rightX, cy, 12, 12, lockHover);
                if (monoLock) {
                    g.drawCenteredString(font, "§a✓", rightX + 6, cy + 2, VanillaUIHelper.TEXT_WHITE);
                }
                g.drawString(font, Component.translatable("wh_npcs.ui.actions.mono_lock").getString(),
                        rightX + 18, cy + 2,
                        monoLock ? VanillaUIHelper.TEXT_WHITE : VanillaUIHelper.TEXT_DARK_GRAY, false);
            }
            default -> g.drawString(font,
                    Component.translatable("wh_npcs.ui.actions.json_only", action.type()).getString(),
                    rightX, mainY + 18, VanillaUIHelper.TEXT_STATUS, false);
        }
    }

    private void renderItemsEditor(GuiGraphics g, int mouseX, int mouseY, String caption) {
        g.drawString(font, caption, rightX, mainY + 2, VanillaUIHelper.TEXT_GRAY, false);
        for (int i = 0; i < 3; i++) {
            int x = colX(i);
            g.drawCenteredString(font, Component.translatable("wh_npcs.ui.actions.slot", i + 1).getString(),
                    x + 35, mainY + 14,
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
            g.drawString(font, Component.translatable("wh_npcs.ui.actions.pcs").getString(),
                    x + 52, countY() + 3, VanillaUIHelper.TEXT_DARK_GRAY, false);
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
        g.renderComponentTooltip(font, lines, x, y);
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
            int y = leftY + 4;
            for (int i = 0; i < actions.size(); i++) {
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
        } else if (button == 1 && handleEditorClick(mouseX, mouseY, true)) {
            return true;
        }
        return superMouseClicked(mouseX, mouseY, button);
    }

    private boolean handleEditorClick(double mouseX, double mouseY, boolean rightClick) {
        DialogueAction action = current();
        if (action == null) {
            return false;
        }
        switch (action.type()) {
            case "set_flag" -> {
                if (!rightClick && isOver(mouseX, mouseY, rightX, mainY + 42, 160, 18)) {
                    flagValue = !flagValue;
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
                if (!rightClick && isOver(mouseX, mouseY, rightX, mainY + 50, 150, 18)) {
                    writeBack();
                    if (minecraft != null) {
                        int idx = selected;
                        minecraft.setScreen(EmotecraftScreen.forPicker(this, ref -> {
                            emoteDraftId = ref.id();
                            emoteDraftName = ref.name();
                            emoteDraftAuthor = ref.author();
                            if (idx >= 0 && idx < actions.size()) {
                                actions.set(idx, new Actions.EmotecraftEmote(
                                        emoteDraftId, emoteDraftName, emoteDraftAuthor));
                            }
                        }));
                    }
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
                    if (isOver(mouseX, mouseY, rightX, mainY + 150, 132, 12)) {
                        monoLock = !monoLock;
                        writeBack();
                        return true;
                    }
                }
            }
            case "give_item", "take_item" -> {
                for (int i = 0; i < 3; i++) {
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
