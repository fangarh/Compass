package net.afterday.compas;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import net.afterday.compas.iff.IffBleFieldRadio;
import net.afterday.compas.iff.IffConfidence;
import net.afterday.compas.iff.IffConfidence.Snapshot;
import net.afterday.compas.iff.IffDistanceTrend;
import net.afterday.compas.iff.IffFieldLocatorSnapshot;
import net.afterday.compas.iff.IffFieldMapSnapshot;
import net.afterday.compas.iff.IffFieldRunSummary;
import net.afterday.compas.iff.IffForegroundRadioService;
import net.afterday.compas.iff.IffGpsSnapshot;
import net.afterday.compas.iff.IffOfficeProximityVerdict;
import net.afterday.compas.iff.IffOperatorFieldSnapshotStore;
import net.afterday.compas.iff.IffRemoteWitnessReport;
import net.afterday.compas.iff.IffRemoteWitnessStore;
import net.afterday.compas.iff.IffRadioWitnessStore;
import net.afterday.compas.iff.IffRadioWitnessStore.RssiWindowSnapshot;
import net.afterday.compas.iff.IffRadioWitnessStore.WitnessSnapshot;
import net.afterday.compas.iff.IffTacticalMapView;
import net.afterday.compas.iff.IffUdpWitnessTransport;
import net.afterday.compas.iff.IffWitnessQuorum;
import net.afterday.compas.iff.IffWifiTargetObservationStore;
import net.afterday.compas.logging.FieldDiagnosticLog;

public class IffActivity extends Activity {
    private static final int TAB_CONTACT = 0;
    private static final int TAB_TEAM = 1;
    private static final int TAB_MAP = 2;
    private static final int TAB_LOG = 3;
    private static final int LOCAL_PLAYER_INDEX = 0;
    private static final long APPROACH_DURATION_MS = 120000L;
    private static final long RADIO_REFRESH_MS = 2000L;
    private static final long DISTANCE_WINDOW_MS = 6000L;
    private static final String PREFS_NAME = "iff";
    private static final String PREF_LOCAL_DEVICE_PLAYER_ID = "local_device_player_id";
    private static final String PREF_FIELD_RADIO_ENABLED = "field_radio_enabled";
    private static final String PREF_TRUSTED_PLAYER_PREFIX = "trusted_player_";

    private final IffPlayer[] roster = new IffPlayer[] {
            new IffPlayer("local-you", "Вы", true),
            new IffPlayer("petya", "Петя", false),
            new IffPlayer("vasya", "Вася", false),
            new IffPlayer("zhenya", "Женя", false)
    };

    private final Handler handler = new Handler();
    private final IffOperatorFieldSnapshotStore operatorFieldSnapshotStore =
            new IffOperatorFieldSnapshotStore();
    private int activeTab = TAB_TEAM;
    private int selectedPlayerIndex = LOCAL_PLAYER_INDEX;
    private String localDevicePlayerId = "local-you";
    private boolean approachActive;
    private long approachUntilMs;

    private Button contactTab;
    private Button teamTab;
    private Button mapTab;
    private Button logTab;
    private Button approachButton;
    private Button trustButton;
    private Button recordCheckButton;
    private Button radioServiceButton;
    private TextView title;
    private TextView subtitle;
    private TextView status;
    private TextView body;
    private LinearLayout bodyContainer;
    private String lastFieldCheckSummary = "нет записанных проверок";
    private boolean fieldRadioEnabled = true;

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
            IffRadioWitnessStore.logFreshnessTransitions("iff_activity_refresh");
            render();
            handler.postDelayed(this, RADIO_REFRESH_MS);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FieldDiagnosticLog.start(this);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.iff_activity);
        loadLocalDeviceIdentity();
        loadFieldRadioPreference();
        bindViews();
        setTypeface();
        setListeners();
        render();
    }

    @Override
    protected void onResume() {
        super.onResume();
        IffUdpWitnessTransport.ensureStarted();
        ensureFieldRadioService();
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
        IffUdpWitnessTransport.stop();
        super.onPause();
    }

    private void bindViews() {
        contactTab = (Button) findViewById(R.id.iff_contact_tab);
        teamTab = (Button) findViewById(R.id.iff_team_tab);
        mapTab = (Button) findViewById(R.id.iff_map_tab);
        logTab = (Button) findViewById(R.id.iff_log_tab);
        approachButton = (Button) findViewById(R.id.iff_approach);
        trustButton = (Button) findViewById(R.id.iff_trust);
        recordCheckButton = (Button) findViewById(R.id.iff_record_check);
        radioServiceButton = (Button) findViewById(R.id.iff_radio_service);
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
        logTab.setTypeface(mono, Typeface.BOLD);
        approachButton.setTypeface(mono, Typeface.BOLD);
        trustButton.setTypeface(mono, Typeface.BOLD);
        recordCheckButton.setTypeface(mono, Typeface.BOLD);
        radioServiceButton.setTypeface(mono, Typeface.BOLD);
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
        logTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activeTab = TAB_LOG;
                render();
            }
        });
        approachButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (activeTab == TAB_CONTACT && !isLocalDevice(roster[selectedPlayerIndex])) {
                    setLocalDevicePlayer(selectedPlayerIndex);
                } else {
                    toggleApproach();
                }
            }
        });
        trustButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleSelectedTrust();
            }
        });
        recordCheckButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recordFieldCheck();
            }
        });
        radioServiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleFieldRadioService();
            }
        });
    }

    private void toggleApproach() {
        approachActive = !approachActive;
        if (approachActive) {
            approachUntilMs = System.currentTimeMillis() + APPROACH_DURATION_MS;
            selectedPlayerIndex = localDevicePlayerIndex();
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
        IffPlayer selected = roster[selectedPlayerIndex];
        if (activeTab == TAB_CONTACT && !isLocalDevice(selected)) {
            approachButton.setText("ЭТОТ ТЕЛ.");
        } else {
            approachButton.setText(approachActive ? "ОТМЕНИТЬ ПОДХОД" : "Я ПОДХОЖУ");
        }
        renderTrustButton(selected);
        radioServiceButton.setText(fieldRadioEnabled ? "RADIO ON" : "RADIO OFF");
        radioServiceButton.setTextColor(fieldRadioEnabled ? 0xff7dff73 : 0xffffd16a);
        renderTabs();
        if (activeTab == TAB_CONTACT) {
            renderContact();
        } else if (activeTab == TAB_TEAM) {
            renderTeam();
        } else if (activeTab == TAB_MAP) {
            renderMap();
        } else {
            renderLog();
        }
    }

    private void renderTabs() {
        setTabState(contactTab, activeTab == TAB_CONTACT);
        setTabState(teamTab, activeTab == TAB_TEAM);
        setTabState(mapTab, activeTab == TAB_MAP);
        setTabState(logTab, activeTab == TAB_LOG);
    }

    private void setTabState(Button tab, boolean active) {
        tab.setTextColor(active ? 0xffffd16a : 0xffb8c49a);
    }

    private void renderContact() {
        resetBody();
        IffPlayer selected = roster[selectedPlayerIndex];
        WitnessSnapshot witness = IffRadioWitnessStore.getWitness(selected.playerId);
        Snapshot confidence = confidenceFor(selected, witness);
        IffWitnessQuorum.Snapshot quorum = witnessQuorumFor(selected, witness);
        CombatSnapshot combat = combatFor(selected, confidence, quorum);
        if (SystemClock.elapsedRealtime() >= 0L) {
            renderContactGame(selected, witness, confidence, quorum, combat);
            return;
        }
        boolean selectedIsLocalDevice = isLocalDevice(selected);
        boolean localApproachSelected = approachActive && selectedIsLocalDevice;
        title.setText(localApproachSelected ? "ВЫ ПОДХОДИТЕ" : selected.displayName);
        subtitle.setText(selectedIsLocalDevice ? "этот телефон объявляет этого участника" : "локальный roster trust + radio witness");
        status.setText("COMBAT: " + combat.state + " / " + combat.action + "\n"
                + "OPERATOR: " + operatorVerdictLabel(confidence, quorum) + "\n"
                + "OFFICE ROLE: " + officeTestRole(localDevicePlayer()) + "\n"
                + "CONFIDENCE\n" + confidence.compactStatus() + "\nWITNESSES: " + quorum.compact());
        body.setText("ИГРОК\n"
                + "- имя: " + selected.displayName + "\n"
                + "- id: " + selected.playerId + "\n"
                + "- команда: локальная IFF группа\n"
                + "- office role: " + officeTestRole(selected) + "\n"
                + "- this device role: " + officeTestRole(localDevicePlayer()) + "\n"
                + "- trust: " + trustLabel(selected) + "\n"
                + "- ожидаемый beacon: " + IffRadioWitnessStore.expectedBeaconSsid(selected.playerId) + "\n\n"
                + "БОЕВОЙ ВИД\n"
                + combatDetails(combat) + "\n\n"
                + "OPERATOR VIEW\n"
                + operatorDetails(selected, confidence, quorum, combat) + "\n\n"
                + "СЛОИ УВЕРЕННОСТИ\n"
                + confidenceDetails(confidence) + "\n\n"
                + "СВИДЕТЕЛИ\n"
                + witnessDetails(witness) + "\n\n"
                + "FIELD RADIO POLICY\n"
                + fieldRadioPolicyDetails() + "\n\n"
                + "WITNESS QUORUM\n"
                + witnessQuorumDetails(selected, quorum) + "\n\n"
                + "РЕШЕНИЕ\n"
                + decisionText(confidence, quorum) + "\n\n"
                + "FIELD CHECK\n"
                + "- последняя запись: " + lastFieldCheckSummary);
    }

    private void renderTeam() {
        resetBody();
        if (SystemClock.elapsedRealtime() >= 0L) {
            renderTeamGame();
            return;
        }
        title.setText("КОМАНДА");
        subtitle.setText("локальный roster + field radio identity");
        status.setText((approachActive ? "ВЫ        ПОДХОДИТЕ   локально\n" : "")
                + "THIS DEVICE: " + localDevicePlayer().displayName + "\n"
                + "OFFICE ROLE: " + officeTestRole(localDevicePlayer()) + "\n"
                + "OFFICE VERDICT: " + officeProximityLine() + "\n"
                + "OPERATOR: " + teamOperatorSummaryLine()
                + " / TRUSTED " + trustedRosterCount() + "/" + (roster.length - 1) + "\n"
                + "COMBAT: current " + combatStateCount("CURRENT")
                + " / stale " + combatStateCount("STALE")
                + " / unknown " + combatStateCount("UNKNOWN") + "\n"
                + "WITNESS: current " + currentWitnessEvidenceCount()
                + " / stale " + staleWitnessEvidenceCount()
                + " / radio " + freshWitnessCount() + "/" + strongProximityCount() + "\n"
                + "FIELD RADIO: " + (fieldRadioEnabled ? "ON" : "OFF")
                + " / " + IffBleFieldRadio.compactStatus() + "\n"
                + "REMOTE REPORTS: " + remoteReportCount()
                + " / DIRECTION: UNKNOWN");
        body.setText("Выберите участника, чтобы открыть карточку контакта.\n"
                + "Долгое нажатие назначает, кем является этот телефон.\n"
                + "TRUST помечает участника локально доверенным, но не доказывает proximity.\n"
                + "Боевой статус показывает current/stale/unknown отдельно от identity.\n"
                + "Operator summary отделяет current witness от stale evidence.\n"
                + "Проценты - текущая уверенность слоя, а не финальное доказательство.\n"
                + "Field radio не должен требовать общей Wi-Fi сети.\n"
                + "Office proximity: " + officeProximityLine() + "\n"
                + "Office samples: " + officeProximitySamplesLine() + "\n"
                + "Radio control: " + (fieldRadioEnabled ? "ON" : "OFF") + "\n"
                + "Radio service: " + IffForegroundRadioService.compactStatus() + "\n"
                + "BLE lifecycle: " + IffBleFieldRadio.lifecycleStatus() + "\n"
                + "BLE skeleton: " + IffBleFieldRadio.compactStatus() + "\n"
                + "Trusted roster entries: " + trustedRosterCount() + "/" + (roster.length - 1) + "\n"
                + "Remote witness contract: " + IffRemoteWitnessReport.CONTRACT_VERSION + "\n"
                + "Signature status пока placeholder: " + IffRemoteWitnessReport.SIGNATURE_PENDING + "\n"
                + "UDP debug: " + IffUdpWitnessTransport.compactStatus() + "\n"
                + "Transport: UDP diagnostic channel.\n"
                + "Последняя проверка: " + lastFieldCheckSummary);
        bodyContainer.removeAllViews();
        for (int i = 0; i < roster.length; i++) {
            bodyContainer.addView(createRosterButton(i));
        }
        bodyContainer.addView(body);
    }

    private void renderContactGame(IffPlayer selected, WitnessSnapshot witness, Snapshot confidence,
                                   IffWitnessQuorum.Snapshot quorum, CombatSnapshot combat) {
        boolean selectedIsLocalDevice = isLocalDevice(selected);
        title.setText(approachActive && selectedIsLocalDevice ? "APPROACHING" : selected.displayName);
        subtitle.setText(selectedIsLocalDevice ? "this phone identity" : "field contact");
        status.setText("COMBAT: " + combat.state + " / " + combat.action + "\n"
                + "OPERATOR: " + operatorVerdictLabel(confidence, quorum) + "\n"
                + "PROXIMITY: " + confidence.proximity.label + " " + confidence.proximity.score + "%\n"
                + "DISTANCE: " + distanceTrendFor(selected).compact() + "\n"
                + "WITNESS: " + quorum.compact());
        body.setText("PLAYER\n"
                + "- name: " + selected.displayName + "\n"
                + "- id: " + selected.playerId + "\n"
                + "- trust: " + trustLabel(selected) + "\n"
                + "- radio: " + rosterRadioLabel(selected, witness) + "\n"
                + "- distance: " + distanceTrendFor(selected).compact() + "\n"
                + "- office role: " + officeTestRole(selected) + "\n\n"
                + "ACTION\n"
                + "- " + combat.action + "\n"
                + "- last check: " + lastFieldCheckSummary);
    }

    private void renderTeamGame() {
        title.setText("TEAM");
        subtitle.setText("field roster");
        status.setText((approachActive ? "APPROACHING locally\n" : "")
                + "THIS DEVICE: " + localDevicePlayer().displayName + "\n"
                + "OFFICE: " + officeProximityLine() + "\n"
                + "CONTACTS: current " + currentWitnessEvidenceCount()
                + " / stale " + staleWitnessEvidenceCount()
                + " / unknown " + combatStateCount("UNKNOWN") + "\n"
                + "RADIO: " + (fieldRadioEnabled ? "ON" : "OFF"));
        bodyContainer.removeAllViews();
        for (int i = 0; i < roster.length; i++) {
            bodyContainer.addView(createRosterButton(i));
        }
    }

    private void renderMap() {
        resetBody();
        if (SystemClock.elapsedRealtime() >= 0L) {
            renderMapGame();
            return;
        }
        title.setText("КАРТА");
        subtitle.setText("field contacts");
        status.setText("POSITION/DIRECTION: UNKNOWN 0%\n"
                + "OFFICE ROLE: " + officeTestRole(localDevicePlayer()) + "\n"
                + "RADIO: local " + freshWitnessCount() + " fresh / remote " + remoteReportCount() + "\n"
                + "RADIO CONTROL: " + (fieldRadioEnabled ? "ON" : "OFF") + "\n"
                + "FIELD RADIO: " + IffBleFieldRadio.compactStatus() + "\n"
                + "RADIO SERVICE: " + IffForegroundRadioService.compactStatus() + "\n"
                + "BLE POLICY: " + IffBleFieldRadio.lifecycleStatus() + "\n"
                + "UDP DEBUG: " + IffUdpWitnessTransport.compactStatus());
        bodyContainer.removeAllViews();
        IffTacticalMapView mapView = new IffTacticalMapView(this);
        LinearLayout.LayoutParams mapParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(220));
        mapParams.setMargins(0, 0, 0, dp(8));
        mapView.setLayoutParams(mapParams);
        mapView.setState(localDevicePlayer().displayName, mapPoints());
        mapView.setFieldState(fieldMapSnapshot());
        bodyContainer.addView(mapView);
        bodyContainer.addView(body);
        body.setText(mapWitnessList());
    }

    private void renderMapGame() {
        title.setText("MAP");
        subtitle.setText("field contacts");
        status.setText("POSITION: UNKNOWN\n"
                + "OFFICE: " + officeProximityLine() + "\n"
                + "DISTANCE: " + officeDistanceTrendLine() + "\n"
                + "GPS: " + gpsUiStatus() + "\n"
                + "RADIO: " + freshWitnessCount() + " current / " + staleWitnessEvidenceCount() + " stale");
        bodyContainer.removeAllViews();
        IffTacticalMapView mapView = new IffTacticalMapView(this);
        LinearLayout.LayoutParams mapParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(260));
        mapParams.setMargins(0, 0, 0, dp(8));
        mapView.setLayoutParams(mapParams);
        mapView.setState(localDevicePlayer().displayName, mapPoints());
        mapView.setFieldState(fieldMapSnapshot());
        bodyContainer.addView(mapView);
        bodyContainer.addView(body);
        body.setText(fieldMapSummary());
    }

    private void renderLog() {
        resetBody();
        title.setText("LOG");
        subtitle.setText("field diagnostics");
        status.setText("RADIO: " + (fieldRadioEnabled ? "ON" : "OFF") + "\n"
                + "OFFICE: " + officeProximityLine() + "\n"
                + "DISTANCE: " + distanceTrendFor(roster[selectedPlayerIndex]).compact() + "\n"
                + "GPS: " + gpsUiStatus() + "\n"
                + "RUN: " + fieldRunHeader() + "\n"
                + "LAST CHECK: " + lastFieldCheckSummary);
        IffPlayer selected = roster[selectedPlayerIndex];
        WitnessSnapshot witness = IffRadioWitnessStore.getWitness(selected.playerId);
        Snapshot confidence = confidenceFor(selected, witness);
        IffWitnessQuorum.Snapshot quorum = witnessQuorumFor(selected, witness);
        CombatSnapshot combat = combatFor(selected, confidence, quorum);
        body.setText("SELECTED\n"
                + operatorDetails(selected, confidence, quorum, combat) + "\n\n"
                + "CONFIDENCE\n"
                + confidenceDetails(confidence) + "\n\n"
                + "WITNESS\n"
                + witnessDetails(witness) + "\n\n"
                + "FIELD RADIO\n"
                + fieldRadioPolicyDetails() + "\n\n"
                + "OFFICE\n"
                + "- verdict: " + officeProximityLine() + "\n"
                + "- samples: " + officeProximitySamplesLine() + "\n\n"
                + "DISTANCE / MOVEMENT\n"
                + "- selected: " + distanceTrendFor(selected).compact() + "\n"
                + "- office: " + officeDistanceTrendLine() + "\n"
                + "- gps: " + gpsUiStatus() + "\n\n"
                + "FIELD LOCATOR\n"
                + "- service: " + IffForegroundRadioService.compactStatus() + "\n"
                + "- two-anchor: " + IffWifiTargetObservationStore.compactStatus() + "\n\n"
                + "FIELD RUN\n"
                + IffFieldRunSummary.details() + "\n\n"
                + "QUORUM\n"
                + witnessQuorumDetails(selected, quorum));
    }

    private void recordFieldCheck() {
        IffPlayer selected = roster[selectedPlayerIndex];
        WitnessSnapshot witness = IffRadioWitnessStore.getWitness(selected.playerId);
        Snapshot confidence = confidenceFor(selected, witness);
        IffWitnessQuorum.Snapshot quorum = witnessQuorumFor(selected, witness);
        CombatSnapshot combat = combatFor(selected, confidence, quorum);
        IffOfficeProximityVerdict.Snapshot officeVerdict = officeProximityVerdict();
        IffDistanceTrend.Snapshot distanceTrend = distanceTrendFor(selected);
        boolean trustedPlayer = isTrustedPlayer(selected);
        String trustLabel = trustLabel(selected);
        String witnessState = witness == null
                ? "none"
                : witness.freshnessLabel() + " rssi=" + witness.rssi + " ageMs=" + witness.ageMs()
                + " ssid=\"" + safe(witness.ssid) + "\" bssid=" + safe(witness.bssid);
        FieldDiagnosticLog.event("IFF_DIAG", "event=field_check"
                + " playerId=" + selected.playerId
                + " displayName=\"" + safe(selected.displayName) + "\""
                + " localDevicePlayerId=" + localDevicePlayerId
                + " officeRole=" + officeTestRole(localDevicePlayer())
                + " selectedOfficeRole=" + officeTestRole(selected)
                + " selectedIsLocalDevice=" + isLocalDevice(selected)
                + " trustedPlayer=" + trustedPlayer
                + " trustLabel=" + trustLabel
                + " combatState=" + combat.state
                + " combatAction=" + combat.action
                + " identityLabel=" + confidence.identity.label
                + " identityScore=" + confidence.identity.score
                + " proximityLabel=" + confidence.proximity.label
                + " proximityScore=" + confidence.proximity.score
                + " positionLabel=" + confidence.position.label
                + " positionScore=" + confidence.position.score
                + " directionLabel=" + confidence.direction.label
                + " directionScore=" + confidence.direction.score
                + " operatorVerdict=" + operatorVerdictLabel(confidence, quorum)
                + " officeProximityVerdict=" + officeVerdict.label
                + " officeProximityDeltaDb=" + officeVerdict.deltaDb
                + " officeProximityReason=\"" + safe(officeVerdict.reason) + "\""
                + " officeProximityA=\"" + safe(officeSampleLabel("vasya")) + "\""
                + " officeProximityB=\"" + safe(officeSampleLabel("zhenya")) + "\""
                + " distanceClass=" + distanceTrend.distanceClass
                + " distanceConfidence=" + distanceTrend.distanceConfidence
                + " movementTrend=" + distanceTrend.movementTrend
                + " movementConfidence=" + distanceTrend.movementConfidence
                + " movementRssiDeltaDb=" + distanceTrend.movementRssiDeltaDb
                + " gpsStatus=" + gpsUiStatus().split(" ")[0]
                + " gpsAccuracyM=na gpsDistanceM=na gpsBearingDeg=na"
                + " witnessQuorum=" + quorum.label
                + " witnessFreshSources=" + quorum.freshSources
                + " witnessPossibleSources=" + quorum.possibleSources
                + " remoteWitnessContract=" + IffRemoteWitnessReport.CONTRACT_VERSION
                + " remoteReportCount=" + quorum.remoteReportCount
                + " remoteFreshSources=" + quorum.remoteFreshSources
                + " remoteStaleSources=" + quorum.remoteStaleSources
                + " fieldRadioStatus=\"" + safe(IffBleFieldRadio.compactStatus()) + "\""
                + " fieldRadioPolicy=\"" + safe(IffBleFieldRadio.lifecycleStatus()) + "\""
                + " wifiTargetStatus=\"" + safe(IffWifiTargetObservationStore.compactStatus()) + "\""
                + " fieldRadioEnabled=" + fieldRadioEnabled
                + " transportStatus=\"" + safe(IffUdpWitnessTransport.compactStatus()) + "\""
                + " witness=" + witnessState
                + " localApproach=" + approachActive);
        lastFieldCheckSummary = selected.displayName + ": identity " + confidence.identity.score
                + "% / proximity " + confidence.proximity.score + "% / trust " + trustLabel
                + " / combat " + combat.state
                + " / witness " + (witness == null ? "none" : witness.freshnessLabel());
        activeTab = TAB_CONTACT;
        render();
    }

    private void toggleSelectedTrust() {
        IffPlayer selected = roster[selectedPlayerIndex];
        if (isLocalDevice(selected)) {
            lastFieldCheckSummary = selected.displayName + ": trust is LOCAL_SELF";
            activeTab = TAB_CONTACT;
            render();
            return;
        }
        boolean trusted = !hasLocalTrust(selected);
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putBoolean(trustPreferenceKey(selected), trusted)
                .apply();
        lastFieldCheckSummary = selected.displayName + ": trust " + trustLabel(selected);
        FieldDiagnosticLog.event("IFF_DIAG", "event=iff_trust_toggle"
                + " playerId=" + selected.playerId
                + " displayName=\"" + safe(selected.displayName) + "\""
                + " trustedPlayer=" + isTrustedPlayer(selected)
                + " trustLabel=" + trustLabel(selected)
                + " localDevicePlayerId=" + localDevicePlayerId);
        activeTab = TAB_CONTACT;
        render();
    }

    private void resetBody() {
        bodyContainer.removeAllViews();
        bodyContainer.addView(body);
    }

    private void loadLocalDeviceIdentity() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String saved = prefs.getString(PREF_LOCAL_DEVICE_PLAYER_ID, "local-you");
        localDevicePlayerId = playerIndexForId(saved) >= 0 ? saved : "local-you";
    }

    private void setLocalDevicePlayer(int playerIndex) {
        IffPlayer player = roster[playerIndex];
        localDevicePlayerId = player.playerId;
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putString(PREF_LOCAL_DEVICE_PLAYER_ID, localDevicePlayerId)
                .apply();
        selectedPlayerIndex = playerIndex;
        activeTab = TAB_CONTACT;
        approachActive = false;
        handler.removeCallbacks(expireApproach);
        lastFieldCheckSummary = player.displayName + ": this device identity selected";
        FieldDiagnosticLog.event("IFF_DIAG", "event=device_identity_selected"
                + " localDevicePlayerId=" + player.playerId
                + " officeRole=" + officeTestRole(player)
                + " displayName=\"" + safe(player.displayName) + "\"");
        ensureFieldRadioService();
        render();
    }

    private void loadFieldRadioPreference() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        fieldRadioEnabled = prefs.getBoolean(PREF_FIELD_RADIO_ENABLED, true);
    }

    private void setFieldRadioEnabled(boolean enabled) {
        fieldRadioEnabled = enabled;
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putBoolean(PREF_FIELD_RADIO_ENABLED, fieldRadioEnabled)
                .apply();
    }

    private void toggleFieldRadioService() {
        setFieldRadioEnabled(!fieldRadioEnabled);
        if (fieldRadioEnabled) {
            IffForegroundRadioService.start(this, localDevicePlayerId);
        } else {
            IffForegroundRadioService.stop(this);
            IffBleFieldRadio.stop("operator_disabled");
        }
        FieldDiagnosticLog.event("IFF_DIAG", "event=iff_radio_operator_toggle"
                + " enabled=" + fieldRadioEnabled
                + " localDevicePlayerId=" + localDevicePlayerId
                + " service=\"" + safe(IffForegroundRadioService.compactStatus()) + "\""
                + " policy=\"" + safe(IffBleFieldRadio.lifecycleStatus()) + "\"");
        render();
    }

    private void ensureFieldRadioService() {
        if (fieldRadioEnabled) {
            IffForegroundRadioService.start(this, localDevicePlayerId);
        } else {
            IffForegroundRadioService.stop(this);
        }
    }

    private boolean isLocalDevice(IffPlayer player) {
        return player != null && player.playerId.equals(localDevicePlayerId);
    }

    private boolean isTrustedPlayer(IffPlayer player) {
        return isLocalDevice(player) || hasLocalTrust(player);
    }

    private boolean hasLocalTrust(IffPlayer player) {
        if (player == null || isLocalDevice(player)) {
            return false;
        }
        return getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getBoolean(trustPreferenceKey(player), false);
    }

    private String trustPreferenceKey(IffPlayer player) {
        return PREF_TRUSTED_PLAYER_PREFIX + player.playerId;
    }

    private String trustLabel(IffPlayer player) {
        if (isLocalDevice(player)) {
            return "LOCAL_SELF";
        }
        return hasLocalTrust(player) ? "LOCAL_TRUSTED" : "UNTRUSTED";
    }

    private String trustRosterBadge(IffPlayer player) {
        if (isLocalDevice(player)) {
            return "SELF";
        }
        return hasLocalTrust(player) ? "TRUST" : "UNTRUST";
    }

    private void renderTrustButton(IffPlayer selected) {
        if (isLocalDevice(selected)) {
            trustButton.setText("SELF");
            trustButton.setEnabled(false);
            trustButton.setTextColor(0xffb8c49a);
        } else if (hasLocalTrust(selected)) {
            trustButton.setText("UNTRUST");
            trustButton.setEnabled(true);
            trustButton.setTextColor(0xff7dff73);
        } else {
            trustButton.setText("TRUST");
            trustButton.setEnabled(true);
            trustButton.setTextColor(0xffffd16a);
        }
    }

    private IffPlayer localDevicePlayer() {
        int index = localDevicePlayerIndex();
        return roster[index < 0 ? LOCAL_PLAYER_INDEX : index];
    }

    private String officeTestRole(IffPlayer player) {
        if (player == null) {
            return "UNASSIGNED";
        }
        if ("vasya".equals(player.playerId)) {
            return "PHONE_A_WITNESS";
        }
        if ("zhenya".equals(player.playerId)) {
            return "PHONE_B_WITNESS";
        }
        if ("petya".equals(player.playerId)) {
            return "PHONE_C_MOVING_TARGET";
        }
        if ("local-you".equals(player.playerId)) {
            return "PHONE_OPERATOR";
        }
        return "UNASSIGNED";
    }

    private int localDevicePlayerIndex() {
        int index = playerIndexForId(localDevicePlayerId);
        return index < 0 ? LOCAL_PLAYER_INDEX : index;
    }

    private int playerIndexForId(String playerId) {
        if (playerId == null) {
            return -1;
        }
        for (int i = 0; i < roster.length; i++) {
            if (playerId.equals(roster[i].playerId)) {
                return i;
            }
        }
        return -1;
    }

    private Button createRosterButton(final int playerIndex) {
        IffPlayer player = roster[playerIndex];
        Button button = new Button(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(44));
        params.setMargins(0, dp(4), 0, 0);
        button.setLayoutParams(params);
        button.setBackgroundResource(R.drawable.popup_button);
        WitnessSnapshot witness = IffRadioWitnessStore.getWitness(player.playerId);
        Snapshot confidence = confidenceFor(player, witness);
        IffWitnessQuorum.Snapshot quorum = witnessQuorumFor(player, witness);
        CombatSnapshot combat = combatFor(player, confidence, quorum);
        button.setTextColor(playerIndex == selectedPlayerIndex ? 0xffffd16a : combatTextColor(combat));
        button.setText(player.displayName + (isLocalDevice(player) ? "  [THIS DEVICE]" : "")
                + (!isLocalDevice(player) && hasLocalTrust(player) ? "  [TRUSTED]" : "")
                + "\n" + operatorRosterLine(player, confidence, quorum, witness, combat));
        button.setTextSize(12);
        button.setTransformationMethod(null);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedPlayerIndex = playerIndex;
                activeTab = TAB_CONTACT;
                render();
            }
        });
        button.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                setLocalDevicePlayer(playerIndex);
                return true;
            }
        });
        return button;
    }

    private String rosterRadioLabel(IffPlayer player, WitnessSnapshot witness) {
        if (isLocalDevice(player) && approachActive) {
            return "LOCAL_ONLY";
        }
        if (witness == null) {
            return "UNKNOWN";
        }
        if (!witness.isFresh()) {
            return witness.freshnessLabel() + " " + formatAge(witness.ageMs());
        }
        return witness.proximityLabel() + " " + witness.rssi + "dBm "
                + (witness.isBleWitness() ? "BLE" : "WIFI");
    }

    private Snapshot confidenceFor(IffPlayer player, WitnessSnapshot witness) {
        return IffConfidence.evaluate(player.playerId, isLocalDevice(player), approachActive, isTrustedPlayer(player), witness);
    }

    private IffWitnessQuorum.Snapshot witnessQuorumFor(IffPlayer player, WitnessSnapshot witness) {
        int possibleSources = roster.length - 1;
        return IffWitnessQuorum.evaluate(player.playerId, witness, IffRemoteWitnessStore.getReportsFor(player.playerId), possibleSources);
    }

    private CombatSnapshot combatFor(IffPlayer selected, Snapshot confidence, IffWitnessQuorum.Snapshot quorum) {
        if (isLocalDevice(selected) && approachActive) {
            return new CombatSnapshot("LOCAL_DECLARED", "LOCAL_STATUS_ONLY",
                    "локальная кнопка сообщает намерение игрока, но не radio proof");
        }
        if (quorum.hasMultiWitness()) {
            return new CombatSnapshot("CURRENT_MULTI", "TRACK_CURRENT_CONTACT",
                    "несколько fresh witness источников; crypto identity все еще отдельно");
        }
        if (quorum.freshSources > 0) {
            return new CombatSnapshot("CURRENT_SINGLE", "WATCH_CURRENT_CONTACT",
                    "есть fresh radio witness, но только один источник");
        }
        if (quorum.staleSources > 0 || "STALE_RADIO".equals(confidence.proximity.label)) {
            return new CombatSnapshot("STALE", "DO_NOT_TREAT_AS_NEAR",
                    "есть только старое radio evidence; это не current proximity proof");
        }
        return new CombatSnapshot("UNKNOWN", "NO_CURRENT_CONTACT",
                "нет текущего radio witness; участник не считается обнаруженным рядом");
    }

    private String combatDetails(CombatSnapshot combat) {
        return "- state: " + combat.state + "\n"
                + "- action: " + combat.action + "\n"
                + "- reason: " + combat.reason;
    }

    private String confidenceDetails(Snapshot confidence) {
        return confidence.identity.detailLine("identity") + "\n"
                + confidence.proximity.detailLine("proximity") + "\n"
                + confidence.position.detailLine("position") + "\n"
                + confidence.direction.detailLine("direction");
    }

    private String decisionText(Snapshot confidence, IffWitnessQuorum.Snapshot quorum) {
        if (quorum.hasMultiWitness()) {
            return "- есть current multi-witness evidence: " + quorum.freshSources + "/" + quorum.possibleSources + "\n"
                    + "- это отдельный witness слой, не crypto identity\n"
                    + "- confidence layers остаются как показано выше\n"
                    + "- direction и точная position пока неизвестны";
        }
        if (quorum.freshSources > 0) {
            return "- есть current single-witness evidence\n"
                    + "- одного источника мало для quorum\n"
                    + "- identity не повышается без crypto\n"
                    + "- direction и точная position пока неизвестны";
        }
        if (quorum.staleSources > 0) {
            return "- есть только stale witness evidence\n"
                    + "- это память о старом сигнале, не текущий proof\n"
                    + "- для боевого решения держим контакт UNKNOWN\n"
                    + "- direction и точная position пока неизвестны";
        }
        if ("RADIO_NEAR".equals(confidence.proximity.label)) {
            return "- рядом слышен свежий beacon заявленного участника\n"
                    + "- это сильный proximity hint, но не crypto identity\n"
                    + "- quorum: " + quorum.compact() + ", multi-witness еще нет\n"
                    + "- direction и точная position пока неизвестны";
        }
        if ("RADIO_WEAK_HINT".equals(confidence.proximity.label)
                || "RADIO_EDGE_HINT".equals(confidence.proximity.label)) {
            return "- beacon слышен свежо, но RSSI не дает точную дистанцию\n"
                    + "- это слабая proximity-подсказка, не подтверждение близкого контакта\n"
                    + "- quorum: " + quorum.compact() + ", multi-witness еще нет\n"
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

    private String operatorDetails(IffPlayer selected, Snapshot confidence, IffWitnessQuorum.Snapshot quorum,
                                   CombatSnapshot combat) {
        return "- verdict: " + operatorVerdictLabel(confidence, quorum) + "\n"
                + "- combat: " + combat.state + " / " + combat.action + "\n"
                + "- trust: " + trustLabel(selected) + "\n"
                + "- current witnesses: " + quorum.freshSources + "/" + quorum.possibleSources + "\n"
                + "- stale evidence: " + quorum.staleSources + "\n"
                + "- remote fresh/stale/total: " + quorum.remoteFreshSources + "/"
                + quorum.remoteStaleSources + "/" + quorum.remoteReportCount + "\n"
                + "- field radio: " + IffBleFieldRadio.compactStatus() + "\n"
                + "- transport: " + IffUdpWitnessTransport.compactStatus() + "\n"
                + "- identity remains: " + confidence.identity.label + " " + confidence.identity.score + "%\n"
                + "- position/direction: UNKNOWN unless their own layers prove otherwise";
    }

    private String operatorVerdictLabel(Snapshot confidence, IffWitnessQuorum.Snapshot quorum) {
        if (quorum.hasMultiWitness()) {
            return "CURRENT_MULTI_WITNESS";
        }
        if (quorum.freshSources > 0) {
            return "CURRENT_SINGLE_WITNESS";
        }
        if (quorum.staleSources > 0) {
            return "STALE_EVIDENCE_ONLY";
        }
        if ("LOCAL_DECLARED_UNKNOWN".equals(confidence.proximity.label)) {
            return "LOCAL_DECLARED_ONLY";
        }
        return "NO_CURRENT_EVIDENCE";
    }

    private String operatorRosterLine(IffPlayer player, Snapshot confidence, IffWitnessQuorum.Snapshot quorum,
                                      WitnessSnapshot witness, CombatSnapshot combat) {
        return combat.state + " / " + operatorVerdictLabel(confidence, quorum) + " / id " + confidence.identity.score
                + "% / prox " + confidence.proximity.score + "% / " + trustRosterBadge(player)
                + " / " + rosterRadioLabel(player, witness);
    }

    private int combatTextColor(CombatSnapshot combat) {
        if (combat.state.startsWith("CURRENT")) {
            return 0xff7dff73;
        }
        if ("STALE".equals(combat.state) || "LOCAL_DECLARED".equals(combat.state)) {
            return 0xffffd16a;
        }
        return 0xffffffff;
    }

    private String witnessDetails(WitnessSnapshot witness) {
        if (witness == null) {
            return "- нет свежего или старого beacon witness\n"
                    + "- Wi-Fi legacy ищет SSID формата " + IffRadioWitnessStore.SSID_PREFIX + "*\n"
                    + "- BLE field radio: " + IffBleFieldRadio.compactStatus() + "\n"
                    + "- freshness policy: " + IffRadioWitnessStore.freshnessPolicyLabel();
        }
        return "- ssid: " + witness.ssid + "\n"
                + "- bssid: " + witness.bssid + "\n"
                + "- source: " + witness.sourceType() + "\n"
                + "- freshness: " + witness.freshnessLabel() + "\n"
                + "- policy: " + IffRadioWitnessStore.freshnessPolicyLabel() + "\n"
                + "- next transition: " + witness.nextTransitionLabel() + "\n"
                + "- age: " + formatAge(witness.ageMs()) + "\n"
                + "- rssi: " + witness.rssi + " dBm\n"
                + "- frequency: " + witness.frequency + " MHz";
    }

    private String fieldRadioPolicyDetails() {
        return "- lifecycle: " + IffBleFieldRadio.lifecycleStatus() + "\n"
                + "- operator control: " + (fieldRadioEnabled ? "ON" : "OFF") + "\n"
                + "- service: " + IffForegroundRadioService.compactStatus() + "\n"
                + "- foreground service keeps BLE radio outside visible IFF activity\n"
                + "- notification stop action stops BLE scan/advertise and logs service stop\n"
                + "- stale BLE/Wi-Fi witness remains visible but is not current proof\n"
                + "- expired witness returns proximity to UNKNOWN";
    }

    private String witnessQuorumDetails(IffPlayer selected, IffWitnessQuorum.Snapshot quorum) {
        StringBuilder builder = new StringBuilder();
        builder.append("- target: ").append(selected.displayName).append("\n")
                .append("- state: ").append(quorum.compact()).append("\n")
                .append("- local-device: ");
        if (quorum.localWitness == null) {
            builder.append("NO_REPORT\n");
        } else {
            builder.append(quorum.localWitness.freshnessLabel())
                    .append(" ")
                    .append(quorum.localWitness.rssi)
                    .append("dBm age=")
                    .append(formatAge(quorum.localWitness.ageMs()))
                    .append("\n");
        }
        builder.append("- remote contract: ").append(IffRemoteWitnessReport.CONTRACT_VERSION).append("\n")
                .append("- remote reports: ");
        if (quorum.remoteReports.size() == 0) {
            builder.append("none received\n");
        } else {
            builder.append(quorum.remoteReportCount).append(" received\n");
            for (int i = 0; i < quorum.remoteReports.size(); i++) {
                IffRemoteWitnessReport report = quorum.remoteReports.get(i);
                builder.append("  ")
                        .append(report.sourcePlayerId)
                        .append(" -> ")
                        .append(report.freshnessLabel())
                        .append(" ")
                        .append(report.rssi)
                        .append("dBm age=")
                        .append(formatAge(report.ageMs()))
                        .append(" signature=")
                        .append(report.signatureStatus)
                        .append("\n");
            }
        }
        builder.append("- transport: UDP diagnostic channel\n")
                .append("- signature: ").append(IffRemoteWitnessReport.SIGNATURE_PENDING).append("\n")
                .append("- identity is not upgraded by quorum without crypto");
        return builder.toString();
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

    private int strongProximityCount() {
        int count = 0;
        for (int i = 0; i < roster.length; i++) {
            IffPlayer player = roster[i];
            WitnessSnapshot witness = IffRadioWitnessStore.getWitness(player.playerId);
            Snapshot confidence = confidenceFor(player, witness);
            if ("RADIO_NEAR".equals(confidence.proximity.label)) {
                count++;
            }
        }
        return count;
    }

    private int multiWitnessCount() {
        int count = 0;
        for (int i = 0; i < roster.length; i++) {
            IffPlayer player = roster[i];
            WitnessSnapshot witness = IffRadioWitnessStore.getWitness(player.playerId);
            if (witnessQuorumFor(player, witness).hasMultiWitness()) {
                count++;
            }
        }
        return count;
    }

    private int trustedRosterCount() {
        int count = 0;
        for (int i = 0; i < roster.length; i++) {
            if (hasLocalTrust(roster[i])) {
                count++;
            }
        }
        return count;
    }

    private int currentWitnessEvidenceCount() {
        int count = 0;
        for (int i = 0; i < roster.length; i++) {
            IffPlayer player = roster[i];
            WitnessSnapshot witness = IffRadioWitnessStore.getWitness(player.playerId);
            if (witnessQuorumFor(player, witness).freshSources > 0) {
                count++;
            }
        }
        return count;
    }

    private int combatStateCount(String statePrefix) {
        int count = 0;
        for (int i = 0; i < roster.length; i++) {
            IffPlayer player = roster[i];
            if (isLocalDevice(player)) {
                continue;
            }
            WitnessSnapshot witness = IffRadioWitnessStore.getWitness(player.playerId);
            Snapshot confidence = confidenceFor(player, witness);
            IffWitnessQuorum.Snapshot quorum = witnessQuorumFor(player, witness);
            if (combatFor(player, confidence, quorum).state.startsWith(statePrefix)) {
                count++;
            }
        }
        return count;
    }

    private int staleWitnessEvidenceCount() {
        int count = 0;
        for (int i = 0; i < roster.length; i++) {
            IffPlayer player = roster[i];
            WitnessSnapshot witness = IffRadioWitnessStore.getWitness(player.playerId);
            IffWitnessQuorum.Snapshot quorum = witnessQuorumFor(player, witness);
            if (quorum.freshSources == 0 && quorum.staleSources > 0) {
                count++;
            }
        }
        return count;
    }

    private String teamOperatorSummaryLine() {
        int current = currentWitnessEvidenceCount();
        int stale = staleWitnessEvidenceCount();
        if (multiWitnessCount() > 0) {
            return "MULTI CURRENT";
        }
        if (current > 0) {
            return "SINGLE CURRENT";
        }
        if (stale > 0) {
            return "STALE ONLY";
        }
        return "NO CURRENT";
    }

    private IffOfficeProximityVerdict.Snapshot officeProximityVerdict() {
        return IffOfficeProximityVerdict.evaluate(
                localDevicePlayerId,
                officeWindowSample("vasya"),
                officeWindowSample("zhenya"));
    }

    private IffOfficeProximityVerdict.Sample officeWindowSample(String playerId) {
        RssiWindowSnapshot window = IffRadioWitnessStore.getRssiWindow(
                playerId,
                IffOfficeProximityVerdict.WINDOW_MS);
        return window.asOfficeSample();
    }

    private String officeProximityLine() {
        IffOfficeProximityVerdict.Snapshot snapshot = officeProximityVerdict();
        return snapshot.compact();
    }

    private String officeProximitySamplesLine() {
        return "C-A " + officeSampleLabel("vasya") + " / C-B " + officeSampleLabel("zhenya");
    }

    private IffDistanceTrend.Snapshot distanceTrendFor(IffPlayer player) {
        if (player == null || isLocalDevice(player)) {
            return IffDistanceTrend.evaluate(null, null);
        }
        RssiWindowSnapshot current = IffRadioWitnessStore.getRssiWindow(
                player.playerId,
                DISTANCE_WINDOW_MS);
        RssiWindowSnapshot previous = IffRadioWitnessStore.getPreviousRssiWindow(
                player.playerId,
                DISTANCE_WINDOW_MS);
        return IffDistanceTrend.evaluate(current.asDistanceSample(), previous.asDistanceSample());
    }

    private String officeDistanceTrendLine() {
        RssiWindowSnapshot sideA = IffRadioWitnessStore.getRssiWindow("vasya", DISTANCE_WINDOW_MS);
        RssiWindowSnapshot sideB = IffRadioWitnessStore.getRssiWindow("zhenya", DISTANCE_WINDOW_MS);
        RssiWindowSnapshot current = strongestUsable(sideA, sideB);
        if (current == null) {
            return IffDistanceTrend.evaluate(null, null).compact();
        }
        RssiWindowSnapshot previous = IffRadioWitnessStore.getPreviousRssiWindow(
                current.playerId,
                DISTANCE_WINDOW_MS);
        return IffDistanceTrend.evaluate(current.asDistanceSample(), previous.asDistanceSample()).compact();
    }

    private RssiWindowSnapshot strongestUsable(RssiWindowSnapshot left, RssiWindowSnapshot right) {
        boolean leftUsable = left != null && left.fresh && left.validCount > 0;
        boolean rightUsable = right != null && right.fresh && right.validCount > 0;
        if (!leftUsable && !rightUsable) {
            return null;
        }
        if (leftUsable && !rightUsable) {
            return left;
        }
        if (rightUsable && !leftUsable) {
            return right;
        }
        return left.averageRssi >= right.averageRssi ? left : right;
    }

    private String gpsUiStatus() {
        if (!hasLocationPermission()) {
            return "GPS_UNAVAILABLE";
        }
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            return "GPS_UNAVAILABLE";
        }
        Location best = bestLastKnownLocation(locationManager);
        if (best == null) {
            return "GPS_UNAVAILABLE";
        }
        long ageMs = Math.max(0L, System.currentTimeMillis() - best.getTime());
        return IffGpsSnapshot.from(
                ageMs,
                best.hasAccuracy(),
                best.hasAccuracy() ? best.getAccuracy() : -1.0f,
                best.hasBearing(),
                best.hasBearing() ? best.getBearing() : -1.0f).compact();
    }

    private String fieldRunHeader() {
        String compact = IffFieldRunSummary.compact();
        int officeIndex = compact.indexOf(" office=");
        if (officeIndex <= 0) {
            return compact;
        }
        return compact.substring(0, officeIndex);
    }

    private boolean hasLocationPermission() {
        if (Build.VERSION.SDK_INT < 23) {
            return true;
        }
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @Nullable
    private Location bestLastKnownLocation(LocationManager locationManager) {
        Location gps = lastKnown(locationManager, LocationManager.GPS_PROVIDER);
        Location network = lastKnown(locationManager, LocationManager.NETWORK_PROVIDER);
        if (gps == null) {
            return network;
        }
        if (network == null) {
            return gps;
        }
        return gps.getTime() >= network.getTime() ? gps : network;
    }

    @Nullable
    private Location lastKnown(LocationManager locationManager, String provider) {
        try {
            if (!locationManager.isProviderEnabled(provider)) {
                return null;
            }
            return locationManager.getLastKnownLocation(provider);
        } catch (SecurityException e) {
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private String officeSampleLabel(String playerId) {
        RssiWindowSnapshot window = IffRadioWitnessStore.getRssiWindow(
                playerId,
                IffOfficeProximityVerdict.WINDOW_MS);
        if (window.validCount <= 0 && window.outlier127Count <= 0) {
            return "missing";
        }
        return window.freshnessLabel()
                + " avg=" + window.averageRssi + "dBm"
                + " n=" + window.validCount
                + " out127=" + window.outlier127Count
                + " newest=" + formatAge(window.newestAgeMs);
    }

    private int remoteReportCount() {
        int count = 0;
        for (int i = 0; i < roster.length; i++) {
            count += IffRemoteWitnessStore.reportCountFor(roster[i].playerId);
        }
        return count;
    }

    private String simpleMapWitnessList() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < roster.length; i++) {
            IffPlayer player = roster[i];
            WitnessSnapshot witness = IffRadioWitnessStore.getWitness(player.playerId);
            builder.append(player.displayName)
                    .append(": ")
                    .append(mapRadioLabel(witness, witnessQuorumFor(player, witness)))
                    .append("\n");
        }
        return builder.toString();
    }

    private IffFieldMapSnapshot fieldMapSnapshot() {
        IffFieldLocatorSnapshot locator = IffFieldLocatorSnapshot.from(
                IffWifiTargetObservationStore.snapshot(),
                mapRadioDistanceTrend(),
                IffGpsSnapshot.unavailable());
        IffFieldMapSnapshot raw = IffFieldMapSnapshot.from(locator, IffWifiTargetObservationStore.compactStatus());
        return operatorFieldSnapshotStore.update(raw, SystemClock.elapsedRealtime());
    }

    private IffDistanceTrend.Snapshot mapRadioDistanceTrend() {
        if (!IffWifiTargetObservationStore.TARGET_PLAYER_ID.equals(localDevicePlayerId)) {
            return distanceTrendFor(playerById(IffWifiTargetObservationStore.TARGET_PLAYER_ID));
        }
        RssiWindowSnapshot vasya = IffRadioWitnessStore.getRssiWindow("vasya", DISTANCE_WINDOW_MS);
        RssiWindowSnapshot petya = IffRadioWitnessStore.getRssiWindow("petya", DISTANCE_WINDOW_MS);
        RssiWindowSnapshot current = strongestUsable(vasya, petya);
        if (current == null) {
            return IffDistanceTrend.evaluate(null, null);
        }
        RssiWindowSnapshot previous = IffRadioWitnessStore.getPreviousRssiWindow(
                current.playerId,
                DISTANCE_WINDOW_MS);
        return IffDistanceTrend.evaluate(current.asDistanceSample(), previous.asDistanceSample());
    }

    private IffPlayer playerById(String playerId) {
        for (int i = 0; i < roster.length; i++) {
            if (roster[i].playerId.equals(playerId)) {
                return roster[i];
            }
        }
        return null;
    }

    private String fieldMapSummary() {
        IffFieldMapSnapshot map = fieldMapSnapshot();
        return "FIELD MAP\n"
                + "- " + map.statusLine + "\n"
                + "- anchors: " + IffWifiTargetObservationStore.compactStatus() + "\n"
                + "- radio fallback: " + mapRadioDistanceTrend().compact() + "\n\n"
                + simpleMapWitnessList();
    }

    private String mapWitnessList() {
        StringBuilder builder = new StringBuilder();
        builder.append("FIELD CONTACTS\n\n");
        for (int i = 0; i < roster.length; i++) {
            IffPlayer player = roster[i];
            WitnessSnapshot witness = IffRadioWitnessStore.getWitness(player.playerId);
            builder.append(player.displayName)
                    .append(": ")
                    .append(witnessQuorumFor(player, witness).compact())
                    .append(" / ")
                    .append(witness == null ? "radio UNKNOWN" : witness.freshnessLabel() + " " + witness.rssi + "dBm age=" + formatAge(witness.ageMs()))
                    .append("\n");
        }
        builder.append("\nGPS и направление будут отдельными слоями уверенности.\n")
                .append("BLE field radio does not require shared Wi-Fi.\n")
                .append("Freshness policy: ").append(IffRadioWitnessStore.freshnessPolicyLabel()).append("\n")
                .append("BLE lifecycle: ").append(IffBleFieldRadio.lifecycleStatus());
        return builder.toString();
    }

    private List<IffTacticalMapView.MapPoint> mapPoints() {
        List<IffTacticalMapView.MapPoint> points = new ArrayList<>();
        for (int i = 0; i < roster.length; i++) {
            IffPlayer player = roster[i];
            WitnessSnapshot witness = IffRadioWitnessStore.getWitness(player.playerId);
            IffWitnessQuorum.Snapshot quorum = witnessQuorumFor(player, witness);
            boolean current = quorum.freshSources > 0;
            boolean stale = !current && quorum.staleSources > 0;
            points.add(new IffTacticalMapView.MapPoint(
                    player.displayName + (isLocalDevice(player) ? " [THIS]" : ""),
                    mapRadioLabel(witness, quorum),
                    isLocalDevice(player),
                    i == selectedPlayerIndex,
                    current,
                    stale));
        }
        return points;
    }

    private String mapRadioLabel(WitnessSnapshot witness, IffWitnessQuorum.Snapshot quorum) {
        if (witness != null) {
            return witness.sourceType() + " " + witness.freshnessLabel() + " "
                    + witness.rssi + "dBm " + formatAge(witness.ageMs());
        }
        if (quorum.remoteFreshSources > 0) {
            return "REMOTE_FRESH x" + quorum.remoteFreshSources;
        }
        if (quorum.staleSources > 0) {
            return "STALE_EVIDENCE";
        }
        return "UNKNOWN";
    }

    private String formatAge(long ageMs) {
        if (ageMs < 1000L) {
            return ageMs + "ms";
        }
        return (ageMs / 1000L) + "s";
    }

    private String safe(String value) {
        return value == null ? "" : value.replace('\n', ' ').replace('\r', ' ');
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

    private static final class CombatSnapshot {
        final String state;
        final String action;
        final String reason;

        CombatSnapshot(String state, String action, String reason) {
            this.state = state;
            this.action = action;
            this.reason = reason;
        }
    }
}
