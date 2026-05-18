package net.afterday.compas.devices.vibro;

import android.content.Context;
import android.os.Vibrator;
import android.support.v4.util.Pair;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import java.util.concurrent.TimeUnit;
import net.afterday.compas.core.player.Player;
import net.afterday.compas.core.player.PlayerProps;
import net.afterday.compas.settings.Constants;
import net.afterday.compas.settings.Settings;
import net.afterday.compas.settings.SettingsListener;

/* JADX INFO: loaded from: classes.dex */
public class VibroImpl implements Vibro {
    private SettingsListener listener;
    private Vibro off = new VibroOff(null);
    private Vibro on;
    private Settings settings;
    private Vibro strategy;

    public VibroImpl(Context context) {
        this.on = new VibroOn(context);
        this.settings = Settings.instance(context);
        this.strategy = this.settings.getBoolSetting(Constants.VIBRATION) ? this.on : this.off;
        this.listener = new $$Lambda$VibroImpl$z3vvsh6UTKeiXMgGf6gtScAHxo(this);
        this.settings.addSettingsListener(this.listener);
    }

    public /* synthetic */ void lambda$new$0$VibroImpl(String key, String val) {
        if (((key.hashCode() == -1590230414 && key.equals(Constants.VIBRATION)) ? (byte) 0 : (byte) -1) == 0) {
            this.strategy = Boolean.parseBoolean(val) ? this.on : this.off;
        }
    }

    @Override // net.afterday.compas.devices.vibro.Vibro
    public void vibrateDamage(PlayerProps playerProps) {
        this.strategy.vibrateDamage(playerProps);
    }

    @Override // net.afterday.compas.devices.vibro.Vibro
    public void vibrateHit() {
        this.strategy.vibrateHit();
    }

    @Override // net.afterday.compas.devices.vibro.Vibro
    public void vibrateW() {
        this.strategy.vibrateW();
    }

    @Override // net.afterday.compas.devices.vibro.Vibro
    public void vibrateDeath() {
        this.strategy.vibrateDeath();
    }

    @Override // net.afterday.compas.devices.vibro.Vibro
    public void vibrateMessage() {
        this.strategy.vibrateMessage();
    }

    @Override // net.afterday.compas.devices.vibro.Vibro
    public void vibrateAlarm() {
        this.strategy.vibrateAlarm();
    }

    @Override // net.afterday.compas.devices.vibro.Vibro
    public void vibrateTouch() {
        this.strategy.vibrateTouch();
    }

    /* JADX INFO: Access modifiers changed from: private */
    static class VibroOn implements Vibro {
        private Disposable p1;
        private Disposable p2;
        private Vibrator vibrator;
        private long lVmed = 0;
        private long lVmax = 0;
        private int vibroPriority = 0;

        public VibroOn(Context ctx) {
            this.vibrator = (Vibrator) ctx.getSystemService("vibrator");
        }

        @Override // net.afterday.compas.devices.vibro.Vibro
        public void vibrateDamage(PlayerProps playerProps) {
            if (playerProps.getState().getCode() == 1 && this.vibroPriority == 0) {
                boolean vibrated = _vibrateDmg(getMax(playerProps), playerProps);
                if (!vibrated) {
                    this.lVmax = 0L;
                    this.lVmed = 0L;
                }
            }
        }

        @Override // net.afterday.compas.devices.vibro.Vibro
        public void vibrateHit() {
            if (this.vibroPriority < 1) {
                this.vibroPriority = 1;
                this.vibrator.cancel();
                this.p1 = Observable.interval(150L, TimeUnit.MILLISECONDS).take(10L).subscribe(new $$Lambda$VibroImpl$VibroOn$WOqKippJetfdzOCJKW0jHv1a4Xg(this), $$Lambda$VibroImpl$VibroOn$gSuDkHMtlf5pVlDSxb9ceD4nfM.INSTANCE, new $$Lambda$VibroImpl$VibroOn$wWJjdV_sAT1mtcUkz7VtCCnk6DY(this));
            }
        }

        static /* synthetic */ void lambda$vibrateHit$1(Throwable e) {
        }

        public /* synthetic */ void lambda$vibrateHit$0$VibroImpl$VibroOn(Long t) {
            this.vibrator.vibrate(50L);
        }

        public /* synthetic */ void lambda$vibrateHit$2$VibroImpl$VibroOn() {
            this.vibroPriority = 0;
        }

        @Override // net.afterday.compas.devices.vibro.Vibro
        public void vibrateW() {
            if (this.vibroPriority < 2) {
                this.vibroPriority = 2;
                Disposable disposable = this.p1;
                if (disposable != null && !disposable.isDisposed()) {
                    this.p1.dispose();
                }
                this.vibrator.cancel();
                this.vibrator.vibrate(1000L);
                Observable.timer(5L, TimeUnit.SECONDS).subscribe(new $$Lambda$VibroImpl$VibroOn$IrU8uk2IuDBu1V7aYlcxUBVhA6Y(this));
            }
        }

        public /* synthetic */ void lambda$vibrateW$3$VibroImpl$VibroOn(Long t) {
            this.vibroPriority = 0;
        }

        @Override // net.afterday.compas.devices.vibro.Vibro
        public void vibrateDeath() {
            if (this.vibroPriority < 2) {
                this.vibroPriority = 2;
                Disposable disposable = this.p1;
                if (disposable != null && !disposable.isDisposed()) {
                    this.p1.dispose();
                }
                this.vibrator.cancel();
                this.vibrator.vibrate(2000L);
                Observable.timer(5L, TimeUnit.SECONDS).subscribe(new $$Lambda$VibroImpl$VibroOn$ThKhv9iVAuUrYwYFykfVU1hpLU(this));
            }
        }

        public /* synthetic */ void lambda$vibrateDeath$4$VibroImpl$VibroOn(Long t) {
            this.vibroPriority = 0;
        }

        public /* synthetic */ void lambda$vibrateMessage$5$VibroImpl$VibroOn(Long m) {
            this.vibrator.vibrate(30L);
        }

        @Override // net.afterday.compas.devices.vibro.Vibro
        public void vibrateMessage() {
            Observable.interval(1L, TimeUnit.SECONDS).take(10L).subscribe(new $$Lambda$VibroImpl$VibroOn$bRmi6P8qxO5yrOeMFCDOmcsOJek(this));
        }

        public /* synthetic */ void lambda$vibrateTouch$6$VibroImpl$VibroOn(Long m) {
            this.vibrator.vibrate(30L);
        }

        @Override // net.afterday.compas.devices.vibro.Vibro
        public void vibrateTouch() {
            Observable.interval(0L, TimeUnit.SECONDS).take(1L).subscribe(new $$Lambda$VibroImpl$VibroOn$2zXoEzRva2l8AtUTcADzDUpKmsE(this));
        }

        public /* synthetic */ void lambda$vibrateAlarm$7$VibroImpl$VibroOn(Long m) {
            this.vibrator.vibrate(50L);
        }

        @Override // net.afterday.compas.devices.vibro.Vibro
        public void vibrateAlarm() {
            Observable.interval(1L, TimeUnit.SECONDS).take(20L).subscribe(new $$Lambda$VibroImpl$VibroOn$jLQzPUp5yisp5sGuPiHHQNqg_0(this));
        }

        private Pair<Integer, Double> getMax(PlayerProps playerProps) {
            double[] impacts = playerProps.getImpacts();
            double max = 0.0d;
            int infl = 0;
            if (impacts == null) {
                return new Pair<>(0, Double.valueOf(0.0d));
            }
            int level = playerProps.getLevel();
            for (int i = 0; i < impacts.length; i++) {
                double s = impacts[i];
                if (i != 0) {
                    if (i != 1) {
                        if (i != 2) {
                            if ((i == 3 || i == 4) && s > max && level >= 4) {
                                max = s;
                                infl = i;
                            }
                        } else if (s > max && level >= 3) {
                            max = s;
                            infl = i;
                        }
                    } else if (s > max && level >= 2) {
                        max = s;
                        infl = i;
                    }
                } else if (s > max) {
                    max = s;
                    infl = i;
                }
            }
            return new Pair<>(Integer.valueOf(infl), Double.valueOf(max));
        }

        private boolean _vibrateDmg(Pair<Integer, Double> infl, PlayerProps playerProps) {
            double strength = infl.second.doubleValue();
            if (strength <= 0.0d || ((infl.first.intValue() == 2 && playerProps.getFraction() == Player.FRACTION.MONOLITH) || strength <= 0.0d || (infl.first.intValue() == 0 && playerProps.getFraction() == Player.FRACTION.DARKEN))) {
                return false;
            }
            if (strength >= 16.0d) {
                if (this.lVmax == 0) {
                    this.vibrator.cancel();
                    this.vibrator.vibrate(100L);
                    this.lVmax = System.currentTimeMillis();
                    return true;
                }
                if (System.currentTimeMillis() - this.lVmax >= 5000) {
                    return false;
                }
                this.vibrator.cancel();
                this.vibrator.vibrate(100L);
                this.lVmax = System.currentTimeMillis();
                return true;
            }
            if (strength < 7.0d) {
                return false;
            }
            if (this.lVmed == 0) {
                this.vibrator.cancel();
                this.vibrator.vibrate(25L);
                this.lVmed = System.currentTimeMillis();
                return true;
            }
            if (System.currentTimeMillis() - this.lVmed >= 10000) {
                return false;
            }
            this.vibrator.cancel();
            this.vibrator.vibrate(25L);
            this.lVmed = System.currentTimeMillis();
            return true;
        }

        private long[] p(long a, long b) {
            return new long[]{a, b};
        }
    }

    private static class VibroOff implements Vibro {
        private VibroOff() {
        }

        /* synthetic */ VibroOff(Object x0) {
            this();
        }

        @Override // net.afterday.compas.devices.vibro.Vibro
        public void vibrateDamage(PlayerProps playerProps) {
        }

        @Override // net.afterday.compas.devices.vibro.Vibro
        public void vibrateHit() {
        }

        @Override // net.afterday.compas.devices.vibro.Vibro
        public void vibrateW() {
        }

        @Override // net.afterday.compas.devices.vibro.Vibro
        public void vibrateDeath() {
        }

        @Override // net.afterday.compas.devices.vibro.Vibro
        public void vibrateMessage() {
        }

        @Override // net.afterday.compas.devices.vibro.Vibro
        public void vibrateAlarm() {
        }

        @Override // net.afterday.compas.devices.vibro.Vibro
        public void vibrateTouch() {
        }
    }
}
