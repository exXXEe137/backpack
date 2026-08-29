package com.exxxee.backpack.Network;

import com.exxxee.backpack.Network.paylo.ModuleDragPayload;
import com.exxxee.backpack.Network.paylo.ModuleIventoryPayload;
import com.exxxee.backpack.Network.paylo.SwitchModulePayload;
import com.exxxee.backpack.Network.payloadHandler.DragOperation;
import com.exxxee.backpack.Network.payloadHandler.ModuleInventoryOperation;
import com.exxxee.backpack.api.helper.data.BackpackDataHelper;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.Context;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ServerNetWorking {

    public static void init () {
        PayloadTypeRegistry.serverboundPlay().register(com.exxxee.backpack.Network.paylo.SwitchModulePayload.TYPE, com.exxxee.backpack.Network.paylo.SwitchModulePayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(com.exxxee.backpack.Network.paylo.SwitchModulePayload.TYPE, (payload, context) -> switchModuleHandler(payload, context));

        PayloadTypeRegistry.serverboundPlay().register(com.exxxee.backpack.Network.paylo.ModuleIventoryPayload.TYPE, com.exxxee.backpack.Network.paylo.ModuleIventoryPayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(com.exxxee.backpack.Network.paylo.ModuleIventoryPayload.TYPE, (payload, context) -> moduleHandler(payload, context));

        PayloadTypeRegistry.serverboundPlay().register(com.exxxee.backpack.Network.paylo.ModuleDragPayload.TYPE, com.exxxee.backpack.Network.paylo.ModuleDragPayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(com.exxxee.backpack.Network.paylo.ModuleDragPayload.TYPE, ((payload, context) -> dragHandler(payload, context)));

    }

    public static void switchModuleHandler (SwitchModulePayload payload, Context context) {
        context.server().execute(()->{
            ServerPlayer player = context.player();
            if (!BackpackDataHelper.isValidModuleIndex(player, payload.tabIndex())) {return;}
            List<ItemStack> modules =  new ArrayList<>(BackpackDataHelper.getModules(player));
            ItemStack module = modules.get(payload.tabIndex());
            ItemStack carried = player.containerMenu.getCarried();
            modules.set(payload.tabIndex(), carried);
            player.containerMenu.setCarried(module);
            BackpackDataHelper.updateBackpack(player, modules);
        });
    }

    private static final ModuleInventoryOperation moduleOperation = new ModuleInventoryOperation();

    public static void moduleHandler (ModuleIventoryPayload payload, Context context) {
        context.server().execute(()->{
            moduleOperation.Operation(context.player(), payload.slotIndex(), payload.button(), payload.shift());
        });
    }

    private static final DragOperation dragOperation = new DragOperation();

    public static void dragHandler (ModuleDragPayload payload, Context context) {
        context.server().execute(()->{
            dragOperation.Operation(context.player(), payload.dragType(), payload.slots());
        });
    }

}
