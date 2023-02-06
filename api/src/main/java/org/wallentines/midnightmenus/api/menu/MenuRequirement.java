package org.wallentines.midnightmenus.api.menu;

import org.wallentines.mdcfg.serializer.SerializeContext;
import org.wallentines.mdcfg.serializer.SerializeResult;
import org.wallentines.mdcfg.serializer.Serializer;
import org.wallentines.midnightcore.api.Registries;
import org.wallentines.midnightcore.api.item.MItemStack;
import org.wallentines.midnightcore.api.player.MPlayer;
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

        Registry<RequirementType<MPlayer>> reg = Registries.REQUIREMENT_REGISTRY;

        return reg.getId(requirement.getType()) + "(" + requirement.getValue() + ")";
    }

    public boolean checkOrDeny(MidnightMenu menu, MPlayer player, MItemStack stack) {

        if(requirement.check(player)) return true;

        if(denyAction != null) {
            denyAction.execute(menu, player, stack);
        }
        return false;
    }

    public static final Serializer<MenuRequirement> SERIALIZER = new Serializer<>() {

        @Override
        public <O> SerializeResult<O> serialize(SerializeContext<O> context, MenuRequirement value) {

            Serializer<Requirement<MPlayer>> serializer = Requirement.serializer(Registries.REQUIREMENT_REGISTRY);
            SerializeResult<O> res = serializer.serialize(context, value.requirement);
            if(!res.isComplete()) return SerializeResult.failure(res.getError());

            O out = res.getOrThrow();
            if(value.denyAction != null) {

                SerializeResult<O> denyResult = MenuAction.SERIALIZER.serialize(context, value.denyAction);
                if(!denyResult.isComplete()) return SerializeResult.failure(res.getError());

                context.set("deny_action", denyResult.getOrThrow(), out);
            }

            return SerializeResult.success(out);
        }

        @Override
        public <O> SerializeResult<MenuRequirement> deserialize(SerializeContext<O> context, O value) {

            Registry<MenuRequirement> registry = MidnightMenusAPI.getInstance().getRequirementRegistry();
            O id = context.get("id", value);
            if(context.isString(id)) {
                return registry.nameSerializer().deserialize(context, id);
            }

            Serializer<Requirement<MPlayer>> serializer = Requirement.serializer(Registries.REQUIREMENT_REGISTRY);
            SerializeResult<Requirement<MPlayer>> res = serializer.deserialize(context, value);
            if(!res.isComplete()) return SerializeResult.failure(res.getError());

            O deny = context.get("deny_action", value);
            MenuAction act = null;
            if(deny != null) {
                act = MenuAction.SERIALIZER.deserialize(context, deny).get().orElse(null);
            }

            return SerializeResult.success(new MenuRequirement(res.getOrThrow(), act));
        }
    };
}
