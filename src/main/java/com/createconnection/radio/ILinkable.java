package com.createconnection.radio;

import net.minecraft.nbt.CompoundTag;

public interface ILinkable {
    RadioLink getLink();

    void setLink(RadioLink link);

    static void writeLink(CompoundTag tag, RadioLink link) {
        if (link != null) {
            tag.putUUID("Network", link.networkId());
            tag.putString("NetworkName", link.name());
        }
    }

    static RadioLink readLink(CompoundTag tag) {
        return tag.hasUUID("Network")
            ? new RadioLink(tag.getUUID("Network"), tag.getString("NetworkName"))
            : null;
    }
}
