package net.afterday.compas;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class IffActivity extends Activity {
    private static final int TAB_CONTACT = 0;
    private static final int TAB_TEAM = 1;
    private static final int TAB_MAP = 2;
    private static final int LOCAL_PLAYER_INDEX = 0;
    private static final long APPROACH_DURATION_MS = 120000L;

    private final IffPlayer[] roster = new IffPlayer[] {
            new IffPlayer("local-you", "Вы", true),
            new IffPlayer("petya", "Петя", false),
            new IffPlayer("vasya", "Вася", false),
            new IffPlayer("zhenya", "Женя", false)
    };

    private final Handler handler = new Handler();
    private int activeTab = TAB_TEAM;
    private int selectedPlayerIndex = LOCAL_PLAYER_INDEX;
    private boolean approachActive;
    private long approachUntilMs;

    private Button contactTab;
    private Button teamTab;
    private Button mapTab;
    private Button approachButton;
    private TextView title;
    private TextView subtitle;
    private TextView status;
    private TextView body;
    private LinearLayout bodyContainer;

    private final Runnable expireApproach = new Runnable() {
        @Override
        public void run() {
            approachActive = false;
            render();
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.iff_activity);
        bindViews();
        setTypeface();
        setListeners();
        render();
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
        if (approachActive) {
            scheduleApproachExpire();
        }
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(expireApproach);
        super.onPause();
    }

    private void bindViews() {
        contactTab = (Button) findViewById(R.id.iff_contact_tab);
        teamTab = (Button) findViewById(R.id.iff_team_tab);
        mapTab = (Button) findViewById(R.id.iff_map_tab);
        approachButton = (Button) findViewById(R.id.iff_approach);
        title = (TextView) findViewById(R.id.iff_title);
        subtitle = (TextView) findViewById(R.id.iff_subtitle);
        status = (TextView) findViewById(R.id.iff_status);
        body = (TextView) findViewById(R.id.iff_body);
        bodyContainer = (LinearLayout) findViewById(R.id.iff_body_container);
    }

    private void setTypeface() {
        Typeface mono = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL);
        title.setTypeface(mono, Typeface.BOLD);
        subtitle.setTypeface(mono);
        status.setTypeface(mono, Typeface.BOLD);
        body.setTypeface(mono);
        contactTab.setTypeface(mono, Typeface.BOLD);
        teamTab.setTypeface(mono, Typeface.BOLD);
        mapTab.setTypeface(mono, Typeface.BOLD);
        approachButton.setTypeface(mono, Typeface.BOLD);
    }

    private void setListeners() {
        contactTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activeTab = TAB_CONTACT;
                render();
            }
        });
        teamTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activeTab = TAB_TEAM;
                render();
            }
        });
        mapTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activeTab = TAB_MAP;
                render();
            }
        });
        approachButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleApproach();
            }
        });
    }

    private void toggleApproach() {
        approachActive = !approachActive;
        if (approachActive) {
            approachUntilMs = System.currentTimeMillis() + APPROACH_DURATION_MS;
            selectedPlayerIndex = LOCAL_PLAYER_INDEX;
            activeTab = TAB_CONTACT;
            scheduleApproachExpire();
        } else {
            handler.removeCallbacks(expireApproach);
        }
        render();
    }

    private void scheduleApproachExpire() {
        handler.removeCallbacks(expireApproach);
        handler.postDelayed(expireApproach, Math.max(0L, approachUntilMs - System.currentTimeMillis()));
    }

    private void render() {
        approachButton.setText(approachActive ? "ОТМЕНИТЬ ПОДХОД" : "Я ПОДХОЖУ");
        renderTabs();
        if (activeTab == TAB_CONTACT) {
            renderContact();
        } else if (activeTab == TAB_TEAM) {
            renderTeam();
        } else {
            renderMap();
        }
    }

    private void renderTabs() {
        setTabState(contactTab, activeTab == TAB_CONTACT);
        setTabState(teamTab, activeTab == TAB_TEAM);
        setTabState(mapTab, activeTab == TAB_MAP);
    }

    private void setTabState(Button tab, boolean active) {
        tab.setTextColor(active ? 0xffffd16a : 0xffb8c49a);
    }

    private void renderContact() {
        resetBody();
        IffPlayer selected = roster[selectedPlayerIndex];
        boolean localApproachSelected = approachActive && selected.local;
        title.setText(localApproachSelected ? "ВЫ ПОДХОДИТЕ" : selected.displayName);
        subtitle.setText(selected.local ? "локальный игрок" : "локальный roster");
        status.setText("IDENTITY: " + identityStatus(selected) + "\n"
                + "PROXIMITY: UNKNOWN - радио не подтверждено\n"
                + "POSITION: UNKNOWN - GPS не доказывает близость\n"
                + "DIRECTION: UNKNOWN - азимут не рассчитан");
        body.setText("ИГРОК\n"
                + "- имя: " + selected.displayName + "\n"
                + "- id: " + selected.playerId + "\n"
                + "- команда: локальная IFF группа\n\n"
                + "СВИДЕТЕЛИ\n"
                + "- нет phone-to-phone событий\n"
                + "- нет свежего радиосигнала\n\n"
                + "РЕШЕНИЕ\n"
                + "- участник известен только из локального roster\n"
                + "- неподтвержденная близость остается UNKNOWN\n"
                + "- кнопка Я ПОДХОЖУ меняет только статус локального игрока");
    }

    private void renderTeam() {
        resetBody();
        title.setText("КОМАНДА");
        subtitle.setText("локальный roster, без радиоподтверждения");
        status.setText((approachActive ? "ВЫ        ПОДХОДИТЕ   локально\n" : "")
                + "УЧАСТНИКОВ: " + roster.length + "\n"
                + "БЛИЗОСТЬ: UNKNOWN для всех");
        body.setText("Выберите участника, чтобы открыть карточку контакта.\n"
                + "Roster подтверждает только заявленную личность в локальном списке.");
        for (int i = 0; i < roster.length; i++) {
            bodyContainer.addView(createRosterButton(i));
        }
    }

    private void renderMap() {
        resetBody();
        title.setText("КАРТА");
        subtitle.setText("позиции и свидетели");
        status.setText("ПОЗИЦИЯ: нет данных\nСВИДЕТЕЛИ: нет данных\nНАПРАВЛЕНИЕ: не подтверждено");
        body.setText("Карта MVP будет показывать своих, свежесть точек, круг ошибки GPS и связи свидетелей.");
    }

    private String identityStatus(IffPlayer player) {
        if (player.local) {
            return approachActive ? "LOCAL_SELF_APPROACH" : "LOCAL_SELF";
        }
        return "ROSTER_ONLY - не подтверждено радио";
    }

    private void resetBody() {
        bodyContainer.removeAllViews();
        bodyContainer.addView(body);
    }

    private Button createRosterButton(final int playerIndex) {
        IffPlayer player = roster[playerIndex];
        Button button = new Button(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(54));
        params.setMargins(0, dp(8), 0, 0);
        button.setLayoutParams(params);
        button.setBackgroundResource(R.drawable.popup_button);
        button.setTextColor(playerIndex == selectedPlayerIndex ? 0xffffd16a : 0xffffffff);
        button.setText(player.displayName + "\nidentity: " + rosterIdentityLabel(player)
                + " / proximity: UNKNOWN");
        button.setTextSize(13);
        button.setTransformationMethod(null);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedPlayerIndex = playerIndex;
                activeTab = TAB_CONTACT;
                render();
            }
        });
        return button;
    }

    private String rosterIdentityLabel(IffPlayer player) {
        if (player.local) {
            return approachActive ? "LOCAL_SELF_APPROACH" : "LOCAL_SELF";
        }
        return "ROSTER_ONLY";
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private static final class IffPlayer {
        final String playerId;
        final String displayName;
        final boolean local;

        IffPlayer(String playerId, String displayName, boolean local) {
            this.playerId = playerId;
            this.displayName = displayName;
            this.local = local;
        }
    }
}
