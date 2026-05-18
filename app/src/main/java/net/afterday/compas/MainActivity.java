package net.afterday.compas;

import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Process;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import java.util.ArrayList;
import java.util.List;
import net.afterday.compas.LocalMainService;
import net.afterday.compas.core.gameState.Frame;
import net.afterday.compas.core.inventory.Inventory;
import net.afterday.compas.core.inventory.items.Events.ItemAdded;
import net.afterday.compas.core.inventory.items.Item;
import net.afterday.compas.core.player.Player;
import net.afterday.compas.core.player.PlayerProps;
import net.afterday.compas.engine.events.EmissionEventBus;
import net.afterday.compas.engine.events.ItemEventsBus;
import net.afterday.compas.engine.events.PlayerEventBus;
import net.afterday.compas.fragment.BloodFragment;
import net.afterday.compas.fragment.InventoryFragment;
import net.afterday.compas.fragment.ItemInfoFragment;
import net.afterday.compas.fragment.ScannerFragment;
import net.afterday.compas.fragment.SuicideConfirmationFragment;
import net.afterday.compas.sensors.Battery.BatteryStatus;
import net.afterday.compas.settings.Settings;
import net.afterday.compas.settings.SettingsListener;
import net.afterday.compas.view.Bar;
import net.afterday.compas.view.Battery;
import net.afterday.compas.view.Clock;
import net.afterday.compas.view.Compass;
import net.afterday.compas.view.CountDownTimer;
import net.afterday.compas.view.Geiger;
import net.afterday.compas.view.Healthbar;
import net.afterday.compas.view.Indicator;
import net.afterday.compas.view.LevelIndicator;
import net.afterday.compas.view.LevelProgress;
import net.afterday.compas.view.Radbar;
import net.afterday.compas.view.SmallLogListAdapter;
import net.afterday.compas.view.Tube;

/* JADX INFO: loaded from: classes.dex */
public class MainActivity extends AppCompatActivity {
    private static final int PRESS_DELAY = 500;
    private static final int PRESS_DELAY_ONE = 50;
    private static final int PRESS_LONG_DELAY = 1000;
    private static final int PRESS_SUICIDE = 1000;
    private static final int STARTUP_PERMISSIONS_REQUEST = 1001;
    private Observable<Long> countDownStream;
    private CountDownTimer countDownTimer;
    private Player.STATE currentState;
    private Observable<Frame> framesStream;
    private Disposable framesSubscribtion;
    private boolean hasActiveArmor;
    private boolean hasActiveBooster;
    private boolean hasActiveDevice;
    private Disposable impactsSubsciption;
    private Observable<ItemAdded> itemAddedStream;
    private ViewGroup layout;
    private LevelProgress levelProgress;
    private SmallLogListAdapter logAdapter;
    private RecyclerView logList;
    private Bar mArmorBar;
    private Battery mBattery;
    private Clock mClock;
    private Compass mCompass;
    private ViewGroup mContentView;
    private Bar mDeviceBar;
    private Geiger mGeiger;
    private Healthbar mHealthbar;
    private Indicator mIndicator;
    private ImageButton mQrButton;
    private Radbar mRadbar;
    private Bar mStaminaBar;
    private Tube mTube;
    private Observable<Integer> playerLevelStream;
    private Observable<Player.STATE> playerStateStream;
    private SettingsListener settingsListener;
    private LocalMainService stalkerApp;
    private Disposable userActionsSubscribtion;
    public boolean upd = false;
    private final String TAG = "MainActivity";
    private CompositeDisposable disposables = new CompositeDisposable();
    private long qrBtnPressTime = 0;
    private long hBarPressTime = 0;
    private long tubePressTime = 0;
    private long staminaPressTime = 0;
    private long devicePressTime = 0;
    private long armorPressTime = 0;
    private long rBarPressTime = 0;
    private long lastTick = 0;
    private long duration = 0;
    private boolean showArtifactsSignal = true;
    private boolean active = false;
    private boolean serviceBound = false;
    private Subject<Integer> orientationChanges = BehaviorSubject.create();
    private ServiceConnection serviceConnection = new AnonymousClass1();

    static /* synthetic */ long access$002(MainActivity x0, long x1) {
        x0.lastTick = x1;
        return x1;
    }

    static /* synthetic */ void access$100(MainActivity x0) {
        x0.setupListeners();
    }

    static /* synthetic */ LevelProgress access$1000(MainActivity x0) {
        return x0.levelProgress;
    }

    static /* synthetic */ void access$1100(MainActivity x0) {
        x0.setupLog();
    }

    static /* synthetic */ boolean access$1202(MainActivity x0, boolean x1) {
        x0.active = x1;
        return x1;
    }

    static /* synthetic */ Tube access$1300(MainActivity x0) {
        return x0.mTube;
    }

    static /* synthetic */ ImageButton access$1400(MainActivity x0) {
        return x0.mQrButton;
    }

    static /* synthetic */ Battery access$1500(MainActivity x0) {
        return x0.mBattery;
    }

    static /* synthetic */ void access$1600(MainActivity x0, Frame x1) {
        x0.updateViews(x1);
    }

    static /* synthetic */ CountDownTimer access$1700(MainActivity x0) {
        return x0.countDownTimer;
    }

    static /* synthetic */ Player.STATE access$1800(MainActivity x0) {
        return x0.currentState;
    }

    static /* synthetic */ Player.STATE access$1802(MainActivity x0, Player.STATE x1) {
        x0.currentState = x1;
        return x1;
    }

    static /* synthetic */ boolean access$1900(MainActivity x0) {
        return x0.showArtifactsSignal;
    }

    static /* synthetic */ boolean access$1902(MainActivity x0, boolean x1) {
        x0.showArtifactsSignal = x1;
        return x1;
    }

    static /* synthetic */ Indicator access$200(MainActivity x0) {
        return x0.mIndicator;
    }

    static /* synthetic */ Geiger access$2000(MainActivity x0) {
        return x0.mGeiger;
    }

    static /* synthetic */ long access$2100(MainActivity x0) {
        return x0.rBarPressTime;
    }

    static /* synthetic */ long access$2102(MainActivity x0, long x1) {
        x0.rBarPressTime = x1;
        return x1;
    }

    static /* synthetic */ boolean access$2200(MainActivity x0) {
        return x0.isAlive();
    }

    static /* synthetic */ void access$2300(MainActivity x0, int x1) {
        x0.openInventory(x1);
    }

    static /* synthetic */ long access$2400(MainActivity x0) {
        return x0.hBarPressTime;
    }

    static /* synthetic */ long access$2402(MainActivity x0, long x1) {
        x0.hBarPressTime = x1;
        return x1;
    }

    static /* synthetic */ long access$2500(MainActivity x0) {
        return x0.tubePressTime;
    }

    static /* synthetic */ long access$2502(MainActivity x0, long x1) {
        x0.tubePressTime = x1;
        return x1;
    }

    static /* synthetic */ void access$2600(MainActivity x0) {
        x0.openSuicideConfirmation();
    }

    static /* synthetic */ LocalMainService access$300(MainActivity x0) {
        return x0.stalkerApp;
    }

    static /* synthetic */ LocalMainService access$302(MainActivity x0, LocalMainService x1) {
        x0.stalkerApp = x1;
        return x1;
    }

    static /* synthetic */ Observable access$400(MainActivity x0) {
        return x0.framesStream;
    }

    static /* synthetic */ Observable access$402(MainActivity x0, Observable x1) {
        x0.framesStream = x1;
        return x1;
    }

    static /* synthetic */ Observable access$500(MainActivity x0) {
        return x0.countDownStream;
    }

    static /* synthetic */ Observable access$502(MainActivity x0, Observable x1) {
        x0.countDownStream = x1;
        return x1;
    }

    static /* synthetic */ Observable access$600(MainActivity x0) {
        return x0.playerLevelStream;
    }

    static /* synthetic */ Observable access$602(MainActivity x0, Observable x1) {
        x0.playerLevelStream = x1;
        return x1;
    }

    static /* synthetic */ Observable access$700(MainActivity x0) {
        return x0.playerStateStream;
    }

    static /* synthetic */ Observable access$702(MainActivity x0, Observable x1) {
        x0.playerStateStream = x1;
        return x1;
    }

    static /* synthetic */ CompositeDisposable access$800(MainActivity x0) {
        return x0.disposables;
    }

    static /* synthetic */ Observable access$900(MainActivity x0) {
        return x0.itemAddedStream;
    }

    static /* synthetic */ Observable access$902(MainActivity x0, Observable x1) {
        x0.itemAddedStream = x1;
        return x1;
    }

    /* JADX INFO: renamed from: net.afterday.compas.MainActivity$1, reason: invalid class name */
    class AnonymousClass1 implements ServiceConnection {
        AnonymousClass1() {
        }

        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            MainActivity.access$002(MainActivity.this, System.currentTimeMillis());
            MainActivity.this.setVolumeControlStream(3);
            MainActivity.access$100(MainActivity.this);
            MainActivity.access$200(MainActivity.this).setVisibility(8);
            MainActivity.access$302(MainActivity.this, ((LocalMainService.MainBinder) iBinder).getService());
            MainActivity mainActivity = MainActivity.this;
            MainActivity.access$402(mainActivity, MainActivity.access$300(mainActivity).getFramesStream());
            MainActivity mainActivity2 = MainActivity.this;
            MainActivity.access$502(mainActivity2, MainActivity.access$300(mainActivity2).getCountDownStream());
            MainActivity mainActivity3 = MainActivity.this;
            MainActivity.access$602(mainActivity3, MainActivity.access$300(mainActivity3).getPlayerLevelStream());
            MainActivity mainActivity4 = MainActivity.this;
            MainActivity.access$702(mainActivity4, MainActivity.access$300(mainActivity4).getPlayerStateStream());
            MainActivity.access$800(MainActivity.this).add(MainActivity.access$600(MainActivity.this).observeOn(AndroidSchedulers.mainThread()).subscribe(new $$Lambda$MainActivity$1$BA5f9aScbL0AvBe7sV8woe4BY(this)));
            MainActivity.access$800(MainActivity.this).add(MainActivity.access$700(MainActivity.this).observeOn(AndroidSchedulers.mainThread()).subscribe(new $$Lambda$MainActivity$1$RTcw5sYNL8fdyZ9N1kfO0wjCsM(this)));
            MainActivity.access$800(MainActivity.this).add(MainActivity.access$500(MainActivity.this).observeOn(AndroidSchedulers.mainThread()).subscribe(new $$Lambda$MainActivity$1$Lqer16B0xY6L3Ask5OHG7a_hmmw(this)));
            MainActivity.access$800(MainActivity.this).add(MainActivity.access$400(MainActivity.this).observeOn(AndroidSchedulers.mainThread()).subscribe(new $$Lambda$MainActivity$1$B_X56S1z4FGKAvfvFwGbic9FPAM(this)));
            MainActivity mainActivity5 = MainActivity.this;
            MainActivity.access$902(mainActivity5, MainActivity.access$300(mainActivity5).getItemAddedStream());
            MainActivity.access$1000(MainActivity.this).addOnLevelChangedListener(new $$Lambda$MainActivity$1$2ZBDXJvanivZ__QN5ebpadhhpbY(this));
            MainActivity.access$800(MainActivity.this).add(MainActivity.access$900(MainActivity.this).observeOn(AndroidSchedulers.mainThread()).subscribe(new $$Lambda$MainActivity$1$Hg7cXGuTTe4WSq_Q9iGLRL85E(this)));
            MainActivity.access$800(MainActivity.this).add(MainActivity.access$300(MainActivity.this).getBatteryStatusStream().observeOn(AndroidSchedulers.mainThread()).subscribe(new $$Lambda$MainActivity$1$GyfiTGu_L9_tnijH2ipBzDn98mw(this)));
            MainActivity.access$400(MainActivity.this).take(1L).observeOn(AndroidSchedulers.mainThread()).subscribe(new $$Lambda$MainActivity$1$K2cmbEbY84ZBMozY_Szg789RZ6k(this));
            MainActivity.access$800(MainActivity.this).add(Observable.combineLatest(EmissionEventBus.instance().getEmissionStateStream(), PlayerEventBus.instance().getPlayerFractionStream(), $$Lambda$MainActivity$1$cPNHwParvMjeuXNyCMYDO8mEoSA.INSTANCE).observeOn(AndroidSchedulers.mainThread()).subscribe(new $$Lambda$MainActivity$1$WaEMGL4ujDQk1GUndFkSACebyY(this)));
            Log.d("MainActivity", "SERVICE CONNECTED!!!!");
            MainActivity.access$1100(MainActivity.this);
            MainActivity.access$1202(MainActivity.this, true);
        }

        public /* synthetic */ void lambda$onServiceConnected$0$MainActivity$1(Integer pl) {
            MainActivity.access$1300(MainActivity.this).setLevel(pl.intValue());
            MainActivity.access$2000(MainActivity.this).setLevel(pl.intValue());
            MainActivity.access$200(MainActivity.this).setLevel(pl.intValue());
            if (pl.intValue() >= 4) {
                MainActivity.access$2000(MainActivity.this).setFingerPrint(true);
            }
            if (pl.intValue() == 5) {
                MainActivity.access$200(MainActivity.this).setVisibility(0);
                MainActivity.access$2000(MainActivity.this).setBrokenGlass(true);
            }
        }

        public /* synthetic */ void lambda$onServiceConnected$1$MainActivity$1(Player.STATE s) {
            MainActivity.access$1802(MainActivity.this, s);
            MainActivity.access$1300(MainActivity.this).setState(s);
            MainActivity.access$1902(MainActivity.this, s.getCode() == 1);
            if (!MainActivity.access$1900(MainActivity.this)) {
                MainActivity.access$200(MainActivity.this).setStrength(0.0f);
            }
        }

        public /* synthetic */ void lambda$onServiceConnected$2$MainActivity$1(Long t) {
            MainActivity.access$1700(MainActivity.this).setSecondsLeft(t);
        }

        public /* synthetic */ void lambda$onServiceConnected$3$MainActivity$1(Frame frame) {
            MainActivity.access$1600(MainActivity.this, frame);
        }

        public /* synthetic */ void lambda$onServiceConnected$4$MainActivity$1(int l) {
            ((LevelIndicator) MainActivity.access$1400(MainActivity.this)).setLevel(1);
        }

        public /* synthetic */ void lambda$onServiceConnected$5$MainActivity$1(ItemAdded ia) {
            MainActivity.access$1000(MainActivity.this).setProgress(ia);
        }

        public /* synthetic */ void lambda$onServiceConnected$6$MainActivity$1(BatteryStatus b) {
            MainActivity.access$1500(MainActivity.this).setStatus(b);
        }

        public /* synthetic */ void lambda$onServiceConnected$7$MainActivity$1(Frame frame) {
            if (frame.getPlayerProps().getLevel() == 5) {
                MainActivity.access$1000(MainActivity.this).showMax(true);
            } else {
                MainActivity.access$1000(MainActivity.this).setProgress(frame.getPlayerProps().getLevelXp());
            }
            ((LevelIndicator) MainActivity.access$1400(MainActivity.this)).setLevel(frame.getPlayerProps().getLevel());
        }

        /* JADX WARN: Multi-variable type inference failed */
        public /* synthetic */ void lambda$onServiceConnected$9$MainActivity$1(Pair s) {
            MainActivity.access$1300(MainActivity.this).setFraction((Player.FRACTION) s.second);
            if (s.second == Player.FRACTION.MONOLITH) {
                MainActivity.access$1300(MainActivity.this).setEmission(false);
            } else {
                MainActivity.access$1300(MainActivity.this).setEmission(((Boolean) s.first).booleanValue());
            }
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d("MainActivity", "SERVICE DISCONNECTED!!!!");
        }
    }

    static /* synthetic */ Pair lambda$onServiceConnected$8(Boolean e, Player.FRACTION f) {
        return new Pair(e, f);
    }

    private void setOrientation(int o) {
        StringBuilder sb = new StringBuilder();
        sb.append("SET ORIENTATION: ");
        sb.append(o == 1 ? "PORTRAIT" : o == 0 ? "LANDSCAPE" : "UNKNOWN");
        Log.e("MainActivity", sb.toString());
        if (o == 1 && getRequestedOrientation() != 1) {
            setRequestedOrientation(1);
        } else if (o == 0 && getRequestedOrientation() != 8) {
            setRequestedOrientation(8);
        }
    }

    @Override // android.support.v7.app.AppCompatActivity, android.support.v4.app.FragmentActivity, android.support.v4.app.BaseFragmentActivityGingerbread, android.app.Activity
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(1024, 1024);
        try {
            getSupportActionBar().hide();
        } catch (Exception e) {
        }
        this.disposables.add(this.orientationChanges.skip(1L).observeOn(AndroidSchedulers.mainThread()).subscribe(new $$Lambda$MainActivity$cIuzdQHpU9ZItTx09Y6mbVeoCks(this)));
        this.orientationChanges.onNext(Integer.valueOf(Settings.instance().getIntSetting(net.afterday.compas.settings.Constants.ORIENTATION)));
        int o = Settings.instance().getIntSetting(net.afterday.compas.settings.Constants.ORIENTATION);
        setOrientation(o);
        setContentView(R.layout.activity_main);
        bindViews();
        setViewListeners();
        this.disposables.add(Observable.combineLatest(PlayerEventBus.instance().getPlayerFractionStream(), this.orientationChanges, $$Lambda$MainActivity$EnWNj7cHhbF7LzqAM_xMElgyM.INSTANCE).observeOn(AndroidSchedulers.mainThread()).subscribe(new $$Lambda$MainActivity$ef4UBKBpUoGe9qcyh46udKOCk(this)));
        startCompassServiceWhenReady();
    }

    private void startCompassServiceWhenReady() {
        String[] missingPermissions = getMissingStartupPermissions();
        if (missingPermissions.length == 0) {
            startAndBindCompassService();
            return;
        }
        ActivityCompat.requestPermissions(this, missingPermissions, STARTUP_PERMISSIONS_REQUEST);
    }

    private String[] getMissingStartupPermissions() {
        ArrayList<String> permissions = new ArrayList<>();
        addMissingPermission(permissions, Manifest.permission.ACCESS_FINE_LOCATION);
        addMissingPermission(permissions, Manifest.permission.ACCESS_COARSE_LOCATION);
        addMissingPermission(permissions, Manifest.permission.CAMERA);
        if (Build.VERSION.SDK_INT >= 31) {
            addMissingPermission(permissions, Manifest.permission.BLUETOOTH_SCAN);
            addMissingPermission(permissions, Manifest.permission.BLUETOOTH_CONNECT);
        }
        if (Build.VERSION.SDK_INT >= 33) {
            addMissingPermission(permissions, Manifest.permission.NEARBY_WIFI_DEVICES);
            addMissingPermission(permissions, Manifest.permission.POST_NOTIFICATIONS);
        }
        return permissions.toArray(new String[permissions.size()]);
    }

    private void addMissingPermission(ArrayList<String> permissions, String permission) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(permission);
        }
    }

    private void startAndBindCompassService() {
        if (this.serviceBound) {
            return;
        }
        Intent serviceIntent = new Intent(this, (Class<?>) LocalMainService.class);
        ContextCompat.startForegroundService(this, serviceIntent);
        bindService(serviceIntent, this.serviceConnection, BIND_AUTO_CREATE);
        this.serviceBound = true;
    }

    @Override // android.support.v4.app.FragmentActivity, android.app.Activity
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != STARTUP_PERMISSIONS_REQUEST) {
            return;
        }
        if (getMissingStartupPermissions().length == 0) {
            startAndBindCompassService();
            return;
        }
        Toast.makeText(this, "Для запуска PDA Compass нужны разрешения датчиков", Toast.LENGTH_LONG).show();
    }

    public /* synthetic */ void lambda$onCreate$0$MainActivity(Integer o) {
        setOrientation(o.intValue());
    }

    static /* synthetic */ Pair lambda$onCreate$1(Player.FRACTION pf, Integer x) {
        return new Pair(pf, x);
    }

    /* JADX WARN: Multi-variable type inference failed */
    public /* synthetic */ void lambda$onCreate$2$MainActivity(Pair p) {
        setBackground((Player.FRACTION) p.first, ((Integer) p.second).intValue());
    }

    private void setupLog() {
        RecyclerView.LayoutManager logListManager = new LinearLayoutManager(this, 1, true);
        ((LinearLayoutManager) logListManager).setStackFromEnd(true);
        this.logList.setLayoutManager(logListManager);
        this.logAdapter = new SmallLogListAdapter(this, new ArrayList());
        this.logList.setAdapter(this.logAdapter);
        this.disposables.add(this.stalkerApp.getLogStream().subscribe(new $$Lambda$MainActivity$VKstrb853Kwj0cjG0Vvt4oNHWjA(this, logListManager)));
    }

    public /* synthetic */ void lambda$setupLog$3$MainActivity(RecyclerView.LayoutManager logListManager, List log) {
        this.logAdapter.setDataset(log);
        logListManager.scrollToPosition(log.size() - 1);
    }

    private void updateViews(Frame frame) {
        PlayerProps pProps = frame.getPlayerProps();
        this.upd = true;
        pProps.getState();
        this.upd = false;
        if (pProps.getState().getCode() == 1 && (pProps.emissionHit() || pProps.anomalyHit() || pProps.burerHit() || pProps.controllerHit() || pProps.mentalHit())) {
            showBlood();
            pProps.getState();
        }
        if (pProps.getState().getCode() == 1 && pProps.getHealthImpact() <= 0.0d) {
            this.mGeiger.setAnomaly((float) pProps.getAnomalyImpact());
            this.mGeiger.setMental((float) pProps.getMentalImpact());
            this.mGeiger.setMonolith((float) pProps.getMonolithImpact());
            this.mGeiger.toSvh((float) pProps.getRadiationImpact(), 1000L);
        } else {
            this.mGeiger.setAnomaly(0.0f);
            this.mGeiger.setMental(0.0f);
            this.mGeiger.setMonolith(0.0f);
            this.mGeiger.toSvh(0.0f, 750L);
        }
        this.mTube.setParameters(pProps.getRadiationImpact(), pProps.getAnomalyImpact(), pProps.getMentalImpact(), pProps.getMonolithImpact(), pProps.getControllerImpact(), pProps.getBurerImpact(), pProps.getHealthImpact(), pProps.getState());
        this.mRadbar.setInfo(pProps.getHealth(), pProps.getRadiation(), pProps.getHealthImpact(), pProps.getController(), pProps.hasRadiationInstant());
        this.mHealthbar.setInfo(pProps.getHealth(), pProps.getHealthImpact(), pProps.getController(), pProps.hasHealthInstant());
        if (this.showArtifactsSignal) {
            if (frame.getPlayerProps().getHealthImpact() > 0.0d) {
                this.mIndicator.setStrength(0.0f);
            } else {
                this.mIndicator.setStrength((float) pProps.getArtefactImpact());
            }
        }
        this.mStaminaBar.setPercents(pProps.getBoosterPercents());
        this.mDeviceBar.setPercents(pProps.getDevicePercents());
        this.mArmorBar.setPercents(pProps.getArmorPercents());
        this.hasActiveArmor = pProps.getArmorPercents() > 0.0d;
        this.hasActiveBooster = pProps.getBoosterPercents() > 0.0d;
        this.hasActiveDevice = pProps.getDevicePercents() > 0.0d;
    }

    private void setViewListeners() {
        this.mStaminaBar.setOnTouchListener(new $$Lambda$MainActivity$VZ9qB0e2fUAruITEnkd9UBxI2_8(this));
        this.mDeviceBar.setOnTouchListener(new $$Lambda$MainActivity$sVd1cedN8VFuoHGhnGmBJj7SZ5M(this));
        this.mArmorBar.setOnTouchListener(new $$Lambda$MainActivity$xEvCYm98SWF5wyoXwHPpKT0iVPs(this));
        this.mQrButton.setOnTouchListener(new $$Lambda$MainActivity$6uO8oY_j9sXKnjCLwnBqutuFrs(this));
        this.mRadbar.setOnTouchListener(new AnonymousClass2());
        this.mHealthbar.setOnTouchListener(new AnonymousClass3());
        this.mTube.setOnTouchListener(new AnonymousClass4());
    }

    public /* synthetic */ boolean lambda$setViewListeners$4$MainActivity(View v, MotionEvent event) {
        int action = event.getAction();
        if (action == 0) {
            this.staminaPressTime = System.currentTimeMillis();
        } else if (action == 1 && System.currentTimeMillis() - this.staminaPressTime > 500 && isAlive()) {
            openInventory(2);
        }
        return true;
    }

    public /* synthetic */ boolean lambda$setViewListeners$5$MainActivity(View v, MotionEvent event) {
        int action = event.getAction();
        if (action == 0) {
            this.devicePressTime = System.currentTimeMillis();
        } else if (action == 1 && System.currentTimeMillis() - this.devicePressTime > 500 && isAlive()) {
            openInventory(9);
        }
        return true;
    }

    public /* synthetic */ boolean lambda$setViewListeners$6$MainActivity(View v, MotionEvent event) {
        int action = event.getAction();
        if (action == 0) {
            this.armorPressTime = System.currentTimeMillis();
        } else if (action == 1 && System.currentTimeMillis() - this.armorPressTime > 500 && isAlive()) {
            openInventory(3);
        }
        return true;
    }

    public /* synthetic */ boolean lambda$setViewListeners$7$MainActivity(View v, MotionEvent event) {
        int action = event.getAction();
        if (action == 0) {
            long delay = System.currentTimeMillis();
            this.qrBtnPressTime = delay;
        } else if (action == 1) {
            long delay2 = isAlive() ? 500L : 1000L;
            if (System.currentTimeMillis() - this.qrBtnPressTime > delay2) {
                openScanner();
            }
        }
        return true;
    }

    /* JADX INFO: renamed from: net.afterday.compas.MainActivity$2, reason: invalid class name */
    class AnonymousClass2 implements View.OnTouchListener {
        AnonymousClass2() {
        }

        @Override // android.view.View.OnTouchListener
        public boolean onTouch(View v, MotionEvent event) {
            int action = event.getAction();
            if (action == 0) {
                MainActivity.access$2102(MainActivity.this, System.currentTimeMillis());
            } else if (action == 1 && System.currentTimeMillis() - MainActivity.access$2100(MainActivity.this) > 500 && MainActivity.access$2200(MainActivity.this)) {
                MainActivity.access$2300(MainActivity.this, 99);
            }
            return true;
        }
    }

    /* JADX INFO: renamed from: net.afterday.compas.MainActivity$3, reason: invalid class name */
    class AnonymousClass3 implements View.OnTouchListener {
        AnonymousClass3() {
        }

        @Override // android.view.View.OnTouchListener
        public boolean onTouch(View v, MotionEvent event) {
            int action = event.getAction();
            if (action == 0) {
                MainActivity.access$2402(MainActivity.this, System.currentTimeMillis());
            } else if (action == 1 && System.currentTimeMillis() - MainActivity.access$2400(MainActivity.this) > 500 && MainActivity.access$2200(MainActivity.this)) {
                MainActivity.access$2300(MainActivity.this, 99);
            }
            return true;
        }
    }

    /* JADX INFO: renamed from: net.afterday.compas.MainActivity$4, reason: invalid class name */
    class AnonymousClass4 implements View.OnTouchListener {
        AnonymousClass4() {
        }

        @Override // android.view.View.OnTouchListener
        public boolean onTouch(View v, MotionEvent event) {
            int action = event.getAction();
            if (action == 0) {
                MainActivity.access$2502(MainActivity.this, System.currentTimeMillis());
            } else if (action == 1 && MainActivity.access$1800(MainActivity.this) != null && MainActivity.access$1800(MainActivity.this).getSuicideType() != 4 && System.currentTimeMillis() - MainActivity.access$2500(MainActivity.this) > 1000) {
                if (MainActivity.access$1800(MainActivity.this) == Player.STATE.W_ABDUCTED) {
                    PlayerEventBus.instance().suicide();
                    return true;
                }
                MainActivity.access$2600(MainActivity.this);
            }
            return true;
        }
    }

    private boolean isAlive() {
        Player.STATE state = this.currentState;
        return state != null && state.getCode() == 1;
    }

    private void openSuicideConfirmation() {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag(PlayerEventBus.SUICIDE);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        DialogFragment newFragment = new SuicideConfirmationFragment();
        newFragment.show(ft, "scanner");
    }

    private void showBlood() {
        if (!this.active) {
            return;
        }
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        DialogFragment bloodFragment = new BloodFragment();
        bloodFragment.show(ft, "blood");
    }

    private void bindViews() {
        this.mGeiger = (Geiger) findViewById(R.id.geiger);
        this.mCompass = (Compass) findViewById(R.id.compass);
        this.mRadbar = (Radbar) findViewById(R.id.radbar);
        this.mHealthbar = (Healthbar) findViewById(R.id.healthbar);
        this.mArmorBar = (Bar) findViewById(R.id.armorbar);
        this.mStaminaBar = (Bar) findViewById(R.id.staminabar);
        this.mDeviceBar = (Bar) findViewById(R.id.devicebar);
        this.mBattery = (Battery) findViewById(R.id.battery);
        this.mTube = (Tube) findViewById(R.id.tube);
        this.mQrButton = (ImageButton) findViewById(R.id.qrbutton);
        this.logList = (RecyclerView) findViewById(R.id.log_list);
        this.mIndicator = (Indicator) findViewById(R.id.indicator);
        this.countDownTimer = (CountDownTimer) findViewById(R.id.countdown);
        this.levelProgress = (LevelProgress) findViewById(R.id.levelProgress);
        this.layout = (ViewGroup) findViewById(R.id.activity_main);
        if (Settings.instance().getBoolSetting(net.afterday.compas.settings.Constants.COMPASS)) {
            this.mCompass.compassOn();
        } else {
            this.mCompass.compassOff();
        }
    }

    private void setBackground(Player.FRACTION pf, int orientation) {
        this.layout.setBackground(ContextCompat.getDrawable(this, getBackground(pf, orientation)));
    }

    /* JADX INFO: renamed from: net.afterday.compas.MainActivity$5, reason: invalid class name */
    static /* synthetic */ class AnonymousClass5 {
        static final /* synthetic */ int[] $SwitchMap$net$afterday$compas$core$player$Player$FRACTION = new int[Player.FRACTION.values().length];

        static {
            try {
                $SwitchMap$net$afterday$compas$core$player$Player$FRACTION[Player.FRACTION.MONOLITH.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$net$afterday$compas$core$player$Player$FRACTION[Player.FRACTION.STALKER.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$net$afterday$compas$core$player$Player$FRACTION[Player.FRACTION.GAMEMASTER.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$net$afterday$compas$core$player$Player$FRACTION[Player.FRACTION.DARKEN.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
        }
    }

    private int getBackground(Player.FRACTION pf, int orientation) {
        int i = AnonymousClass5.$SwitchMap$net$afterday$compas$core$player$Player$FRACTION[pf.ordinal()];
        if (i == 1) {
            return orientation == 0 ? R.drawable.background_h_monolith : R.drawable.background_v_monolith;
        }
        if (i == 2) {
            return orientation == 0 ? R.drawable.background_h_merged : R.drawable.background_v_merged;
        }
        if (i == 3) {
            return orientation == 0 ? R.drawable.background_h_gamemaster : R.drawable.background_v_gamemaster;
        }
        if (i != 4) {
            return -1;
        }
        return orientation == 0 ? R.drawable.background_h_darken : R.drawable.background_v_darken;
    }

    private void openInventory(int type) {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        if (type == 99) {
            Fragment prev = getFragmentManager().findFragmentByTag("inventory");
            if (prev != null) {
                ft.remove(prev);
            }
            ft.addToBackStack(null);
            Bundle b = new Bundle();
            b.putInt("type", type);
            DialogFragment newFragment = new InventoryFragment();
            newFragment.setArguments(b);
            newFragment.show(ft, "inventory");
            return;
        }
        if ((type == 3 && this.hasActiveArmor) || ((type == 2 && this.hasActiveBooster) || (type == 9 && this.hasActiveDevice))) {
            ItemEventsBus.instance().getUserItems().take(1L).subscribe(new $$Lambda$MainActivity$Vd03GPQtFBYiu__tgy7kwxGlgnA(this, type));
            ItemEventsBus.instance().requestItems();
        }
    }

    public /* synthetic */ void lambda$openInventory$8$MainActivity(int type, Inventory inv) {
        if (type == 2) {
            openItemInfo(inv.getActiveBooster());
        } else if (type == 3) {
            openItemInfo(inv.getActiveArmor());
        } else if (type == 9) {
            openItemInfo(inv.getActiveDevice());
        }
    }

    private void openScanner() {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("scanner");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        DialogFragment newFragment = ScannerFragment.newInstance();
        newFragment.show(ft, "scanner");
    }

    private void setupListeners() {
        ItemEventsBus ieBus = LocalMainService.getInstance().getItemEventBus();
        this.disposables.add(ieBus.getItemAddedEvents().observeOn(AndroidSchedulers.mainThread()).subscribe(new $$Lambda$MainActivity$kN0I4J1UbBsc99gQBHxFwLnBKwA(this)));
        this.disposables.add(ieBus.getUnknownItemEvents().observeOn(AndroidSchedulers.mainThread()).subscribe(new $$Lambda$MainActivity$b5sMOSgOxA1qerBV1FERTft7ae4(this)));
        this.settingsListener = new $$Lambda$MainActivity$ZnxmrvBnM8PbxeUw5PO1856bMoU(this);
        Settings.instance().addSettingsListener(this.settingsListener);
    }

    public /* synthetic */ void lambda$setupListeners$9$MainActivity(Item i) {
        itemAdded(i);
    }

    public /* synthetic */ void lambda$setupListeners$10$MainActivity(String i) {
        unknownItem(i);
    }

    public /* synthetic */ void lambda$setupListeners$11$MainActivity(java.lang.String r4, java.lang.String r5) {
        if ("ORIENTATION".equals(r4)) {
            this.orientationChanges.onNext(Integer.valueOf(Integer.parseInt(r5)));
            return;
        }
        if ("COMPASS".equals(r4)) {
            try {
                if (Boolean.parseBoolean(r5)) {
                    this.mCompass.compassOn();
                } else {
                    this.mCompass.compassOff();
                }
            } catch (Exception e) {
            }
        }
    }

    private void itemAdded(Item item) {
    }

    private void unknownItem(String item) {
    }

    @Override // android.support.v7.app.AppCompatActivity, android.support.v4.app.FragmentActivity, android.app.Activity
    protected void onDestroy() {
        Log.e("MainActivity", "ON DESTROY!!!!");
        super.onDestroy();
        if (!this.disposables.isDisposed()) {
            this.disposables.dispose();
            this.settingsListener = null;
        }
        if (this.serviceBound) {
            unbindService(this.serviceConnection);
            this.serviceBound = false;
        }
    }

    @Override // android.support.v4.app.FragmentActivity, android.app.Activity
    protected void onPause() {
        super.onPause();
        this.active = false;
    }

    @Override // android.support.v4.app.FragmentActivity, android.app.Activity
    protected void onResume() {
        super.onResume();
        this.active = true;
    }

    @Override // android.support.v4.app.FragmentActivity, android.app.Activity
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        stopService(new Intent(this, (Class<?>) LocalMainService.class));
        Process.killProcess(Process.myPid());
    }

    private long getUsedMemory() {
        try {
            Runtime info = Runtime.getRuntime();
            long freeSize = info.freeMemory();
            long totalSize = info.totalMemory();
            long usedSize = totalSize - freeSize;
            return usedSize;
        } catch (Exception e) {
            e.printStackTrace();
            return -1L;
        }
    }

    private void openItemInfo(Item item) {
        Log.d("MainActivity", "openItemInfo");
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("item");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        ItemInfoFragment newFragment = ItemInfoFragment.newInstance(item);
        newFragment.show(ft, "item");
    }
}
