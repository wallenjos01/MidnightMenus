package org.wallentines.midnightmenus.api.menu;

import org.wallentines.mdcfg.serializer.ObjectSerializer;
import org.wallentines.mdcfg.serializer.Serializer;
import org.wallentines.midnightcore.api.item.MItemStack;
import org.wallentines.midnightcore.api.player.MPlayer;

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

    public static final Serializer<MenuAction> SERIALIZER = ObjectSerializer.create(
            MenuActionType.ACTION_TYPE_REGISTRY.nameSerializer().entry("type", ma -> ma.type),
            Serializer.STRING.entry("value", ma -> ma.value),
            MenuRequirement.SERIALIZER.<MenuAction>entry("requirement", ma -> ma.requirement).optional(),
            MenuAction::new
    );

}
