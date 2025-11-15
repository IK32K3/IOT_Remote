#include <Arduino.h>
#include <ArduinoJson.h>
#include <PubSubClient.h>
#include <WiFi.h>
#include <WifiUdp.h>
#include <cstring>

#include "App.h"
#include "Config.h"
#include "DeviceManager.h"
#include "devices/AcController.h"
#include "devices/FanController.h"
namespace {

constexpr unsigned long kStatusIntervalMs = 60UL * 1000UL;
const String kStatusTopic = String("iot/nodes/") + NODE_ID + "/status";
const String kCommandTopic = String("iot/nodes/") + NODE_ID + "/commands";
const String kLegacyAcTopic = String("iot/nodes/") + NODE_ID + "/ir/test";
const String kFanCommandTopic =
    String("iot/nodes/") + NODE_ID + "/fan/cmd";
const String kDiscoveryResponsePrefix = "MQTT://";
WiFiClient wifiClient;
PubSubClient mqtt(wifiClient);
DeviceManager deviceManager;
AcController acController(NODE_ID, IR_LED_PIN, AC_POWER_RELAY_PIN);
FanController fanController(NODE_ID, IR_LED_PIN);

unsigned long lastStatusPublished = 0;

void ensureWifiConnected();
void ensureMqttConnected();
void publishAvailability();
void publishDeviceState(DeviceController &controller, bool retained = true);
void handleMqttMessage(char *topic, byte *payload, unsigned int length);
bool configureMqttServer();
bool autoDiscoverBroker(String &hostOut, uint16_t &portOut);
IPAddress calculateBroadcastAddress();
bool parseDiscoveryResponse(const String &response, String &hostOut,
                            uint16_t &portOut);

bool mqttServerConfigured = false;
String resolvedMqttHost = MQTT_HOST;
uint16_t resolvedMqttPort = MQTT_PORT;


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

  deviceManager.registerController(acController);
  deviceManager.registerController(fanController);
  deviceManager.begin();

  ensureWifiConnected();

  configureMqttServer();
  mqtt.setCallback(handleMqttMessage);
}

void loopApp() {
  ensureWifiConnected();
  ensureMqttConnected();

  mqtt.loop();

  const unsigned long now = millis();
  if (now - lastStatusPublished > kStatusIntervalMs) {
    publishAvailability();
  }
}

namespace {

void ensureWifiConnected() {
  if (WiFi.isConnected()) return;

  Serial.printf("[WIFI] Connecting to %s\n", WIFI_SSID);
  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);

  uint8_t retry = 0;
  while (!WiFi.isConnected()) {
    delay(500);
    Serial.print('.');
    if (++retry >= 60) {
      Serial.println(F("\n[WIFI] Failed to connect, retrying"));
      retry = 0;
    }
  }
  Serial.printf("\n[WIFI] Connected, IP: %s\n", WiFi.localIP().toString().c_str());
  mqttServerConfigured = false;
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
      mqtt.subscribe(kFanCommandTopic.c_str(), 1);
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

  String device = doc["device"].as<String>();
  if (device.isEmpty()) {
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

}  // namespace