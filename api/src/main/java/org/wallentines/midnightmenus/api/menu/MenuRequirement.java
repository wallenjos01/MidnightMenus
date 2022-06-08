package org.wallentines.midnightmenus.api.menu;

import org.wallentines.midnightcore.api.MidnightCoreAPI;
import org.wallentines.midnightcore.api.item.MItemStack;
import org.wallentines.midnightcore.api.player.MPlayer;
import org.wallentines.midnightlib.config.ConfigSection;
import org.wallentines.midnightlib.config.serialization.ConfigSerializer;
import org.wallentines.midnightlib.registry.Identifier;
import org.wallentines.midnightlib.registry.Registry;
import org.wallentines.midnightlib.requirement.Requirement;
import org.wallentines.midnightlib.requirement.RequirementType;
import org.wallentines.midnightmenus.api.MidnightMenusAPI;

public class MenuRequirement {

    private final Requirement<MPlayer> requirement;
    private final MenuAction denyAction;

    public MenuRequirement(Requirement<MPlayer> requirement, MenuAction denyAction) {
        this.requirement = requirement;
        this.denyAction = denyAction;
    }

    public boolean check(MPlayer player) {
        return requirement.check(player);
    }

    @Override
    public String toString() {

        Registry<RequirementType<MPlayer>> reg = MidnightCoreAPI.getInstance().getRequirementRegistry();

        return reg.getId(requirement.getType()) + "(" + requirement.getValue() + ")";
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

                Identifier id = Identifier.parse("id");
                MenuRequirement out = MidnightMenusAPI.getInstance().getRequirementRegistry().get(id);
                if(out != null) return out;

            }

            Requirement.RequirementSerializer<MPlayer> serializer = new Requirement.RequirementSerializer<>(MidnightCoreAPI.getInstance().getRequirementRegistry());
            Requirement<MPlayer> req = serializer.deserialize(section);
            MenuAction action = null;

            if (section.has("deny_action", ConfigSection.class)) {
                action = MenuAction.SERIALIZER.deserialize(section.getSection("deny_action"));
            }

            return new MenuRequirement(req, action);
        }

        @Override
        public ConfigSection serialize(MenuRequirement object) {

            Requirement.RequirementSerializer<MPlayer> serializer = new Requirement.RequirementSerializer<>(MidnightCoreAPI.getInstance().getRequirementRegistry());
            ConfigSection out = serializer.serialize(object.requirement);
            if(object.denyAction != null) {
                out.set("deny_action", MenuAction.SERIALIZER.serialize(object.denyAction));
            }

            return out;
        }
    };

}
