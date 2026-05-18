package net.afterday.compas.devices.sound;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.net.Uri;
import net.afterday.compas.R;

/* JADX INFO: loaded from: classes.dex */
public class Sound {
    private static final String TAG = "SOUND";
    private boolean burrerPlaying;
    private boolean controllerPlaying;
    private Context ctx;
    private int mAbducted;
    private int mAnomalyDeath;
    private int mAnomalyTick;
    private int mArtefact;
    private AudioManager mAudioManager;
    private int mBulbBreak;
    private int mBurer;
    private int mBurerPresence;
    private int mCompassOff;
    private int mCompassOn;
    private int mControl;
    private int mController;
    private int mControllerPresence;
    private int mDie;
    private int mEmissionEnds;
    private int mEmissionHit;
    private int mEmissionPeriodical;
    private int mEmissionStarts;
    private int mEmissionWarning;
    private int mGlassBreak;
    private int mHealing;
    private int mInventoryClose;
    private int mInventoryDrop;
    private int mInventoryOpen;
    private int mInventoryUse;
    private int mItemScanned;
    private int mLevelUp;
    private int mMental;
    private int mMentalHit;
    private int mMonolithHit;
    private int mPdaOff;
    private int mPdaOn;
    private int mPdaWake;
    private int mRadTick;
    private int mTransmutating;
    private int mWTimer;
    private int mZombify;
    private String pckg;
    private MediaPlayer burrerPlayer = new MediaPlayer();
    private MediaPlayer controllerPlayer = new MediaPlayer();
    private int currentPlaying = -1;
    private long hStarted = 0;
    private SoundPool mSoundPool = new SoundPool(16, 3, 32);

    public Sound(Context ctx) {
        this.ctx = ctx;
        this.pckg = ctx.getPackageName();
        this.mAudioManager = (AudioManager) ctx.getSystemService("audio");
        preparePlayer(this.burrerPlayer, R.raw.burer_presence, new $$Lambda$Sound$QVEixm6hMaWrHSiS2t70eeKA13Y(this));
        preparePlayer(this.controllerPlayer, R.raw.controller_presence, new $$Lambda$Sound$52wvsPLZtDZxY9vIGmq9xocCtI(this));
        this.mRadTick = this.mSoundPool.load(ctx, R.raw.rad_click, 1);
        this.mAnomalyTick = this.mSoundPool.load(ctx, R.raw.anomaly, 1);
        this.mMental = this.mSoundPool.load(ctx, R.raw.mental, 1);
        this.mEmissionStarts = this.mSoundPool.load(ctx, R.raw.pda_emission_begins, 1);
        this.mEmissionWarning = this.mSoundPool.load(ctx, R.raw.pda_emission_warning, 1);
        this.mEmissionHit = this.mSoundPool.load(ctx, R.raw.emission_hit, 1);
        this.mEmissionPeriodical = this.mSoundPool.load(ctx, R.raw.emission_periodical, 1);
        this.mEmissionEnds = this.mSoundPool.load(ctx, R.raw.pda_emission_ends, 1);
        this.mAnomalyDeath = this.mSoundPool.load(ctx, R.raw.ano_kill, 1);
        this.mDie = this.mSoundPool.load(ctx, R.raw.die, 1);
        this.mZombify = this.mSoundPool.load(ctx, R.raw.zombified, 1);
        this.mControl = this.mSoundPool.load(ctx, R.raw.controlled, 1);
        this.mBurer = this.mSoundPool.load(ctx, R.raw.burer, 1);
        this.mController = this.mSoundPool.load(ctx, R.raw.controller, 1);
        this.mControllerPresence = this.mSoundPool.load(ctx, R.raw.controller_presence, 1);
        this.mHealing = this.mSoundPool.load(ctx, R.raw.healing, 1);
        this.mGlassBreak = this.mSoundPool.load(ctx, R.raw.glass_break, 1);
        this.mBulbBreak = this.mSoundPool.load(ctx, R.raw.bulb_break, 1);
        this.mLevelUp = this.mSoundPool.load(ctx, R.raw.pda_level_up, 1);
        this.mCompassOn = this.mSoundPool.load(ctx, R.raw.compass_on, 1);
        this.mCompassOff = this.mSoundPool.load(ctx, R.raw.compass_off, 1);
        this.mInventoryOpen = this.mSoundPool.load(ctx, R.raw.inv_open, 1);
        this.mInventoryDrop = this.mSoundPool.load(ctx, R.raw.inv_drop, 1);
        this.mInventoryUse = this.mSoundPool.load(ctx, R.raw.inv_use, 1);
        this.mInventoryClose = this.mSoundPool.load(ctx, R.raw.inv_close, 1);
        this.mPdaOn = this.mSoundPool.load(ctx, R.raw.pda_app_start, 1);
        this.mPdaOff = this.mSoundPool.load(ctx, R.raw.pda_app_stop, 1);
        this.mPdaWake = this.mSoundPool.load(ctx, R.raw.pda_app_wake_up, 1);
        this.mBurerPresence = this.mSoundPool.load(ctx, R.raw.burer_presence, 1);
        this.mTransmutating = this.mSoundPool.load(ctx, R.raw.transmutating, 1);
        this.mWTimer = this.mSoundPool.load(ctx, R.raw.w_timer_begins, 1);
        this.mMentalHit = this.mSoundPool.load(ctx, R.raw.mental_hit, 1);
        this.mMonolithHit = this.mSoundPool.load(ctx, R.raw.monolith_call_1, 1);
        this.mAbducted = this.mSoundPool.load(ctx, R.raw.abducted, 1);
        this.mItemScanned = this.mSoundPool.load(ctx, R.raw.pda_qr_scanned, 1);
        this.mArtefact = this.mSoundPool.load(ctx, R.raw.pda_artefact, 1);
    }

    public /* synthetic */ void lambda$new$0$Sound(MediaPlayer s) {
        this.burrerPlaying = false;
    }

    public /* synthetic */ void lambda$new$1$Sound(MediaPlayer s) {
        this.controllerPlaying = false;
    }

    public void playRadClick() {
        float vol = (((float) Math.random()) * 0.1f) + 0.5f;
        float freq = (((float) Math.random()) * 0.2f) + 0.9f;
        this.mSoundPool.play(this.mRadTick, vol, vol, 2, 0, freq);
    }

    public void playGlassBreak() {
        this.mSoundPool.play(this.mGlassBreak, 1.0f, 1.0f, 2, 0, 1.0f);
    }

    public void playBulbBreak() {
        this.mSoundPool.play(this.mBulbBreak, 1.0f, 1.0f, 2, 0, 1.0f);
    }

    public void playLevelUp() {
        this.mSoundPool.play(this.mLevelUp, 1.0f, 1.0f, 2, 0, 1.0f);
    }

    public void playHealing() {
        this.mSoundPool.play(this.mHealing, 1.0f, 1.0f, 2, 0, 1.0f);
    }

    public void stopHealing() {
        this.mSoundPool.stop(this.mHealing);
    }

    public void playAnomalyClick() {
        this.mSoundPool.play(this.mAnomalyTick, 1.0f, 1.0f, 2, 0, 1.0f);
    }

    public void playMental() {
        this.mSoundPool.play(this.mMental, 1.0f, 1.0f, 2, 0, 1.0f);
    }

    public void playAnomalyDeath() {
        this.mSoundPool.play(this.mAnomalyDeath, 1.0f, 1.0f, 2, 0, 1.0f);
    }

    public void playEmissionWarning() {
        this.mSoundPool.play(this.mEmissionWarning, 1.0f, 1.0f, 1, 0, 1.0f);
    }

    public void playEmissionStarts() {
        this.mSoundPool.play(this.mEmissionStarts, 1.0f, 1.0f, 1, 0, 1.0f);
    }

    public void playEmissionHit() {
        this.mSoundPool.play(this.mEmissionHit, 1.0f, 1.0f, 1, 0, 1.0f);
    }

    public void playEmissionPeriodical() {
        this.mSoundPool.play(this.mEmissionPeriodical, 1.0f, 1.0f, 1, 0, 1.0f);
    }

    public void playEmissionEnds() {
        this.mSoundPool.play(this.mEmissionEnds, 1.0f, 1.0f, 1, 0, 1.0f);
    }

    public void playCompassOn() {
        this.mSoundPool.play(this.mCompassOn, 1.0f, 1.0f, 1, 0, 1.0f);
    }

    public void playCompassOff() {
        this.mSoundPool.play(this.mCompassOff, 1.0f, 1.0f, 1, 0, 1.0f);
    }

    public void playInventoryOpen() {
        this.mSoundPool.play(this.mInventoryOpen, 1.0f, 1.0f, 1, 0, 1.0f);
    }

    public void playInventoryDrop() {
        this.mSoundPool.play(this.mInventoryDrop, 1.0f, 1.0f, 1, 0, 1.0f);
    }

    public void playInventoryUse() {
        this.mSoundPool.play(this.mInventoryUse, 1.0f, 1.0f, 1, 0, 1.0f);
    }

    public void playInventoryClose() {
        this.mSoundPool.play(this.mInventoryClose, 1.0f, 1.0f, 1, 0, 1.0f);
    }

    public void playPdaOn() {
        this.mSoundPool.play(this.mPdaOn, 1.0f, 1.0f, 1, 0, 1.0f);
    }

    public void playPdaOff() {
        this.mSoundPool.play(this.mPdaOff, 1.0f, 1.0f, 1, 0, 1.0f);
    }

    public void playPdaWake() {
        this.mSoundPool.play(this.mPdaWake, 1.0f, 1.0f, 1, 0, 1.0f);
    }

    public void playWTimer() {
        this.mSoundPool.play(this.mWTimer, 1.0f, 1.0f, 1, 0, 1.0f);
    }

    public void playDeath() {
        this.mSoundPool.play(this.mDie, 1.0f, 1.0f, 1, 0, 1.0f);
    }

    public void playTransmutating() {
        this.mSoundPool.play(this.mTransmutating, 1.0f, 1.0f, 1, 0, 1.0f);
    }

    public void playAbducted() {
        this.mSoundPool.play(this.mAbducted, 1.0f, 1.0f, 1, 0, 1.0f);
    }

    public void playZombify() {
        this.mSoundPool.play(this.mZombify, 1.0f, 1.0f, 1, 0, 1.0f);
    }

    public void playControlled() {
        this.mSoundPool.play(this.mControl, 1.0f, 1.0f, 1, 0, 1.0f);
    }

    public void playController() {
        this.mSoundPool.play(this.mController, 1.0f, 1.0f, 1, 0, 1.0f);
    }

    public void playBurer() {
        this.mSoundPool.play(this.mBurer, 1.0f, 1.0f, 2, 0, 1.0f);
    }

    public void playMentalHit() {
        this.mSoundPool.play(this.mMentalHit, 1.0f, 1.0f, 1, 0, 1.0f);
    }

    public void playMonolithHit() {
        this.mSoundPool.play(this.mMonolithHit, 1.0f, 1.0f, 2, 0, 1.0f);
    }

    public void playItemScanned() {
        this.mSoundPool.play(this.mItemScanned, 1.0f, 1.0f, 2, 0, 1.0f);
    }

    public void playArtefact() {
        this.mSoundPool.play(this.mArtefact, 1.0f, 1.0f, 2, 0, 1.0f);
    }

    public void playControllerPresence() {
        if (!this.controllerPlaying) {
            this.controllerPlaying = true;
            this.controllerPlayer.start();
        }
    }

    public void playBurerPresence() {
        if (!this.burrerPlaying) {
            this.burrerPlaying = true;
            this.burrerPlayer.start();
        }
    }

    private boolean preparePlayer(MediaPlayer mediaPlayer, int resId, MediaPlayer.OnCompletionListener mp) {
        try {
            mediaPlayer.setDataSource(this.ctx, getSounUri(resId));
            mediaPlayer.prepare();
            mediaPlayer.setOnCompletionListener(mp);
            mediaPlayer.setOnErrorListener(new AnonymousClass1(mp));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /* JADX INFO: renamed from: net.afterday.compas.devices.sound.Sound$1, reason: invalid class name */
    class AnonymousClass1 implements MediaPlayer.OnErrorListener {
        final /* synthetic */ MediaPlayer.OnCompletionListener val$mp;

        AnonymousClass1(MediaPlayer.OnCompletionListener onCompletionListener) {
            this.val$mp = onCompletionListener;
        }

        @Override // android.media.MediaPlayer.OnErrorListener
        public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
            this.val$mp.onCompletion(mediaPlayer);
            return true;
        }
    }

    private Uri getSounUri(int res) {
        return Uri.parse("android.resource://" + this.pckg + "/" + res);
    }
}
