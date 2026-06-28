package com.withouthonor.npcs.common.skin;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;

public final class DefaultSkins {

    private static final String MIRROR =
            "https://raw.githubusercontent.com/InventivetalentDev/minecraft-assets/1.20.2/assets/minecraft/textures/entity/player/";

    public record DefaultSkin(String id, String displayName, boolean slim) {

        public String spec() {
            return "default:" + id;
        }

        public String url() {
            return MIRROR + (slim ? "slim/" : "wide/") + id + ".png";
        }
    }

    public static final List<DefaultSkin> ALL = List.of(
            new DefaultSkin("steve", "Steve", false),
            new DefaultSkin("alex", "Alex", true),
            new DefaultSkin("noor", "Noor", true),
            new DefaultSkin("sunny", "Sunny", false),
            new DefaultSkin("ari", "Ari", false),
            new DefaultSkin("zuri", "Zuri", false),
            new DefaultSkin("makena", "Makena", true),
            new DefaultSkin("kai", "Kai", false),
            new DefaultSkin("efe", "Efe", true),

            new DefaultSkin("alessia", "Alessia", true));

    private static final java.util.Set<String> BUNDLED = java.util.Set.of("alessia");

    public static String bundledResourcePath(String id) {
        return "/assets/wh_npcs/textures/entity/companion/" + id + ".png";
    }

    public static boolean isBundled(String id) {
        return BUNDLED.contains(id);
    }

    private DefaultSkins() {
    }

    @Nullable
    public static DefaultSkin bySpec(String spec) {
        if (!spec.toLowerCase(Locale.ROOT).startsWith("default:")) {
            return null;
        }
        String id = spec.substring("default:".length()).toLowerCase(Locale.ROOT).trim();
        for (DefaultSkin skin : ALL) {
            if (skin.id().equals(id)) {
                return skin;
            }
        }
        return null;
    }
}
