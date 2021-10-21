package me.m1dnightninja.midnightmenus.api.menu;

import me.m1dnightninja.midnightcore.api.MidnightCoreAPI;
import me.m1dnightninja.midnightcore.api.config.ConfigSection;
import me.m1dnightninja.midnightcore.api.inventory.MInventoryGUI;
import me.m1dnightninja.midnightcore.api.inventory.MItemStack;
import me.m1dnightninja.midnightcore.api.module.lang.ILangModule;
import me.m1dnightninja.midnightcore.api.player.MPlayer;
import me.m1dnightninja.midnightcore.api.text.MComponent;
import me.m1dnightninja.midnightmenus.api.MidnightMenusAPI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class MidnightMenu {

    private final List<Entry> entries = new ArrayList<>();
    private final HashMap<MPlayer, MInventoryGUI> playerGuis = new HashMap<>();

    private final MComponent title;
    private final int pageSize;
    private final MenuRequirement openRequirement;

    public MidnightMenu(MComponent title, int pageSize, MenuRequirement requirement) {
        this.title = title;
        this.pageSize = pageSize;
        this.openRequirement = requirement;
    }

    private MInventoryGUI createGUI(MPlayer player) {

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

        ILangModule langModule = MidnightMenusAPI.getInstance().getLangProvider().getModule();

        MInventoryGUI igui = MidnightCoreAPI.getInstance().createInventoryGUI(langModule.applyPlaceholders(title, player));

        for(Entry ent : visible.values()) {

            MItemStack is = ent.createItem(player);
            igui.setItem(is, ent.slot, new MInventoryGUI.ClickAction() {

                final Entry entry = ent;

                @Override
                public void onClick(MInventoryGUI.ClickType type, MPlayer user) {

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

    public void addEntry(int index, int priority, ConfigSection itemData, MenuRequirement viewRequirement, HashMap<MInventoryGUI.ClickType, List<MenuAction>> actions ) {

        Entry ent = new Entry();
        ent.slot = index;
        ent.priority = priority;
        ent.item = itemData;
        ent.viewRequirement = viewRequirement;
        ent.actions = actions;

        entries.add(ent);

    }

    public int getPage(MPlayer pl) {
        if(!playerGuis.containsKey(pl)) return -1;
        return playerGuis.get(pl).getPlayerPage(pl);
    }

    public void advancePage(MPlayer pl, int count) {
        if(!playerGuis.containsKey(pl)) return;

        MInventoryGUI gui = playerGuis.get(pl);
        gui.open(pl, Math.max(0, Math.min(gui.pageCount(), gui.getPlayerPage(pl) + count)));
        playerGuis.put(pl, gui);
    }

    public void open(MPlayer pl, int page) {

        if(openRequirement != null && !openRequirement.checkOrDeny(this, pl, null)) {
            return;
        }

        MInventoryGUI gui = createGUI(pl);

        gui.addCallback(this::close);
        gui.setPageSize(pageSize);
        gui.open(pl, page);

        playerGuis.put(pl, gui);
    }

    public void open(MPlayer pl) {
        open(pl, 0);
    }

    public void close(MPlayer pl) {
        MInventoryGUI gui = playerGuis.remove(pl);
        if(gui != null) gui.close(pl);
    }

    public static MidnightMenu parse(ConfigSection sec) {

        MComponent title = MComponent.Serializer.parse(sec.getString("title"));
        int pageSize = sec.has("page_size", Number.class) ? sec.getInt("page_size") : 0;
        MenuRequirement req = sec.has("requirement", ConfigSection.class) ? MenuRequirement.SERIALIZER.deserialize(sec.getSection("requirement")) : null;

        MidnightMenu out = new MidnightMenu(title, pageSize, req);
        if(sec.has("entries", List.class)) {

            for(ConfigSection ent : sec.getList("entries", ConfigSection.class)) {

                out.entries.add(Entry.parse(ent));
            }
        }

        return out;
    }


    private static class Entry {
        ConfigSection item;
        int slot;
        int priority;
        MenuRequirement viewRequirement;
        HashMap<MInventoryGUI.ClickType, List<MenuAction>> actions = new HashMap<>();

        private static ILangModule langModule;

        private static ConfigSection parseSection(ConfigSection sec, MPlayer player) {

            ConfigSection out = new ConfigSection();

            for(String s : sec.getKeys()) {

                out.set(s, parseObject(sec.get(s), player));

            }

            return out;
        }

        private static Object parseObject(Object obj, MPlayer player) {

            if(langModule == null) {
                langModule = MidnightMenusAPI.getInstance().getLangProvider().getModule();
            }

            if(obj instanceof String) {

                return langModule.applyPlaceholdersFlattened((String) obj, player);

            } else if(obj instanceof List) {

                List<Object> out = new ArrayList<>();
                for(Object o : (List<?>) obj) {
                    out.add(parseObject(o, player));
                }

                return out;

            } else if(obj instanceof ConfigSection) {

                return parseSection((ConfigSection) obj, player);
            }
            return obj;
        }

        MItemStack createItem(MPlayer player) {

            ConfigSection sec = parseSection(item, player);
            return MItemStack.SERIALIZER.deserialize(sec);
        }

        static Entry parse(ConfigSection sec) {

            Entry out = new Entry();

            out.slot = sec.getInt("slot");
            out.priority = sec.has("priority") ? sec.getInt("priority") : 0;
            out.item = sec.getSection("item");
            out.viewRequirement = sec.has("requirement", ConfigSection.class) ? MenuRequirement.SERIALIZER.deserialize(sec.getSection("requirement")) : null;

            ConfigSection actions = sec.getSection("actions");
            if(actions != null) {
                for(String s : actions.getKeys()) {
                    try {
                        MInventoryGUI.ClickType t = MInventoryGUI.ClickType.valueOf(s.toUpperCase(Locale.ROOT));
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
