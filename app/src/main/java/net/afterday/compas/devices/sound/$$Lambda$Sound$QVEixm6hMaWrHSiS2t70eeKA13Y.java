package net.afterday.compas.devices.sound;

import android.media.MediaPlayer;

/* JADX INFO: renamed from: net.afterday.compas.devices.sound.-$$Lambda$Sound$QVEixm6hMaWrHSiS2t70eeKA13Y, reason: invalid class name */
/* JADX INFO: compiled from: lambda */
/* JADX INFO: loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$Sound$QVEixm6hMaWrHSiS2t70eeKA13Y implements MediaPlayer.OnCompletionListener {
    private final /* synthetic */ Sound f$0;

    public /* synthetic */ $$Lambda$Sound$QVEixm6hMaWrHSiS2t70eeKA13Y(Sound sound) {
        this.f$0 = sound;
    }

    @Override // android.media.MediaPlayer.OnCompletionListener
    public final void onCompletion(MediaPlayer mediaPlayer) {
        this.f$0.lambda$new$0$Sound(mediaPlayer);
    }
}
