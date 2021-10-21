package me.m1dnightninja.midnightmenus.api.menu;

import me.m1dnightninja.midnightcore.api.config.ConfigSection;
import me.m1dnightninja.midnightcore.api.config.ConfigSerializer;
import me.m1dnightninja.midnightcore.api.inventory.MItemStack;
import me.m1dnightninja.midnightcore.api.player.MPlayer;
import me.m1dnightninja.midnightcore.api.player.Requirement;
import me.m1dnightninja.midnightcore.api.player.RequirementType;
import me.m1dnightninja.midnightcore.api.registry.MIdentifier;
import me.m1dnightninja.midnightmenus.api.MidnightMenusAPI;

public class MenuRequirement {

    private final Requirement requirement;
    private final MenuAction denyAction;

    public MenuRequirement(Requirement requirement, MenuAction denyAction) {
        this.requirement = requirement;
        this.denyAction = denyAction;
    }

    public boolean check(MPlayer player) {
        return requirement.check(player);
    }

    @Override
    public String toString() {
        return RequirementType.REQUIREMENT_TYPE_REGISTRY.getId(requirement.getType()) + "(" + requirement.getValue() + ")";
    }

    public boolean checkOrDeny(MidnightMenu menu, MPlayer player, MItemStack stack) {

        if(requirement.check(player)) return true;

        if(denyAction != null) {
            denyAction.execute(menu, player, stack);
        }
        return false;
    }

    public static final ConfigSerializer<MenuRequirement> SERIALIZER = new ConfigSerializer<>() {
        @Override
        public MenuRequirement deserialize(ConfigSection section) {

            if(section.has("id", String.class)) {

                MIdentifier id = MIdentifier.parse("id");
                MenuRequirement out = MidnightMenusAPI.REQUIREMENT_REGISTRY.get(id);
                if(out != null) return out;

            }

            Requirement req = Requirement.SERIALIZER.deserialize(section);
            MenuAction action = null;

            if (section.has("deny_action", ConfigSection.class)) {
                action = MenuAction.SERIALIZER.deserialize(section.getSection("deny_action"));
            }

            return new MenuRequirement(req, action);
        }

        @Override
        public ConfigSection serialize(MenuRequirement object) {

            ConfigSection out = Requirement.SERIALIZER.serialize(object.requirement);
            if(object.denyAction != null) {
                out.set("deny_action", MenuAction.SERIALIZER.serialize(object.denyAction));
            }

            return out;
        }
    };

}
