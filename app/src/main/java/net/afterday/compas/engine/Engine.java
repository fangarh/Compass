package net.afterday.compas.engine;

import android.support.v4.util.Pair;
import android.util.Log;
import com.google.gson.JsonObject;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Scheduler;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;
import net.afterday.compas.R;
import net.afterday.compas.core.Controls;
import net.afterday.compas.core.Game;
import net.afterday.compas.core.GameImpl;
import net.afterday.compas.core.events.PlayerEventsListener;
import net.afterday.compas.core.gameState.Frame;
import net.afterday.compas.core.gameState.State;
import net.afterday.compas.core.influences.Emission;
import net.afterday.compas.core.influences.InfluencesPack;
import net.afterday.compas.core.inventory.items.Events.ItemAdded;
import net.afterday.compas.core.inventory.items.Item;
import net.afterday.compas.core.player.Impacts;
import net.afterday.compas.core.player.Player;
import net.afterday.compas.core.serialization.Jsonable;
import net.afterday.compas.core.serialization.Serializer;
import net.afterday.compas.devices.DeviceProvider;
import net.afterday.compas.effects.Effects;
import net.afterday.compas.engine.actions.Action;
import net.afterday.compas.engine.actions.ActionsExecutor;
import net.afterday.compas.engine.events.CodeInputEventBus;
import net.afterday.compas.engine.events.EmissionEventBus;
import net.afterday.compas.engine.events.ItemEventsBus;
import net.afterday.compas.engine.events.PlayerEventBus;
import net.afterday.compas.engine.influences.InfluenceProviderImpl;
import net.afterday.compas.engine.influences.InfluencesController;
import net.afterday.compas.engine.threading.Threads;
import net.afterday.compas.logging.Logger;
import net.afterday.compas.persistency.PersistencyProvider;
import net.afterday.compas.sensors.SensorsProvider;
import net.afterday.compas.serialization.SharedPrefsSerializer;
import net.afterday.compas.util.Triple;

/* JADX INFO: loaded from: classes.dex */
public class Engine implements Jsonable {
    private static final String COUNTDOWN = "COUNTDOWN";
    private static final int MIN1 = 60;
    private static final int MIN30 = 1800;
    private static final int MIN5 = 300;
    private static final int MIN60 = 3600;
    private static final int NUMBER_OF_CPUS = 4;
    private static final int SEC30 = 30;
    private static final String START = "Start";
    private static final String TAG = "Engine";
    public static final int TICK_MILLISECONDS = 1000;
    private static Engine instance;
    private Scheduler computation;
    private DeviceProvider deviceProvider;
    private Effects effects;
    private ActionsExecutor executor;
    private Game game;
    private Observable<Long> gameRunning;
    private InfluencesController influenceProvider;
    private Observable<InfluencesPack> influencesStream;
    private JsonObject o;
    private PersistencyProvider persistencyProvider;
    private SensorsProvider sensorsProvider;
    private Serializer serializer;
    private Observable<String> startCommands;
    private Observable<Long> ticks;
    private Observable<Long> secondsLeft = BehaviorSubject.createDefault(-1L);
    private Subject<Boolean> acceptsInfluences = BehaviorSubject.create();
    private Subject<Integer> playerLevel = BehaviorSubject.create();
    private Subject<ItemAdded> itemAdded = PublishSubject.create();
    private Subject<Player.STATE> currentPlayerState = PlayerEventBus.instance().getPlayerStateStream();
    private Subject<Long> countdownStarted = BehaviorSubject.create();
    private Observable<Impacts.STATE> impactsStatesStream = PublishSubject.create();
    private Subject<Frame> framesStream = BehaviorSubject.create();
    private Subject<Player.FRACTION> fractionStream = PlayerEventBus.instance().getPlayerFractionStream();
    private Subject<String> commands = PublishSubject.create();
    private Subject<State> gameStates = BehaviorSubject.create();
    private CompositeDisposable currentEmission = new CompositeDisposable();
    private Subject<Boolean> influencesRunning = BehaviorSubject.createDefault(false);
    private Controls controls = new ControlsImpl(this, null);
    private ItemEventsBus itemEventsBus = ItemEventsBus.instance();

    public static /* synthetic */ void lambda$ggXWF3zuMiXIL2rX4BFfSm6DGgc(Engine engine, Game game) {
        engine.startGame(game);
    }

    static /* synthetic */ Controls access$1000(Engine x0) {
        return x0.controls;
    }

    static /* synthetic */ Observable access$1100(Engine x0) {
        return x0.secondsLeft;
    }

    static /* synthetic */ Game access$1200(Engine x0) {
        return x0.game;
    }

    static /* synthetic */ Observable access$1300(Engine x0) {
        return x0.impactsStatesStream;
    }

    static /* synthetic */ Subject access$1400(Engine x0) {
        return x0.itemAdded;
    }

    static /* synthetic */ Subject access$1500(Engine x0) {
        return x0.fractionStream;
    }

    static /* synthetic */ Subject access$1600(Engine x0) {
        return x0.playerLevel;
    }

    static /* synthetic */ void access$200(Engine x0, int x1) {
        x0.notifyEmission(x1);
    }

    static /* synthetic */ String access$300(Engine x0, Calendar x1) {
        return x0.calToStr(x1);
    }

    static /* synthetic */ void access$400(Engine x0) {
        x0.notifyFakeEmission();
    }

    static /* synthetic */ void access$500(Engine x0, int x1) {
        x0.startEmission(x1);
    }

    static /* synthetic */ Subject access$600(Engine x0) {
        return x0.acceptsInfluences;
    }

    static /* synthetic */ Subject access$700(Engine x0) {
        return x0.influencesRunning;
    }

    static /* synthetic */ Subject access$800(Engine x0) {
        return x0.countdownStarted;
    }

    static /* synthetic */ Subject access$900(Engine x0) {
        return x0.currentPlayerState;
    }

    private Engine() {
        this.gameStates.onNext(State.NOT_STARTED);
        this.computation = Threads.computation();
        this.ticks = Observable.interval(0L, 1000L, TimeUnit.MILLISECONDS);
        this.gameRunning = this.gameStates.filter($$Lambda$Engine$MEZ1PvDf9ll7JR2XBnzbbMZhxs.INSTANCE).switchMap(new $$Lambda$Engine$w6Q3aItYwTLz2ptXxVqVzA433s(this));
        this.executor = ActionsExecutor.instance(this.gameRunning);
        this.startCommands = this.commands.filter($$Lambda$Engine$aM2OD5CbFeTUz2H2gzm7R2Fw_oU.INSTANCE);
        this.startCommands.observeOn(this.computation).take(1L).map(new $$Lambda$Engine$gPxwIpknrqeVztb5G9aLgksNPSw(this)).subscribe(new $$Lambda$Engine$ggXWF3zuMiXIL2rX4BFfSm6DGgc(this));
    }

    static /* synthetic */ boolean lambda$new$0(State gs) {
        return gs == State.STARTED;
    }

    public /* synthetic */ ObservableSource lambda$new$1$Engine(State st) {
        return st == State.NOT_STARTED ? Observable.empty() : this.ticks;
    }

    static /* synthetic */ boolean lambda$new$2(String cmd) {
        return cmd == START;
    }

    public /* synthetic */ Game lambda$new$3$Engine(String cmd) {
        return initializeGame();
    }

    public void start() {
        String errorMsg = null;
        if (this.sensorsProvider == null) {
            errorMsg = "SensorsProvider not set.";
        }
        if (this.persistencyProvider == null) {
            errorMsg = "Persistency provider not set.";
        }
        if (errorMsg != null) {
            throw new IllegalStateException(errorMsg);
        }
        this.commands.onNext(START);
    }

    public Observable<Frame> getFramesStream() {
        return this.framesStream;
    }

    public ItemEventsBus getItemEventsBus() {
        return this.itemEventsBus;
    }

    private Game initializeGame() {
        this.serializer = SharedPrefsSerializer.instance();
        this.game = new GameImpl(this.persistencyProvider, this.serializer);
        this.game.getPlayer().addPlayerEventsListener(new PlayerEventsListenerImpl(this, null));
        this.playerLevel.onNext(Integer.valueOf(this.game.getPlayer().getPlayerProps().getLevel()));
        this.fractionStream.onNext(this.game.getPlayer().getPlayerProps().getFraction());
        Player.STATE ps = this.game.getPlayer().getPlayerProps().getState();
        Jsonable jso = this.serializer.deserialize(COUNTDOWN);
        long left = -1;
        if (jso != null) {
            this.o = jso.toJson();
            if (this.o.has("left")) {
                left = this.o.get("left").getAsLong();
            }
        } else {
            this.o = new JsonObject();
            this.o.addProperty("left", (Number) (-1L));
        }
        this.currentPlayerState.onNext(ps);
        if (left > 0) {
            this.countdownStarted.onNext(Long.valueOf(System.currentTimeMillis() - ((ps.getWaitTime() - left) * 1000)));
            ((Subject) this.secondsLeft).onNext(Long.valueOf(left));
        }
        this.secondsLeft.skip(1L).subscribe(new $$Lambda$Engine$SpOCx8rFlG_evD2_pErD6AwhLgM(this));
        this.influenceProvider = new InfluenceProviderImpl(this.sensorsProvider, this.persistencyProvider.getInfluencesPersistency(), this.gameRunning);
        this.influencesStream = this.influenceProvider.getInfluenceStream();
        Observable.combineLatest(this.influencesRunning, this.playerLevel, $$Lambda$Engine$QJET32yWBZzf3wHgK9t917_XSV8.INSTANCE).subscribe(new $$Lambda$Engine$AXPnYrA0ReTPSWrfCzdUIWkGRI(this));
        this.effects = new Effects(this.deviceProvider);
        this.effects.setPlayerStatesStream(this.currentPlayerState);
        this.effects.setPlayerLevelStream(this.playerLevel);
        this.effects.setImpactsStatesStream(this.impactsStatesStream);
        return this.game;
    }

    public /* synthetic */ void lambda$initializeGame$4$Engine(Long s) {
        if (s.longValue() % 5 == 0 || s.longValue() == -1) {
            this.o.addProperty("left", s);
            this.serializer.serialize(COUNTDOWN, this);
        }
    }

    static /* synthetic */ Pair lambda$initializeGame$5(Boolean r, Integer l) {
        return new Pair(r, l);
    }

    /* JADX WARN: Multi-variable type inference failed */
    public /* synthetic */ void lambda$initializeGame$6$Engine(Pair p) {
        if (((Boolean) p.first).booleanValue()) {
            this.influenceProvider.start(((Integer) p.second).intValue());
        } else {
            this.influenceProvider.stop(((Integer) p.second).intValue());
        }
    }

    private void startGame(Game game) {
        this.influencesStream.observeOn(this.computation).subscribe(new $$Lambda$Engine$Rhui28xjbbahEKCKmLSbxYArss(this, game));
        CodeInputEventBus.getCodeScans().observeOn(this.computation).subscribe(new $$Lambda$Engine$d8dqkwfFTlvkVfP_nzgZdFKPjHI(game));
        this.itemEventsBus.getDropItemEvents().observeOn(this.computation).subscribe(new $$Lambda$Engine$xL72DkP3tjgWzrmczHPU5rFHK5g(game));
        this.itemEventsBus.getUserItemsRequests().subscribe(new $$Lambda$Engine$OiaDkqNSTEEEwsmffxcof6IKQ5E(this, game));
        this.itemEventsBus.getUseItemRequests().subscribe(new $$Lambda$Engine$saDmpyWoyMUtJFkJL9p82hVrL1Q(this, game));
        setupSuicides();
        setupEmissions();
        Observable.combineLatest(EmissionEventBus.instance().getEmissionStateStream(), this.currentPlayerState, PlayerEventBus.instance().getPlayerFractionStream(), $$Lambda$Engine$3PNNt2ygH2_ALEbWIcFyvQy8IK8.INSTANCE).subscribe(new $$Lambda$Engine$Kuf9G4yAGJwx2xHclgdE9zJn8E(this));
        ItemEventsBus.instance().getItemUsedEvents().filter($$Lambda$Engine$8knwBC9ZN8xFluejut32DK7PWO8.INSTANCE).withLatestFrom(EmissionEventBus.instance().getEmissionStateStream(), $$Lambda$Engine$leiBipiW50_2U4lYFTvLk3bkNM.INSTANCE).filter($$Lambda$Engine$RfF9X2708XSpK_lb9NATUjLy2s.INSTANCE).subscribe(new $$Lambda$Engine$YsrUuN5aSpPsJKFqdebjbZB4Rho(this));
        setupStateTimers();
        this.framesStream.onNext(game.start());
        this.controls.startInfluences();
        this.acceptsInfluences.onNext(true);
        this.gameStates.onNext(State.STARTED);
    }

    public /* synthetic */ void lambda$startGame$7$Engine(Game game, InfluencesPack inf) {
        this.framesStream.onNext(game.acceptInfluences(inf));
    }

    static /* synthetic */ void lambda$startGame$8(Game game, String code) {
        CodeInputEventBus.codeAccepted(game.acceptCode(code));
    }

    static /* synthetic */ void lambda$startGame$9(Game game, Item item) {
        game.getPlayer().dropItem(item);
    }

    public /* synthetic */ void lambda$startGame$10$Engine(Game game, String r) {
        this.itemEventsBus.userItemsLoaded(game.getInventory());
    }

    public /* synthetic */ void lambda$startGame$11$Engine(Game game, Item r) {
        this.framesStream.onNext(game.useItem(r));
    }

    static /* synthetic */ Triple lambda$startGame$12(Boolean e, Player.STATE p, Player.FRACTION f) {
        return new Triple(e, p, f);
    }

    /* JADX WARN: Multi-variable type inference failed */
    public /* synthetic */ void lambda$startGame$13$Engine(Triple pr) {
        if (((Boolean) pr.first).booleanValue() && ((Player.STATE) pr.second).getCode() == 1 && pr.third != Player.FRACTION.MONOLITH) {
            this.influenceProvider.startEmission();
        }
    }

    static /* synthetic */ boolean lambda$startGame$14(Item i) {
        return i.getItemDescriptor().getName() == "Anabiotic";
    }

    static /* synthetic */ Boolean lambda$startGame$15(Item i, Boolean e) {
        return e;
    }

    static /* synthetic */ boolean lambda$startGame$16(Boolean e) {
        return e.booleanValue();
    }

    public /* synthetic */ void lambda$startGame$17$Engine(Boolean e) {
        emissionEnded();
    }

    private Disposable setupStateTimers() {
        CompositeDisposable cd = new CompositeDisposable();
        cd.add(makeCountDownForStates(Player.STATE.W_CONTROLLED, Player.STATE.CONTROLLED, Player.STATE.DEAD_CONTROLLER));
        cd.add(makeCountDownForStates(Player.STATE.W_MENTALLED, Player.STATE.MENTALLED, Player.STATE.DEAD_MENTAL));
        cd.add(makeCountDownForStates(Player.STATE.W_DEAD_ANOMALY, Player.STATE.DEAD_ANOMALY));
        cd.add(makeCountDownForStates(Player.STATE.W_DEAD_BURER, Player.STATE.DEAD_BURER));
        cd.add(makeCountDownForStates(Player.STATE.W_DEAD_RADIATION, Player.STATE.DEAD_RADIATION));
        cd.add(makeCountDownForStates(Player.STATE.W_ABDUCTED, Player.STATE.DEAD_BURER));
        cd.add(makeCountDownForStates(Player.STATE.ABDUCTED, Player.STATE.ALIVE));
        return cd;
    }

    private Disposable makeCountDownForStates(Player.STATE s1, Player.STATE s2, Player.STATE s3) {
        return makeCountDownForStates(s1, s1.getWaitTime(), s2, s2.getWaitTime(), s3);
    }

    private Disposable makeCountDownForStates(Player.STATE s1, Player.STATE s2) {
        return makeCountDownForStates(s1, s1.getWaitTime(), s2);
    }

    private Disposable makeCountDownForStates(Player.STATE state1, long time1, Player.STATE finalState) {
        CompositeDisposable cd = new CompositeDisposable();
        Observable<Long> stateCountDown = makeCountDownFor(state1);
        cd.add(stateCountDown.subscribe(new $$Lambda$Engine$a_JVl5Q4Pw8SNc83g09WZqWKlU0(this, time1, finalState)));
        return cd;
    }

    public /* synthetic */ void lambda$makeCountDownForStates$18$Engine(long time1, Player.STATE finalState, Long t) {
        if (t.longValue() <= time1) {
            ((Subject) this.secondsLeft).onNext(Long.valueOf(time1 - t.longValue()));
        } else {
            this.game.getPlayer().setState(finalState);
            ((Subject) this.secondsLeft).onNext(Long.valueOf(time1 - t.longValue()));
        }
    }

    private void setupEmissions() {
        new CompositeDisposable();
        List<Emission> emissions = this.persistencyProvider.getInfluencesPersistency().getEmissions();
        long now = System.currentTimeMillis();
        for (Emission e : emissions) {
            Calendar strtAt = e.getStartTime();
            if (strtAt.getTimeInMillis() + ((long) (e.duration() * 1000 * 60)) >= now && (!e.isFake() || strtAt.getTimeInMillis() >= now)) {
                if (now < strtAt.getTimeInMillis() - ((long) ((e.notifyBefore() * 1000) * 60))) {
                    this.executor.postAction(new AnonymousClass1(strtAt, e));
                }
                this.executor.postAction(new AnonymousClass2(e, strtAt, now));
            }
        }
    }

    /* JADX INFO: renamed from: net.afterday.compas.engine.Engine$1, reason: invalid class name */
    class AnonymousClass1 implements Action {
        final /* synthetic */ Emission val$e;
        final /* synthetic */ Calendar val$strtAt;

        AnonymousClass1(Calendar calendar, Emission emission) {
            this.val$strtAt = calendar;
            this.val$e = emission;
        }

        @Override // net.afterday.compas.engine.actions.Action
        public long startTime() {
            return this.val$strtAt.getTimeInMillis() - ((long) ((this.val$e.notifyBefore() * 1000) * 60));
        }

        @Override // net.afterday.compas.engine.actions.Action
        public void execute() {
            Engine.access$200(Engine.this, this.val$e.notifyBefore());
        }

        public String toString() {
            return "Notify emission action: " + Engine.access$300(Engine.this, this.val$strtAt);
        }
    }

    /* JADX INFO: renamed from: net.afterday.compas.engine.Engine$2, reason: invalid class name */
    class AnonymousClass2 implements Action {
        final /* synthetic */ Emission val$e;
        final /* synthetic */ long val$now;
        final /* synthetic */ Calendar val$strtAt;

        AnonymousClass2(Emission emission, Calendar calendar, long j) {
            this.val$e = emission;
            this.val$strtAt = calendar;
            this.val$now = j;
        }

        @Override // net.afterday.compas.engine.actions.Action
        public long startTime() {
            return this.val$e.getStartTime().getTimeInMillis();
        }

        @Override // net.afterday.compas.engine.actions.Action
        public void execute() {
            if (this.val$e.isFake()) {
                Engine.access$400(Engine.this);
            } else if (this.val$strtAt.getTimeInMillis() < this.val$now) {
                Engine.access$500(Engine.this, (int) (((long) this.val$e.duration()) - (((this.val$now - this.val$strtAt.getTimeInMillis()) / 1000) / 60)));
            } else {
                Engine.access$500(Engine.this, this.val$e.duration());
            }
        }

        public String toString() {
            return "Start emission action: " + Engine.access$300(Engine.this, this.val$strtAt);
        }
    }

    private String calToStr(Calendar c) {
        return "Year: " + c.get(1) + " Month: " + c.get(2) + " Day: " + c.get(5) + " Hour: " + c.get(11) + " Min: " + c.get(12) + " Sec: " + c.get(13) + " Milis: " + c.getTimeInMillis();
    }

    private void notifyFakeEmission() {
        Log.e(TAG, "EMISSION FAKE");
        Logger.d(R.string.message_emission_fake);
        EmissionEventBus.instance().fakeEmission();
    }

    private void notifyEmission(int emissionStartAfter) {
        Log.e(TAG, "EMISSION WILL START");
        Logger.e(R.string.message_emission_approaching);
        EmissionEventBus.instance().emissionWillStart(emissionStartAfter);
    }

    private void startEmission(int endAfter) {
        Log.e(TAG, "EMISSION STARTED");
        Logger.e(R.string.message_emission_started);
        EmissionEventBus.instance().setEmissionActive(true);
        if (this.currentEmission == null) {
            this.currentEmission = new CompositeDisposable();
        }
        this.currentEmission.add(Observable.timer(endAfter, TimeUnit.MINUTES).take(1L).subscribe(new $$Lambda$Engine$EvdVGfSj6F7Acw2rZ6BLJUgtSSc(this)));
    }

    public /* synthetic */ void lambda$startEmission$19$Engine(Long t) {
        emissionEnded();
    }

    private void emissionEnded() {
        CompositeDisposable compositeDisposable = this.currentEmission;
        if (compositeDisposable != null && !compositeDisposable.isDisposed()) {
            this.currentEmission.dispose();
            this.currentEmission = null;
        }
        Logger.d(R.string.message_emission_ended);
        this.influenceProvider.stopEmission();
        EmissionEventBus.instance().setEmissionActive(false);
    }

    static /* synthetic */ boolean lambda$setupSuicides$20(String c) {
        return c == PlayerEventBus.SUICIDE;
    }

    private Disposable setupSuicides() {
        Observable<String> suicideRequests = PlayerEventBus.instance().getPlayerCommandsStream().filter($$Lambda$Engine$Pzt8zykOVA4_MaTa58uKDtqTY.INSTANCE);
        return suicideRequests.withLatestFrom(this.currentPlayerState, $$Lambda$Engine$XWfoFtD1YI17ezb8z4wyazgLSYQ.INSTANCE).observeOn(this.computation).subscribe(new $$Lambda$Engine$a9BgvSDiJxtPc5XwxpIlC9QsfgE(this));
    }

    static /* synthetic */ Player.STATE lambda$setupSuicides$21(String s, Player.STATE cs) {
        return cs;
    }

    public /* synthetic */ void lambda$setupSuicides$22$Engine(Player.STATE cs) {
        if (cs.getSuicideType() != 4) {
            int i = AnonymousClass3.$SwitchMap$net$afterday$compas$core$player$Player$STATE[cs.ordinal()];
            if (i == 1) {
                this.framesStream.onNext(this.game.getPlayer().setState(Player.STATE.W_ABDUCTED));
                return;
            }
            if (i == 2) {
                this.game.getPlayer().setState(Player.STATE.ABDUCTED);
            } else if (i == 3 || i == 4 || i == 5) {
                this.game.getPlayer().setState(Player.STATE.DEAD_BURER);
            }
        }
    }

    /* JADX INFO: renamed from: net.afterday.compas.engine.Engine$3, reason: invalid class name */
    static /* synthetic */ class AnonymousClass3 {
        static final /* synthetic */ int[] $SwitchMap$net$afterday$compas$core$player$Player$STATE = new int[Player.STATE.values().length];

        static {
            try {
                $SwitchMap$net$afterday$compas$core$player$Player$STATE[Player.STATE.ALIVE.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$net$afterday$compas$core$player$Player$STATE[Player.STATE.W_ABDUCTED.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$net$afterday$compas$core$player$Player$STATE[Player.STATE.ABDUCTED.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$net$afterday$compas$core$player$Player$STATE[Player.STATE.CONTROLLED.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$net$afterday$compas$core$player$Player$STATE[Player.STATE.MENTALLED.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
        }
    }

    private Disposable makeCountDownForStates(Player.STATE state1, long time1, Player.STATE state2, long time2, Player.STATE finalState) {
        CompositeDisposable cd = new CompositeDisposable();
        Observable<Long> state1CountDown = makeCountDownFor(state1);
        cd.add(state1CountDown.subscribe(new $$Lambda$Engine$IzKCYl5QkAz87UpbQaW8REz8bSg(this, time1, state2)));
        Observable<Long> state2CountDown = makeCountDownFor(state2);
        cd.add(state2CountDown.subscribe(new $$Lambda$Engine$WzKUzi0AKRep1XW6fjsrvP5vv3w(this, time2, finalState, time1)));
        return cd;
    }

    public /* synthetic */ void lambda$makeCountDownForStates$23$Engine(long time1, Player.STATE state2, Long t) {
        if (t.longValue() <= time1) {
            ((Subject) this.secondsLeft).onNext(Long.valueOf(time1 - t.longValue()));
        } else {
            this.game.getPlayer().setState(state2);
            ((Subject) this.secondsLeft).onNext(Long.valueOf(time1 - t.longValue()));
        }
    }

    public /* synthetic */ void lambda$makeCountDownForStates$24$Engine(long time2, Player.STATE finalState, long time1, Long t) {
        if (t.longValue() <= time2) {
            ((Subject) this.secondsLeft).onNext(Long.valueOf(time2 - t.longValue()));
        } else {
            this.game.getPlayer().setState(finalState);
            ((Subject) this.secondsLeft).onNext(Long.valueOf(time1 - t.longValue()));
        }
    }

    private Observable<Long> countUntil(Observable<Long> stream, long seconds, boolean runnning) {
        if (runnning) {
            return stream.filter(new $$Lambda$Engine$T9F15mCkGbSHf7BlA1zcOU65sCM(seconds));
        }
        return stream.filter(new $$Lambda$Engine$lmWpPli2Kk7WwnBRsMMGiOXX6g(seconds));
    }

    static /* synthetic */ boolean lambda$countUntil$25(long seconds, Long t) {
        return t.longValue() <= seconds;
    }

    static /* synthetic */ boolean lambda$countUntil$26(long seconds, Long t) {
        return t.longValue() > seconds;
    }

    static /* synthetic */ Long lambda$makeCountDownFor$28(Long g, Long s) {
        return Long.valueOf((System.currentTimeMillis() - s.longValue()) / 1000);
    }

    private Observable<Long> makeCountDownFor(Player.STATE state) {
        return this.currentPlayerState.switchMap(new $$Lambda$Engine$v1lSYMYdaroSdxG_gYhdqmyHJRc(this, state)).withLatestFrom(this.countdownStarted, $$Lambda$Engine$0GVlRiZBJ7kQYFGAsP0iukBD3w.INSTANCE);
    }

    public /* synthetic */ ObservableSource lambda$makeCountDownFor$27$Engine(Player.STATE state, Player.STATE ps) {
        return ps == state ? this.gameRunning : Observable.empty();
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public void setSensorsProvider(SensorsProvider sensorsProvider) {
        this.sensorsProvider = sensorsProvider;
    }

    public void setPersistencyProvider(PersistencyProvider provider) {
        this.persistencyProvider = provider;
    }

    public void setDeviceProvider(DeviceProvider devideProvider) {
        this.deviceProvider = devideProvider;
    }

    public Observable<Player.STATE> getPlayerStateStream() {
        return this.currentPlayerState;
    }

    public Observable<ItemAdded> getItemAddedStream() {
        return this.itemAdded;
    }

    public static Engine instance() {
        if (instance == null) {
            instance = new Engine();
        }
        return instance;
    }

    public Observable<Long> getCountDownStream() {
        return this.secondsLeft;
    }

    public Observable<Integer> getPlayerLevelStream() {
        return this.playerLevel;
    }

    @Override // net.afterday.compas.core.serialization.Jsonable
    public JsonObject toJson() {
        return this.o;
    }

    private class ControlsImpl implements Controls {
        private ControlsImpl() {
        }

        /* synthetic */ ControlsImpl(Engine x0, Object x1) {
            this();
        }

        @Override // net.afterday.compas.core.Controls
        public void stopInfluences() {
            Engine.access$600(Engine.this).onNext(false);
            Engine.access$700(Engine.this).onNext(false);
        }

        @Override // net.afterday.compas.core.Controls
        public void startInfluences() {
            Engine.access$600(Engine.this).onNext(true);
            Engine.access$700(Engine.this).onNext(true);
        }
    }

    private class PlayerEventsListenerImpl implements PlayerEventsListener {
        private PlayerEventsListenerImpl() {
        }

        /* synthetic */ PlayerEventsListenerImpl(Engine x0, Object x1) {
            this();
        }

        @Override // net.afterday.compas.core.events.PlayerEventsListener
        public void onPlayerStateChanged(Player.STATE oldState, Player.STATE newState) {
            Log.d(Engine.TAG, "onPlayerStateChanged " + newState);
            if (newState == Player.STATE.W_DEAD_BURER) {
                Engine.access$800(Engine.this).onNext(Long.valueOf(System.currentTimeMillis()));
                Logger.d(R.string.message_suicide);
                Engine.access$900(Engine.this).onNext(newState);
                Engine.access$1000(Engine.this).stopInfluences();
                return;
            }
            if (newState == Player.STATE.DEAD_BURER) {
                ((Subject) Engine.access$1100(Engine.this)).onNext(-1L);
                Logger.e(R.string.message_dead);
                Engine.access$900(Engine.this).onNext(newState);
                Engine.access$1000(Engine.this).startInfluences();
                return;
            }
            if (newState == Player.STATE.W_CONTROLLED) {
                Engine.access$800(Engine.this).onNext(Long.valueOf(System.currentTimeMillis()));
                Logger.d(R.string.message_controller_trans);
                Engine.access$900(Engine.this).onNext(newState);
                Engine.access$1000(Engine.this).stopInfluences();
                return;
            }
            if (newState == Player.STATE.CONTROLLED) {
                Engine.access$800(Engine.this).onNext(Long.valueOf(System.currentTimeMillis()));
                Logger.e(R.string.message_controller_undercontrol);
                Engine.access$900(Engine.this).onNext(newState);
                Engine.access$1000(Engine.this).stopInfluences();
                return;
            }
            if (newState == Player.STATE.DEAD_CONTROLLER) {
                ((Subject) Engine.access$1100(Engine.this)).onNext(-1L);
                Logger.e(R.string.message_dead_controller);
                Engine.access$900(Engine.this).onNext(Player.STATE.DEAD_CONTROLLER);
                Engine.access$1000(Engine.this).startInfluences();
                return;
            }
            if (newState == Player.STATE.W_MENTALLED) {
                Engine.access$800(Engine.this).onNext(Long.valueOf(System.currentTimeMillis()));
                Logger.d(R.string.message_mental_trans);
                Engine.access$900(Engine.this).onNext(newState);
                Engine.access$1000(Engine.this).stopInfluences();
                return;
            }
            if (newState == Player.STATE.MENTALLED) {
                Engine.access$800(Engine.this).onNext(Long.valueOf(System.currentTimeMillis()));
                Logger.e(R.string.message_mental_zombified);
                Engine.access$900(Engine.this).onNext(newState);
                Engine.access$1000(Engine.this).stopInfluences();
                return;
            }
            if (newState == Player.STATE.DEAD_MENTAL) {
                Logger.e(R.string.message_dead_mental);
                Engine.access$900(Engine.this).onNext(Player.STATE.DEAD_MENTAL);
                Engine.access$1000(Engine.this).startInfluences();
                return;
            }
            if (newState == Player.STATE.W_DEAD_ANOMALY) {
                Engine.access$800(Engine.this).onNext(Long.valueOf(System.currentTimeMillis()));
                Logger.d(R.string.message_suicide);
                Engine.access$900(Engine.this).onNext(Player.STATE.W_DEAD_ANOMALY);
                Engine.access$1000(Engine.this).stopInfluences();
                return;
            }
            if (newState == Player.STATE.DEAD_ANOMALY) {
                Logger.e(R.string.message_dead_anomaly);
                Engine.access$900(Engine.this).onNext(Player.STATE.DEAD_ANOMALY);
                Engine.access$1000(Engine.this).startInfluences();
                return;
            }
            if (newState == Player.STATE.ALIVE) {
                Logger.d(R.string.message_revive);
                Engine.access$900(Engine.this).onNext(Player.STATE.ALIVE);
                ((Subject) Engine.access$1100(Engine.this)).onNext(-1L);
                Engine.access$1000(Engine.this).startInfluences();
                return;
            }
            if (newState == Player.STATE.W_DEAD_RADIATION) {
                Engine.access$800(Engine.this).onNext(Long.valueOf(System.currentTimeMillis()));
                Logger.d(R.string.message_suicide);
                Engine.access$900(Engine.this).onNext(Player.STATE.W_DEAD_RADIATION);
                Engine.access$1000(Engine.this).stopInfluences();
                return;
            }
            if (newState == Player.STATE.DEAD_RADIATION) {
                Logger.e(R.string.message_dead_radiation);
                Engine.access$900(Engine.this).onNext(Player.STATE.DEAD_RADIATION);
                Engine.access$1000(Engine.this).startInfluences();
                return;
            }
            if (newState == Player.STATE.W_ABDUCTED) {
                Engine.access$800(Engine.this).onNext(Long.valueOf(System.currentTimeMillis()));
                Logger.d(R.string.message_suicide);
                Engine.access$900(Engine.this).onNext(Player.STATE.W_ABDUCTED);
                Engine.access$1000(Engine.this).stopInfluences();
                return;
            }
            if (newState == Player.STATE.ABDUCTED) {
                Engine.access$1200(Engine.this).getPlayer().getPlayerProps().addHealth(1.0d);
                Engine.access$800(Engine.this).onNext(Long.valueOf(System.currentTimeMillis()));
                Logger.e(R.string.message_abducted);
                Engine.access$900(Engine.this).onNext(Player.STATE.ABDUCTED);
                Engine.access$1000(Engine.this).startInfluences();
                return;
            }
            if (newState == Player.STATE.DEAD_EMISSION) {
                ((Subject) Engine.access$1100(Engine.this)).onNext(-1L);
                Logger.e(R.string.message_dead_emission);
                Engine.access$900(Engine.this).onNext(newState);
                Engine.access$1000(Engine.this).startInfluences();
            }
        }

        @Override // net.afterday.compas.core.events.PlayerEventsListener
        public void onImpactsStateChanged(Impacts.STATE oldState, Impacts.STATE newState) {
            ((Subject) Engine.access$1300(Engine.this)).onNext(newState);
        }

        @Override // net.afterday.compas.core.events.PlayerEventsListener
        public void onItemAdded(ItemAdded itemAdded) {
            ItemEventsBus.instance().itemAdded(itemAdded.getItem());
            Engine.access$1400(Engine.this).onNext(itemAdded);
            Logger.logItemAdded(itemAdded);
        }

        @Override // net.afterday.compas.core.events.PlayerEventsListener
        public void onItemUsed(Item item) {
            ItemEventsBus.instance().itemUsed(item);
            Logger.logItemUsed(item);
        }

        @Override // net.afterday.compas.core.events.PlayerEventsListener
        public void onItemDropped(Item item) {
            ItemEventsBus.instance().itemDropped(item);
            Logger.logItemDropped(item);
        }

        @Override // net.afterday.compas.core.events.PlayerEventsListener
        public void onFractionChanged(Player.FRACTION newFraction, Player.FRACTION oldFraction) {
            Logger.d("Fraction changed to " + newFraction.toString());
            Engine.access$1500(Engine.this).onNext(newFraction);
        }

        @Override // net.afterday.compas.core.events.PlayerEventsListener
        public void onPlayerLevelChanged(int level) {
            Engine.access$1600(Engine.this).onNext(Integer.valueOf(level));
        }
    }
}
