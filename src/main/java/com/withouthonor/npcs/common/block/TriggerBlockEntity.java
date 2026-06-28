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

    public String actionsJson() {
        return actionsToJson(actions);
    }

    public String conditionsJson() {
        return conditionsToJson(conditions);
    }

    public void apply(String actionsJson, String conditionsJson, boolean once, byte enterDir) {
        this.actions = actionsFromJson(actionsJson);
        this.conditions = conditionsFromJson(conditionsJson);
        this.once = once;
        this.enterDir = dirFromByte(enterDir);
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
        DialogueCondition.Context ctx = new DialogueCondition.Context(player, null);
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
