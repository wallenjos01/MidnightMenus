package me.m1dnightninja.midnightmenus.api.menu;

import me.m1dnightninja.midnightcore.api.MidnightCoreAPI;
import me.m1dnightninja.midnightcore.api.inventory.MItemStack;
import me.m1dnightninja.midnightcore.api.module.lang.ILangModule;
import me.m1dnightninja.midnightcore.api.module.lang.ILangProvider;
import me.m1dnightninja.midnightcore.api.registry.MIdentifier;
import me.m1dnightninja.midnightcore.api.player.MPlayer;
import me.m1dnightninja.midnightcore.api.registry.MRegistry;
import me.m1dnightninja.midnightmenus.api.MidnightMenusAPI;

public interface MenuActionType {

    void execute(MidnightMenu menu, MPlayer clicker, String data, MItemStack stack);

    MRegistry<MenuActionType> ACTION_TYPE_REGISTRY = new MRegistry<>();

    static MenuActionType register(String id, MenuActionType act) {
        return ACTION_TYPE_REGISTRY.register(MIdentifier.parseOrDefault(id, "midnightmenus"), act);
    }

    MenuActionType MESSAGE = register("message", (menu, clicker, data, stack) -> {

        ILangModule module = MidnightMenusAPI.getInstance().getLangProvider().getModule();
        clicker.sendMessage(module.parseText(data, clicker, menu));

    });

    MenuActionType LANG = register("lang", (menu, clicker, data, stack) -> {

        ILangProvider prov = MidnightMenusAPI.getInstance().getLangProvider();
        prov.sendMessage(data, clicker, menu);

    });

    MenuActionType CHANGE_PAGE = register("change_page", (menu, clicker, data, stack) -> menu.advancePage(clicker, Integer.parseInt(data)));

    MenuActionType CLOSE = register("close", (menu, clicker, data, stack) -> menu.close(clicker));


    MenuActionType OPEN = register("open", (menu, clicker, data, stack) -> {

        ILangModule prov = MidnightMenusAPI.getInstance().getLangProvider().getModule();

        String id = data;
        int page = 0;
        if(data.contains(";")) {
            String[] ss = data.split(";");
            id = ss[0];
            page = Integer.parseInt(ss[1]);
        }

        MidnightMenu newMenu = MidnightMenusAPI.MENU_REGISTRY.get(MIdentifier.parse(id));
        if(newMenu == null) return;

        newMenu.open(clicker, page);

    });

    MenuActionType COMMAND = register("command", (menu, clicker, data, stack) ->  {

        ILangModule mod = MidnightMenusAPI.getInstance().getLangProvider().getModule();
        MidnightCoreAPI.getInstance().executeConsoleCommand(mod.applyPlaceholdersFlattened(data, clicker));
    });

    MenuActionType PLAYER_COMMAND = register("player_command", (menu, clicker, data, stack) ->  {

        ILangModule mod = MidnightMenusAPI.getInstance().getLangProvider().getModule();
        clicker.executeCommand(mod.applyPlaceholdersFlattened(data, clicker));
    });

    MenuActionType ITEM = register("item", (menu, clicker, data, stack) -> {

        clicker.giveItem(stack.copy());
    });

}
