package com.withouthonor.npcs.compat;

import net.minecraftforge.fml.ModList;

public final class Compat {

    public static final String EMOTECRAFT = "emotecraft";
    public static final String CURIOS = "curios";
    public static final String CARRYON = "carryon";
    public static final String ETCHED = "etched";

    private static Boolean emotecraftLoaded;
    private static EmotecraftBridge emotecraft;
    private static Boolean curiosLoaded;
    private static CuriosBridge curios;
    private static Boolean carryonLoaded;
    private static Boolean etchedLoaded;
    private static EtchedBridge etched;

    private Compat() {
    }

    public static boolean emotecraftLoaded() {
        if (emotecraftLoaded == null) {
            emotecraftLoaded = ModList.get() != null && ModList.get().isLoaded(EMOTECRAFT);
        }
        return emotecraftLoaded;
    }

    public static EmotecraftBridge emotecraft() {
        if (emotecraft == null) {
            if (emotecraftLoaded()) {
                emotecraft = new com.withouthonor.npcs.compat.emotecraft.EmotecraftBridgeImpl();
            } else {
                emotecraft = NoopEmotecraftBridge.INSTANCE;
            }
        }
        return emotecraft;
    }

    @javax.annotation.Nullable
    public static EmotecraftClientBridge emotecraftClient() {
        EmotecraftBridge bridge = emotecraft();
        return bridge instanceof EmotecraftClientBridge client ? client : null;
    }

    public static boolean curiosLoaded() {
        if (curiosLoaded == null) {
            curiosLoaded = ModList.get() != null && ModList.get().isLoaded(CURIOS);
        }
        return curiosLoaded;
    }

    public static CuriosBridge curios() {
        if (curios == null) {
            if (curiosLoaded()) {
                curios = new com.withouthonor.npcs.compat.curios.CuriosBridgeImpl();
            } else {
                curios = NoopCuriosBridge.INSTANCE;
            }
        }
        return curios;
    }

    public static boolean etchedLoaded() {
        if (etchedLoaded == null) {
            etchedLoaded = ModList.get() != null && ModList.get().isLoaded(ETCHED);
        }
        return etchedLoaded;
    }

    public static EtchedBridge etched() {
        if (etched == null) {
            if (etchedLoaded()) {
                etched = new com.withouthonor.npcs.compat.etched.EtchedBridgeImpl();
            } else {
                etched = NoopEtchedBridge.INSTANCE;
            }
        }
        return etched;
    }

    @javax.annotation.Nullable
    public static EtchedClientBridge etchedClient() {
        EtchedBridge bridge = etched();
        return bridge instanceof EtchedClientBridge client ? client : null;
    }

    public static boolean carryonLoaded() {
        if (carryonLoaded == null) {
            carryonLoaded = ModList.get() != null && ModList.get().isLoaded(CARRYON);
        }
        return carryonLoaded;
    }
}
