package com.titammods.hephaestus_tools.client.workbench;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.titammods.hephaestus_tools.tables.scene.WorkbenchTuning;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.Locale;

final class WorkbenchTuningCommand {
    private WorkbenchTuningCommand() {}

    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        WorkbenchTuningStore.ensureLoaded();
        dispatcher.register(Commands.literal("hcam")
                .then(Commands.literal("skip")
                        .then(Commands.argument("on", com.mojang.brigadier.arguments.BoolArgumentType.bool())
                                .executes(ctx -> {
                                    boolean on = com.mojang.brigadier.arguments.BoolArgumentType.getBool(ctx, "on");
                                    WorkbenchTuningStore.setSkipCinematics(on);
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "[hcam] skip cinematics = " + on), false);
                                    return 1;
                                })))
                .then(Commands.literal("get").executes(ctx -> print(ctx.getSource(), false)))
                .then(Commands.literal("dump").executes(ctx -> print(ctx.getSource(), true)))
                .then(Commands.literal("reset").executes(ctx -> {
                    WorkbenchTuning.resetAll();
                    WorkbenchTuningStore.save();
                    ctx.getSource().sendSuccess(() -> Component.literal("[hcam] defaults restored"), false);
                    return 1;
                }))
                .then(Commands.literal("set")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                        Arrays.stream(WorkbenchTuning.values())
                                                .map(t -> t.name().toLowerCase(Locale.ROOT)), builder))
                                .then(Commands.argument("value", DoubleArgumentType.doubleArg())
                                        .executes(ctx -> {
                                            String name = StringArgumentType.getString(ctx, "name");
                                            double value = DoubleArgumentType.getDouble(ctx, "value");
                                            var param = WorkbenchTuning.byName(name);
                                            if (param.isEmpty()) {
                                                ctx.getSource().sendFailure(Component.literal("[hcam] unknown value: " + name));
                                                return 0;
                                            }
                                            param.get().set(value);
                                            WorkbenchTuningStore.save();
                                            ctx.getSource().sendSuccess(() -> Component.literal(
                                                    "[hcam] " + param.get().name() + " = " + fmt(param.get().get())), false);
                                            return 1;
                                        })))));
    }

    private static int print(CommandSourceStack source, boolean asDefaults) {
        for (WorkbenchTuning t : WorkbenchTuning.values()) {
            String line = asDefaults
                    ? t.name() + "(" + fmt(t.get()) + "),"
                    : t.name().toLowerCase(Locale.ROOT) + " = " + fmt(t.get())
                            + (t.get() == t.defaultValue() ? "" : "  (default " + fmt(t.defaultValue()) + ")");
            source.sendSuccess(() -> Component.literal(line), false);
        }
        return 1;
    }

    private static String fmt(double v) {
        return String.format(Locale.ROOT, "%.3f", v);
    }
}
