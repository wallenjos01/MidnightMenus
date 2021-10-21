package me.m1dnightninja.midnightmenus.fabric;

import me.m1dnightninja.midnightcore.api.MidnightCoreAPI;
import me.m1dnightninja.midnightcore.api.config.ConfigProvider;
import me.m1dnightninja.midnightcore.common.config.JsonConfigProvider;
import me.m1dnightninja.midnightcore.fabric.MidnightCore;
import me.m1dnightninja.midnightcore.fabric.MidnightCoreModInitializer;
import me.m1dnightninja.midnightmenus.api.MidnightMenusAPI;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;

import java.io.File;

public class MidnightMenus implements MidnightCoreModInitializer {

    @Override
    public void onInitialize() { }

    @Override
    public void onAPICreated(MidnightCore core, MidnightCoreAPI api) {

        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> new MainCommand().register(dispatcher));

        ConfigProvider prov = new JsonConfigProvider();
        new MidnightMenusAPI(new File("config", "MidnightMenus"), prov.loadFromStream(getClass().getResourceAsStream("/assets/midnightmenus/lang/en_us.json")));

    }
}
