package ru.unrealsoftware.velocounter;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import ru.unrealsoftware.velocounter.gps.GPSSatelliteCounter;
import ru.unrealsoftware.velocounter.gps.IGPSSatelliteCounterCallback;
import ru.unrealsoftware.velocounter.gps.INmeaListenerCallback;
import ru.unrealsoftware.velocounter.gps.NmeaListener;
import ru.unrealsoftware.velocounter.speed.MedianGPSSpeedCounter;

public class MainActivity extends AppCompatActivity {

    private static final int requestGpsPermissionsCallback = 4242;

    private LocationManager locationManager;
    private TextView sensorTick;
    private TextView speedLabel;
    private TextView speedShadowLabel;
    private TextView distanceLabel;
    private TextView maxLabel;
    private TextView avsLabel;
    private TextView tmLabel;
    private TextView satelitesCount;
    private TextView instantSpeedLabel;

    private ImageView gpsImage;
    private ImageView speedImage;

    private int ticksSensor;
    private boolean firstTick;

    private long lastTick;
    private double maxSpeed;
    private double totalDistance;
    private long lastSensor;
    private long lastSpeed;
    private long tripTime;

    private long tripTimeBegin;

    private int setupMode;
    private boolean dstIsBlinking;
    private boolean blinkState;
    private boolean mxsIsBlinking;

    private GPSSatelliteCounter gpsSatelliteCounter;
    private NmeaListener nmeaListener;
    private MedianGPSSpeedCounter medianGPSSpeedCounter;

    private static Location lastLocation;

    private static final String PREFS_FILE = "last_state";
    private SharedPreferences settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        settings = getSharedPreferences(PREFS_FILE, MODE_PRIVATE);

        Typeface face = Typeface.createFromAsset(getAssets(), "17372.otf");

        lastTick = new Date().getTime();

        sensorTick = findViewById(R.id.sensorTick);
        sensorTick.setTypeface(face);

        speedLabel = findViewById(R.id.speedLabel);
        speedLabel.setTypeface(face);

        speedShadowLabel = findViewById(R.id.speedShadowLabel);
        speedShadowLabel.setTypeface(face);

        distanceLabel = findViewById(R.id.distanceLabel);
        distanceLabel.setTypeface(face);

        maxLabel = findViewById(R.id.maxLabel);
        maxLabel.setTypeface(face);

        avsLabel = findViewById(R.id.avsLabel);
        avsLabel.setTypeface(face);

        tmLabel = findViewById(R.id.tmLabel);
        tmLabel.setTypeface(face);

        gpsImage = findViewById(R.id.imgGps);
        gpsImage.setVisibility(View.INVISIBLE);

        speedImage = findViewById(R.id.imgSpeed);
        speedImage.setVisibility(View.INVISIBLE);

        satelitesCount = findViewById(R.id.satelitesCount);
        satelitesCount.setTypeface(face);

        instantSpeedLabel = findViewById(R.id.instantSpeedLabel);

        totalDistance = settings.getFloat("dist",0);

        setupMode = 0;
        tripTime = 0;
        firstTick = true;
        startGpsTracker();

        Timer mTimer = new Timer();
        MyTimerTask mMyTimerTask = new MyTimerTask();

        medianGPSSpeedCounter = new MedianGPSSpeedCounter(4);

        mTimer.schedule(mMyTimerTask, 1000, 1000);
    }

    @Override
    public void onResume() {
        super.onResume();

        sensorTick = findViewById(R.id.sensorTick);
        speedLabel = findViewById(R.id.speedLabel);
        distanceLabel = findViewById(R.id.distanceLabel);
        maxLabel = findViewById(R.id.maxLabel);
        avsLabel = findViewById(R.id.avsLabel);
        tmLabel = findViewById(R.id.tmLabel);
    }

    @Override
    public void onDestroy() {

        writeState();
        super.onDestroy();
    }

    private void writeState() {

        SharedPreferences.Editor prefEditor = settings.edit();
        prefEditor.putFloat("dist", (float) totalDistance).apply();
    }

    private String formatDecimalValue(BigDecimal value, int scale) {

        if (value == null) return "  . ";
        if (value.compareTo(BigDecimal.TEN) < 0) {
            return " " + value.setScale(scale, RoundingMode.HALF_UP).toString();
        } else {
            return value.setScale(scale, RoundingMode.HALF_UP).toString();
        }
    }

    private String formatIntValue(long value) {

        return value < 10 ? "0" + value : Long.toString(value);
    }

    @SuppressLint("DefaultLocale")
    private String formatTimeValue(long millis) {

        String value = "";
        long h = millis / 1000 / 3600;
        long m = (millis - (h * 3600000)) / 60000;
        long s = (millis - (m * 60000) - (h * 3600000)) / 1000;

        value = String.format("%d:%s:%s", h, formatIntValue(m), formatIntValue(s));
        return value;
    }

    @SuppressLint("SetTextI18n")
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            //Обрабатываем нажатие кнопки увеличения громкости:
            case KeyEvent.KEYCODE_VOLUME_UP:

                setupMode++;
                if (setupMode > 5) setupMode = 0;
                switch (setupMode) {

                    case 0:
                        dstIsBlinking = false;
                        mxsIsBlinking = false;
                        break;

                    case 1:
                        dstIsBlinking = true;
                        mxsIsBlinking = false;
                        break;

                    case 2:
                        dstIsBlinking = false;
                        mxsIsBlinking = true;
                        break;

                    default:
                        dstIsBlinking = false;
                        mxsIsBlinking = false;
                        break;
                }

                return true;

            case KeyEvent.KEYCODE_VOLUME_DOWN:

                if (setupMode != 0) {

                    if (dstIsBlinking) {
                        totalDistance = 0;
                        if (distanceLabel != null) {
                            distanceLabel.setText("0");
                        }
                    }
                    if (mxsIsBlinking) {
                        maxSpeed = 0;
                        if (maxLabel != null) {
                            maxLabel.setText("0.0");
                        }
                    }
                }

                return true;

            default:
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void startGpsTracker() {
        // Использование GPS
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (locationManager != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                    PackageManager.PERMISSION_GRANTED) {
                return;
            }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    800, 20, locationListener);

            locationManager.addNmeaListener((GpsStatus.NmeaListener) (timestamp, nmea) -> {

                if (!TextUtils.isEmpty(nmea)) {
                    ///parseNMEA(nmea);
                }
            });
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

            nmeaListener = new NmeaListener(new INmeaListenerCallback() {
                @Override
                public void onNmeaMessage(String message, long timestamp) {

                    if (!TextUtils.isEmpty(message)) {
                        ///parseNMEA(nmea);
                    }
                }
            });

            gpsSatelliteCounter = new GPSSatelliteCounter(new IGPSSatelliteCounterCallback() {
                @SuppressLint("SetTextI18n")
                @Override
                public void setCount(int count) {
                    satelitesCount.setText(Integer.toString(count));
                }
            });
        } else {
            satelitesCount.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        requestPermissions();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        boolean success = true;
        for (int grantResult : grantResults) {
            if (grantResult != PackageManager.PERMISSION_GRANTED) {
                success = false;
                break;
            }
        }
        if (success)
            requestPermissions();
        else
            showAlertAboutPermissions();
    }

    private void requestPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates("gps", 1000, 0, locationListener);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                locationManager.registerGnssStatusCallback(gpsSatelliteCounter);
            }
        } else ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION
        }, requestGpsPermissionsCallback);
    }

    private void showAlertAboutPermissions() {
        new AlertDialog.Builder(this)
                .setTitle("Нет разрешений")
                .setMessage("Приложению требуется разрешение.")
                .setPositiveButton("Разрешить", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        requestPermissions();
                    }
                })
                .setNegativeButton("Запретить", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        finish();
                    }
                }).show();
    }

    private void resetScreen() {

        if (speedLabel != null) {
            speedLabel.setText(" 0.0");
        }
        if (sensorTick != null) {
            ///sensorTick.setText(Integer.toString(ticksSensor));
        }
        if (maxLabel != null) {
            maxLabel.setText("0.0");
        }
        if (distanceLabel != null) {
            distanceLabel.setText("0");
        }
        if (avsLabel != null) {
            avsLabel.setText("0.0");
        }
        if (tmLabel != null) {
            tmLabel.setText("0:00:00");
        }
    }

    @SuppressLint("SetTextI18n")
    private void calcParameters(double distance, long timeGap) {

        double speed = 0;
        if (!(distance == 0 || timeGap == 0)) {
            speed = (distance / timeGap) * 3600;
        }

        if (speed > 1 && speed < 100) {

            lastSpeed = new Date().getTime();
            if (speedImage != null) {
                speedImage.setVisibility(View.VISIBLE);
            }
            if (speedLabel != null) {
                instantSpeedLabel.setText(formatDecimalValue(BigDecimal.valueOf(speed), 1));
                double spMedian = medianGPSSpeedCounter.getSpeed(speed);
                speedLabel.setText(formatDecimalValue(BigDecimal.valueOf(spMedian), 1));
            }
            if (sensorTick != null) {
                ///sensorTick.setText(Integer.toString(ticksSensor));
            }
            if (maxSpeed < speed) {
                maxSpeed = speed;
                maxLabel.setText(formatDecimalValue(BigDecimal.valueOf(maxSpeed), 1));
            }
            if (distanceLabel != null) {
                distanceLabel.setText(formatDecimalValue(BigDecimal.valueOf(totalDistance / 1000), 2));
            }
            if (avsLabel != null) {

            }
            if (tmLabel != null) {
                tmLabel.setText(formatTimeValue(new Date().getTime() - tripTimeBegin));
            }
        }
    }

    static double haversine(double lat1, double lon1, double lat2, double lon2) {

        // расстояние между широтой и долготой
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        // преобразовать в радианы
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);

        // применить формулы
        double a = Math.pow(Math.sin(dLat / 2), 2) + Math.pow(Math.sin(dLon / 2), 2) *
                Math.cos(lat1) * Math.cos(lat2);

        double rad = 6371210;
        double c = 2 * Math.asin(Math.sqrt(a));
        return rad * c;
    }

    LocationListener locationListener = new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {

            if (location != null) {
                ticksSensor++;
                lastSensor = new Date().getTime();
                gpsImage.setVisibility(View.VISIBLE);

                if (firstTick) {
                    tripTimeBegin = new Date().getTime();
                    resetScreen();
                }
                if (lastLocation != null) {
                    double distance = haversine(location.getLatitude(), location.getLongitude(),
                            lastLocation.getLatitude(), lastLocation.getLongitude());

                    totalDistance += distance;

                    long timeGap = location.getTime() - lastTick;

                    calcParameters(distance, timeGap);
                }

                if (sensorTick != null) {
                    sensorTick.setText(Integer.toString(ticksSensor));
                }

                lastLocation = new Location("last");
                lastLocation.setLongitude(location.getLongitude());
                lastLocation.setLatitude(location.getLatitude());

                lastTick = location.getTime();
                firstTick = false;
            }
        }

        /**
         * @param s
         * @param i
         * @param bundle
         * @deprecated
         */
        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }

        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {

        }
    };

    class MyTimerTask extends TimerTask {

        @Override
        public void run() {

            runOnUiThread(new Runnable() {

                @Override
                public void run() {

                    if (setupMode != 0) {

                        blinkState = !blinkState;
                        if (dstIsBlinking) {
                            if (blinkState) {
                                distanceLabel.setVisibility(View.VISIBLE);
                            } else {
                                distanceLabel.setVisibility(View.INVISIBLE);
                            }
                        } else {
                            distanceLabel.setVisibility(View.VISIBLE);
                        }
                        if (mxsIsBlinking) {
                            if (blinkState) {
                                maxLabel.setVisibility(View.VISIBLE);
                            } else {
                                maxLabel.setVisibility(View.INVISIBLE);
                            }
                        } else {
                            maxLabel.setVisibility(View.VISIBLE);
                        }
                    }

                    boolean stateSaveTime = new Date().getTime() - lastSpeed > 10000;
                    if (stateSaveTime) {

                        writeState();
                    }

                    boolean hideSpeed = new Date().getTime() - lastSpeed > 3000;
                    if (hideSpeed) {

                        if (speedImage != null) {
                            speedImage.setVisibility(View.INVISIBLE);
                        }

                        if (speedLabel != null) {
                            instantSpeedLabel.setText(formatDecimalValue(BigDecimal.valueOf(0), 1));
                            speedLabel.setText(formatDecimalValue(BigDecimal.valueOf(0), 1));
                        }
                    }

                    boolean enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
                    if (!enabled) {
                        if (gpsImage != null) {
                            gpsImage.setVisibility(View.INVISIBLE);
                        }
                    }
                }
            });
        }
    }
}