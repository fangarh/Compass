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
import android.widget.TextView;

public class IffActivity extends Activity {
    private static final int TAB_CONTACT = 0;
    private static final int TAB_TEAM = 1;
    private static final int TAB_MAP = 2;
    private static final long APPROACH_DURATION_MS = 120000L;

    private final Handler handler = new Handler();
    private int activeTab = TAB_TEAM;
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
        title.setText(approachActive ? "ВЫ ПОДХОДИТЕ" : "КОНТАКТ");
        subtitle.setText(approachActive ? "локальный режим активен" : "активных подходов нет");
        status.setText(approachActive ? "ИДЕНТИЧНОСТЬ: локальный прототип\nБЛИЗОСТЬ: не подтверждена\nНАПРАВЛЕНИЕ: нет данных" : "ПОДТВЕРЖДЕННЫХ КОНТАКТОВ НЕТ");
        body.setText(approachActive
                ? "СВИДЕТЕЛИ\n- нет радиосвидетелей\n\nСИГНАЛ\n- phone-to-phone обмен не включен\n- командный ключ не настроен\n\nСЛЕДУЮЩИЙ СЛОЙ\n- roster\n- trusted player id\n- radio witness events"
                : "Ожидание активного подхода своего игрока.");
    }

    private void renderTeam() {
        title.setText("КОМАНДА");
        subtitle.setText(approachActive ? "ваш подход активен" : "локальный roster не настроен");
        status.setText(approachActive ? "ВЫ        ПОДХОДИТЕ   локально" : "ИГРОКИ НЕ ЗАГРУЖЕНЫ");
        body.setText("ПОРЯДОК MVP\n1. roster известных своих\n2. режим я подхожу\n3. свежие радиосвидетели\n4. доверие и близость\n5. карта с областью ошибки");
    }

    private void renderMap() {
        title.setText("КАРТА");
        subtitle.setText("позиции и свидетели");
        status.setText("ПОЗИЦИЯ: нет данных\nСВИДЕТЕЛИ: нет данных\nНАПРАВЛЕНИЕ: не подтверждено");
        body.setText("Карта MVP будет показывать своих, свежесть точек, круг ошибки GPS и связи свидетелей.");
    }
}
