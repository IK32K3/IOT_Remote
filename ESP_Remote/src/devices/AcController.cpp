#include "devices/AcController.h"

#include <cstdlib>
#include <cstring>
#include <IRutils.h>

namespace {
#if !AC_CONTROLLER_HAS_REMOTE_MODEL_ENUM
constexpr uint16_t kDaikinBase = 0x0100;
constexpr uint16_t kLgBase = 0x0200;
constexpr uint16_t kMitsubishiBase = 0x0300;
constexpr uint16_t kPanasonicBase = 0x0400;
constexpr uint16_t kSamsungBase = 0x0500;
constexpr uint16_t kSharpBase = 0x0600;
constexpr uint16_t kSonyBase = 0x0700;
constexpr uint16_t kTclBase = 0x0800;
constexpr uint16_t kAquaBase = 0x0900;
#endif

template <typename T>
auto tryBegin(T &obj, int) -> decltype(obj.begin(), void()) {
  obj.begin();
}

template <typename T>
void tryBegin(T &, ...) {}
}  // namespace

#if AC_CONTROLLER_HAS_REMOTE_MODEL_ENUM
#define AC_REMOTE_MODEL(symbol, fallback) stdAc::ac_remote_model_t::symbol
#else
#define AC_REMOTE_MODEL(symbol, fallback) fallback
#endif

const AcController::IrModelConfig AcController::kModels[] = {
    {"Daikin", "ARC480A1", decode_type_t::DAIKIN,
     AC_REMOTE_MODEL(kDaikinARC480A1, kDaikinBase + 0x01), 1},
    {"Daikin", "ARC423A5", decode_type_t::DAIKIN,
     AC_REMOTE_MODEL(kDaikin2, kDaikinBase + 0x02), 2},
    {"Daikin", "ARC433A46", decode_type_t::DAIKIN,
     AC_REMOTE_MODEL(kDaikin216, kDaikinBase + 0x03), 3},
    {"Daikin", "ARC433A70", decode_type_t::DAIKIN,
     AC_REMOTE_MODEL(kDaikin160, kDaikinBase + 0x04), 4},
    {"Daikin", "ARC452A1", decode_type_t::DAIKIN,
     AC_REMOTE_MODEL(kDaikin176, kDaikinBase + 0x05), 5},
    {"Daikin", "ARC452A2", decode_type_t::DAIKIN,
     AC_REMOTE_MODEL(kDaikin32, kDaikinBase + 0x06), 6},
    {"Daikin", "ARC466A21", decode_type_t::DAIKIN,
     AC_REMOTE_MODEL(kDaikin64, kDaikinBase + 0x07), 7},
    {"Daikin", "ARC480A5", decode_type_t::DAIKIN,
     AC_REMOTE_MODEL(kDaikin152, kDaikinBase + 0x08), 8},
    {"Daikin", "ARC433A1", decode_type_t::DAIKIN,
     AC_REMOTE_MODEL(kDaikin3, kDaikinBase + 0x09), 9},
    {"Daikin", "ARC433A18", decode_type_t::DAIKIN,
     AC_REMOTE_MODEL(kDaikin64_2, kDaikinBase + 0x0A), 10},
    {"Daikin", "ARC477A1", decode_type_t::DAIKIN,
     AC_REMOTE_MODEL(kDaikin128, kDaikinBase + 0x0B), 11},
    {"LG", "AKB74955603", decode_type_t::LG,
     AC_REMOTE_MODEL(kLg, kLgBase + 0x01), 1},
    {"LG", "AKB74955604", decode_type_t::LG,
     AC_REMOTE_MODEL(kLg2, kLgBase + 0x02), 2},
    {"Mitsubishi", "MSZ-GL", decode_type_t::MITSUBISHI_AC,
     AC_REMOTE_MODEL(kMitsubishi, kMitsubishiBase + 0x01), 1},
    {"Mitsubishi", "MSZ-GE", decode_type_t::MITSUBISHI_AC,
     AC_REMOTE_MODEL(kMitsubishi112, kMitsubishiBase + 0x02), 2},
    {"Mitsubishi", "MSZ-EF", decode_type_t::MITSUBISHI_AC,
     AC_REMOTE_MODEL(kMitsubishi136, kMitsubishiBase + 0x03), 3},
    {"Mitsubishi", "Heavy-88", decode_type_t::MITSUBISHI_HEAVY_88,
     AC_REMOTE_MODEL(kMitsubishiHeavy88, kMitsubishiBase + 0x04), 4},
    {"Mitsubishi", "Heavy-152", decode_type_t::MITSUBISHI_HEAVY_152,
     AC_REMOTE_MODEL(kMitsubishiHeavy152, kMitsubishiBase + 0x05), 5},
    {"Panasonic", "A75C3747", decode_type_t::PANASONIC_AC,
     AC_REMOTE_MODEL(kPanasonic, kPanasonicBase + 0x01), 1},
    {"Panasonic", "A75C3748", decode_type_t::PANASONIC_AC,
     AC_REMOTE_MODEL(kPanasonic2, kPanasonicBase + 0x02), 2},
    {"Panasonic", "CZ-RD514C", decode_type_t::PANASONIC_AC,
     AC_REMOTE_MODEL(kPanasonicJke, kPanasonicBase + 0x03), 3},
    {"Panasonic", "CZ-T056", decode_type_t::PANASONIC_AC,
     AC_REMOTE_MODEL(kPanasonicLke, kPanasonicBase + 0x04), 4},
    {"Samsung", "DB93-14871C", decode_type_t::SAMSUNG_AC,
     AC_REMOTE_MODEL(kSamsung, kSamsungBase + 0x01), 1},
    {"Samsung", "DB93-14974C", decode_type_t::SAMSUNG_AC,
     AC_REMOTE_MODEL(kSamsung12, kSamsungBase + 0x02), 2},
    {"Samsung", "DB93-16223A", decode_type_t::SAMSUNG_AC,
     AC_REMOTE_MODEL(kSamsung36, kSamsungBase + 0x03), 3},
    {"Samsung", "DB93-11489L", decode_type_t::SAMSUNG_AC,
     AC_REMOTE_MODEL(kSamsungDB93, kSamsungBase + 0x04), 4},
    {"Sharp", "A903JB", decode_type_t::SHARP_AC,
     AC_REMOTE_MODEL(kSharp, kSharpBase + 0x01), 1},
    {"Sharp", "CRMC-A843JBEZ", decode_type_t::SHARP_AC,
     AC_REMOTE_MODEL(kSharpAc, kSharpBase + 0x02), 2},
    {"Sony", "RM-AC001", decode_type_t::SONY,
     AC_REMOTE_MODEL(kSony, kSonyBase + 0x01), 1},
    {"TCL", "GZ-1002B", decode_type_t::TCL112AC,
     AC_REMOTE_MODEL(kTcl, kTclBase + 0x01), 1},
    {"TCL", "GZ-1002A", decode_type_t::TCL112AC,
     AC_REMOTE_MODEL(kTcl112Ac, kTclBase + 0x02), 2},
    {"Aqua", "AQV-RD", decode_type_t::COOLIX,
     AC_REMOTE_MODEL(kAqua, kAquaBase + 0x01), 1},
};

#undef AC_REMOTE_MODEL

AcController::AcController(const char *nodeId, uint8_t irPin)
    : stateTopic_(String("iot/nodes/") + nodeId + "/ac/state"),
      irPin_(irPin),
      irAc_(irPin),
      irSend_(irPin) {}

void AcController::begin() {
  if (!irReady_) {
    tryBegin(irAc_, 0);
    tryBegin(irSend_, 0);
    irReady_ = true;
  }

  Serial.printf("[AC] Controller ready (IR pin=%u)\n", irPin_);
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
  if (cmd["brand"].is<const char *>())
    remote_.brand = cmd["brand"].as<const char *>();
  if (cmd["type"].is<const char *>())
    remote_.type = cmd["type"].as<const char *>();
  if (cmd["index"].is<uint16_t>())
    remote_.index = cmd["index"].as<uint16_t>();

  String command = cmd["cmd"].as<String>();
  if (command.isEmpty()) {
    Serial.println(F("[AC] Missing command name"));
    return false;
  }
  if (command.equalsIgnoreCase("key")) {
    const String key = cmd["key"].as<String>();
    if (key.isEmpty()) {
      Serial.println(F("[AC] Missing key name"));
      return false;
    }

    JsonObjectConst learnedIr = cmd["ir"].as<JsonObjectConst>();
    if (!learnedIr.isNull()) {
      const char *protoStr = learnedIr["protocol"].as<const char *>();
      const char *codeStr = learnedIr["code"].as<const char *>();
      const uint16_t bits = learnedIr["bits"].as<uint16_t>();
      if (protoStr != nullptr && codeStr != nullptr && bits > 0) {
        const decode_type_t protocol = strToDecodeType(protoStr);
        if (protocol != decode_type_t::UNKNOWN) {
          const uint64_t value = strtoull(codeStr, nullptr, 16);
          saveLearnedCommand(key, protocol, value, bits);
        }
      }
    }

    if (!sendLearnedKey(key)) {
      Serial.printf("[AC][IR] No learned mapping for key=%s\n", key.c_str());
      return false;
    }

    stateDoc.clear();
    serializeState(stateDoc);
    return true;
  }
  bool stateChanged = false;

  if (command.equalsIgnoreCase("power")) {
    if (cmd["value"].is<bool>()) {
      state_.power = cmd["value"].as<bool>();
      stateChanged = true;
    }
  } else if (command.equalsIgnoreCase("toggle")) {
    state_.power = !state_.power;
    stateChanged = true;
  } else if (command.equalsIgnoreCase("set")) {
    if (cmd["power"].is<bool>())
      state_.power = cmd["power"].as<bool>();
    if (cmd["mode"].is<const char *>())
      state_.mode = cmd["mode"].as<const char *>();
    if (cmd["temp"].is<int>())
      state_.temp = cmd["temp"].as<int>();
    if (cmd["fan"].is<const char *>())
      state_.fan = cmd["fan"].as<const char *>();
    if (cmd["swing"].is<bool>())
      state_.swing = cmd["swing"].as<bool>();
    stateChanged = true;
  } else if (command.equalsIgnoreCase("temp")) {
    if (cmd["value"].is<int>()) {
      state_.temp = cmd["value"].as<int>();
      stateChanged = true;
    }
  } else if (command.equalsIgnoreCase("mode")) {
    if (cmd["value"].is<const char *>()) {
      state_.mode = cmd["value"].as<const char *>();
      stateChanged = true;
    }
  } else if (command.equalsIgnoreCase("fan")) {
    if (cmd["value"].is<const char *>()) {
      state_.fan = cmd["value"].as<const char *>();
      stateChanged = true;
    }
  } else if (command.equalsIgnoreCase("swing")) {
    if (cmd["value"].is<bool>()) {
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
  const IrModelConfig *model =
      findModel(remote_.brand, remote_.type, remote_.index);
  if (model == nullptr) {
    Serial.printf("[AC][IR] No IR model for brand=%s type=%s index=%u\n",
                  remote_.brand.c_str(), remote_.type.c_str(), remote_.index);
  } else {
    remote_.brand = model->brand;
    if (remote_.type.length() == 0 && model->type != nullptr)
      remote_.type = model->type;
    if (remote_.index == 0) remote_.index = model->index;
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

void AcController::saveLearnedCommand(const String &key, decode_type_t protocol,
                                      uint64_t value, uint16_t nbits) {
  for (auto &entry : learnedCommands_) {
    if (key.equalsIgnoreCase(entry.key)) {
      entry.protocol = protocol;
      entry.value = value;
      entry.nbits = nbits;
      return;
    }
  }

  LearnedCommand cmd;
  cmd.key = key;
  cmd.protocol = protocol;
  cmd.value = value;
  cmd.nbits = nbits;
  learnedCommands_.push_back(cmd);
}

bool AcController::sendLearnedKey(const String &key) {
  for (const auto &entry : learnedCommands_) {
    if (key.equalsIgnoreCase(entry.key)) {
      irSend_.send(entry.protocol, entry.value, entry.nbits);
      Serial.printf("[AC][IR] Sent learned key=%s protocol=%d value=0x%llX bits=%u\n",
                    key.c_str(), static_cast<int>(entry.protocol),
                    static_cast<unsigned long long>(entry.value), entry.nbits);
      return true;
    }
  }
  return false;
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
    bool typeMatch = entry.type == nullptr || entry.type[0] == '\0';
    if (!typeMatch) {
      if (type.length() == 0 || type.equalsIgnoreCase("ac")) {
        typeMatch = true;
      } else {
        typeMatch = type.equalsIgnoreCase(entry.type);
      }
    }
    if (brandMatch && typeMatch) {
      if (fallback == nullptr) fallback = &entry;
      if (index == 0 || entry.index == index) {
        return &entry;
      }
    }
  }
  return fallback;