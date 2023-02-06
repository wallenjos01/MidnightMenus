package org.wallentines.midnightmenus.api.menu;

import org.wallentines.mdcfg.ConfigObject;
import org.wallentines.mdcfg.ConfigSection;
import org.wallentines.mdcfg.serializer.*;
import org.wallentines.midnightcore.api.item.InventoryGUI;
import org.wallentines.midnightcore.api.item.MItemStack;
import org.wallentines.midnightcore.api.player.MPlayer;
import org.wallentines.midnightcore.api.text.MComponent;
import org.wallentines.midnightcore.api.text.PlaceholderManager;
import org.wallentines.midnightmenus.api.MidnightMenusAPI;

import java.util.*;

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

        InventoryGUI igui = player.getServer().getMidnightCore().createGUI(PlaceholderManager.INSTANCE.applyPlaceholders(title, player));

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


    public static final Serializer<MidnightMenu> SERIALIZER = ObjectSerializer.create(
            MComponent.SERIALIZER.entry("title", menu -> menu.title),
            NumberSerializer.forInt(0, 6).<MidnightMenu>entry("page_size", menu -> menu.pageSize).orElse(0),
            MenuRequirement.SERIALIZER.<MidnightMenu>entry("requirement", menu -> menu.openRequirement).optional(),
            Serializer.STRING.entry("command", MidnightMenu::getCommand).optional(),
            Serializer.STRING.entry("command_permission", MidnightMenu::getCommandPermission).optional(),
            Entry.SERIALIZER.listOf().entry("entries", menu -> menu.entries),
            (title, size, requirement, command, commandPerm, entries) -> {

                MidnightMenu out = new MidnightMenu(title, size, requirement, command, commandPerm);
                out.entries.addAll(entries);
                return out;
            }
    );


    private static class Entry {
        MItemStack item;
        String name;
        List<String> lore = new ArrayList<>();
        int slot;
        int priority;
        MenuRequirement viewRequirement;
        HashMap<InventoryGUI.ClickType, Collection<MenuAction>> actions = new HashMap<>();


        public Entry(MItemStack item, String name, Collection<String> lore, int slot, int priority, MenuRequirement viewRequirement, Map<InventoryGUI.ClickType, Collection<MenuAction>> actions) {
            this.item = item;
            this.name = name;
            if(lore != null) this.lore.addAll(lore);
            this.slot = slot;
            this.priority = priority;
            this.viewRequirement = viewRequirement;
            if(actions != null) this.actions.putAll(actions);
        }

        public MItemStack getItem(MidnightMenu menu, MPlayer player) {

            ConfigSection tag = item.getTag().copy();
            resolveTag(player, tag);

            MItemStack.Builder builder = MItemStack.Builder.of(item.getType()).withAmount(item.getCount()).withTag(tag);

            if (name != null) {
                builder.withName(PlaceholderManager.INSTANCE.parseText(name, player, menu));
            }
            if (lore != null) {
                List<MComponent> newLore = new ArrayList<>();
                lore.forEach(str -> newLore.add(PlaceholderManager.INSTANCE.parseText(str, player, menu)));
                builder.withLore(newLore);
            }
            return builder.build();
        }

        private ConfigSection resolveTag(MPlayer player, ConfigSection section) {

            for(String key : section.getKeys()) {
                ConfigObject obj = section.get(key);
                if(obj.isSection()) {
                    section.set(key, resolveTag(player, obj.asSection()));
                } else if(obj.isString()) {
                    section.set(key, PlaceholderManager.INSTANCE.applyPlaceholdersFlattened(obj.asString(), player));
                } else {
                    section.set(key, obj);
                }
            }
            return section;
        }


        static final Serializer<Entry> SERIALIZER = ObjectSerializer.create(
                MItemStack.SERIALIZER.entry("item", e -> e.item),
                Serializer.STRING.<Entry>entry("name", e -> e.name).optional(),
                Serializer.STRING.listOf().<Entry>entry("lore", e -> e.lore).optional(),
                Serializer.INT.entry("slot", e -> e.slot),
                Serializer.INT.<Entry>entry("priority", e -> e.priority).orElse(0),
                MenuRequirement.SERIALIZER.<Entry>entry("requirement", e -> e.viewRequirement).optional(),
                MenuAction.SERIALIZER.listOf().mapOf(InlineSerializer.of(InventoryGUI.ClickType::name, str -> InventoryGUI.ClickType.valueOf(str.toUpperCase()))).<Entry>entry("actions", e -> e.actions).optional(),
                Entry::new
        );
    }

}
