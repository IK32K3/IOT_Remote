#include "devices/TvController.h"

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

  return out;
}

const TvController::KeyCommand kGenericTvCommands[] = {};

// LG TV codeset #1 (NEC 32-bit, common on many LG remotes)
const TvController::KeyCommand kLgTvCommands1[] = {
    {"POWER", decode_type_t::NEC, 0x20DF10EF, 32},
    {"MUTE", decode_type_t::NEC, 0x20DF906F, 32},
    {"VOL_UP", decode_type_t::NEC, 0x20DF40BF, 32},
    {"VOL_DOWN", decode_type_t::NEC, 0x20DFC03F, 32},
    {"CH_UP", decode_type_t::NEC, 0x20DF00FF, 32},
    {"CH_DOWN", decode_type_t::NEC, 0x20DF807F, 32},
    {"TV_AV", decode_type_t::NEC, 0x20DFD02F, 32},
    {"MENU", decode_type_t::NEC, 0x20DFC23D, 32},
    {"EXIT", decode_type_t::NEC, 0x20DFDA25, 32},
    {"UP", decode_type_t::NEC, 0x20DF02FD, 32},
    {"DOWN", decode_type_t::NEC, 0x20DF827D, 32},
    {"LEFT", decode_type_t::NEC, 0x20DFE01F, 32},
    {"RIGHT", decode_type_t::NEC, 0x20DF609F, 32},
    {"OK", decode_type_t::NEC, 0x20DF22DD, 32},
    {"BACK", decode_type_t::NEC, 0x20DF14EB, 32},
    {"HOME", decode_type_t::NEC, 0x20DF3EC1, 32},
    {"MORE", decode_type_t::NEC, 0x20DF55AA, 32},
    {"DIGIT_0", decode_type_t::NEC, 0x20DF08F7, 32},
    {"DIGIT_1", decode_type_t::NEC, 0x20DF8877, 32},
    {"DIGIT_2", decode_type_t::NEC, 0x20DF48B7, 32},
    {"DIGIT_3", decode_type_t::NEC, 0x20DFC837, 32},
    {"DIGIT_4", decode_type_t::NEC, 0x20DF28D7, 32},
    {"DIGIT_5", decode_type_t::NEC, 0x20DFA857, 32},
    {"DIGIT_6", decode_type_t::NEC, 0x20DF6897, 32},
    {"DIGIT_7", decode_type_t::NEC, 0x20DFE817, 32},
    {"DIGIT_8", decode_type_t::NEC, 0x20DF18E7, 32},
    {"DIGIT_9", decode_type_t::NEC, 0x20DF9867, 32},
};

// LG TV codeset #2/#3: LG protocol samples (limited keys; extend as needed).
const TvController::KeyCommand kLgTvCommands2[] = {
    {"POWER", decode_type_t::LG, 0x04B4AE51, 28},
};

const TvController::KeyCommand kLgTvCommands3[] = {
    {"POWER", decode_type_t::LG, 0xB4B4AE51, 32},
};

// Samsung TV codeset #1 (Samsung 32-bit, common mapping)
const TvController::KeyCommand kSamsungTvCommands1[] = {
    {"POWER", decode_type_t::SAMSUNG, 0xE0E040BF, 32},
    {"MUTE", decode_type_t::SAMSUNG, 0xE0E0F00F, 32},
    {"VOL_UP", decode_type_t::SAMSUNG, 0xE0E0E01F, 32},
    {"VOL_DOWN", decode_type_t::SAMSUNG, 0xE0E0D02F, 32},
    {"CH_UP", decode_type_t::SAMSUNG, 0xE0E048B7, 32},
    {"CH_DOWN", decode_type_t::SAMSUNG, 0xE0E008F7, 32},
    {"TV_AV", decode_type_t::SAMSUNG, 0xE0E0807F, 32},
    {"MENU", decode_type_t::SAMSUNG, 0xE0E058A7, 32},
    {"EXIT", decode_type_t::SAMSUNG, 0xE0E0B44B, 32},
    {"UP", decode_type_t::SAMSUNG, 0xE0E006F9, 32},
    {"DOWN", decode_type_t::SAMSUNG, 0xE0E08679, 32},
    {"LEFT", decode_type_t::SAMSUNG, 0xE0E0A659, 32},
    {"RIGHT", decode_type_t::SAMSUNG, 0xE0E046B9, 32},
    {"OK", decode_type_t::SAMSUNG, 0xE0E016E9, 32},
    {"BACK", decode_type_t::SAMSUNG, 0xE0E01AE5, 32},
    {"HOME", decode_type_t::SAMSUNG, 0xE0E09E61, 32},
    {"MORE", decode_type_t::SAMSUNG, 0xE0E0F807, 32},
    {"DIGIT_0", decode_type_t::SAMSUNG, 0xE0E08877, 32},
    {"DIGIT_1", decode_type_t::SAMSUNG, 0xE0E020DF, 32},
    {"DIGIT_2", decode_type_t::SAMSUNG, 0xE0E0A05F, 32},
    {"DIGIT_3", decode_type_t::SAMSUNG, 0xE0E0609F, 32},
    {"DIGIT_4", decode_type_t::SAMSUNG, 0xE0E010EF, 32},
    {"DIGIT_5", decode_type_t::SAMSUNG, 0xE0E0906F, 32},
    {"DIGIT_6", decode_type_t::SAMSUNG, 0xE0E050AF, 32},
    {"DIGIT_7", decode_type_t::SAMSUNG, 0xE0E030CF, 32},
    {"DIGIT_8", decode_type_t::SAMSUNG, 0xE0E0B04F, 32},
    {"DIGIT_9", decode_type_t::SAMSUNG, 0xE0E0708F, 32},
};

// Samsung TV codeset #2: alternate POWER (discrete ON), other keys same.
const TvController::KeyCommand kSamsungTvCommands2[] = {
    {"POWER", decode_type_t::SAMSUNG, 0xE0E09966, 32},
    {"MUTE", decode_type_t::SAMSUNG, 0xE0E0F00F, 32},
    {"VOL_UP", decode_type_t::SAMSUNG, 0xE0E0E01F, 32},
    {"VOL_DOWN", decode_type_t::SAMSUNG, 0xE0E0D02F, 32},
    {"CH_UP", decode_type_t::SAMSUNG, 0xE0E048B7, 32},
    {"CH_DOWN", decode_type_t::SAMSUNG, 0xE0E008F7, 32},
    {"TV_AV", decode_type_t::SAMSUNG, 0xE0E0807F, 32},
    {"MENU", decode_type_t::SAMSUNG, 0xE0E058A7, 32},
    {"EXIT", decode_type_t::SAMSUNG, 0xE0E0B44B, 32},
    {"UP", decode_type_t::SAMSUNG, 0xE0E006F9, 32},
    {"DOWN", decode_type_t::SAMSUNG, 0xE0E08679, 32},
    {"LEFT", decode_type_t::SAMSUNG, 0xE0E0A659, 32},
    {"RIGHT", decode_type_t::SAMSUNG, 0xE0E046B9, 32},
    {"OK", decode_type_t::SAMSUNG, 0xE0E016E9, 32},
    {"BACK", decode_type_t::SAMSUNG, 0xE0E01AE5, 32},
    {"HOME", decode_type_t::SAMSUNG, 0xE0E09E61, 32},
    {"MORE", decode_type_t::SAMSUNG, 0xE0E0F807, 32},
    {"DIGIT_0", decode_type_t::SAMSUNG, 0xE0E08877, 32},
    {"DIGIT_1", decode_type_t::SAMSUNG, 0xE0E020DF, 32},
    {"DIGIT_2", decode_type_t::SAMSUNG, 0xE0E0A05F, 32},
    {"DIGIT_3", decode_type_t::SAMSUNG, 0xE0E0609F, 32},
    {"DIGIT_4", decode_type_t::SAMSUNG, 0xE0E010EF, 32},
    {"DIGIT_5", decode_type_t::SAMSUNG, 0xE0E0906F, 32},
    {"DIGIT_6", decode_type_t::SAMSUNG, 0xE0E050AF, 32},
    {"DIGIT_7", decode_type_t::SAMSUNG, 0xE0E030CF, 32},
    {"DIGIT_8", decode_type_t::SAMSUNG, 0xE0E0B04F, 32},
    {"DIGIT_9", decode_type_t::SAMSUNG, 0xE0E0708F, 32},
};

// Sony TV codeset #1/#2/#3: SIRC (12/15/20-bit variants).
// Values are compatible with IRsend::sendSony() format (bit-reversed payload).
const TvController::KeyCommand kSonyTvCommands1[] = {
    {"POWER", decode_type_t::SONY, 0x0A90, 12},
    {"MUTE", decode_type_t::SONY, 0x0290, 12},
    {"VOL_UP", decode_type_t::SONY, 0x0490, 12},
    {"VOL_DOWN", decode_type_t::SONY, 0x0C90, 12},
    {"CH_UP", decode_type_t::SONY, 0x0090, 12},
    {"CH_DOWN", decode_type_t::SONY, 0x0890, 12},
    {"TV_AV", decode_type_t::SONY, 0x0A50, 12},
    {"MENU", decode_type_t::SONY, 0x0070, 12},
    {"EXIT", decode_type_t::SONY, 0x0C70, 12},
    {"HOME", decode_type_t::SONY, 0x0070, 12},
    {"BACK", decode_type_t::SONY, 0x0C70, 12},
    {"MORE", decode_type_t::SONY, 0x05D0, 12},
    {"UP", decode_type_t::SONY, 0x02F0, 12},
    {"DOWN", decode_type_t::SONY, 0x0AF0, 12},
    {"LEFT", decode_type_t::SONY, 0x02D0, 12},
    {"RIGHT", decode_type_t::SONY, 0x0CD0, 12},
    {"OK", decode_type_t::SONY, 0x0A70, 12},
    {"DIGIT_0", decode_type_t::SONY, 0x0010, 12},
    {"DIGIT_1", decode_type_t::SONY, 0x0810, 12},
    {"DIGIT_2", decode_type_t::SONY, 0x0410, 12},
    {"DIGIT_3", decode_type_t::SONY, 0x0C10, 12},
    {"DIGIT_4", decode_type_t::SONY, 0x0210, 12},
    {"DIGIT_5", decode_type_t::SONY, 0x0A10, 12},
    {"DIGIT_6", decode_type_t::SONY, 0x0610, 12},
    {"DIGIT_7", decode_type_t::SONY, 0x0E10, 12},
    {"DIGIT_8", decode_type_t::SONY, 0x0110, 12},
    {"DIGIT_9", decode_type_t::SONY, 0x0910, 12},
};

const TvController::KeyCommand kSonyTvCommands2[] = {
    {"POWER", decode_type_t::SONY, 0x5480, 15},
    {"MUTE", decode_type_t::SONY, 0x1480, 15},
    {"VOL_UP", decode_type_t::SONY, 0x2480, 15},
    {"VOL_DOWN", decode_type_t::SONY, 0x6480, 15},
    {"CH_UP", decode_type_t::SONY, 0x0480, 15},
    {"CH_DOWN", decode_type_t::SONY, 0x4480, 15},
    {"TV_AV", decode_type_t::SONY, 0x5280, 15},
    {"MENU", decode_type_t::SONY, 0x0380, 15},
    {"EXIT", decode_type_t::SONY, 0x6380, 15},
    {"HOME", decode_type_t::SONY, 0x0380, 15},
    {"BACK", decode_type_t::SONY, 0x6380, 15},
    {"MORE", decode_type_t::SONY, 0x2E80, 15},
    {"UP", decode_type_t::SONY, 0x1780, 15},
    {"DOWN", decode_type_t::SONY, 0x5780, 15},
    {"LEFT", decode_type_t::SONY, 0x1680, 15},
    {"RIGHT", decode_type_t::SONY, 0x6680, 15},
    {"OK", decode_type_t::SONY, 0x5380, 15},
    {"DIGIT_0", decode_type_t::SONY, 0x0080, 15},
    {"DIGIT_1", decode_type_t::SONY, 0x4080, 15},
    {"DIGIT_2", decode_type_t::SONY, 0x2080, 15},
    {"DIGIT_3", decode_type_t::SONY, 0x6080, 15},
    {"DIGIT_4", decode_type_t::SONY, 0x1080, 15},
    {"DIGIT_5", decode_type_t::SONY, 0x5080, 15},
    {"DIGIT_6", decode_type_t::SONY, 0x3080, 15},
    {"DIGIT_7", decode_type_t::SONY, 0x7080, 15},
    {"DIGIT_8", decode_type_t::SONY, 0x0880, 15},
    {"DIGIT_9", decode_type_t::SONY, 0x4880, 15},
};

const TvController::KeyCommand kSonyTvCommands3[] = {
    {"POWER", decode_type_t::SONY, 0x0A9000, 20},
    {"MUTE", decode_type_t::SONY, 0x029000, 20},
    {"VOL_UP", decode_type_t::SONY, 0x049000, 20},
    {"VOL_DOWN", decode_type_t::SONY, 0x0C9000, 20},
    {"CH_UP", decode_type_t::SONY, 0x009000, 20},
    {"CH_DOWN", decode_type_t::SONY, 0x089000, 20},
    {"TV_AV", decode_type_t::SONY, 0x0A5000, 20},
    {"MENU", decode_type_t::SONY, 0x007000, 20},
    {"EXIT", decode_type_t::SONY, 0x0C7000, 20},
    {"HOME", decode_type_t::SONY, 0x007000, 20},
    {"BACK", decode_type_t::SONY, 0x0C7000, 20},
    {"MORE", decode_type_t::SONY, 0x05D000, 20},
    {"UP", decode_type_t::SONY, 0x02F000, 20},
    {"DOWN", decode_type_t::SONY, 0x0AF000, 20},
    {"LEFT", decode_type_t::SONY, 0x02D000, 20},
    {"RIGHT", decode_type_t::SONY, 0x0CD000, 20},
    {"OK", decode_type_t::SONY, 0x0A7000, 20},
    {"DIGIT_0", decode_type_t::SONY, 0x001000, 20},
    {"DIGIT_1", decode_type_t::SONY, 0x081000, 20},
    {"DIGIT_2", decode_type_t::SONY, 0x041000, 20},
    {"DIGIT_3", decode_type_t::SONY, 0x0C1000, 20},
    {"DIGIT_4", decode_type_t::SONY, 0x021000, 20},
    {"DIGIT_5", decode_type_t::SONY, 0x0A1000, 20},
    {"DIGIT_6", decode_type_t::SONY, 0x061000, 20},
    {"DIGIT_7", decode_type_t::SONY, 0x0E1000, 20},
    {"DIGIT_8", decode_type_t::SONY, 0x011000, 20},
    {"DIGIT_9", decode_type_t::SONY, 0x091000, 20},
};

// Panasonic TV (IRDB: Panasonic/TV/128,0.csv) - Panasonic (Kaseikyo) 48-bit.
// Uses IRsend::encodePanasonic(0x4004, device=0x80, subdevice=0x00, function)
const TvController::KeyCommand kPanasonicTvCommands1[] = {
    {"POWER", decode_type_t::PANASONIC, 0x400480003DBD, 48},     // POWER TOGGLE
    {"MUTE", decode_type_t::PANASONIC, 0x4004800032B2, 48},      // VOLUME MUTE TOGGLE
    {"VOL_UP", decode_type_t::PANASONIC, 0x4004800020A0, 48},    // VOLUME UP
    {"VOL_DOWN", decode_type_t::PANASONIC, 0x4004800021A1, 48},  // VOLUME DOWN
    {"CH_UP", decode_type_t::PANASONIC, 0x4004800034B4, 48},     // CHANNEL UP
    {"CH_DOWN", decode_type_t::PANASONIC, 0x4004800035B5, 48},   // CHANNEL DOWN
    {"TV_AV", decode_type_t::PANASONIC, 0x400480000585, 48},     // INPUT SELECT/SCROLL
    {"MENU", decode_type_t::PANASONIC, 0x4004800052D2, 48},      // MENU
    {"EXIT", decode_type_t::PANASONIC, 0x40048000D353, 48},      // EXIT
    {"BACK", decode_type_t::PANASONIC, 0x40048000D454, 48},      // RETURN
    {"MORE", decode_type_t::PANASONIC, 0x4004800039B9, 48},      // INFO / RECALL
    {"UP", decode_type_t::PANASONIC, 0x400480004ACA, 48},        // CURSOR UP
    {"DOWN", decode_type_t::PANASONIC, 0x400480004BCB, 48},      // CURSOR DOWN
    {"LEFT", decode_type_t::PANASONIC, 0x400480004ECE, 48},      // CURSOR LEFT
    {"RIGHT", decode_type_t::PANASONIC, 0x400480004FCF, 48},     // CURSOR RIGHT
    {"OK", decode_type_t::PANASONIC, 0x4004800049C9, 48},        // CURSOR ENTER/SELECT
    {"DIGIT_0", decode_type_t::PANASONIC, 0x400480001999, 48},   // DIGIT 0/10
    {"DIGIT_1", decode_type_t::PANASONIC, 0x400480001090, 48},   // DIGIT 1
    {"DIGIT_2", decode_type_t::PANASONIC, 0x400480001191, 48},   // DIGIT 2
    {"DIGIT_3", decode_type_t::PANASONIC, 0x400480001292, 48},   // DIGIT 3
    {"DIGIT_4", decode_type_t::PANASONIC, 0x400480001393, 48},   // DIGIT 4
    {"DIGIT_5", decode_type_t::PANASONIC, 0x400480001494, 48},   // DIGIT 5
    {"DIGIT_6", decode_type_t::PANASONIC, 0x400480001595, 48},   // DIGIT 6
    {"DIGIT_7", decode_type_t::PANASONIC, 0x400480001696, 48},   // DIGIT 7
    {"DIGIT_8", decode_type_t::PANASONIC, 0x400480001797, 48},   // DIGIT 8
    {"DIGIT_9", decode_type_t::PANASONIC, 0x400480001898, 48},   // DIGIT 9
};

// Sharp TV (IRDB: Sharp/TV/1,-1.csv) - Sharp 15-bit.
const TvController::KeyCommand kSharpTvCommands1[] = {
    {"POWER", decode_type_t::SHARP, 0x41A2, 15},
    {"MUTE", decode_type_t::SHARP, 0x43A2, 15},
    {"VOL_UP", decode_type_t::SHARP, 0x40A2, 15},
    {"VOL_DOWN", decode_type_t::SHARP, 0x42A2, 15},
    {"CH_UP", decode_type_t::SHARP, 0x4222, 15},
    {"CH_DOWN", decode_type_t::SHARP, 0x4122, 15},
    {"TV_AV", decode_type_t::SHARP, 0x4322, 15},
    {"MENU", decode_type_t::SHARP, 0x4012, 15},
    {"BACK", decode_type_t::SHARP, 0x43D2, 15},  // FLASHBACK
    {"EXIT", decode_type_t::SHARP, 0x43D2, 15},  // FLASHBACK
    {"DIGIT_0", decode_type_t::SHARP, 0x4142, 15},
    {"DIGIT_1", decode_type_t::SHARP, 0x4202, 15},
    {"DIGIT_2", decode_type_t::SHARP, 0x4102, 15},
    {"DIGIT_3", decode_type_t::SHARP, 0x4302, 15},
    {"DIGIT_4", decode_type_t::SHARP, 0x4082, 15},
    {"DIGIT_5", decode_type_t::SHARP, 0x4282, 15},
    {"DIGIT_6", decode_type_t::SHARP, 0x4182, 15},
    {"DIGIT_7", decode_type_t::SHARP, 0x4382, 15},
    {"DIGIT_8", decode_type_t::SHARP, 0x4042, 15},
    {"DIGIT_9", decode_type_t::SHARP, 0x4242, 15},
};

// Mitsubishi TV (IRDB: Mitsubishi/TV/1,-1.csv) - OEM Sharp 15-bit.
const TvController::KeyCommand kMitsubishiTvCommands1[] = {
    {"POWER", decode_type_t::SHARP, 0x41A2, 15},
    {"MUTE", decode_type_t::SHARP, 0x43A2, 15},
    {"VOL_UP", decode_type_t::SHARP, 0x40A2, 15},
    {"VOL_DOWN", decode_type_t::SHARP, 0x42A2, 15},
    {"CH_UP", decode_type_t::SHARP, 0x4222, 15},
    {"CH_DOWN", decode_type_t::SHARP, 0x4122, 15},
    {"TV_AV", decode_type_t::SHARP, 0x4322, 15},
    {"MENU", decode_type_t::SHARP, 0x4012, 15},
    {"BACK", decode_type_t::SHARP, 0x43D2, 15},  // FLASHBACK
    {"EXIT", decode_type_t::SHARP, 0x43D2, 15},  // FLASHBACK
    {"DIGIT_0", decode_type_t::SHARP, 0x4142, 15},
    {"DIGIT_1", decode_type_t::SHARP, 0x4202, 15},
    {"DIGIT_2", decode_type_t::SHARP, 0x4102, 15},
    {"DIGIT_3", decode_type_t::SHARP, 0x4302, 15},
    {"DIGIT_4", decode_type_t::SHARP, 0x4082, 15},
    {"DIGIT_5", decode_type_t::SHARP, 0x4282, 15},
    {"DIGIT_6", decode_type_t::SHARP, 0x4182, 15},
    {"DIGIT_7", decode_type_t::SHARP, 0x4382, 15},
    {"DIGIT_8", decode_type_t::SHARP, 0x4042, 15},
    {"DIGIT_9", decode_type_t::SHARP, 0x4242, 15},
};

// Toshiba TV (IRDB: Toshiba/TV/64,-1.csv) - NEC1/NEC 32-bit.
const TvController::KeyCommand kToshibaTvCommands1[] = {
    {"POWER", decode_type_t::NEC, 0x40BF12ED, 32},
    {"MUTE", decode_type_t::NEC, 0x40BF10EF, 32},
    {"VOL_UP", decode_type_t::NEC, 0x40BF1AE5, 32},
    {"VOL_DOWN", decode_type_t::NEC, 0x40BF1EE1, 32},
    {"CH_UP", decode_type_t::NEC, 0x40BF1BE4, 32},
    {"CH_DOWN", decode_type_t::NEC, 0x40BF1FE0, 32},
    {"TV_AV", decode_type_t::NEC, 0x40BF0FF0, 32},
    {"MENU", decode_type_t::NEC, 0x40BF807F, 32},
    {"OK", decode_type_t::NEC, 0x40BF17E8, 32},     // ENTER
    {"EXIT", decode_type_t::NEC, 0x40BF58A7, 32},   // EXIT
    {"BACK", decode_type_t::NEC, 0x40BF1CE3, 32},   // RECALL
    {"MORE", decode_type_t::NEC, 0x40BF1CE3, 32},   // RECALL
    {"DIGIT_0", decode_type_t::NEC, 0x40BF00FF, 32},
    {"DIGIT_1", decode_type_t::NEC, 0x40BF01FE, 32},
    {"DIGIT_2", decode_type_t::NEC, 0x40BF02FD, 32},
    {"DIGIT_3", decode_type_t::NEC, 0x40BF03FC, 32},
    {"DIGIT_4", decode_type_t::NEC, 0x40BF04FB, 32},
    {"DIGIT_5", decode_type_t::NEC, 0x40BF05FA, 32},
    {"DIGIT_6", decode_type_t::NEC, 0x40BF06F9, 32},
    {"DIGIT_7", decode_type_t::NEC, 0x40BF07F8, 32},
    {"DIGIT_8", decode_type_t::NEC, 0x40BF08F7, 32},
    {"DIGIT_9", decode_type_t::NEC, 0x40BF09F6, 32},
};

// Philips TV (IRDB: Philips/TV/0,-1.csv) - RC5 12-bit.
const TvController::KeyCommand kPhilipsTvCommands1[] = {
    {"POWER", decode_type_t::RC5, 12, 12},
    {"MUTE", decode_type_t::RC5, 13, 12},
    {"VOL_UP", decode_type_t::RC5, 16, 12},
    {"VOL_DOWN", decode_type_t::RC5, 17, 12},
    {"CH_UP", decode_type_t::RC5, 32, 12},
    {"CH_DOWN", decode_type_t::RC5, 33, 12},
    {"TV_AV", decode_type_t::RC5, 56, 12},  // EXT. INPUT
    {"MENU", decode_type_t::RC5, 46, 12},
    {"EXIT", decode_type_t::RC5, 15, 12},
    {"UP", decode_type_t::RC5, 28, 12},
    {"DOWN", decode_type_t::RC5, 29, 12},
    {"RIGHT", decode_type_t::RC5, 43, 12},
    {"LEFT", decode_type_t::RC5, 44, 12},
    {"DIGIT_0", decode_type_t::RC5, 0, 12},
    {"DIGIT_1", decode_type_t::RC5, 1, 12},
    {"DIGIT_2", decode_type_t::RC5, 2, 12},
    {"DIGIT_3", decode_type_t::RC5, 3, 12},
    {"DIGIT_4", decode_type_t::RC5, 4, 12},
    {"DIGIT_5", decode_type_t::RC5, 5, 12},
    {"DIGIT_6", decode_type_t::RC5, 6, 12},
    {"DIGIT_7", decode_type_t::RC5, 7, 12},
    {"DIGIT_8", decode_type_t::RC5, 8, 12},
    {"DIGIT_9", decode_type_t::RC5, 9, 12},
};

// JVC TV (IRDB: JVC/TV/3,-1.csv) - JVC 16-bit.
const TvController::KeyCommand kJvcTvCommands1[] = {
    {"POWER", decode_type_t::JVC, 0xC0E8, 16},
    {"MUTE", decode_type_t::JVC, 0xC038, 16},
    {"VOL_UP", decode_type_t::JVC, 0xC078, 16},
    {"VOL_DOWN", decode_type_t::JVC, 0xC0F8, 16},
    {"CH_UP", decode_type_t::JVC, 0xC098, 16},
    {"CH_DOWN", decode_type_t::JVC, 0xC018, 16},
    {"TV_AV", decode_type_t::JVC, 0xC0C8, 16},
    {"MENU", decode_type_t::JVC, 0xC05E, 16},
    {"EXIT", decode_type_t::JVC, 0xC067, 16},
    {"BACK", decode_type_t::JVC, 0xC0A0, 16},  // RETURN
    {"OK", decode_type_t::JVC, 0xC050, 16},
    {"UP", decode_type_t::JVC, 0xC098, 16},
    {"DOWN", decode_type_t::JVC, 0xC018, 16},
    {"LEFT", decode_type_t::JVC, 0xC0F8, 16},
    {"RIGHT", decode_type_t::JVC, 0xC078, 16},
    {"DIGIT_0", decode_type_t::JVC, 0xC004, 16},
    {"DIGIT_1", decode_type_t::JVC, 0xC084, 16},
    {"DIGIT_2", decode_type_t::JVC, 0xC044, 16},
    {"DIGIT_3", decode_type_t::JVC, 0xC0C4, 16},
    {"DIGIT_4", decode_type_t::JVC, 0xC024, 16},
    {"DIGIT_5", decode_type_t::JVC, 0xC0A4, 16},
    {"DIGIT_6", decode_type_t::JVC, 0xC064, 16},
    {"DIGIT_7", decode_type_t::JVC, 0xC0E4, 16},
    {"DIGIT_8", decode_type_t::JVC, 0xC014, 16},
    {"DIGIT_9", decode_type_t::JVC, 0xC094, 16},
};

// Sanyo TV (IRDB: Sanyo/TV/56,-1.csv) - NEC1/NEC 32-bit.
const TvController::KeyCommand kSanyoTvCommands1[] = {
    {"POWER", decode_type_t::NEC, 0x38C712ED, 32},
    {"MUTE", decode_type_t::NEC, 0x38C718E7, 32},
    {"VOL_UP", decode_type_t::NEC, 0x38C70EF1, 32},
    {"VOL_DOWN", decode_type_t::NEC, 0x38C70FF0, 32},
    {"CH_UP", decode_type_t::NEC, 0x38C70AF5, 32},
    {"CH_DOWN", decode_type_t::NEC, 0x38C70BF4, 32},
    {"TV_AV", decode_type_t::NEC, 0x38C713EC, 32},
    {"MENU", decode_type_t::NEC, 0x38C717E8, 32},
    {"BACK", decode_type_t::NEC, 0x38C719E6, 32},  // RECALL
    {"EXIT", decode_type_t::NEC, 0x38C719E6, 32},  // RECALL
    {"DIGIT_0", decode_type_t::NEC, 0x38C700FF, 32},
    {"DIGIT_1", decode_type_t::NEC, 0x38C701FE, 32},
    {"DIGIT_2", decode_type_t::NEC, 0x38C702FD, 32},
    {"DIGIT_3", decode_type_t::NEC, 0x38C703FC, 32},
    {"DIGIT_4", decode_type_t::NEC, 0x38C704FB, 32},
    {"DIGIT_5", decode_type_t::NEC, 0x38C705FA, 32},
    {"DIGIT_6", decode_type_t::NEC, 0x38C706F9, 32},
    {"DIGIT_7", decode_type_t::NEC, 0x38C707F8, 32},
    {"DIGIT_8", decode_type_t::NEC, 0x38C708F7, 32},
    {"DIGIT_9", decode_type_t::NEC, 0x38C709F6, 32},
};

const TvController::RemoteConfig kTvRemotes[] = {
    {"", "", 0, kGenericTvCommands,
     sizeof(kGenericTvCommands) / sizeof(kGenericTvCommands[0])},
    {"LG", "TV", 1, kLgTvCommands1,
     sizeof(kLgTvCommands1) / sizeof(kLgTvCommands1[0])},
    {"LG", "TV", 2, kLgTvCommands2,
     sizeof(kLgTvCommands2) / sizeof(kLgTvCommands2[0])},
    {"LG", "TV", 3, kLgTvCommands3,
     sizeof(kLgTvCommands3) / sizeof(kLgTvCommands3[0])},
    {"Samsung", "TV", 1, kSamsungTvCommands1,
     sizeof(kSamsungTvCommands1) / sizeof(kSamsungTvCommands1[0])},
    {"Samsung", "TV", 2, kSamsungTvCommands2,
     sizeof(kSamsungTvCommands2) / sizeof(kSamsungTvCommands2[0])},
    {"Sony", "TV", 1, kSonyTvCommands1,
     sizeof(kSonyTvCommands1) / sizeof(kSonyTvCommands1[0])},
    {"Sony", "TV", 2, kSonyTvCommands2,
     sizeof(kSonyTvCommands2) / sizeof(kSonyTvCommands2[0])},
    {"Sony", "TV", 3, kSonyTvCommands3,
     sizeof(kSonyTvCommands3) / sizeof(kSonyTvCommands3[0])},
    {"Panasonic", "TV", 1, kPanasonicTvCommands1,
     sizeof(kPanasonicTvCommands1) / sizeof(kPanasonicTvCommands1[0])},
    {"Sharp", "TV", 1, kSharpTvCommands1,
     sizeof(kSharpTvCommands1) / sizeof(kSharpTvCommands1[0])},
    {"Mitsubishi", "TV", 1, kMitsubishiTvCommands1,
     sizeof(kMitsubishiTvCommands1) / sizeof(kMitsubishiTvCommands1[0])},
    {"Toshiba", "TV", 1, kToshibaTvCommands1,
     sizeof(kToshibaTvCommands1) / sizeof(kToshibaTvCommands1[0])},
    {"Philips", "TV", 1, kPhilipsTvCommands1,
     sizeof(kPhilipsTvCommands1) / sizeof(kPhilipsTvCommands1[0])},
    {"JVC", "TV", 1, kJvcTvCommands1,
     sizeof(kJvcTvCommands1) / sizeof(kJvcTvCommands1[0])},
    {"Sanyo", "TV", 1, kSanyoTvCommands1,
     sizeof(kSanyoTvCommands1) / sizeof(kSanyoTvCommands1[0])},

    // Generic "try list" for brands without a curated codeset.
    // Indexes are deliberately large to avoid clashing with brand-specific sets.
    {"", "TV", 1001, kSamsungTvCommands1,
     sizeof(kSamsungTvCommands1) / sizeof(kSamsungTvCommands1[0])},
    {"", "TV", 1002, kSamsungTvCommands2,
     sizeof(kSamsungTvCommands2) / sizeof(kSamsungTvCommands2[0])},
    {"", "TV", 1003, kLgTvCommands1,
     sizeof(kLgTvCommands1) / sizeof(kLgTvCommands1[0])},
    {"", "TV", 1004, kSonyTvCommands1,
     sizeof(kSonyTvCommands1) / sizeof(kSonyTvCommands1[0])},
    {"", "TV", 1005, kSonyTvCommands2,
     sizeof(kSonyTvCommands2) / sizeof(kSonyTvCommands2[0])},
    {"", "TV", 1006, kSonyTvCommands3,
     sizeof(kSonyTvCommands3) / sizeof(kSonyTvCommands3[0])},
    {"", "TV", 1007, kPanasonicTvCommands1,
     sizeof(kPanasonicTvCommands1) / sizeof(kPanasonicTvCommands1[0])},
    {"", "TV", 1008, kSharpTvCommands1,
     sizeof(kSharpTvCommands1) / sizeof(kSharpTvCommands1[0])},
    {"", "TV", 1009, kMitsubishiTvCommands1,
     sizeof(kMitsubishiTvCommands1) / sizeof(kMitsubishiTvCommands1[0])},
    {"", "TV", 1010, kLgTvCommands2,
     sizeof(kLgTvCommands2) / sizeof(kLgTvCommands2[0])},
    {"", "TV", 1011, kLgTvCommands3,
     sizeof(kLgTvCommands3) / sizeof(kLgTvCommands3[0])},
    {"", "TV", 1012, kToshibaTvCommands1,
     sizeof(kToshibaTvCommands1) / sizeof(kToshibaTvCommands1[0])},
    {"", "TV", 1013, kPhilipsTvCommands1,
     sizeof(kPhilipsTvCommands1) / sizeof(kPhilipsTvCommands1[0])},
    {"", "TV", 1014, kJvcTvCommands1,
     sizeof(kJvcTvCommands1) / sizeof(kJvcTvCommands1[0])},
    {"", "TV", 1015, kSanyoTvCommands1,
     sizeof(kSanyoTvCommands1) / sizeof(kSanyoTvCommands1[0])},
};

int clampVolume(int v) {
  return std::max(0, std::min(100, v));
}

}  // namespace

const TvController::RemoteConfig TvController::kRemotes[] = {
    kTvRemotes[0],
    kTvRemotes[1],
    kTvRemotes[2],
    kTvRemotes[3],
    kTvRemotes[4],
    kTvRemotes[5],
    kTvRemotes[6],
    kTvRemotes[7],
    kTvRemotes[8],
    kTvRemotes[9],
    kTvRemotes[10],
    kTvRemotes[11],
    kTvRemotes[12],
    kTvRemotes[13],
    kTvRemotes[14],
    kTvRemotes[15],
    kTvRemotes[16],
    kTvRemotes[17],
    kTvRemotes[18],
    kTvRemotes[19],
    kTvRemotes[20],
    kTvRemotes[21],
    kTvRemotes[22],
    kTvRemotes[23],
    kTvRemotes[24],
    kTvRemotes[25],
    kTvRemotes[26],
    kTvRemotes[27],
    kTvRemotes[28],
    kTvRemotes[29],
    kTvRemotes[30],
};

TvController::TvController(const char *nodeId, uint8_t irPin)
    : stateTopic_(String("iot/nodes/") + nodeId + "/tv/state"),
      irPin_(irPin),
      irSend_(irPin, IR_SEND_INVERTED, IR_SEND_USE_MODULATION) {}

void TvController::begin() {
  if (!irReady_) {
    tryBegin(irSend_, 0);
    irReady_ = true;
  }
  Serial.printf("[TV] Controller ready (IR pin=%u)\n", irPin_);
}

void TvController::serializeState(JsonDocument &doc) const {
  doc["device"] = deviceType();
  doc["power"] = state_.power;
  doc["muted"] = state_.muted;
  doc["volume"] = state_.volume;
  doc["channel"] = state_.channel;
  doc["input"] = state_.input;
  doc["brand"] = remoteBrand_;
  doc["type"] = remoteType_;
  doc["index"] = remoteIndex_;
  doc["updatedAt"] = millis();
}

bool TvController::handleCommand(JsonObjectConst cmd, JsonDocument &stateDoc) {
  if (cmd["brand"].is<const char *>())
    remoteBrand_ = cmd["brand"].as<const char *>();
  if (cmd["type"].is<const char *>())
    remoteType_ = cmd["type"].as<const char *>();
  if (cmd["index"].is<uint16_t>())
    remoteIndex_ = cmd["index"].as<uint16_t>();

  String action = cmd["cmd"].as<String>();
  if (action.isEmpty()) {
    Serial.println(F("[TV] Missing command name"));
    return false;
  }

  bool updated = false;

  if (action.equalsIgnoreCase("key")) {
    const String key = canonicalizeKey(cmd["key"].as<String>());
    if (key.isEmpty()) {
      Serial.println(F("[TV] Missing key name"));
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
      Serial.printf("[TV][IR] No IR mapping for brand=%s key=%s\n",
                    remoteBrand_.c_str(), key.c_str());
    }
  } else if (action.equalsIgnoreCase("channel")) {
    String channelStr = cmd["channel"].as<String>();
    if (channelStr.isEmpty() && cmd["value"].is<const char *>()) {
      channelStr = cmd["value"].as<const char *>();
    }
    if (channelStr.isEmpty()) {
      Serial.println(F("[TV] Missing channel value"));
      return false;
    }
    sendChannelDigits(channelStr);
    state_.channel = channelStr.toInt() > 0 ? channelStr.toInt() : state_.channel;
    updated = true;
  } else if (action.equalsIgnoreCase("set")) {
    if (cmd["power"].is<bool>()) {
      state_.power = cmd["power"].as<bool>();
      updated = true;
    }
    if (cmd["muted"].is<bool>()) {
      state_.muted = cmd["muted"].as<bool>();
      updated = true;
    }
    if (cmd["volume"].is<int>()) {
      state_.volume = clampVolume(cmd["volume"].as<int>());
      updated = true;
    }
    if (cmd["channel"].is<int>()) {
      int ch = cmd["channel"].as<int>();
      state_.channel = ch > 0 ? ch : state_.channel;
      updated = true;
    }
    if (cmd["input"].is<const char *>()) {
      state_.input = cmd["input"].as<const char *>();
      updated = true;
    }
  }

  if (!updated) {
    return false;
  }

  return applyState(stateDoc);
}

bool TvController::sendKey(const String &key) {
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

  uint64_t value = cmd->value;
  if (cmd->protocol == decode_type_t::RC5 || cmd->protocol == decode_type_t::RC5X) {
    if (rc5Toggle_) value = irSend_.toggleRC5(value);
    rc5Toggle_ = !rc5Toggle_;
  }
  irSend_.send(cmd->protocol, value, cmd->nbits);
  Serial.printf("[TV][IR] Sent key=%s protocol=%d value=0x%llX bits=%u\n",
                key.c_str(), static_cast<int>(cmd->protocol),
                static_cast<unsigned long long>(value), cmd->nbits);
  return true;
}

bool TvController::sendChannelDigits(const String &channel) {
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

bool TvController::learnKey(const String &key, decode_type_t protocol,
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

void TvController::saveLearnedCommand(const String &key, decode_type_t protocol,
                                      uint64_t value, uint16_t nbits,
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

bool TvController::sendLearnedKey(const String &key) {
  const String normalizedKey = canonicalizeKey(key);
  for (const auto &entry : learnedCommands_) {
    if (normalizedKey.equalsIgnoreCase(entry.key) ||
        canonicalizeKey(entry.key).equalsIgnoreCase(normalizedKey)) {
      if (!entry.raw.empty() && entry.nbits > 64) {
        irSend_.send(entry.protocol, entry.raw.data(),
                     static_cast<uint16_t>(entry.raw.size()));
      } else {
        uint64_t value = entry.value;
        if (entry.protocol == decode_type_t::RC5 ||
            entry.protocol == decode_type_t::RC5X) {
          if (rc5Toggle_) value = irSend_.toggleRC5(value);
          rc5Toggle_ = !rc5Toggle_;
        }
        irSend_.send(entry.protocol, value, entry.nbits);
      }
      Serial.printf(
          "[TV][IR] Sent learned key=%s protocol=%d value=0x%llX bits=%u\n",
          normalizedKey.c_str(), static_cast<int>(entry.protocol),
          static_cast<unsigned long long>(entry.value), entry.nbits);
      return true;
    }
  }
  return false;
}

bool TvController::applyKeyEffects(const String &key) {
  bool changed = false;
  if (key.equalsIgnoreCase("POWER")) {
    state_.power = !state_.power;
    changed = true;
  } else if (key.equalsIgnoreCase("MUTE")) {
    state_.muted = !state_.muted;
    changed = true;
  } else if (key.equalsIgnoreCase("VOL_UP")) {
    state_.volume = clampVolume(state_.volume + 1);
    changed = true;
  } else if (key.equalsIgnoreCase("VOL_DOWN")) {
    state_.volume = clampVolume(state_.volume - 1);
    changed = true;
  } else if (key.equalsIgnoreCase("CH_UP")) {
    state_.channel = std::max(1, state_.channel + 1);
    changed = true;
  } else if (key.equalsIgnoreCase("CH_DOWN")) {
    state_.channel = std::max(1, state_.channel - 1);
    changed = true;
  }
  return changed;
}

bool TvController::applyState(JsonDocument &stateDoc) {
  // Skip publishing state for TV (stateless operation)
  stateDoc.clear();
  return false;
}

const TvController::RemoteConfig *TvController::findRemote(
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

const TvController::KeyCommand *TvController::findKey(
    const RemoteConfig *remote, const String &key) {
  if (remote == nullptr) return nullptr;
  for (size_t i = 0; i < remote->commandCount; ++i) {
    if (key.equalsIgnoreCase(remote->commands[i].key)) {
      return &remote->commands[i];
    }
  }
  return nullptr;
}
