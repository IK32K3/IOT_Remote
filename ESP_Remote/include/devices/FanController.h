#pragma once

#include <ArduinoJson.h>
#include <IRremoteESP8266.h>
#include <IRsend.h>
#include <vector>

#include "Config.h"
#include "DeviceManager.h"

struct FanState {
  bool power = false;
  uint8_t speed = 0;      // 0 = off/lowest
  bool swing = false;
  String type = "normal";
  uint16_t timer = 0;     // minutes remaining
};

class FanController : public DeviceController {
 public:
  struct KeyCommand {
    const char *key;
    decode_type_t protocol;
    uint64_t value;
    uint16_t nbits;
  };

  struct RemoteConfig {
    const char *brand;
    const char *type;
    uint16_t index;
    const KeyCommand *commands;
    size_t commandCount;
  };

  FanController(const char *nodeId, uint8_t irPin);

  const char *deviceType() const override { return "fan"; }
  const char *stateTopic() const override { return stateTopic_.c_str(); }
  void begin() override;
  void serializeState(JsonDocument &doc) const override;
  bool handleCommand(JsonObjectConst cmd, JsonDocument &stateDoc) override;

 private:

  static const RemoteConfig kRemotes[];

  static const RemoteConfig *findRemote(const String &brand,
                                        const String &type,
                                        uint16_t index);
  static const KeyCommand *findKey(const RemoteConfig *remote,
                                   const String &key);

  bool sendKey(const String &key);
  bool applyKeyEffects(const String &key);
  bool applyState(JsonDocument &stateDoc);

  struct LearnedCommand {
    String key;
    decode_type_t protocol;
    uint64_t value;
    uint16_t nbits;
  };

  void saveLearnedCommand(const String &key, decode_type_t protocol,
                          uint64_t value, uint16_t nbits);
  bool sendLearnedKey(const String &key);

  String stateTopic_;
  uint8_t irPin_;
  IRsend irSend_;
  bool irReady_ = false;
  FanState state_;
  String remoteBrand_;
  String remoteType_;
  uint16_t remoteIndex_ = 0;
  uint8_t typeIndex_ = 0;
  std::vector<LearnedCommand> learnedCommands_;
};