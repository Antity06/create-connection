package com.createconnection.client.ponder;

import com.createconnection.phone.AbstractPhoneBlock;
import com.createconnection.radio.MicrophoneStandBlock;
import com.createconnection.radio.RadioAudioBlock;
import com.createconnection.radio.RadioBlock;
import com.createconnection.radio.RadioStationBlock;
import com.createconnection.registry.TBlocks;
import com.createconnection.registry.TItems;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class ConnectionScenes {
    private ConnectionScenes() {}

    private static ItemStack stack(String ns, String path) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(ns, path)));
    }

    public static void phoneLine(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("phone_line", "Проводная телефонная связь");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();
        scene.idle(10);

        BlockPos phoneA = util.grid().at(1, 1, 3);
        BlockPos phoneB = util.grid().at(5, 1, 3);
        BlockPos gen = util.grid().at(3, 1, 5);

        scene.world().showSection(util.select().position(phoneA).add(util.select().position(phoneB)), Direction.DOWN);
        scene.idle(15);

        scene.overlay().showText(70)
            .text("Домашний телефон — проводной аппарат с собственным 5-значным номером")
            .pointAt(util.vector().blockSurface(phoneA, Direction.UP))
            .placeNearTarget();
        scene.idle(80);

        scene.world().showSection(util.select().position(gen), Direction.DOWN);
        scene.idle(10);

        Vec3 termA1 = util.vector().of(1.25, 1.25, 3.85);
        Vec3 termA2 = util.vector().of(1.75, 1.25, 3.85);
        Vec3 termB1 = util.vector().of(5.25, 1.25, 3.85);
        Vec3 termB2 = util.vector().of(5.75, 1.25, 3.85);
        Vec3 genPos = util.vector().centerOf(gen);

        scene.overlay().showControls(termA1, Pointing.DOWN, 40)
            .rightClick()
            .withItem(stack("powergrid", "copper_cord"));
        scene.overlay().showLine(PonderPalette.OUTPUT, genPos, termA1, 160);
        scene.overlay().showLine(PonderPalette.INPUT, genPos, termA2, 160);
        scene.overlay().showLine(PonderPalette.OUTPUT, termA1, termB1, 160);
        scene.overlay().showLine(PonderPalette.INPUT, termA2, termB2, 160);
        scene.idle(15);

        scene.overlay().showText(80)
            .text("Подключите две клеммы сзади к линии Power Grid напряжением не менее 12 В. Оба телефона должны быть на одной линии!")
            .pointAt(termA1)
            .placeNearTarget();
        scene.idle(90);

        scene.overlay().showControls(util.vector().topOf(phoneA), Pointing.DOWN, 50).rightClick();
        scene.overlay().showText(75)
            .text("ПКМ открывает дисковый номеронабиратель. Кликните по строке своего номера сверху (✎), чтобы задать 5 цифр.")
            .pointAt(util.vector().topOf(phoneA))
            .placeNearTarget();
        scene.idle(85);

        scene.world().modifyBlock(phoneB, s -> s.setValue(AbstractPhoneBlock.RINGING, true), false);
        scene.overlay().showText(75)
            .text("При вызове второй аппарат звенит и выдаёт сигнал компаратора 15. Нажмите ПКМ, чтобы снять трубку, или LShift — чтобы сбросить вызов.")
            .pointAt(util.vector().topOf(phoneB))
            .placeNearTarget();
        scene.idle(85);

        scene.world().modifyBlock(phoneB, s -> s.setValue(AbstractPhoneBlock.RINGING, false), false);
        scene.markAsFinished();
    }

    public static void phoneBell(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("phone_bell", "Звонок, покраска и перенапряжение");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(10);

        BlockPos phone = util.grid().at(2, 1, 2);
        scene.world().showSection(util.select().position(phone), Direction.DOWN);
        scene.idle(15);

        scene.overlay().showControls(util.vector().topOf(phone), Pointing.DOWN, 45)
            .rightClick()
            .withItem(new ItemStack(Items.BELL));
        scene.idle(10);
        scene.world().modifyBlock(phone, s -> s.setValue(AbstractPhoneBlock.BELL, true).setValue(AbstractPhoneBlock.RINGING, true), false);

        scene.overlay().showText(75)
            .text("ПКМ колоколом крепит его сбоку: при входящем звонке молоточек качается, а звон слышен издалека! Shift+ПКМ пустой рукой снимает колокол.")
            .pointAt(util.vector().blockSurface(phone, Direction.EAST))
            .placeNearTarget();
        scene.idle(85);

        scene.world().modifyBlock(phone, s -> s.setValue(AbstractPhoneBlock.RINGING, false), false);
        scene.overlay().showControls(util.vector().topOf(phone), Pointing.DOWN, 45)
            .rightClick()
            .withItem(new ItemStack(Items.RED_DYE));
        scene.idle(10);
        scene.world().modifyBlock(phone, s -> s.setValue(AbstractPhoneBlock.COLOR, 15), false);

        scene.overlay().showText(65)
            .text("Любой из 16 красителей перекрашивает корпус аппарата (мокрая губка смывает краску)")
            .pointAt(util.vector().topOf(phone))
            .placeNearTarget();
        scene.idle(75);

        scene.effects().emitParticles(
            util.vector().centerOf(phone),
            scene.effects().simpleParticleEmitter(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, new Vec3(0.0, 0.08, 0.0)),
            4.0f,
            30
        );
        scene.overlay().showText(70)
            .text("Осторожно: при напряжении свыше 240 В обмотка перегорает и телефон выпадает предметом!")
            .colored(PonderPalette.RED)
            .pointAt(util.vector().topOf(phone))
            .placeNearTarget();
        scene.idle(80);
        scene.markAsFinished();
    }

    public static void payphone(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("payphone", "Уличный таксофон");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(10);

        BlockPos pay = util.grid().at(2, 1, 2);
        scene.world().showSection(util.select().position(pay), Direction.DOWN);
        scene.idle(15);

        scene.overlay().showText(75)
            .text("Уличный телефон крепится на стену или столб. Клеммы питания находятся сверху крышки.")
            .pointAt(util.vector().topOf(pay))
            .placeNearTarget();
        scene.idle(85);

        scene.overlay().showControls(util.vector().centerOf(pay), Pointing.RIGHT, 50)
            .withItem(stack("create", "copper_nugget"));
        scene.overlay().showText(80)
            .text("У таксофона нет собственного номера — позвонить на него нельзя, а каждый исходящий вызов списывает 1 медный самородок. У получателя номер определится как «Неизвестный».")
            .pointAt(util.vector().centerOf(pay))
            .placeNearTarget();
        scene.idle(90);
        scene.markAsFinished();
    }

    public static void servicePhone(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("service_phone", "Торговый телефон: жители и Удалённый склад");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();
        scene.idle(10);

        BlockPos srv = util.grid().at(3, 1, 3);
        BlockPos job = util.grid().at(4, 1, 3);
        BlockPos seat = util.grid().at(2, 1, 3);
        BlockPos ticker = util.grid().at(1, 1, 3);

        scene.world().showSection(util.select().position(srv).add(util.select().position(job)), Direction.DOWN);
        var villagerLink = scene.world().createEntity(level -> {
            Villager v = EntityType.VILLAGER.create(level);
            v.setVillagerData(v.getVillagerData().setProfession(VillagerProfession.LIBRARIAN).setLevel(2));
            v.moveTo(4.5, 1.0, 2.5, 180.0f, 0.0f);
            return v;
        });
        scene.idle(15);

        scene.overlay().showText(85)
            .text("Поставьте блок профессии вплотную к Торговому телефону — при звонке на его номер откроется окно торговли привязанного жителя (если он не спит и рядом с рабочим местом)!")
            .pointAt(util.vector().topOf(srv))
            .placeNearTarget();
        scene.idle(95);

        scene.world().modifyEntity(villagerLink, net.minecraft.world.entity.Entity::discard);
        scene.world().hideSection(util.select().position(job), Direction.UP);
        scene.idle(15);

        scene.world().showSection(util.select().position(seat).add(util.select().position(ticker)), Direction.DOWN);
        scene.idle(15);

        scene.overlay().showText(85)
            .text("Либо поставьте рядом Горелку Блейза / Сиденье со смотрителем у Контроллера запасов (Stock Ticker) — звонок откроет заказ предметов с вашего склада из любой точки сети!")
            .colored(PonderPalette.GREEN)
            .pointAt(util.vector().topOf(ticker))
            .placeNearTarget();
        scene.idle(95);
        scene.markAsFinished();
    }

    public static void radioStation(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("radio_station", "Радиостанция и радарная сеть");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();
        scene.idle(10);

        BlockPos station = util.grid().at(2, 1, 3);
        BlockPos dataLink = util.grid().at(2, 2, 3);
        BlockPos controller = util.grid().at(4, 1, 3);
        BlockPos bearing = util.grid().at(5, 1, 3);
        BlockPos dish = util.grid().at(5, 2, 3);

        scene.world().showSection(util.select().fromTo(2, 1, 3, 5, 3, 3), Direction.DOWN);
        scene.idle(15);

        scene.overlay().showControls(util.vector().topOf(station), Pointing.DOWN, 45)
            .rightClick()
            .withItem(stack("create_radar", "data_link"));
        scene.overlay().showLine(PonderPalette.BLUE, util.vector().centerOf(dataLink), util.vector().centerOf(controller), 90);

        scene.overlay().showText(85)
            .text("Свяжите Радиостанцию с Контроллером сети (Network Filterer) из Create: Radars с помощью Передатчика данных (Data Link), как монитор с радаром!")
            .pointAt(util.vector().centerOf(dataLink))
            .placeNearTarget();
        scene.idle(95);

        scene.overlay().showText(80)
            .text("Пока тарелка радара в этой сети вращается, Радиостанция онлайн и покрывает радиус вашей радарной антенны.")
            .colored(PonderPalette.GREEN)
            .pointAt(util.vector().topOf(dish))
            .placeNearTarget();
        scene.idle(90);

        scene.overlay().showText(75)
            .text("ПКМ по станции открывает настройку частоты 00–15. Если на ней говорят по рации или подан редстоун — станция вещает в эфир на своей частоте.")
            .pointAt(util.vector().topOf(station))
            .placeNearTarget();
        scene.idle(85);
        scene.markAsFinished();
    }

    public static void radio(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("radio", "Радио: приём, передача и редстоун по воздуху");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();
        scene.idle(10);

        BlockPos tx = util.grid().at(1, 1, 3);
        BlockPos lever = util.grid().at(1, 1, 4);
        BlockPos rx = util.grid().at(5, 1, 3);
        BlockPos wire = util.grid().at(5, 1, 2);
        BlockPos lamp = util.grid().at(5, 1, 1);

        scene.world().showSection(util.select().fromTo(1, 1, 1, 5, 1, 4), Direction.DOWN);
        scene.idle(15);

        scene.overlay().showText(75)
            .text("ПКМ по Радио открывает поворотную ручку частоты (00–15) и переключатель режима: Приём или Передача. Без привязки к станции радиус составляет 30 блоков.")
            .pointAt(util.vector().topOf(tx))
            .placeNearTarget();
        scene.idle(85);

        scene.world().modifyBlock(lever, s -> s.setValue(LeverBlock.POWERED, true), false);
        scene.world().modifyBlock(tx, s -> s.setValue(RadioBlock.ACTIVE, true), false);
        scene.world().modifyBlock(rx, s -> s.setValue(RadioBlock.ACTIVE, true), false);
        scene.world().modifyBlock(wire, s -> s.setValue(RedStoneWireBlock.POWER, 15), false);
        scene.world().modifyBlock(lamp, s -> s.setValue(net.minecraft.world.level.block.RedstoneLampBlock.LIT, true), false);
        scene.overlay().showLine(PonderPalette.OUTPUT, util.vector().topOf(tx), util.vector().topOf(rx), 70);

        scene.overlay().showText(80)
            .text("Подайте редстоун на передатчик — все приёмники на той же частоте выдадут сигнал редстоуна такой же силы и включат ретрансляцию звука!")
            .colored(PonderPalette.OUTPUT)
            .pointAt(util.vector().topOf(rx))
            .placeNearTarget();
        scene.idle(90);
        scene.markAsFinished();
    }

    public static void walkieAndHeadset(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("walkie_and_headset", "Рация и радиогарнитура");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(10);

        BlockPos station = util.grid().at(2, 1, 2);
        scene.world().showSection(util.select().position(station), Direction.DOWN);
        scene.idle(15);

        scene.overlay().showControls(util.vector().topOf(station), Pointing.DOWN, 45)
            .rightClick()
            .withItem(new ItemStack(TItems.WALKIE_TALKIE.get()));
        scene.overlay().showText(75)
            .text("Shift+ПКМ Рацией или Радиогарнитурой по Радиостанции привязывает их к её закрытой сети. Без привязки они работают в открытом канале на 30 блоков.")
            .pointAt(util.vector().topOf(station))
            .placeNearTarget();
        scene.idle(85);

        scene.overlay().showControls(util.vector().of(2.5, 2.2, 2.5), Pointing.DOWN, 45)
            .rightClick()
            .withItem(new ItemStack(TItems.WALKIE_TALKIE.get()));
        scene.overlay().showText(75)
            .text("Обычный ПКМ с рацией в руке включает/выключает режим передачи (загорается красная лампа). Слушать эфир можно даже когда рация просто лежит в инвентаре!")
            .colored(PonderPalette.GREEN)
            .pointAt(util.vector().of(2.5, 2.0, 2.5))
            .placeNearTarget();
        scene.idle(85);

        scene.overlay().showControls(util.vector().of(2.5, 2.2, 2.5), Pointing.DOWN, 45)
            .withItem(new ItemStack(TItems.RADIO_HEADSET.get()));
        scene.overlay().showText(70)
            .text("Радиогарнитура надевается на голову и постоянно передаёт и принимает голос без рук!")
            .pointAt(util.vector().of(2.5, 2.0, 2.5))
            .placeNearTarget();
        scene.idle(80);
        scene.markAsFinished();
    }

    public static void radioAudio(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("radio_audio", "Радиомикрофон, динамик, рупор и стойка");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();
        scene.idle(10);

        BlockPos mic = util.grid().at(1, 1, 3);
        BlockPos spk = util.grid().at(5, 1, 3);
        BlockPos stand = util.grid().at(3, 1, 3);

        scene.world().showSection(util.select().position(mic).add(util.select().position(spk)).add(util.select().position(stand)), Direction.DOWN);
        scene.idle(15);

        scene.world().modifyBlock(mic, s -> s.setValue(RadioAudioBlock.POWERED, true), false);
        scene.world().modifyBlock(spk, s -> s.setValue(RadioAudioBlock.POWERED, true), false);
        scene.overlay().showLine(PonderPalette.BLUE, util.vector().topOf(mic), util.vector().topOf(spk), 70);

        scene.overlay().showText(75)
            .text("Запитанный редстоуном Радиомикрофон улавливает голос и звуки (включая пластинки!) и транслирует их из всех Радиодинамиков той же частоты.")
            .pointAt(util.vector().topOf(mic))
            .placeNearTarget();
        scene.idle(85);

        scene.overlay().showControls(util.vector().topOf(spk), Pointing.DOWN, 45)
            .rightClick()
            .withItem(new ItemStack(TItems.MEGAPHONE.get()));
        scene.idle(10);
        scene.world().modifyBlock(spk, s -> s.setValue(RadioAudioBlock.MEGAPHONE, true), false);

        scene.overlay().showText(75)
            .text("ПКМ Рупором по динамику устанавливает его сверху и увеличивает радиус вещания до 64 блоков! В руках рупор работает при зажатии ПКМ.")
            .colored(PonderPalette.OUTPUT)
            .pointAt(util.vector().topOf(spk))
            .placeNearTarget();
        scene.idle(85);

        ItemStack handMic = new ItemStack(TItems.HAND_MICROPHONE.get());
        scene.overlay().showControls(util.vector().topOf(spk), Pointing.DOWN, 40).rightClick().withItem(handMic);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, spk, new AABB(spk), 60);
        scene.idle(45);

        scene.overlay().showControls(util.vector().topOf(stand), Pointing.DOWN, 40).rightClick().withItem(handMic);
        scene.world().modifyBlock(stand, s -> s.setValue(MicrophoneStandBlock.HAS_MIC, true), false);
        scene.overlay().showLine(PonderPalette.GREEN, util.vector().topOf(stand), util.vector().topOf(spk), 70);

        scene.overlay().showText(85)
            .text("Ручной микрофон привязывается ПКМ к динамикам напрямую. В руках работает при удержании ПКМ, а установленный на Стойку — вещает постоянно без редстоуна!")
            .colored(PonderPalette.GREEN)
            .pointAt(util.vector().topOf(stand))
            .placeNearTarget();
        scene.idle(95);
        scene.markAsFinished();
    }
}
