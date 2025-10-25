#include "devices/AcController.h"

#include <cstring>

namespace {
template <typename T>
auto tryBegin(T &obj, int) -> decltype(obj.begin(), void()) {
  obj.begin();
}

template <typename T>
void tryBegin(T &, ...) {}
}  // namespace

const AcController::IrModelConfig AcController::kModels[] = {
    {"Daikin", "FTKQ", decode_type_t::DAIKIN, 1},
    {"Daikin", "", decode_type_t::DAIKIN, 1},
    {"LG", "", decode_type_t::LG, 1},
    {"Mitsubishi", "", decode_type_t::MITSUBISHI_AC, 1},
    {"Panasonic", "", decode_type_t::PANASONIC_AC, 1},
    {"Samsung", "", decode_type_t::SAMSUNG_AC, 1},
    {"Sharp", "", decode_type_t::SHARP_AC, 1},
    {"Sony", "", decode_type_t::SONY, 1},
    {"TCL", "", decode_type_t::TCL112AC, 1},
};

AcController::AcController(const char *nodeId, uint8_t irPin, uint8_t relayPin)
    : stateTopic_(String("iot/nodes/") + nodeId + "/ac/state"),
      irPin_(irPin),
      relayPin_(relayPin),
      irAc_(irPin) {}

void AcController::begin() {
  pinMode(relayPin_, OUTPUT);
  digitalWrite(relayPin_, LOW);

  if (!irReady_) {
    tryBegin(irAc_, 0);
    irReady_ = true;
  }

  Serial.printf("[AC] Controller ready (IR pin=%u, relay pin=%u)\n", irPin_,
                relayPin_);
}

void AcController::serializeState(JsonDocument &doc) const {
  doc["device"] = deviceType();
  doc["power"] = state_.power;
  doc["mode"] = state_.mode;
  doc["temp"] = state_.temp;
  doc["fan"] = state_.fan;
  doc["swing"] = state_.swing;
  doc["brand"] = remote_.brand;
  doc["type"] = remote_.type;
  doc["index"] = remote_.index;
  doc["updatedAt"] = millis();
}

bool AcController::handleCommand(JsonObjectConst cmd, JsonDocument &stateDoc) {
  if (cmd.containsKey("brand")) remote_.brand = cmd["brand"].as<const char*>();
  if (cmd.containsKey("type")) remote_.type = cmd["type"].as<const char*>();
  if (cmd.containsKey("index")) remote_.index = cmd["index"].as<uint16_t>();

  String command = cmd["cmd"].as<String>();
  if (command.isEmpty()) {
    Serial.println(F("[AC] Missing command name"));
    return false;
  }
  bool stateChanged = false;

  if (command.equalsIgnoreCase("power")) {
    if (cmd.containsKey("value")) {
      state_.power = cmd["value"].as<bool>();
      stateChanged = true;
    }
  } else if (command.equalsIgnoreCase("toggle")) {
    state_.power = !state_.power;
    stateChanged = true;
  } else if (command.equalsIgnoreCase("set")) {
    if (cmd.containsKey("power")) state_.power = cmd["power"].as<bool>();
    if (cmd.containsKey("mode")) state_.mode = cmd["mode"].as<const char*>();
    if (cmd.containsKey("temp")) state_.temp = cmd["temp"].as<int>();
    if (cmd.containsKey("fan")) state_.fan = cmd["fan"].as<const char*>();
    if (cmd.containsKey("swing")) state_.swing = cmd["swing"].as<bool>();
    stateChanged = true;
  } else if (command.equalsIgnoreCase("temp")) {
    if (cmd.containsKey("value")) {
      state_.temp = cmd["value"].as<int>();
      stateChanged = true;
    }
  } else if (command.equalsIgnoreCase("mode")) {
    if (cmd.containsKey("value")) {
      state_.mode = cmd["value"].as<const char*>();
      stateChanged = true;
    }
  } else if (command.equalsIgnoreCase("fan")) {
    if (cmd.containsKey("value")) {
      state_.fan = cmd["value"].as<const char*>();
      stateChanged = true;
    }
  } else if (command.equalsIgnoreCase("swing")) {
    if (cmd.containsKey("value")) {
      state_.swing = cmd["value"].as<bool>();
      stateChanged = true;
    }
  }

  if (!stateChanged) {
    return false;
  }

  return applyState(stateDoc);
}

bool AcController::applyState(JsonDocument &stateDoc) {
  digitalWrite(relayPin_, state_.power ? HIGH : LOW);

  const IrModelConfig *model =
      findModel(remote_.brand, remote_.type, remote_.index);
  if (model == nullptr) {
    Serial.printf("[AC][IR] No IR model for brand=%s type=%s index=%u\n",
                  remote_.brand.c_str(), remote_.type.c_str(), remote_.index);
  } else {
    irAc_.next.protocol = model->protocol;
    irAc_.next.model = model->model;
    irAc_.next.power = state_.power;
    irAc_.next.degrees = state_.temp;
    irAc_.next.celsius = true;
    irAc_.next.mode = parseMode(state_.mode);
    irAc_.next.fanspeed = parseFan(state_.fan);
    irAc_.next.swingv = parseSwing(state_.swing);
    irAc_.next.swingh = stdAc::swingh_t::kOff;
    // Some IRremoteESP8266 releases expose the light field as a private enum,
    // so skip forcing it on to keep compilation working across versions.
    irAc_.sendAc();
    Serial.println(F("[AC][IR] Command sent"));
  }

  stateDoc.clear();
  serializeState(stateDoc);
  return true;
}

const AcController::IrModelConfig *AcController::findModel(const String &brand,
                                                           const String &type,
                                                           uint16_t index) {
  if (brand.length() == 0) {
    return &kModels[0];
  }
  const IrModelConfig *fallback = nullptr;
  for (const auto &entry : kModels) {
    bool brandMatch = brand.equalsIgnoreCase(entry.brand);
    bool typeMatch = entry.type == nullptr || entry.type[0] == '\0' ||
                     type.equalsIgnoreCase(entry.type);
    if (brandMatch && typeMatch) {
      if (fallback == nullptr) fallback = &entry;
      if (index == 0 || entry.model == index) {
        return &entry;
      }
    }
  }
  return fallback;
}

stdAc::opmode_t AcController::parseMode(const String &mode) {
  if (mode.equalsIgnoreCase("heat")) return stdAc::opmode_t::kHeat;
  if (mode.equalsIgnoreCase("dry")) return stdAc::opmode_t::kDry;
  if (mode.equalsIgnoreCase("fan")) return stdAc::opmode_t::kFan;
  if (mode.equalsIgnoreCase("auto")) return stdAc::opmode_t::kAuto;
  return stdAc::opmode_t::kCool;
}

stdAc::fanspeed_t AcController::parseFan(const String &fan) {
  if (fan.equalsIgnoreCase("low")) return stdAc::fanspeed_t::kLow;
  if (fan.equalsIgnoreCase("med") || fan.equalsIgnoreCase("medium"))
    return stdAc::fanspeed_t::kMedium;
  if (fan.equalsIgnoreCase("high")) return stdAc::fanspeed_t::kHigh;
  if (fan.equalsIgnoreCase("max")) return stdAc::fanspeed_t::kMax;
  return stdAc::fanspeed_t::kAuto;
}

stdAc::swingv_t AcController::parseSwing(bool enabled) {
  return enabled ? stdAc::swingv_t::kAuto : stdAc::swingv_t::kOff;
}