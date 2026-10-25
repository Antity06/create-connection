package com.createconnection.client;

import com.createconnection.network.RemoteTickerPayload;
import com.simibubi.create.content.logistics.BigItemStack;
import java.util.List;

public final class ClientRemoteStock {
    public static volatile boolean clientMenuIsRemote = false;
    private static volatile List<List<BigItemStack>> categories = List.of();

    private ClientRemoteStock() {}

    public static void onPayload(RemoteTickerPayload payload) {
        categories = payload.categories();
    }

    public static List<List<BigItemStack>> categories() {
        return categories;
    }

    public static void clear() {
        clientMenuIsRemote = false;
        categories = List.of();
    }
}
