package ru.unrealsoftware.velocounter.gps;

import android.location.GnssStatus;
import android.location.GpsStatus;
import android.location.OnNmeaMessageListener;
import android.os.Build;

import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.N)
public class GPSSatelliteCounter extends GnssStatus.Callback {

    private final IGPSSatelliteCounterCallback counterCallback;

    public GPSSatelliteCounter(IGPSSatelliteCounterCallback counterCallback) {
        this.counterCallback = counterCallback;
    }

    @Override
    public void onSatelliteStatusChanged(GnssStatus status) {
        super.onSatelliteStatusChanged(status);
        counterCallback.setCount(status.getSatelliteCount());
    }
}
