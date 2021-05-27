package me.m1dnightninja.midnightmenus.api.menu;

import me.m1dnightninja.midnightcore.api.MidnightCoreAPI;
import me.m1dnightninja.midnightcore.api.config.ConfigSection;
import me.m1dnightninja.midnightcore.api.inventory.AbstractInventoryGUI;
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
    private final HashMap<MPlayer, AbstractInventoryGUI> playerGuis = new HashMap<>();

    private final MComponent title;
    private final int pageSize;
    private final MenuRequirement openRequirement;

    public MidnightMenu(MComponent title, int pageSize, MenuRequirement requirement) {
        this.title = title;
        this.pageSize = pageSize;
        this.openRequirement = requirement;
    }

    private AbstractInventoryGUI createGUI(MPlayer player) {

        HashMap<Integer, Entry> visible = new HashMap<>();
        for(Entry ent : entries) {
            if(ent.viewRequirement != null && !ent.viewRequirement.check(player)) continue;
            if(visible.containsKey(ent.slot) && visible.get(ent.slot).priority >= ent.priority) continue;

            visible.put(ent.slot, ent);
        }

        ILangModule langModule = MidnightMenusAPI.getInstance().getLangProvider().getModule();

        AbstractInventoryGUI igui = MidnightCoreAPI.getInstance().createInventoryGUI(langModule.applyPlaceholders(title, player));

        for(Entry ent : visible.values()) {

            int aPageSize = (pageSize == 0 ? 6 : pageSize);
            int slot = ent.slot % (aPageSize * 9);
            igui.setItem(ent.createItem(player), slot, (type, user) -> {
                for(MenuAction act : ent.actions.get(type)) {
                    act.execute(MidnightMenu.this, player);
                }
            });

        }

        return igui;
    }

    public void addEntry(int index, int priority, ConfigSection itemData, MenuRequirement viewRequirement, HashMap<AbstractInventoryGUI.ClickType, List<MenuAction>> actions ) {

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

        AbstractInventoryGUI gui = playerGuis.get(pl);
        gui.open(pl, Math.max(0, Math.min(gui.pageCount(), gui.getPlayerPage(pl) + count)));
    }

    public void open(MPlayer pl, int page) {

        if(openRequirement != null && !openRequirement.checkOrDeny(pl)) {
            return;
        }

        AbstractInventoryGUI gui = createGUI(pl);

        gui.addCallback(this::close);
        gui.setPageSize(pageSize);
        gui.open(pl, page);

        playerGuis.put(pl, gui);
    }

    public void open(MPlayer pl) {
        open(pl, 0);
    }

    public void close(MPlayer pl) {
        AbstractInventoryGUI gui = playerGuis.remove(pl);
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
        HashMap<AbstractInventoryGUI.ClickType, List<MenuAction>> actions = new HashMap<>();

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
                    AbstractInventoryGUI.ClickType t = AbstractInventoryGUI.ClickType.valueOf(s.toUpperCase(Locale.ROOT));

                    List<MenuAction> acts = new ArrayList<>();
                    List<ConfigSection> secs = actions.getList(s, ConfigSection.class);
                    for(ConfigSection o : secs) {
                        acts.add(MenuAction.SERIALIZER.deserialize(o));
                    }

                    out.actions.put(t, acts);

                }
            }

            return out;

        }


    }

}
