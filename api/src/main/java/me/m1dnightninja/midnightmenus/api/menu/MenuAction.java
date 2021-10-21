package me.m1dnightninja.midnightmenus.api.menu;

import me.m1dnightninja.midnightcore.api.config.ConfigSection;
import me.m1dnightninja.midnightcore.api.config.ConfigSerializer;
import me.m1dnightninja.midnightcore.api.inventory.MItemStack;
import me.m1dnightninja.midnightcore.api.player.MPlayer;
import me.m1dnightninja.midnightcore.api.registry.MIdentifier;

public class MenuAction {

    private final MenuActionType type;
    private final String value;
    private final MenuRequirement requirement;

    public MenuAction(MenuActionType type, String value) {
        this.type = type;
        this.value = value;
        this.requirement = null;
    }

    public MenuAction(MenuActionType type, String value, MenuRequirement req) {
        this.type = type;
        this.value = value;
        this.requirement = req;
    }

    public void execute(MidnightMenu menu, MPlayer player, MItemStack stack) {

        if(requirement != null && !requirement.checkOrDeny(menu, player, stack)) return;
        type.execute(menu, player, value, stack);

    }

    public static final ConfigSerializer<MenuAction> SERIALIZER = new ConfigSerializer<>() {
        @Override
        public MenuAction deserialize(ConfigSection section) {

            MenuActionType type = MenuActionType.ACTION_TYPE_REGISTRY.get(MIdentifier.parseOrDefault(section.getString("type"), "midnightmenus"));
            String value = section.has("value") ? section.getString("value") : "";

            MenuRequirement requirement = null;
            if(section.has("requirement", ConfigSection.class)) {
                requirement = MenuRequirement.SERIALIZER.deserialize(section.getSection("requirement"));
            }

            return new MenuAction(type, value, requirement);
        }

        @Override
        public ConfigSection serialize(MenuAction object) {
            return null;
        }
    };

}
