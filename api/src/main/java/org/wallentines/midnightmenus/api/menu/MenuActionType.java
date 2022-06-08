package org.wallentines.midnightmenus.api.menu;

import org.wallentines.midnightcore.api.MidnightCoreAPI;
import org.wallentines.midnightcore.api.item.MItemStack;
import org.wallentines.midnightcore.api.module.lang.LangModule;
import org.wallentines.midnightcore.api.module.lang.LangProvider;
import org.wallentines.midnightcore.api.module.messaging.MessagingModule;
import org.wallentines.midnightcore.api.player.MPlayer;
import org.wallentines.midnightlib.config.ConfigSection;
import org.wallentines.midnightlib.registry.Identifier;
import org.wallentines.midnightlib.registry.Registry;
import org.wallentines.midnightmenus.api.MidnightMenusAPI;

public interface MenuActionType {

    void execute(MidnightMenu menu, MPlayer clicker, String data, MItemStack stack);

    Registry<MenuActionType> ACTION_TYPE_REGISTRY = new Registry<>();

    static MenuActionType register(String id, MenuActionType act) {
        return ACTION_TYPE_REGISTRY.register(Identifier.parseOrDefault(id, "midnightmenus"), act);
    }

    MenuActionType MESSAGE = register("message", (menu, clicker, data, stack) -> {

        LangModule module = MidnightMenusAPI.getInstance().getLangProvider().getModule();
        clicker.sendMessage(module.parseText(data, clicker, menu));

    });

    MenuActionType LANG = register("lang", (menu, clicker, data, stack) -> {

        LangProvider prov = MidnightMenusAPI.getInstance().getLangProvider();
        clicker.sendMessage(prov.getMessage(data, clicker, menu));

    });

    MenuActionType CHANGE_PAGE = register("change_page", (menu, clicker, data, stack) -> menu.advancePage(clicker, Integer.parseInt(data)));

    MenuActionType CLOSE = register("close", (menu, clicker, data, stack) -> menu.close(clicker));


    MenuActionType OPEN = register("open", (menu, clicker, data, stack) -> {

        LangModule prov = MidnightMenusAPI.getInstance().getLangProvider().getModule();

        String id = data;
        int page = 0;
        if(data.contains(";")) {
            String[] ss = data.split(";");
            id = ss[0];
            page = Integer.parseInt(ss[1]);
        }

        MidnightMenu newMenu = MidnightMenusAPI.getInstance().getMenuRegistry().get(Identifier.parse(id));
        if(newMenu == null) return;

        newMenu.open(clicker, page);

    });

    MenuActionType COMMAND = register("command", (menu, clicker, data, stack) ->  {

        LangModule mod = MidnightMenusAPI.getInstance().getLangProvider().getModule();
        MidnightCoreAPI.getInstance().executeConsoleCommand(mod.parseText(data, clicker).getAllContent(), false);
    });

    MenuActionType PLAYER_COMMAND = register("player_command", (menu, clicker, data, stack) ->  {

        LangModule mod = MidnightMenusAPI.getInstance().getLangProvider().getModule();
        clicker.executeCommand(mod.parseText(data, clicker).getAllContent());
    });

    MenuActionType ITEM = register("item", (menu, clicker, data, stack) -> {

        clicker.giveItem(stack.copy());
    });

    MenuActionType SERVER = register("server", (menu, clicker, data, stack) -> {

        MessagingModule module = MidnightCoreAPI.getInstance().getModuleManager().getModule(MessagingModule.class);
        module.sendMessage(clicker, new Identifier("midnightcore", "send"), new ConfigSection().with("server", data));

    });

}
