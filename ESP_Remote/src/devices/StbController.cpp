#include "devices/StbController.h"

#include <Arduino.h>
#include <IRutils.h>
#include <algorithm>
#include <cstdlib>

namespace {
template <typename T>
auto tryBegin(T &obj, int) -> decltype(obj.begin(), void()) {
  obj.begin();
}

template <typename T>
void tryBegin(T &, ...) {}

constexpr uint16_t kChannelGapMs = 120;

String canonicalizeKey(const String &key) {
  String out = key;
  out.trim();
  out.toUpperCase();

  // Common aliases (helps keep ESP compatible if Android naming changes).
  if (out.equalsIgnoreCase("SOURCE") || out.equalsIgnoreCase("INPUT") ||
      out.equalsIgnoreCase("AV") || out.equalsIgnoreCase("TVAV")) {
    return "TV_AV";
  }
  if (out.equalsIgnoreCase("ENTER") || out.equalsIgnoreCase("SELECT") ||
      out.equalsIgnoreCase("CONFIRM")) {
    return "OK";
  }
  if (out.equalsIgnoreCase("RETURN")) {
    return "BACK";
  }
  if (out.equalsIgnoreCase("SETTINGS") || out.equalsIgnoreCase("OPTIONS")) {
    return "MENU";
  }
  if (out.equalsIgnoreCase("INFO") || out.equalsIgnoreCase("TOOLS") ||
      out.equalsIgnoreCase("MORE_INFO")) {
    return "MORE";
  }
  if (out.equalsIgnoreCase("CHANNEL_UP")) return "CH_UP";
  if (out.equalsIgnoreCase("CHANNEL_DOWN")) return "CH_DOWN";
  if (out.equalsIgnoreCase("VOLUME_UP")) return "VOL_UP";
  if (out.equalsIgnoreCase("VOLUME_DOWN")) return "VOL_DOWN";
  if (out.equalsIgnoreCase("PAGEUP")) return "PAGE_UP";
  if (out.equalsIgnoreCase("PAGEDOWN")) return "PAGE_DOWN";

  return out;
}

// Samsung STB codeset #1 (NEC 32-bit, BN59-00603A-STB)
const StbController::KeyCommand kSamsungStbCommands1[] = {
    {"POWER", decode_type_t::NEC, 0x909040BF, 32},
    {"MUTE", decode_type_t::NEC, 0xE0E0F00F, 32},
    {"TV_AV", decode_type_t::NEC, 0x9090807F, 32},  // KEY_CYCLEWINDOWS
    {"VOL_UP", decode_type_t::NEC, 0xE0E0E01F, 32},
    {"VOL_DOWN", decode_type_t::NEC, 0xE0E0D02F, 32},
    {"CH_UP", decode_type_t::NEC, 0x909048B7, 32},
    {"CH_DOWN", decode_type_t::NEC, 0x909008F7, 32},
    // Some STB remotes use page +/- as channel +/-.
    {"PAGE_UP", decode_type_t::NEC, 0x909048B7, 32},
    {"PAGE_DOWN", decode_type_t::NEC, 0x909008F7, 32},
    {"MENU", decode_type_t::NEC, 0x909058A7, 32},
    {"EXIT", decode_type_t::NEC, 0x9090B44B, 32},
    {"UP", decode_type_t::NEC, 0x909006F9, 32},
    {"DOWN", decode_type_t::NEC, 0x90908679, 32},
    {"LEFT", decode_type_t::NEC, 0x9090A659, 32},
    {"RIGHT", decode_type_t::NEC, 0x909046B9, 32},
    {"OK", decode_type_t::NEC, 0x909016E9, 32},    // Enter/OK
    {"BACK", decode_type_t::NEC, 0x9090C837, 32},  // Pre-CH
    {"MORE", decode_type_t::NEC, 0x9090F20D, 32},  // KEY_INFO
    {"DIGIT_0", decode_type_t::NEC, 0x90908877, 32},
    {"DIGIT_1", decode_type_t::NEC, 0x909020DF, 32},
    {"DIGIT_2", decode_type_t::NEC, 0x9090A05F, 32},
    {"DIGIT_3", decode_type_t::NEC, 0x9090609F, 32},
    {"DIGIT_4", decode_type_t::NEC, 0x909010EF, 32},
    {"DIGIT_5", decode_type_t::NEC, 0x9090906F, 32},
    {"DIGIT_6", decode_type_t::NEC, 0x909050AF, 32},
    {"DIGIT_7", decode_type_t::NEC, 0x909030CF, 32},
    {"DIGIT_8", decode_type_t::NEC, 0x9090B04F, 32},
    {"DIGIT_9", decode_type_t::NEC, 0x9090708F, 32},
};

// Generic Cable Box (IRDB: Comcast/Cable Box/0,-1.csv) - G.I.Cable 16-bit.
// For GICABLE, IRDB's "function" maps directly to the 16-bit payload.
const StbController::KeyCommand kGicableStbCommands1[] = {
    {"POWER", decode_type_t::GICABLE, 10, 16},
    {"CH_UP", decode_type_t::GICABLE, 11, 16},
    {"CH_DOWN", decode_type_t::GICABLE, 12, 16},
    {"PAGE_UP", decode_type_t::GICABLE, 58, 16},
    {"PAGE_DOWN", decode_type_t::GICABLE, 59, 16},
    {"MENU", decode_type_t::GICABLE, 25, 16},
    {"EXIT", decode_type_t::GICABLE, 18, 16},
    {"BACK", decode_type_t::GICABLE, 19, 16},  // LAST
    {"MORE", decode_type_t::GICABLE, 51, 16},  // INFO
    {"UP", decode_type_t::GICABLE, 52, 16},
    {"DOWN", decode_type_t::GICABLE, 53, 16},
    {"LEFT", decode_type_t::GICABLE, 54, 16},
    {"RIGHT", decode_type_t::GICABLE, 55, 16},
    {"OK", decode_type_t::GICABLE, 17, 16},  // OK/SELECT
    {"DIGIT_0", decode_type_t::GICABLE, 0, 16},
    {"DIGIT_1", decode_type_t::GICABLE, 1, 16},
    {"DIGIT_2", decode_type_t::GICABLE, 2, 16},
    {"DIGIT_3", decode_type_t::GICABLE, 3, 16},
    {"DIGIT_4", decode_type_t::GICABLE, 4, 16},
    {"DIGIT_5", decode_type_t::GICABLE, 5, 16},
    {"DIGIT_6", decode_type_t::GICABLE, 6, 16},
    {"DIGIT_7", decode_type_t::GICABLE, 7, 16},
    {"DIGIT_8", decode_type_t::GICABLE, 8, 16},
    {"DIGIT_9", decode_type_t::GICABLE, 9, 16},
};

const StbController::KeyCommand kGenericStbCommands[] = {};

const StbController::RemoteConfig kStbRemotes[] = {
    {"", "", 0, kGenericStbCommands,
     sizeof(kGenericStbCommands) / sizeof(kGenericStbCommands[0])},

    {"Samsung", "STB", 1, kSamsungStbCommands1,
     sizeof(kSamsungStbCommands1) / sizeof(kSamsungStbCommands1[0])},

    // Cable Box family (G.I.Cable)
    {"Comcast", "STB", 1, kGicableStbCommands1,
     sizeof(kGicableStbCommands1) / sizeof(kGicableStbCommands1[0])},
    {"Motorola", "STB", 1, kGicableStbCommands1,
     sizeof(kGicableStbCommands1) / sizeof(kGicableStbCommands1[0])},
    {"General Instrument", "STB", 1, kGicableStbCommands1,
     sizeof(kGicableStbCommands1) / sizeof(kGicableStbCommands1[0])},
    {"Jerrold", "STB", 1, kGicableStbCommands1,
     sizeof(kGicableStbCommands1) / sizeof(kGicableStbCommands1[0])},
    {"Zinwell", "STB", 1, kGicableStbCommands1,
     sizeof(kGicableStbCommands1) / sizeof(kGicableStbCommands1[0])},
    {"Novaplex", "STB", 1, kGicableStbCommands1,
     sizeof(kGicableStbCommands1) / sizeof(kGicableStbCommands1[0])},

    // Generic "try list" for brands without a curated codeset.
    {"", "STB", 3001, kSamsungStbCommands1,
     sizeof(kSamsungStbCommands1) / sizeof(kSamsungStbCommands1[0])},
    {"", "STB", 3002, kGicableStbCommands1,
     sizeof(kGicableStbCommands1) / sizeof(kGicableStbCommands1[0])},
};
}  // namespace

const StbController::RemoteConfig StbController::kRemotes[] = {
    kStbRemotes[0],
    kStbRemotes[1],
    kStbRemotes[2],
    kStbRemotes[3],
    kStbRemotes[4],
    kStbRemotes[5],
    kStbRemotes[6],
    kStbRemotes[7],
    kStbRemotes[8],
    kStbRemotes[9],
};

StbController::StbController(const char *nodeId, uint8_t irPin)
    : stateTopic_(String("iot/nodes/") + nodeId + "/stb/state"),
      irPin_(irPin),
      irSend_(irPin) {}

void StbController::begin() {
  if (!irReady_) {
    tryBegin(irSend_, 0);
    irReady_ = true;
  }
  Serial.printf("[STB] Controller ready (IR pin=%u)\n", irPin_);
}

void StbController::serializeState(JsonDocument &doc) const {
  doc["device"] = deviceType();
  doc["power"] = state_.power;
  doc["muted"] = state_.muted;
  doc["channel"] = state_.channel;
  doc["brand"] = remoteBrand_;
  doc["type"] = remoteType_;
  doc["index"] = remoteIndex_;
  doc["updatedAt"] = millis();
}

bool StbController::handleCommand(JsonObjectConst cmd,
                                  JsonDocument &stateDoc) {
  if (cmd["brand"].is<const char *>())
    remoteBrand_ = cmd["brand"].as<const char *>();
  if (cmd["type"].is<const char *>())
    remoteType_ = cmd["type"].as<const char *>();
  if (cmd["index"].is<uint16_t>())
    remoteIndex_ = cmd["index"].as<uint16_t>();

  String action = cmd["cmd"].as<String>();
  if (action.isEmpty()) {
    Serial.println(F("[STB] Missing command name"));
    return false;
  }

  bool updated = false;

  if (action.equalsIgnoreCase("key")) {
    const String key = canonicalizeKey(cmd["key"].as<String>());
    if (key.isEmpty()) {
      Serial.println(F("[STB] Missing key name"));
      return false;
    }

    // If IR payload included, store it as learned.
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
      Serial.printf("[STB][IR] No IR mapping for brand=%s key=%s\n",
                    remoteBrand_.c_str(), key.c_str());
    }
  } else if (action.equalsIgnoreCase("channel")) {
    String channelStr = cmd["channel"].as<String>();
    if (channelStr.isEmpty() && cmd["value"].is<const char *>()) {
      channelStr = cmd["value"].as<const char *>();
    }
    if (channelStr.isEmpty()) {
      Serial.println(F("[STB] Missing channel value"));
      return false;
    }
    sendChannelDigits(channelStr);
    state_.channel = channelStr.toInt() > 0 ? channelStr.toInt() : state_.channel;
    updated = true;
  }

  if (!updated) {
    return false;
  }

  return applyState(stateDoc);
}

bool StbController::sendKey(const String &key) {
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

  if (!cmd->nbits || cmd->protocol == decode_type_t::UNKNOWN) {
    return false;
  }

  irSend_.send(cmd->protocol, cmd->value, cmd->nbits);
  Serial.printf("[STB][IR] Sent key=%s protocol=%d value=0x%llX bits=%u\n",
                key.c_str(), static_cast<int>(cmd->protocol),
                static_cast<unsigned long long>(cmd->value), cmd->nbits);
  return true;
}

bool StbController::sendChannelDigits(const String &channel) {
  bool anySent = false;
  for (size_t i = 0; i < channel.length(); ++i) {
    const char c = channel[i];
    if (c >= '0' && c <= '9') {
      String key = "DIGIT_";
      key += c;
      anySent = sendKey(key) || anySent;
      delay(kChannelGapMs);
    } else if (c == '-' || c == '_') {
      anySent = sendKey("DASH") || anySent;
      delay(kChannelGapMs);
    }
  }
  return anySent;
}

bool StbController::learnKey(const String &key, decode_type_t protocol,
                             uint64_t value, uint16_t nbits,
                             const std::vector<uint8_t> &raw) {
  const String normalizedKey = canonicalizeKey(key);
  if (normalizedKey.length() == 0 || protocol == decode_type_t::UNKNOWN ||
      nbits == 0) {
    return false;
  }
  saveLearnedCommand(normalizedKey, protocol, value, nbits, raw);
  return true;
}

void StbController::saveLearnedCommand(const String &key,
                                       decode_type_t protocol, uint64_t value,
                                       uint16_t nbits,
                                       const std::vector<uint8_t> &raw) {
  const String normalizedKey = canonicalizeKey(key);
  for (auto &entry : learnedCommands_) {
    if (normalizedKey.equalsIgnoreCase(entry.key) ||
        canonicalizeKey(entry.key).equalsIgnoreCase(normalizedKey)) {
      entry.protocol = protocol;
      entry.value = value;
      entry.nbits = nbits;
      entry.raw = raw;
      return;
    }
  }

  LearnedCommand cmd;
  cmd.key = normalizedKey;
  cmd.protocol = protocol;
  cmd.value = value;
  cmd.nbits = nbits;
  cmd.raw = raw;
  learnedCommands_.push_back(cmd);
}

bool StbController::sendLearnedKey(const String &key) {
  const String normalizedKey = canonicalizeKey(key);
  for (const auto &entry : learnedCommands_) {
    if (normalizedKey.equalsIgnoreCase(entry.key) ||
        canonicalizeKey(entry.key).equalsIgnoreCase(normalizedKey)) {
      if (!entry.raw.empty() && entry.nbits > 64) {
        irSend_.send(entry.protocol, entry.raw.data(),
                     static_cast<uint16_t>(entry.raw.size()));
      } else {
        irSend_.send(entry.protocol, entry.value, entry.nbits);
      }
      Serial.printf(
          "[STB][IR] Sent learned key=%s protocol=%d value=0x%llX bits=%u\n",
          normalizedKey.c_str(), static_cast<int>(entry.protocol),
          static_cast<unsigned long long>(entry.value), entry.nbits);
      return true;
    }
  }
  return false;
}

bool StbController::applyKeyEffects(const String &key) {
  bool changed = false;
  if (key.equalsIgnoreCase("POWER")) {
    state_.power = !state_.power;
    changed = true;
  } else if (key.equalsIgnoreCase("MUTE")) {
    state_.muted = !state_.muted;
    changed = true;
  } else if (key.startsWith("DIGIT_")) {
    // Channel changes handled when full channel sent; keep state intact here.
  } else if (key.equalsIgnoreCase("CH_UP")) {
    state_.channel = std::max(1, state_.channel + 1);
    changed = true;
  } else if (key.equalsIgnoreCase("CH_DOWN")) {
    state_.channel = std::max(1, state_.channel - 1);
    changed = true;
  }
  return changed;
}

bool StbController::applyState(JsonDocument &stateDoc) {
  // Stateless: do not publish updates
  stateDoc.clear();
  return false;
}

const StbController::RemoteConfig *StbController::findRemote(
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

const StbController::KeyCommand *StbController::findKey(
    const RemoteConfig *remote, const String &key) {
  if (remote == nullptr) return nullptr;
  for (size_t i = 0; i < remote->commandCount; ++i) {
    if (key.equalsIgnoreCase(remote->commands[i].key)) {
      return &remote->commands[i];
    }
  }
  return nullptr;
}
