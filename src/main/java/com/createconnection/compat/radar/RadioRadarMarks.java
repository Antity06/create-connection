package com.createconnection.compat.radar;

import com.createconnection.radio.RadioBlockEntity;
import com.createconnection.radio.RadioRegistry;
import com.createconnection.radio.RadioStationBlockEntity;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public final class RadioRadarMarks {
    public static final String RECEIVER = "create_connection:radio_receiver";
    public static final String TRANSMITTER = "create_connection:radio_transmitter";
    public static final int RECEIVER_COLOR = 16747546;
    public static final int TRANSMITTER_COLOR = 9132587;

    private RadioRadarMarks() {}

    public static void forEach(Level level, BiConsumer<BlockPos, Boolean> consumer) {
        for (RadioBlockEntity r : RadioRegistry.radios()) {
            if (!r.isRemoved() && r.getLevel() == level) {
                consumer.accept(r.getBlockPos(), r.isTransmitter());
            }
        }
        for (RadioStationBlockEntity st : RadioRegistry.stations()) {
            if (!st.isRemoved() && st.getLevel() == level) {
                consumer.accept(st.getBlockPos(), true);
            }
        }
    }

    public static int colorOf(String entityType) {
        if (RECEIVER.equals(entityType)) {
            return RECEIVER_COLOR;
        }
        if (TRANSMITTER.equals(entityType)) {
            return TRANSMITTER_COLOR;
        }
        return -1;
    }
}
