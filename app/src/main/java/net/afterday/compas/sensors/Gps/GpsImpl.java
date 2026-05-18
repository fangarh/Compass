package net.afterday.compas.sensors.Gps;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import java.util.concurrent.TimeUnit;

/* JADX INFO: loaded from: classes.dex */
public class GpsImpl implements Gps {
    private static final String TAG = "GpsImpl";
    private Context context;
    private Disposable d;
    private LocationManager locationManager;
    private Subject<Integer> satelitesCount = BehaviorSubject.createDefault(0);
    private LocationListener locationListener = new LocationListenerImpl(this, null);

    static /* synthetic */ Subject access$100(GpsImpl x0) {
        return x0.satelitesCount;
    }

    public GpsImpl(Context context) {
        this.context = context;
    }

    @Override // net.afterday.compas.sensors.Sensor
    public void start() {
        if (this.locationManager == null) {
            this.locationManager = (LocationManager) this.context.getSystemService("location");
            this.d = Observable.timer(0L, TimeUnit.SECONDS).take(1L).observeOn(AndroidSchedulers.mainThread()).subscribe(new $$Lambda$GpsImpl$24F3UnXtCcWIOJB19dJbGDEIjAA(this));
        }
    }

    public /* synthetic */ void lambda$start$0$GpsImpl(Long t) {
        this.locationManager.requestLocationUpdates("gps", 0L, 0.0f, this.locationListener);
    }

    @Override // net.afterday.compas.sensors.Sensor
    public void stop() {
        Disposable disposable = this.d;
        if (disposable != null && !disposable.isDisposed()) {
            this.d.dispose();
        }
        LocationManager locationManager = this.locationManager;
        if (locationManager != null) {
            locationManager.removeUpdates(this.locationListener);
            this.locationManager = null;
        }
        this.satelitesCount.onNext(0);
    }

    @Override // net.afterday.compas.sensors.Sensor
    public Observable<Integer> getSensorResultsStream() {
        return this.satelitesCount;
    }

    private class LocationListenerImpl implements LocationListener {
        private LocationListenerImpl() {
        }

        /* synthetic */ LocationListenerImpl(GpsImpl x0, Object x1) {
            this();
        }

        @Override // android.location.LocationListener
        public void onLocationChanged(Location location) {
            Bundle b = location.getExtras();
            int satellites = b.getInt("satellites");
            Log.e(GpsImpl.TAG, "onLocationChanged: " + satellites);
            GpsImpl.access$100(GpsImpl.this).onNext(Integer.valueOf(satellites));
        }

        @Override // android.location.LocationListener
        public void onStatusChanged(String s, int i, Bundle bundle) {
            Log.e(GpsImpl.TAG, "onStatusChanged: " + s + " Bundle: " + bundle);
        }

        @Override // android.location.LocationListener
        public void onProviderEnabled(String s) {
        }

        @Override // android.location.LocationListener
        public void onProviderDisabled(String s) {
        }
    }
}
