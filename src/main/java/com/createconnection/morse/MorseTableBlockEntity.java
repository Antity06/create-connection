package com.createconnection.morse;

import com.createconnection.network.MorseTablePayload;
import com.createconnection.radio.RadioRegistry;
import com.createconnection.registry.TItems;
import com.createconnection.registry.TSounds;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class MorseTableBlockEntity extends SmartBlockEntity {
    private boolean hasPaper;
    private ItemStack card = ItemStack.EMPTY;
    private String draftMessage = "";
    private boolean playing;
    private boolean toneOn;
    private int playTick;
    private boolean[] timeline = new boolean[0];

    public MorseTableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    @Override
    public void initialize() {
        super.initialize();
        if (level != null && !level.isClientSide) {
            RadioRegistry.addMorseTable(this);
        }
    }

    @Override
    public void invalidate() {
        super.invalidate();
        RadioRegistry.removeMorseTable(this);
    }

    public boolean hasPaper() {
        return hasPaper;
    }

    public ItemStack getCard() {
        return card;
    }

    public boolean isCardRecorded() {
        return !card.isEmpty() && !PunchCardItem.getMessage(card).isEmpty();
    }

    public String getMessage() {
        if (!card.isEmpty()) {
            return PunchCardItem.getMessage(card);
        }
        return draftMessage;
    }

    public boolean isPlaying() {
        return playing;
    }

    public boolean isToneOn() {
        return playing && toneOn;
    }

    public int getPlayTick() {
        return playTick;
    }

    public int getTotalTicks() {
        return timeline.length;
    }

    public void setPaper(boolean paper) {
        this.hasPaper = paper;
        if (paper) {
            this.draftMessage = "";
        }
        stopPlaying();
        syncState();
    }

    public void setPaperClientPreview() {
        this.hasPaper = true;
        this.card = ItemStack.EMPTY;
        this.draftMessage = "";
    }

    public void setCard(ItemStack stack) {
        this.card = stack;
        this.hasPaper = false;
        this.draftMessage = PunchCardItem.getMessage(stack);
        stopPlaying();
        syncState();
    }

    public void setCardClientPreview(ItemStack stack) {
        this.card = stack;
        this.hasPaper = false;
        this.draftMessage = PunchCardItem.getMessage(stack);
    }

    public ItemStack takeItemForDrop() {
        stopPlaying();
        if (!card.isEmpty()) {
            ItemStack out = card;
            card = ItemStack.EMPTY;
            return out;
        }
        if (hasPaper) {
            hasPaper = false;
            return new ItemStack(Items.PAPER);
        }
        return ItemStack.EMPTY;
    }

    public void handleAction(ServerPlayer player, int action, String text) {
        if (level == null || level.isClientSide) return;
        if (!hasPaper && card.isEmpty()) {
            stopPlaying();
            return;
        }

        String cleanText = text == null ? "" : text.trim();
        if (cleanText.length() > 48) {
            cleanText = cleanText.substring(0, 48);
        }

        if (action == MorseTablePayload.ACTION_PUNCH) {
            if (isCardRecorded()) {
                return;
            }
            if (cleanText.isEmpty() || MorseCode.encode(cleanText).isEmpty()) {
                return;
            }
            if (!hasPaper && card.isEmpty()) {
                return;
            }
            this.hasPaper = false;
            this.draftMessage = cleanText;
            ItemStack newCard = new ItemStack(TItems.PUNCH_CARD.get());
            PunchCardItem.setMessage(newCard, cleanText);
            this.card = newCard;
            stopPlaying();
            level.playSound(null, worldPosition, SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.BLOCKS, 1.0f, 1.0f);
            syncState();
        } else if (action == MorseTablePayload.ACTION_PLAY_STOP) {
            if (playing) {
                stopPlaying();
            } else {
                if (isCardRecorded()) {
                    startPlaying();
                }
            }
        } else if (action == MorseTablePayload.ACTION_TAKE_CARD) {
            stopPlaying();
            if (!card.isEmpty()) {
                ItemStack out = card;
                card = ItemStack.EMPTY;
                draftMessage = "";
                if (!player.addItem(out)) {
                    Block.popResource(level, worldPosition, out);
                }
                level.playSound(null, worldPosition, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.8f, 1.2f);
                syncState();
            } else if (hasPaper) {
                hasPaper = false;
                draftMessage = "";
                ItemStack out = new ItemStack(Items.PAPER);
                if (!player.addItem(out)) {
                    Block.popResource(level, worldPosition, out);
                }
                level.playSound(null, worldPosition, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.8f, 1.2f);
                syncState();
            }
        }
    }

    public void togglePlay() {
        if (playing) {
            stopPlaying();
        } else if (isCardRecorded()) {
            startPlaying();
        }
    }

    public void startPlaying() {
        if (!isCardRecorded()) {
            return;
        }
        String msg = PunchCardItem.getMessage(card);
        if (msg.isEmpty()) {
            return;
        }
        this.timeline = MorseCode.toTimeline(msg);
        if (this.timeline.length == 0) {
            return;
        }
        this.playing = true;
        this.playTick = 0;
        this.toneOn = false;
        syncState();
    }

    public void stopPlaying() {
        if (!playing && !toneOn) return;
        this.playing = false;
        this.toneOn = false;
        this.playTick = 0;
        syncState();
    }

    private void syncState() {
        if (level == null || level.isClientSide) return;
        BlockState state = getBlockState();
        boolean hasAny = hasPaper || !card.isEmpty();
        BlockState updated = state;
        if (state.hasProperty(MorseTableBlock.HAS_CARD)) {
            updated = updated.setValue(MorseTableBlock.HAS_CARD, hasAny);
        }
        if (state.hasProperty(MorseTableBlock.PLAYING)) {
            updated = updated.setValue(MorseTableBlock.PLAYING, playing);
        }
        if (updated != state) {
            level.setBlock(worldPosition, updated, 3);
        }
        level.updateNeighborsAt(worldPosition, state.getBlock());
        setChanged();
        sendData();
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null) return;
        if (level.isClientSide) {
            if (playing && timeline.length > 0 && playTick < timeline.length) {
                playTick++;
            }
            return;
        }
        if (!playing) return;
        if (!isCardRecorded() || timeline.length == 0 || playTick >= timeline.length) {
            stopPlaying();
            return;
        }
        boolean current = timeline[playTick];
        boolean prev = playTick > 0 && timeline[playTick - 1];
        if (current != toneOn) {
            toneOn = current;
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }
        if (current) {
            RadioRegistry.notifyMorsePulseNear(level, worldPosition, 8.0);
            if (!prev || (playTick % 2 == 0)) {
                level.playSound(null, worldPosition, TSounds.MORSE_TONE.get(), SoundSource.BLOCKS, 0.85f, 1.0f);
            }
        }
        playTick++;
        if (playTick % 4 == 0 || playTick >= timeline.length) {
            sendData();
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putBoolean("HasPaper", hasPaper);
        tag.putString("Draft", draftMessage);
        tag.putBoolean("Playing", playing);
        tag.putInt("PlayTick", playTick);
        if (!card.isEmpty()) {
            tag.putBoolean("HasCardItem", true);
            tag.putString("CardMsg", PunchCardItem.getMessage(card));
        } else {
            tag.putBoolean("HasCardItem", false);
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        hasPaper = tag.getBoolean("HasPaper");
        draftMessage = tag.getString("Draft");
        playing = tag.getBoolean("Playing");
        playTick = tag.getInt("PlayTick");
        if (tag.getBoolean("HasCardItem")) {
            ItemStack st = new ItemStack(TItems.PUNCH_CARD.get());
            PunchCardItem.setMessage(st, tag.getString("CardMsg"));
            card = st;
        } else {
            card = ItemStack.EMPTY;
        }
        String msg = getMessage();
        this.timeline = msg.isEmpty() ? new boolean[0] : MorseCode.toTimeline(msg);
    }
}
