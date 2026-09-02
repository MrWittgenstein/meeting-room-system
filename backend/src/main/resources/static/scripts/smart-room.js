import * as THREE from "/vendor/three/three.module.js";
import { OrbitControls } from "/vendor/three/examples/jsm/controls/OrbitControls.js";

const roomDefinitions = [
    {
        key: "board",
        numericId: 14,
        roomCode: "meeting_room_14",
        label: "董事会议室",
        modelPage: "/assets/models/conference-room.html",
        title: "董事会议室三维展示与环境联动",
        banner:
            "当前页面模拟董事会议室的温湿度、光照与投影联动，可作为树莓派边缘控制与智能照明策略的软件系统展示界面。",
        summary:
            "偏正式的长桌布局，适合领导决策、商务会谈和高规格汇报。",
        points: ["长桌 + 12席位", "木饰面与投影幕布", "线性吊灯与墙面展示区"],
        category: "董事 / 培训 / 创意",
        hotspots: [
            { id: "projector", name: "投影", position: [0, 1.55, -1.38], mesh: "screen" },
            { id: "light", name: "灯光", position: [0.18, 2.5, -0.25], mesh: "ceilingLight" },
            { id: "temperature", name: "温湿度", position: [1.75, 1.55, -1.1], mesh: "sensorPole" },
            { id: "photoresistor", name: "光敏", position: [-2.18, 1.3, -0.8], mesh: "sensorPole" }
        ]
    },
    {
        key: "training",
        numericId: 15,
        roomCode: "meeting_room_15",
        label: "培训研讨室",
        modelPage: "/assets/models/nordic-room.html",
        title: "培训研讨室三维展示与环境联动",
        banner:
            "面向培训和研讨场景，强化投影、灯光和人员状态联动，适合演示汇报与课程管理。",
        summary:
            "面向课程演示和小型培训，强调投影清晰度、照明分区与人员分布状态。",
        points: ["分组桌面与讲台", "投影优先级更高", "适合培训、答辩、汇报"],
        category: "培训 / 演示 / 研讨",
        hotspots: [
            { id: "projector", name: "投影", position: [0.2, 1.6, -1.38], mesh: "screen" },
            { id: "light", name: "灯光", position: [-0.2, 2.5, -0.18], mesh: "ceilingLight" },
            { id: "temperature", name: "温湿度", position: [1.68, 1.6, -0.9], mesh: "sensorPole" },
            { id: "photoresistor", name: "光敏", position: [-2.12, 1.22, -0.88], mesh: "sensorPole" }
        ]
    },
    {
        key: "creative",
        numericId: 32,
        roomCode: "meeting_room_32",
        label: "创意协作室",
        modelPage: "/assets/models/sunny-meeting-room.html",
        title: "创意协作室三维展示与环境联动",
        banner:
            "面向创意协作场景，突出环境舒适度、灯光氛围和开放协作模式联动。",
        summary:
            "强调轻协作与头脑风暴，环境策略偏柔和，支持创意展示、快速讨论与演示切换。",
        points: ["开放协作桌组", "氛围照明与柔性灯光", "适合创意共创与远程协作"],
        category: "创意 / 协作 / 远程",
        hotspots: [
            { id: "projector", name: "投影", position: [0.12, 1.52, -1.34], mesh: "screen" },
            { id: "light", name: "灯光", position: [0.08, 2.44, -0.24], mesh: "ceilingLight" },
            { id: "temperature", name: "温湿度", position: [1.68, 1.6, -0.92], mesh: "sensorPole" },
            { id: "photoresistor", name: "光敏", position: [-2.1, 1.18, -0.72], mesh: "sensorPole" }
        ]
    }
];

const modeDefinitions = [
    {
        key: "daily",
        label: "日常办公",
        badge: "仿真控制",
        apply: (state) => {
            state.devices.projector = false;
            state.devices.light = true;
            state.devices.auto = true;
            state.devices.cooling = false;
            state.devices.heating = false;
            state.devices.humidifier = false;
            state.devices.dehumidifier = false;
            state.devices.lightOn = true;
            state.devices.curtainOpen = true;
            state.devices.buzzerOn = false;
            state.devices.alarm = false;
        }
    },
    {
        key: "focus",
        label: "专注讨论",
        badge: "联动优化",
        apply: (state) => {
            state.devices.projector = false;
            state.devices.light = true;
            state.devices.auto = true;
            state.devices.cooling = false;
            state.devices.heating = false;
            state.devices.humidifier = false;
            state.devices.dehumidifier = false;
            state.devices.lightOn = true;
            state.devices.curtainOpen = false;
            state.devices.buzzerOn = false;
            state.devices.alarm = false;
        }
    },
    {
        key: "presentation",
        label: "演示汇报",
        badge: "投影优先",
        apply: (state) => {
            state.devices.projector = true;
            state.devices.light = false;
            state.devices.auto = true;
            state.devices.cooling = true;
            state.devices.heating = false;
            state.devices.humidifier = false;
            state.devices.dehumidifier = false;
            state.devices.lightOn = false;
            state.devices.curtainOpen = false;
            state.devices.buzzerOn = false;
            state.devices.alarm = false;
        }
    }
];

const deviceDefinitions = [
    { key: "projector", label: "投影" },
    { key: "light", label: "灯光" },
    { key: "auto", label: "自动" }
];

const presetDefinitions = [
    { key: "overview", label: "全景" },
    { key: "front", label: "正前方" },
    { key: "top", label: "俯视" }
];

const cameraControlDefinitions = [
    { key: "turnLeft", label: "左转" },
    { key: "turnRight", label: "右转" },
    { key: "tiltUp", label: "上仰" },
    { key: "tiltDown", label: "下俯" },
    { key: "panLeft", label: "左移" },
    { key: "panRight", label: "右移" },
    { key: "panUp", label: "上移" },
    { key: "panDown", label: "下移" },
    { key: "zoomIn", label: "放大" },
    { key: "zoomOut", label: "缩小" }
];

const apiBaseUrl = (() => {
    const configured = window.SMART_ROOM_API_BASE_URL;
    if (typeof configured === "string" && configured.trim()) {
        return configured.replace(/\/$/, "");
    }

    const { protocol, hostname, port } = window.location;
    if (port === "5500") {
        return `${protocol}//${hostname}:8080`;
    }
    return "";
})();

const hotspotDetails = {
    projector: {
        title: "智能投影终端",
        subtitleOn: "显示设备 · 投影运行中",
        subtitleOff: "显示设备 · 投影待机",
        description:
            "连接会议主机与大屏幕布，用于投放会议议程、PPT 和视频会议画面，可延伸到 HDMI/无线投屏控制与树莓派显示输出实验。",
        activeBadge: "投影运行中",
        idleBadge: "投影待机",
        deviceLabel: "投影设备"
    },
    light: {
        title: "智能照明节点",
        subtitleOn: "照明设备 · 灯光已开启",
        subtitleOff: "照明设备 · 灯光已关闭",
        description:
            "根据光照强度与系统模式自动联动，支持日常办公、专注讨论与演示汇报三种照明策略。",
        activeBadge: "灯光开启",
        idleBadge: "灯光关闭",
        deviceLabel: "照明回路"
    },
    temperature: {
        title: "温湿度环境节点",
        subtitleOn: "环境感知 · 温湿度稳定",
        subtitleOff: "环境感知 · 温湿度波动",
        description:
            "采集温度和湿度信息，参与制冷、制热、加湿和除湿联动决策，为会议室维持舒适环境。",
        activeBadge: "环境稳定",
        idleBadge: "关注波动",
        deviceLabel: "温湿度传感器"
    },
    photoresistor: {
        title: "光敏感知节点",
        subtitleOn: "环境感知 · 光照正常",
        subtitleOff: "环境感知 · 光照偏暗",
        description:
            "检测室内光照强度，参与灯光与窗帘协同控制，保证投影可视性和空间明暗平衡。",
        activeBadge: "光照正常",
        idleBadge: "需要补光",
        deviceLabel: "光敏传感器"
    }
};

const appState = {
    selectedRoomKey: "board",
    selectedModeKey: "focus",
    selectedPresetKey: "overview",
    selectedHotspotId: "projector",
    dataSource: "mock",
    lastApiFetchOk: false,
    syncMessage: "当前支持后端数据轮询",
    commandPending: false,
    pendingDeviceKeys: new Set(),
    devices: {
        projector: true,
        light: true,
        auto: true,
        cooling: false,
        heating: false,
        humidifier: false,
        dehumidifier: false,
        lightOn: true,
        curtainOpen: false,
        buzzerOn: false,
        alarm: false
    },
    sensors: {
        deviceId: "raspi-01",
        temperature: 25.8,
        humidity: 46,
        light: 62,
        lightRaw: 158,
        lightLux: 532,
        smoke: null,
        smokeRaw: null,
        smokeLevel: "未部署",
        distance: null,
        personNear: false,
        presence: false,
        faceDetected: false,
        faceCount: 0,
        authorized: false,
        doorState: "closed",
        timestamp: null
    },
    logs: []
};

const dom = {
    roomSwitches: document.getElementById("room-switches"),
    modeSwitches: document.getElementById("mode-switches"),
    deviceToggles: document.getElementById("device-toggles"),
    viewPresets: document.getElementById("view-presets"),
    cameraControls: document.getElementById("camera-controls"),
    hotspotTabs: document.getElementById("hotspot-tabs"),
    hotspotLayer: document.getElementById("hotspot-layer"),
    deviceStatusList: document.getElementById("device-status-list"),
    sensorStatusList: document.getElementById("sensor-status-list"),
    logList: document.getElementById("log-list"),
    roomDetailTitle: document.getElementById("room-detail-title"),
    roomDetailSummary: document.getElementById("room-detail-summary"),
    roomDetailPoints: document.getElementById("room-detail-points"),
    modeBadge: document.getElementById("mode-badge"),
    bannerDescription: document.getElementById("banner-description"),
    viewerTitle: document.getElementById("viewer-title"),
    statRoomCount: document.getElementById("stat-room-count"),
    statDeviceCount: document.getElementById("stat-device-count"),
    statSystemStatus: document.getElementById("stat-system-status"),
    statSystemNote: document.getElementById("stat-system-note"),
    healthBadge: document.getElementById("health-badge"),
    healthTitle: document.getElementById("health-title"),
    healthCopy: document.getElementById("health-copy"),
    dataSourceBadge: document.getElementById("data-source-badge"),
    hotspotCardTitle: document.getElementById("hotspot-card-title"),
    hotspotCardBadge: document.getElementById("hotspot-card-badge"),
    hotspotCardSubtitle: document.getElementById("hotspot-card-subtitle"),
    hotspotCardDescription: document.getElementById("hotspot-card-description"),
    hotspotCardMeta: document.getElementById("hotspot-card-meta"),
    sceneShell: document.getElementById("scene-shell"),
    sceneCanvas: document.getElementById("scene-canvas"),
    sceneIframe: document.getElementById("scene-iframe"),
    syncTag: document.getElementById("sync-tag")
};

const sceneController = createSceneController(dom.sceneCanvas, dom.hotspotLayer, onHotspotSelected);

bootstrap();

async function bootstrap() {
    modeDefinitions.find((mode) => mode.key === appState.selectedModeKey)?.apply(appState);
    pushLog("页面初始化完成，已载入董事会议室联动面板。");

    renderStaticControls();
    await sceneController.init();
    updateDerivedView();
    await loadRoomStatus();
    setInterval(() => {
        loadRoomStatus().catch(() => undefined);
    }, 10000);
}

function renderStaticControls() {
    renderRoomSwitches();
    renderModeSwitches();
    renderDeviceToggles();
    renderPresets();
    renderCameraControls();
    renderHotspotTabs();
}

function renderRoomSwitches() {
    dom.roomSwitches.innerHTML = "";
    roomDefinitions.forEach((room) => {
        const button = document.createElement("button");
        button.type = "button";
        button.className = `switch-card${room.key === appState.selectedRoomKey ? " active" : ""}`;
        button.textContent = room.label;
        button.addEventListener("click", async () => {
            if (appState.selectedRoomKey === room.key) {
                return;
            }
            appState.selectedRoomKey = room.key;
            appState.selectedHotspotId = "projector";
            pushLog(`切换到${room.label}。`);
            renderRoomSwitches();
            renderHotspotTabs();
            updateDerivedView();
            sceneController.switchRoom(room);
            await loadRoomStatus();
        });
        dom.roomSwitches.appendChild(button);
    });
}

function renderModeSwitches() {
    dom.modeSwitches.innerHTML = "";
    modeDefinitions.forEach((mode) => {
        const button = document.createElement("button");
        button.type = "button";
        button.disabled = appState.commandPending;
        button.className = `switch-card${mode.key === appState.selectedModeKey ? " active" : ""}`;
        button.textContent = mode.label;
        button.addEventListener("click", async () => {
            appState.selectedModeKey = mode.key;
            mode.apply(appState);
            pushLog(`切换系统模式：${mode.label}。`);
            renderModeSwitches();
            renderDeviceToggles();
            updateDerivedView();
            sceneController.syncState(getCurrentRoom(), appState);
            await sendModeCommands(mode);
        });
        dom.modeSwitches.appendChild(button);
    });
}

function renderDeviceToggles() {
    dom.deviceToggles.innerHTML = "";
    deviceDefinitions.forEach((device) => {
        const active = Boolean(appState.devices[device.key]);
        const pending = appState.pendingDeviceKeys.has(device.key);
        const disabled = pending || appState.commandPending;
        const card = document.createElement("button");
        card.type = "button";
        card.disabled = disabled;
        card.className = `device-toggle${active ? " active" : ""}${pending ? " pending" : ""}`;
        card.innerHTML = `
            <span class="device-toggle-name">${device.label}状态</span>
            <strong class="device-toggle-value">${pending ? "执行中" : active ? "已开" : "已关"}</strong>
        `;
        card.addEventListener("click", async () => {
            if (appState.commandPending) {
                return;
            }
            const nextValue = !active;
            applyLocalDeviceState(device.key, nextValue);
            pushLog(`正在下发${device.label}${nextValue ? "开启" : "关闭"}命令。`);
            updateDerivedView();
            sceneController.syncState(getCurrentRoom(), appState);
            await sendDeviceCommand(device.key, nextValue);
        });
        dom.deviceToggles.appendChild(card);
    });
}

function setCommandPending(deviceKeys, pending) {
    deviceKeys.forEach((deviceKey) => {
        if (pending) {
            appState.pendingDeviceKeys.add(deviceKey);
        } else {
            appState.pendingDeviceKeys.delete(deviceKey);
        }
    });
    appState.commandPending = appState.pendingDeviceKeys.size > 0;
    renderModeSwitches();
    renderDeviceToggles();
}

function applyLocalDeviceState(deviceKey, enabled) {
    appState.devices[deviceKey] = enabled;
    if (deviceKey === "light") {
        appState.devices.lightOn = enabled;
    }
    if (deviceKey === "auto") {
        appState.devices.auto = enabled;
    }
    if (deviceKey === "projector") {
        appState.selectedHotspotId = "projector";
    }
}

async function sendDeviceCommand(deviceKey, enabled) {
    const targetMap = {
        projector: "projector",
        light: "light",
        auto: "auto"
    };
    const command = deviceKey === "auto" ? "set_auto_mode" : "set_device_state";
    const target = targetMap[deviceKey] || deviceKey;
    setCommandPending([deviceKey], true);
    appState.syncMessage = `正在下发${target}控制命令`;
    updateDerivedView();
    try {
        const result = await postIotCommand({
            command,
            target,
            value: enabled
        });
        pushLog(`树莓派已确认：${result.message || "命令已执行"}。`);
        await loadRoomStatus({ silent: true });
        appState.syncMessage = `${target}控制命令已确认`;
    } catch (error) {
        pushLog(`控制下发失败：${error.message}`);
        appState.syncMessage = `控制下发失败：${error.message}`;
        await loadRoomStatus({ silent: true, keepSyncMessage: true });
    } finally {
        setCommandPending([deviceKey], false);
        updateDerivedView();
        sceneController.syncState(getCurrentRoom(), appState);
    }
}

async function sendModeCommands(mode) {
    const pendingKeys = ["auto", "projector", "light"];
    setCommandPending(pendingKeys, true);
    appState.syncMessage = `正在下发${mode.label}模式`;
    updateDerivedView();
    const commands = [
        { command: "set_auto_mode", target: "auto", value: appState.devices.auto },
        { command: "set_device_state", target: "projector", value: appState.devices.projector },
        { command: "set_device_state", target: "light", value: appState.devices.lightOn },
        { command: "set_device_state", target: "curtain", value: appState.devices.curtainOpen }
    ];
    try {
        for (const command of commands) {
            await postIotCommand(command);
        }
        pushLog(`树莓派已应用${mode.label}模式。`);
        await loadRoomStatus({ silent: true });
        appState.syncMessage = `${mode.label}模式已确认`;
    } catch (error) {
        pushLog(`模式下发失败：${error.message}`);
        appState.syncMessage = `模式下发失败：${error.message}`;
        await loadRoomStatus({ silent: true, keepSyncMessage: true });
    } finally {
        setCommandPending(pendingKeys, false);
        updateDerivedView();
        sceneController.syncState(getCurrentRoom(), appState);
    }
}

async function postIotCommand(commandPayload) {
    const deviceId = appState.sensors.deviceId || "raspi-01";
    const response = await fetch(`${apiBaseUrl}/iot/devices/${encodeURIComponent(deviceId)}/commands`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify(commandPayload)
    });
    if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
    }
    const payload = await response.json();
    if (payload.code !== 1) {
        throw new Error(payload.message || "command failed");
    }
    return payload.data || {};
}

function renderPresets() {
    dom.viewPresets.innerHTML = "";
    presetDefinitions.forEach((preset) => {
        const button = document.createElement("button");
        button.type = "button";
        button.className = `preset-button${preset.key === appState.selectedPresetKey ? " active" : ""}`;
        button.textContent = preset.label;
        button.addEventListener("click", () => {
            appState.selectedPresetKey = preset.key;
            sceneController.setPreset(preset.key);
            pushLog(`切换视角预设：${preset.label}。`);
            renderPresets();
        });
        dom.viewPresets.appendChild(button);
    });
}

function renderCameraControls() {
    dom.cameraControls.innerHTML = "";
    cameraControlDefinitions.forEach((control) => {
        const button = document.createElement("button");
        button.type = "button";
        button.className = "control-button";
        button.textContent = control.label;
        button.addEventListener("click", () => {
            sceneController.applyCameraControl(control.key);
            pushLog(`执行镜头控制：${control.label}。`);
        });
        dom.cameraControls.appendChild(button);
    });
}

function renderHotspotTabs() {
    dom.hotspotTabs.innerHTML = "";
    getCurrentRoom().hotspots.forEach((hotspot) => {
        const button = document.createElement("button");
        button.type = "button";
        button.className = `hotspot-tab${hotspot.id === appState.selectedHotspotId ? " active" : ""}`;
        button.textContent = hotspot.name;
        button.addEventListener("click", () => onHotspotSelected(hotspot.id));
        dom.hotspotTabs.appendChild(button);
    });
}

function onHotspotSelected(hotspotId) {
    appState.selectedHotspotId = hotspotId;
    renderHotspotTabs();
    updateDeviceCard();
    sceneController.highlightHotspot(hotspotId);
}

async function loadRoomStatus(options = {}) {
    const { silent = false, keepSyncMessage = false } = options;
    const room = getCurrentRoom();
    try {
        const response = await fetch(`${apiBaseUrl}/iot/rooms/${room.numericId}/status`, { cache: "no-store" });
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }

        const payload = await response.json();
        if (payload.code !== 1 || !payload.data) {
            throw new Error(payload.message || "room status unavailable");
        }

        hydrateFromStatus(payload.data);
        appState.lastApiFetchOk = true;
        appState.dataSource = "live";
        if (!keepSyncMessage) {
            appState.syncMessage = "当前连接 Spring 后端 IoT 状态";
        }
        if (!silent) {
            pushLog(`已拉取${room.label}最新状态。`);
        }
    } catch (error) {
        appState.lastApiFetchOk = false;
        appState.dataSource = "mock";
        if (!keepSyncMessage) {
            appState.syncMessage = "当前使用本地仿真数据";
        }
        simulateRoomTelemetry(room);
    }

    renderModeSwitches();
    renderDeviceToggles();
    updateDerivedView();
    sceneController.syncState(room, appState);
}

function hydrateFromStatus(status) {
    const environment = status.environment || {};
    const presence = status.presence || {};
    const accessControl = status.access_control || {};
    const devices = status.devices || {};
    const system = status.system || {};

    appState.sensors.deviceId = status.device_id ?? appState.sensors.deviceId;
    appState.sensors.temperature = environment.temperature ?? appState.sensors.temperature;
    appState.sensors.humidity = environment.humidity ?? appState.sensors.humidity;
    appState.sensors.light = environment.light ?? appState.sensors.light;
    appState.sensors.lightRaw = environment.light_raw ?? appState.sensors.lightRaw;
    appState.sensors.lightLux = environment.light_lux ?? appState.sensors.lightLux;
    appState.sensors.smoke = environment.smoke ?? appState.sensors.smoke;
    appState.sensors.smokeRaw = environment.smoke_raw ?? appState.sensors.smokeRaw;
    appState.sensors.smokeLevel = environment.smoke_level ?? appState.sensors.smokeLevel;
    appState.sensors.distance = presence.distance ?? appState.sensors.distance;
    appState.sensors.personNear = presence.person_near ?? appState.sensors.personNear;
    appState.sensors.presence = presence.presence ?? appState.sensors.presence;
    appState.sensors.faceDetected = accessControl.face_detected ?? appState.sensors.faceDetected;
    appState.sensors.faceCount = accessControl.face_count ?? appState.sensors.faceCount;
    appState.sensors.authorized = accessControl.authorized ?? appState.sensors.authorized;
    appState.sensors.doorState = accessControl.door_state ?? appState.sensors.doorState;
    appState.sensors.timestamp = status.timestamp ?? appState.sensors.timestamp;

    appState.devices.cooling = devices.cooling ?? appState.devices.cooling;
    appState.devices.heating = devices.heating ?? appState.devices.heating;
    appState.devices.humidifier = devices.humidifier ?? appState.devices.humidifier;
    appState.devices.dehumidifier = devices.dehumidifier ?? appState.devices.dehumidifier;
    appState.devices.projector = devices.projector ?? appState.devices.projector;
    appState.devices.lightOn = devices.light_on ?? appState.devices.lightOn;
    appState.devices.curtainOpen = devices.curtain_open ?? appState.devices.curtainOpen;
    appState.devices.buzzerOn = devices.buzzer_on ?? appState.devices.buzzerOn;
    appState.devices.alarm = devices.alarm ?? appState.devices.alarm;
    appState.devices.light = devices.light_on ?? appState.devices.light;
    appState.devices.auto = devices.auto_mode ?? (system.sensor_status !== "error");
}

function simulateRoomTelemetry(room) {
    const now = new Date();
    const baseOffset = room.numericId - 1;
    appState.sensors.temperature = roundTo(24.6 + Math.sin(now.getMinutes() / 8 + baseOffset) * 1.4, 1);
    appState.sensors.humidity = Math.round(48 + Math.cos(now.getMinutes() / 7 + baseOffset) * 11);
    appState.sensors.light = Math.max(12, Math.min(95, Math.round(58 + Math.sin(now.getMinutes() / 6 + baseOffset) * 18)));
    appState.sensors.lightRaw = Math.round(appState.sensors.light * 2.55);
    appState.sensors.lightLux = Math.round(180 + appState.sensors.light * 5.7);
    appState.sensors.smoke = null;
    appState.sensors.smokeRaw = null;
    appState.sensors.smokeLevel = "未部署";
    appState.sensors.distance = room.key === "training" ? 82.5 : room.key === "creative" ? 138.2 : 118.4;
    appState.sensors.personNear = appState.sensors.distance < 100;
    appState.sensors.presence = room.key === "training";
    appState.sensors.faceDetected = false;
    appState.sensors.faceCount = 0;
    appState.sensors.authorized = false;
    appState.sensors.doorState = "closed";
    appState.sensors.timestamp = now.toISOString().slice(0, 19);
}

function updateDerivedView() {
    const room = getCurrentRoom();
    const mode = modeDefinitions.find((entry) => entry.key === appState.selectedModeKey);
    const deviceCount = 7;
    const healthState = deriveHealthState();

    dom.modeBadge.textContent = mode?.badge || "仿真控制";
    dom.bannerDescription.textContent = room.banner;
    dom.viewerTitle.textContent = `${room.label} · three.js 模型嵌入窗口`;
    dom.roomDetailTitle.textContent = room.label;
    dom.roomDetailSummary.textContent = room.summary;
    dom.roomDetailPoints.innerHTML = room.points.map((item) => `<li>${item}</li>`).join("");

    dom.statRoomCount.textContent = String(roomDefinitions.length);
    dom.statDeviceCount.textContent = String(deviceCount);
    dom.statSystemStatus.textContent = healthState.title;
    dom.statSystemNote.textContent = healthState.note;

    dom.healthTitle.textContent = healthState.headline;
    dom.healthCopy.textContent = healthState.copy;
    dom.healthBadge.textContent = healthState.title;
    dom.healthBadge.className = `pill ${healthState.badgeClass}`;

    dom.dataSourceBadge.textContent = appState.dataSource === "live" ? "后端数据" : "仿真数据";
    dom.syncTag.textContent = appState.syncMessage;

    renderDeviceStatus();
    renderSensorStatus();
    updateDeviceCard();
    renderLogs();
    sceneController.updateHotspots(room);
    sceneController.switchRoom(room);
}

function deriveHealthState() {
    const temp = appState.sensors.temperature;
    const humidity = appState.sensors.humidity;
    const lightLux = appState.sensors.lightLux;
    const tooHot = typeof temp === "number" && temp > 28;
    const tooCold = typeof temp === "number" && temp < 20;
    const humidityIssue = typeof humidity === "number" && (humidity > 65 || humidity < 40);
    const lightIssue = typeof lightLux === "number" && lightLux < 180;
    const warning = tooHot || tooCold || humidityIssue || lightIssue || appState.devices.alarm;

    if (warning) {
        return {
            title: "关注",
            badgeClass: "warning",
            headline: "环境波动 · 联动调整",
            copy: "当前检测到环境偏离目标区间，系统已切换到联动修正策略。",
            note: "存在轻微波动，策略已启动。"
        };
    }

    return {
        title: "正常",
        badgeClass: "success",
        headline: "运行正常 · 自动联动",
        copy: "环境稳定，树莓派边缘控制器在线。",
        note: "环境稳定，树莓派边缘控制器在线。"
    };
}

function renderDeviceStatus() {
    const rows = [
        ["投影状态", appState.devices.projector ? "已开启" : "已关闭"],
        ["灯光状态", appState.devices.lightOn ? "已开启" : "已关闭"],
        ["自动模式", appState.devices.auto ? "已开启" : "已关闭"],
        ["人员状态", appState.sensors.presence ? "当前有人" : "当前无人"]
    ];
    dom.deviceStatusList.innerHTML = rows.map(renderStatusRow).join("");
}

function renderSensorStatus() {
    const rows = [
        ["温湿度", `${formatNumber(appState.sensors.temperature)} °C / ${formatInteger(appState.sensors.humidity)}%`],
        ["光照强度", `${formatInteger(appState.sensors.lightLux)} lx`],
        ["烟雾浓度", appState.sensors.smoke == null ? "未部署" : formatNumber(appState.sensors.smoke)],
        ["距离感知", appState.sensors.distance == null ? "未部署" : `${formatNumber(appState.sensors.distance)} cm`]
    ];
    dom.sensorStatusList.innerHTML = rows.map(renderStatusRow).join("");
}

function renderStatusRow([label, value]) {
    return `
        <div class="status-row">
            <span class="status-row-label">${label}</span>
            <strong class="status-row-value">${value}</strong>
        </div>
    `;
}

function updateDeviceCard() {
    const detail = hotspotDetails[appState.selectedHotspotId];
    const active = resolveHotspotActive(appState.selectedHotspotId);
    dom.hotspotCardTitle.textContent = detail.title;
    dom.hotspotCardBadge.textContent = active ? detail.activeBadge : detail.idleBadge;
    dom.hotspotCardBadge.className = `pill ${active ? "success" : "warning"}`;
    dom.hotspotCardSubtitle.textContent = active ? detail.subtitleOn : detail.subtitleOff;
    dom.hotspotCardDescription.textContent = detail.description;

    const metaRows = buildHotspotMeta(appState.selectedHotspotId);
    dom.hotspotCardMeta.innerHTML = metaRows
        .map(
            ([label, value]) => `
                <div class="device-card-meta-row">
                    <span>${label}</span>
                    <strong>${value}</strong>
                </div>
            `
        )
        .join("");
}

function buildHotspotMeta(hotspotId) {
    switch (hotspotId) {
        case "projector":
            return [
                ["设备状态", appState.devices.projector ? "投影运行中" : "投影待机"],
                ["联动结果", appState.devices.projector ? "幕布已激活" : "等待演示模式"],
                ["当前模式", modeDefinitions.find((mode) => mode.key === appState.selectedModeKey)?.label || "未知"]
            ];
        case "light":
            return [
                ["灯光策略", appState.devices.lightOn ? "照明开启" : "照明关闭"],
                ["自动联动", appState.devices.auto ? "已启用" : "手动控制"],
                ["环境亮度", `${formatInteger(appState.sensors.lightLux)} lx`]
            ];
        case "temperature":
            return [
                ["当前温度", `${formatNumber(appState.sensors.temperature)} °C`],
                ["当前湿度", `${formatInteger(appState.sensors.humidity)}%`],
                [
                    "环境策略",
                    appState.devices.cooling
                        ? "制冷运行"
                        : appState.devices.heating
                            ? "制热运行"
                            : "舒适区间"
                ]
            ];
        case "photoresistor":
        default:
            return [
                ["光照百分比", `${formatInteger(appState.sensors.light)}%`],
                ["原始采样", appState.sensors.lightRaw == null ? "--" : String(appState.sensors.lightRaw)],
                ["窗帘状态", appState.devices.curtainOpen ? "已打开" : "已关闭"]
            ];
    }
}

function resolveHotspotActive(hotspotId) {
    switch (hotspotId) {
        case "projector":
            return appState.devices.projector;
        case "light":
            return appState.devices.lightOn;
        case "temperature":
            return !deriveHealthState().badgeClass.includes("warning");
        case "photoresistor":
            return (appState.sensors.lightLux ?? 0) >= 180;
        default:
            return false;
    }
}

function renderLogs() {
    dom.logList.innerHTML = appState.logs
        .slice(0, 5)
        .map(
            (item) => `
                <article class="log-item">
                    <span class="log-time">${item.time}</span>
                    <p class="log-text">${item.text}</p>
                </article>
            `
        )
        .join("");
}

function pushLog(text) {
    const timestamp = new Date();
    const timeText = timestamp.toLocaleTimeString("zh-CN", { hour: "2-digit", minute: "2-digit", hour12: false });
    appState.logs.unshift({ time: timeText, text });
    appState.logs = appState.logs.slice(0, 8);
    renderLogs();
}

function getCurrentRoom() {
    return roomDefinitions.find((room) => room.key === appState.selectedRoomKey) || roomDefinitions[0];
}

function formatNumber(value) {
    if (value == null || Number.isNaN(Number(value))) {
        return "--";
    }
    return Number(value).toFixed(1);
}

function formatInteger(value) {
    if (value == null || Number.isNaN(Number(value))) {
        return "--";
    }
    return String(Math.round(Number(value)));
}

function roundTo(value, digits) {
    const factor = 10 ** digits;
    return Math.round(value * factor) / factor;
}

function createSceneController(container, hotspotLayer, hotspotSelectHandler) {
    const iframe = document.getElementById("scene-iframe");
    let renderer = null;
    let scene = null;
    let camera = null;
    let controls = null;
    const hotspotMeshes = new Map();
    const meshRegistry = new Map();
    const hotspotDomRegistry = new Map();
    let sceneGroup = null;
    let mountedAdapter = null;
    let containerSize = { width: 0, height: 0 };
    let usingIframe = false;
    let currentRoomKey = null;
    let fallbackReady = false;

    function ensureFallbackRenderer() {
        if (fallbackReady) {
            return true;
        }

        try {
            renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });
            scene = new THREE.Scene();
            camera = new THREE.PerspectiveCamera(42, 1, 0.1, 100);
            controls = new OrbitControls(camera, renderer.domElement);
            sceneGroup = new THREE.Group();

            renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
            renderer.setClearColor(0x000000, 0);
            container.innerHTML = "";
            container.appendChild(renderer.domElement);

            controls.enableDamping = true;
            controls.target.set(0, 1.15, -0.4);
            controls.maxDistance = 10;
            controls.minDistance = 2.4;
            camera.position.set(4.8, 3.4, 5.4);

            scene.add(sceneGroup);
            buildBaseLighting();
            buildFallbackScene();
            fallbackReady = true;
            return true;
        } catch (error) {
            console.error("Failed to initialize fallback three.js renderer:", error);
            return false;
        }
    }

    function buildBaseLighting() {
        if (!scene) {
            return;
        }
        scene.background = new THREE.Color(0xdfe8f4);
        const ambientLight = new THREE.AmbientLight(0xffffff, 1.24);
        scene.add(ambientLight);

        const directionalLight = new THREE.DirectionalLight(0xfff0de, 1.05);
        directionalLight.position.set(5, 8, 4);
        scene.add(directionalLight);

        const fillLight = new THREE.DirectionalLight(0xd4ebff, 0.8);
        fillLight.position.set(-4, 5, 2);
        scene.add(fillLight);
    }

    function buildFallbackScene() {
        if (!sceneGroup) {
            return;
        }
        sceneGroup.clear();
        hotspotMeshes.clear();
        meshRegistry.clear();

        const room = new THREE.Group();
        sceneGroup.add(room);

        const floor = new THREE.Mesh(
            new THREE.BoxGeometry(8.2, 0.18, 5.8),
            new THREE.MeshStandardMaterial({ color: 0x2e3440, roughness: 0.82 })
        );
        floor.position.set(0, -0.1, 0);
        room.add(floor);

        const ceiling = new THREE.Mesh(
            new THREE.BoxGeometry(8.2, 0.14, 5.8),
            new THREE.MeshStandardMaterial({ color: 0xcfd8e6, roughness: 0.72 })
        );
        ceiling.position.set(0, 2.95, 0);
        room.add(ceiling);

        const wallMaterial = new THREE.MeshStandardMaterial({ color: 0x888f99, roughness: 0.96 });
        const backWall = new THREE.Mesh(new THREE.BoxGeometry(8.2, 3, 0.14), wallMaterial);
        backWall.position.set(0, 1.4, -2.86);
        room.add(backWall);

        const sideWall = new THREE.Mesh(new THREE.BoxGeometry(0.14, 3, 5.8), wallMaterial);
        sideWall.position.set(4.03, 1.4, 0);
        room.add(sideWall);

        const tableTop = new THREE.Mesh(
            new THREE.BoxGeometry(3.1, 0.14, 1.35),
            new THREE.MeshStandardMaterial({ color: 0x7d6247, roughness: 0.44 })
        );
        tableTop.position.set(0, 0.82, 0.2);
        room.add(tableTop);

        const tableBase = new THREE.Mesh(
            new THREE.BoxGeometry(2.4, 0.26, 1.1),
            new THREE.MeshStandardMaterial({ color: 0x3b3f48, roughness: 0.8 })
        );
        tableBase.position.set(0, 0.48, 0.2);
        room.add(tableBase);

        buildChairs(room);
        buildScreen(room);
        buildLightFixture(room);
        buildPlant(room);
        buildSensorPole(room);
    }

    function buildChairs(parent) {
        const chairGeometry = new THREE.BoxGeometry(0.36, 0.72, 0.36);
        const backGeometry = new THREE.BoxGeometry(0.36, 0.58, 0.08);
        const legGeometry = new THREE.CylinderGeometry(0.025, 0.025, 0.62, 12);
        const seatMaterial = new THREE.MeshStandardMaterial({ color: 0x29477b, roughness: 0.62 });
        const legMaterial = new THREE.MeshStandardMaterial({ color: 0x1f2429, roughness: 0.9 });

        const chairPositions = [
            [-1.18, 0, 0.95], [-0.38, 0, 0.95], [0.38, 0, 0.95], [1.18, 0, 0.95],
            [-1.18, 0, -0.58], [-0.38, 0, -0.58], [0.38, 0, -0.58], [1.18, 0, -0.58],
            [-1.9, 0, 0.22], [1.9, 0, 0.22]
        ];

        chairPositions.forEach(([x, y, z]) => {
            const chair = new THREE.Group();
            chair.position.set(x, y, z);

            const seat = new THREE.Mesh(chairGeometry, seatMaterial);
            seat.position.set(0, 0.58, 0);
            chair.add(seat);

            const back = new THREE.Mesh(backGeometry, seatMaterial);
            back.position.set(0, 1.02, -0.13);
            chair.add(back);

            const legOffsets = [
                [-0.12, 0.3, -0.12], [0.12, 0.3, -0.12], [-0.12, 0.3, 0.12], [0.12, 0.3, 0.12]
            ];
            legOffsets.forEach(([lx, ly, lz]) => {
                const leg = new THREE.Mesh(legGeometry, legMaterial);
                leg.position.set(lx, ly, lz);
                chair.add(leg);
            });

            parent.add(chair);
        });
    }

    function buildScreen(parent) {
        const screen = new THREE.Mesh(
            new THREE.BoxGeometry(1.35, 0.78, 0.04),
            new THREE.MeshStandardMaterial({
                color: 0x182127,
                emissive: 0x0d2e2f,
                emissiveIntensity: 0.28,
                roughness: 0.2
            })
        );
        screen.position.set(0, 1.55, -2.72);
        parent.add(screen);
        meshRegistry.set("screen", screen);
    }

    function buildLightFixture(parent) {
        const lightBar = new THREE.Mesh(
            new THREE.BoxGeometry(3.3, 0.08, 0.18),
            new THREE.MeshStandardMaterial({
                color: 0x2b2f34,
                emissive: 0xf3d7ae,
                emissiveIntensity: 0.82,
                roughness: 0.28
            })
        );
        lightBar.position.set(0, 2.42, -0.22);
        parent.add(lightBar);
        meshRegistry.set("ceilingLight", lightBar);
    }

    function buildPlant(parent) {
        const pot = new THREE.Mesh(
            new THREE.CylinderGeometry(0.14, 0.18, 0.34, 18),
            new THREE.MeshStandardMaterial({ color: 0x8a684f, roughness: 0.75 })
        );
        pot.position.set(-2.72, 0.16, -1.38);
        parent.add(pot);

        const leafMaterial = new THREE.MeshStandardMaterial({ color: 0x4b8d70, roughness: 0.48 });
        [0.3, 0.55, 0.8].forEach((height, index) => {
            const leaf = new THREE.Mesh(new THREE.SphereGeometry(0.16 + index * 0.02, 18, 18), leafMaterial);
            leaf.position.set(-2.72, height, -1.38);
            parent.add(leaf);
        });
    }

    function buildSensorPole(parent) {
        const pole = new THREE.Mesh(
            new THREE.CylinderGeometry(0.03, 0.03, 0.9, 10),
            new THREE.MeshStandardMaterial({ color: 0x7890b1, roughness: 0.7 })
        );
        pole.position.set(-2.16, 0.55, -0.76);
        parent.add(pole);
        meshRegistry.set("sensorPole", pole);
    }

    function updateHotspots(room) {
        hotspotLayer.innerHTML = "";
        hotspotDomRegistry.clear();
        if (usingIframe || !sceneGroup) {
            return;
        }
        room.hotspots.forEach((hotspot) => {
            const anchor = new THREE.Object3D();
            anchor.position.set(...hotspot.position);
            sceneGroup.add(anchor);
            hotspotMeshes.set(hotspot.id, anchor);

            const marker = document.createElement("div");
            marker.className = `hotspot-marker${hotspot.id === appState.selectedHotspotId ? " active" : ""}`;
            marker.innerHTML = `
                <span class="hotspot-pin"></span>
                <button type="button" class="hotspot-badge">${hotspot.name}</button>
            `;
            marker.querySelector(".hotspot-badge").addEventListener("click", () => hotspotSelectHandler(hotspot.id));
            hotspotLayer.appendChild(marker);
            hotspotDomRegistry.set(hotspot.id, marker);
        });
    }

    function highlightHotspot(hotspotId) {
        if (usingIframe) {
            return;
        }
        hotspotDomRegistry.forEach((node, id) => {
            node.classList.toggle("active", id === hotspotId);
        });

        const room = getCurrentRoom();
        room.hotspots.forEach((hotspot) => {
            const mesh = meshRegistry.get(hotspot.mesh);
            if (!mesh || !mesh.material) {
                return;
            }
            if (Array.isArray(mesh.material)) {
                return;
            }
            if ("emissiveIntensity" in mesh.material) {
                const active = hotspot.id === hotspotId;
                mesh.material.emissiveIntensity = hotspot.id === "light"
                    ? active ? 1.2 : (appState.devices.lightOn ? 0.82 : 0.12)
                    : hotspot.id === "projector"
                        ? active ? 0.52 : (appState.devices.projector ? 0.28 : 0.04)
                        : active ? 0.18 : 0.02;
            }
        });
    }

    function syncState(room, state) {
        if (usingIframe) {
            postMessageToIframe("device.sync", { devices: state.devices });
            return;
        }
        const screen = meshRegistry.get("screen");
        const ceilingLight = meshRegistry.get("ceilingLight");
        if (screen?.material && "emissiveIntensity" in screen.material) {
            screen.material.emissiveIntensity = state.devices.projector ? 0.36 : 0.04;
            screen.material.color.setHex(state.devices.projector ? 0x1a232a : 0x182127);
        }
        if (ceilingLight?.material && "emissiveIntensity" in ceilingLight.material) {
            ceilingLight.material.emissiveIntensity = state.devices.lightOn ? 0.9 : 0.1;
            ceilingLight.material.color.setHex(state.devices.lightOn ? 0x2b2f34 : 0x34383d);
        }
        updateHotspots(room);
        highlightHotspot(state.selectedHotspotId);
    }

    function setPreset(presetKey) {
        if (usingIframe) {
            postMessageToIframe("camera.preset", { preset: presetKey });
            return;
        }
        if (!ensureFallbackRenderer()) {
            return;
        }
        const presets = {
            overview: { position: new THREE.Vector3(4.8, 3.4, 5.4), target: new THREE.Vector3(0, 1.15, -0.4) },
            front: { position: new THREE.Vector3(0, 2.0, 6.2), target: new THREE.Vector3(0, 1.4, -0.6) },
            top: { position: new THREE.Vector3(0.2, 7.4, 0.6), target: new THREE.Vector3(0, 0.9, -0.2) }
        };
        const preset = presets[presetKey] || presets.overview;
        camera.position.copy(preset.position);
        controls.target.copy(preset.target);
        controls.update();
    }

    function applyCameraControl(controlKey) {
        if (usingIframe) {
            postMessageToIframe("camera.control", { control: controlKey });
            return;
        }
        if (!ensureFallbackRenderer()) {
            return;
        }
        const panStep = 0.35;
        const orbitStep = 0.18;
        const zoomFactor = 0.92;
        switch (controlKey) {
            case "turnLeft":
                rotateAzimuth(orbitStep);
                break;
            case "turnRight":
                rotateAzimuth(-orbitStep);
                break;
            case "tiltUp":
                rotatePolar(-orbitStep * 0.7);
                break;
            case "tiltDown":
                rotatePolar(orbitStep * 0.7);
                break;
            case "panLeft":
                pan(new THREE.Vector3(-panStep, 0, 0));
                break;
            case "panRight":
                pan(new THREE.Vector3(panStep, 0, 0));
                break;
            case "panUp":
                pan(new THREE.Vector3(0, panStep, 0));
                break;
            case "panDown":
                pan(new THREE.Vector3(0, -panStep, 0));
                break;
            case "zoomIn":
                zoom(zoomFactor);
                break;
            case "zoomOut":
                zoom(1 / zoomFactor);
                break;
            default:
                break;
        }
    }

    function rotateAzimuth(angle) {
        const offset = camera.position.clone().sub(controls.target);
        const spherical = new THREE.Spherical().setFromVector3(offset);
        spherical.theta += angle;
        offset.setFromSpherical(spherical);
        camera.position.copy(controls.target.clone().add(offset));
        controls.update();
    }

    function rotatePolar(angle) {
        const offset = camera.position.clone().sub(controls.target);
        const spherical = new THREE.Spherical().setFromVector3(offset);
        spherical.phi = THREE.MathUtils.clamp(spherical.phi + angle, 0.32, Math.PI / 2.02);
        offset.setFromSpherical(spherical);
        camera.position.copy(controls.target.clone().add(offset));
        controls.update();
    }

    function pan(delta) {
        camera.position.add(delta);
        controls.target.add(delta);
        controls.update();
    }

    function zoom(scale) {
        const offset = camera.position.clone().sub(controls.target).multiplyScalar(scale);
        camera.position.copy(controls.target.clone().add(offset));
        controls.update();
    }

    async function init() {
        window.addEventListener("resize", resize);
        await switchRoom(getCurrentRoom());
        resize();
        setPreset(appState.selectedPresetKey);
        animate();
    }

    async function tryLoadAdapter(room) {
        try {
            if (room?.modelPage) {
                iframe.src = room.modelPage;
                iframe.classList.add("active");
                container.style.display = "none";
                hotspotLayer.style.display = "none";
                usingIframe = true;
                mountedAdapter = null;
                iframe.onload = () => {
                    postMessageToIframe("camera.preset", { preset: appState.selectedPresetKey });
                    postMessageToIframe("device.sync", { devices: appState.devices });
                };
                return;
            }

            if (!ensureFallbackRenderer()) {
                return;
            }
            const module = await import("/assets/models/model-adapter.js");
            if (module?.mountMeetingRoomModel) {
                mountedAdapter = await module.mountMeetingRoomModel({
                    THREE,
                    scene,
                    sceneGroup,
                    camera,
                    controls,
                    container,
                    onHotspotSelect: hotspotSelectHandler
                });
            } else if (module?.modelUrl) {
                await loadGlb(module.modelUrl);
            } else {
                await loadGlb("/assets/models/meeting-room.glb");
            }
            iframe.classList.remove("active");
            iframe.removeAttribute("src");
            iframe.onload = null;
            container.style.display = "block";
            hotspotLayer.style.display = "block";
            usingIframe = false;
        } catch (error) {
            mountedAdapter = null;
            iframe.classList.remove("active");
            iframe.removeAttribute("src");
            iframe.onload = null;
            container.style.display = "block";
            hotspotLayer.style.display = "block";
            usingIframe = false;
        }
    }

    function postMessageToIframe(type, payload) {
        if (!iframe?.contentWindow) {
            return;
        }
        iframe.contentWindow.postMessage(
            {
                channel: "smart-room-control",
                type,
                payload,
            },
            "*"
        );
    }

    async function loadGlb(modelUrl) {
        if (!sceneGroup) {
            return;
        }
        const exists = await fetch(modelUrl, { method: "HEAD" }).then((res) => res.ok).catch(() => false);
        if (!exists) {
            return;
        }

        const { GLTFLoader } = await import("/vendor/three/examples/jsm/loaders/GLTFLoader.js");
        const gltfLoader = new GLTFLoader();

        return new Promise((resolve, reject) => {
            gltfLoader.load(
                modelUrl,
                (gltf) => {
                    sceneGroup.clear();
                    sceneGroup.add(gltf.scene);
                    resolve();
                },
                undefined,
                reject
            );
        });
    }

    function resize() {
        if (usingIframe || !renderer || !camera) {
            return;
        }
        const { clientWidth, clientHeight } = container;
        if (!clientWidth || !clientHeight) {
            return;
        }
        containerSize = { width: clientWidth, height: clientHeight };
        camera.aspect = clientWidth / clientHeight;
        camera.updateProjectionMatrix();
        renderer.setSize(clientWidth, clientHeight, false);
    }

    function projectHotspots() {
        if (usingIframe || !camera) {
            return;
        }
        hotspotMeshes.forEach((anchor, hotspotId) => {
            const marker = hotspotDomRegistry.get(hotspotId);
            if (!marker) {
                return;
            }

            const projected = anchor.position.clone();
            anchor.getWorldPosition(projected);
            projected.project(camera);

            const visible = projected.z < 1 && projected.z > -1;
            marker.style.display = visible ? "flex" : "none";
            if (!visible) {
                return;
            }

            const x = (projected.x * 0.5 + 0.5) * containerSize.width;
            const y = (-projected.y * 0.5 + 0.5) * containerSize.height;
            marker.style.left = `${x}px`;
            marker.style.top = `${y}px`;
        });
    }

    function animate() {
        requestAnimationFrame(animate);
        if (!usingIframe && renderer && scene && camera && controls) {
            controls.update();
            mountedAdapter?.update?.(appState);
            renderer.render(scene, camera);
            projectHotspots();
        }
    }

    async function switchRoom(room) {
        if (!room || currentRoomKey === room.key) {
            return;
        }
        currentRoomKey = room.key;
        await tryLoadAdapter(room);
    }

    return {
        init,
        updateHotspots,
        highlightHotspot,
        syncState,
        setPreset,
        applyCameraControl,
        switchRoom
    };
}
