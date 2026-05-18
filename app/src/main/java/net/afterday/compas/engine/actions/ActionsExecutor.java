package net.afterday.compas.engine.actions;

import io.reactivex.Observable;
import java.util.ArrayList;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public class ActionsExecutor {
    private static final String TAG = "ActionsExecutor";
    private static ActionsExecutor instance;
    private List<Action> actions = new ArrayList();
    private Action closestAction;

    private ActionsExecutor(Observable<Long> ticks) {
        ticks.subscribe(new $$Lambda$ActionsExecutor$3ADWAVq5uTUR6Ss1nPbgmmGURcg(this));
    }

    public /* synthetic */ void lambda$new$0$ActionsExecutor(Long t) {
        Action action = this.closestAction;
        if (action != null && action.startTime() <= System.currentTimeMillis()) {
            Action action2 = this.closestAction;
            this.actions.remove(this.closestAction);
            this.closestAction = findClosestAction();
            action2.execute();
        }
    }

    public void postAction(Action action) {
        this.actions.add(action);
        this.closestAction = findClosestAction();
    }

    public static ActionsExecutor instance() {
        ActionsExecutor actionsExecutor = instance;
        if (actionsExecutor == null) {
            throw new IllegalStateException("Actions executor must be initialized!");
        }
        return actionsExecutor;
    }

    public static ActionsExecutor instance(Observable<Long> ticks) {
        if (instance == null) {
            instance = new ActionsExecutor(ticks);
        }
        return instance;
    }

    private Action findClosestAction() {
        Action ca = null;
        for (Action a : this.actions) {
            if (ca == null) {
                ca = a;
            } else if (a.startTime() < ca.startTime()) {
                ca = a;
            }
        }
        return ca;
    }
}
