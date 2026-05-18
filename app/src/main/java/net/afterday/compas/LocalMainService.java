package net.afterday.compas;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.widget.RemoteViews;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import java.util.List;
import net.afterday.compas.core.Game;
import net.afterday.compas.core.gameState.Frame;
import net.afterday.compas.core.inventory.items.Events.ItemAdded;
import net.afterday.compas.core.player.Player;
import net.afterday.compas.core.serialization.Serializer;
import net.afterday.compas.core.userActions.UserActionsPack;
import net.afterday.compas.db.DataBase;
import net.afterday.compas.devices.DeviceProvider;
import net.afterday.compas.devices.DeviceProviderImpl;
import net.afterday.compas.engine.Engine;
import net.afterday.compas.engine.events.ItemEventsBus;
import net.afterday.compas.logging.LogLine;
import net.afterday.compas.logging.Logger;
import net.afterday.compas.persistency.PersistencyProviderImpl;
import net.afterday.compas.sensors.Battery.Battery;
import net.afterday.compas.sensors.Battery.BatteryStatus;
import net.afterday.compas.sensors.SensorsProviderImpl;
import net.afterday.compas.serialization.SharedPrefsSerializer;
import net.afterday.compas.util.Fonts;

/* JADX INFO: loaded from: classes.dex */
public class LocalMainService extends Service {
    private static final String CHANNEL_ID = "compass_foreground";
    private static final String TAG = "LocalMainService";
    private static LocalMainService instance;
    private Battery battery;
    private Observable<BatteryStatus> batteryStatusStream;
    private DataBase dataBase;
    private Engine engine;
    private Fonts fonts;
    private Observable<Frame> framesStream;
    private Game game;
    private Logger logger;
    private Serializer serializer;
    private IBinder binder = new MainBinder();
    private boolean running = false;
    private Observable<UserActionsPack> userActionsStream = PublishSubject.create();

    @Override // android.app.Service
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    @Override // android.app.Service
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if (!this.running) {
            Log.e(TAG, "----------------------------------------------------------------");
            startForeground();
            initGame();
        }
        this.running = true;
        return 1;
    }

    @Override // android.app.Service
    public void onDestroy() {
        super.onDestroy();
    }

    @Override // android.app.Service
    @Nullable
    public IBinder onBind(Intent intent) {
        return this.binder;
    }

    @Override // android.app.Service
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    private void startForeground() {
        ensureNotificationChannel();
        Intent resultIntent = new Intent(this, (Class<?>) MainActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntentWithParentStack(resultIntent);
        PendingIntent pIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        RemoteViews collapsed = new RemoteViews(getPackageName(), R.layout.notification);
        Intent intentAction = new Intent(this, (Class<?>) ActionsReceiver.class);
        intentAction.putExtra("ServiceControlls", "STOP");
        PendingIntent stopServiceIntent = PendingIntent.getBroadcast(this, 1, intentAction, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        collapsed.setOnClickPendingIntent(R.id.open, pIntent);
        collapsed.setOnClickPendingIntent(R.id.stop, stopServiceIntent);
        Notification n = new NotificationCompat.Builder(this, CHANNEL_ID).setContent(collapsed).setSmallIcon(R.mipmap.ic_launcher).build();
        startForeground(1, n);
    }

    private void ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < 26) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Compass service", NotificationManager.IMPORTANCE_LOW);
        NotificationManager notificationManager = (NotificationManager) getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void initGame() {
        DeviceProvider deviceProvider = new DeviceProviderImpl(this);
        this.serializer = SharedPrefsSerializer.instance(this);
        this.dataBase = DataBase.instance(this);
        this.logger = Logger.instance(this, deviceProvider.getVibrator());
        this.engine = Engine.instance();
        this.engine.setPersistencyProvider(new PersistencyProviderImpl());
        this.engine.setSensorsProvider(SensorsProviderImpl.initialize(this));
        this.engine.setDeviceProvider(deviceProvider);
        this.battery = SensorsProviderImpl.instance().getBatterySensor();
        this.batteryStatusStream = this.battery.getSensorResultsStream();
        this.battery.start();
        this.framesStream = this.engine.getFramesStream();
        this.engine.start();
    }

    public class MainBinder extends Binder {
        public MainBinder() {
        }

        public LocalMainService getService() {
            return LocalMainService.this;
        }
    }

    public Observable<Frame> getFramesStream() {
        return this.framesStream;
    }

    public Observable<Long> getCountDownStream() {
        return this.engine.getCountDownStream();
    }

    public Observable<Integer> getPlayerLevelStream() {
        return this.engine.getPlayerLevelStream();
    }

    public Observable<Player.STATE> getPlayerStateStream() {
        return this.engine.getPlayerStateStream();
    }

    public Observable<ItemAdded> getItemAddedStream() {
        return this.engine.getItemAddedStream();
    }

    public Observable<List<LogLine>> getLogStream() {
        return this.logger.getLogStream();
    }

    public Observable<BatteryStatus> getBatteryStatusStream() {
        return this.batteryStatusStream;
    }

    public ItemEventsBus getItemEventBus() {
        return this.engine.getItemEventsBus();
    }

    public static LocalMainService getInstance() {
        return instance;
    }
}
