#include "devices/ProjectorController.h"

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

// InFocus projector (IRDB: InFocus/Video Projector/135,78.csv) - NEC1/NEC 32-bit.
// Provides SOURCE, FREEZE, ZOOM_IN/OUT, KEYSTONE+/- etc (mapped to TRAP_UP/DOWN).
const ProjectorController::KeyCommand kInFocusProjectorCommands1[] = {
    {"POWER", decode_type_t::NEC, 0x72E1E817, 32},
    {"MUTE", decode_type_t::NEC, 0x72E100FF, 32},
    {"FREEZE", decode_type_t::NEC, 0x72E1708F, 32},
    {"SOURCE", decode_type_t::NEC, 0x72E108F7, 32},
    {"VOL_UP", decode_type_t::NEC, 0x72E110EF, 32},
    {"VOL_DOWN", decode_type_t::NEC, 0x72E120DF, 32},
    {"PAGE_UP", decode_type_t::NEC, 0x72E1E11E, 32},    // PREVIOUS
    {"PAGE_DOWN", decode_type_t::NEC, 0x72E1C13E, 32},  // NEXT
    {"ZOOM_IN", decode_type_t::NEC, 0x72E150AF, 32},
    {"ZOOM_OUT", decode_type_t::NEC, 0x72E1D02F, 32},
    {"MENU", decode_type_t::NEC, 0x72E140BF, 32},
    {"EXIT", decode_type_t::NEC, 0x72E1E916, 32},  // ESC
    {"BACK", decode_type_t::NEC, 0x72E1E916, 32},  // ESC
    {"INFO", decode_type_t::NEC, 0x72E150AF, 32},  // HELP (fallback to ZOOM_IN)
    {"VIDEO", decode_type_t::NEC, 0x72E1A05F, 32},
    {"TRAP_UP", decode_type_t::NEC, 0x72E104FB, 32},    // KEYSTONE +
    {"TRAP_DOWN", decode_type_t::NEC, 0x72E1847B, 32},  // KEYSTONE -
    {"OK", decode_type_t::NEC, 0x72E1F906, 32},         // ENTER
    {"UP", decode_type_t::NEC, 0x72E1C837, 32},
    {"DOWN", decode_type_t::NEC, 0x72E128D7, 32},
    {"LEFT", decode_type_t::NEC, 0x72E119E6, 32},
    {"RIGHT", decode_type_t::NEC, 0x72E159A6, 32},
};

// Epson projector (IRDB: Epson/Projector/131,85.csv) - NEC2/NEC 32-bit.
const ProjectorController::KeyCommand kEpsonProjectorCommands1[] = {
    {"POWER", decode_type_t::NEC, 0xAAC109F6, 32},
    {"MUTE", decode_type_t::NEC, 0xAAC1C936, 32},  // A/V MUTE / BLANK
    {"FREEZE", decode_type_t::NEC, 0xAAC149B6, 32},
    {"SOURCE", decode_type_t::NEC, 0xAAC131CE, 32},  // SOURCE SEARCH
    {"VOL_UP", decode_type_t::NEC, 0xAAC119E6, 32},
    {"VOL_DOWN", decode_type_t::NEC, 0xAAC19966, 32},
    {"PAGE_UP", decode_type_t::NEC, 0xAAC10DF2, 32},    // CURSOR UP
    {"PAGE_DOWN", decode_type_t::NEC, 0xAAC14DB2, 32},  // CURSOR DOWN
    {"ZOOM_IN", decode_type_t::NEC, 0xAAC1718E, 32},    // ZOOM (single key)
    {"ZOOM_OUT", decode_type_t::NEC, 0xAAC1718E, 32},   // ZOOM (single key)
    {"MENU", decode_type_t::NEC, 0xAAC159A6, 32},
    {"EXIT", decode_type_t::NEC, 0xAAC121DE, 32},  // ESC
    {"BACK", decode_type_t::NEC, 0xAAC121DE, 32},  // ESC
    {"INFO", decode_type_t::NEC, 0xAAC1A956, 32},  // HELP
    {"VIDEO", decode_type_t::NEC, 0xAAC10EF1, 32},
    {"USB", decode_type_t::NEC, 0xAAC16E91, 32},
    {"OK", decode_type_t::NEC, 0xAAC1A15E, 32},     // ENTER
    {"UP", decode_type_t::NEC, 0xAAC10DF2, 32},     // CURSOR UP
    {"RIGHT", decode_type_t::NEC, 0xAAC18D72, 32},  // CURSOR RIGHT
    {"DOWN", decode_type_t::NEC, 0xAAC14DB2, 32},   // CURSOR DOWN
    {"LEFT", decode_type_t::NEC, 0xAAC1CD32, 32},   // CURSOR LEFT
};

// BenQ projector (IRDB: BenQ/Projector/48,-1.csv) - NEC1/NEC 32-bit.
const ProjectorController::KeyCommand kBenqProjectorCommands1[] = {
    {"POWER", decode_type_t::NEC, 0x0CF318E7, 32},
    {"MUTE", decode_type_t::NEC, 0x0CF348B7, 32},
    {"FREEZE", decode_type_t::NEC, 0x0CF3708F, 32},
    {"SOURCE", decode_type_t::NEC, 0x0CF310EF, 32},
    {"VOL_UP", decode_type_t::NEC, 0x0CF358A7, 32},
    {"VOL_DOWN", decode_type_t::NEC, 0x0CF344BB, 32},
    {"ZOOM_IN", decode_type_t::NEC, 0x0CF334CB, 32},   // D.ZOOM+
    {"ZOOM_OUT", decode_type_t::NEC, 0x0CF3B44B, 32},  // D.ZOOM-
    {"MENU", decode_type_t::NEC, 0x0CF330CF, 32},
    {"BACK", decode_type_t::NEC, 0x0CF308F7, 32},   // RETURN
    {"UP", decode_type_t::NEC, 0x0CF350AF, 32},     // UP
    {"DOWN", decode_type_t::NEC, 0x0CF304FB, 32},   // DOWN
    {"LEFT", decode_type_t::NEC, 0x0CF300FF, 32},   // LEFT
    {"RIGHT", decode_type_t::NEC, 0x0CF340BF, 32},  // RIGHT
};

// Optoma projector (IRDB: Optoma/Projector/50,-1.csv) - NEC1/NEC 32-bit.
const ProjectorController::KeyCommand kOptomaProjectorCommands1[] = {
    {"POWER", decode_type_t::NEC, 0x32CD02FD, 32},   // Power On
    {"SOURCE", decode_type_t::NEC, 0x32CD05FA, 32},  // Mode
    {"MENU", decode_type_t::NEC, 0x32CD0EF1, 32},
    {"OK", decode_type_t::NEC, 0x32CD0FF0, 32},  // Enter
    {"LEFT", decode_type_t::NEC, 0x32CD10EF, 32},
    {"RIGHT", decode_type_t::NEC, 0x32CD12ED, 32},
    {"VOL_UP", decode_type_t::NEC, 0x32CD11EE, 32},
    {"VOL_DOWN", decode_type_t::NEC, 0x32CD14EB, 32},
};

// Sony projector (IRDB: Sony/Video Projector/84,-1.csv) - Sony15 (SIRC 15-bit).
const ProjectorController::KeyCommand kSonyProjectorCommands1[] = {
    {"POWER", decode_type_t::SONY, 0x542A, 15},
    {"MUTE", decode_type_t::SONY, 0x142A, 15},
    {"VOL_UP", decode_type_t::SONY, 0x242A, 15},
    {"VOL_DOWN", decode_type_t::SONY, 0x642A, 15},
    {"MENU", decode_type_t::SONY, 0x4A2A, 15},
    {"VIDEO", decode_type_t::SONY, 0x2A2A, 15},
    {"SOURCE", decode_type_t::SONY, 0x2A2A, 15},
    {"UP", decode_type_t::SONY, 0x562A, 15},
    {"DOWN", decode_type_t::SONY, 0x362A, 15},
    {"LEFT", decode_type_t::SONY, 0x162A, 15},
    {"RIGHT", decode_type_t::SONY, 0x662A, 15},
    {"OK", decode_type_t::SONY, 0x2D2A, 15},  // ENTER
};

// Hitachi projector (IRDB: Hitachi/Video Projector/80,-1.csv) - NEC1/NEC 32-bit.
const ProjectorController::KeyCommand kHitachiProjectorCommands1[] = {
    {"POWER", decode_type_t::NEC, 0x50AF17E8, 32},     // STANDBY/ON
    {"MUTE", decode_type_t::NEC, 0x50AF0BF4, 32},
    {"VOL_UP", decode_type_t::NEC, 0x50AF12ED, 32},
    {"VOL_DOWN", decode_type_t::NEC, 0x50AF15EA, 32},
    {"SOURCE", decode_type_t::NEC, 0x50AF20DF, 32},  // VIDEO 1/2
    {"MENU", decode_type_t::NEC, 0x50AF10EF, 32},    // CALL
    {"ZOOM_IN", decode_type_t::NEC, 0x50AF708F, 32},   // ZOOM TELE
    {"ZOOM_OUT", decode_type_t::NEC, 0x50AF718E, 32},  // ZOOM WIDE
    {"UP", decode_type_t::NEC, 0x50AF4EB1, 32},        // MENU ^
    {"DOWN", decode_type_t::NEC, 0x50AF53AC, 32},      // MENU V
    {"RIGHT", decode_type_t::NEC, 0x50AF5CA3, 32},     // MENU >
    {"LEFT", decode_type_t::NEC, 0x50AF5DA2, 32},      // MENU <
};

// Sanyo projector (IRDB: Sanyo/Video Projector/48,-1.csv) - NEC1/NEC 32-bit.
const ProjectorController::KeyCommand kSanyoProjectorCommands1[] = {
    {"POWER", decode_type_t::NEC, 0x30CF00FF, 32},
    {"MUTE", decode_type_t::NEC, 0x30CF0BF4, 32},
    {"VOL_UP", decode_type_t::NEC, 0x30CF09F6, 32},
    {"VOL_DOWN", decode_type_t::NEC, 0x30CF0AF5, 32},
    {"MENU", decode_type_t::NEC, 0x30CF1CE3, 32},
    {"SOURCE", decode_type_t::NEC, 0x30CF05FA, 32},  // V.MODE
    {"ZOOM_IN", decode_type_t::NEC, 0x30CF47B8, 32},
    {"ZOOM_OUT", decode_type_t::NEC, 0x30CF46B9, 32},
    {"TRAP_UP", decode_type_t::NEC, 0x30CF5BA4, 32},    // KEYSTONE
    {"TRAP_DOWN", decode_type_t::NEC, 0x30CF5BA4, 32},  // KEYSTONE
};

// Sharp projector (IRDB: Sharp/Video Projector/13,-1.csv) - Sharp 15-bit.
const ProjectorController::KeyCommand kSharpProjectorCommands1[] = {
    {"POWER", decode_type_t::SHARP, 0x59A2, 15},
    {"MUTE", decode_type_t::SHARP, 0x5BA2, 15},
    {"VOL_UP", decode_type_t::SHARP, 0x58A2, 15},
    {"VOL_DOWN", decode_type_t::SHARP, 0x5AA2, 15},
    {"SOURCE", decode_type_t::SHARP, 0x5B22, 15},  // INPUT SELECT
    {"MENU", decode_type_t::SHARP, 0x588E, 15},
    {"OK", decode_type_t::SHARP, 0x5BAA, 15},  // ENTER
    {"TRAP_UP", decode_type_t::SHARP, 0x58E6, 15},
    {"TRAP_DOWN", decode_type_t::SHARP, 0x5AE6, 15},
    {"MORE", decode_type_t::SHARP, 0x5A72, 15},  // STATUS
};

// JVC projector (IRDB: JVC/Projector/115,-1.csv) - JVC 16-bit.
const ProjectorController::KeyCommand kJvcProjectorCommands1[] = {
    {"POWER", decode_type_t::JVC, 0xCEA0, 16},  // POWER ON
    {"POWER_OFF", decode_type_t::JVC, 0xCE60, 16},
    {"MENU", decode_type_t::JVC, 0xCE74, 16},
    {"EXIT", decode_type_t::JVC, 0xCEC0, 16},
    {"OK", decode_type_t::JVC, 0xCEF4, 16},
    {"UP", decode_type_t::JVC, 0xCE80, 16},
    {"DOWN", decode_type_t::JVC, 0xCE40, 16},
    {"LEFT", decode_type_t::JVC, 0xCE6C, 16},
    {"RIGHT", decode_type_t::JVC, 0xCE2C, 16},
    {"VIDEO", decode_type_t::JVC, 0xCED2, 16},
    {"SOURCE", decode_type_t::JVC, 0xCED2, 16},
    {"MORE", decode_type_t::JVC, 0xCE2E, 16},  // INFO
};

// Boxlight projector (IRDB: Boxlight/Projector/48,-1.csv) - NEC1/NEC 32-bit.
const ProjectorController::KeyCommand kBoxlightProjectorCommands1[] = {
    {"POWER", decode_type_t::NEC, 0x30CF00FF, 32},
    {"FREEZE", decode_type_t::NEC, 0x30CF43BC, 32},
    {"SOURCE", decode_type_t::NEC, 0x30CF05FA, 32},  // VIDEO 1
    {"MENU", decode_type_t::NEC, 0x30CF1CE3, 32},
    {"ZOOM_IN", decode_type_t::NEC, 0x30CF47B8, 32},
    {"ZOOM_OUT", decode_type_t::NEC, 0x30CF46B9, 32},
    {"TRAP_UP", decode_type_t::NEC, 0x30CF8E71, 32},
    {"TRAP_DOWN", decode_type_t::NEC, 0x30CF8F70, 32},
};

const ProjectorController::KeyCommand kGenericProjectorCommands[] = {};

const ProjectorController::RemoteConfig kProjectorRemotes[] = {
    {"", "", 0, kGenericProjectorCommands,
     sizeof(kGenericProjectorCommands) / sizeof(kGenericProjectorCommands[0])},

    {"InFocus", "PROJECTOR", 1, kInFocusProjectorCommands1,
     sizeof(kInFocusProjectorCommands1) / sizeof(kInFocusProjectorCommands1[0])},
    {"Epson", "PROJECTOR", 2, kEpsonProjectorCommands1,
     sizeof(kEpsonProjectorCommands1) / sizeof(kEpsonProjectorCommands1[0])},
    {"BenQ", "PROJECTOR", 3, kBenqProjectorCommands1,
     sizeof(kBenqProjectorCommands1) / sizeof(kBenqProjectorCommands1[0])},
    {"Optoma", "PROJECTOR", 4, kOptomaProjectorCommands1,
     sizeof(kOptomaProjectorCommands1) / sizeof(kOptomaProjectorCommands1[0])},
    {"Sony", "PROJECTOR", 5, kSonyProjectorCommands1,
     sizeof(kSonyProjectorCommands1) / sizeof(kSonyProjectorCommands1[0])},
    {"Hitachi", "PROJECTOR", 6, kHitachiProjectorCommands1,
     sizeof(kHitachiProjectorCommands1) / sizeof(kHitachiProjectorCommands1[0])},
    {"Sanyo", "PROJECTOR", 7, kSanyoProjectorCommands1,
     sizeof(kSanyoProjectorCommands1) / sizeof(kSanyoProjectorCommands1[0])},
    {"Sharp", "PROJECTOR", 8, kSharpProjectorCommands1,
     sizeof(kSharpProjectorCommands1) / sizeof(kSharpProjectorCommands1[0])},
    {"JVC", "PROJECTOR", 9, kJvcProjectorCommands1,
     sizeof(kJvcProjectorCommands1) / sizeof(kJvcProjectorCommands1[0])},
    {"Boxlight", "PROJECTOR", 10, kBoxlightProjectorCommands1,
     sizeof(kBoxlightProjectorCommands1) / sizeof(kBoxlightProjectorCommands1[0])},

    // Generic "try list" indexes (avoid clashing with brand-specific sets).
    {"", "PROJECTOR", 4001, kInFocusProjectorCommands1,
     sizeof(kInFocusProjectorCommands1) / sizeof(kInFocusProjectorCommands1[0])},
    {"", "PROJECTOR", 4002, kEpsonProjectorCommands1,
     sizeof(kEpsonProjectorCommands1) / sizeof(kEpsonProjectorCommands1[0])},
    {"", "PROJECTOR", 4003, kBenqProjectorCommands1,
     sizeof(kBenqProjectorCommands1) / sizeof(kBenqProjectorCommands1[0])},
    {"", "PROJECTOR", 4004, kOptomaProjectorCommands1,
     sizeof(kOptomaProjectorCommands1) / sizeof(kOptomaProjectorCommands1[0])},
    {"", "PROJECTOR", 4005, kSonyProjectorCommands1,
     sizeof(kSonyProjectorCommands1) / sizeof(kSonyProjectorCommands1[0])},
    {"", "PROJECTOR", 4006, kHitachiProjectorCommands1,
     sizeof(kHitachiProjectorCommands1) / sizeof(kHitachiProjectorCommands1[0])},
    {"", "PROJECTOR", 4007, kSanyoProjectorCommands1,
     sizeof(kSanyoProjectorCommands1) / sizeof(kSanyoProjectorCommands1[0])},
    {"", "PROJECTOR", 4008, kSharpProjectorCommands1,
     sizeof(kSharpProjectorCommands1) / sizeof(kSharpProjectorCommands1[0])},
    {"", "PROJECTOR", 4009, kJvcProjectorCommands1,
     sizeof(kJvcProjectorCommands1) / sizeof(kJvcProjectorCommands1[0])},
    {"", "PROJECTOR", 4010, kBoxlightProjectorCommands1,
     sizeof(kBoxlightProjectorCommands1) / sizeof(kBoxlightProjectorCommands1[0])},
};
}  // namespace

const ProjectorController::RemoteConfig ProjectorController::kRemotes[] = {
    kProjectorRemotes[0],  kProjectorRemotes[1],  kProjectorRemotes[2],
    kProjectorRemotes[3],  kProjectorRemotes[4],  kProjectorRemotes[5],
    kProjectorRemotes[6],  kProjectorRemotes[7],  kProjectorRemotes[8],
    kProjectorRemotes[9],  kProjectorRemotes[10], kProjectorRemotes[11],
    kProjectorRemotes[12], kProjectorRemotes[13], kProjectorRemotes[14],
    kProjectorRemotes[15], kProjectorRemotes[16], kProjectorRemotes[17],
    kProjectorRemotes[18], kProjectorRemotes[19], kProjectorRemotes[20],
};

ProjectorController::ProjectorController(const char *nodeId, uint8_t irPin)
    : stateTopic_(String("iot/nodes/") + nodeId + "/projector/state"),
      irPin_(irPin),
      irSend_(irPin) {}

void ProjectorController::begin() {
  if (!irReady_) {
    tryBegin(irSend_, 0);
    irReady_ = true;
  }
  Serial.printf("[PROJECTOR] Controller ready (IR pin=%u)\n", irPin_);
}

void ProjectorController::serializeState(JsonDocument &doc) const {
  doc["device"] = deviceType();
  doc["power"] = state_.power;
  doc["frozen"] = state_.frozen;
  doc["brand"] = remoteBrand_;
  doc["type"] = remoteType_;
  doc["index"] = remoteIndex_;
  doc["updatedAt"] = millis();
}

bool ProjectorController::handleCommand(JsonObjectConst cmd,
                                        JsonDocument &stateDoc) {
  if (cmd["brand"].is<const char *>())
    remoteBrand_ = cmd["brand"].as<const char *>();
  if (cmd["type"].is<const char *>())
    remoteType_ = cmd["type"].as<const char *>();
  if (cmd["index"].is<uint16_t>())
    remoteIndex_ = cmd["index"].as<uint16_t>();

  String action = cmd["cmd"].as<String>();
  if (action.isEmpty()) {
    Serial.println(F("[PROJECTOR] Missing command name"));
    return false;
  }

  bool updated = false;

  if (action.equalsIgnoreCase("key")) {
    const String key = cmd["key"].as<String>();
    if (key.isEmpty()) {
      Serial.println(F("[PROJECTOR] Missing key name"));
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
      Serial.printf("[PROJECTOR][IR] No IR mapping for brand=%s key=%s\n",
                    remoteBrand_.c_str(), key.c_str());
    }
  }

  if (!updated) {
    return false;
  }

  return applyState(stateDoc);
}

bool ProjectorController::sendKey(const String &key) {
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
  Serial.printf("[PROJECTOR][IR] Sent key=%s protocol=%d value=0x%llX bits=%u\n",
                key.c_str(), static_cast<int>(cmd->protocol),
                static_cast<unsigned long long>(cmd->value), cmd->nbits);
  return true;
}

bool ProjectorController::learnKey(const String &key, decode_type_t protocol,
                                   uint64_t value, uint16_t nbits,
                                   const std::vector<uint8_t> &raw) {
  if (key.length() == 0 || protocol == decode_type_t::UNKNOWN || nbits == 0) {
    return false;
  }
  saveLearnedCommand(key, protocol, value, nbits, raw);
  return true;
}

void ProjectorController::saveLearnedCommand(const String &key,
                                             decode_type_t protocol,
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

bool ProjectorController::sendLearnedKey(const String &key) {
  for (const auto &entry : learnedCommands_) {
    if (key.equalsIgnoreCase(entry.key)) {
      if (!entry.raw.empty() && entry.nbits > 64) {
        irSend_.send(entry.protocol, entry.raw.data(),
                     static_cast<uint16_t>(entry.raw.size()));
      } else {
        irSend_.send(entry.protocol, entry.value, entry.nbits);
      }
      Serial.printf(
          "[PROJECTOR][IR] Sent learned key=%s protocol=%d value=0x%llX bits=%u\n",
          key.c_str(), static_cast<int>(entry.protocol),
          static_cast<unsigned long long>(entry.value), entry.nbits);
      return true;
    }
  }
  return false;
}

bool ProjectorController::applyKeyEffects(const String &key) {
  bool changed = false;
  if (key.equalsIgnoreCase("POWER")) {
    state_.power = !state_.power;
    changed = true;
  } else if (key.equalsIgnoreCase("FREEZE")) {
    state_.frozen = !state_.frozen;
    changed = true;
  }
  return changed;
}

bool ProjectorController::applyState(JsonDocument &stateDoc) {
  // Stateless: do not publish updates
  stateDoc.clear();
  return false;
}

const ProjectorController::RemoteConfig *ProjectorController::findRemote(
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

const ProjectorController::KeyCommand *ProjectorController::findKey(
    const RemoteConfig *remote, const String &key) {
  if (remote == nullptr) return nullptr;
  for (size_t i = 0; i < remote->commandCount; ++i) {
    if (key.equalsIgnoreCase(remote->commands[i].key)) {
      return &remote->commands[i];
    }
  }
  return nullptr;
}
