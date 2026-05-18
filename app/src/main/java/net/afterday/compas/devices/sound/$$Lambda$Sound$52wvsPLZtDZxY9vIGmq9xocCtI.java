package net.afterday.compas.devices.sound;

import android.media.MediaPlayer;

/* JADX INFO: renamed from: net.afterday.compas.devices.sound.-$$Lambda$Sound$52wvsPLZtDZxY9vIGmq9xo-cCtI, reason: invalid class name */
/* JADX INFO: compiled from: lambda */
/* JADX INFO: loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$Sound$52wvsPLZtDZxY9vIGmq9xocCtI implements MediaPlayer.OnCompletionListener {
    private final /* synthetic */ Sound f$0;

    public /* synthetic */ $$Lambda$Sound$52wvsPLZtDZxY9vIGmq9xocCtI(Sound sound) {
        this.f$0 = sound;
    }

    @Override // android.media.MediaPlayer.OnCompletionListener
    public final void onCompletion(MediaPlayer mediaPlayer) {
        this.f$0.lambda$new$1$Sound(mediaPlayer);
    }
}
