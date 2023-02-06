package org.wallentines.midnightmenus.api.menu;

import org.wallentines.midnightcore.api.item.MItemStack;
import org.wallentines.midnightcore.api.module.messaging.MessagingModule;
import org.wallentines.midnightcore.api.player.MPlayer;
import org.wallentines.midnightcore.api.text.LangProvider;
import org.wallentines.midnightcore.api.text.PlaceholderManager;
import org.wallentines.mdcfg.ConfigSection;
import org.wallentines.midnightlib.registry.Identifier;
import org.wallentines.midnightlib.registry.Registry;
import org.wallentines.midnightmenus.api.MidnightMenusAPI;

public interface MenuActionType {

    void execute(MidnightMenu menu, MPlayer clicker, String data, MItemStack stack);

    Registry<MenuActionType> ACTION_TYPE_REGISTRY = new Registry<>("midnightmenus");

    static MenuActionType register(String id, MenuActionType act) {
        return ACTION_TYPE_REGISTRY.register(Identifier.parseOrDefault(id, "midnightmenus"), act);
    }

    MenuActionType MESSAGE = register("message", (menu, clicker, data, stack) -> clicker.sendMessage(PlaceholderManager.INSTANCE.parseText(data, clicker, menu)));

    MenuActionType LANG = register("lang", (menu, clicker, data, stack) -> {

        LangProvider prov = MidnightMenusAPI.getInstance().getLangProvider();
        clicker.sendMessage(prov.getMessage(data, clicker, menu));

    });

    MenuActionType CHANGE_PAGE = register("change_page", (menu, clicker, data, stack) -> menu.advancePage(clicker, Integer.parseInt(data)));

    MenuActionType CLOSE = register("close", (menu, clicker, data, stack) -> menu.close(clicker));


    MenuActionType OPEN = register("open", (menu, clicker, data, stack) -> {

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

    MenuActionType COMMAND = register("command", (menu, clicker, data, stack) -> clicker.getServer().executeCommand(PlaceholderManager.INSTANCE.parseText(data, clicker).getAllContent(), false));

    MenuActionType PLAYER_COMMAND = register("player_command", (menu, clicker, data, stack) -> clicker.executeCommand(PlaceholderManager.INSTANCE.parseText(data, clicker).getAllContent()));

    MenuActionType ITEM = register("item", (menu, clicker, data, stack) -> clicker.giveItem(stack.copy()));

    MenuActionType SERVER = register("server", (menu, clicker, data, stack) -> {

        MessagingModule module = clicker.getServer().getModule(MessagingModule.class);
        module.sendMessage(clicker, new Identifier("midnightcore", "send"), new ConfigSection().with("server", data));

    });

}
