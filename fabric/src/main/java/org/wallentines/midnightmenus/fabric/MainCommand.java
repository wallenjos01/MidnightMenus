package org.wallentines.midnightmenus.fabric;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import me.lucko.fabric.api.permissions.v0.Permissions;
import org.wallentines.midnightcore.api.text.CustomPlaceholderInline;
import org.wallentines.midnightcore.fabric.player.FabricPlayer;
import org.wallentines.midnightcore.fabric.util.CommandUtil;
import org.wallentines.midnightcore.fabric.util.ConversionUtil;
import org.wallentines.midnightlib.registry.Registry;
import org.wallentines.midnightmenus.api.MidnightMenusAPI;
import org.wallentines.midnightmenus.api.menu.MidnightMenu;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(
            Commands.literal("menu")
                .requires(Permissions.require("midnightmenus.command", 3))
                .then(Commands.literal("open")
                    .requires(Permissions.require("midnightmenus.command.open", 3))
                    .then(Commands.argument("menu", ResourceLocationArgument.id())
                        .suggests((context, builder) -> {
                            Registry<MidnightMenu> menus = MidnightMenusAPI.getInstance().getMenuRegistry();
                            List<ResourceLocation> out = new ArrayList<>(menus.getSize());
                            for(int i = 0 ; i < menus.getSize() ; i++) {
                                out.add(ConversionUtil.toResourceLocation(menus.idAtIndex(i)));
                            }
                            return SharedSuggestionProvider.suggestResource(out, builder);
                        })
                        .executes(context -> openCommand(context, context.getArgument("menu", ResourceLocation.class), context.getSource().getPlayerOrException()))
                        .then(Commands.argument("targets", EntityArgument.players())
                            .executes(context -> openCommand(context, context.getArgument("menu", ResourceLocation.class), context.getArgument("targets", EntitySelector.class).findPlayers(context.getSource())))
                        )
                    )
                )
                .then(Commands.literal("reload")
                    .requires(Permissions.require("midnightmenus.command.open", 3))
                    .executes(MainCommand::reloadCommand)
                )
            );
    }

    private static int openCommand(CommandContext<CommandSourceStack> stack, ResourceLocation menuId, ServerPlayer target) {

        return openCommand(stack, menuId, Collections.singletonList(target));
    }


    private static int openCommand(CommandContext<CommandSourceStack> stack, ResourceLocation menuId, List<ServerPlayer> targets) {

        try {
            MidnightMenu menu = MidnightMenusAPI.getInstance().getMenuRegistry().get(ConversionUtil.toIdentifier(menuId));
            if (menu == null) {
                CommandUtil.sendCommandFailure(stack, MidnightMenusAPI.getInstance().getLangProvider(), "command.error.invalid_menu");
                return 0;
            }

            for (ServerPlayer pl : targets) {
                menu.open(FabricPlayer.wrap(pl), 0);
            }

            if (targets.size() == 1) {
                CommandUtil.sendCommandSuccess(stack, MidnightMenusAPI.getInstance().getLangProvider(), false, "command.open.result", FabricPlayer.wrap(targets.get(0)), CustomPlaceholderInline.create("menu_id", menuId.toString()));
            } else {
                CommandUtil.sendCommandSuccess(stack, MidnightMenusAPI.getInstance().getLangProvider(), false, "command.open.result.multiple", CustomPlaceholderInline.create("player_count", targets.size() + ""), CustomPlaceholderInline.create("menu_id", menuId.toString()));
            }

        } catch(Exception ex) {
            ex.printStackTrace();
        }

        return targets.size();

    }

    private static int reloadCommand(CommandContext<CommandSourceStack> context) {

        try {
            long time = System.currentTimeMillis();
            MidnightMenusAPI.getInstance().reload();
            time = System.currentTimeMillis() - time;

            CommandUtil.sendCommandSuccess(context, MidnightMenusAPI.getInstance().getLangProvider(), false, "command.reload.result", CustomPlaceholderInline.create("elapsed", time + ""));

            return (int) time;
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return 0;
    }


}
