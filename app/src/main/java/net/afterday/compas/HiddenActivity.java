package net.afterday.compas;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;

/* JADX INFO: loaded from: classes.dex */
public class HiddenActivity extends Activity {
    @Override // android.app.Activity
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(1024, 1024);
        setContentView(R.layout.activity_hidden);
    }
}
