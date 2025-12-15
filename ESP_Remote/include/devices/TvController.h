#pragma once

#include <ArduinoJson.h>
#include <IRremoteESP8266.h>
#include <IRsend.h>
#include <vector>

#include "Config.h"
#include "DeviceManager.h"

struct TvState {
  bool power = false;
  bool muted = false;
  int volume = 0;
  int channel = 1;
  String input = "";
};

class TvController : public DeviceController {
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

  TvController(const char *nodeId, uint8_t irPin);

  const char *deviceType() const override { return "tv"; }
  const char *stateTopic() const override { return stateTopic_.c_str(); }
  void begin() override;
  void serializeState(JsonDocument &doc) const override;
  bool handleCommand(JsonObjectConst cmd, JsonDocument &stateDoc) override;
  bool learnKey(const String &key, decode_type_t protocol, uint64_t value,
                uint16_t nbits, const std::vector<uint8_t> &raw = {});

 private:
  static const RemoteConfig kRemotes[];
  static const RemoteConfig *findRemote(const String &brand,
                                        const String &type,
                                        uint16_t index);
  static const KeyCommand *findKey(const RemoteConfig *remote,
                                   const String &key);

  bool sendKey(const String &key);
  bool sendChannelDigits(const String &channel);
  bool applyKeyEffects(const String &key);
  bool applyState(JsonDocument &stateDoc);

  struct LearnedCommand {
    String key;
    decode_type_t protocol;
    uint64_t value;
    uint16_t nbits;
    std::vector<uint8_t> raw;
  };

  void saveLearnedCommand(const String &key, decode_type_t protocol,
                          uint64_t value, uint16_t nbits,
                          const std::vector<uint8_t> &raw = {});
  bool sendLearnedKey(const String &key);

  String stateTopic_;
  uint8_t irPin_;
  IRsend irSend_;
  bool irReady_ = false;
  bool rc5Toggle_ = false;
  TvState state_;
  String remoteBrand_;
  String remoteType_;
  uint16_t remoteIndex_ = 0;
  std::vector<LearnedCommand> learnedCommands_;
};
