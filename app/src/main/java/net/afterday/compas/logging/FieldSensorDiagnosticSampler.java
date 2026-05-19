package net.afterday.compas.logging;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import java.util.List;
import java.util.Locale;

/* JADX INFO: loaded from: classes.dex */
public final class FieldSensorDiagnosticSampler {
    private static final Object LOCK = new Object();
    private static final long TICK_INTERVAL_MS = 1000;
    private static boolean started = false;

    private FieldSensorDiagnosticSampler() {
    }

    public static void start(Context context) {
        if (context == null) {
            return;
        }
        synchronized (LOCK) {
            if (started) {
                return;
            }
            started = true;
        }
        new Sampler(context.getApplicationContext()).start();
    }

    private static final class Sampler implements SensorEventListener, LocationListener {
        private final Context context;
        private final HandlerThread thread;
        private final Handler handler;
        private final SensorManager sensorManager;
        private final LocationManager locationManager;
        private final float[] accel = new float[3];
        private final float[] gyro = new float[3];
        private final float[] magnetic = new float[3];
        private final float[] rotation = new float[5];
        private final float[] orientation = new float[3];
        private final float[] rotationMatrix = new float[9];
        private boolean hasAccel = false;
        private boolean hasGyro = false;
        private boolean hasMagnetic = false;
        private boolean hasRotation = false;
        private boolean hasPressure = false;
        private boolean hasLight = false;
        private boolean hasProximity = false;
        private boolean hasStepCounter = false;
        private float pressure = Float.NaN;
        private float light = Float.NaN;
        private float proximity = Float.NaN;
        private float stepCounter = Float.NaN;
        private Location lastLocation;
        private long lastLocationElapsedMs = 0;

        Sampler(Context context) {
            this.context = context;
            this.thread = new HandlerThread("field-sensor-diagnostics");
            this.thread.start();
            this.handler = new Handler(this.thread.getLooper());
            this.sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        }

        void start() {
            FieldDiagnosticLog.sensor("event=sampler_start intervalMs=" + TICK_INTERVAL_MS);
            registerSensors();
            registerLocationProviders();
            this.handler.post(this.tick);
        }

        private final Runnable tick = new Runnable() {
            @Override
            public void run() {
                logTick();
                handler.postDelayed(this, TICK_INTERVAL_MS);
            }
        };

        private void registerSensors() {
            if (this.sensorManager == null) {
                FieldDiagnosticLog.sensor("event=sensors_unavailable manager=false");
                return;
            }
            registerSensor(Sensor.TYPE_ACCELEROMETER, "accelerometer");
            registerSensor(Sensor.TYPE_GYROSCOPE, "gyroscope");
            registerSensor(Sensor.TYPE_MAGNETIC_FIELD, "magnetic");
            registerSensor(Sensor.TYPE_ROTATION_VECTOR, "rotation_vector");
            registerSensor(Sensor.TYPE_PRESSURE, "pressure");
            registerSensor(Sensor.TYPE_LIGHT, "light");
            registerSensor(Sensor.TYPE_PROXIMITY, "proximity");
            registerSensor(Sensor.TYPE_STEP_COUNTER, "step_counter");
            List<Sensor> sensors = this.sensorManager.getSensorList(Sensor.TYPE_ALL);
            FieldDiagnosticLog.sensor("event=sensor_inventory count=" + sensors.size() + " names=\"" + compactSensorList(sensors) + "\"");
        }

        private void registerSensor(int type, String name) {
            Sensor sensor = this.sensorManager.getDefaultSensor(type);
            if (sensor == null) {
                FieldDiagnosticLog.sensor("event=sensor_register name=" + name + " available=false");
                return;
            }
            try {
                boolean ok = this.sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME, this.handler);
                FieldDiagnosticLog.sensor("event=sensor_register name=" + name + " available=true registered=" + ok
                        + " vendor=\"" + sensor.getVendor() + "\" version=" + sensor.getVersion()
                        + " resolution=" + fmt(sensor.getResolution()) + " maxRange=" + fmt(sensor.getMaximumRange())
                        + " powerMa=" + fmt(sensor.getPower()));
            } catch (SecurityException e) {
                FieldDiagnosticLog.sensor("event=sensor_register name=" + name + " denied=true error=\"SecurityException\"");
            } catch (Exception e2) {
                FieldDiagnosticLog.sensor("event=sensor_register name=" + name + " error=\"" + e2.getClass().getSimpleName() + "\"");
            }
        }

        private void registerLocationProviders() {
            if (this.locationManager == null) {
                FieldDiagnosticLog.location("event=provider_register manager=false");
                return;
            }
            boolean permission = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) || hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
            FieldDiagnosticLog.location("event=provider_status permission=" + permission
                    + " gpsEnabled=" + isProviderEnabled(LocationManager.GPS_PROVIDER)
                    + " networkEnabled=" + isProviderEnabled(LocationManager.NETWORK_PROVIDER));
            if (!permission) {
                return;
            }
            requestProvider(LocationManager.GPS_PROVIDER);
            requestProvider(LocationManager.NETWORK_PROVIDER);
            logLastKnown(LocationManager.GPS_PROVIDER);
            logLastKnown(LocationManager.NETWORK_PROVIDER);
        }

        private void requestProvider(String provider) {
            if (!isProviderEnabled(provider)) {
                FieldDiagnosticLog.location("event=provider_register provider=" + provider + " enabled=false");
                return;
            }
            try {
                this.locationManager.requestLocationUpdates(provider, 1000L, 0.0f, this, this.thread.getLooper());
                FieldDiagnosticLog.location("event=provider_register provider=" + provider + " enabled=true registered=true minTimeMs=1000 minDistanceM=0");
            } catch (SecurityException e) {
                FieldDiagnosticLog.location("event=provider_register provider=" + provider + " denied=true error=\"SecurityException\"");
            } catch (Exception e2) {
                FieldDiagnosticLog.location("event=provider_register provider=" + provider + " error=\"" + e2.getClass().getSimpleName() + "\"");
            }
        }

        private void logLastKnown(String provider) {
            try {
                Location location = this.locationManager.getLastKnownLocation(provider);
                if (location != null) {
                    FieldDiagnosticLog.location(formatLocation("last_known", location));
                }
            } catch (SecurityException e) {
                FieldDiagnosticLog.location("event=last_known provider=" + provider + " denied=true error=\"SecurityException\"");
            } catch (Exception e2) {
                FieldDiagnosticLog.location("event=last_known provider=" + provider + " error=\"" + e2.getClass().getSimpleName() + "\"");
            }
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event == null || event.sensor == null) {
                return;
            }
            int type = event.sensor.getType();
            if (type == Sensor.TYPE_ACCELEROMETER) {
                copy(event.values, this.accel);
                this.hasAccel = true;
            } else if (type == Sensor.TYPE_GYROSCOPE) {
                copy(event.values, this.gyro);
                this.hasGyro = true;
            } else if (type == Sensor.TYPE_MAGNETIC_FIELD) {
                copy(event.values, this.magnetic);
                this.hasMagnetic = true;
            } else if (type == Sensor.TYPE_ROTATION_VECTOR) {
                copy(event.values, this.rotation);
                this.hasRotation = true;
            } else if (type == Sensor.TYPE_PRESSURE) {
                this.pressure = event.values[0];
                this.hasPressure = true;
            } else if (type == Sensor.TYPE_LIGHT) {
                this.light = event.values[0];
                this.hasLight = true;
            } else if (type == Sensor.TYPE_PROXIMITY) {
                this.proximity = event.values[0];
                this.hasProximity = true;
            } else if (type == Sensor.TYPE_STEP_COUNTER) {
                this.stepCounter = event.values[0];
                this.hasStepCounter = true;
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            if (sensor != null) {
                FieldDiagnosticLog.sensor("event=accuracy name=\"" + sensor.getName() + "\" type=" + sensor.getType() + " accuracy=" + accuracy);
            }
        }

        @Override
        public void onLocationChanged(Location location) {
            this.lastLocation = location;
            this.lastLocationElapsedMs = SystemClock.elapsedRealtime();
            FieldDiagnosticLog.location(formatLocation("update", location));
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            FieldDiagnosticLog.location("event=provider_status_changed provider=" + provider + " status=" + status);
        }

        @Override
        public void onProviderEnabled(String provider) {
            FieldDiagnosticLog.location("event=provider_enabled provider=" + provider);
        }

        @Override
        public void onProviderDisabled(String provider) {
            FieldDiagnosticLog.location("event=provider_disabled provider=" + provider);
        }

        private void logTick() {
            float yaw = Float.NaN;
            float pitch = Float.NaN;
            float roll = Float.NaN;
            if (this.hasRotation) {
                SensorManager.getRotationMatrixFromVector(this.rotationMatrix, this.rotation);
                SensorManager.getOrientation(this.rotationMatrix, this.orientation);
                yaw = degrees(this.orientation[0]);
                pitch = degrees(this.orientation[1]);
                roll = degrees(this.orientation[2]);
            } else if (this.hasAccel && this.hasMagnetic && SensorManager.getRotationMatrix(this.rotationMatrix, null, this.accel, this.magnetic)) {
                SensorManager.getOrientation(this.rotationMatrix, this.orientation);
                yaw = degrees(this.orientation[0]);
                pitch = degrees(this.orientation[1]);
                roll = degrees(this.orientation[2]);
            }
            long locationAgeMs = this.lastLocationElapsedMs > 0 ? SystemClock.elapsedRealtime() - this.lastLocationElapsedMs : -1;
            FieldDiagnosticLog.sensor("event=tick intervalMs=" + TICK_INTERVAL_MS
                    + " accel=" + vector(this.accel, this.hasAccel)
                    + " gyro=" + vector(this.gyro, this.hasGyro)
                    + " magnetic=" + vector(this.magnetic, this.hasMagnetic)
                    + " yawDeg=" + fmt(yaw)
                    + " pitchDeg=" + fmt(pitch)
                    + " rollDeg=" + fmt(roll)
                    + " pressureHpa=" + value(this.pressure, this.hasPressure)
                    + " lightLux=" + value(this.light, this.hasLight)
                    + " proximityCm=" + value(this.proximity, this.hasProximity)
                    + " stepCounter=" + value(this.stepCounter, this.hasStepCounter)
                    + " locationAgeMs=" + locationAgeMs);
        }

        private String formatLocation(String event, Location location) {
            if (location == null) {
                return "event=" + event + " null=true";
            }
            Bundle extras = location.getExtras();
            int satellites = extras != null ? extras.getInt("satellites", -1) : -1;
            return "event=" + event
                    + " provider=" + location.getProvider()
                    + " lat=" + fmt(location.getLatitude())
                    + " lon=" + fmt(location.getLongitude())
                    + " accuracyM=" + (location.hasAccuracy() ? fmt(location.getAccuracy()) : "na")
                    + " altitudeM=" + (location.hasAltitude() ? fmt(location.getAltitude()) : "na")
                    + " speedMps=" + (location.hasSpeed() ? fmt(location.getSpeed()) : "na")
                    + " bearingDeg=" + (location.hasBearing() ? fmt(location.getBearing()) : "na")
                    + " timeMs=" + location.getTime()
                    + " elapsedRealtimeNanos=" + (Build.VERSION.SDK_INT >= 17 ? location.getElapsedRealtimeNanos() : -1)
                    + " satellites=" + satellites;
        }

        private boolean isProviderEnabled(String provider) {
            try {
                return this.locationManager != null && this.locationManager.isProviderEnabled(provider);
            } catch (Exception e) {
                return false;
            }
        }

        private boolean hasPermission(String permission) {
            return Build.VERSION.SDK_INT < 23 || this.context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
        }

        private static void copy(float[] from, float[] to) {
            int length = Math.min(from.length, to.length);
            for (int i = 0; i < length; i++) {
                to[i] = from[i];
            }
        }

        private static float degrees(float radians) {
            float degrees = (float) Math.toDegrees(radians);
            if (degrees < 0.0f) {
                degrees += 360.0f;
            }
            return degrees;
        }

        private static String vector(float[] values, boolean available) {
            if (!available) {
                return "na";
            }
            return fmt(values[0]) + "," + fmt(values[1]) + "," + fmt(values[2]);
        }

        private static String value(float value, boolean available) {
            return available ? fmt(value) : "na";
        }

        private static String fmt(double value) {
            if (Double.isNaN(value) || Double.isInfinite(value)) {
                return "na";
            }
            return String.format(Locale.US, "%.3f", Double.valueOf(value));
        }

        private static String compactSensorList(List<Sensor> sensors) {
            StringBuilder builder = new StringBuilder();
            int limit = Math.min(sensors.size(), 24);
            for (int i = 0; i < limit; i++) {
                if (i > 0) {
                    builder.append("; ");
                }
                Sensor sensor = sensors.get(i);
                builder.append(sensor.getType()).append(":").append(sensor.getName());
            }
            if (sensors.size() > limit) {
                builder.append("; ...");
            }
            return builder.toString().replace('\n', ' ').replace('\r', ' ');
        }
    }
}
