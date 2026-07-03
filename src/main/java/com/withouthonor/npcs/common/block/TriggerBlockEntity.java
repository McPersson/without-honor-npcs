package com.withouthonor.npcs.common.block;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.withouthonor.npcs.common.dialogue.action.ActionTypes;
import com.withouthonor.npcs.common.dialogue.action.DialogueAction;
import com.withouthonor.npcs.common.dialogue.condition.ConditionTypes;
import com.withouthonor.npcs.common.dialogue.condition.DialogueCondition;
import com.withouthonor.npcs.common.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TriggerBlockEntity extends BlockEntity {

    private List<DialogueAction> actions = new ArrayList<>();
    private List<DialogueCondition> conditions = new ArrayList<>();
    private boolean once = true;
    private Direction enterDir;
    // Привязанный NPC — контекст действий триггера (эмоции/реплики/атака от его имени).
    @javax.annotation.Nullable
    private UUID targetNpc;
    private final Set<UUID> fired = new HashSet<>();
    private final Map<UUID, Long> lastInside = new HashMap<>();

    public TriggerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TRIGGER.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && level.isClientSide) {
            TriggerClientIndex.add(this);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && level.isClientSide) {
            TriggerClientIndex.remove(this);
        }
    }

    public boolean isOnce() {
        return once;
    }

    public byte enterDirByte() {
        return dirToByte(enterDir);
    }

    public static byte dirToByte(@javax.annotation.Nullable Direction d) {
        return d == null ? -1 : (byte) d.get3DDataValue();
    }

    @javax.annotation.Nullable
    public static Direction dirFromByte(byte b) {
        return b < 0 ? null : Direction.from3DDataValue(b);
    }

    @javax.annotation.Nullable
    public UUID getTargetNpc() {
        return targetNpc;
    }

    public void setTargetNpc(@javax.annotation.Nullable UUID targetNpc) {
        this.targetNpc = targetNpc;
        setChanged();
    }

    /** Отображаемое имя привязанного NPC (для GUI); пустая строка, если не найден. */
    public String targetNpcName() {
        if (targetNpc == null || !(level instanceof ServerLevel sl)) {
            return "";
        }
        String name = "";
        if (sl.getEntity(targetNpc) instanceof com.withouthonor.npcs.common.entity.CompanionEntity c) {
            name = c.getName().getString();
        } else {
            var entry = com.withouthonor.npcs.common.storage.CompanionIndex.get(sl.getServer()).byId(targetNpc);
            if (entry != null) {
                name = entry.name();
            }
        }
        return name.length() > 64 ? name.substring(0, 64) : name;
    }

    public String actionsJson() {
        return actionsToJson(actions);
    }

    public String conditionsJson() {
        return conditionsToJson(conditions);
    }

    public void apply(String actionsJson, String conditionsJson, boolean once, byte enterDir,
                      @javax.annotation.Nullable UUID targetNpc) {
        this.actions = actionsFromJson(actionsJson);
        this.conditions = conditionsFromJson(conditionsJson);
        this.once = once;
        this.enterDir = dirFromByte(enterDir);
        this.targetNpc = targetNpc;
        this.fired.clear();
        setChanged();
    }

    public void tryFire(ServerPlayer player) {
        if (actions.isEmpty()) {
            return;
        }
        long now = player.level().getGameTime();
        UUID id = player.getUUID();
        Long last = lastInside.put(id, now);
        if (lastInside.size() > 32) {
            lastInside.values().removeIf(t -> now - t > 10L);
        }
        boolean entered = last == null || now - last > 1L;
        if (!entered) {
            return;
        }
        if (enterDir != null && !enteredFrom(player, enterDir)) {
            return;
        }
        if (once && fired.contains(id)) {
            return;
        }
        // Привязанный NPC как контекст действий; не найден/не прогружен — null, действия тихо пропустят.
        com.withouthonor.npcs.common.entity.CompanionEntity npc =
                level instanceof ServerLevel sl && targetNpc != null
                        && sl.getEntity(targetNpc) instanceof com.withouthonor.npcs.common.entity.CompanionEntity c
                        ? c : null;
        DialogueCondition.Context ctx = new DialogueCondition.Context(player, npc);
        if (!DialogueCondition.testAll(conditions, ctx)) {
            return;
        }
        DialogueAction.executeAll(actions, ctx);
        if (once) {
            fired.add(id);
            setChanged();
        }
    }

    private static boolean enteredFrom(ServerPlayer player, Direction required) {
        double dx = player.getX() - player.xOld;
        double dz = player.getZ() - player.zOld;
        if (dx * dx + dz * dz < 1.0E-4) {
            return false;
        }
        return Direction.getNearest(dx, 0.0, dz) == required;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("Actions", actionsToJson(actions));
        tag.putString("Conditions", conditionsToJson(conditions));
        tag.putBoolean("Once", once);
        if (enterDir != null) {
            tag.putByte("EnterDir", (byte) enterDir.get3DDataValue());
        }
        if (targetNpc != null) {
            tag.putUUID("TargetNpc", targetNpc);
        }
        ListTag list = new ListTag();
        for (UUID id : fired) {
            list.add(NbtUtils.createUUID(id));
        }
        tag.put("Fired", list);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        actions = actionsFromJson(tag.getString("Actions"));
        conditions = conditionsFromJson(tag.getString("Conditions"));
        once = !tag.contains("Once") || tag.getBoolean("Once");
        enterDir = tag.contains("EnterDir") ? Direction.from3DDataValue(tag.getByte("EnterDir")) : null;
        targetNpc = tag.hasUUID("TargetNpc") ? tag.getUUID("TargetNpc") : null;
        fired.clear();
        if (tag.contains("Fired", Tag.TAG_LIST)) {
            for (Tag t : tag.getList("Fired", Tag.TAG_INT_ARRAY)) {
                fired.add(NbtUtils.loadUUID(t));
            }
        }
    }

    public static String actionsToJson(List<DialogueAction> actions) {
        JsonArray arr = new JsonArray();
        for (DialogueAction a : actions) {
            arr.add(a.toJson());
        }
        return arr.toString();
    }

    public static List<DialogueAction> actionsFromJson(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return new ArrayList<>(ActionTypes.parseList(JsonParser.parseString(json).getAsJsonArray()));
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public static String conditionsToJson(List<DialogueCondition> conditions) {
        JsonArray arr = new JsonArray();
        for (DialogueCondition c : conditions) {
            arr.add(c.toJson());
        }
        return arr.toString();
    }

    public static List<DialogueCondition> conditionsFromJson(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return new ArrayList<>(ConditionTypes.parseList(JsonParser.parseString(json).getAsJsonArray()));
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
