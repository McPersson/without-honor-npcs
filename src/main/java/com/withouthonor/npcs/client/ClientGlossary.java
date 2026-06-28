package com.withouthonor.npcs.client;

import com.withouthonor.npcs.common.glossary.GlossaryTerm;

import javax.annotation.Nullable;
import java.util.List;

public final class ClientGlossary {

    private static List<GlossaryTerm> terms = List.of();

    private ClientGlossary() {
    }

    public static void set(List<GlossaryTerm> list) {
        terms = List.copyOf(list);
    }

    public static List<GlossaryTerm> all() {
        return terms;
    }

    @Nullable
    public static GlossaryTerm byId(String id) {
        for (GlossaryTerm term : terms) {
            if (term.getId().equals(id)) {
                return term;
            }
        }
        return null;
    }
}
