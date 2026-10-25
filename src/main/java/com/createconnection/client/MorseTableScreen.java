package com.createconnection.client;

import com.createconnection.CreateConnection;
import com.createconnection.morse.MorseCode;
import com.createconnection.morse.MorseTableBlockEntity;
import com.createconnection.network.MorseTablePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

public class MorseTableScreen extends Screen {
    private static final ResourceLocation PANEL = CreateConnection.id("textures/gui/morse_table.png");
    public static final int W = 210;
    public static final int H = 156;

    private final BlockPos pos;
    private int left;
    private int top;
    private EditBox inputBox;
    private Button punchButton;
    private Button playButton;
    private Button takeButton;

    public MorseTableScreen(BlockPos pos) {
        super(Component.translatable("block.create_connection.morse_table"));
        this.pos = pos;
    }

    private MorseTableBlockEntity be() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.getBlockEntity(pos) instanceof MorseTableBlockEntity table) {
            return table;
        }
        return null;
    }

    @Override
    protected void init() {
        this.left = (this.width - W) / 2;
        this.top = (this.height - H) / 2;

        MorseTableBlockEntity table = be();
        String initial = table != null ? table.getMessage() : "";
        boolean recorded = table != null && table.isCardRecorded();

        this.inputBox = new EditBox(this.font, this.left + 14, this.top + 38, 182, 16, Component.translatable("create_connection.gui.morse_table.hint"));
        this.inputBox.setMaxLength(48);
        this.inputBox.setBordered(false);
        this.inputBox.setTextColor(0xFFF0D080);
        this.inputBox.setValue(initial);
        this.inputBox.setEditable(!recorded && table != null && (table.hasPaper() || !table.getCard().isEmpty()));
        this.addRenderableWidget(this.inputBox);
        if (!recorded) {
            this.setInitialFocus(this.inputBox);
        }

        this.punchButton = Button.builder(Component.translatable("create_connection.gui.morse_table.punch"), b -> {
            MorseTableBlockEntity t = be();
            if (t == null || t.isCardRecorded() || !t.hasPaper()) {
                return;
            }
            String msg = this.inputBox.getValue().trim();
            if (!msg.isEmpty()) {
                PacketDistributor.sendToServer(new MorseTablePayload(this.pos, MorseTablePayload.ACTION_PUNCH, msg));
            }
        }).bounds(this.left + 12, this.top + 106, 60, 20).build();

        this.playButton = Button.builder(Component.translatable("create_connection.gui.morse_table.play"), b -> {
            MorseTableBlockEntity t = be();
            if (t == null || !t.isCardRecorded()) {
                return;
            }
            PacketDistributor.sendToServer(new MorseTablePayload(this.pos, MorseTablePayload.ACTION_PLAY_STOP, ""));
        }).bounds(this.left + 75, this.top + 106, 60, 20).build();

        this.takeButton = Button.builder(Component.translatable("create_connection.gui.morse_table.take"), b -> {
            PacketDistributor.sendToServer(new MorseTablePayload(this.pos, MorseTablePayload.ACTION_TAKE_CARD, ""));
        }).bounds(this.left + 138, this.top + 106, 60, 20).build();

        this.addRenderableWidget(this.punchButton);
        this.addRenderableWidget(this.playButton);
        this.addRenderableWidget(this.takeButton);
        updateWidgetStates(table);
    }

    private void updateWidgetStates(MorseTableBlockEntity table) {
        if (table == null) return;
        boolean recorded = table.isCardRecorded();
        boolean hasPaper = table.hasPaper();
        boolean hasAny = hasPaper || !table.getCard().isEmpty();

        if (recorded) {
            String cardMsg = table.getMessage();
            if (!cardMsg.equals(this.inputBox.getValue())) {
                this.inputBox.setValue(cardMsg);
            }
            this.inputBox.setEditable(false);
            this.inputBox.setFocused(false);
            this.punchButton.active = false;
            this.playButton.active = true;
        } else if (hasPaper) {
            this.inputBox.setEditable(true);
            String morse = MorseCode.encode(this.inputBox.getValue());
            this.punchButton.active = !morse.isEmpty();
            this.playButton.active = false;
        } else {
            this.inputBox.setEditable(false);
            this.punchButton.active = false;
            this.playButton.active = false;
        }
        this.playButton.setMessage(Component.translatable(
            table.isPlaying() ? "create_connection.gui.morse_table.stop" : "create_connection.gui.morse_table.play"
        ));
        this.takeButton.active = hasAny;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        MorseTableBlockEntity table = be();
        if (table == null || this.minecraft.player == null || this.minecraft.player.position().distanceToSqr(pos.getCenter()) > 64.0) {
            onClose();
            return;
        }
        if (!table.hasPaper() && table.getCard().isEmpty()) {
            onClose();
            return;
        }
        updateWidgetStates(table);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        MorseTableBlockEntity table = be();
        if (table != null && table.isCardRecorded()) {
            if (keyCode == 256) {
                this.onClose();
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        if (this.inputBox.isFocused()) {
            if (keyCode == 256) {
                this.onClose();
                return true;
            }
            if (this.inputBox.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
            if (keyCode != 258) {
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
        renderBackground(g, mouseX, mouseY, partialTick);
        g.blit(PANEL, this.left, this.top, 0.0f, 0.0f, W, H, 256, 256);

        g.drawString(this.font, this.title, this.left + 12, this.top + 8, 0xFFE8D8B0, false);
        MorseTableBlockEntity table = be();
        boolean recorded = table != null && table.isCardRecorded();
        Component subTitle = Component.translatable(
            recorded ? "create_connection.gui.morse_table.locked" : "create_connection.gui.morse_table.hint"
        );
        g.drawString(this.font, subTitle, this.left + 12, this.top + 23, 0xFF3E2918, false);

        String text = this.inputBox.getValue();
        String rawMorse = MorseCode.encode(text);
        String prettyMorse = MorseCode.formatPretty(rawMorse);

        int tapeX0 = this.left + 16;
        int tapeY0 = this.top + 64;
        int maxW = 176;

        int cx = tapeX0;
        for (int i = 0; i < rawMorse.length(); i++) {
            char c = rawMorse.charAt(i);
            if (c == '.') {
                if (cx + 4 > tapeX0 + maxW) break;
                g.fill(cx, tapeY0 + 3, cx + 3, tapeY0 + 9, 0xFF2B2118);
                cx += 5;
            } else if (c == '-') {
                if (cx + 9 > tapeX0 + maxW) break;
                g.fill(cx, tapeY0 + 3, cx + 8, tapeY0 + 9, 0xFF2B2118);
                cx += 10;
            } else if (c == ' ') {
                cx += 4;
            } else if (c == '/') {
                cx += 6;
            }
        }

        if (table != null && table.isPlaying() && table.getTotalTicks() > 0) {
            float progress = (float) table.getPlayTick() / (float) table.getTotalTicks();
            int headX = tapeX0 + Math.round(progress * Math.max(8, cx - tapeX0));
            g.fill(headX, this.top + 61, headX + 2, this.top + 97, 0xFFD94826);
        }

        String trimmedMorse = this.font.plainSubstrByWidth(prettyMorse.isEmpty() ? "—" : prettyMorse, 176);
        g.drawString(this.font, trimmedMorse, this.left + 16, this.top + 84, 0xFF3E2918, false);

        Component status;
        int statusColor = 0xFFD0C090;
        if (table != null) {
            if (table.isPlaying()) {
                status = Component.translatable("create_connection.gui.morse_table.status_playing");
                statusColor = 0xFF70E070;
            } else if (recorded) {
                status = Component.translatable("create_connection.gui.morse_table.status_card");
                statusColor = 0xFFF0D070;
            } else if (table.hasPaper()) {
                status = Component.translatable("create_connection.gui.morse_table.status_paper");
            } else {
                status = Component.translatable("create_connection.gui.morse_table.status_empty");
                statusColor = 0xFFE07060;
            }
        } else {
            status = Component.empty();
        }
        g.drawString(this.font, status, this.left + 14, this.top + 135, statusColor, false);

        super.render(g, mouseX, mouseY, partialTick);
    }
}
