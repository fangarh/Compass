package net.afterday.compas;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.afterday.compas.iff.IffBleFieldRadio;
import net.afterday.compas.iff.IffConfidence;
import net.afterday.compas.iff.IffConfidence.Snapshot;
import net.afterday.compas.iff.IffDistanceTrend;
import net.afterday.compas.iff.IffFieldTeamProfile;
import net.afterday.compas.iff.IffFieldLocatorSnapshot;
import net.afterday.compas.iff.IffFieldMapSnapshot;
import net.afterday.compas.iff.IffFieldRunSummary;
import net.afterday.compas.iff.IffForegroundRadioService;
import net.afterday.compas.iff.IffGpsSnapshot;
import net.afterday.compas.iff.IffMapScale;
import net.afterday.compas.iff.IffOfficeProximityVerdict;
import net.afterday.compas.iff.IffOperatorFieldSnapshotStore;
import net.afterday.compas.iff.IffParticipantMapModel;
import net.afterday.compas.iff.IffParticipantState;
import net.afterday.compas.iff.IffRemoteWitnessReport;
import net.afterday.compas.iff.IffRemoteWitnessStore;
import net.afterday.compas.iff.IffRadioWitnessStore;
import net.afterday.compas.iff.IffRadioWitnessStore.RssiWindowSnapshot;
import net.afterday.compas.iff.IffRadioWitnessStore.WitnessSnapshot;
import net.afterday.compas.iff.IffTacticalMapView;
import net.afterday.compas.iff.IffTeamRosterStore;
import net.afterday.compas.iff.IffUdpWitnessTransport;
import net.afterday.compas.iff.IffWitnessQuorum;
import net.afterday.compas.iff.IffWifiTargetObservationStore;
import net.afterday.compas.logging.FieldDiagnosticLog;

public class IffActivity extends Activity implements SensorEventListener {
    private static final int TAB_CONTACT = 0;
    private static final int TAB_TEAM = 1;
    private static final int TAB_MAP = 2;
    private static final int TAB_LOG = 3;
    private static final int LOCAL_PLAYER_INDEX = 0;
    private static final long APPROACH_DURATION_MS = 120000L;
    private static final long RADIO_REFRESH_MS = 1000L;
    private static final long DISTANCE_WINDOW_MS = 6000L;
    private static final String PREFS_NAME = "iff";
    private static final String PREF_LOCAL_DEVICE_PLAYER_ID = "local_device_player_id";
    private static final String PREF_PLAYER_DISPLAY_NAME_PREFIX = "player_display_name_";
    private static final String PREF_FIELD_RADIO_ENABLED = "field_radio_enabled";
    private static final String PREF_MAP_SCALE_RANGE_METERS = "map_scale_range_meters";
    private static final String PREF_TRUSTED_PLAYER_PREFIX = "trusted_player_";
    private static final String PREF_TEAM_ROSTER = "team_roster";
    private static final String PREF_TEAM_REMOVED_PLAYERS = "team_removed_players";

    private IffPlayer[] roster = new IffPlayer[] {
            new IffPlayer("local-you", "Вы", true),
            new IffPlayer("petya", "Петя", false),
            new IffPlayer("vasya", "Вася", false),
            new IffPlayer("zhenya", "Женя", false)
    };
    private final List<String> removedPlayerIds = new ArrayList<String>();

    private final Handler handler = new Handler();
    private final IffOperatorFieldSnapshotStore operatorFieldSnapshotStore =
            new IffOperatorFieldSnapshotStore();
    private int activeTab = TAB_TEAM;
    private int selectedPlayerIndex = LOCAL_PLAYER_INDEX;
    private String localDevicePlayerId = "local-you";
    private boolean approachActive;
    private long approachUntilMs;
    private SensorManager sensorManager;
    private Sensor rotationSensor;
    private Sensor accelerometerSensor;
    private Sensor magneticSensor;
    private final float[] rotationMatrix = new float[9];
    private final float[] orientation = new float[3];
    private final float[] accelValues = new float[3];
    private final float[] magneticValues = new float[3];
    private boolean hasAccelValues;
    private boolean hasMagneticValues;
    private boolean headingAvailable;
    private float phoneHeadingDeg;
    private long lastHeadingRenderElapsedMs;

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
    private int mapScaleRangeMeters = IffMapScale.defaultRangeMeters();
    private boolean teamSearchActive;

    private final Runnable expireApproach = new Runnable() {
        @Override
        public void run() {
            approachActive = false;
            IffForegroundRadioService.clearApproach();
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
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.iff_activity);
        loadTeamRoster();
        loadLocalDeviceIdentity();
        loadPlayerDisplayNames();
        loadFieldRadioPreference();
        loadMapScalePreference();
        bindViews();
        setTypeface();
        setListeners();
        setupHeadingSensors();
        render();
    }

    @Override
    protected void onResume() {
        super.onResume();
        IffUdpWitnessTransport.ensureStarted();
        ensureFieldRadioService();
        startHeadingSensors();
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
        stopHeadingSensors();
        IffUdpWitnessTransport.stop();
        super.onPause();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event == null || event.sensor == null) {
            return;
        }
        int type = event.sensor.getType();
        if (type == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
            updateHeadingFromMatrix();
        } else if (type == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, accelValues, 0, Math.min(event.values.length, accelValues.length));
            hasAccelValues = true;
            updateHeadingFromAccelMag();
        } else if (type == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, magneticValues, 0, Math.min(event.values.length, magneticValues.length));
            hasMagneticValues = true;
            updateHeadingFromAccelMag();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
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
                if (activeTab == TAB_MAP) {
                    return;
                }
                toggleFieldRadioService();
            }
        });
    }

    private void setupHeadingSensors() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager == null) {
            return;
        }
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    private void startHeadingSensors() {
        if (sensorManager == null) {
            return;
        }
        if (rotationSensor != null) {
            sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_GAME);
            return;
        }
        if (accelerometerSensor != null) {
            sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_GAME);
        }
        if (magneticSensor != null) {
            sensorManager.registerListener(this, magneticSensor, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    private void stopHeadingSensors() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    private void updateHeadingFromAccelMag() {
        if (!hasAccelValues || !hasMagneticValues) {
            return;
        }
        if (SensorManager.getRotationMatrix(rotationMatrix, null, accelValues, magneticValues)) {
            updateHeadingFromMatrix();
        }
    }

    private void updateHeadingFromMatrix() {
        SensorManager.getOrientation(rotationMatrix, orientation);
        phoneHeadingDeg = normalizeDegrees((float) Math.toDegrees(orientation[0]));
        headingAvailable = true;
        long now = SystemClock.elapsedRealtime();
        if (activeTab == TAB_MAP && now - lastHeadingRenderElapsedMs >= 250L) {
            lastHeadingRenderElapsedMs = now;
            render();
        }
    }

    private float normalizeDegrees(float degrees) {
        float value = degrees % 360.0f;
        return value < 0.0f ? value + 360.0f : value;
    }

    private void toggleApproach() {
        approachActive = !approachActive;
        if (approachActive) {
            approachUntilMs = System.currentTimeMillis() + APPROACH_DURATION_MS;
            selectedPlayerIndex = localDevicePlayerIndex();
            activeTab = TAB_CONTACT;
            IffForegroundRadioService.activateApproach();
            if (fieldRadioEnabled) {
                IffForegroundRadioService.start(this, localDevicePlayerId, localDevicePlayer().displayName);
            }
            scheduleApproachExpire();
        } else {
            IffForegroundRadioService.clearApproach();
            handler.removeCallbacks(expireApproach);
        }
        render();
    }

    private void scheduleApproachExpire() {
        handler.removeCallbacks(expireApproach);
        handler.postDelayed(expireApproach, Math.max(0L, approachUntilMs - System.currentTimeMillis()));
    }

    private void render() {
        ensureValidRosterSelection();
        IffPlayer selected = roster[selectedPlayerIndex];
        if (activeTab == TAB_CONTACT && !isLocalDevice(selected)) {
            approachButton.setText("ЭТОТ ТЕЛ.");
        } else {
            approachButton.setText(approachActive ? "ОТМЕНИТЬ ПОДХОД" : "Я ПОДХОЖУ");
        }
        renderTrustButton(selected);
        if (activeTab == TAB_MAP) {
            radioServiceButton.setText("РАЦИЯ КАРТЫ");
            radioServiceButton.setEnabled(false);
            radioServiceButton.setTextColor(0xffb8c49a);
        } else {
            radioServiceButton.setText(fieldRadioEnabled ? "РАЦИЯ ВКЛ" : "РАЦИЯ ВЫКЛ");
            radioServiceButton.setEnabled(true);
            radioServiceButton.setTextColor(fieldRadioEnabled ? 0xff7dff73 : 0xffffd16a);
        }
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
        String selectedDisplayName = displayNameFor(selected);
        title.setText(localApproachSelected ? "ВЫ ПОДХОДИТЕ" : selectedDisplayName);
        subtitle.setText(selectedIsLocalDevice ? "этот телефон объявляет этого участника" : "локальный состав, доверие и радиосвидетель");
        status.setText("БОЕВОЙ СТАТУС: " + combatStateDisplay(combat) + " / " + combatActionDisplay(combat) + "\n"
                + "ОЦЕНКА: " + operatorVerdictDisplay(confidence, quorum) + "\n"
                + "РОЛЬ ТЕСТА: " + officeTestRole(localDevicePlayer()) + "\n"
                + "УВЕРЕННОСТЬ\n" + ruStatus(confidence.compactStatus()) + "\nСВИДЕТЕЛИ: " + ruStatus(quorum.compact()));
        body.setText("ИГРОК\n"
                + "- имя: " + selectedDisplayName + "\n"
                + "- id: " + selected.playerId + "\n"
                + "- команда: локальная IFF группа\n"
                + "- роль теста: " + officeTestRole(selected) + "\n"
                + "- роль этого телефона: " + officeTestRole(localDevicePlayer()) + "\n"
                + "- доверие: " + trustLabel(selected) + "\n"
                + "- ожидаемый маяк: " + IffRadioWitnessStore.expectedBeaconSsid(selected.playerId) + "\n\n"
                + "БОЕВОЙ ВИД\n"
                + combatDetails(combat) + "\n\n"
                + "ВИД ОПЕРАТОРА\n"
                + operatorDetails(selected, confidence, quorum, combat) + "\n\n"
                + "СЛОИ УВЕРЕННОСТИ\n"
                + confidenceDetails(confidence) + "\n\n"
                + "СВИДЕТЕЛИ\n"
                + witnessDetails(witness) + "\n\n"
                + "ПРАВИЛА ПОЛЕВОЙ РАЦИИ\n"
                + fieldRadioPolicyDetails() + "\n\n"
                + "КВОРУМ СВИДЕТЕЛЕЙ\n"
                + witnessQuorumDetails(selected, quorum) + "\n\n"
                + "РЕШЕНИЕ\n"
                + decisionText(confidence, quorum) + "\n\n"
                + "ПОЛЕВАЯ ПРОВЕРКА\n"
                + "- последняя запись: " + lastFieldCheckSummary);
    }

    private void renderTeam() {
        resetBody();
        if (SystemClock.elapsedRealtime() >= 0L) {
            renderTeamGame();
            return;
        }
        title.setText("КОМАНДА");
        subtitle.setText("локальный состав и личность полевой рации");
        status.setText((approachActive ? "ВЫ        ПОДХОДИТЕ   локально\n" : "")
                + "ЭТОТ ТЕЛЕФОН: " + localDevicePlayer().displayName + "\n"
                + "РОЛЬ ТЕСТА: " + officeTestRole(localDevicePlayer()) + "\n"
                + "ИТОГ ТЕСТА: " + officeProximityLine() + "\n"
                + "ОПЕРАТОР: " + teamOperatorSummaryLine()
                + " / ДОВЕРЕНЫ " + trustedRosterCount() + "/" + (roster.length - 1) + "\n"
                + "БОЙ: свежие " + combatStateCount("CURRENT")
                + " / старые " + combatStateCount("STALE")
                + " / нет данных " + combatStateCount("UNKNOWN") + "\n"
                + "СВИДЕТЕЛИ: свежие " + currentWitnessEvidenceCount()
                + " / старые " + staleWitnessEvidenceCount()
                + " / рация " + freshWitnessCount() + "/" + strongProximityCount() + "\n"
                + "ПОЛЕВАЯ РАЦИЯ: " + radioEnabledLabel()
                + " / " + IffBleFieldRadio.compactStatus() + "\n"
                + "УДАЛЕННЫЕ ОТЧЕТЫ: " + remoteReportCount()
                + " / НАПРАВЛЕНИЕ: НЕТ ДАННЫХ");
        body.setText("Выберите участника, чтобы открыть карточку контакта.\n"
                + "Долгое нажатие назначает, кем является этот телефон.\n"
                + "Доверие помечает участника локально доверенным, но не доказывает близость.\n"
                + "Боевой статус показывает свежие/старые/неизвестные данные отдельно от личности.\n"
                + "Сводка оператора отделяет свежих свидетелей от старых свидетельств.\n"
                + "Проценты - текущая уверенность слоя, а не финальное доказательство.\n"
                + "Полевая рация не должна требовать общей Wi-Fi сети.\n"
                + "Тест близости: " + officeProximityLine() + "\n"
                + "Замеры теста: " + officeProximitySamplesLine() + "\n"
                + "Управление рацией: " + radioEnabledLabel() + "\n"
                + "Сервис рации: " + IffForegroundRadioService.compactStatus() + "\n"
                + "Состояние BLE: " + IffBleFieldRadio.lifecycleStatus() + "\n"
                + "BLE-радио: " + IffBleFieldRadio.compactStatus() + "\n"
                + "Доверенные участники: " + trustedRosterCount() + "/" + (roster.length - 1) + "\n"
                + "Контракт удаленных свидетелей: " + IffRemoteWitnessReport.CONTRACT_VERSION + "\n"
                + "Статус подписи пока заглушка: " + IffRemoteWitnessReport.SIGNATURE_PENDING + "\n"
                + "Отладка UDP: " + IffUdpWitnessTransport.compactStatus() + "\n"
                + "Транспорт: диагностический канал UDP.\n"
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
        String selectedDisplayName = displayNameFor(selected);
        title.setText(approachActive && selectedIsLocalDevice ? "ПОДХОД" : selectedDisplayName);
        subtitle.setText(selectedIsLocalDevice ? "личность этого телефона" : "полевой контакт");
        status.setText("БОЕВОЙ СТАТУС: " + combatStateDisplay(combat) + " / " + combatActionDisplay(combat) + "\n"
                + "ОЦЕНКА: " + operatorVerdictDisplay(confidence, quorum) + "\n"
                + "БЛИЗОСТЬ: " + confidence.proximity.label + " " + confidence.proximity.score + "%\n"
                + "ДИСТАНЦИЯ: " + ruStatus(distanceTrendFor(selected).compact()) + "\n"
                + "СВИДЕТЕЛИ: " + ruStatus(quorum.compact()));
        body.setText("ИГРОК\n"
                + "- имя: " + selectedDisplayName + "\n"
                + "- id: " + selected.playerId + "\n"
                + "- доверие: " + trustLabel(selected) + "\n"
                + "- рация: " + rosterRadioLabel(selected, witness) + "\n"
                + "- дистанция: " + ruStatus(distanceTrendFor(selected).compact()) + "\n"
                + "- роль теста: " + officeTestRole(selected) + "\n\n"
                + "ДЕЙСТВИЕ\n"
                + "- " + combatActionDisplay(combat) + "\n"
                + "- последняя проверка: " + lastFieldCheckSummary);
        if (!selectedIsLocalDevice) {
            bodyContainer.addView(createRemoveTeamMemberButton(selected));
        }
    }

    private void renderTeamGame() {
        title.setText("КОМАНДА");
        subtitle.setText("полевой состав");
        status.setText((approachActive ? "ЛОКАЛЬНО: ПОДХОД\n" : "")
                + "ЭТОТ ТЕЛЕФОН: " + fieldRoleLabel(localDevicePlayerId) + " / "
                + localDevicePlayer().displayName + "\n"
                + "ПОЛЕВАЯ РАЦИЯ: " + radioEnabledLabel()
                + " / " + ruStatus(IffBleFieldRadio.compactStatus()) + "\n"
                + "КОНТАКТЫ: свежие " + currentWitnessEvidenceCount()
                + " / старые " + staleWitnessEvidenceCount()
                + " / нет данных " + combatStateCount("UNKNOWN") + "\n"
                + "КАРТА: " + ruStatus(participantMapSummary(participantMapSnapshot())));
        bodyContainer.removeAllViews();
        bodyContainer.addView(createLocalNameButton());
        bodyContainer.addView(createFieldSetupSummary());
        bodyContainer.addView(createFieldRoleControls());
        bodyContainer.addView(createResetFieldTeamButton());
        for (int i = 0; i < roster.length; i++) {
            bodyContainer.addView(createRosterButton(i));
        }
        body.setText(fieldReadinessDetails());
        bodyContainer.addView(body);
    }

    private void renderMap() {
        resetBody();
        if (SystemClock.elapsedRealtime() >= 0L) {
            renderMapGame();
            return;
        }
        title.setText("КАРТА");
        setHeaderVisible(false);
        subtitle.setText("карта дистанции");
        status.setText("");
        bodyContainer.removeAllViews();
        IffTacticalMapView mapView = new IffTacticalMapView(this);
        LinearLayout.LayoutParams mapParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                mapHeightPx());
        mapParams.setMargins(0, 0, 0, 0);
        mapView.setLayoutParams(mapParams);
        mapView.setState(localDevicePlayer().displayName, mapPoints());
        mapView.setParticipantState(participantMapSnapshot());
        mapView.setMapRangeMeters(mapScaleRangeMeters);
        mapView.setPhoneHeading(headingAvailable, phoneHeadingDeg);
        bodyContainer.addView(mapView);
        bodyContainer.addView(createMapScaleControls());
    }

    private void renderMapGame() {
        setHeaderVisible(false);
        title.setText("КАРТА");
        subtitle.setText("карта дистанции");
        status.setText("");
        bodyContainer.removeAllViews();
        IffTacticalMapView mapView = new IffTacticalMapView(this);
        LinearLayout.LayoutParams mapParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                mapHeightPx());
        mapParams.setMargins(0, 0, 0, 0);
        mapView.setLayoutParams(mapParams);
        mapView.setState(localDevicePlayer().displayName, mapPoints());
        mapView.setParticipantState(participantMapSnapshot());
        mapView.setMapRangeMeters(mapScaleRangeMeters);
        mapView.setPhoneHeading(headingAvailable, phoneHeadingDeg);
        bodyContainer.addView(mapView);
        bodyContainer.addView(createMapScaleControls());
    }

    private void renderLog() {
        resetBody();
        title.setText("ЖУРНАЛ");
        subtitle.setText("полевая диагностика");
        status.setText("РАЦИЯ: " + radioEnabledLabel() + "\n"
                + "ЭТОТ ТЕЛЕФОН: " + fieldRoleLabel(localDevicePlayerId) + " / "
                + localDevicePlayer().displayName + "\n"
                + "ДИСТАНЦИЯ: " + ruStatus(distanceTrendFor(roster[selectedPlayerIndex]).compact()) + "\n"
                + "GPS: " + ruStatus(gpsUiStatus()) + "\n"
                + "ЗАПУСК: " + fieldRunHeader() + "\n"
                + "ПОСЛЕДНЯЯ ПРОВЕРКА: " + lastFieldCheckSummary);
        IffPlayer selected = roster[selectedPlayerIndex];
        WitnessSnapshot witness = IffRadioWitnessStore.getWitness(selected.playerId);
        Snapshot confidence = confidenceFor(selected, witness);
        IffWitnessQuorum.Snapshot quorum = witnessQuorumFor(selected, witness);
        CombatSnapshot combat = combatFor(selected, confidence, quorum);
        body.setText("ВЫБРАНО\n"
                + operatorDetails(selected, confidence, quorum, combat) + "\n\n"
                + "УВЕРЕННОСТЬ\n"
                + confidenceDetails(confidence) + "\n\n"
                + "СВИДЕТЕЛИ\n"
                + witnessDetails(witness) + "\n\n"
                + "ПОЛЕВАЯ РАЦИЯ\n"
                + fieldRadioPolicyDetails() + "\n\n"
                + "ГОТОВНОСТЬ В ПОЛЕ\n"
                + fieldReadinessDetails() + "\n\n"
                + "ТЕСТОВАЯ СХЕМА\n"
                + "- итог: " + officeProximityLine() + "\n"
                + "- замеры: " + officeProximitySamplesLine() + "\n\n"
                + "ДИСТАНЦИЯ / ДВИЖЕНИЕ\n"
                + "- выбранный: " + ruStatus(distanceTrendFor(selected).compact()) + "\n"
                + "- тестовая схема: " + ruStatus(officeDistanceTrendLine()) + "\n"
                + "- gps: " + ruStatus(gpsUiStatus()) + "\n\n"
                + "ПОЛЕВОЙ ЛОКАТОР\n"
                + "- сервис: " + ruStatus(IffForegroundRadioService.compactStatus()) + "\n"
                + "- две опоры: " + ruStatus(IffWifiTargetObservationStore.compactStatus()) + "\n\n"
                + "КАРТА\n"
                + fieldMapSummary() + "\n\n"
                + participantMapDetails(participantMapSnapshot()) + "\n\n"
                + "ПОЛЕВОЙ ЗАПУСК\n"
                + IffFieldRunSummary.details() + "\n\n"
                + "ПОДТВЕРЖДЕНИЯ\n"
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
        lastFieldCheckSummary = displayNameFor(selected) + ": личность " + confidence.identity.score
                + "% / близость " + confidence.proximity.score + "% / доверие " + trustLabel
                + " / бой " + combatStateDisplay(combat)
                + " / свидетель " + (witness == null ? "нет" : witness.freshnessLabel());
        activeTab = TAB_CONTACT;
        render();
    }

    private void toggleSelectedTrust() {
        IffPlayer selected = roster[selectedPlayerIndex];
        if (isLocalDevice(selected)) {
            lastFieldCheckSummary = displayNameFor(selected) + ": это текущий телефон";
            activeTab = TAB_CONTACT;
            render();
            return;
        }
        boolean trusted = !hasLocalTrust(selected);
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putBoolean(trustPreferenceKey(selected), trusted)
                .apply();
        lastFieldCheckSummary = displayNameFor(selected) + ": доверие " + trustLabel(selected);
        FieldDiagnosticLog.event("IFF_DIAG", "event=iff_trust_toggle"
                + " playerId=" + selected.playerId
                + " displayName=\"" + safe(displayNameFor(selected)) + "\""
                + " trustedPlayer=" + isTrustedPlayer(selected)
                + " trustLabel=" + trustLabel(selected)
                + " localDevicePlayerId=" + localDevicePlayerId);
        activeTab = TAB_CONTACT;
        render();
    }

    private void resetBody() {
        setHeaderVisible(true);
        bodyContainer.removeAllViews();
        bodyContainer.addView(body);
    }

    private void setHeaderVisible(boolean visible) {
        int visibility = visible ? View.VISIBLE : View.GONE;
        title.setVisibility(visibility);
        subtitle.setVisibility(visibility);
        status.setVisibility(visibility);
    }

    private int mapHeightPx() {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int availableWidth = Math.max(dp(320), metrics.widthPixels - dp(20));
        int desiredHeight = Math.max(dp(180), Math.round(availableWidth * 9.0f / 16.0f));
        int reservedHeight = dp(10 + 38 + 8 + 8 + 44 + 10 + 52);
        int availableHeight = Math.max(dp(180), metrics.heightPixels - reservedHeight);
        return Math.min(desiredHeight, availableHeight);
    }

    private void ensureValidRosterSelection() {
        if (roster.length == 0) {
            roster = rosterFromEntries(IffFieldTeamProfile.defaultFieldEntries());
            saveTeamRoster();
        }
        if (playerIndexForId(localDevicePlayerId) < 0) {
            localDevicePlayerId = roster[LOCAL_PLAYER_INDEX].playerId;
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    .edit()
                    .putString(PREF_LOCAL_DEVICE_PLAYER_ID, localDevicePlayerId)
                    .apply();
        }
        if (selectedPlayerIndex < 0 || selectedPlayerIndex >= roster.length) {
            selectedPlayerIndex = localDevicePlayerIndex();
        }
    }

    private void loadLocalDeviceIdentity() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String fallbackPlayerId = roster.length == 0 ? "local-you" : roster[LOCAL_PLAYER_INDEX].playerId;
        String saved = prefs.getString(PREF_LOCAL_DEVICE_PLAYER_ID, fallbackPlayerId);
        localDevicePlayerId = playerIndexForId(saved) >= 0 ? saved : fallbackPlayerId;
    }

    private void loadTeamRoster() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        List<IffTeamRosterStore.Entry> entries =
                IffTeamRosterStore.deserializeTeam(prefs.getString(PREF_TEAM_ROSTER, ""));
        roster = rosterFromEntries(IffFieldTeamProfile.fieldEntriesPreservingNames(entries));
        removedPlayerIds.clear();
    }

    private void saveTeamRoster() {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putString(PREF_TEAM_ROSTER, IffTeamRosterStore.serializeTeam(teamEntries()))
                .putString(PREF_TEAM_REMOVED_PLAYERS, IffTeamRosterStore.serializeRemoved(removedPlayerIds))
                .apply();
    }

    private List<IffTeamRosterStore.Entry> teamEntries() {
        List<IffTeamRosterStore.Entry> entries = new ArrayList<IffTeamRosterStore.Entry>();
        for (int i = 0; i < roster.length; i++) {
            entries.add(new IffTeamRosterStore.Entry(roster[i].playerId, roster[i].displayName));
        }
        return entries;
    }

    private IffPlayer[] rosterFromEntries(List<IffTeamRosterStore.Entry> entries) {
        List<IffTeamRosterStore.Entry> safeEntries = entries == null || entries.isEmpty()
                ? IffTeamRosterStore.defaultEntries()
                : entries;
        IffPlayer[] players = new IffPlayer[safeEntries.size()];
        for (int i = 0; i < safeEntries.size(); i++) {
            IffTeamRosterStore.Entry entry = safeEntries.get(i);
            players[i] = new IffPlayer(entry.playerId, entry.displayName, "local-you".equals(entry.playerId));
        }
        return players;
    }

    private void loadPlayerDisplayNames() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        for (int i = 0; i < roster.length; i++) {
            IffPlayer player = roster[i];
            String saved = prefs.getString(displayNamePreferenceKey(player), player.displayName);
            String normalized = normalizeDisplayName(saved, player.displayName);
            player.displayName = normalized;
        }
        saveTeamRoster();
    }

    private void showRenameDialog(final int playerIndex) {
        if (playerIndex < 0 || playerIndex >= roster.length) {
            return;
        }
        final IffPlayer player = roster[playerIndex];
        final EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        input.setText(player.displayName);
        input.setSelectAllOnFocus(true);
        new AlertDialog.Builder(this)
                .setTitle("Имя телефона")
                .setView(input)
                .setPositiveButton("СОХРАНИТЬ", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setPlayerDisplayName(playerIndex, input.getText() == null ? "" : input.getText().toString());
                    }
                })
                .setNegativeButton("ОТМЕНА", null)
                .show();
    }

    private void setPlayerDisplayName(int playerIndex, String displayName) {
        if (playerIndex < 0 || playerIndex >= roster.length) {
            return;
        }
        IffPlayer player = roster[playerIndex];
        player.displayName = normalizeDisplayName(displayName, player.playerId);
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putString(displayNamePreferenceKey(player), player.displayName)
                .apply();
        saveTeamRoster();
        if (isLocalDevice(player)) {
            ensureFieldRadioService();
        }
        FieldDiagnosticLog.event("IFF_DIAG", "event=device_display_name_set"
                + " playerId=" + player.playerId
                + " displayName=\"" + safe(player.displayName) + "\""
                + " localDevicePlayerId=" + localDevicePlayerId);
        render();
    }

    private String displayNamePreferenceKey(IffPlayer player) {
        return PREF_PLAYER_DISPLAY_NAME_PREFIX + player.playerId;
    }

    private String normalizeDisplayName(String value, String fallback) {
        return IffTeamRosterStore.normalizeDisplayName(value, fallback);
    }

    private void setLocalDevicePlayer(int playerIndex) {
        if (playerIndex < 0 || playerIndex >= roster.length) {
            return;
        }
        IffPlayer player = roster[playerIndex];
        localDevicePlayerId = player.playerId;
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putString(PREF_LOCAL_DEVICE_PLAYER_ID, localDevicePlayerId)
                .apply();
        selectedPlayerIndex = playerIndex;
        activeTab = TAB_CONTACT;
        approachActive = false;
        IffForegroundRadioService.clearApproach();
        handler.removeCallbacks(expireApproach);
        lastFieldCheckSummary = player.displayName + ": выбран как этот телефон";
        FieldDiagnosticLog.event("IFF_DIAG", "event=device_identity_selected"
                + " localDevicePlayerId=" + player.playerId
                + " officeRole=" + officeTestRole(player)
                + " displayName=\"" + safe(player.displayName) + "\"");
        ensureFieldRadioService();
        render();
    }

    private void selectFieldRole(String playerId) {
        String normalizedPlayerId = IffTeamRosterStore.normalizePlayerId(playerId);
        if (!IffFieldTeamProfile.isFieldPlayerId(normalizedPlayerId)) {
            lastFieldCheckSummary = "роль отклонена: " + safe(playerId);
            render();
            return;
        }
        applyFieldRosterPreservingNames();
        localDevicePlayerId = normalizedPlayerId;
        setFieldRadioEnabled(true);
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putString(PREF_LOCAL_DEVICE_PLAYER_ID, localDevicePlayerId)
                .apply();
        selectedPlayerIndex = Math.max(0, playerIndexForId(localDevicePlayerId));
        activeTab = TAB_TEAM;
        teamSearchActive = false;
        approachActive = false;
        IffForegroundRadioService.clearApproach();
        handler.removeCallbacks(expireApproach);
        IffForegroundRadioService.start(this, localDevicePlayerId, localDevicePlayer().displayName);
        lastFieldCheckSummary = fieldRoleLabel(localDevicePlayerId)
                + " выбрана для " + localDevicePlayer().displayName;
        FieldDiagnosticLog.event("IFF_DIAG", "event=field_role_selected"
                + " localDevicePlayerId=" + safe(localDevicePlayerId)
                + " role=" + safe(fieldRoleLabel(localDevicePlayerId))
                + " displayName=\"" + safe(localDevicePlayer().displayName) + "\""
                + " teamSize=" + roster.length);
        render();
    }

    private void resetFieldTeam() {
        applyFieldRosterPreservingNames();
        if (!IffFieldTeamProfile.isFieldPlayerId(localDevicePlayerId)) {
            localDevicePlayerId = roster[LOCAL_PLAYER_INDEX].playerId;
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    .edit()
                    .putString(PREF_LOCAL_DEVICE_PLAYER_ID, localDevicePlayerId)
                    .apply();
        }
        selectedPlayerIndex = Math.max(0, playerIndexForId(localDevicePlayerId));
        activeTab = TAB_TEAM;
        teamSearchActive = false;
        lastFieldCheckSummary = "команда сброшена до A/B/C";
        FieldDiagnosticLog.event("IFF_DIAG", "event=field_team_reset"
                + " localDevicePlayerId=" + safe(localDevicePlayerId)
                + " teamSize=" + roster.length);
        ensureFieldRadioService();
        render();
    }

    private void applyFieldRosterPreservingNames() {
        roster = rosterFromEntries(IffFieldTeamProfile.fieldEntriesPreservingNames(teamEntries()));
        removedPlayerIds.clear();
        saveTeamRoster();
    }

    private void addTeamMember(String playerId, String displayName) {
        if (!IffFieldTeamProfile.isFieldPlayerId(playerId)) {
            lastFieldCheckSummary = "добавление отклонено: роль не A/B/C: " + safe(playerId);
            render();
            return;
        }
        List<IffTeamRosterStore.Entry> entries = teamEntries();
        if (!IffTeamRosterStore.addOrRestore(entries, removedPlayerIds, playerId, displayName)) {
            lastFieldCheckSummary = "добавление отклонено: " + safe(playerId);
            render();
            return;
        }
        roster = rosterFromEntries(entries);
        saveTeamRoster();
        selectedPlayerIndex = Math.max(0, playerIndexForId(playerId));
        activeTab = TAB_CONTACT;
        teamSearchActive = false;
        lastFieldCheckSummary = safe(displayName) + ": добавлен в команду";
        FieldDiagnosticLog.event("IFF_DIAG", "event=team_member_added"
                + " playerId=" + safe(playerId)
                + " displayName=\"" + safe(displayName) + "\""
                + " teamSize=" + roster.length);
        render();
    }

    private void removeTeamMember(final int playerIndex) {
        if (playerIndex < 0 || playerIndex >= roster.length) {
            return;
        }
        final IffPlayer player = roster[playerIndex];
        if (isLocalDevice(player)) {
            lastFieldCheckSummary = player.displayName + ": нельзя убрать этот телефон";
            render();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Убрать из команды")
                .setMessage(player.displayName + " исчезнет, пока поиск не добавит этот телефон снова.")
                .setPositiveButton("УБРАТЬ", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        removeTeamMemberConfirmed(player);
                    }
                })
                .setNegativeButton("ОТМЕНА", null)
                .show();
    }

    private void removeTeamMemberConfirmed(IffPlayer player) {
        List<IffTeamRosterStore.Entry> entries = teamEntries();
        if (!IffTeamRosterStore.remove(entries, removedPlayerIds, player.playerId, localDevicePlayerId)) {
            lastFieldCheckSummary = player.displayName + ": удаление отклонено";
            render();
            return;
        }
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .remove(trustPreferenceKey(player))
                .remove(displayNamePreferenceKey(player))
                .apply();
        roster = rosterFromEntries(entries);
        saveTeamRoster();
        selectedPlayerIndex = Math.max(0, localDevicePlayerIndex());
        activeTab = TAB_TEAM;
        lastFieldCheckSummary = player.displayName + ": удален из команды";
        FieldDiagnosticLog.event("IFF_DIAG", "event=team_member_removed"
                + " playerId=" + safe(player.playerId)
                + " displayName=\"" + safe(player.displayName) + "\""
                + " teamSize=" + roster.length);
        render();
    }

    private void loadFieldRadioPreference() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        fieldRadioEnabled = prefs.getBoolean(PREF_FIELD_RADIO_ENABLED, true);
    }

    private void loadMapScalePreference() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        mapScaleRangeMeters = IffMapScale.normalizeRangeMeters(
                prefs.getInt(PREF_MAP_SCALE_RANGE_METERS, IffMapScale.defaultRangeMeters()));
    }

    private void setMapScaleRangeMeters(int rangeMeters) {
        mapScaleRangeMeters = IffMapScale.normalizeRangeMeters(rangeMeters);
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putInt(PREF_MAP_SCALE_RANGE_METERS, mapScaleRangeMeters)
                .apply();
        render();
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
            IffForegroundRadioService.start(this, localDevicePlayerId, localDevicePlayer().displayName);
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
            IffForegroundRadioService.start(this, localDevicePlayerId, localDevicePlayer().displayName);
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
            return "ЭТОТ ТЕЛЕФОН";
        }
        return hasLocalTrust(player) ? "ДОВЕРЕН ЛОКАЛЬНО" : "НЕТ ДОВЕРИЯ";
    }

    private String trustRosterBadge(IffPlayer player) {
        if (isLocalDevice(player)) {
            return "ЭТОТ";
        }
        return hasLocalTrust(player) ? "ДОВЕРИЕ" : "НЕДОВЕРИЕ";
    }

    private void renderTrustButton(IffPlayer selected) {
        if (isLocalDevice(selected)) {
            trustButton.setText("ЭТОТ");
            trustButton.setEnabled(false);
            trustButton.setTextColor(0xffb8c49a);
        } else if (hasLocalTrust(selected)) {
            trustButton.setText("СНЯТЬ ДОВ.");
            trustButton.setEnabled(true);
            trustButton.setTextColor(0xff7dff73);
        } else {
            trustButton.setText("ДОВЕРЯТЬ");
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
            return "НЕ НАЗНАЧЕН";
        }
        if ("vasya".equals(player.playerId)) {
            return "ТЕЛЕФОН A / СВИДЕТЕЛЬ";
        }
        if ("zhenya".equals(player.playerId)) {
            return "ТЕЛЕФОН B / СВИДЕТЕЛЬ";
        }
        if ("petya".equals(player.playerId)) {
            return "ТЕЛЕФОН C / ЦЕЛЬ";
        }
        if ("local-you".equals(player.playerId)) {
            return "ТЕЛЕФОН ОПЕРАТОРА";
        }
        return "НЕ НАЗНАЧЕН";
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

    private String fieldRoleLabel(String playerId) {
        IffFieldTeamProfile.Role role = IffFieldTeamProfile.roleForPlayerId(playerId);
        return role == null ? "ДРУГАЯ" : "РОЛЬ " + role.label;
    }

    private String radioEnabledLabel() {
        return fieldRadioEnabled ? "ВКЛ" : "ВЫКЛ";
    }

    private Button createLocalNameButton() {
        Button button = new Button(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(44));
        params.setMargins(0, 0, 0, dp(4));
        button.setLayoutParams(params);
        button.setBackgroundResource(R.drawable.popup_button);
        button.setTextColor(0xff7dff73);
        button.setText("ИМЯ: " + localDevicePlayer().displayName + "  [ИЗМЕНИТЬ]");
        button.setTextSize(12);
        button.setTransformationMethod(null);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRenameDialog(localDevicePlayerIndex());
            }
        });
        return button;
    }

    private TextView createFieldSetupSummary() {
        TextView view = new TextView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(6), 0, dp(4));
        view.setLayoutParams(params);
        view.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        view.setTextColor(0xffffd16a);
        view.setTextSize(12);
        view.setText("НАСТРОЙКА КОМАНДЫ\n"
                + "Роль этого телефона: " + fieldRoleLabel(localDevicePlayerId)
                + " / имя: " + localDevicePlayer().displayName + "\n"
                + "На каждом телефоне выберите A/B/C. В команде не больше 3 телефонов.");
        return view;
    }

    private LinearLayout createFieldRoleControls() {
        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(44));
        params.setMargins(0, dp(4), 0, dp(4));
        controls.setLayoutParams(params);

        List<IffFieldTeamProfile.Role> roles = IffFieldTeamProfile.roles();
        for (int i = 0; i < roles.size(); i++) {
            controls.addView(createFieldRoleButton(roles.get(i)));
        }
        return controls;
    }

    private Button createFieldRoleButton(final IffFieldTeamProfile.Role role) {
        Button button = new Button(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1.0f);
        params.setMargins(dp(2), 0, dp(2), 0);
        button.setLayoutParams(params);
        button.setBackgroundResource(R.drawable.popup_button);
        button.setTextColor(role.playerId.equals(localDevicePlayerId) ? 0xff7dff73 : 0xffffffff);
        button.setText(role.label);
        button.setTextSize(16);
        button.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        button.setTransformationMethod(null);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectFieldRole(role.playerId);
            }
        });
        return button;
    }

    private Button createResetFieldTeamButton() {
        Button button = new Button(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(44));
        params.setMargins(0, dp(4), 0, dp(6));
        button.setLayoutParams(params);
        button.setBackgroundResource(R.drawable.popup_button);
        button.setTextColor(0xffb8c49a);
        button.setText("СБРОСИТЬ КОМАНДУ A/B/C");
        button.setTextSize(12);
        button.setTransformationMethod(null);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetFieldTeam();
            }
        });
        return button;
    }

    private Button createTeamSearchButton() {
        Button button = new Button(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(44));
        params.setMargins(0, dp(4), 0, dp(4));
        button.setLayoutParams(params);
        button.setBackgroundResource(R.drawable.popup_button);
        button.setTextColor(teamSearchActive ? 0xffffd16a : 0xffb8c49a);
        button.setText(teamSearchActive ? "ПОИСК: ВКЛ" : "ПОИСК");
        button.setTextSize(12);
        button.setTransformationMethod(null);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                teamSearchActive = !teamSearchActive;
                render();
            }
        });
        return button;
    }

    private Button createDiscoveredPlayerButton(final IffPlayer player) {
        Button button = new Button(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(52));
        params.setMargins(0, dp(4), 0, 0);
        button.setLayoutParams(params);
        button.setBackgroundResource(R.drawable.popup_button);
        button.setTextColor(0xff7dff73);
        button.setText(player.displayName + "  [ДОБАВИТЬ]\n"
                + "id=" + player.playerId + " / " + discoveryLine(player.playerId));
        button.setTextSize(12);
        button.setTransformationMethod(null);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addTeamMember(player.playerId, player.displayName);
            }
        });
        return button;
    }

    private Button createRemoveTeamMemberButton(final IffPlayer player) {
        Button button = new Button(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(44));
        params.setMargins(0, dp(8), 0, dp(4));
        button.setLayoutParams(params);
        button.setBackgroundResource(R.drawable.popup_button);
        button.setTextColor(0xffff7a7a);
        button.setText("УБРАТЬ ИЗ КОМАНДЫ");
        button.setTextSize(12);
        button.setTransformationMethod(null);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeTeamMember(playerIndexForId(player.playerId));
            }
        });
        return button;
    }

    private Button createRosterButton(final int playerIndex) {
        IffPlayer player = roster[playerIndex];
        String displayName = displayNameFor(player);
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
        button.setText(displayName + (isLocalDevice(player) ? "  [ЭТОТ ТЕЛЕФОН]" : "")
                + (!isLocalDevice(player) && hasLocalTrust(player) ? "  [ДОВЕРЕН]" : "")
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
                if (isLocalDevice(roster[playerIndex])) {
                    showRenameDialog(playerIndex);
                } else {
                    setLocalDevicePlayer(playerIndex);
                }
                return true;
            }
        });
        return button;
    }

    private LinearLayout createMapScaleControls() {
        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams controlsParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(44));
        controlsParams.setMargins(0, dp(8), 0, 0);
        controls.setLayoutParams(controlsParams);

        Button zoomOut = createMapScaleButton("-");
        zoomOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setMapScaleRangeMeters(IffMapScale.zoomOutRangeMeters(mapScaleRangeMeters));
            }
        });

        TextView scaleLabel = new TextView(this);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1.4f);
        labelParams.setMargins(dp(6), 0, dp(6), 0);
        scaleLabel.setLayoutParams(labelParams);
        scaleLabel.setGravity(Gravity.CENTER);
        scaleLabel.setBackgroundColor(0xff182015);
        scaleLabel.setTextColor(0xffffd16a);
        scaleLabel.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        scaleLabel.setTextSize(16);
        scaleLabel.setText(IffMapScale.label(mapScaleRangeMeters));

        Button zoomIn = createMapScaleButton("+");
        zoomIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setMapScaleRangeMeters(IffMapScale.zoomInRangeMeters(mapScaleRangeMeters));
            }
        });

        controls.addView(zoomOut);
        controls.addView(scaleLabel);
        controls.addView(zoomIn);
        return controls;
    }

    private Button createMapScaleButton(String text) {
        Button button = new Button(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1.0f);
        button.setLayoutParams(params);
        button.setBackgroundResource(R.drawable.popup_button);
        button.setTextColor(0xffffffff);
        button.setText(text);
        button.setTextSize(18);
        button.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        button.setTransformationMethod(null);
        return button;
    }

    private String rosterRadioLabel(IffPlayer player, WitnessSnapshot witness) {
        if (isLocalDevice(player) && approachActive) {
            return "только локально";
        }
        if (witness == null) {
            return "нет данных";
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
                    "локальная кнопка сообщает намерение игрока, но не радиодоказательство");
        }
        if (quorum.hasMultiWitness()) {
            return new CombatSnapshot("CURRENT_MULTI", "TRACK_CURRENT_CONTACT",
                    "несколько свежих свидетелей; криптоподтверждение личности все еще отдельно");
        }
        if (quorum.freshSources > 0) {
            return new CombatSnapshot("CURRENT_SINGLE", "WATCH_CURRENT_CONTACT",
                    "есть свежий радиосвидетель, но только один источник");
        }
        if (quorum.staleSources > 0 || "STALE_RADIO".equals(confidence.proximity.label)) {
            return new CombatSnapshot("STALE", "DO_NOT_TREAT_AS_NEAR",
                    "есть только старое радиосвидетельство; это не текущее доказательство близости");
        }
        return new CombatSnapshot("UNKNOWN", "NO_CURRENT_CONTACT",
                "нет текущего радиосвидетеля; участник не считается обнаруженным рядом");
    }

    private String combatDetails(CombatSnapshot combat) {
        return "- состояние: " + combatStateDisplay(combat) + "\n"
                + "- действие: " + combatActionDisplay(combat) + "\n"
                + "- причина: " + combat.reason;
    }

    private String confidenceDetails(Snapshot confidence) {
        return ruStatus(confidence.identity.detailLine("личность")) + "\n"
                + ruStatus(confidence.proximity.detailLine("близость")) + "\n"
                + ruStatus(confidence.position.detailLine("позиция")) + "\n"
                + ruStatus(confidence.direction.detailLine("направление"));
    }

    private String decisionText(Snapshot confidence, IffWitnessQuorum.Snapshot quorum) {
        if (quorum.hasMultiWitness()) {
            return "- есть свежие свидетельства от нескольких телефонов: " + quorum.freshSources + "/" + quorum.possibleSources + "\n"
                    + "- это отдельный слой свидетелей, а не криптоподтверждение личности\n"
                    + "- слои уверенности остаются как показано выше\n"
                    + "- направление и точная позиция пока неизвестны";
        }
        if (quorum.freshSources > 0) {
            return "- есть свежее свидетельство от одного телефона\n"
                    + "- одного источника мало для кворума\n"
                    + "- доверие к личности не повышается без криптоподписи\n"
                    + "- направление и точная позиция пока неизвестны";
        }
        if (quorum.staleSources > 0) {
            return "- есть только старые свидетельства\n"
                    + "- это память о старом сигнале, а не текущее доказательство\n"
                    + "- для боевого решения держим контакт в состоянии нет данных\n"
                    + "- направление и точная позиция пока неизвестны";
        }
        if ("RADIO_NEAR".equals(confidence.proximity.label)) {
            return "- рядом слышен свежий маяк заявленного участника\n"
                    + "- это сильная подсказка близости, но не криптоподтверждение личности\n"
                    + "- кворум: " + ruStatus(quorum.compact()) + ", нескольких свидетелей еще нет\n"
                    + "- направление и точная позиция пока неизвестны";
        }
        if ("RADIO_WEAK_HINT".equals(confidence.proximity.label)
                || "RADIO_EDGE_HINT".equals(confidence.proximity.label)) {
            return "- маяк слышен свежо, но RSSI не дает точную дистанцию\n"
                    + "- это слабая подсказка близости, а не подтверждение близкого контакта\n"
                    + "- кворум: " + ruStatus(quorum.compact()) + ", нескольких свидетелей еще нет\n"
                    + "- направление и точная позиция пока неизвестны";
        }
        if ("LOCAL_DECLARED_UNKNOWN".equals(confidence.proximity.label)) {
            return "- локальный игрок заявил подход\n"
                    + "- это полезный статус на экране, но не радиодоказательство\n"
                    + "- направление и точная позиция пока неизвестны";
        }
        if (confidence.proximity.score > 0) {
            return "- есть слабое или устаревшее радиосвидетельство\n"
                    + "- для боевого решения считаем близость осторожной\n"
                    + "- направление и точная позиция пока неизвестны";
        }
        return "- участник остается известен только по локальному составу\n"
                + "- близость не подтверждена\n"
                + "- направление и точная позиция пока неизвестны";
    }

    private String operatorDetails(IffPlayer selected, Snapshot confidence, IffWitnessQuorum.Snapshot quorum,
                                   CombatSnapshot combat) {
        return "- итог: " + operatorVerdictDisplay(confidence, quorum) + "\n"
                + "- боевой статус: " + combatStateDisplay(combat) + " / " + combatActionDisplay(combat) + "\n"
                + "- доверие: " + trustLabel(selected) + "\n"
                + "- свежие свидетели: " + quorum.freshSources + "/" + quorum.possibleSources + "\n"
                + "- старые свидетельства: " + quorum.staleSources + "\n"
                + "- удаленные свежие/старые/всего: " + quorum.remoteFreshSources + "/"
                + quorum.remoteStaleSources + "/" + quorum.remoteReportCount + "\n"
                + "- полевая рация: " + ruStatus(IffBleFieldRadio.compactStatus()) + "\n"
                + "- транспорт: " + ruStatus(IffUdpWitnessTransport.compactStatus()) + "\n"
                + "- личность остается: " + confidence.identity.label + " " + confidence.identity.score + "%\n"
                + "- позиция/направление: неизвестны, пока их не подтвердят отдельные слои";
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

    private String operatorVerdictDisplay(Snapshot confidence, IffWitnessQuorum.Snapshot quorum) {
        if (quorum.hasMultiWitness()) {
            return "свежий контакт подтвержден несколькими телефонами";
        }
        if (quorum.freshSources > 0) {
            return "свежий контакт от одного источника";
        }
        if (quorum.staleSources > 0) {
            return "только старые свидетельства";
        }
        if ("LOCAL_DECLARED_UNKNOWN".equals(confidence.proximity.label)) {
            return "заявлено локально, без радио";
        }
        return "нет свежих данных";
    }

    private String combatStateDisplay(CombatSnapshot combat) {
        if (combat == null) {
            return "нет данных";
        }
        if ("LOCAL_DECLARED".equals(combat.state)) {
            return "локально заявлено";
        }
        if ("CURRENT_MULTI".equals(combat.state)) {
            return "свежий контакт, несколько свидетелей";
        }
        if ("CURRENT_SINGLE".equals(combat.state)) {
            return "свежий контакт, один свидетель";
        }
        if ("STALE".equals(combat.state)) {
            return "старые данные";
        }
        return "нет данных";
    }

    private String combatActionDisplay(CombatSnapshot combat) {
        if (combat == null) {
            return "ничего не делать";
        }
        if ("LOCAL_STATUS_ONLY".equals(combat.action)) {
            return "только локальный статус";
        }
        if ("TRACK_CURRENT_CONTACT".equals(combat.action)) {
            return "отслеживать свежий контакт";
        }
        if ("WATCH_CURRENT_CONTACT".equals(combat.action)) {
            return "наблюдать свежий контакт";
        }
        if ("DO_NOT_TREAT_AS_NEAR".equals(combat.action)) {
            return "не считать рядом";
        }
        return "нет свежего контакта";
    }

    private String operatorRosterLine(IffPlayer player, Snapshot confidence, IffWitnessQuorum.Snapshot quorum,
                                      WitnessSnapshot witness, CombatSnapshot combat) {
        return combatStateDisplay(combat) + " / " + operatorVerdictDisplay(confidence, quorum) + " / личн. " + confidence.identity.score
                + "% / близ. " + confidence.proximity.score + "% / " + trustRosterBadge(player)
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
            return "- нет свежего или старого радиосвидетеля\n"
                    + "- старый Wi-Fi ищет SSID формата " + IffRadioWitnessStore.SSID_PREFIX + "*\n"
                    + "- полевая BLE-рация: " + ruStatus(IffBleFieldRadio.compactStatus()) + "\n"
                    + "- правило свежести: " + ruStatus(IffRadioWitnessStore.freshnessPolicyLabel());
        }
        return "- ssid: " + witness.ssid + "\n"
                + "- bssid: " + witness.bssid + "\n"
                + "- источник: " + ruStatus(witness.sourceType()) + "\n"
                + "- свежесть: " + ruStatus(witness.freshnessLabel()) + "\n"
                + "- правило: " + ruStatus(IffRadioWitnessStore.freshnessPolicyLabel()) + "\n"
                + "- следующее изменение: " + ruStatus(witness.nextTransitionLabel()) + "\n"
                + "- возраст: " + formatAge(witness.ageMs()) + "\n"
                + "- rssi: " + witness.rssi + " dBm\n"
                + "- частота: " + witness.frequency + " MHz";
    }

    private String fieldReadinessDetails() {
        IffParticipantMapModel.Snapshot map = participantMapSnapshot();
        return "- роль этого телефона: " + fieldRoleLabel(localDevicePlayerId) + "\n"
                + "- имя этого телефона: " + localDevicePlayer().displayName + "\n"
                + "- полевая рация включена: " + (fieldRadioEnabled ? "да" : "нет") + "\n"
                + "- сервис рации: " + ruStatus(IffForegroundRadioService.compactStatus()) + "\n"
                + "- BLE объявление/поиск: " + ruStatus(IffBleFieldRadio.compactStatus()) + "\n"
                + "- состояние BLE: " + ruStatus(IffBleFieldRadio.lifecycleStatus()) + "\n"
                + "- состав команды: " + fieldRosterLine() + "\n"
                + "- видны на карте: " + participantMapVisibleLine(map) + "\n"
                + "- готовность карты: " + ruStatus(participantMapSummary(map)) + "\n"
                + "- почему карта скрывает точки: " + ruStatus(mapHiddenReason(map)) + "\n"
                + "- gps: " + ruStatus(gpsUiStatus());
    }

    private String fieldRosterLine() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < roster.length; i++) {
            IffPlayer player = roster[i];
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(fieldRoleLabel(player.playerId))
                    .append("=")
                    .append(displayNameFor(player));
            if (isLocalDevice(player)) {
                builder.append(" [ЭТОТ]");
            }
        }
        return builder.toString();
    }

    private String participantMapVisibleLine(IffParticipantMapModel.Snapshot snapshot) {
        if (snapshot == null || snapshot.points == null || snapshot.points.size() == 0) {
            return "нет";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < snapshot.points.size(); i++) {
            IffParticipantMapModel.Point point = snapshot.points.get(i);
            if (point == null) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(safe(point.displayName))
                    .append(" ")
                    .append(point.distanceM)
                    .append("м");
        }
        return builder.length() == 0 ? "нет" : builder.toString();
    }

    private String mapHiddenReason(IffParticipantMapModel.Snapshot snapshot) {
        if (snapshot == null) {
            return "снимок карты участников недоступен";
        }
        if (snapshot.points != null && snapshot.points.size() > 0) {
            return "видно";
        }
        String reason = safe(snapshot.reason);
        return reason.length() == 0 ? "нет видимых GPS-точек на карте" : reason;
    }

    private String fieldRadioPolicyDetails() {
        return "- состояние: " + ruStatus(IffBleFieldRadio.lifecycleStatus()) + "\n"
                + "- ручное управление: " + radioEnabledLabel() + "\n"
                + "- сервис: " + ruStatus(IffForegroundRadioService.compactStatus()) + "\n"
                + "- фоновый сервис держит BLE-рацию, даже если экран IFF закрыт\n"
                + "- кнопка остановки в уведомлении выключает BLE-поиск/объявление и пишет остановку в журнал\n"
                + "- старые BLE/Wi-Fi свидетельства остаются видимыми, но не считаются текущим доказательством\n"
                + "- просроченный свидетель возвращает близость в состояние неизвестно";
    }

    private String witnessQuorumDetails(IffPlayer selected, IffWitnessQuorum.Snapshot quorum) {
        StringBuilder builder = new StringBuilder();
        builder.append("- цель: ").append(displayNameFor(selected)).append("\n")
                .append("- состояние: ").append(ruStatus(quorum.compact())).append("\n")
                .append("- этот телефон: ");
        if (quorum.localWitness == null) {
            builder.append("НЕТ ОТЧЕТА\n");
        } else {
            builder.append(ruStatus(quorum.localWitness.freshnessLabel()))
                    .append(" ")
                    .append(quorum.localWitness.rssi)
                    .append("dBm возраст=")
                    .append(formatAge(quorum.localWitness.ageMs()))
                    .append("\n");
        }
        builder.append("- контракт удаленных отчетов: ").append(IffRemoteWitnessReport.CONTRACT_VERSION).append("\n")
                .append("- удаленные отчеты: ");
        if (quorum.remoteReports.size() == 0) {
            builder.append("не получены\n");
        } else {
            builder.append(quorum.remoteReportCount).append(" получено\n");
            for (int i = 0; i < quorum.remoteReports.size(); i++) {
                IffRemoteWitnessReport report = quorum.remoteReports.get(i);
                builder.append("  ")
                        .append(report.sourcePlayerId)
                        .append(" -> ")
                        .append(ruStatus(report.freshnessLabel()))
                        .append(" ")
                        .append(report.rssi)
                        .append("dBm возраст=")
                        .append(formatAge(report.ageMs()))
                        .append(" подпись=")
                        .append(ruStatus(report.signatureStatus))
                        .append("\n");
            }
        }
        builder.append("- транспорт: диагностический канал UDP\n")
                .append("- подпись: ").append(IffRemoteWitnessReport.SIGNATURE_PENDING).append("\n")
                .append("- кворум без криптоподписи не повышает доверие к личности");
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
            return "НЕСКОЛЬКО СВЕЖИХ";
        }
        if (current > 0) {
            return "ОДИН СВЕЖИЙ";
        }
        if (stale > 0) {
            return "ТОЛЬКО СТАРЫЕ";
        }
        return "НЕТ СВЕЖИХ";
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
            return "GPS НЕДОСТУПЕН";
        }
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            return "GPS НЕДОСТУПЕН";
        }
        Location best = bestLastKnownLocation(locationManager);
        if (best == null) {
            return "GPS НЕДОСТУПЕН";
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
            return "нет данных";
        }
        return window.freshnessLabel()
                + " средн.=" + window.averageRssi + "dBm"
                + " n=" + window.validCount
                + " out127=" + window.outlier127Count
                + " новейш.=" + formatAge(window.newestAgeMs);
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
            builder.append(displayNameFor(player))
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

    private String displayNameFor(IffPlayer player) {
        if (player == null) {
            return "телефон";
        }
        if (isLocalDevice(player)) {
            return player.displayName;
        }
        String learned = IffForegroundRadioService.participantDisplayNameFor(player.playerId);
        if (learned != null && learned.length() > 0 && !learned.equals(player.playerId)) {
            return learned;
        }
        IffParticipantState state = participantStateById(player.playerId);
        if (state != null && state.displayName != null && state.displayName.length() > 0
                && !state.displayName.equals(player.playerId)) {
            return state.displayName;
        }
        return player.displayName;
    }

    private List<IffPlayer> discoveredTeamCandidates() {
        LinkedHashMap<String, IffPlayer> candidates = new LinkedHashMap<String, IffPlayer>();
        long now = SystemClock.elapsedRealtime();
        List<IffParticipantState> states = IffForegroundRadioService.participantStatesSnapshot();
        for (int i = 0; i < states.size(); i++) {
            IffParticipantState state = states.get(i);
            if (state == null || Math.max(0L, now - state.receivedTimeMillis) > 120000L) {
                continue;
            }
            addDiscoveryCandidate(candidates, state.playerId, state.displayName);
        }
        List<WitnessSnapshot> witnesses = IffRadioWitnessStore.snapshot();
        for (int i = 0; i < witnesses.size(); i++) {
            WitnessSnapshot witness = witnesses.get(i);
            if (witness == null || witness.ageMs() > IffRadioWitnessStore.STALE_MS) {
                continue;
            }
            addDiscoveryCandidate(candidates, witness.playerId, IffTeamRosterStore.defaultDisplayNameFor(witness.playerId));
        }
        return new ArrayList<IffPlayer>(candidates.values());
    }

    private void addDiscoveryCandidate(Map<String, IffPlayer> candidates, String playerId, String displayName) {
        String normalizedPlayerId = IffTeamRosterStore.normalizePlayerId(playerId);
        if (normalizedPlayerId == null
                || playerIndexForId(normalizedPlayerId) >= 0
                || normalizedPlayerId.equals(localDevicePlayerId)) {
            return;
        }
        if (!candidates.containsKey(normalizedPlayerId)) {
            candidates.put(normalizedPlayerId, new IffPlayer(
                    normalizedPlayerId,
                    normalizeDisplayName(displayName, normalizedPlayerId),
                    false));
        }
    }

    private String discoveryLine(String playerId) {
        WitnessSnapshot witness = IffRadioWitnessStore.getWitness(playerId);
        if (witness != null && witness.ageMs() <= IffRadioWitnessStore.STALE_MS) {
            return witness.sourceType() + " " + witness.freshnessLabel()
                    + " " + witness.rssi + "dBm " + formatAge(witness.ageMs());
        }
        IffParticipantState state = participantStateById(playerId);
        if (state != null) {
            return "GPS возраст=" + formatAge(Math.max(0L, SystemClock.elapsedRealtime() - state.receivedTimeMillis))
                    + " источник=" + safe(state.sourcePlayerId);
        }
        return "недавно слышали";
    }

    private IffParticipantState participantStateById(String playerId) {
        List<IffParticipantState> states = IffForegroundRadioService.participantStatesSnapshot();
        for (int i = 0; i < states.size(); i++) {
            IffParticipantState state = states.get(i);
            if (state != null && state.playerId.equals(playerId)) {
                return state;
            }
        }
        return null;
    }

    private String fieldMapSummary() {
        IffFieldMapSnapshot map = fieldMapSnapshot();
        IffParticipantMapModel.Snapshot participants = participantMapSnapshot();
        return "ПОЛЕВАЯ КАРТА\n"
                + "- " + ruStatus(map.statusLine) + "\n"
                + "- участники: " + ruStatus(participantMapSummary(participants)) + "\n"
                + "- старые опоры: " + ruStatus(IffWifiTargetObservationStore.compactStatus()) + "\n"
                + "- запасная оценка по рации: " + ruStatus(mapRadioDistanceTrend().compact()) + "\n\n"
                + simpleMapWitnessList();
    }

    private IffParticipantMapModel.Snapshot participantMapSnapshot() {
        IffParticipantMapModel.Snapshot snapshot =
                IffForegroundRadioService.participantMapSnapshot(localDevicePlayerId);
        return snapshot == null ? null : snapshot.filteredToPlayerIds(teamPlayerIdSet());
    }

    private Set<String> teamPlayerIdSet() {
        Set<String> ids = new LinkedHashSet<String>();
        for (int i = 0; i < roster.length; i++) {
            ids.add(roster[i].playerId);
        }
        return ids;
    }

    private String participantMapSummary(IffParticipantMapModel.Snapshot snapshot) {
        if (snapshot == null) {
            return "НЕТ видно=0 скрыто=0";
        }
        return ruStatus(snapshot.mode)
                + " видно=" + (snapshot.points == null ? 0 : snapshot.points.size())
                + " скрыто=" + snapshot.hiddenCount
                + " причина=" + ruStatus(safe(snapshot.reason));
    }

    private String participantMapDetails(IffParticipantMapModel.Snapshot snapshot) {
        if (snapshot == null) {
            return "КАРТА УЧАСТНИКОВ\n- нет";
        }
        StringBuilder builder = new StringBuilder();
        builder.append("КАРТА УЧАСТНИКОВ\n")
                .append("- ").append(ruStatus(participantMapSummary(snapshot))).append("\n");
        if (snapshot.points == null || snapshot.points.size() == 0) {
            return builder.append("- точки: нет").toString();
        }
        for (int i = 0; i < snapshot.points.size(); i++) {
            IffParticipantMapModel.Point point = snapshot.points.get(i);
            builder.append("- ")
                    .append(point.displayName)
                    .append(": ")
                    .append(point.distanceM)
                    .append("м +/-")
                    .append(Math.round(point.distanceAccuracyMeters))
                    .append("м ")
                    .append(point.ageMs <= 2500L ? "СВЕЖЕЕ" : "СТАРОЕ")
                    .append(" азимут=")
                    .append(point.bearingDeg)
                    .append("град точн.=")
                    .append(Math.round(point.accuracyMeters))
                    .append("м источник=")
                    .append(safe(point.sourcePlayerId))
                    .append(" переход=")
                    .append(point.hopCount)
                    .append(" rssi=")
                    .append(point.rssiDbm)
                    .append(" подход=")
                    .append(point.approachActive)
                    .append("\n");
        }
        return builder.toString();
    }

    private String mapWitnessList() {
        StringBuilder builder = new StringBuilder();
        builder.append("ПОЛЕВЫЕ КОНТАКТЫ\n\n");
        for (int i = 0; i < roster.length; i++) {
            IffPlayer player = roster[i];
            WitnessSnapshot witness = IffRadioWitnessStore.getWitness(player.playerId);
            builder.append(displayNameFor(player))
                    .append(": ")
                    .append(ruStatus(witnessQuorumFor(player, witness).compact()))
                    .append(" / ")
                    .append(witness == null ? "рация: нет данных" : ruStatus(witness.freshnessLabel()) + " " + witness.rssi + "dBm возраст=" + formatAge(witness.ageMs()))
                    .append("\n");
        }
        builder.append("\nGPS и направление будут отдельными слоями уверенности.\n")
                .append("Полевая BLE-рация не требует общей Wi-Fi сети.\n")
                .append("Правило свежести: ").append(ruStatus(IffRadioWitnessStore.freshnessPolicyLabel())).append("\n")
                .append("Состояние BLE: ").append(ruStatus(IffBleFieldRadio.lifecycleStatus()));
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
                    player.playerId,
                    displayNameFor(player) + (isLocalDevice(player) ? " [ЭТОТ]" : ""),
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
            return ruStatus(witness.sourceType()) + " " + ruStatus(witness.freshnessLabel()) + " "
                    + witness.rssi + "dBm " + formatAge(witness.ageMs());
        }
        if (quorum.remoteFreshSources > 0) {
            return "УДАЛЕННО СВЕЖЕЕ x" + quorum.remoteFreshSources;
        }
        if (quorum.staleSources > 0) {
            return "СТАРОЕ СВИДЕТЕЛЬСТВО";
        }
        return "НЕТ ДАННЫХ";
    }

    private String formatAge(long ageMs) {
        if (ageMs < 1000L) {
            return ageMs + "мс";
        }
        return (ageMs / 1000L) + "с";
    }

    private String ruStatus(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("UNKNOWN", "НЕТ ДАННЫХ")
                .replace("CURRENT", "СВЕЖЕЕ")
                .replace("STALE", "СТАРОЕ")
                .replace("NO_REPORT", "НЕТ ОТЧЕТА")
                .replace("NO_MAP", "НЕТ КАРТЫ")
                .replace("NO_ANCHORS", "НЕТ ОПОР")
                .replace("ONE_ANCHOR", "ОДНА ОПОРА")
                .replace("TWO_ANCHORS", "ДВЕ ОПОРЫ")
                .replace("INSUFFICIENT_DATA", "МАЛО ДАННЫХ")
                .replace("MISSING", "НЕТ")
                .replace("missing", "нет")
                .replace("fresh", "свежее")
                .replace("stale", "старое")
                .replace("old", "старое")
                .replace("visible", "видно")
                .replace("hidden", "скрыто")
                .replace("reason", "причина")
                .replace("target", "цель")
                .replace("left", "левая")
                .replace("right", "правая")
                .replace("locator", "локатор")
                .replace("source", "источник")
                .replace("service", "сервис")
                .replace("enabled", "включено")
                .replace("disabled", "выключено")
                .replace("running", "работает")
                .replace("stopped", "остановлено")
                .replace("ageMs", "возрастМс");
    }

    private String safe(String value) {
        return value == null ? "" : value.replace('\n', ' ').replace('\r', ' ');
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private static final class IffPlayer {
        final String playerId;
        String displayName;
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
