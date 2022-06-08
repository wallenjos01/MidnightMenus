package org.wallentines.midnightmenus.api.menu;

import org.wallentines.midnightcore.api.item.MItemStack;
import org.wallentines.midnightcore.api.player.MPlayer;
import org.wallentines.midnightlib.config.ConfigSection;
import org.wallentines.midnightlib.config.serialization.ConfigSerializer;
import org.wallentines.midnightlib.registry.Identifier;

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

            MenuActionType type = MenuActionType.ACTION_TYPE_REGISTRY.get(Identifier.parseOrDefault(section.getString("type"), "midnightmenus"));
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
