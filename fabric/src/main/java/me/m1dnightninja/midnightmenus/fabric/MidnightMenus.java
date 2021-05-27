package me.m1dnightninja.midnightmenus.fabric;

import me.m1dnightninja.midnightcore.api.MidnightCoreAPI;
import me.m1dnightninja.midnightcore.api.config.ConfigProvider;
import me.m1dnightninja.midnightcore.api.module.lang.CustomPlaceholderInline;
import me.m1dnightninja.midnightcore.api.module.lang.ILangModule;
import me.m1dnightninja.midnightcore.common.config.JsonConfigProvider;
import me.m1dnightninja.midnightcore.fabric.MidnightCore;
import me.m1dnightninja.midnightcore.fabric.api.MidnightCoreModInitializer;
import me.m1dnightninja.midnightcore.fabric.player.FabricPlayer;
import me.m1dnightninja.midnightmenus.api.MidnightMenusAPI;
import me.m1dnightninja.midnightmenus.api.menu.MenuActionType;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import java.io.File;

public class MidnightMenus implements MidnightCoreModInitializer {

    @Override
    public void onInitialize() { }

    @Override
    public void onAPICreated(MidnightCore core, MidnightCoreAPI api) {
        ILangModule mod = MidnightCoreAPI.getInstance().getModule(ILangModule.class);

        MenuActionType.register("command", (menu, clicker, data) -> MidnightCore.getServer().getCommands().performCommand(MidnightCore.getServer().createCommandSourceStack(), mod.applyPlaceholdersFlattened(data, menu, clicker)));
        MenuActionType.register("player_command", (menu, clicker, data) -> {

            ServerPlayer pl = ((FabricPlayer) clicker).getMinecraftPlayer();
            if(pl == null) return;

            CommandSourceStack st = pl.createCommandSourceStack();

            MidnightCore.getServer().getCommands().performCommand(st, mod.applyPlaceholdersFlattened(data, menu, clicker));

        });
        
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> new MainCommand().register(dispatcher));

        ConfigProvider prov = new JsonConfigProvider();
        new MidnightMenusAPI(new File("config", "MidnightMenus"), prov.loadFromStream(getClass().getResourceAsStream("/assets/midnightmenus/lang/en_us.json")));

    }
}
