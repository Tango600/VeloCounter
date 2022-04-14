package ru.unrealsoftware.velocounter.gps;

import android.location.OnNmeaMessageListener;
import android.os.Build;

import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.N)
public class NmeaListener implements OnNmeaMessageListener {

    private final INmeaListenerCallback callback;

    public NmeaListener(INmeaListenerCallback callback) {
        this.callback = callback;
    }

    @Override
    public void onNmeaMessage(String message, long timestamp) {
        this.callback.onNmeaMessage(message, timestamp);
    }
}
