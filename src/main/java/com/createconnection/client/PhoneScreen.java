package com.createconnection.client;

import com.createconnection.CreateConnection;
import com.createconnection.network.PhoneActionPayload;
import com.createconnection.network.PhoneStatePayload;
import com.createconnection.phone.CallManager;
import com.createconnection.phone.PhoneKind;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;
import org.patryk3211.powergrid.collections.ModdedSoundEvents;

public class PhoneScreen extends Screen {
    private static final ResourceLocation PANEL = CreateConnection.id("textures/gui/phone_panel.png");
    private static final ResourceLocation PLATE = CreateConnection.id("textures/gui/dial_plate.png");
    private static final ResourceLocation WHEEL = CreateConnection.id("textures/gui/dial_wheel.png");
    private static final ResourceLocation STOP = CreateConnection.id("textures/gui/dial_stop.png");
    private static final ResourceLocation HIGHLIGHT = CreateConnection.id("textures/gui/dial_highlight.png");

    private static final int W = 180;
    private static final int H = 192;
    private static final int DIAL_CX = 90;
    private static final int DIAL_CY = 120;
    private static final int DIAL_SIZE = 112;
    private static final int HOLE_RING = 40;
    private static final int HOLE_RADIUS = 10;
    private static final float STOP_ANGLE = 55.0f;
    private static final float FORWARD_SPEED = 420.0f;
    private static final float RETURN_SPEED = 260.0f;
    private static final int NUMBER_LENGTH = 5;
    private static final int NUM_X0 = 10;
    private static final int NUM_X1 = 132;
    private static final int NUM_Y0 = 9;
    private static final int NUM_Y1 = 23;

    private PhoneStatePayload state;
    private int left;
    private int top;
    private final StringBuilder dialed = new StringBuilder();
    private int animDigit = -1;
    private float angle = 0.0f;
    private float targetAngle = 0.0f;
    private boolean returning = false;
    private long lastFrameNanos;
    private boolean editingNumber = false;
    private final StringBuilder numberInput = new StringBuilder();

    public PhoneScreen(PhoneStatePayload state) {
        super(Component.translatable("create_connection.gui.phone"));
        this.state = state;
    }

    public BlockPos getPos() {
        return this.state.pos();
    }

    private PhoneKind kind() {
        return PhoneKind.values()[this.state.kind()];
    }

    private CallManager.Status status() {
        return CallManager.Status.values()[this.state.status()];
    }

    private static float holeAngle(int d) {
        int idx = d == 0 ? 9 : d - 1;
        return 115.0f + (float) idx * 27.0f;
    }

    private static float travel(int d) {
        float h = holeAngle(d);
        float diff = h - STOP_ANGLE;
        if (diff <= 0.0f) diff += 360.0f;
        return diff - 10.0f;
    }

    private int holeX(int d) {
        double rad = Math.toRadians(holeAngle(d));
        return this.left + DIAL_CX + (int) Math.round(Math.cos(rad) * (double) HOLE_RING);
    }

    private int holeY(int d) {
        double rad = Math.toRadians(holeAngle(d));
        return this.top + DIAL_CY + (int) Math.round(Math.sin(rad) * (double) HOLE_RING);
    }

    private int hoveredHole(double mx, double my) {
        if (!kind().canDial || this.animDigit >= 0 || status() != CallManager.Status.IDLE) {
            return -1;
        }
        for (int d = 0; d <= 9; d++) {
            double dx = mx - (double) holeX(d);
            double dy = my - (double) holeY(d);
            if (dx * dx + dy * dy <= (double) (HOLE_RADIUS * HOLE_RADIUS)) {
                return d;
            }
        }
        return -1;
    }

    private boolean overNumberLine(double mx, double my) {
        return this.state.canEdit()
            && mx >= (double) (this.left + NUM_X0)
            && mx <= (double) (this.left + NUM_X1)
            && my >= (double) (this.top + NUM_Y0)
            && my <= (double) (this.top + NUM_Y1);
    }

    @Override
    protected void init() {
        this.left = (this.width - W) / 2;
        this.top = (this.height - H) / 2;
        this.lastFrameNanos = System.nanoTime();
    }

    public void update(PhoneStatePayload next) {
        int prevStatus = this.state.status();
        this.state = next;
        if (prevStatus != next.status()) {
            this.dialed.setLength(0);
        }
    }

    private void tickDial() {
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (float) (now - this.lastFrameNanos) / 1.0E9f);
        this.lastFrameNanos = now;
        if (this.animDigit < 0) return;

        if (!this.returning) {
            float prev = this.angle;
            this.angle += FORWARD_SPEED * dt;
            if ((int) (this.angle / 27.0f) > (int) (prev / 27.0f)) {
                click(0.85f);
            }
            if (this.angle >= this.targetAngle) {
                this.angle = this.targetAngle;
                this.returning = true;
                click(0.6f);
            }
        } else {
            float prev = this.angle;
            this.angle -= RETURN_SPEED * dt;
            if ((int) (prev / 18.0f) > (int) (Math.max(0.0f, this.angle) / 18.0f)) {
                click(1.35f);
            }
            if (this.angle <= 0.0f) {
                int finished = this.animDigit;
                this.angle = 0.0f;
                this.animDigit = -1;
                this.returning = false;
                onDigitDialed(finished);
            }
        }
    }

    private void startDial(int d) {
        if (!kind().canDial || this.animDigit >= 0 || status() != CallManager.Status.IDLE) return;
        if (this.dialed.length() >= NUMBER_LENGTH) return;
        this.editingNumber = false;
        this.animDigit = d;
        this.angle = 0.0f;
        this.targetAngle = travel(d);
        this.returning = false;
        click(0.95f);
    }

    private void onDigitDialed(int d) {
        if (this.dialed.length() >= NUMBER_LENGTH) return;
        this.dialed.append(d);
        if (this.dialed.length() == NUMBER_LENGTH) {
            String target = this.dialed.toString();
            this.dialed.setLength(0);
            send(PhoneActionPayload.DIAL, target);
        }
    }

    private void startEditingNumber() {
        this.editingNumber = true;
        this.numberInput.setLength(0);
        this.numberInput.append(this.state.ownNumber());
        click(1.1f);
    }

    private void commitNumber() {
        if (this.editingNumber) {
            this.editingNumber = false;
            if (this.numberInput.length() == NUMBER_LENGTH && !this.numberInput.toString().equals(this.state.ownNumber())) {
                send(PhoneActionPayload.SET_NUMBER, this.numberInput.toString());
            }
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            if (overNumberLine(mx, my)) {
                if (!this.editingNumber) {
                    startEditingNumber();
                }
                return true;
            }
            if (this.editingNumber) {
                commitNumber();
            }
            int d = hoveredHole(mx, my);
            if (d >= 0) {
                startDial(d);
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.editingNumber) {
            if (keyCode == 256) {
                this.editingNumber = false;
                return true;
            }
            if (keyCode == 257 || keyCode == 335) {
                commitNumber();
                return true;
            }
            if (keyCode == 259 && !this.numberInput.isEmpty()) {
                this.numberInput.deleteCharAt(this.numberInput.length() - 1);
                click(1.0f);
                return true;
            }
            int d = -1;
            if (keyCode >= 48 && keyCode <= 57) d = keyCode - 48;
            else if (keyCode >= 320 && keyCode <= 329) d = keyCode - 320;
            if (d >= 0 && this.numberInput.length() < NUMBER_LENGTH) {
                this.numberInput.append(d);
                click(1.2f);
                if (this.numberInput.length() == NUMBER_LENGTH) {
                    commitNumber();
                }
                return true;
            }
            return true;
        }

        if (keyCode == 340 || keyCode == 344) {
            if (status() != CallManager.Status.IDLE) {
                send(PhoneActionPayload.HANGUP, "");
                onClose();
            } else if (!this.dialed.isEmpty()) {
                this.dialed.setLength(0);
                click(0.6f);
            }
            return true;
        }
        if (keyCode == 259 && !this.dialed.isEmpty() && this.animDigit < 0) {
            this.dialed.deleteCharAt(this.dialed.length() - 1);
            return true;
        }
        if (keyCode == 32 && kind().hasSpeaker) {
            send(PhoneActionPayload.TOGGLE_SPEAKER, "");
            return true;
        }
        int d = -1;
        if (keyCode >= 48 && keyCode <= 57) d = keyCode - 48;
        else if (keyCode >= 320 && keyCode <= 329) d = keyCode - 320;
        if (d >= 0) {
            startDial(d);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void click(float pitch) {
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModdedSoundEvents. ROTARY_CLICK.getMainEvent(), pitch, 0.5f));
        }
    }

    private void send(int action, String arg) {
        PacketDistributor.sendToServer(new PhoneActionPayload(this.state.pos(), action, arg));
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        g.blit(PANEL, this.left, this.top, 0.0f, 0.0f, W, H, W, H);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        tickDial();
        super.render(g, mouseX, mouseY, partialTick);
        renderDisplay(g, mouseX, mouseY);

        int half = DIAL_SIZE / 2;
        int dLeft = this.left + DIAL_CX - half;
        int dTop = this.top + DIAL_CY - half;
        g.blit(PLATE, dLeft, dTop, 0.0f, 0.0f, DIAL_SIZE, DIAL_SIZE, DIAL_SIZE, DIAL_SIZE);

        int hov = hoveredHole(mouseX, mouseY);
        if (hov >= 0) {
            drawHighlight(g, holeX(hov), holeY(hov));
        }

        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate((float) (this.left + DIAL_CX), (float) (this.top + DIAL_CY), 0.0f);
        pose.mulPose(Axis.ZP.rotationDegrees(-this.angle));
        g.blit(WHEEL, -half, -half, 0.0f, 0.0f, DIAL_SIZE, DIAL_SIZE, DIAL_SIZE, DIAL_SIZE);
        pose.popPose();

        g.blit(STOP, dLeft, dTop, 0.0f, 0.0f, DIAL_SIZE, DIAL_SIZE, DIAL_SIZE, DIAL_SIZE);

        Component hint = status() != CallManager.Status.IDLE
            ? Component.translatable("create_connection.gui.shift_hangup")
            : (kind().hasSpeaker
                ? Component.translatable("create_connection.gui.space_speaker")
                : Component.translatable("create_connection.gui.dial_hint"));
        g.drawCenteredString(this.font, hint, this.left + W / 2, this.top + H - 13, 0xA89B84);
    }

    private void drawHighlight(GuiGraphics g, int cx, int cy) {
        g.blit(HIGHLIGHT, cx - 12, cy - 12, 0.0f, 0.0f, 24, 24, 24, 24);
    }

    private void renderDisplay(GuiGraphics g, int mx, int my) {
        int pwrColor = this.state.powered() ? -10817430 : -38342;
        g.fill(this.left + W - 18, this.top + 13, this.left + W - 12, this.top + 19, pwrColor);

        if (kind() == PhoneKind.PAYPHONE) {
            g.drawString(
                this.font,
                Component.translatable("create_connection.gui.payphone_banner").withStyle(ChatFormatting.GOLD),
                this.left + 12,
                this.top + 12,
                0xFFFFFF,
                false
            );
        } else if (this.editingNumber) {
            String cur = (System.currentTimeMillis() / 400L) % 2L == 0L ? "_" : " ";
            String padded = String.format("%-5s", this.numberInput.toString() + cur).replace(' ', '·');
            g.drawString(
                this.font,
                Component.translatable("create_connection.gui.own_number", padded).withStyle(ChatFormatting.YELLOW),
                this.left + 12,
                this.top + 12,
                0xFFFFFF,
                false
            );
        } else {
            String num = this.state.ownNumber().isEmpty() ? "-----" : this.state.ownNumber();
            boolean hov = overNumberLine(mx, my);
            ChatFormatting fmt = hov ? ChatFormatting.YELLOW : ChatFormatting.AQUA;
            Component line = Component.translatable("create_connection.gui.own_number", num).withStyle(fmt);
            if (this.state.canEdit()) {
                line = line.copy().append(Component.literal(" ✎").withStyle(hov ? ChatFormatting.GOLD : ChatFormatting.DARK_GRAY));
            }
            g.drawString(this.font, line, this.left + 12, this.top + 12, 0xFFFFFF, false);
        }

        g.drawString(this.font, statusLine(), this.left + 12, this.top + 26, 0xE6E0D0, false);

        if (status() == CallManager.Status.IDLE && kind().canDial) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < NUMBER_LENGTH; i++) {
                if (i > 0) sb.append(' ');
                sb.append(i < this.dialed.length() ? this.dialed.charAt(i) : '_');
            }
            String s = sb.toString();
            int sw = this.font.width(s);
            g.drawString(this.font, s, this.left + W - 24 - sw, this.top + 12, 0xFFD37F, false);
        }

        if (kind().hasSpeaker && this.state.speakerMode()) {
            Component spk = Component.translatable("create_connection.gui.speaker_badge").withStyle(ChatFormatting.GOLD);
            g.drawString(this.font, spk, this.left + W - 12 - this.font.width(spk), this.top + 26, 0xFFFFFF, false);
        }
    }

    private Component statusLine() {
        if (!this.state.message().getString().isEmpty()) {
            return this.state.message().copy().withStyle(ChatFormatting.RED);
        }
        if (!this.state.powered()) {
            return Component.translatable("create_connection.status.no_power").withStyle(ChatFormatting.RED);
        }
        return switch (status()) {
            case IDLE -> Component.translatable("create_connection.status.ready").withStyle(ChatFormatting.GREEN);
            case DIALING_OUT -> Component.translatable("create_connection.status.calling", this.state.peerNumber())
                .withStyle(ChatFormatting.YELLOW);
            case RINGING_IN -> Component.translatable("create_connection.status.incoming", peer())
                .withStyle(ChatFormatting.GOLD);
            case ACTIVE -> Component.translatable("create_connection.status.active", peer())
                .withStyle(ChatFormatting.AQUA);
        };
    }

    private Component peer() {
        return this.state.peerNumber().isEmpty()
            ? Component.translatable("create_connection.unknown_caller")
            : Component.literal(this.state.peerNumber());
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return !this.editingNumber;
    }

    @Override
    public void removed() {
        send(PhoneActionPayload.CLOSE_SCREEN, "");
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
