package com.createconnection.phone;

public enum PhoneKind {
    HOME(true, true),
    PAYPHONE(true, false),
    SERVICE(false, false);

    public final boolean canDial;
    public final boolean hasSpeaker;

    PhoneKind(boolean canDial, boolean hasSpeaker) {
        this.canDial = canDial;
        this.hasSpeaker = hasSpeaker;
    }
}
