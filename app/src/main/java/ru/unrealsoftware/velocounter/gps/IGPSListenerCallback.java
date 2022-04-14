package ru.unrealsoftware.velocounter.gps;

import android.location.Location;

public interface IGPSListenerCallback {

    void onLocationChanged(Location location);
    void onProviderStatusChanged(boolean enabled);
}
