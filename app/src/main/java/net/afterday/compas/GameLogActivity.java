package net.afterday.compas;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import java.util.ArrayList;
import java.util.List;
import io.reactivex.disposables.CompositeDisposable;
import net.afterday.compas.logging.Logger;
import net.afterday.compas.view.SmallLogListAdapter;

public class GameLogActivity extends Activity {
    private final CompositeDisposable disposables = new CompositeDisposable();
    private SmallLogListAdapter logAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_game_log);

        findViewById(R.id.close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        RecyclerView logList = (RecyclerView) findViewById(R.id.log_list);
        final RecyclerView.LayoutManager logListManager = new LinearLayoutManager(this, 1, true);
        ((LinearLayoutManager) logListManager).setStackFromEnd(true);
        logList.setLayoutManager(logListManager);
        logAdapter = new SmallLogListAdapter(this, new ArrayList());
        logList.setAdapter(logAdapter);
        try {
            disposables.add(Logger.instance().getLogStream().subscribe(new io.reactivex.functions.Consumer<List>() {
                @Override
                public void accept(List log) {
                    logAdapter.setDataset(log);
                    if (log.size() > 0) {
                        logListManager.scrollToPosition(log.size() - 1);
                    }
                }
            }));
        } catch (IllegalStateException e) {
            logAdapter.setDataset(new ArrayList());
        }
    }

    @Override
    protected void onDestroy() {
        disposables.clear();
        super.onDestroy();
    }
}
