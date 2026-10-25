package com.createconnection.phone;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public class PhoneDirectory extends SavedData {
    public static final Pattern NUMBER = Pattern.compile("\d{5}");
    private static final String NAME = "create_connection_phone_directory";
    private final Map<String, BlockPos> entries = new HashMap<>();

    public static PhoneDirectory get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
            new SavedData.Factory<>(PhoneDirectory::new, PhoneDirectory::load, null),
            NAME
        );
    }

    private static String key(ServerLevel level, String number) {
        return level.dimension().location() + "#" + number;
    }

    public BlockPos lookup(ServerLevel level, String number) {
        return this.entries.get(key(level, number));
    }

    public boolean claim(ServerLevel level, String number, BlockPos pos) {
        if (!NUMBER.matcher(number).matches()) {
            return false;
        }
        String k = key(level, number);
        BlockPos cur = this.entries.get(k);
        if (cur != null && !cur.equals(pos) && isStillValid(level, cur, number)) {
            return false;
        }
        this.entries.put(k, pos.immutable());
        setDirty();
        return true;
    }

    public void release(ServerLevel level, String number, BlockPos pos) {
        if (number == null || number.isEmpty()) return;
        String k = key(level, number);
        if (pos.equals(this.entries.get(k))) {
            this.entries.remove(k);
            setDirty();
        }
    }

    public void forceRelease(ServerLevel level, String number) {
        if (this.entries.remove(key(level, number)) != null) {
            setDirty();
        }
    }

    private static boolean isStillValid(ServerLevel level, BlockPos pos, String number) {
        if (!level.isLoaded(pos)) return true;
        return level.getBlockEntity(pos) instanceof TelephoneBlockEntity be && number.equals(be.getNumber());
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        this.entries.forEach((k, p) -> {
            CompoundTag e = new CompoundTag();
            e.putString("Key", k);
            e.putLong("Pos", p.asLong());
            list.add(e);
        });
        tag.put("Entries", list);
        return tag;
    }

    private static PhoneDirectory load(CompoundTag tag, HolderLookup.Provider registries) {
        PhoneDirectory d = new PhoneDirectory();
        for (Tag t : tag.getList("Entries", 10)) {
            CompoundTag e = (CompoundTag) t;
            d.entries.put(e.getString("Key"), BlockPos.of(e.getLong("Pos")));
        }
        return d;
    }
}
