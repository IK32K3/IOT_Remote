#include <Arduino.h>
#include <ArduinoJson.h>
#include <PubSubClient.h>
#include <WiFi.h>
#include <WiFiProv.h>
#include <WifiUdp.h>
#include <cstring>
#include <IRutils.h>

#include "App.h"
#include "Config.h"
#include "DeviceManager.h"
#include "IrLearner.h"
#include "WifiKnownNetworks.h"
#include "devices/AcController.h"
#include "devices/TvController.h"
#include "devices/FanController.h"
#include "devices/StbController.h"
#include "devices/DvdController.h"
#include "devices/ProjectorController.h"
namespace {

constexpr unsigned long kStatusIntervalMs = 60UL * 1000UL;
constexpr unsigned long kWifiConnectAttemptMs = 10000UL;
const String kStatusTopic = String("iot/nodes/") + NODE_ID + "/status";
const String kCommandTopic = String("iot/nodes/") + NODE_ID + "/commands";
const String kLegacyAcTopic = String("iot/nodes/") + NODE_ID + "/ir/test";
const String kAcCommandTopic = String("iot/nodes/") + NODE_ID + "/ac/cmd";
const String kFanCommandTopic =
    String("iot/nodes/") + NODE_ID + "/fan/cmd";
const String kTvCommandTopic =
    String("iot/nodes/") + NODE_ID + "/tv/cmd";
const String kDvdCommandTopic =
    String("iot/nodes/") + NODE_ID + "/dvd/cmd";
const String kStbCommandTopic =
    String("iot/nodes/") + NODE_ID + "/stb/cmd";
const String kProjectorCommandTopic =
    String("iot/nodes/") + NODE_ID + "/projector/cmd";
const String kLearnCommandTopic =
    String("iot/nodes/") + NODE_ID + "/ir/learn/cmd";
const String kFanLearnCommandTopic =
    String("iot/nodes/") + NODE_ID + "/fan/learn/cmd";
const String kLearnResultTopic =
    String("iot/nodes/") + NODE_ID + "/ir/learn";
const String kDeviceLearnResultPrefix =
    String("iot/nodes/") + NODE_ID + "/";
const String kDiscoveryResponsePrefix = "MQTT://";
WiFiClient wifiClient;
PubSubClient mqtt(wifiClient);
DeviceManager deviceManager;
AcController acController(NODE_ID, IR_LED_PIN);
FanController fanController(NODE_ID, IR_LED_PIN);
TvController tvController(NODE_ID, IR_LED_PIN);
StbController stbController(NODE_ID, IR_LED_PIN);
DvdController dvdController(NODE_ID, IR_LED_PIN);
ProjectorController projectorController(NODE_ID, IR_LED_PIN);
IrLearner irLearner(IR_RECEIVER_PIN);

unsigned long lastStatusPublished = 0;

void ensureWifiConnected();
void ensureMqttConnected();
void publishAvailability();
void publishDeviceState(DeviceController &controller, bool retained = true);
void handleMqttMessage(char *topic, byte *payload, unsigned int length);
void handleLearnCommand(JsonObjectConst cmd, const String &topicDevice = String());
bool configureMqttServer();
bool autoDiscoverBroker(String &hostOut, uint16_t &portOut);
IPAddress calculateBroadcastAddress();
bool parseDiscoveryResponse(const String &response, String &hostOut,
                            uint16_t &portOut);
void publishLearningResult(const IrLearningResult &result);
bool mqttServerConfigured = false;
String resolvedMqttHost = MQTT_HOST;
uint16_t resolvedMqttPort = MQTT_PORT;
bool provisioningStarted = false;
unsigned long wifiAttemptStartedAt = 0;
bool wifiBeginCalled = false;


String readPayload(byte *payload, unsigned int length) {
  String data;
  data.reserve(length);
  for (unsigned int i = 0; i < length; ++i) {
    data += static_cast<char>(payload[i]);
  }
  return data;
}

String inferDeviceFromTopic(const String &topic) {
  if (topic.equalsIgnoreCase(kLegacyAcTopic)) {
    return "ac";
  }
  if (topic.equalsIgnoreCase(kFanCommandTopic)) {
    return "fan";
  }

  const String prefix = String("iot/nodes/") + NODE_ID + "/";
  if (!topic.startsWith(prefix)) {
    return "";
  }

  const int start = prefix.length();
  const int nextSlash = topic.indexOf('/', start);
  if (nextSlash == -1) {
    const String suffix = topic.substring(start);
    if (suffix.equalsIgnoreCase("commands")) {
      return "";
    }
    return suffix;
  }

  return topic.substring(start, nextSlash);
}

}  // namespace

void setupApp() {
  pinMode(STATUS_LED_PIN, OUTPUT);
  digitalWrite(STATUS_LED_PIN, LOW);

  Serial.begin(115200);
  delay(100);
  Serial.println();
  Serial.println(F("[BOOT] ESP32 multi-device node starting"));

  WifiKnownNetworks::begin();

  WiFi.onEvent([](arduino_event_t *sys_event) {
    switch (sys_event->event_id) {
      case ARDUINO_EVENT_WIFI_STA_GOT_IP:
        Serial.printf("[WIFI] Connected, IP: %s\n",
                      IPAddress(sys_event->event_info.got_ip.ip_info.ip.addr)
                          .toString()
                          .c_str());
        WifiKnownNetworks::markUsed(WiFi.SSID());
        mqttServerConfigured = false;
        provisioningStarted = false;
        wifiBeginCalled = false;
        wifiAttemptStartedAt = 0;
        break;
      case ARDUINO_EVENT_WIFI_STA_DISCONNECTED:
        Serial.println(F("[WIFI] Disconnected"));
        mqttServerConfigured = false;
        break;
      case ARDUINO_EVENT_PROV_START:
        Serial.println(F("[WIFI][PROV] BLE provisioning started"));
        break;
      case ARDUINO_EVENT_PROV_CRED_RECV:
        Serial.printf("[WIFI][PROV] Credentials received (ssid=%s)\n",
                      (const char *)sys_event->event_info.prov_cred_recv.ssid);
        WifiKnownNetworks::upsert(
            String((const char *)sys_event->event_info.prov_cred_recv.ssid),
            String((const char *)sys_event->event_info.prov_cred_recv.password));
        break;
      case ARDUINO_EVENT_PROV_CRED_FAIL:
        Serial.printf("[WIFI][PROV] Provisioning failed (reason=%d)\n",
                      (int)sys_event->event_info.prov_fail_reason);
        break;
      case ARDUINO_EVENT_PROV_CRED_SUCCESS:
        Serial.println(F("[WIFI][PROV] Provisioning success"));
        break;
      case ARDUINO_EVENT_PROV_END:
        Serial.println(F("[WIFI][PROV] Provisioning ended"));
        break;
      default:
        break;
    }
  });

  deviceManager.registerController(acController);
  deviceManager.registerController(fanController);
  deviceManager.registerController(tvController);
  deviceManager.registerController(stbController);
  deviceManager.registerController(dvdController);
  deviceManager.registerController(projectorController);
  deviceManager.begin();

  irLearner.setResultCallback(publishLearningResult);
  irLearner.begin();
  
  ensureWifiConnected();

  configureMqttServer();
  mqtt.setCallback(handleMqttMessage);
}

void loopApp() {
  ensureWifiConnected();
  ensureMqttConnected();

  mqtt.loop();
  irLearner.loop();

  const unsigned long now = millis();
  if (now - lastStatusPublished > kStatusIntervalMs) {
    publishAvailability();
  }
}

namespace {

void ensureWifiConnected() {
  if (WiFi.isConnected()) return;

  WiFi.mode(WIFI_STA);

  // First: try reconnecting using stored credentials in NVS (from previous run
  // or provisioning).
  if (!wifiBeginCalled) {
    wifiBeginCalled = true;
    wifiAttemptStartedAt = millis();
    WifiKnownNetworks::Network picked;
    if (WifiKnownNetworks::selectBestFromScan(picked)) {
      Serial.printf("[WIFI] Connecting to known SSID=%s (saved list size=%u)\n",
                    picked.ssid.c_str(),
                    static_cast<unsigned>(WifiKnownNetworks::count()));
      WiFi.begin(picked.ssid.c_str(), picked.password.c_str());
    } else {
      Serial.printf("[WIFI] Connecting using stored credentials (known list size=%u)\n",
                    static_cast<unsigned>(WifiKnownNetworks::count()));
      WiFi.begin();
    }
  }

  // If connection is taking too long, fall back to compile-time SSID (if set),
  // otherwise start BLE provisioning.
  if (wifiAttemptStartedAt != 0 &&
      millis() - wifiAttemptStartedAt < kWifiConnectAttemptMs) {
    return;
  }

  // Fallback to hardcoded Wi-Fi, if provided.
  if (!provisioningStarted && strlen(WIFI_SSID) > 0) {
    provisioningStarted = false;
    wifiAttemptStartedAt = millis();
    Serial.printf("[WIFI] Fallback connect to SSID=%s\n", WIFI_SSID);
    WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
    WifiKnownNetworks::upsert(String(WIFI_SSID), String(WIFI_PASSWORD));
    return;
  }

  // Start BLE provisioning once. After credentials are received, WiFiProv will
  // connect automatically and ARDUINO_EVENT_WIFI_STA_GOT_IP will fire.
  if (!provisioningStarted) {
#if CONFIG_BLUEDROID_ENABLED
    provisioningStarted = true;
    wifiBeginCalled = false;
    wifiAttemptStartedAt = 0;

    const String serviceName =
        String("PROV_") + String(NODE_ID) + "_" + WiFi.macAddress().substring(12);
    Serial.printf("[WIFI][PROV] Start BLE provisioning (service=%s)\n",
                  serviceName.c_str());
    Serial.println(
        F("[WIFI][PROV] Use Espressif 'ESP BLE Provisioning' app to send SSID/password"));

    // Security 0: no proof-of-possession, simplest UX.
    WiFiProv.beginProvision(WIFI_PROV_SCHEME_BLE,
                            WIFI_PROV_SCHEME_HANDLER_FREE_BTDM,
                            WIFI_PROV_SECURITY_0, nullptr, serviceName.c_str(),
                            nullptr, nullptr, false);
#else
    Serial.println(
        F("[WIFI][PROV] BLE not available in this build (CONFIG_BLUEDROID_ENABLED=0)"));
#endif
  }
}

void ensureMqttConnected() {
  if (mqtt.connected()) return;

  if (!configureMqttServer()) {
    delay(1000);
    return;
  }

  while (!mqtt.connected()) {
    String clientId = String("esp32-") + String(NODE_ID) + "-" + WiFi.macAddress();
    clientId.replace(":", "");
    clientId += "-" + String(millis() & 0xFFFF, HEX);
    Serial.printf("[MQTT] Connecting to %s:%u (clientId=%s)\n", resolvedMqttHost.c_str(),
                  MQTT_PORT, clientId.c_str());

    bool connected;
    if (strlen(MQTT_USERNAME) > 0 || strlen(MQTT_PASSWORD) > 0) {
      connected = mqtt.connect(clientId.c_str(), MQTT_USERNAME, MQTT_PASSWORD,
                               kStatusTopic.c_str(), 1, true, "offline");
    } else {
      connected = mqtt.connect(clientId.c_str(), kStatusTopic.c_str(), 1, true,
                               "offline");
    }

    if (connected) {
      Serial.println(F("[MQTT] Connected"));
      mqtt.subscribe(kCommandTopic.c_str(), 1);
      mqtt.subscribe(kLegacyAcTopic.c_str(), 1);
      mqtt.subscribe(kAcCommandTopic.c_str(), 1);
      mqtt.subscribe(kFanCommandTopic.c_str(), 1);
      mqtt.subscribe(kTvCommandTopic.c_str(), 1);
      mqtt.subscribe(kDvdCommandTopic.c_str(), 1);
      mqtt.subscribe(kStbCommandTopic.c_str(), 1);
      mqtt.subscribe(kProjectorCommandTopic.c_str(), 1);
      mqtt.subscribe(kLearnCommandTopic.c_str(), 1);
      mqtt.subscribe(kFanLearnCommandTopic.c_str(), 1);
      publishAvailability();
      for (size_t i = 0; i < deviceManager.count(); ++i) {
        if (auto *controller = deviceManager.at(i)) {
          publishDeviceState(*controller);
        }
      }
    } else {
      Serial.printf("[MQTT] Failed rc=%d, retry in 2s\n", mqtt.state());
      if (strlen(MQTT_HOST) == 0) {
        mqttServerConfigured = false;
      }
      delay(2000);
    }
  }
}

bool configureMqttServer() {
  if (mqttServerConfigured) return true;
  if (!WiFi.isConnected()) return false;

  if (strlen(MQTT_HOST) > 0) {
    resolvedMqttHost = MQTT_HOST;
    resolvedMqttPort = MQTT_PORT;
    mqtt.setServer(resolvedMqttHost.c_str(), resolvedMqttPort);
    mqttServerConfigured = true;
    Serial.printf("[MQTT] Using configured broker %s:%u\n", resolvedMqttHost.c_str(),
                  resolvedMqttPort);
    return true;
  }

  String discoveredHost;
  uint16_t discoveredPort = MQTT_PORT;
  if (!autoDiscoverBroker(discoveredHost, discoveredPort)) {
    Serial.println(F("[DISCOVERY] Broker not found, retrying"));
    return false;
  }

  resolvedMqttHost = discoveredHost;
  resolvedMqttPort = discoveredPort;
  mqtt.setServer(resolvedMqttHost.c_str(), resolvedMqttPort);
  mqttServerConfigured = true;
  Serial.printf("[MQTT] Auto-discovered broker %s:%u\n", resolvedMqttHost.c_str(),
                resolvedMqttPort);
  return true;
}

bool autoDiscoverBroker(String &hostOut, uint16_t &portOut) {
  WiFiUDP udp;
  if (!udp.begin(0)) {
    Serial.println(F("[DISCOVERY] Failed to start UDP socket"));
    return false;
  }

  const IPAddress broadcast = calculateBroadcastAddress();
  Serial.printf("[DISCOVERY] Broadcasting request to %s:%u\n",
                broadcast.toString().c_str(), MQTT_DISCOVERY_PORT);

  udp.beginPacket(broadcast, MQTT_DISCOVERY_PORT);
  udp.write(reinterpret_cast<const uint8_t *>(MQTT_DISCOVERY_REQUEST),
            strlen(MQTT_DISCOVERY_REQUEST));
  udp.endPacket();

  const unsigned long start = millis();
  while (millis() - start < MQTT_DISCOVERY_TIMEOUT_MS) {
    int packetSize = udp.parsePacket();
    if (packetSize <= 0) {
      delay(100);
      continue;
    }

    char buffer[128];
    const int len = udp.read(buffer, sizeof(buffer) - 1);
    if (len <= 0) {
      continue;
    }
    buffer[len] = '\0';

    String payload(buffer);
    payload.trim();
    if (!parseDiscoveryResponse(payload, hostOut, portOut)) {
      continue;
    }

    Serial.printf("[DISCOVERY] Received broker %s:%u\n", hostOut.c_str(), portOut);
    udp.stop();
    return true;
  }

  udp.stop();
  return false;
}

bool parseDiscoveryResponse(const String &response, String &hostOut,
                            uint16_t &portOut) {
  if (!response.startsWith(kDiscoveryResponsePrefix)) {
    return false;
  }

  String payload = response.substring(kDiscoveryResponsePrefix.length());
  const int colon = payload.indexOf(':');
  if (colon == -1) {
    return false;
  }

  String host = payload.substring(0, colon);
  host.trim();
  String portStr = payload.substring(colon + 1);
  portStr.trim();

  if (host.isEmpty()) {
    return false;
  }

  uint16_t port = static_cast<uint16_t>(portStr.toInt());
  if (port == 0) {
    port = MQTT_PORT;
  }

  hostOut = host;
  portOut = port;
  return true;
}

IPAddress calculateBroadcastAddress() {
  const uint32_t ip = static_cast<uint32_t>(WiFi.localIP());
  const uint32_t mask = static_cast<uint32_t>(WiFi.subnetMask());
  const uint32_t broadcast = (ip & mask) | ~mask;
  return IPAddress(broadcast);
}

void publishAvailability() {
  if (!mqtt.connected()) return;

  if (!mqtt.publish(kStatusTopic.c_str(), "online", true)) {
    Serial.println(F("[MQTT] Failed to publish availability"));
  } else {
    lastStatusPublished = millis();
    digitalWrite(STATUS_LED_PIN, HIGH);
  }
}

void publishDeviceState(DeviceController &controller, bool retained) {
  if (!mqtt.connected()) return;
  // Only AC publishes state; others run stateless
  if (strcmp(controller.deviceType(), "ac") != 0) return;

  JsonDocument doc;
  controller.serializeState(doc);

  char buffer[256];
  size_t len = serializeJson(doc, buffer, sizeof(buffer));
  if (len == 0) {
    Serial.println(F("[STATE] Failed to serialize state"));
    return;
  }

  if (!mqtt.publish(controller.stateTopic(), buffer, retained)) {
    Serial.printf("[STATE] Failed to publish %s\n", controller.deviceType());
  } else {
    Serial.printf("[STATE] Published %s: %s\n", controller.deviceType(), buffer);
  }
}

void handleMqttMessage(char *topic, byte *payload, unsigned int length) {
  const String data = readPayload(payload, length);
  Serial.printf("[MQTT] Message on %s: %s\n", topic, data.c_str());

  JsonDocument doc;
  DeserializationError err = deserializeJson(doc, data);
  if (err) {
    Serial.printf("[MQTT] JSON parse error: %s\n", err.c_str());
    return;
  }

  const String topicStr(topic);

  if (topicStr.equalsIgnoreCase(kLearnCommandTopic)) {
    handleLearnCommand(doc.as<JsonObjectConst>());
    return;
  }

  if (topicStr.equalsIgnoreCase(kFanLearnCommandTopic)) {
    handleLearnCommand(doc.as<JsonObjectConst>(), "fan");
    return;
  }

  String device = doc["device"].as<String>();
  if (device.isEmpty() || device.equalsIgnoreCase("null")) {
    device = inferDeviceFromTopic(topicStr);
  }

  DeviceController *controller = deviceManager.find(device);
  if (controller == nullptr) {
    Serial.printf("[MQTT] No controller for device '%s'\n", device.c_str());
    return;
  }

  JsonDocument stateDoc;
  stateDoc.clear();
  if (controller->handleCommand(doc.as<JsonObjectConst>(), stateDoc)) {
    // Only AC publishes state; other devices run stateless (command-only).
    if (strcmp(controller->deviceType(), "ac") != 0) {
      return;
    }

    char buffer[256];
    size_t len = serializeJson(stateDoc, buffer, sizeof(buffer));
    if (len == 0) {
      Serial.println(F("[STATE] Failed to serialize updated state"));
      return;
    }

    if (!mqtt.publish(controller->stateTopic(), buffer, true)) {
      Serial.printf("[STATE] Failed to publish updated %s state\n",
                    controller->deviceType());
    } else {
      Serial.printf("[STATE] Updated %s: %s\n", controller->deviceType(), buffer);
    }
  }
}

void handleLearnCommand(JsonObjectConst cmd, const String &topicDevice) {
  const String action = cmd["cmd"].as<String>();
  if (!action.equalsIgnoreCase("learn")) {
    Serial.printf("[IR][LEARN] Unsupported cmd=%s\n", action.c_str());
    return;
  }

  String device = cmd["device"].as<String>();
  if (device.isEmpty()) {
    device = topicDevice;
  }
  String key = cmd["key"].as<String>();

  if (key.isEmpty()) {
    Serial.println(F("[IR][LEARN] Missing key"));
    IrLearningResult result;
    result.success = false;
    result.device = device;
    result.key = key;
    result.error = "missing_key";
    publishLearningResult(result);
    return;
  }

  String error;
  if (!irLearner.startLearning(device, key, error)) {
    Serial.printf("[IR][LEARN] Cannot start: %s\n", error.c_str());
    IrLearningResult result;
    result.success = false;
    result.device = device;
    result.key = key;
    result.error = error;
    publishLearningResult(result);
    return;
  }

  Serial.printf("[IR][LEARN] Listening for %s/%s\n", device.c_str(),
                key.c_str());
}

void publishLearningResult(const IrLearningResult &result) {
  if (!mqtt.connected()) {
    Serial.println(F("[IR][LEARN] MQTT not connected, dropping result"));
    return;
  }

  JsonDocument doc;
  String device = result.device.length() > 0 ? result.device : String("GENERIC");
  doc["device"] = device;
  doc["key"] = result.key;
  doc["status"] = result.success ? "ok" : "error";
  if (result.success) {
    doc["protocol"] = result.protocol;
    doc["code"] = result.code;
    doc["bits"] = result.bits;

    // Lưu lại vào controller tương ứng để phát lại mà không cần app gửi kèm "ir"
    decode_type_t proto = strToDecodeType(result.protocol.c_str());
    uint64_t value = 0;
    if (result.bits > 0 && result.bits <= 64 && result.code.length() > 0) {
      value = strtoull(result.code.c_str(), nullptr, 16);
    }
    if (proto != decode_type_t::UNKNOWN && result.bits > 0 &&
        (!result.raw.empty() || value != 0)) {
      String devLower = device;
      devLower.toLowerCase();
      if (devLower.equalsIgnoreCase("ac")) {
        acController.learnKey(result.key, proto, value, result.bits,
                              result.raw);
      } else if (devLower.equalsIgnoreCase("fan")) {
        fanController.learnKey(result.key, proto, value, result.bits,
                               result.raw);
      } else if (devLower.equalsIgnoreCase("tv")) {
        tvController.learnKey(result.key, proto, value, result.bits,
                               result.raw);
      } else if (devLower.equalsIgnoreCase("stb")) {
        stbController.learnKey(result.key, proto, value, result.bits,
                               result.raw);
      } else if (devLower.equalsIgnoreCase("dvd")) {
        dvdController.learnKey(result.key, proto, value, result.bits,
                               result.raw);
      } else if (devLower.equalsIgnoreCase("projector")) {
        projectorController.learnKey(result.key, proto, value, result.bits,
                                     result.raw);
      }
    }
  } else if (result.error.length() > 0) {
    doc["error"] = result.error;
  }

  // AC state codes can be long; keep enough room for JSON payload.
  char buffer[768];
  size_t len = serializeJson(doc, buffer, sizeof(buffer));
  if (len == 0) {
    Serial.println(F("[IR][LEARN] Failed to serialize result"));
    return;
  }

  String deviceLower = device;
  deviceLower.toLowerCase();
  const String deviceTopic = kDeviceLearnResultPrefix + deviceLower + "/learn";

  const bool generalOk = mqtt.publish(kLearnResultTopic.c_str(), buffer, false);
  const bool deviceOk = mqtt.publish(deviceTopic.c_str(), buffer, false);

  if (!generalOk || !deviceOk) {
    Serial.printf(
        "[IR][LEARN] Failed to publish result (general=%d device=%d)\n",
        generalOk, deviceOk);
  } else {
    Serial.printf("[IR][LEARN] Result published to %s and %s: %s\n",
                  kLearnResultTopic.c_str(), deviceTopic.c_str(), buffer);
  }
}

}  // namespace
