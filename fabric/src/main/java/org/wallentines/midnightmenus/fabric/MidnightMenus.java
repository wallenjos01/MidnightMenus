package org.wallentines.midnightmenus.fabric;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.api.ModInitializer;
import net.minecraft.commands.Commands;
import org.wallentines.midnightcore.fabric.event.MidnightCoreAPICreatedEvent;
import org.wallentines.midnightcore.fabric.event.server.CommandLoadEvent;
import org.wallentines.midnightcore.fabric.player.FabricPlayer;
import org.wallentines.midnightlib.config.serialization.json.JsonConfigProvider;
import org.wallentines.midnightlib.event.Event;
import org.wallentines.midnightmenus.api.menu.MidnightMenu;
import org.wallentines.midnightmenus.common.MidnightMenusImpl;

import java.nio.file.Paths;

public class MidnightMenus implements ModInitializer {

    private MidnightMenusImpl api;

    @Override
    public void onInitialize() {

        Event.register(MidnightCoreAPICreatedEvent.class, this, event -> {

            api = new MidnightMenusImpl(Paths.get("config/MidnightMenus"), JsonConfigProvider.INSTANCE.loadFromStream(getClass().getResourceAsStream("/midnightmenus/lang/en_us.json")));
        });

        Event.register(CommandLoadEvent.class, this, event -> {

            MainCommand.register(event.getDispatcher());
            for(MidnightMenu menu : api.getMenuRegistry()) {
                String cmd = menu.getCommand();
                String perm = menu.getCommandPermission();
                if(cmd != null) {
                    event.getDispatcher().register(Commands.literal(cmd).requires(context -> {
                        if(perm == null) return true;
                        return Permissions.check(context, perm);
                    }).executes(context -> {
                        menu.open(FabricPlayer.wrap(context.getSource().getPlayerOrException()));
                        return 1;
                    }));
                }
            }

        });

    }
}
