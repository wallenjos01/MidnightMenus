package me.m1dnightninja.midnightmenus.api.menu;

import me.m1dnightninja.midnightcore.api.player.MPlayer;
import me.m1dnightninja.midnightcore.api.registry.MIdentifier;
import me.m1dnightninja.midnightcore.api.registry.MRegistry;

public interface MenuRequirementType {

    boolean check(MPlayer player, String value);

    MRegistry<MenuRequirementType> REQUIREMENT_TYPE_REGISTRY = new MRegistry<>();

    static MenuRequirementType register(String id, MenuRequirementType act) {
        return REQUIREMENT_TYPE_REGISTRY.register(MIdentifier.parseOrDefault(id, "midnightmenus"), act);
    }

    MenuRequirementType PERMISSION = register("permission", MPlayer::hasPermission);
    MenuRequirementType WORLD = register("world", (player, value) -> player.getDimension().equals(MIdentifier.parseOrDefault(value)));

}
