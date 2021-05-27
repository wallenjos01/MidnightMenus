package me.m1dnightninja.midnightmenus.api.menu;

import me.m1dnightninja.midnightcore.api.config.ConfigSection;
import me.m1dnightninja.midnightcore.api.config.ConfigSerializer;
import me.m1dnightninja.midnightcore.api.player.MPlayer;
import me.m1dnightninja.midnightcore.api.registry.MIdentifier;
import me.m1dnightninja.midnightmenus.api.MidnightMenusAPI;

public class MenuRequirement {

    private final MenuRequirementType type;
    private final String value;
    private final MenuAction denyAction;

    public MenuRequirement(MenuRequirementType type, String value, MenuAction denyAction) {
        this.type = type;
        this.value = value;
        this.denyAction = denyAction;
    }

    public boolean check(MPlayer player) {

        return type.check(player, value);
    }

    public boolean checkOrDeny(MPlayer player) {

        if(type.check(player, value)) return true;

        if(denyAction != null) {
            denyAction.execute(null, player);
        }
        return false;
    }

    public static final ConfigSerializer<MenuRequirement> SERIALIZER = new ConfigSerializer<MenuRequirement>() {
        @Override
        public MenuRequirement deserialize(ConfigSection section) {

            if(section.has("id", String.class)) {

                MIdentifier id = MIdentifier.parse("id");
                MenuRequirement out = MidnightMenusAPI.REQUIREMENT_REGISTRY.get(id);
                if(out != null) return out;

            }

            MenuRequirementType type = MenuRequirementType.REQUIREMENT_TYPE_REGISTRY.get(MIdentifier.parseOrDefault(section.getString("type"), "midnightmenus"));
            String value = section.getString("value");
            MenuAction action = null;

            if (section.has("deny_action", ConfigSection.class)) {
                action = MenuAction.SERIALIZER.deserialize(section.getSection("deny_action"));
            }

            return new MenuRequirement(type, value, action);
        }

        @Override
        public ConfigSection serialize(MenuRequirement object) {

            ConfigSection out = new ConfigSection();
            out.set("type", MenuRequirementType.REQUIREMENT_TYPE_REGISTRY.getId(object.type).toString());
            out.set("value", object.value);

            return out;
        }
    };

}
