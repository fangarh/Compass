package net.afterday.compas.effects;

import android.animation.ValueAnimator;
import android.os.HandlerThread;
import android.support.v4.util.Pair;
import android.view.animation.LinearInterpolator;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import net.afterday.compas.LocalMainService;
import net.afterday.compas.R;
import net.afterday.compas.core.gameState.Frame;
import net.afterday.compas.core.inventory.items.Item;
import net.afterday.compas.core.player.Impacts;
import net.afterday.compas.core.player.Player;
import net.afterday.compas.core.player.PlayerProps;
import net.afterday.compas.devices.DeviceProvider;
import net.afterday.compas.devices.sound.Sound;
import net.afterday.compas.devices.vibro.Vibro;
import net.afterday.compas.engine.events.EmissionEventBus;
import net.afterday.compas.engine.events.ItemEventsBus;
import net.afterday.compas.engine.events.PlayerEventBus;
import net.afterday.compas.logging.Logger;

/* JADX INFO: loaded from: classes.dex */
public class Effects {
    private static final String TAG = "Effects";
    private Disposable anomalySubsciption;
    private Disposable artefactSubsciption;
    private Player.STATE currentState;
    private DeviceProvider deviceProvider;
    private Disposable impactsStatesSubscription;
    private Disposable playerLevelSubscription;
    private Disposable playerStatesSubsciption;
    protected Runnable radPlayer;
    private Scheduler scheduler;
    private Sound sound;
    private ValueAnimator soundAnimator;
    private Vibro vibro;
    private Timer anomalyTimer = new Timer(true);
    private TimerTask playAnomalyClick = new AnonymousClass1();

    static /* synthetic */ Sound access$000(Effects x0) {
        return x0.sound;
    }

    /* JADX INFO: renamed from: net.afterday.compas.effects.Effects$1, reason: invalid class name */
    class AnonymousClass1 extends TimerTask {
        AnonymousClass1() {
        }

        @Override // java.util.TimerTask, java.lang.Runnable
        public void run() {
            Effects.access$000(Effects.this).playAnomalyClick();
        }
    }

    public Effects(DeviceProvider deviceProvider) {
        this.deviceProvider = deviceProvider;
        this.sound = deviceProvider.getSoundPlayer();
        this.vibro = deviceProvider.getVibrator();
        HandlerThread handlerThread = new HandlerThread("backgroundThread");
        handlerThread.start();
        this.scheduler = AndroidSchedulers.from(handlerThread.getLooper());
        LocalMainService.getInstance().getFramesStream().observeOn(this.scheduler).subscribe(new $$Lambda$Effects$UXY0CzwcyxBn4ShtUWrTD8ZQcSQ(this));
        ItemEventsBus.instance().getItemAddedEvents().subscribe(new $$Lambda$Effects$4BT456oRc9KO5cveIGSNIvF4pWM(this));
        ItemEventsBus.instance().getItemUsedEvents().subscribe(new $$Lambda$Effects$Z4rx_K76D6UK5eyh2JaG6MvkNo4(this));
        ItemEventsBus.instance().getItemDroppedEvents().subscribe(new $$Lambda$Effects$jrSFNE8UEAWxElyolB6f4N4b6A(this));
        EmissionEventBus.instance().getEmissionStateStream().skip(1L).withLatestFrom(PlayerEventBus.instance().getPlayerStateStream(), $$Lambda$Effects$Jq_nyWoTBK4kTaXqRa0sqDxCxo.INSTANCE).subscribe(new $$Lambda$Effects$pmALRo1sT7ieE5H15WyG8FySAys(this));
        EmissionEventBus.instance().getEmissionWarnings().withLatestFrom(PlayerEventBus.instance().getPlayerStateStream(), $$Lambda$Effects$W9TAd6Oznk80dJstGPZjsXVzY0s.INSTANCE).subscribe(new $$Lambda$Effects$d8D6fiMOlEGA7x9Jxcyz4x0TiU(this));
        EmissionEventBus.instance().getFakeEmissions().withLatestFrom(PlayerEventBus.instance().getPlayerStateStream(), $$Lambda$Effects$Ne6f5pKnjzvNMZBgvP7xxUMhjE.INSTANCE).subscribe(new $$Lambda$Effects$rwC0GzvM1qE0ekLrd1dd2KRoY(this));
    }

    public /* synthetic */ void lambda$new$0$Effects(Frame f) {
        processFrame(f);
    }

    public /* synthetic */ void lambda$new$1$Effects(Item ia) {
        this.sound.playItemScanned();
        this.vibro.vibrateTouch();
    }

    public /* synthetic */ void lambda$new$2$Effects(Item ia) {
        this.sound.playInventoryUse();
        this.vibro.vibrateTouch();
    }

    public /* synthetic */ void lambda$new$3$Effects(Item ia) {
        this.sound.playInventoryDrop();
        this.vibro.vibrateTouch();
    }

    static /* synthetic */ Pair lambda$new$4(Boolean e, Player.STATE ps) {
        return new Pair(e, ps);
    }

    /* JADX WARN: Multi-variable type inference failed */
    public /* synthetic */ void lambda$new$5$Effects(Pair s) {
        if (((Player.STATE) s.second).getCode() == 1) {
            if (((Boolean) s.first).booleanValue()) {
                this.sound.playEmissionStarts();
                this.vibro.vibrateAlarm();
            } else {
                this.sound.playEmissionEnds();
                this.vibro.vibrateMessage();
            }
        }
    }

    static /* synthetic */ Pair lambda$new$6(Integer e, Player.STATE ps) {
        return new Pair(e, ps);
    }

    /* JADX WARN: Multi-variable type inference failed */
    public /* synthetic */ void lambda$new$7$Effects(Pair s) {
        if (((Player.STATE) s.second).getCode() == 1) {
            this.sound.playEmissionWarning();
            this.vibro.vibrateMessage();
        }
    }

    static /* synthetic */ Pair lambda$new$8(Integer e, Player.STATE ps) {
        return new Pair(e, ps);
    }

    /* JADX WARN: Multi-variable type inference failed */
    public /* synthetic */ void lambda$new$9$Effects(Pair s) {
        if (((Player.STATE) s.second).getCode() == 1) {
            this.sound.playEmissionEnds();
            this.vibro.vibrateMessage();
        }
    }

    public void setPlayerStatesStream(Observable<Player.STATE> statesStream) {
        Disposable disposable = this.playerStatesSubsciption;
        if (disposable != null && !disposable.isDisposed()) {
            this.playerStatesSubsciption.dispose();
        }
        this.playerStatesSubsciption = statesStream.observeOn(this.scheduler).subscribe(new $$Lambda$Effects$gOmgxAgQ8hLdgiCcO1Emv0KwqRU(this));
    }

    public /* synthetic */ void lambda$setPlayerStatesStream$10$Effects(Player.STATE s) {
        switch (s) {
            case MENTALLED:
                this.sound.playZombify();
                this.vibro.vibrateHit();
                break;
            case CONTROLLED:
                this.sound.playControlled();
                this.vibro.vibrateHit();
                break;
            case ABDUCTED:
                this.sound.playAbducted();
                this.vibro.vibrateHit();
                break;
            case DEAD_BURER:
                this.sound.playBulbBreak();
                this.sound.playDeath();
                this.vibro.vibrateDeath();
                break;
            case DEAD_MENTAL:
                this.sound.playBulbBreak();
                this.sound.playDeath();
                this.vibro.vibrateDeath();
                break;
            case W_ABDUCTED:
                this.sound.playWTimer();
                this.vibro.vibrateW();
                break;
            case W_MENTALLED:
                this.sound.playMentalHit();
                this.sound.playTransmutating();
                this.vibro.vibrateW();
                break;
            case W_DEAD_ANOMALY:
                this.sound.playAnomalyDeath();
                this.sound.playWTimer();
                this.vibro.vibrateW();
                break;
            case DEAD_ANOMALY:
                this.sound.playBulbBreak();
                this.sound.playDeath();
                this.vibro.vibrateDeath();
                break;
            case W_CONTROLLED:
                this.sound.playController();
                this.sound.playTransmutating();
                this.vibro.vibrateW();
                break;
            case W_DEAD_BURER:
                this.sound.playBurer();
                this.sound.playWTimer();
                this.vibro.vibrateW();
                break;
            case DEAD_CONTROLLER:
                this.sound.playBulbBreak();
                this.sound.playDeath();
                this.vibro.vibrateDeath();
                break;
            case W_DEAD_RADIATION:
                this.sound.playWTimer();
                this.vibro.vibrateW();
                break;
            case DEAD_RADIATION:
                this.sound.playBulbBreak();
                this.sound.playDeath();
                this.vibro.vibrateDeath();
                break;
            case DEAD_EMISSION:
                this.sound.playEmissionHit();
                this.sound.playBulbBreak();
                this.sound.playDeath();
                this.vibro.vibrateDeath();
                break;
        }
    }

    public void setImpactsStatesStream(Observable<Impacts.STATE> impactsStatesStream) {
    }

    public void setPlayerLevelStream(Observable<Integer> playerLevelStream) {
        Disposable disposable = this.playerLevelSubscription;
        if (disposable != null && !disposable.isDisposed()) {
            this.playerLevelSubscription.dispose();
        }
        this.playerLevelSubscription = playerLevelStream.subscribe(new $$Lambda$Effects$2fnnViru4uhMjfEYAp6pIdk8Apk(this));
    }

    public /* synthetic */ void lambda$setPlayerLevelStream$11$Effects(Integer l) {
        if (l.intValue() == 1) {
            this.sound.playLevelUp();
            Logger.d(R.string.message_level_1);
        }
        if (l.intValue() == 2) {
            this.sound.playLevelUp();
            Logger.d(R.string.message_level_2);
        }
        if (l.intValue() == 3) {
            this.sound.playLevelUp();
            Logger.d(R.string.message_level_3);
        }
        if (l.intValue() == 4) {
            this.sound.playLevelUp();
            Logger.d(R.string.message_level_4);
        }
        if (l.intValue() == 5) {
            this.sound.playGlassBreak();
            Logger.d(R.string.message_level_max);
        }
    }

    private void processFrame(Frame frame) {
        PlayerProps playerProps = frame.getPlayerProps();
        if (playerProps.getState().getCode() != 1 || playerProps.getHealthImpact() > 0.0d) {
            return;
        }
        this.soundAnimator = ValueAnimator.ofInt(0, 60);
        this.vibro.vibrateDamage(frame.getPlayerProps());
        playHits(playerProps);
        playRad(playerProps);
        playController(playerProps);
        playAnomaly(playerProps);
        playArtefact(playerProps);
        playMental(playerProps);
        playBurer(playerProps);
    }

    private void playHits(PlayerProps playerProps) {
        boolean hasHit = false;
        if (playerProps.anomalyHit()) {
            hasHit = true;
            this.sound.playAnomalyDeath();
            Logger.e(R.string.message_anomaly_hit);
        }
        if (playerProps.burerHit()) {
            hasHit = true;
            this.sound.playBurer();
            Logger.e(R.string.message_burer_hit);
        }
        if (playerProps.controllerHit()) {
            hasHit = true;
            this.sound.playController();
            Logger.e(R.string.message_controller_hit);
        }
        if (playerProps.mentalHit()) {
            hasHit = true;
            this.sound.playMentalHit();
            Logger.e(R.string.message_mental_hit);
        }
        if (playerProps.monolithHit()) {
            hasHit = true;
            this.sound.playMonolithHit();
            Logger.d(R.string.message_monolit_call);
        }
        if (playerProps.emissionHit()) {
            hasHit = true;
            this.sound.playEmissionHit();
            Logger.e(R.string.message_emission_hit);
        }
        if (hasHit) {
            if (!playerProps.mentalHit() || playerProps.getFraction() != Player.FRACTION.MONOLITH) {
                this.vibro.vibrateHit();
            }
        }
    }

    private void playRad(PlayerProps playerProps) {
        double strength = playerProps.getRadiationImpact();
        if (strength == 0.0d) {
            return;
        }
        this.soundAnimator.cancel();
        this.soundAnimator.setDuration(1000L);
        this.soundAnimator.setInterpolator(new LinearInterpolator());
        this.soundAnimator.addUpdateListener(new $$Lambda$Effects$nNREGxqTz0Pr8UayDCmukZmok4(this, strength));
        this.soundAnimator.start();
    }

    public /* synthetic */ void lambda$playRad$12$Effects(double strength, ValueAnimator v) {
        double rand = Math.random();
        double probability = Math.min(strength / 17.0d, 0.9d);
        if (rand <= probability) {
            this.sound.playRadClick();
        }
    }

    private void playMental(PlayerProps playerProps) {
        if (playerProps.getMentalImpact() > 0.0d && playerProps.getFraction() != Player.FRACTION.MONOLITH && playerProps.getLevel() >= 3) {
            this.sound.playMental();
        }
    }

    private void playBurer(PlayerProps playerProps) {
        if (playerProps.getBurerImpact() <= 0.0d) {
            return;
        }
        playerProps.getBurerImpact();
        int level = playerProps.getLevel();
        if (level >= 4) {
            this.sound.playBurerPresence();
        } else {
            playUnknown(playerProps);
        }
    }

    private void playController(PlayerProps playerProps) {
        if (playerProps.getControllerImpact() <= 0.0d) {
            return;
        }
        playerProps.getControllerImpact();
        int level = playerProps.getLevel();
        if (level >= 4) {
            this.sound.playControllerPresence();
        } else {
            playUnknown(playerProps);
        }
    }

    private void playAnomaly(PlayerProps playerProps) {
        if (playerProps.getAnomalyImpact() <= 0.0d) {
            return;
        }
        int level = playerProps.getLevel();
        double strength = playerProps.getAnomalyImpact();
        if (level >= 2 && playerProps.getState().getCode() == 1) {
            Disposable disposable = this.anomalySubsciption;
            if (disposable != null && !disposable.isDisposed()) {
                this.anomalySubsciption.dispose();
            }
            int to = Math.max(100, 1000 / ((int) Math.ceil(strength)));
            this.anomalySubsciption = Observable.interval(to, TimeUnit.MILLISECONDS).take(1000L, TimeUnit.MILLISECONDS).subscribe(new $$Lambda$Effects$d6R83TYfCnV_SiLsNz5erUD0Ks(this));
            return;
        }
        playUnknown(playerProps);
    }

    public /* synthetic */ void lambda$playAnomaly$13$Effects(Long t) {
        this.sound.playAnomalyClick();
    }

    private void playArtefact(PlayerProps playerProps) {
        if (playerProps.getArtefactImpact() <= 0.0d) {
            return;
        }
        int level = playerProps.getLevel();
        double strength = playerProps.getArtefactImpact();
        if (level >= 5 && playerProps.getState().getCode() == 1) {
            Disposable disposable = this.artefactSubsciption;
            if (disposable != null && !disposable.isDisposed()) {
                this.artefactSubsciption.dispose();
            }
            int to = Math.max(1000, 1000 / ((int) Math.ceil(strength)));
            this.artefactSubsciption = Observable.interval(to, TimeUnit.MILLISECONDS).take(1000L, TimeUnit.MILLISECONDS).subscribe(new $$Lambda$Effects$E3Ztw45gmsMX4Uq9R5jrOEPFa5I(this));
        }
    }

    public /* synthetic */ void lambda$playArtefact$14$Effects(Long t) {
        this.sound.playArtefact();
    }

    private void playUnknown(PlayerProps playerProps) {
    }
}
