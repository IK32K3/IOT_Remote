#include "devices/FanController.h"

#include <Arduino.h>
#include <algorithm>
#include <cstdlib>
#include <IRutils.h>
#include <vector>

namespace {
template <typename T>
auto tryBegin(T &obj, int) -> decltype(obj.begin(), void()) {
  obj.begin();
}

template <typename T>
void tryBegin(T &, ...) {}

constexpr uint8_t kMaxSpeed = 5;
const char *const kFanTypes[] = {"normal", "natural", "sleep"};
constexpr uint8_t kFanTypeCount = sizeof(kFanTypes) / sizeof(kFanTypes[0]);

constexpr uint16_t kTimerOptions[] = {0, 60, 120, 240};
constexpr uint8_t kTimerOptionCount = sizeof(kTimerOptions) / sizeof(kTimerOptions[0]);

const FanController::KeyCommand kLgCommands[] = {
    {"POWER", decode_type_t::NEC, 0x20DF10EF, 32},
    {"TIMER", decode_type_t::NEC, 0x20DF906F, 32},
    {"SPEED_UP", decode_type_t::NEC, 0x20DF40BF, 32},
    {"SPEED_DOWN", decode_type_t::NEC, 0x20DFC03F, 32},
    {"SWING", decode_type_t::NEC, 0x20DF0CF3, 32},
    {"TYPE", decode_type_t::NEC, 0x20DF22DD, 32},
};

const FanController::KeyCommand kPanasonicCommands[] = {
    {"POWER", decode_type_t::PANASONIC, 0x400401UL, 48},
    {"TIMER", decode_type_t::PANASONIC, 0x400409UL, 48},
    {"SPEED_UP", decode_type_t::PANASONIC, 0x400405UL, 48},
    {"SPEED_DOWN", decode_type_t::PANASONIC, 0x400406UL, 48},
    {"SWING", decode_type_t::PANASONIC, 0x400408UL, 48},
    {"TYPE", decode_type_t::PANASONIC, 0x400407UL, 48},
};

const FanController::KeyCommand kMitsubishiCommands[] = {
    {"POWER", decode_type_t::MITSUBISHI, 0x11090B, 24},
    {"TIMER", decode_type_t::MITSUBISHI, 0x11090E, 24},
    {"SPEED_UP", decode_type_t::MITSUBISHI, 0x110902, 24},
    {"SPEED_DOWN", decode_type_t::MITSUBISHI, 0x110906, 24},
    {"SWING", decode_type_t::MITSUBISHI, 0x110904, 24},
    {"TYPE", decode_type_t::MITSUBISHI, 0x110908, 24},
};

const FanController::KeyCommand kSamsungCommands[] = {
    {"POWER", decode_type_t::SAMSUNG, 0x707, 12},
    {"TIMER", decode_type_t::SAMSUNG, 0x70F, 12},
    {"SPEED_UP", decode_type_t::SAMSUNG, 0x702, 12},
    {"SPEED_DOWN", decode_type_t::SAMSUNG, 0x706, 12},
    {"SWING", decode_type_t::SAMSUNG, 0x704, 12},
    {"TYPE", decode_type_t::SAMSUNG, 0x708, 12},
};

const FanController::KeyCommand kSharpCommands[] = {
    {"POWER", decode_type_t::SHARP, 0x5DA2, 15},
    {"TIMER", decode_type_t::SHARP, 0x5DA0, 15},
    {"SPEED_UP", decode_type_t::SHARP, 0x5DA8, 15},
    {"SPEED_DOWN", decode_type_t::SHARP, 0x5DA4, 15},
    {"SWING", decode_type_t::SHARP, 0x5DA6, 15},
    {"TYPE", decode_type_t::SHARP, 0x5DAE, 15},
};

const FanController::KeyCommand kToshibaCommands[] = {
    {"POWER", decode_type_t::NEC, 0x2FD48B7, 32},
    {"TIMER", decode_type_t::NEC, 0x2FD40BF, 32},
    {"SPEED_UP", decode_type_t::NEC, 0x2FD00FF, 32},
    {"SPEED_DOWN", decode_type_t::NEC, 0x2FD807F, 32},
    {"SWING", decode_type_t::NEC, 0x2FD609F, 32},
    {"TYPE", decode_type_t::NEC, 0x2FD20DF, 32},
};

const FanController::KeyCommand kGenericCommands[] = {
    {"POWER", decode_type_t::NEC, 0x00FF00FF, 32},
    {"TIMER", decode_type_t::NEC, 0x00FF807F, 32},
    {"SPEED_UP", decode_type_t::NEC, 0x00FF40BF, 32},
    {"SPEED_DOWN", decode_type_t::NEC, 0x00FFC03F, 32},
    {"SWING", decode_type_t::NEC, 0x00FF20DF, 32},
    {"TYPE", decode_type_t::NEC, 0x00FFA05F, 32},
};

}  // namespace

const FanController::RemoteConfig FanController::kRemotes[] = {
    // Curated codesets (index=1) per brand.
    {"LG", "FAN", 1, kLgCommands,
     sizeof(kLgCommands) / sizeof(kLgCommands[0])},
    {"Panasonic", "FAN", 1, kPanasonicCommands,
     sizeof(kPanasonicCommands) / sizeof(kPanasonicCommands[0])},
    {"Mitsubishi", "FAN", 1, kMitsubishiCommands,
     sizeof(kMitsubishiCommands) / sizeof(kMitsubishiCommands[0])},
    {"Samsung", "FAN", 1, kSamsungCommands,
     sizeof(kSamsungCommands) / sizeof(kSamsungCommands[0])},
    {"Sharp", "FAN", 1, kSharpCommands,
     sizeof(kSharpCommands) / sizeof(kSharpCommands[0])},
    {"Toshiba", "FAN", 1, kToshibaCommands,
     sizeof(kToshibaCommands) / sizeof(kToshibaCommands[0])},

    // "Try list" models (for brands without a curated codeset, or if index=1 doesn't work).
    // Kept as indexes 1..7 so CodeSetTest can iterate 1..10 and users can try alternatives.
    // Index=1 is a generic NEC fallback for brands that don't have a curated set.
    {"", "FAN", 1, kGenericCommands,
     sizeof(kGenericCommands) / sizeof(kGenericCommands[0])},
    {"", "FAN", 2, kLgCommands, sizeof(kLgCommands) / sizeof(kLgCommands[0])},
    {"", "FAN", 3, kPanasonicCommands,
     sizeof(kPanasonicCommands) / sizeof(kPanasonicCommands[0])},
    {"", "FAN", 4, kMitsubishiCommands,
     sizeof(kMitsubishiCommands) / sizeof(kMitsubishiCommands[0])},
    {"", "FAN", 5, kSamsungCommands,
     sizeof(kSamsungCommands) / sizeof(kSamsungCommands[0])},
    {"", "FAN", 6, kSharpCommands,
     sizeof(kSharpCommands) / sizeof(kSharpCommands[0])},
    {"", "FAN", 7, kToshibaCommands,
     sizeof(kToshibaCommands) / sizeof(kToshibaCommands[0])},
};

FanController::FanController(const char *nodeId, uint8_t irPin)
    : stateTopic_(String("iot/nodes/") + nodeId + "/fan/state"),
      irPin_(irPin),
      irSend_(irPin) {}

void FanController::begin() {
  if (!irReady_) {
    tryBegin(irSend_, 0);
    irReady_ = true;
  }
  Serial.printf("[FAN] Controller ready (IR pin=%u)\n", irPin_);
}

void FanController::serializeState(JsonDocument &doc) const {
  doc["device"] = deviceType();
  doc["power"] = state_.power;
  doc["speed"] = state_.speed;
  doc["swing"] = state_.swing;
  doc["type"] = state_.type;
  doc["timer"] = state_.timer;
  doc["brand"] = remoteBrand_;
  doc["profileType"] = remoteType_;
  doc["index"] = remoteIndex_;
  doc["updatedAt"] = millis();
}

bool FanController::handleCommand(JsonObjectConst cmd, JsonDocument &stateDoc) {
  if (cmd["brand"].is<const char *>())
    remoteBrand_ = cmd["brand"].as<const char *>();
  if (cmd["type"].is<const char *>())
    remoteType_ = cmd["type"].as<const char *>();
  if (cmd["index"].is<uint16_t>())
    remoteIndex_ = cmd["index"].as<uint16_t>();

  String action = cmd["cmd"].as<String>();
  if (action.isEmpty()) {
    Serial.println(F("[FAN] Missing command name"));
    return false;
  }

  bool updated = false;

  if (action.equalsIgnoreCase("key")) {
    const String key = cmd["key"].as<String>();
    if (key.isEmpty()) {
      Serial.println(F("[FAN] Missing key name"));
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
          std::vector<uint8_t> raw;
          const size_t nbytes = (bits + 7) / 8;
          raw.reserve(nbytes);
          String hexStr(codeStr);
          while (hexStr.length() < nbytes * 2) {
            hexStr = String('0') + hexStr;
          }
          for (size_t i = 0; i + 1 < hexStr.length(); i += 2) {
            char buf[3] = {hexStr[i], hexStr[i + 1], '\0'};
            raw.push_back(static_cast<uint8_t>(strtoul(buf, nullptr, 16)));
          }
          saveLearnedCommand(key, protocol, value, bits, raw);
        }
      }
    }

    updated = applyKeyEffects(key);
    if (!sendKey(key)) {
      Serial.printf("[FAN][IR] No IR mapping for brand=%s key=%s\n",
                    remoteBrand_.c_str(), key.c_str());
    }
  } else if (action.equalsIgnoreCase("set")) {
    if (cmd["power"].is<bool>()) {
      state_.power = cmd["power"].as<bool>();
      updated = true;
    }
    if (cmd["speed"].is<int>()) {
      int value = cmd["speed"].as<int>();
      value = std::max(0, std::min<int>(kMaxSpeed, value));
      state_.speed = static_cast<uint8_t>(value);
      updated = true;
    }
    if (cmd["swing"].is<bool>()) {
      state_.swing = cmd["swing"].as<bool>();
      updated = true;
    }
    if (cmd["type"].is<const char *>()) {
      state_.type = cmd["type"].as<const char *>();
      for (uint8_t i = 0; i < kFanTypeCount; ++i) {
        if (state_.type.equalsIgnoreCase(kFanTypes[i])) {
          typeIndex_ = i;
          break;
        }
      }
      updated = true;
    }
    if (cmd["timer"].is<int>()) {
      int value = cmd["timer"].as<int>();
      value = std::max(0, value);
      state_.timer = static_cast<uint16_t>(value);
      updated = true;
    }
  }

  if (!updated) {
    return false;
  }

  return applyState(stateDoc);
}

bool FanController::sendKey(const String &key) {
  if (sendLearnedKey(key)) {
    return true;
  }

  const RemoteConfig *remote =
      findRemote(remoteBrand_, remoteType_, remoteIndex_);
  if (remote == nullptr) {
    return false;
  }
  const KeyCommand *cmd = findKey(remote, key);
  if (cmd == nullptr) {
    return false;
  }
  irSend_.send(cmd->protocol, cmd->value, cmd->nbits);
  Serial.printf("[FAN][IR] Sent key=%s protocol=%d value=0x%llX bits=%u\n",
                key.c_str(), static_cast<int>(cmd->protocol),
                static_cast<unsigned long long>(cmd->value), cmd->nbits);
  return true;
}

bool FanController::learnKey(const String &key, decode_type_t protocol,
                             uint64_t value, uint16_t nbits,
                             const std::vector<uint8_t> &raw) {
  if (key.length() == 0 || protocol == decode_type_t::UNKNOWN || nbits == 0) {
    return false;
  }
  saveLearnedCommand(key, protocol, value, nbits, raw);
  return true;
}

void FanController::saveLearnedCommand(const String &key, decode_type_t protocol,
                                       uint64_t value, uint16_t nbits,
                                       const std::vector<uint8_t> &raw) {
  for (auto &entry : learnedCommands_) {
    if (key.equalsIgnoreCase(entry.key)) {
      entry.protocol = protocol;
      entry.value = value;
      entry.nbits = nbits;
      entry.raw = raw;
      return;
    }
  }

  LearnedCommand cmd;
  cmd.key = key;
  cmd.protocol = protocol;
  cmd.value = value;
  cmd.nbits = nbits;
  cmd.raw = raw;
  learnedCommands_.push_back(cmd);
}

bool FanController::sendLearnedKey(const String &key) {
  for (const auto &entry : learnedCommands_) {
    if (key.equalsIgnoreCase(entry.key)) {
      if (!entry.raw.empty() && entry.nbits > 64) {
        irSend_.send(entry.protocol, entry.raw.data(),
                     static_cast<uint16_t>(entry.raw.size()));
      } else {
        irSend_.send(entry.protocol, entry.value, entry.nbits);
      }
      Serial.printf(
          "[FAN][IR] Sent learned key=%s protocol=%d value=0x%llX bits=%u\n",
          key.c_str(), static_cast<int>(entry.protocol),
          static_cast<unsigned long long>(entry.value), entry.nbits);
      return true;
    }
  }
  return false;
}


bool FanController::applyKeyEffects(const String &key) {
  bool changed = false;
  if (key.equalsIgnoreCase("POWER")) {
    state_.power = !state_.power;
    changed = true;
  } else if (key.equalsIgnoreCase("SPEED_UP")) {
    if (state_.speed < kMaxSpeed) {
      state_.speed++;
      changed = true;
    }
  } else if (key.equalsIgnoreCase("SPEED_DOWN")) {
    if (state_.speed > 0) {
      state_.speed--;
      changed = true;
    }
  } else if (key.equalsIgnoreCase("SWING")) {
    state_.swing = !state_.swing;
    changed = true;
  } else if (key.equalsIgnoreCase("TYPE")) {
    typeIndex_ = (typeIndex_ + 1) % kFanTypeCount;
    state_.type = kFanTypes[typeIndex_];
    changed = true;
  } else if (key.equalsIgnoreCase("TIMER")) {
    const uint16_t current = state_.timer;
    uint8_t index = 0;
    for (; index < kTimerOptionCount; ++index) {
      if (kTimerOptions[index] == current) {
        break;
      }
    }
    index = (index + 1) % kTimerOptionCount;
    state_.timer = kTimerOptions[index];
    changed = true;
  }
  return changed;
}

bool FanController::applyState(JsonDocument &stateDoc) {
  // Fan: operate statelessly (no MQTT state publish)
  stateDoc.clear();
  return false;
}

const FanController::RemoteConfig *FanController::findRemote(
    const String &brand, const String &type, uint16_t index) {
  const RemoteConfig *fallback = nullptr;
  for (const auto &remote : kRemotes) {
    bool brandMatch = brand.length() == 0 ||
                      remote.brand == nullptr || remote.brand[0] == '\0' ||
                      brand.equalsIgnoreCase(remote.brand);
    bool typeMatch = remote.type == nullptr || remote.type[0] == '\0' ||
                     type.equalsIgnoreCase(remote.type);
    bool indexMatch = remote.index == 0 || index == 0 || remote.index == index;
    if (brandMatch && typeMatch && indexMatch) {
      if (fallback == nullptr) fallback = &remote;
      if ((remote.brand != nullptr && brand.equalsIgnoreCase(remote.brand)) ||
          (remote.index != 0 && remote.index == index)) {
        return &remote;
      }
    }
  }
  return fallback;
}

const FanController::KeyCommand *FanController::findKey(
    const RemoteConfig *remote, const String &key) {
  if (remote == nullptr) return nullptr;
  for (size_t i = 0; i < remote->commandCount; ++i) {
    if (key.equalsIgnoreCase(remote->commands[i].key)) {
      return &remote->commands[i];
    }
  }
  return nullptr;
  
}
