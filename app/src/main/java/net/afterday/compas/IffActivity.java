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
import net.afterday.compas.iff.IffConfidence;
import net.afterday.compas.iff.IffConfidence.Snapshot;
import net.afterday.compas.iff.IffRadioWitnessStore;
import net.afterday.compas.iff.IffRadioWitnessStore.WitnessSnapshot;

public class IffActivity extends Activity {
    private static final int TAB_CONTACT = 0;
    private static final int TAB_TEAM = 1;
    private static final int TAB_MAP = 2;
    private static final int LOCAL_PLAYER_INDEX = 0;
    private static final long APPROACH_DURATION_MS = 120000L;
    private static final long RADIO_REFRESH_MS = 2000L;

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
    private final Runnable refreshRadioState = new Runnable() {
        @Override
        public void run() {
            render();
            handler.postDelayed(this, RADIO_REFRESH_MS);
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
        handler.removeCallbacks(refreshRadioState);
        handler.postDelayed(refreshRadioState, RADIO_REFRESH_MS);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(expireApproach);
        handler.removeCallbacks(refreshRadioState);
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
        WitnessSnapshot witness = IffRadioWitnessStore.getWitness(selected.playerId);
        Snapshot confidence = confidenceFor(selected, witness);
        boolean localApproachSelected = approachActive && selected.local;
        title.setText(localApproachSelected ? "ВЫ ПОДХОДИТЕ" : selected.displayName);
        subtitle.setText(selected.local ? "локальный игрок" : "локальный roster + radio witness");
        status.setText("CONFIDENCE\n" + confidence.compactStatus());
        body.setText("ИГРОК\n"
                + "- имя: " + selected.displayName + "\n"
                + "- id: " + selected.playerId + "\n"
                + "- команда: локальная IFF группа\n"
                + "- ожидаемый beacon: " + IffRadioWitnessStore.expectedBeaconSsid(selected.playerId) + "\n\n"
                + "СЛОИ УВЕРЕННОСТИ\n"
                + confidenceDetails(confidence) + "\n\n"
                + "СВИДЕТЕЛИ\n"
                + witnessDetails(witness) + "\n\n"
                + "РЕШЕНИЕ\n"
                + decisionText(confidence));
    }

    private void renderTeam() {
        resetBody();
        title.setText("КОМАНДА");
        subtitle.setText("локальный roster + Wi-Fi beacon witness");
        status.setText((approachActive ? "ВЫ        ПОДХОДИТЕ   локально\n" : "")
                + "УЧАСТНИКОВ: " + roster.length + "\n"
                + "RADIO FRESH: " + freshWitnessCount() + "\n"
                + "PROXIMITY OK: " + confidentProximityCount() + "\n"
                + "DIRECTION: UNKNOWN");
        body.setText("Выберите участника, чтобы открыть карточку контакта.\n"
                + "Проценты - текущая уверенность слоя, а не финальное доказательство.");
        for (int i = 0; i < roster.length; i++) {
            bodyContainer.addView(createRosterButton(i));
        }
    }

    private void renderMap() {
        resetBody();
        title.setText("КАРТА");
        subtitle.setText("позиции и свидетели");
        status.setText("POSITION: UNKNOWN 0%\nСВИДЕТЕЛИ: " + freshWitnessCount() + " fresh\nDIRECTION: UNKNOWN 0%");
        body.setText(mapWitnessList());
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
        WitnessSnapshot witness = IffRadioWitnessStore.getWitness(player.playerId);
        Snapshot confidence = confidenceFor(player, witness);
        button.setText(player.displayName + "\nidentity " + confidence.identity.score + "% / proximity "
                + confidence.proximity.score + "% / " + rosterRadioLabel(player, witness));
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

    private String rosterRadioLabel(IffPlayer player, WitnessSnapshot witness) {
        if (player.local && approachActive) {
            return "LOCAL_ONLY";
        }
        if (witness == null) {
            return "UNKNOWN";
        }
        if (!witness.isFresh()) {
            return "STALE " + formatAge(witness.ageMs());
        }
        return witness.proximityLabel() + " " + witness.rssi + "dBm";
    }

    private Snapshot confidenceFor(IffPlayer player, WitnessSnapshot witness) {
        return IffConfidence.evaluate(player.playerId, player.local, approachActive, witness);
    }

    private String confidenceDetails(Snapshot confidence) {
        return confidence.identity.detailLine("identity") + "\n"
                + confidence.proximity.detailLine("proximity") + "\n"
                + confidence.position.detailLine("position") + "\n"
                + confidence.direction.detailLine("direction");
    }

    private String decisionText(Snapshot confidence) {
        if (confidence.proximity.score >= 55) {
            return "- рядом слышен свежий beacon заявленного участника\n"
                    + "- это proximity proof, но не crypto identity\n"
                    + "- direction и точная position пока неизвестны";
        }
        if ("LOCAL_DECLARED_UNKNOWN".equals(confidence.proximity.label)) {
            return "- локальный игрок заявил подход\n"
                    + "- это полезный UI-статус, но не radio proof\n"
                    + "- direction и точная position пока неизвестны";
        }
        if (confidence.proximity.score > 0) {
            return "- есть слабое или устаревшее radio-свидетельство\n"
                    + "- для боевого решения держим proximity осторожной\n"
                    + "- direction и точная position пока неизвестны";
        }
        return "- участник остается известен только по локальному roster\n"
                + "- proximity не подтверждена\n"
                + "- direction и точная position пока неизвестны";
    }

    private String witnessDetails(WitnessSnapshot witness) {
        if (witness == null) {
            return "- нет свежего или старого beacon witness\n"
                    + "- телефон ищет SSID формата " + IffRadioWitnessStore.SSID_PREFIX + "*";
        }
        return "- ssid: " + witness.ssid + "\n"
                + "- bssid: " + witness.bssid + "\n"
                + "- freshness: " + witness.freshnessLabel() + "\n"
                + "- age: " + formatAge(witness.ageMs()) + "\n"
                + "- rssi: " + witness.rssi + " dBm\n"
                + "- frequency: " + witness.frequency + " MHz";
    }

    private int freshWitnessCount() {
        int count = 0;
        for (int i = 0; i < roster.length; i++) {
            WitnessSnapshot witness = IffRadioWitnessStore.getWitness(roster[i].playerId);
            if (witness != null && witness.isFresh()) {
                count++;
            }
        }
        return count;
    }

    private int confidentProximityCount() {
        int count = 0;
        for (int i = 0; i < roster.length; i++) {
            IffPlayer player = roster[i];
            WitnessSnapshot witness = IffRadioWitnessStore.getWitness(player.playerId);
            Snapshot confidence = confidenceFor(player, witness);
            if (confidence.proximity.score >= 55) {
                count++;
            }
        }
        return count;
    }

    private String mapWitnessList() {
        StringBuilder builder = new StringBuilder();
        builder.append("КАРТА ПОКА НЕ РИСУЕТ АЗИМУТ\n\n");
        for (int i = 0; i < roster.length; i++) {
            IffPlayer player = roster[i];
            WitnessSnapshot witness = IffRadioWitnessStore.getWitness(player.playerId);
            builder.append(player.displayName)
                    .append(": ")
                    .append(witness == null ? "radio UNKNOWN" : witness.freshnessLabel() + " " + witness.rssi + "dBm age=" + formatAge(witness.ageMs()))
                    .append("\n");
        }
        builder.append("\nGPS и направление будут отдельными слоями уверенности.");
        return builder.toString();
    }

    private String formatAge(long ageMs) {
        if (ageMs < 1000L) {
            return ageMs + "ms";
        }
        return (ageMs / 1000L) + "s";
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
