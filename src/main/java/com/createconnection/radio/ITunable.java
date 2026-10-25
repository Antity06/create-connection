package com.createconnection.radio;

public interface ITunable {
    int MAX_FREQUENCY = 15;

    int getFrequency();

    void configure(int frequency, boolean transmit);

    default float getTune() {
        return (float) getFrequency();
    }

    default void configureTune(float tune, boolean transmit) {
        configure(Math.round(tune), transmit);
    }
}
