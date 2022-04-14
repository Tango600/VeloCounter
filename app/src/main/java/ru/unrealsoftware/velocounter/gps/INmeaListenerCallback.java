package ru.unrealsoftware.velocounter.gps;

public interface INmeaListenerCallback {
    void onNmeaMessage(String message, long timestamp);
}
