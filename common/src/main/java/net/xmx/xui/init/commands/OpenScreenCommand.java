/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.init.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * Command to open screens via reflection.
 * Fixed to run the UI operation on the correct Render Thread.
 *
 * @author xI-Mx-Ix
 */
public class OpenScreenCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                LiteralArgumentBuilder.<CommandSourceStack>literal("openscreen")
                        .requires(source -> source.hasPermission(4)) // Operators only
                        .then(Commands.argument("screen", StringArgumentType.string())
                                .executes(context -> {
                                    String screenName = StringArgumentType.getString(context, "screen");
                                    CommandSourceStack source = context.getSource();

                                    try {
                                        Class<?> screenClass = Class.forName(screenName);

                                        if (Screen.class.isAssignableFrom(screenClass)) {
                                            Screen screenToOpen = (Screen) screenClass.getDeclaredConstructor().newInstance();

                                            Minecraft.getInstance().execute(() -> {
                                                Minecraft.getInstance().setScreen(screenToOpen);
                                            });

                                            source.sendSuccess(() ->
                                                    Component.literal("Screen opened: " + screenName), false
                                            );
                                            System.out.println("[Client] Screen opened: " + screenName);
                                        } else {
                                            throw new IllegalArgumentException("Invalid Screen class: " + screenName);
                                        }
                                    } catch (Exception e) {
                                        String msg = "Error opening screen: " + e.getMessage();
                                        source.sendFailure(Component.literal(msg));
                                        System.err.println("[Client] " + msg);
                                        e.printStackTrace();
                                    }
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
        );
    }
}