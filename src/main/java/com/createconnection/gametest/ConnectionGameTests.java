package com.createconnection.gametest;

import com.createconnection.CreateConnection;
import com.createconnection.audio.AudioRouter;
import com.createconnection.audio.SoundReactions;
import com.createconnection.compat.create.RemoteStockKeeper;
import com.createconnection.phone.AbstractPhoneBlock;
import com.createconnection.phone.CallManager;
import com.createconnection.phone.PhoneDirectory;
import com.createconnection.phone.PhoneLineTracer;
import com.createconnection.phone.TelephoneBlockEntity;
import com.createconnection.phone.service.ServicePhoneBlock;
import com.createconnection.radio.HandMicrophoneItem;
import com.createconnection.radio.MicrophoneStandBlock;
import com.createconnection.radio.MicrophoneStandBlockEntity;
import com.createconnection.radio.RadioAudioBlock;
import com.createconnection.radio.RadioBlock;
import com.createconnection.radio.RadioBlockEntity;
import com.createconnection.radio.RadioDataLinkHandler;
import com.createconnection.radio.RadioLink;
import com.createconnection.radio.RadioRegistry;
import com.createconnection.radio.RadioStationBlock;
import com.createconnection.radio.RadioStationBlockEntity;
import com.createconnection.radio.WalkieTalkieItem;
import com.createconnection.registry.TBlocks;
import com.createconnection.registry.TItems;
import com.happysg.radar.block.radar.bearing.RadarBearingBlockEntity;
import com.happysg.radar.block.radar.behavior.IRadar;
import com.happysg.radar.block.radar.track.RadarTrack;
import com.mojang.authlib.GameProfile;
import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SculkSensorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SculkSensorPhase;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.patryk3211.powergrid.electricity.CreativeSourceBlockEntity;
import org.patryk3211.powergrid.electricity.wire.BlockWireEndpoint;
import org.patryk3211.powergrid.electricity.wire.WireEntity;

@GameTestHolder(CreateConnection.MOD_ID)
@PrefixGameTestTemplate(false)
public class ConnectionGameTests {
    private static final String EMPTY = "empty_16";

    private static Block block(String ns, String path) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(ns, path));
    }

    private static void placeSource(GameTestHelper h, BlockPos rel, float voltage) {
        h.setBlock(rel, block("powergrid", "creative_voltage_source"));
        if (h.getBlockEntity(rel) instanceof CreativeSourceBlockEntity be) {
            be.setValue(voltage);
        }
    }

    private static void wire(GameTestHelper h, BlockPos a, int ta, BlockPos b, int tb) {
        ServerLevel level = h.getLevel();
        Item wireItem = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("powergrid", "copper_cord"));
        WireEntity.create(
            level,
            new BlockWireEndpoint(h.absolutePos(a), ta),
            new BlockWireEndpoint(h.absolutePos(b), tb),
            new ItemStack(wireItem, 1),
            null
        );
    }

    private static void wirePair(GameTestHelper h, BlockPos a, BlockPos b) {
        wire(h, a, 0, b, 0);
        wire(h, a, 1, b, 1);
    }

    private static ServerPlayer fakePlayer(GameTestHelper h, String name) {
        UUID id = UUID.nameUUIDFromBytes(("cc_test_" + name).getBytes(StandardCharsets.UTF_8));
        return FakePlayerFactory.get(h.getLevel(), new GameProfile(id, name));
    }

    private static TelephoneBlockEntity phone(GameTestHelper h, BlockPos rel) {
        return (TelephoneBlockEntity) h.getBlockEntity(rel);
    }

    private static void assignNumber(GameTestHelper h, TelephoneBlockEntity be, String num) {
        ServerLevel level = h.getLevel();
        PhoneDirectory dir = PhoneDirectory.get(level.getServer());
        dir.forceRelease(level, num);
        dir.claim(level, num, be.getBlockPos());
        be.setNumberRaw(num);
    }

    private static void releaseNumber(GameTestHelper h, TelephoneBlockEntity be) {
        PhoneDirectory.get(h.getLevel().getServer()).release(h.getLevel(), be.getNumber(), be.getBlockPos());
    }

    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void phoneWithoutWiresIsUnpowered(GameTestHelper h) {
        BlockPos p = new BlockPos(4, 1, 4);
        h.setBlock(p, TBlocks.HOME_PHONE.get());
        h.runAfterDelay(5L, () -> {
            h.assertFalse(phone(h, p).isPowered(), "Телефон без проводов не должен быть под напряжением");
            h.succeed();
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void phonesOnPoweredLineAreConnected(GameTestHelper h) {
        BlockPos src = new BlockPos(2, 1, 2);
        BlockPos a = new BlockPos(4, 1, 2);
        BlockPos b = new BlockPos(8, 1, 2);
        placeSource(h, src, 24.0f);
        h.setBlock(a, TBlocks.HOME_PHONE.get());
        h.setBlock(b, TBlocks.HOME_PHONE.get());
        wirePair(h, src, a);
        wirePair(h, a, b);
        h.runAfterDelay(15L, () -> {
            h.assertTrue(phone(h, a).isPowered(), "A voltage=" + phone(h, a).getVoltage());
            h.assertTrue(phone(h, b).isPowered(), "B voltage=" + phone(h, b).getVoltage());
            PhoneLineTracer.invalidate();
            h.assertTrue(
                PhoneLineTracer.connected(h.getLevel(), h.absolutePos(a), h.absolutePos(b)),
                "A и B должны быть на одной линии"
            );
            h.succeed();
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void fullCallCycle(GameTestHelper h) {
        BlockPos src = new BlockPos(2, 1, 4);
        BlockPos a = new BlockPos(4, 1, 4);
        BlockPos b = new BlockPos(8, 1, 4);
        placeSource(h, src, 24.0f);
        h.setBlock(a, TBlocks.HOME_PHONE.get());
        h.setBlock(b, TBlocks.HOME_PHONE.get());
        wirePair(h, src, a);
        wirePair(h, a, b);
        ServerPlayer caller = fakePlayer(h, "caller");
        CallManager cm = CallManager.get();

        h.startSequence()
            .thenWaitUntil(() -> h.assertTrue(phone(h, a).isPowered() && phone(h, b).isPowered(), "Ждём питания"))
            .thenExecute(() -> {
                phone(h, a).setSpeakerMode(true);
                phone(h, b).setSpeakerMode(true);
                assignNumber(h, phone(h, a), "70001");
                assignNumber(h, phone(h, b), "70002");
                Component err = cm.dial(caller, phone(h, a), "70002");
                h.assertTrue(cm.statusOf(phone(h, b).globalPos()) == CallManager.Status.RINGING_IN, "dial err=" + err.getString());
            })
            .thenExecute(() -> h.assertTrue(cm.statusOf(phone(h, a).globalPos()) == CallManager.Status.DIALING_OUT, "A должен ждать ответа"))
            .thenExecute(() -> {
                BlockState st = h.getBlockState(b);
                h.assertTrue(st.getAnalogOutputSignal(h.getLevel(), h.absolutePos(b)) == 15, "Компаратор у звонящего аппарата должен давать 15");
            })
            .thenIdle(5)
            .thenExecute(() -> cm.answer(caller, phone(h, b)))
            .thenIdle(5)
            .thenExecute(() -> h.assertTrue(cm.statusOf(phone(h, a).globalPos()) == CallManager.Status.ACTIVE, "Разговор должен держаться"))
            .thenExecute(() -> cm.hangup(phone(h, a)))
            .thenExecute(() -> {
                h.assertTrue(cm.statusOf(phone(h, b).globalPos()) == CallManager.Status.IDLE, "После отбоя B свободен");
                releaseNumber(h, phone(h, a));
                releaseNumber(h, phone(h, b));
            })
            .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 80)
    public static void payphoneHasNoNumber(GameTestHelper h) {
        BlockPos src = new BlockPos(2, 1, 6);
        BlockPos pay = new BlockPos(4, 1, 6);
        BlockPos home = new BlockPos(8, 1, 6);
        placeSource(h, src, 24.0f);
        h.setBlock(pay, TBlocks.PAYPHONE.get());
        h.setBlock(home, TBlocks.HOME_PHONE.get());
        wirePair(h, src, pay);
        wirePair(h, pay, home);
        ServerPlayer sp = fakePlayer(h, "payuser");
        CallManager cm = CallManager.get();

        h.startSequence()
            .thenWaitUntil(() -> h.assertTrue(phone(h, pay).isPowered() && phone(h, home).isPowered(), "Ждём питания"))
            .thenExecute(() -> {
                cm.setNumber(sp, phone(h, pay), "70011");
                h.assertTrue(phone(h, pay).getNumber().isEmpty(), "Уличному телефону номер не назначается");
                phone(h, pay).setSpeakerMode(true);
                phone(h, home).setSpeakerMode(true);
                assignNumber(h, phone(h, home), "70012");
                sp.getInventory().add(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "copper_nugget")), 4));
                Component res = cm.dial(sp, phone(h, pay), "70012");
                CallManager.Call call = cm.callAt(phone(h, home).globalPos());
                h.assertTrue(call != null, "Dial result=" + res.getString());
                h.assertTrue(call.callerNumber.isEmpty(), "У принявшего номер звонящего неизвестен");
                cm.hangup(phone(h, pay));
            })
            .thenExecute(() -> releaseNumber(h, phone(h, home)))
            .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 80)
    public static void separateLinesCannotCall(GameTestHelper h) {
        BlockPos src1 = new BlockPos(2, 1, 2);
        BlockPos a = new BlockPos(4, 1, 2);
        BlockPos src2 = new BlockPos(2, 1, 10);
        BlockPos b = new BlockPos(4, 1, 10);
        placeSource(h, src1, 24.0f);
        h.setBlock(a, TBlocks.HOME_PHONE.get());
        wirePair(h, src1, a);
        placeSource(h, src2, 24.0f);
        h.setBlock(b, TBlocks.HOME_PHONE.get());
        wirePair(h, src2, b);
        ServerPlayer sp = fakePlayer(h, "isolated");
        CallManager cm = CallManager.get();

        h.startSequence()
            .thenWaitUntil(() -> h.assertTrue(phone(h, a).isPowered() && phone(h, b).isPowered(), "Ждём питания"))
            .thenExecute(() -> {
                assignNumber(h, phone(h, a), "70011");
                assignNumber(h, phone(h, b), "70012");
                PhoneLineTracer.invalidate();
                h.assertFalse(PhoneLineTracer.connected(h.getLevel(), h.absolutePos(a), h.absolutePos(b)), "Разные линии не должны быть связаны");
                Component msg = cm.dial(sp, phone(h, a), "70012");
                h.assertTrue(cm.statusOf(phone(h, b).globalPos()) == CallManager.Status.IDLE, "Вызов на другую линию не должен пройти");
                h.assertTrue(
                    msg.getContents() instanceof TranslatableContents tc && tc.getKey().equals("create_connection.msg.no_line"),
                    "msg=" + msg.getString()
                );
                releaseNumber(h, phone(h, a));
                releaseNumber(h, phone(h, b));
            })
            .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void directoryRejectsDuplicateNumbers(GameTestHelper h) {
        BlockPos a = new BlockPos(3, 1, 3);
        BlockPos b = new BlockPos(6, 1, 3);
        h.setBlock(a, TBlocks.HOME_PHONE.get());
        h.setBlock(b, TBlocks.HOME_PHONE.get());
        ServerPlayer sp = fakePlayer(h, "dup");
        CallManager cm = CallManager.get();
        PhoneDirectory.get(h.getLevel().getServer()).forceRelease(h.getLevel(), "70021");

        cm.setNumber(sp, phone(h, a), "70021");
        h.assertTrue("70021".equals(phone(h, a).getNumber()), "A должен получить 70021");
        cm.setNumber(sp, phone(h, b), "70021");
        h.assertFalse("70021".equals(phone(h, b).getNumber()), "B не должен отобрать занятый номер 70021");
        releaseNumber(h, phone(h, a));
        h.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 80)
    public static void sneakHangsUp(GameTestHelper h) {
        BlockPos src = new BlockPos(2, 1, 4);
        BlockPos a = new BlockPos(4, 1, 4);
        BlockPos b = new BlockPos(8, 1, 4);
        placeSource(h, src, 24.0f);
        h.setBlock(a, TBlocks.HOME_PHONE.get());
        h.setBlock(b, TBlocks.HOME_PHONE.get());
        wirePair(h, src, a);
        wirePair(h, a, b);
        ServerPlayer p1 = fakePlayer(h, "sneak1");
        ServerPlayer p2 = fakePlayer(h, "sneak2");
        CallManager cm = CallManager.get();

        h.startSequence()
            .thenWaitUntil(() -> h.assertTrue(phone(h, a).isPowered() && phone(h, b).isPowered(), "Ждём питания"))
            .thenExecute(() -> {
                phone(h, a).setSpeakerMode(true);
                phone(h, b).setSpeakerMode(true);
                assignNumber(h, phone(h, a), "70031");
                assignNumber(h, phone(h, b), "70032");
                cm.dial(p1, phone(h, a), "70032");
                cm.answer(p2, phone(h, b));
            })
            .thenIdle(2)
            .thenExecute(() -> h.assertTrue(cm.statusOf(phone(h, a).globalPos()) == CallManager.Status.ACTIVE, "Разговор идёт"))
            .thenExecute(() -> p1.setShiftKeyDown(true))
            .thenIdle(3)
            .thenExecute(() -> {
                h.assertTrue(cm.statusOf(phone(h, a).globalPos()) == CallManager.Status.IDLE, "LShift должен положить трубку");
                p1.setShiftKeyDown(false);
                releaseNumber(h, phone(h, a));
                releaseNumber(h, phone(h, b));
            })
            .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void walkieTogglesOnRightClick(GameTestHelper h) {
        ServerPlayer sp = fakePlayer(h, "walkie");
        ItemStack stack = new ItemStack(TItems.WALKIE_TALKIE.get());
        sp.setItemInHand(InteractionHand.MAIN_HAND, stack);
        h.assertFalse(WalkieTalkieItem.isActive(stack), "Изначально рация выключена");
        stack.use(h.getLevel(), sp, InteractionHand.MAIN_HAND);
        h.assertTrue(WalkieTalkieItem.isActive(stack), "ПКМ включает рацию");
        stack.use(h.getLevel(), sp, InteractionHand.MAIN_HAND);
        h.assertFalse(WalkieTalkieItem.isActive(stack), "Повторный ПКМ выключает рацию");
        h.succeed();
    }

    private static RadioBlockEntity radio(GameTestHelper h, BlockPos rel, int freq, boolean tx) {
        h.setBlock(rel, TBlocks.RADIO.get());
        RadioBlockEntity be = (RadioBlockEntity) h.getBlockEntity(rel);
        be.configure(freq, tx);
        return be;
    }

    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void unlinkedRadioRedstoneWorks(GameTestHelper h) {
        BlockPos tx = new BlockPos(3, 1, 4);
        BlockPos rx = new BlockPos(9, 1, 4);
        radio(h, tx, 7, true);
        radio(h, rx, 7, false);

        h.startSequence()
            .thenIdle(2)
            .thenExecute(() -> h.setBlock(tx.west(), Blocks.REDSTONE_BLOCK))
            .thenIdle(3)
            .thenExecute(() -> {
                RadioBlockEntity rxBe = (RadioBlockEntity) h.getBlockEntity(rx);
                h.assertTrue(rxBe.getOutputSignal() == 15, "out=" + rxBe.getOutputSignal());
                h.assertBlockProperty(rx, RadioBlock.ACTIVE, true);
                h.assertBlockProperty(tx, RadioBlock.ACTIVE, true);
            })
            .thenExecute(() -> h.setBlock(tx.west(), Blocks.AIR))
            .thenIdle(3)
            .thenExecute(() -> {
                RadioBlockEntity rxBe = (RadioBlockEntity) h.getBlockEntity(rx);
                h.assertTrue(rxBe.getOutputSignal() == 0, "После снятия сигнала приёмник должен погаснуть");
                h.assertBlockProperty(tx, RadioBlock.ACTIVE, false);
            })
            .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void radioFrequenciesAreIsolated(GameTestHelper h) {
        BlockPos tx = new BlockPos(3, 1, 6);
        BlockPos rx = new BlockPos(8, 1, 6);
        radio(h, tx, 3, true);
        radio(h, rx, 4, false);
        h.setBlock(tx.west(), Blocks.REDSTONE_BLOCK);

        h.startSequence()
            .thenIdle(4)
            .thenExecute(() -> {
                RadioBlockEntity rxBe = (RadioBlockEntity) h.getBlockEntity(rx);
                h.assertTrue(rxBe.getFrequency() == 4, "Частота приёмника должна быть 04");
                h.assertTrue(rxBe.getOutputSignal() == 0, "Частота 04 не должна слышать 03");
            })
            .thenExecute(() -> ((RadioBlockEntity) h.getBlockEntity(rx)).configure(3, false))
            .thenIdle(3)
            .thenExecute(() -> h.assertTrue(((RadioBlockEntity) h.getBlockEntity(rx)).getOutputSignal() == 15, "После настройки на 03 сигнал должен прийти"))
            .thenExecute(() -> ((RadioBlockEntity) h.getBlockEntity(rx)).configure(3, true))
            .thenIdle(3)
            .thenExecute(() -> {
                h.assertBlockProperty(rx, RadioBlock.TRANSMIT, true);
                h.assertTrue(((RadioBlockEntity) h.getBlockEntity(rx)).getOutputSignal() == 0, "В режиме передачи радио не выдаёт сигнал");
            })
            .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 30)
    public static void radioHas30BlockRange(GameTestHelper h) {
        BlockPos p = new BlockPos(4, 1, 4);
        RadioBlockEntity r = radio(h, p, 2, false);
        h.runAfterDelay(3L, () -> {
            h.assertTrue(r.getRange() == 30, "range=" + r.getRange());
            h.succeed();
        });
    }

    private static PlayerInteractEvent.RightClickBlock rightClick(GameTestHelper h, ServerPlayer sp, BlockPos absPos, Direction face) {
        BlockHitResult hit = new BlockHitResult(absPos.getCenter(), face, absPos, false);
        return new PlayerInteractEvent.RightClickBlock(sp, InteractionHand.MAIN_HAND, absPos, hit);
    }

    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void stationConnectsAndRadioHearsIt(GameTestHelper h) {
        BlockPos ctrl = new BlockPos(4, 1, 8);
        BlockPos stPos = new BlockPos(6, 1, 8);
        BlockPos rSame = new BlockPos(10, 1, 8);
        BlockPos rOther = new BlockPos(10, 1, 10);

        h.setBlock(ctrl, block("create_radar", "network_filterer"));
        h.setBlock(stPos, TBlocks.RADIO_STATION.get());
        RadioStationBlockEntity st = (RadioStationBlockEntity) h.getBlockEntity(stPos);
        st.setFrequency(5);
        h.setBlock(stPos.west(), Blocks.REDSTONE_BLOCK);
        radio(h, rSame, 5, false);
        radio(h, rOther, 6, false);

        ServerPlayer sp = fakePlayer(h, "station_linker");
        ItemStack dlItem = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create_radar", "data_link")));
        sp.setItemInHand(InteractionHand.MAIN_HAND, dlItem);

        BlockPos absSt = h.absolutePos(stPos);
        BlockPos absCtrl = h.absolutePos(ctrl);
        RadioDataLinkHandler.onRightClickBlock(rightClick(h, sp, absSt, Direction.UP));
        RadioDataLinkHandler.onRightClickBlock(rightClick(h, sp, absCtrl, Direction.UP));

        BlockState dlState = block("create_radar", "data_link").defaultBlockState()
            .setValue(BlockStateProperties.FACING, Direction.UP);
        h.setBlock(stPos.above(), dlState);

        h.startSequence()
            .thenIdle(45)
            .thenExecute(() -> {
                h.assertTrue(st.getEffectiveRange() == 256.0, "range=" + st.getEffectiveRange());
                h.assertBlockProperty(stPos, RadioStationBlock.ONLINE, true);
                RadioBlockEntity rx = (RadioBlockEntity) h.getBlockEntity(rSame);
                h.assertFalse(rx.getStation().isEmpty(), "Радио на частоте 05 должно поймать станцию");
                h.assertTrue(rx.getOutputSignal() == 15, "rx out=" + rx.getOutputSignal());
                RadioBlockEntity rx6 = (RadioBlockEntity) h.getBlockEntity(rOther);
                h.assertTrue(rx6.getOutputSignal() == 0 && rx6.getStation().isEmpty(), "Частота 06 станцию не слышит");

                ServerLevel lvl = h.getLevel();
                Vec3 origin = h.absoluteVec(new Vec3(6.5, 1.5, 8.5));
                RadioLink link = st.getLink();
                h.assertTrue(
                    RadioRegistry.canReach(link, lvl, origin, link, lvl, origin.add(200.0, 0.0, 0.0)),
                    "Рации сети в зоне станции слышат друг друга"
                );
                h.assertFalse(
                    RadioRegistry.canReach(link, lvl, origin, null, lvl, origin.add(5.0, 0.0, 0.0)),
                    "Рация без привязки закрытую сеть не слышит"
                );
            })
            .thenExecute(() -> h.setBlock(stPos.above(), Blocks.AIR))
            .thenIdle(45)
            .thenExecute(() -> {
                h.assertTrue(st.getEffectiveRange() == 0.0, "Без передатчика данных станция не работает");
                h.assertTrue(((RadioBlockEntity) h.getBlockEntity(rSame)).getOutputSignal() == 0, "Радио теряет станцию");
            })
            .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void phoneBellAndDye(GameTestHelper h) {
        BlockPos p = new BlockPos(4, 1, 4);
        h.setBlock(p, TBlocks.HOME_PHONE.get());
        ServerPlayer sp = fakePlayer(h, "dyer");
        BlockPos abs = h.absolutePos(p);
        BlockHitResult hit = new BlockHitResult(abs.getCenter(), Direction.UP, abs, false);

        ItemStack bell = new ItemStack(Items.BELL);
        sp.setItemInHand(InteractionHand.MAIN_HAND, bell);
        h.getLevel().getBlockState(abs).useItemOn(bell, h.getLevel(), sp, InteractionHand.MAIN_HAND, hit);
        h.assertBlockProperty(p, AbstractPhoneBlock.BELL, true);

        ItemStack red = new ItemStack(Items.RED_DYE);
        sp.setItemInHand(InteractionHand.MAIN_HAND, red);
        h.getLevel().getBlockState(abs).useItemOn(red, h.getLevel(), sp, InteractionHand.MAIN_HAND, hit);
        h.assertTrue(h.getBlockState(p).getValue(AbstractPhoneBlock.COLOR) > 0, "Краситель должен покрасить телефон");

        ItemStack sponge = new ItemStack(Items.WET_SPONGE);
        sp.setItemInHand(InteractionHand.MAIN_HAND, sponge);
        h.getLevel().getBlockState(abs).useItemOn(sponge, h.getLevel(), sp, InteractionHand.MAIN_HAND, hit);
        h.assertBlockProperty(p, AbstractPhoneBlock.COLOR, 0);

        sp.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        sp.setShiftKeyDown(true);
        h.getLevel().getBlockState(abs).useWithoutItem(h.getLevel(), sp, hit);
        sp.setShiftKeyDown(false);
        h.assertBlockProperty(p, AbstractPhoneBlock.BELL, false);
        h.assertTrue(sp.getInventory().countItem(Items.BELL) == 1, "Колокол должен вернуться в инвентарь");
        h.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void servicePhoneBindsVillagerByJobSite(GameTestHelper h) {
        BlockPos srv = new BlockPos(5, 1, 5);
        BlockPos lectern = new BlockPos(6, 1, 5);
        h.setBlock(srv, TBlocks.SERVICE_PHONE.get());
        h.setBlock(lectern, Blocks.LECTERN);

        ServerLevel level = h.getLevel();
        Villager bound = EntityType.VILLAGER.create(level);
        bound.moveTo(h.absolutePos(lectern).getX() + 0.5, h.absolutePos(lectern).getY() + 1, h.absolutePos(lectern).getZ() + 0.5);
        bound.setVillagerData(bound.getVillagerData().setProfession(VillagerProfession.LIBRARIAN).setLevel(2));
        bound.getBrain().setMemory(MemoryModuleType.JOB_SITE, GlobalPos.of(level.dimension(), h.absolutePos(lectern)));
        level.addFreshEntity(bound);

        Villager unbound = EntityType.VILLAGER.create(level);
        unbound.moveTo(h.absolutePos(srv).getX() + 0.5, h.absolutePos(srv).getY() + 1, h.absolutePos(srv).getZ() + 0.5);
        unbound.setVillagerData(unbound.getVillagerData().setProfession(VillagerProfession.FARMER).setLevel(2));
        unbound.getBrain().setMemory(MemoryModuleType.JOB_SITE, GlobalPos.of(level.dimension(), h.absolutePos(new BlockPos(12, 1, 12))));
        level.addFreshEntity(unbound);

        Villager found = ServicePhoneBlock.findBoundVillager(level, h.absolutePos(srv));
        h.assertTrue(found == bound, "Должен находиться только житель, чей рабочий блок стоит вплотную к телефону");
        bound.discard();
        unbound.discard();
        h.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void servicePhoneFindsStockTickerViaBlazeBurner(GameTestHelper h) {
        BlockPos srv = new BlockPos(5, 1, 5);
        BlockPos burner = new BlockPos(6, 1, 5);
        BlockPos ticker = new BlockPos(6, 1, 6);
        h.setBlock(srv, TBlocks.SERVICE_PHONE.get());
        h.setBlock(burner, block("create", "blaze_burner"));
        h.setBlock(ticker, block("create", "stock_ticker"));

        StockTickerBlockEntity found = RemoteStockKeeper.findTicker(h.getLevel(), h.absolutePos(srv));
        h.assertTrue(found != null && found.getBlockPos().equals(h.absolutePos(ticker)), "Торговый телефон должен находить Stock Ticker через Горелку Блейза");
        h.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void unlinkedRangeIs30Blocks(GameTestHelper h) {
        ServerLevel lvl = h.getLevel();
        Vec3 a = h.absoluteVec(new Vec3(1.0, 1.0, 1.0));
        h.assertTrue(RadioRegistry.canReach(null, lvl, a, null, lvl, a.add(29.0, 0.0, 0.0)), "На 29 блоках без станции связь есть");
        h.assertFalse(RadioRegistry.canReach(null, lvl, a, null, lvl, a.add(31.0, 0.0, 0.0)), "На 31 блоке без станции связи нет");
        h.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 80)
    public static void radarMarksRadios(GameTestHelper h) {
        BlockPos radarPos = new BlockPos(4, 1, 4);
        BlockPos rxPos = new BlockPos(8, 1, 4);
        BlockPos txPos = new BlockPos(8, 1, 8);

        h.setBlock(radarPos, block("create_radar", "radar_bearing"));
        radio(h, rxPos, 1, false);
        radio(h, txPos, 1, true);

        RadarBearingBlockEntity radar = (RadarBearingBlockEntity) h.getBlockEntity(radarPos);
        radar.setRunning(true);

        h.startSequence()
            .thenIdle(30)
            .thenExecute(() -> {
                Collection<RadarTrack> tracks = ((IRadar) h.getBlockEntity(radarPos)).getTracks();
                boolean hasRx = tracks.stream().anyMatch(t ->
                    "create_connection:radio_receiver".equals(t.entityType())
                    && (t.getColor().getRGB() & 0xFFFFFF) == 16747546
                );
                boolean hasTx = tracks.stream().anyMatch(t ->
                    "create_connection:radio_transmitter".equals(t.entityType())
                );
                h.assertTrue(
                    hasRx,
                    "На радаре должна быть оранжевая отметка приёмника, tracks="
                        + tracks.stream().map(t -> t.entityType() + ":" + Integer.toHexString(t.getColor().getRGB())).toList()
                );
                h.assertTrue(hasTx, "На радаре должна быть отметка передатчика");
            })
            .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void allRecipesLoad(GameTestHelper h) {
        String[] names = {
            "speaker", "microphone", "home_phone", "payphone", "service_phone",
            "radio_station", "radio", "walkie_talkie", "radio_headset", "megaphone",
            "radio_speaker", "radio_microphone", "microphone_stand", "hand_microphone"
        };
        var rm = h.getLevel().getRecipeManager();
        for (String name : names) {
            Optional<?> r = rm.byKey(CreateConnection.id(name));
            h.assertTrue(r.isPresent(), "Рецепт create_connection:" + name + " должен быть загружен");
        }
        h.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void villagerUnreachableWhenCannotAnswer(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        Villager v = EntityType.VILLAGER.create(level);
        v.moveTo(h.absolutePos(new BlockPos(5, 2, 5)).getX() + 0.5, h.absolutePos(new BlockPos(5, 2, 5)).getY(), h.absolutePos(new BlockPos(5, 2, 5)).getZ() + 0.5);
        v.setVillagerData(v.getVillagerData().setProfession(VillagerProfession.NONE));
        level.addFreshEntity(v);

        h.assertFalse(ServicePhoneBlock.canAnswer(v), "Безработный житель не может ответить");
        v.setVillagerData(v.getVillagerData().setProfession(VillagerProfession.LIBRARIAN).setLevel(2));
        v.getOffers().add(new MerchantOffer(new ItemCost(Items.EMERALD, 1), new ItemStack(Items.BOOK), 10, 2, 0.05f));
        h.assertTrue(ServicePhoneBlock.canAnswer(v), "Торговец с предложениями должен отвечать");

        ServerPlayer dummy = fakePlayer(h, "busy_customer");
        v.setTradingPlayer(dummy);
        h.assertFalse(ServicePhoneBlock.canAnswer(v), "Занятый торговлей житель не может ответить");
        v.setTradingPlayer(null);
        v.discard();
        h.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 80)
    public static void radioSoundsTriggerSculkAndVillagers(GameTestHelper h) {
        BlockPos tx = new BlockPos(2, 1, 2);
        BlockPos rx = new BlockPos(10, 1, 10);
        BlockPos sensor = new BlockPos(11, 1, 10);
        radio(h, tx, 9, true);
        h.setBlock(tx.west(), Blocks.REDSTONE_BLOCK);
        radio(h, rx, 9, false);
        h.setBlock(sensor, Blocks.SCULK_SENSOR);

        ServerLevel level = h.getLevel();
        Villager v = EntityType.VILLAGER.create(level);
        v.moveTo(h.absolutePos(rx).getX() + 0.5, h.absolutePos(rx).getY() + 1, h.absolutePos(rx).getZ() + 1.5);
        level.addFreshEntity(v);

        h.startSequence()
            .thenIdle(5)
            .thenExecute(() -> level.gameEvent(GameEvent.BLOCK_PLACE, h.absoluteVec(new Vec3(1.5, 1.5, 2.5)), GameEvent.Context.of(null, null)))
            .thenIdle(5)
            .thenExecute(() -> h.assertTrue(
                SculkSensorBlock.getPhase(h.getBlockState(sensor)) != SculkSensorPhase.INACTIVE,
                "Скалк-сенсор у принимающего радио должен уловить вибрацию из эфира"
            ))
            .thenExecute(() -> SoundReactions.onRelayedSound(
                level,
                h.absoluteVec(rx.getCenter()),
                BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.BELL_BLOCK)
            ))
            .thenIdle(2)
            .thenExecute(() -> h.assertTrue(
                v.getBrain().hasMemoryValue(MemoryModuleType.HEARD_BELL_TIME),
                "Житель должен услышать колокол из динамика"
            ))
            .thenExecute(v::discard)
            .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void megaphoneOnSpeaker(GameTestHelper h) {
        BlockPos spk = new BlockPos(4, 1, 4);
        h.setBlock(spk, TBlocks.RADIO_SPEAKER.get());
        ServerPlayer sp = fakePlayer(h, "mega_installer");
        BlockPos abs = h.absolutePos(spk);
        BlockHitResult hit = new BlockHitResult(abs.getCenter(), Direction.UP, abs, false);

        ItemStack mega = new ItemStack(TItems.MEGAPHONE.get());
        sp.setItemInHand(InteractionHand.MAIN_HAND, mega);
        h.getLevel().getBlockState(abs).useItemOn(mega, h.getLevel(), sp, InteractionHand.MAIN_HAND, hit);
        h.assertBlockProperty(spk, RadioAudioBlock.MEGAPHONE, true);
        h.assertTrue(mega.isEmpty(), "Мегафон должен установиться на динамик");

        sp.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        sp.setShiftKeyDown(true);
        h.getLevel().getBlockState(abs).useWithoutItem(h.getLevel(), sp, hit);
        sp.setShiftKeyDown(false);
        h.assertBlockProperty(spk, RadioAudioBlock.MEGAPHONE, false);
        h.assertTrue(sp.getInventory().countItem(TItems.MEGAPHONE.get()) == 1, "Мегафон вернулся игроку");
        h.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void microphoneStandActsAsRadioMicrophone(GameTestHelper h) {
        BlockPos spk = new BlockPos(6, 1, 4);
        BlockPos stand = new BlockPos(2, 1, 4);
        h.setBlock(spk, TBlocks.RADIO_SPEAKER.get());
        h.setBlock(spk.east(), Blocks.REDSTONE_BLOCK);
        h.setBlock(stand, TBlocks.MICROPHONE_STAND.get());

        ServerPlayer sp = fakePlayer(h, "stand_user");
        ItemStack mic = new ItemStack(TItems.HAND_MICROPHONE.get());
        sp.setItemInHand(InteractionHand.MAIN_HAND, mic);

        BlockPos absSpk = h.absolutePos(spk);
        h.getLevel().getBlockState(absSpk).useItemOn(
            mic,
            h.getLevel(),
            sp,
            InteractionHand.MAIN_HAND,
            new BlockHitResult(absSpk.getCenter(), Direction.UP, absSpk, false)
        );

        BlockPos absStand = h.absolutePos(stand);
        BlockHitResult standHit = new BlockHitResult(absStand.getCenter(), Direction.UP, absStand, false);
        h.getLevel().getBlockState(absStand).useItemOn(mic, h.getLevel(), sp, InteractionHand.MAIN_HAND, standHit);

        h.assertBlockProperty(stand, MicrophoneStandBlock.HAS_MIC, true);
        h.assertTrue(mic.isEmpty(), "Микрофон перешёл на стойку");

        MicrophoneStandBlockEntity standBe = (MicrophoneStandBlockEntity) h.getBlockEntity(stand);
        h.runAfterDelay(3L, () -> {
            AudioRouter.Source src = AudioRouter.standSource(standBe);
            h.assertTrue(
                src != null && src.sinks().stream().anyMatch(s -> s.key().equals("speaker:" + absSpk)),
                "Стойка с микрофоном без редстоуна передаёт на подключённый динамик"
            );
            sp.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            h.getLevel().getBlockState(absStand).useWithoutItem(h.getLevel(), sp, standHit);
            h.assertBlockProperty(stand, MicrophoneStandBlock.HAS_MIC, false);
            h.assertTrue(sp.getInventory().countItem(TItems.HAND_MICROPHONE.get()) == 1, "Микрофон вернулся игроку");
            h.assertTrue(AudioRouter.standSource(standBe) == null, "Пустая стойка не передаёт");
            h.succeed();
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void handMicLinksToSpeaker(GameTestHelper h) {
        BlockPos spk = new BlockPos(4, 1, 4);
        h.setBlock(spk, TBlocks.RADIO_SPEAKER.get());
        BlockPos absSpk = h.absolutePos(spk);
        BlockHitResult hit = new BlockHitResult(absSpk.getCenter(), Direction.UP, absSpk, false);

        ServerPlayer sp = fakePlayer(h, "hand_mic_user");
        sp.setPos(h.absoluteVec(new Vec3(2.5, 1.0, 2.5)));
        ItemStack mic = new ItemStack(TItems.HAND_MICROPHONE.get());
        sp.setItemInHand(InteractionHand.MAIN_HAND, mic);

        h.getLevel().getBlockState(absSpk).useItemOn(mic, h.getLevel(), sp, InteractionHand.MAIN_HAND, hit);
        List<GlobalPos> linked = HandMicrophoneItem.speakers(mic);
        h.assertTrue(linked.size() == 1 && linked.get(0).pos().equals(absSpk), "ПКМ по динамику подключает его к микрофону");

        sp.startUsingItem(InteractionHand.MAIN_HAND);
        AudioRouter.Source silent = AudioRouter.handMicSource(sp);
        h.assertTrue(silent != null && silent.sinks().isEmpty(), "Без редстоуна динамик молчит");
        sp.stopUsingItem();

        h.setBlock(spk.east(), Blocks.REDSTONE_BLOCK);
        h.runAfterDelay(3L, () -> {
            h.assertTrue(AudioRouter.handMicSource(sp) == null, "Без зажатой ПКМ микрофон не передаёт");
            sp.startUsingItem(InteractionHand.MAIN_HAND);
            AudioRouter.Source active = AudioRouter.handMicSource(sp);
            sp.stopUsingItem();
            h.assertTrue(
                active != null && active.sinks().stream().anyMatch(s -> s.key().equals("speaker:" + absSpk)),
                "С зажатой ПКМ звук идёт на подключённый динамик"
            );
            h.getLevel().getBlockState(absSpk).useItemOn(mic, h.getLevel(), sp, InteractionHand.MAIN_HAND, hit);
            h.assertTrue(HandMicrophoneItem.speakers(mic).isEmpty(), "Повторный ПКМ отключает динамик");
            h.succeed();
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void stationWithoutControllerIsOffline(GameTestHelper h) {
        BlockPos stPos = new BlockPos(4, 1, 4);
        h.setBlock(stPos, TBlocks.RADIO_STATION.get());
        h.runAfterDelay(45L, () -> {
            RadioStationBlockEntity st = (RadioStationBlockEntity) h.getBlockEntity(stPos);
            h.assertTrue(st.getEffectiveRange() == 0.0, "Без контроллера радиус должен быть 0");
            h.assertBlockProperty(stPos, RadioStationBlock.ONLINE, false);
            h.succeed();
        });
    }
}
