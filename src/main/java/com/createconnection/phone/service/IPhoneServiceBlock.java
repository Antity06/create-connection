package com.createconnection.phone.service;

import com.createconnection.phone.TelephoneBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public interface IPhoneServiceBlock {
    Component onCall(ServerPlayer caller, TelephoneBlockEntity callerPhone, TelephoneBlockEntity servicePhone);
}
