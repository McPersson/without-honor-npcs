package com.withouthonor.npcs.common.registry;

import com.withouthonor.npcs.WHCompanions;
import com.withouthonor.npcs.common.command.NameArgument;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCommandArguments {

    public static final DeferredRegister<ArgumentTypeInfo<?, ?>> ARGUMENT_TYPES =
            DeferredRegister.create(Registries.COMMAND_ARGUMENT_TYPE, WHCompanions.MODID);

    public static final RegistryObject<SingletonArgumentInfo<NameArgument>> NAME =
            ARGUMENT_TYPES.register("name", () -> ArgumentTypeInfos.registerByClass(
                    NameArgument.class, SingletonArgumentInfo.contextFree(NameArgument::name)));

    public static void register(IEventBus bus) {
        ARGUMENT_TYPES.register(bus);
    }
}
