package com.withouthonor.npcs.common.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class WhConfig {

    public static final ForgeConfigSpec SPEC;

    private static final ForgeConfigSpec.BooleanValue ALLOW_CARRY_ON_PICKUP;

    private static final ForgeConfigSpec.BooleanValue ALLOW_CLIENT_IMPORT;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        ALLOW_CARRY_ON_PICKUP = b
                .comment("Allow the Carry On mod to pick up NPCs.",
                        "false (default) — carrying NPCs is forbidden.",
                        "true — NPCs can be carried like vanilla mobs.")
                .define("allowCarryOnPickup", false);
        ALLOW_CLIENT_IMPORT = b
                .comment("Allow operators to upload profiles, dialogues and images",
                        "from their own PC to the server (editor import-from-PC buttons).",
                        "true (default) — allowed for operators.",
                        "false — all import-from-PC is blocked, even for operators.")
                .define("allowClientImport", true);
        SPEC = b.build();
    }

    private WhConfig() {
    }

    public static boolean allowCarryOnPickup() {
        return ALLOW_CARRY_ON_PICKUP.get();
    }

    public static boolean allowClientImport() {
        return ALLOW_CLIENT_IMPORT.get();
    }
}
