package com.createconnection;

import com.createconnection.gametest.ConnectionGameTests;
import com.createconnection.phone.CallManager;
import com.createconnection.phone.PhoneLineTracer;
import com.createconnection.phone.PhoneSavedData;
import com.createconnection.phone.TelephoneBlockEntity;
import com.createconnection.radio.RadioBlockEntity;
import com.createconnection.radio.RadioRegistry;
import com.createconnection.radio.RadioStationBlockEntity;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.gametest.framework.GameTestTicker;
import net.minecraft.gametest.framework.RetryOptions;
import net.minecraft.gametest.framework.StructureUtils;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.StructureBlockEntity;

public final class DevCommands {
    private DevCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("cc")
            .requires(s -> s.hasPermission(2))
            .then(Commands.literal("list").executes(DevCommands::listPhones))
            .then(Commands.literal("radios").executes(DevCommands::listRadios))
            .then(Commands.literal("test").executes(ctx -> runTests(ctx, null))
                .then(Commands.argument("name", StringArgumentType.word())
                    .executes(ctx -> runTests(ctx, StringArgumentType.getString(ctx, "name")))))
            .then(Commands.literal("probe")
                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                    .executes(DevCommands::probePhone)))
            .then(Commands.literal("line")
                .then(Commands.argument("a", BlockPosArgument.blockPos())
                    .then(Commands.argument("b", BlockPosArgument.blockPos())
                        .executes(DevCommands::checkLine))))
            .then(Commands.literal("dial")
                .then(Commands.argument("from", BlockPosArgument.blockPos())
                    .then(Commands.argument("number", StringArgumentType.word())
                        .executes(DevCommands::dialFrom))))
            .then(Commands.literal("answer")
                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                    .executes(DevCommands::answerAt)))
            .then(Commands.literal("hangup")
                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                    .executes(DevCommands::hangupAt)));
        dispatcher.register(root);
    }

    private static int listPhones(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        PhoneSavedData data = PhoneSavedData.get(level);
        Map<String, GlobalPos> all = data.allNumbers();
        ctx.getSource().sendSuccess(() -> Component.literal("=== Зарегистрированные номера (" + all.size() + ") ===")
            .withStyle(ChatFormatting.GOLD), false);
        all.forEach((num, gp) -> {
            String status = String.valueOf(CallManager.get().statusOf(gp));
            String power = "?";
            ServerLevel l = level.getServer().getLevel(gp.dimension());
            if (l != null && l.isLoaded(gp.pos()) && l.getBlockEntity(gp.pos()) instanceof TelephoneBlockEntity p) {
                power = p.isPowered() ? String.format("%.1fV OK", p.getVoltage()) : String.format("%.1fV NO", p.getVoltage());
            }
            String line = String.format("  %s @ %s [%s] %s", num, gp.pos().toShortString(), power, status);
            ctx.getSource().sendSuccess(() -> Component.literal(line), false);
        });
        return all.size();
    }

    private static int listRadios(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(() -> Component.literal(
            "=== Станции (" + RadioRegistry.stations().size() + ") / Радио (" + RadioRegistry.radios().size() + ") ===")
            .withStyle(ChatFormatting.GOLD), false);
        for (RadioStationBlockEntity st : RadioRegistry.stations()) {
            String line = String.format("  [ST] %s '%s' ch=%02d range=%.0fm in=%d",
                st.getBlockPos().toShortString(),
                st.getLink().name(),
                st.getFrequency(),
                st.getEffectiveRange(),
                st.getInputSignal());
            ctx.getSource().sendSuccess(() -> Component.literal(line), false);
        }
        for (RadioBlockEntity r : RadioRegistry.radios()) {
            String line = String.format("  [%s] %s ch=%02d range=%dm in=%d out=%d st='%s'",
                r.isTransmitter() ? "TX" : "RX",
                r.getBlockPos().toShortString(),
                r.getFrequency(),
                r.getRange(),
                r.getInputSignal(),
                r.getOutputSignal(),
                r.getStation());
            ctx.getSource().sendSuccess(() -> Component.literal(line), false);
        }
        return 1;
    }

    private static int probePhone(CommandContext<CommandSourceStack> ctx) {
        try {
            BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
            ServerLevel level = ctx.getSource().getLevel();
            if (!(level.getBlockEntity(pos) instanceof TelephoneBlockEntity p)) {
                ctx.getSource().sendFailure(Component.literal("На " + pos.toShortString() + " нет телефона"));
                return 0;
            }
            ctx.getSource().sendSuccess(() -> Component.literal(String.format(
                "Телефон @ %s: num='%s' payphone=%s speaker=%s V=%.2f powered=%s status=%s",
                pos.toShortString(),
                p.getNumber(),
                p.isPayphone(),
                p.isSpeakerMode(),
                p.getVoltage(),
                p.isPowered(),
                CallManager.get().statusOf(p.globalPos())
            )), false);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal(e.getMessage()));
            return 0;
        }
    }

    private static int checkLine(CommandContext<CommandSourceStack> ctx) {
        try {
            BlockPos a = BlockPosArgument.getLoadedBlockPos(ctx, "a");
            BlockPos b = BlockPosArgument.getLoadedBlockPos(ctx, "b");
            PhoneLineTracer.invalidate();
            boolean ok = PhoneLineTracer.connected(ctx.getSource().getLevel(), a, b);
            ctx.getSource().sendSuccess(() -> Component.literal(
                "Линия " + a.toShortString() + " <-> " + b.toShortString() + ": " + (ok ? "СВЯЗАНЫ" : "НЕТ СВЯЗИ"))
                .withStyle(ok ? ChatFormatting.GREEN : ChatFormatting.RED), false);
            return ok ? 1 : 0;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal(e.getMessage()));
            return 0;
        }
    }

    private static int dialFrom(CommandContext<CommandSourceStack> ctx) {
        try {
            BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "from");
            String num = StringArgumentType.getString(ctx, "number");
            ServerLevel level = ctx.getSource().getLevel();
            if (!(level.getBlockEntity(pos) instanceof TelephoneBlockEntity p)) {
                ctx.getSource().sendFailure(Component.literal("Нет телефона"));
                return 0;
            }
            ServerPlayer sp = ctx.getSource().getPlayerOrException();
            Component msg = CallManager.get().dial(sp, p, num);
            ctx.getSource().sendSuccess(() -> msg, false);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal(e.getMessage()));
            return 0;
        }
    }

    private static int answerAt(CommandContext<CommandSourceStack> ctx) {
        try {
            BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
            ServerLevel level = ctx.getSource().getLevel();
            if (!(level.getBlockEntity(pos) instanceof TelephoneBlockEntity p)) {
                return 0;
            }
            CallManager.get().answer(ctx.getSource().getPlayerOrException(), p);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal(e.getMessage()));
            return 0;
        }
    }

    private static int hangupAt(CommandContext<CommandSourceStack> ctx) {
        try {
            BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
            ServerLevel level = ctx.getSource().getLevel();
            if (!(level.getBlockEntity(pos) instanceof TelephoneBlockEntity p)) {
                return 0;
            }
            CallManager.get().hangup(p);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal(e.getMessage()));
            return 0;
        }
    }

    private static int runTests(CommandContext<CommandSourceStack> ctx, String filter) {
        ServerLevel level = ctx.getSource().getLevel();
        BlockPos origin = BlockPos.containing(ctx.getSource().getPosition()).offset(4, 0, 4);
        int count = 0;
        int offsetX = 0;
        for (Method m : ConnectionGameTests.class.getDeclaredMethods()) {
            GameTest gt = m.getAnnotation(GameTest.class);
            if (gt == null) continue;
            if (filter != null && !m.getName().toLowerCase().contains(filter.toLowerCase())) continue;

            String name = m.getName();
            TestFunction tf = new TestFunction(
                gt.batch(),
                name,
                CreateConnection.MOD_ID + ":" + gt.template(),
                Rotation.NONE,
                gt.timeoutTicks(),
                gt.setupTicks(),
                gt.required(),
                gt.manualOnly(),
                gt.attempts(),
                gt.requiredSuccesses(),
                gt.skyAccess(),
                helper -> {
                    try {
                        m.invoke(null, helper);
                    } catch ( ReflectiveOperationException e) {
                        Throwable cause = e.getCause() != null ? e.getCause() : e;
                        if (cause instanceof RuntimeException re) throw re;
                        throw new RuntimeException(cause);
                    }
                }
            );

            BlockPos testPos = origin.offset(offsetX, 0, 0);
            offsetX += 20;
            GameTestInfo info = new GameTestInfo(tf, Rotation.NONE, level, RetryOptions.noRetries());
            Optional<StructureBlockEntity> opt = StructureUtils.prepareTestStructure(info, testPos, Rotation.NONE, level);
            if (opt.isEmpty()) {
                ctx.getSource().sendFailure(Component.literal("Не удалось подготовить структуру для " + name));
            } else {
                info.setStructureBlockPos(opt.get().getBlockPos());
                GameTestRunner runner = new GameTestRunner.Builder(java.util.List.of(info), level).build();
                info.startExecution(1);
                GameTestTicker.SINGLETON.add(info);
                count++;
                ctx.getSource().sendSuccess(() -> Component.literal("▶ Запуск GameTest: " + name + " @ " + testPos.toShortString())
                    .withStyle(ChatFormatting.AQUA), false);
            }
        }
        return count;
    }
}
