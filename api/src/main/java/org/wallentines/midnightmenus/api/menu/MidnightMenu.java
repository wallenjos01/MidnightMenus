package org.wallentines.midnightmenus.api.menu;

import org.wallentines.midnightcore.api.MidnightCoreAPI;
import org.wallentines.midnightcore.api.item.InventoryGUI;
import org.wallentines.midnightcore.api.item.MItemStack;
import org.wallentines.midnightcore.api.module.lang.LangModule;
import org.wallentines.midnightcore.api.player.MPlayer;
import org.wallentines.midnightcore.api.text.MComponent;
import org.wallentines.midnightlib.config.ConfigSection;
import org.wallentines.midnightmenus.api.MidnightMenusAPI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class MidnightMenu {

    private final List<Entry> entries = new ArrayList<>();
    private final HashMap<MPlayer, InventoryGUI> playerGuis = new HashMap<>();

    private final MComponent title;
    private final int pageSize;
    private final MenuRequirement openRequirement;

    private final String command;
    private final String commandPermission;

    public MidnightMenu(MComponent title, int pageSize, MenuRequirement requirement, String command, String commandPermission) {
        this.title = title;
        this.pageSize = pageSize;
        this.openRequirement = requirement;
        this.command = command;
        this.commandPermission = commandPermission;
    }

    private InventoryGUI createGUI(MPlayer player) {

        HashMap<Integer, Entry> visible = new HashMap<>();
        for(Entry ent : entries) {
            if(ent.viewRequirement != null && !ent.viewRequirement.check(player)) {
                continue;
            }
            if(visible.containsKey(ent.slot) && visible.get(ent.slot).priority >= ent.priority) {
                continue;
            }

            visible.put(ent.slot, ent);
        }

        LangModule langModule = MidnightMenusAPI.getInstance().getLangProvider().getModule();

        InventoryGUI igui = MidnightCoreAPI.getInstance().createGUI(langModule.applyPlaceholders(title, player));

        for(Entry ent : visible.values()) {

            MItemStack is = ent.getItem(this, player);
            igui.setItem(ent.slot, is, new InventoryGUI.GUIAction() {

                final Entry entry = ent;

                @Override
                public void onClick(InventoryGUI.ClickType type, MPlayer user) {

                    if(!entry.actions.containsKey(type)) {
                        return;
                    }
                    for(MenuAction act : entry.actions.get(type)) {
                        try {
                            act.execute(MidnightMenu.this, player, is);

                        } catch (Throwable th) {

                            MidnightMenusAPI.getLogger().warn("An error occurred while executing a menu action!");
                            th.printStackTrace();
                        }
                    }
                }
            });
        }

        return igui;
    }

    public int getPage(MPlayer pl) {
        if(!playerGuis.containsKey(pl)) return -1;
        return playerGuis.get(pl).getPage(pl);
    }

    public void advancePage(MPlayer pl, int count) {
        if(!playerGuis.containsKey(pl)) return;

        InventoryGUI gui = playerGuis.get(pl);
        gui.open(pl, Math.max(0, Math.min(gui.pageCount(), gui.getPage(pl) + count)));
        playerGuis.put(pl, gui);
    }

    public void open(MPlayer pl, int page) {

        if(openRequirement != null && !openRequirement.checkOrDeny(this, pl, null)) {
            return;
        }

        InventoryGUI gui = createGUI(pl);

        gui.addCloseCallback(this::close);
        gui.setPageSize(pageSize);
        gui.open(pl, page);

        playerGuis.put(pl, gui);
    }

    public void open(MPlayer pl) {
        open(pl, 0);
    }

    public void close(MPlayer pl) {
        InventoryGUI gui = playerGuis.remove(pl);
        if(gui != null) gui.close(pl);
    }

    public String getCommand() {
        return command;
    }

    public String getCommandPermission() {
        return commandPermission;
    }

    public static MidnightMenu parse(ConfigSection sec) {

        MComponent title = MComponent.parse(sec.getString("title"));
        int pageSize = sec.has("page_size", Number.class) ? sec.getInt("page_size") : 0;
        MenuRequirement req = sec.has("requirement", ConfigSection.class) ? MenuRequirement.SERIALIZER.deserialize(sec.getSection("requirement")) : null;

        String command = sec.getOrDefault("command", null, String.class);
        String commandPermission = sec.getOrDefault("command_permission", null, String.class);

        MidnightMenu out = new MidnightMenu(title, pageSize, req, command, commandPermission);

        sec.getListFiltered("entries", ConfigSection.class).forEach(ent -> out.entries.add(Entry.parse(ent)));
        return out;
    }


    private static class Entry {
        MItemStack item;
        String name;
        List<String> lore;
        int slot;
        int priority;
        MenuRequirement viewRequirement;
        HashMap<InventoryGUI.ClickType, List<MenuAction>> actions = new HashMap<>();

        public MItemStack getItem(MidnightMenu menu, MPlayer player) {

            MItemStack is = item.copy();
            LangModule mod = MidnightCoreAPI.getInstance().getModuleManager().getModule(LangModule.class);
            if(name != null) {
                is.setName(mod.parseText(name, player, menu));
            }
            if(lore != null) {
                List<MComponent> newLore = new ArrayList<>();
                lore.forEach(str -> newLore.add(mod.parseText(str, player, menu)));
                is.setLore(newLore);
            }
            return is;
        }

        static Entry parse(ConfigSection sec) {

            Entry out = new Entry();

            out.slot = sec.getInt("slot");
            out.priority = sec.has("priority") ? sec.getInt("priority") : 0;
            out.item = sec.get("item", MItemStack.class);
            out.name = sec.getOrDefault("name", null, String.class);
            out.viewRequirement = sec.has("requirement", ConfigSection.class) ? MenuRequirement.SERIALIZER.deserialize(sec.getSection("requirement")) : null;

            if(sec.has("lore", List.class)) out.lore = sec.getListFiltered("lore", String.class);

            ConfigSection actions = sec.getSection("actions");
            if(actions != null) {
                for(String s : actions.getKeys()) {
                    try {
                        InventoryGUI.ClickType t = InventoryGUI.ClickType.valueOf(s.toUpperCase(Locale.ROOT));
                        out.actions.put(t, actions.getListFiltered(s, MenuAction.class));

                    } catch (IllegalStateException ex) {

                        MidnightMenusAPI.getLogger().warn("Unknown click type: " + s + "!");
                    }
                }
            }

            return out;

        }


    }

}
