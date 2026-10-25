package com.createconnection.client;

import com.createconnection.CreateConnection;
import com.createconnection.network.RadioConfigPayload;
import com.createconnection.radio.ITunable;
import com.createconnection.radio.PortableStationItem;
import com.createconnection.radio.RadioBlockEntity;
import com.createconnection.radio.RadioStationBlockEntity;
import com.createconnection.registry.TSounds;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Locale;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public class RadioScreen extends Screen {
    private static final ResourceLocation PANEL = CreateConnection.id("textures/gui/radio_panel.png");
    private static final ResourceLocation KNOB = CreateConnection.id("textures/gui/radio_knob.png");
    public static final BlockPos PORTABLE_MARKER_POS = new BlockPos(0, -9999, 0);

    public static final int W = 210;
    public static final int H = 124;
    public static final int KNOB_CX = 168;
    public static final int KNOB_CY = 58;
    public static final int KNOB_SIZE = 64;
    private static final float KNOB_MIN = -135.0f;
    private static final float KNOB_MAX = 135.0f;
    public static final int SCALE_X0 = 14;
    public static final int SCALE_X1 = 118;
    public static final int SCALE_Y0 = 22;
    public static final int SCALE_Y1 = 50;
    public static final int LCD_X0 = 14;
    public static final int LCD_Y0 = 58;
    public static final int LCD_X1 = 62;
    public static final int LCD_Y1 = 92;
    public static final int MODE_X0 = 68;
    public static final int MODE_X1 = 128;
    public static final int TX_Y0 = 58;
    public static final int RX_Y0 = 77;
    public static final int MODE_H = 15;

    private final BlockPos pos;
    private final boolean portable;
    private int left;
    private int top;
    private int frequency;
    private float tune;
    private boolean transmit;
    private float knobAngle;
    private float needle;
    private boolean draggingKnob;
    private boolean draggingScale;
    private long lastFrame;
    private long lastStatic;
    private int idleStaticTicks;
    private boolean station;
    private Component titleOverride;

    public RadioScreen(BlockPos pos) {
        super(Component.translatable("block.create_connection.radio"));
        this.pos = pos;
        this.portable = false;
        ITunable be = be();
        if (be != null) {
            this.tune = be.getTune();
            this.frequency = be.getFrequency();
        }
        this.station = be instanceof RadioStationBlockEntity;
        this.transmit = this.station || (be instanceof RadioBlockEntity r && r.isTransmitter());
        if (this.station) {
            setTitleKey();
        }
        this.knobAngle = angleFor(this.tune);
        this.needle = this.tune;
    }

    public RadioScreen(ItemStack portableStack) {
        super(Component.translatable("item.create_connection.portable_station"));
        this.pos = PORTABLE_MARKER_POS;
        this.portable = true;
        this.station = false;
        this.tune = PortableStationItem.getTune(portableStack);
        this.frequency = Math.round(this.tune);
        this.transmit = PortableStationItem.isTransmitMode(portableStack);
        this.titleOverride = Component.translatable("item.create_connection.portable_station");
        this.knobAngle = angleFor(this.tune);
        this.needle = this.tune;
    }

    private void setTitleKey() {
        this.titleOverride = Component.translatable("block.create_connection.radio_station");
    }

    private ITunable be() {
        if (this.portable) return null;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.getBlockEntity(pos) instanceof ITunable t) {
            return t;
        }
        return null;
    }

    private static float angleFor(float tuneValue) {
        return KNOB_MIN + 270.0f * tuneValue / 15.0f;
    }

    private boolean hasActiveSignal() {
        if (this.portable) {
            return false;
        }
        ITunable be = be();
        if (be instanceof RadioStationBlockEntity st) {
            return st.getInputSignal() > 0;
        }
        if (be instanceof RadioBlockEntity r) {
            return r.getSignalLevel() > 0 || !r.getStation().isEmpty();
        }
        return false;
    }

    @Override
    protected void init() {
        this.left = (this.width - W) / 2;
        this.top = (this.height - H) / 2;
        this.lastFrame = Util.getNanos();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        if (this.portable) {
            if (this.minecraft.player == null || PortableStationItem.findInPlayer(this.minecraft.player).isEmpty()) {
                onClose();
                return;
            }
        } else {
            ITunable be = be();
            if (be == null || this.minecraft.player == null || this.minecraft.player.position().distanceToSqr(pos.getCenter()) > 64.0) {
                onClose();
                return;
            }
        }
        if (!hasActiveSignal()) {
            this.idleStaticTicks++;
            if (this.idleStaticTicks >= 14) {
                this.idleStaticTicks = 0;
                playStatic(0.28f);
            }
        } else {
            this.idleStaticTicks = 0;
        }
    }

    private static float applySoftDetent(float raw) {
        float clamped = Mth.clamp(raw, 0.0f, 15.0f);
        int nearest = Math.round(clamped);
        if (Math.abs(clamped - nearest) < 0.035f) {
            return (float) nearest;
        }
        return Math.round(clamped * 100.0f) / 100.0f;
    }

    private void setTune(float newTune) {
        float clamped = applySoftDetent(newTune);
        if (Math.abs(clamped - this.tune) < 0.005f) {
            return;
        }
        float oldTune = this.tune;
        this.tune = clamped;
        this.frequency = Math.round(clamped);
        if (Math.abs(clamped - oldTune) >= 0.02f && !hasActiveSignal()) {
            playStatic(0.55f);
        }
        send();
    }

    private void setFrequency(int freq) {
        setTune((float) Mth.clamp(freq, 0, 15));
    }

    private void setMode(boolean tx) {
        if (tx == this.transmit || this.station) {
            return;
        }
        this.transmit = tx;
        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.LEVER_CLICK, tx ? 0.6f : 0.5f, 0.4f));
        send();
    }

    private void playStatic(float volume) {
        long now = Util.getMillis();
        if (now - this.lastStatic < 55L) {
            return;
        }
        this.lastStatic = now;
        float pitch = 0.8f + (this.minecraft.level != null ? this.minecraft.level.random.nextFloat() * 0.45f : 0.2f);
        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(TSounds.RADIO_STATIC.get(), pitch, volume));
    }

    private void send() {
        if (this.portable && this.minecraft.player != null) {
            ItemStack st = PortableStationItem.findInPlayer(this.minecraft.player);
            if (!st.isEmpty()) {
                PortableStationItem.configureTune(st, this.tune, this.transmit);
            }
        }
        PacketDistributor.sendToServer(new RadioConfigPayload(this.pos, this.tune, this.transmit));
    }

    private boolean overKnob(double mx, double my) {
        double dx = mx - (this.left + KNOB_CX);
        double dy = my - (this.top + KNOB_CY);
        return dx * dx + dy * dy <= 1024.0;
    }

    private boolean overScale(double mx, double my) {
        double rx = mx - this.left;
        double ry = my - this.top;
        return rx >= SCALE_X0 && rx < SCALE_X1 && ry >= SCALE_Y0 && ry < SCALE_Y1;
    }

    private void dragKnob(double mx, double my) {
        double dx = mx - (this.left + KNOB_CX);
        double dy = my - (this.top + KNOB_CY);
        if (dx * dx + dy * dy < 9.0) {
            return;
        }
        float deg = (float) Math.toDegrees(Math.atan2(dx, -dy));
        deg = Mth.clamp(deg, KNOB_MIN, KNOB_MAX);
        float rawTune = (deg - KNOB_MIN) / 270.0f * 15.0f;
        setTune(rawTune);
    }

    private void dragScale(double mx) {
        double rx = mx - this.left;
        float rawTune = (float) ((rx - SCALE_X0 - 8.0) / 88.0) * 15.0f;
        setTune(rawTune);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            if (overKnob(mx, my)) {
                this.draggingKnob = true;
                dragKnob(mx, my);
                return true;
            }
            if (overScale(mx, my)) {
                this.draggingScale = true;
                dragScale(mx);
                return true;
            }
            double rx = mx - this.left;
            double ry = my - this.top;
            if (!this.station && rx >= MODE_X0 && rx < MODE_X1) {
                if (ry >= TX_Y0 && ry < TX_Y0 + MODE_H) {
                    setMode(true);
                    return true;
                }
                if (ry >= RX_Y0 && ry < RX_Y0 + MODE_H) {
                    setMode(false);
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (this.draggingKnob) {
            dragKnob(mx, my);
            return true;
        }
        if (this.draggingScale) {
            dragScale(mx);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        this.draggingKnob = false;
        this.draggingScale = false;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        if (scrollY != 0.0) {
            float step = hasShiftDown() ? 0.5f : 0.1f;
            setTune(this.tune + (scrollY > 0.0 ? step : -step));
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        float step = hasShiftDown() ? 0.5f : 0.15f;
        switch (keyCode) {
            case 68, 87, 262, 265 -> {
                setTune(this.tune + step);
                return true;
            }
            case 65, 83, 263, 264 -> {
                setTune(this.tune - step);
                return true;
            }
            case 258 -> {
                if (!this.station) {
                    setMode(!this.transmit);
                }
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderTransparentBackground(g);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        long now = Util.getNanos();
        float dt = Math.min(0.1f, (float) (now - this.lastFrame) / 1.0E9f);
        this.lastFrame = now;

        float targetAngle = angleFor(this.tune);
        this.knobAngle += (targetAngle - this.knobAngle) * Math.min(1.0f, dt * 22.0f);
        this.needle += (this.tune - this.needle) * Math.min(1.0f, dt * 18.0f);

        int sig = 0;
        boolean online = false;
        Component statusText;
        if (this.portable) {
            online = true;
            sig = 0;
            statusText = Component.translatable("create_connection.gui.radio.networked", 64);
        } else {
            ITunable be = be();
            if (be instanceof RadioStationBlockEntity st) {
                sig = st.getInputSignal();
                online = st.getEffectiveRange() > 0.0;
                if (!st.isConnected()) {
                    statusText = Component.translatable("create_connection.gui.radio.no_controller");
                } else if (online) {
                    statusText = Component.translatable("create_connection.gui.radio.networked", (int) st.getEffectiveRange());
                } else {
                    statusText = Component.translatable("create_connection.gui.radio.radar_idle");
                }
            } else if (be instanceof RadioBlockEntity r) {
                sig = r.getSignalLevel();
                online = !r.getStation().isEmpty();
                if (online) {
                    statusText = Component.translatable("create_connection.gui.radio.station", r.getStation());
                } else {
                    statusText = Component.translatable("create_connection.gui.radio.open_air", r.getRange());
                }
            } else {
                statusText = Component.empty();
            }
        }

        g.blit(PANEL, this.left, this.top, 0.0f, 0.0f, W, H, 256, 256);
        g.drawString(this.font, this.titleOverride != null ? this.titleOverride : this.title, this.left + 10, this.top + 7, -1517136, false);

        int scaleW = 88;
        for (int i = 0; i <= 15; i++) {
            int tx = this.left + SCALE_X0 + 8 + Math.round((float) i * (float) scaleW / 15.0f);
            boolean major = (i % 5 == 0);
            g.fill(tx, this.top + 50 - (major ? 11 : 7), tx + 1, this.top + 50 - 3, -12965350);
            if (major) {
                String lbl = String.format(Locale.ROOT, "%02d", i);
                g.drawString(this.font, lbl, tx - this.font.width(lbl) / 2 + 1, this.top + 22 + 3, -12965350, false);
            }
        }

        float exactNeedleX = (float) (this.left + SCALE_X0 + 8) + this.needle * (float) scaleW / 15.0f;
        int nx = Math.round(exactNeedleX);
        g.fill(nx, this.top + 24, nx + 1, this.top + 48, -4184548);
        if (Math.abs(exactNeedleX - nx) > 0.2f) {
            int adj = exactNeedleX > nx ? nx + 1 : nx - 1;
            g.fill(adj, this.top + 25, adj + 1, this.top + 47, 0x66C0241C);
        }

        boolean isIntWave = Math.abs(this.tune - Math.round(this.tune)) < 0.05f;
        String lcdText = isIntWave
            ? String.format(Locale.ROOT, "%02d", Math.round(this.tune))
            : String.format(Locale.ROOT, "%04.1f", this.tune);
        float lcdScale = isIntWave ? 3.0f : 2.1f;

        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate((float) this.left + 38.0f, (float) this.top + 75.0f, 0.0f);
        pose.scale(lcdScale, lcdScale, 1.0f);
        g.drawString(
            this.font,
            lcdText,
            -this.font.width(lcdText) / 2 + 1,
            -4,
            this.transmit ? -30147 : -8589174,
            false
        );
        pose.popPose();

        drawMode(g, 58, Component.translatable("create_connection.gui.radio.transmit"), this.transmit, this.transmit && (sig > 0 || ((this.station || this.portable) && online)), -46550);
        if (!this.station) {
            drawMode(g, 77, Component.translatable("create_connection.gui.radio.receive"), !this.transmit, !this.transmit && sig > 0, -10817430);
        }

        pose.pushPose();
        pose.translate((float) (this.left + KNOB_CX), (float) (this.top + KNOB_CY), 0.0f);
        pose.mulPose(Axis.ZP.rotationDegrees(this.knobAngle));
        g.blit(KNOB, -32, -32, 0.0f, 0.0f, 64, 64, 64, 64);
        pose.popPose();

        g.drawString(this.font, statusText, this.left + 12, this.top + 104, online ? -6494209 : -1517136, false);

        for (int b = 0; b < 5; b++) {
            boolean lit = sig > b * 3;
            int bx = this.left + W - 46 + b * 7;
            int bh = 3 + b * 2;
            g.fill(bx, this.top + 112 - bh, bx + 5, this.top + 112, lit ? (this.transmit ? -38342 : -9768838) : -12961222);
        }
    }

    private void drawMode(GuiGraphics g, int yOff, Component label, boolean selected, boolean activeLed, int ledColor) {
        int x0 = this.left + MODE_X0;
        int y0 = this.top + yOff;
        g.fill(x0, y0, this.left + MODE_X1, y0 + 15, selected ? -7706054 : -11912662);
        g.fill(x0 + 1, y0 + 1, this.left + MODE_X1 - 1, y0 + 14, selected ? -4681126 : -10597836);
        int led = activeLed ? ledColor : (selected ? -8758742 : -14016488);
        g.fill(x0 + 4, y0 + 5, x0 + 9, y0 + 10, led);
        g.drawString(this.font, label, x0 + 12, y0 + 4, selected ? -14018038 : -4675448, false);
    }
}
