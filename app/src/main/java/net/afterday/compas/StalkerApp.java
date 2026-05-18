package net.afterday.compas;

import android.app.Application;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import net.afterday.compas.core.Game;
import net.afterday.compas.core.gameState.Frame;
import net.afterday.compas.core.serialization.Serializer;
import net.afterday.compas.core.userActions.UserActionsPack;
import net.afterday.compas.engine.Engine;
import net.afterday.compas.sensors.Battery.Battery;
import net.afterday.compas.settings.Settings;
import net.afterday.compas.util.Fonts;

/* JADX INFO: loaded from: classes.dex */
public class StalkerApp extends Application {
    private static final String TAG = "StalkerApp";
    private static StalkerApp instance;
    private Battery battery;
    private Observable<Integer> batteryLevelStream;
    private Engine engine;
    private Fonts fonts;
    private Observable<Frame> framesStream;
    private Game game;
    private Serializer serializer;
    private Settings settings;
    private Observable<UserActionsPack> userActionsStream = PublishSubject.create();

    @Override // android.app.Application
    public void onCreate() {
        super.onCreate();
        instance = this;
        this.settings = Settings.instance(this);
        this.fonts = Fonts.instance(getAssets());
    }

    public Game getGame() {
        return this.game;
    }

    public static StalkerApp getInstance() {
        return instance;
    }
}
