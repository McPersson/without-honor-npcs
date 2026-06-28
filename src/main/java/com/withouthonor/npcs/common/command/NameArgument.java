package com.withouthonor.npcs.common.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import java.util.Arrays;
import java.util.Collection;

public class NameArgument implements ArgumentType<String> {

    private static final Collection<String> EXAMPLES = Arrays.asList("Boris", "Anna", "\"Sir Lancelot\"");

    public static NameArgument name() {
        return new NameArgument();
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        if (reader.canRead() && (reader.peek() == '"' || reader.peek() == '\'')) {
            return reader.readQuotedString();
        }
        int start = reader.getCursor();
        while (reader.canRead() && reader.peek() != ' ') {
            reader.skip();
        }
        return reader.getString().substring(start, reader.getCursor());
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
