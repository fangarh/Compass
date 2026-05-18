package net.afterday.compas;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Process;

/* JADX INFO: loaded from: classes.dex */
public class ActionsReceiver extends BroadcastReceiver {
    @Override // android.content.BroadcastReceiver
    public void onReceive(Context context, Intent intent) {
        context.stopService(new Intent(context, (Class<?>) LocalMainService.class));
        Process.killProcess(Process.myPid());
    }
}
