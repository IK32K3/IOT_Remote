#include "devices/DvdController.h"

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

String canonicalizeKey(const String &key) {
  String out = key;
  out.trim();
  out.toUpperCase();

  // Common aliases (helps keep ESP compatible if Android naming changes).
  if (out.equalsIgnoreCase("EJECTCD") || out.equalsIgnoreCase("OPEN") ||
      out.equalsIgnoreCase("TRAY")) {
    return "EJECT";
  }
  if (out.equalsIgnoreCase("PLAYPAUSE") || out.equalsIgnoreCase("PLAY/PAUSE") ||
      out.equalsIgnoreCase("PLAY_PAUSE")) {
    return "PLAY_PAUSE";
  }
  if (out.equalsIgnoreCase("FAST_FORWARD") || out.equalsIgnoreCase("FORWARD") ||
      out.equalsIgnoreCase("FFWD") || out.equalsIgnoreCase("FWD")) {
    return "FF";
  }
  if (out.equalsIgnoreCase("FAST_BACKWARD") || out.equalsIgnoreCase("REWIND") ||
      out.equalsIgnoreCase("BACKWARD") || out.equalsIgnoreCase("RW") ||
      out.equalsIgnoreCase("RWD")) {
    return "REW";
  }
  if (out.equalsIgnoreCase("PREVIOUS") || out.equalsIgnoreCase("PREV") ||
      out.equalsIgnoreCase("SKIP_PREV") || out.equalsIgnoreCase("SKIP_BACK") ||
      out.equalsIgnoreCase("TRACK_PREV")) {
    return "PREV";
  }
  if (out.equalsIgnoreCase("SKIP_NEXT") || out.equalsIgnoreCase("SKIP_FORWARD") ||
      out.equalsIgnoreCase("TRACK_NEXT")) {
    return "NEXT";
  }
  if (out.equalsIgnoreCase("ENTER") || out.equalsIgnoreCase("SELECT") ||
      out.equalsIgnoreCase("CONFIRM")) {
    return "OK";
  }
  if (out.equalsIgnoreCase("RETURN")) {
    return "BACK";
  }
  if (out.equalsIgnoreCase("SETTINGS") || out.equalsIgnoreCase("OPTIONS") ||
      out.equalsIgnoreCase("SETUP")) {
    return "MENU";
  }

  return out;
}

// LG Blu-ray/DVD (BD300) - NECx (use NEC 32-bit payload with pre_data 0xB4B4)
const DvdController::KeyCommand kLgDvdCommands1[] = {
    {"POWER", decode_type_t::NEC, 0xB4B46E91, 32},
    {"MUTE", decode_type_t::NEC, 0xB4B4F20D, 32},  // KEY_AUDIO (fallback)
    {"EJECT", decode_type_t::NEC, 0xB4B46C93, 32},
    {"PLAY_PAUSE", decode_type_t::NEC, 0xB4B41CE3, 32},  // KEY_PAUSE
    {"STOP", decode_type_t::NEC, 0xB4B49C63, 32},
    {"FF", decode_type_t::NEC, 0xB4B4CC33, 32},
    {"REW", decode_type_t::NEC, 0xB4B44CB3, 32},
    {"NEXT", decode_type_t::NEC, 0xB4B42CD3, 32},  // KEY_FORWARD
    {"PREV", decode_type_t::NEC, 0xB4B4AC53, 32},  // KEY_BACK

    {"MENU", decode_type_t::NEC, 0xB4B4D22D, 32},
    {"HOME", decode_type_t::NEC, 0xB4B4E619, 32},
    {"BACK", decode_type_t::NEC, 0xB4B4A25D, 32},  // X_KEY_RETURN
    {"EXIT", decode_type_t::NEC, 0xB4B4A25D, 32},  // X_KEY_RETURN

    {"UP", decode_type_t::NEC, 0xB4B4E21D, 32},
    {"DOWN", decode_type_t::NEC, 0xB4B412ED, 32},
    {"LEFT", decode_type_t::NEC, 0xB4B49A65, 32},
    {"RIGHT", decode_type_t::NEC, 0xB4B45AA5, 32},
    {"OK", decode_type_t::NEC, 0xB4B41AE5, 32},

    {"TITLE", decode_type_t::NEC, 0xB4B452AD, 32},
    {"SUBTITLE", decode_type_t::NEC, 0xB4B4EF10, 32},
    {"RED", decode_type_t::NEC, 0xB4B43EC1, 32},
    {"GREEN", decode_type_t::NEC, 0xB4B4BE41, 32},
    {"YELLOW", decode_type_t::NEC, 0xB4B47E81, 32},
    {"BLUE", decode_type_t::NEC, 0xB4B4FE01, 32},

    {"DIGIT_0", decode_type_t::NEC, 0xB4B422DD, 32},
    {"DIGIT_1", decode_type_t::NEC, 0xB4B4DC23, 32},
    {"DIGIT_2", decode_type_t::NEC, 0xB4B43CC3, 32},
    {"DIGIT_3", decode_type_t::NEC, 0xB4B4BC43, 32},
    {"DIGIT_4", decode_type_t::NEC, 0xB4B47C83, 32},
    {"DIGIT_5", decode_type_t::NEC, 0xB4B4FC03, 32},
    {"DIGIT_6", decode_type_t::NEC, 0xB4B402FD, 32},
    {"DIGIT_7", decode_type_t::NEC, 0xB4B4827D, 32},
    {"DIGIT_8", decode_type_t::NEC, 0xB4B442BD, 32},
    {"DIGIT_9", decode_type_t::NEC, 0xB4B4C23D, 32},
};

// Samsung DVD (SV-DVD3E) - NECx (use NEC 32-bit payload with pre_data 0xA0A0)
const DvdController::KeyCommand kSamsungDvdCommands1[] = {
    {"POWER", decode_type_t::NEC, 0xA0A040BF, 32},       // STANDBY/ON
    {"EJECT", decode_type_t::NEC, 0xA0A04CB3, 32},       // KEY_OPEN
    {"PLAY_PAUSE", decode_type_t::NEC, 0xA0A09867, 32},  // KEY_PLAYPAUSE
    {"STOP", decode_type_t::NEC, 0xA0A0A857, 32},        // KEY_STOP
    {"MENU", decode_type_t::NEC, 0xA0A0F807, 32},        // DISC_MENU
    {"BACK", decode_type_t::NEC, 0xA0A0B847, 32},        // KEY_CLEAR
    {"EXIT", decode_type_t::NEC, 0xA0A0B847, 32},        // KEY_CLEAR

    // Combined transport keys: map to both SKIP and SCAN actions.
    {"NEXT", decode_type_t::NEC, 0xA0A058A7, 32},  // FF/NEXT
    {"FF", decode_type_t::NEC, 0xA0A058A7, 32},    // FF/NEXT
    {"PREV", decode_type_t::NEC, 0xA0A018E7, 32},  // FB/PREV
    {"REW", decode_type_t::NEC, 0xA0A018E7, 32},   // FB/PREV

    {"UP", decode_type_t::NEC, 0xA0A034CB, 32},
    {"DOWN", decode_type_t::NEC, 0xA0A0B44B, 32},
    {"LEFT", decode_type_t::NEC, 0xA0A0E817, 32},
    {"RIGHT", decode_type_t::NEC, 0xA0A0C837, 32},
    {"OK", decode_type_t::NEC, 0xA0A0BC43, 32},  // KEY_ENTER

    {"TITLE", decode_type_t::NEC, 0xA0A006F9, 32},
    {"SUBTITLE", decode_type_t::NEC, 0xA0A044BB, 32},

    {"DIGIT_0", decode_type_t::NEC, 0xA0A08877, 32},
    {"DIGIT_1", decode_type_t::NEC, 0xA0A020DF, 32},
    {"DIGIT_2", decode_type_t::NEC, 0xA0A0A05F, 32},
    {"DIGIT_3", decode_type_t::NEC, 0xA0A0609F, 32},
    {"DIGIT_4", decode_type_t::NEC, 0xA0A010EF, 32},
    {"DIGIT_5", decode_type_t::NEC, 0xA0A0906F, 32},
    {"DIGIT_6", decode_type_t::NEC, 0xA0A050AF, 32},
    {"DIGIT_7", decode_type_t::NEC, 0xA0A030CF, 32},
    {"DIGIT_8", decode_type_t::NEC, 0xA0A0B04F, 32},
    {"DIGIT_9", decode_type_t::NEC, 0xA0A0708F, 32},
};

// Sony DVD - RMT-V501A (Sony20 / SIRC 20-bit, device=26 ext=83)
// Values are compatible with IRsend::sendSony() format (bit-reversed payload).
const DvdController::KeyCommand kSonyDvdCommands1[] = {
    {"POWER", decode_type_t::SONY, 0xA8BCA, 20},
    {"EJECT", decode_type_t::SONY, 0x68BCA, 20},
    {"PLAY_PAUSE", decode_type_t::SONY, 0x98BCA, 20},  // Use PAUSE as toggle-like
    {"STOP", decode_type_t::SONY, 0x18BCA, 20},
    {"NEXT", decode_type_t::SONY, 0x6ABCA, 20},
    {"PREV", decode_type_t::SONY, 0xEABCA, 20},
    {"FF", decode_type_t::SONY, 0x38BCA, 20},
    {"REW", decode_type_t::SONY, 0xEABCA, 20},  // No REW code; fall back to PREV

    {"MENU", decode_type_t::SONY, 0xC4BCA, 20},
    {"BACK", decode_type_t::SONY, 0xD8BCA, 20},
    {"EXIT", decode_type_t::SONY, 0xD8BCA, 20},

    {"UP", decode_type_t::SONY, 0x42BCA, 20},
    {"DOWN", decode_type_t::SONY, 0xC2BCA, 20},
    {"LEFT", decode_type_t::SONY, 0x46BCA, 20},
    {"RIGHT", decode_type_t::SONY, 0x86BCA, 20},
    {"OK", decode_type_t::SONY, 0xD0BCA, 20},

    {"SUBTITLE", decode_type_t::SONY, 0x6BCA, 20},

    {"DIGIT_0", decode_type_t::SONY, 0x90BCA, 20},
    {"DIGIT_1", decode_type_t::SONY, 0xBCA, 20},
    {"DIGIT_2", decode_type_t::SONY, 0x80BCA, 20},
    {"DIGIT_3", decode_type_t::SONY, 0x40BCA, 20},
    {"DIGIT_4", decode_type_t::SONY, 0xC0BCA, 20},
    {"DIGIT_5", decode_type_t::SONY, 0x20BCA, 20},
    {"DIGIT_6", decode_type_t::SONY, 0xA0BCA, 20},
    {"DIGIT_7", decode_type_t::SONY, 0x60BCA, 20},
    {"DIGIT_8", decode_type_t::SONY, 0xE0BCA, 20},
    {"DIGIT_9", decode_type_t::SONY, 0x10BCA, 20},
};

// Sony DVD - RMT-V181N (Sony12 / SIRC 12-bit, mixed device ids)
// Values are compatible with IRsend::sendSony() format (bit-reversed payload).
const DvdController::KeyCommand kSonyDvdCommands2[] = {
    {"POWER", decode_type_t::SONY, 0x0A9A, 12},    // VTRPOWER (device 11)
    {"EJECT", decode_type_t::SONY, 0x069A, 12},    // KEY_EJECTCD (device 11)
    {"PLAY_PAUSE", decode_type_t::SONY, 0x059A, 12},
    {"STOP", decode_type_t::SONY, 0x019A, 12},
    {"NEXT", decode_type_t::SONY, 0x0BBA, 12},
    {"FF", decode_type_t::SONY, 0x039A, 12},
    {"REW", decode_type_t::SONY, 0x0D9A, 12},

    {"MENU", decode_type_t::SONY, 0x0070, 12},
    {"UP", decode_type_t::SONY, 0x02F0, 12},
    {"DOWN", decode_type_t::SONY, 0x0AF0, 12},
    {"LEFT", decode_type_t::SONY, 0x02D0, 12},
    {"RIGHT", decode_type_t::SONY, 0x0CD0, 12},

    {"DIGIT_0", decode_type_t::SONY, 0x0910, 12},
    {"DIGIT_1", decode_type_t::SONY, 0x0010, 12},
    {"DIGIT_2", decode_type_t::SONY, 0x0810, 12},
    {"DIGIT_3", decode_type_t::SONY, 0x0410, 12},
    {"DIGIT_4", decode_type_t::SONY, 0x0C10, 12},
    {"DIGIT_5", decode_type_t::SONY, 0x0210, 12},
    {"DIGIT_6", decode_type_t::SONY, 0x0A10, 12},
    {"DIGIT_7", decode_type_t::SONY, 0x0610, 12},
    {"DIGIT_8", decode_type_t::SONY, 0x0E10, 12},
    {"DIGIT_9", decode_type_t::SONY, 0x0110, 12},
};

// Panasonic DVD (IRDB: Panasonic/DVD Player/176,0.csv) - Panasonic 48-bit.
const DvdController::KeyCommand kPanasonicDvdCommands1[] = {
    {"POWER", decode_type_t::PANASONIC, 0x4004B0003D8D, 48},
    {"EJECT", decode_type_t::PANASONIC, 0x4004B00001B1, 48},  // OPEN/CLOSE
    {"PLAY_PAUSE", decode_type_t::PANASONIC, 0x4004B0000ABA, 48},  // PLAY
    {"STOP", decode_type_t::PANASONIC, 0x4004B00000B0, 48},
    {"FF", decode_type_t::PANASONIC, 0x4004B00005B5, 48},  // SEARCH >>
    {"REW", decode_type_t::PANASONIC, 0x4004B00004B4, 48},  // SEARCH <<
    {"NEXT", decode_type_t::PANASONIC, 0x4004B0004AFA, 48},  // SKIP >>
    {"PREV", decode_type_t::PANASONIC, 0x4004B00049F9, 48},  // SKIP <<

    {"MENU", decode_type_t::PANASONIC, 0x4004B0008030, 48},
    {"BACK", decode_type_t::PANASONIC, 0x4004B0008131, 48},  // RETURN
    {"EXIT", decode_type_t::PANASONIC, 0x4004B0008131, 48},  // RETURN
    {"OK", decode_type_t::PANASONIC, 0x4004B0008232, 48},  // ENTER
    {"UP", decode_type_t::PANASONIC, 0x4004B0008535, 48},
    {"DOWN", decode_type_t::PANASONIC, 0x4004B0008636, 48},
    {"LEFT", decode_type_t::PANASONIC, 0x4004B0008737, 48},
    {"RIGHT", decode_type_t::PANASONIC, 0x4004B0008838, 48},

    {"TITLE", decode_type_t::PANASONIC, 0x4004B0009B2B, 48},  // TOP MENU
    {"SUBTITLE", decode_type_t::PANASONIC, 0x4004B0009121, 48},

    {"DIGIT_0", decode_type_t::PANASONIC, 0x4004B00019A9, 48},
    {"DIGIT_1", decode_type_t::PANASONIC, 0x4004B00010A0, 48},
    {"DIGIT_2", decode_type_t::PANASONIC, 0x4004B00011A1, 48},
    {"DIGIT_3", decode_type_t::PANASONIC, 0x4004B00012A2, 48},
    {"DIGIT_4", decode_type_t::PANASONIC, 0x4004B00013A3, 48},
    {"DIGIT_5", decode_type_t::PANASONIC, 0x4004B00014A4, 48},
    {"DIGIT_6", decode_type_t::PANASONIC, 0x4004B00015A5, 48},
    {"DIGIT_7", decode_type_t::PANASONIC, 0x4004B00016A6, 48},
    {"DIGIT_8", decode_type_t::PANASONIC, 0x4004B00017A7, 48},
    {"DIGIT_9", decode_type_t::PANASONIC, 0x4004B00018A8, 48},
};

// Philips DVD (IRDB: Philips/DVD Player/4,-1.csv) - RC6 mode0 20-bit.
const DvdController::KeyCommand kPhilipsDvdCommands1[] = {
    // Values are compatible with IRsend::sendRC6() format.
    // (mode=0, addr=4, cmd=<x>) => 0x4<cmd>
    {"POWER", decode_type_t::RC6, 0x40C, 20},
    {"MENU", decode_type_t::RC6, 0x40F, 20},  // OSD MENU
    {"PLAY_PAUSE", decode_type_t::RC6, 0x42C, 20},
    {"STOP", decode_type_t::RC6, 0x431, 20},
    {"NEXT", decode_type_t::RC6, 0x420, 20},
    {"PREV", decode_type_t::RC6, 0x421, 20},
    {"UP", decode_type_t::RC6, 0x458, 20},
    {"DOWN", decode_type_t::RC6, 0x459, 20},
    {"LEFT", decode_type_t::RC6, 0x45A, 20},
    {"RIGHT", decode_type_t::RC6, 0x45B, 20},
    {"OK", decode_type_t::RC6, 0x45C, 20},
    {"BACK", decode_type_t::RC6, 0x483, 20},  // RETURN
    {"EXIT", decode_type_t::RC6, 0x483, 20},  // RETURN

    {"DIGIT_0", decode_type_t::RC6, 0x400, 20},
    {"DIGIT_1", decode_type_t::RC6, 0x401, 20},
    {"DIGIT_2", decode_type_t::RC6, 0x402, 20},
    {"DIGIT_3", decode_type_t::RC6, 0x403, 20},
    {"DIGIT_4", decode_type_t::RC6, 0x404, 20},
    {"DIGIT_5", decode_type_t::RC6, 0x405, 20},
    {"DIGIT_6", decode_type_t::RC6, 0x406, 20},
    {"DIGIT_7", decode_type_t::RC6, 0x407, 20},
    {"DIGIT_8", decode_type_t::RC6, 0x408, 20},
    {"DIGIT_9", decode_type_t::RC6, 0x409, 20},
};

// Toshiba DVD (IRDB: Toshiba/DVD Player/69,-1.csv) - NEC1/NEC 32-bit.
const DvdController::KeyCommand kToshibaDvdCommands1[] = {
    {"POWER", decode_type_t::NEC, 0x45BA12ED, 32},
    {"PLAY_PAUSE", decode_type_t::NEC, 0x45BA15EA, 32},  // PLAY
    {"STOP", decode_type_t::NEC, 0x45BA14EB, 32},
    {"FF", decode_type_t::NEC, 0x45BA13EC, 32},   // FWD >>
    {"REW", decode_type_t::NEC, 0x45BA19E6, 32},  // REV <<
    {"NEXT", decode_type_t::NEC, 0x45BA24DB, 32},  // SKIP >>
    {"PREV", decode_type_t::NEC, 0x45BA23DC, 32},  // SKIP <<

    {"MENU", decode_type_t::NEC, 0x45BA847B, 32},
    {"BACK", decode_type_t::NEC, 0x45BA22DD, 32},  // RETURN
    {"EXIT", decode_type_t::NEC, 0x45BA22DD, 32},  // RETURN
    {"OK", decode_type_t::NEC, 0x45BA21DE, 32},    // ENTER
    {"UP", decode_type_t::NEC, 0x45BA807F, 32},
    {"DOWN", decode_type_t::NEC, 0x45BA817E, 32},
    {"LEFT", decode_type_t::NEC, 0x45BA51AE, 32},
    {"RIGHT", decode_type_t::NEC, 0x45BA4DB2, 32},

    {"TITLE", decode_type_t::NEC, 0x45BA26D9, 32},     // TITLE SEARCH
    {"SUBTITLE", decode_type_t::NEC, 0x45BA28D7, 32},  // SUBTITLE

    {"DIGIT_0", decode_type_t::NEC, 0x45BA0AF5, 32},
    {"DIGIT_1", decode_type_t::NEC, 0x45BA01FE, 32},
    {"DIGIT_2", decode_type_t::NEC, 0x45BA02FD, 32},
    {"DIGIT_3", decode_type_t::NEC, 0x45BA03FC, 32},
    {"DIGIT_4", decode_type_t::NEC, 0x45BA04FB, 32},
    {"DIGIT_5", decode_type_t::NEC, 0x45BA05FA, 32},
    {"DIGIT_6", decode_type_t::NEC, 0x45BA06F9, 32},
    {"DIGIT_7", decode_type_t::NEC, 0x45BA07F8, 32},
    {"DIGIT_8", decode_type_t::NEC, 0x45BA08F7, 32},
    {"DIGIT_9", decode_type_t::NEC, 0x45BA09F6, 32},
};

// JVC DVD (IRDB: JVC/DVD Player/239,-1.csv) - JVC 16-bit.
const DvdController::KeyCommand kJvcDvdCommands1[] = {
    // Values are compatible with IRsend::sendJVC() format.
    {"POWER", decode_type_t::JVC, 0xF702, 16},
    {"EJECT", decode_type_t::JVC, 0xF722, 16},        // OPEN/CLOSE
    {"PLAY_PAUSE", decode_type_t::JVC, 0xF732, 16},   // PLAY
    {"STOP", decode_type_t::JVC, 0xF7C2, 16},
    {"FF", decode_type_t::JVC, 0xF76E, 16},
    {"REW", decode_type_t::JVC, 0xF70E, 16},
    {"NEXT", decode_type_t::JVC, 0xF70D, 16},
    {"PREV", decode_type_t::JVC, 0xF78D, 16},

    {"MENU", decode_type_t::JVC, 0xF7FE, 16},  // SETUP/CHOICE
    {"BACK", decode_type_t::JVC, 0xF792, 16},  // RETURN
    {"EXIT", decode_type_t::JVC, 0xF792, 16},  // RETURN

    {"DIGIT_0", decode_type_t::JVC, 0xF706, 16},
    {"DIGIT_1", decode_type_t::JVC, 0xF786, 16},
    {"DIGIT_2", decode_type_t::JVC, 0xF746, 16},
    {"DIGIT_3", decode_type_t::JVC, 0xF7C6, 16},
    {"DIGIT_4", decode_type_t::JVC, 0xF726, 16},
    {"DIGIT_5", decode_type_t::JVC, 0xF7A6, 16},
    {"DIGIT_6", decode_type_t::JVC, 0xF766, 16},
    {"DIGIT_7", decode_type_t::JVC, 0xF7E6, 16},
    {"DIGIT_8", decode_type_t::JVC, 0xF716, 16},
    {"DIGIT_9", decode_type_t::JVC, 0xF796, 16},
};

// Yamaha DVD (IRDB: Yamaha/DVD Player/124,-1.csv) - NEC1/NEC 32-bit.
const DvdController::KeyCommand kYamahaDvdCommands1[] = {
    {"POWER", decode_type_t::NEC, 0x7C83807F, 32},
    {"EJECT", decode_type_t::NEC, 0x7C83817E, 32},       // OPEN/CLOSE
    {"PLAY_PAUSE", decode_type_t::NEC, 0x7C83827D, 32},  // PLAY
    {"STOP", decode_type_t::NEC, 0x7C83857A, 32},
    {"FF", decode_type_t::NEC, 0x7C838778, 32},
    {"REW", decode_type_t::NEC, 0x7C838679, 32},
    {"NEXT", decode_type_t::NEC, 0x7C83BA45, 32},  // SKIP >>
    {"PREV", decode_type_t::NEC, 0x7C83B946, 32},  // SKIP <<

    {"MENU", decode_type_t::NEC, 0x7C83B24D, 32},
    {"BACK", decode_type_t::NEC, 0x7C83B748, 32},
    {"EXIT", decode_type_t::NEC, 0x7C83B748, 32},
    {"OK", decode_type_t::NEC, 0x7C83B847, 32},
    {"UP", decode_type_t::NEC, 0x7C83B44B, 32},
    {"DOWN", decode_type_t::NEC, 0x7C83B34C, 32},
    {"LEFT", decode_type_t::NEC, 0x7C83B54A, 32},
    {"RIGHT", decode_type_t::NEC, 0x7C83B649, 32},

    {"DIGIT_0", decode_type_t::NEC, 0x7C83936C, 32},
    {"DIGIT_1", decode_type_t::NEC, 0x7C83946B, 32},
    {"DIGIT_2", decode_type_t::NEC, 0x7C83956A, 32},
    {"DIGIT_3", decode_type_t::NEC, 0x7C839669, 32},
    {"DIGIT_4", decode_type_t::NEC, 0x7C839768, 32},
    {"DIGIT_5", decode_type_t::NEC, 0x7C839867, 32},
    {"DIGIT_6", decode_type_t::NEC, 0x7C839966, 32},
    {"DIGIT_7", decode_type_t::NEC, 0x7C839A65, 32},
    {"DIGIT_8", decode_type_t::NEC, 0x7C839B64, 32},
    {"DIGIT_9", decode_type_t::NEC, 0x7C839C63, 32},
};

// Magnavox DVD (IRDB: Magnavox/DVD Player/1,-1.csv) - NEC1/NEC 32-bit.
const DvdController::KeyCommand kMagnavoxDvdCommands1[] = {
    {"POWER", decode_type_t::NEC, 0x01FE16E9, 32},
    {"EJECT", decode_type_t::NEC, 0x01FE1EE1, 32},       // OPEN/CLOSE
    {"PLAY_PAUSE", decode_type_t::NEC, 0x01FE0FF0, 32},
    {"STOP", decode_type_t::NEC, 0x01FE13EC, 32},
    {"NEXT", decode_type_t::NEC, 0x01FE15EA, 32},
    {"PREV", decode_type_t::NEC, 0x01FE1DE2, 32},
    {"MENU", decode_type_t::NEC, 0x01FE5FA0, 32},  // DISC MENU
    {"BACK", decode_type_t::NEC, 0x01FE5EA1, 32},  // RETURN / TITLE
    {"EXIT", decode_type_t::NEC, 0x01FE5EA1, 32},
    {"OK", decode_type_t::NEC, 0x01FE18E7, 32},
    {"UP", decode_type_t::NEC, 0x01FE5BA4, 32},
    {"DOWN", decode_type_t::NEC, 0x01FE19E6, 32},
    {"LEFT", decode_type_t::NEC, 0x01FE1CE3, 32},
    {"RIGHT", decode_type_t::NEC, 0x01FE14EB, 32},

    {"DIGIT_0", decode_type_t::NEC, 0x01FE5AA5, 32},
    {"DIGIT_1", decode_type_t::NEC, 0x01FE1FE0, 32},
    {"DIGIT_2", decode_type_t::NEC, 0x01FE1BE4, 32},
    {"DIGIT_3", decode_type_t::NEC, 0x01FE17E8, 32},
    {"DIGIT_4", decode_type_t::NEC, 0x01FE5CA3, 32},
    {"DIGIT_5", decode_type_t::NEC, 0x01FE58A7, 32},
    {"DIGIT_6", decode_type_t::NEC, 0x01FE54AB, 32},
    {"DIGIT_7", decode_type_t::NEC, 0x01FE5DA2, 32},
    {"DIGIT_8", decode_type_t::NEC, 0x01FE59A6, 32},
    {"DIGIT_9", decode_type_t::NEC, 0x01FE55AA, 32},
};

// Memorex DVD (IRDB: Memorex/DVD Player/0,-1.csv) - NEC1/NEC 32-bit.
const DvdController::KeyCommand kMemorexDvdCommands1[] = {
    {"POWER", decode_type_t::NEC, 0x00FFC53A, 32},
    {"PLAY_PAUSE", decode_type_t::NEC, 0x00FF936C, 32},  // PLAY/SELECT
    {"STOP", decode_type_t::NEC, 0x00FFC936, 32},
    {"FF", decode_type_t::NEC, 0x00FFC837, 32},   // SCAN >>
    {"REW", decode_type_t::NEC, 0x00FF8877, 32},  // SCAN <<
    {"NEXT", decode_type_t::NEC, 0x00FFD22D, 32},  // SKIP >>
    {"PREV", decode_type_t::NEC, 0x00FF906F, 32},  // SKIP <<
    {"MENU", decode_type_t::NEC, 0x00FFC639, 32},
    {"OK", decode_type_t::NEC, 0x00FF936C, 32},  // SELECT
    {"UP", decode_type_t::NEC, 0x00FFD12E, 32},
    {"DOWN", decode_type_t::NEC, 0x00FFD02F, 32},
    {"LEFT", decode_type_t::NEC, 0x00FF906F, 32},
    {"RIGHT", decode_type_t::NEC, 0x00FFD22D, 32},
    {"DIGIT_0", decode_type_t::NEC, 0x00FF8C73, 32},
    {"DIGIT_1", decode_type_t::NEC, 0x00FF817E, 32},
    {"DIGIT_2", decode_type_t::NEC, 0x00FF837C, 32},
    {"DIGIT_3", decode_type_t::NEC, 0x00FFC13E, 32},
    {"DIGIT_4", decode_type_t::NEC, 0x00FF827D, 32},
    {"DIGIT_5", decode_type_t::NEC, 0x00FF807F, 32},
    {"DIGIT_6", decode_type_t::NEC, 0x00FFC03F, 32},
    {"DIGIT_7", decode_type_t::NEC, 0x00FF8D72, 32},
    {"DIGIT_8", decode_type_t::NEC, 0x00FF8F70, 32},
    {"DIGIT_9", decode_type_t::NEC, 0x00FFCD32, 32},
};

const DvdController::KeyCommand kGenericDvdCommands[] = {};

const DvdController::RemoteConfig kDvdRemotes[] = {
    {"", "", 0, kGenericDvdCommands,
     sizeof(kGenericDvdCommands) / sizeof(kGenericDvdCommands[0])},

    {"LG", "DVD", 1, kLgDvdCommands1,
     sizeof(kLgDvdCommands1) / sizeof(kLgDvdCommands1[0])},
    {"Samsung", "DVD", 1, kSamsungDvdCommands1,
     sizeof(kSamsungDvdCommands1) / sizeof(kSamsungDvdCommands1[0])},
    {"Sony", "DVD", 1, kSonyDvdCommands1,
     sizeof(kSonyDvdCommands1) / sizeof(kSonyDvdCommands1[0])},
    {"Sony", "DVD", 2, kSonyDvdCommands2,
     sizeof(kSonyDvdCommands2) / sizeof(kSonyDvdCommands2[0])},
    {"Panasonic", "DVD", 1, kPanasonicDvdCommands1,
     sizeof(kPanasonicDvdCommands1) / sizeof(kPanasonicDvdCommands1[0])},
    {"Philips", "DVD", 1, kPhilipsDvdCommands1,
     sizeof(kPhilipsDvdCommands1) / sizeof(kPhilipsDvdCommands1[0])},
    {"Toshiba", "DVD", 1, kToshibaDvdCommands1,
     sizeof(kToshibaDvdCommands1) / sizeof(kToshibaDvdCommands1[0])},
    {"JVC", "DVD", 1, kJvcDvdCommands1,
     sizeof(kJvcDvdCommands1) / sizeof(kJvcDvdCommands1[0])},
    {"Yamaha", "DVD", 1, kYamahaDvdCommands1,
     sizeof(kYamahaDvdCommands1) / sizeof(kYamahaDvdCommands1[0])},
    {"Magnavox", "DVD", 1, kMagnavoxDvdCommands1,
     sizeof(kMagnavoxDvdCommands1) / sizeof(kMagnavoxDvdCommands1[0])},
    {"Memorex", "DVD", 1, kMemorexDvdCommands1,
     sizeof(kMemorexDvdCommands1) / sizeof(kMemorexDvdCommands1[0])},

    // Generic "try list" for brands without a curated codeset.
    {"", "DVD", 2001, kLgDvdCommands1,
     sizeof(kLgDvdCommands1) / sizeof(kLgDvdCommands1[0])},
    {"", "DVD", 2002, kSamsungDvdCommands1,
     sizeof(kSamsungDvdCommands1) / sizeof(kSamsungDvdCommands1[0])},
    {"", "DVD", 2003, kSonyDvdCommands1,
     sizeof(kSonyDvdCommands1) / sizeof(kSonyDvdCommands1[0])},
    {"", "DVD", 2004, kSonyDvdCommands2,
     sizeof(kSonyDvdCommands2) / sizeof(kSonyDvdCommands2[0])},
    {"", "DVD", 2005, kPanasonicDvdCommands1,
     sizeof(kPanasonicDvdCommands1) / sizeof(kPanasonicDvdCommands1[0])},
    {"", "DVD", 2006, kPhilipsDvdCommands1,
     sizeof(kPhilipsDvdCommands1) / sizeof(kPhilipsDvdCommands1[0])},
    {"", "DVD", 2007, kToshibaDvdCommands1,
     sizeof(kToshibaDvdCommands1) / sizeof(kToshibaDvdCommands1[0])},
    {"", "DVD", 2008, kJvcDvdCommands1,
     sizeof(kJvcDvdCommands1) / sizeof(kJvcDvdCommands1[0])},
    {"", "DVD", 2009, kYamahaDvdCommands1,
     sizeof(kYamahaDvdCommands1) / sizeof(kYamahaDvdCommands1[0])},
    {"", "DVD", 2010, kMagnavoxDvdCommands1,
     sizeof(kMagnavoxDvdCommands1) / sizeof(kMagnavoxDvdCommands1[0])},
    {"", "DVD", 2011, kMemorexDvdCommands1,
     sizeof(kMemorexDvdCommands1) / sizeof(kMemorexDvdCommands1[0])},
};
}  // namespace

const DvdController::RemoteConfig DvdController::kRemotes[] = {
    kDvdRemotes[0],  kDvdRemotes[1],  kDvdRemotes[2],  kDvdRemotes[3],
    kDvdRemotes[4],  kDvdRemotes[5],  kDvdRemotes[6],  kDvdRemotes[7],
    kDvdRemotes[8],  kDvdRemotes[9],  kDvdRemotes[10], kDvdRemotes[11],
    kDvdRemotes[12], kDvdRemotes[13], kDvdRemotes[14], kDvdRemotes[15],
    kDvdRemotes[16], kDvdRemotes[17], kDvdRemotes[18], kDvdRemotes[19],
    kDvdRemotes[20], kDvdRemotes[21], kDvdRemotes[22],
};

DvdController::DvdController(const char *nodeId, uint8_t irPin)
    : stateTopic_(String("iot/nodes/") + nodeId + "/dvd/state"),
      irPin_(irPin),
      irSend_(irPin, IR_SEND_INVERTED, IR_SEND_USE_MODULATION) {}

void DvdController::begin() {
  if (!irReady_) {
    tryBegin(irSend_, 0);
    irReady_ = true;
  }
  Serial.printf("[DVD] Controller ready (IR pin=%u)\n", irPin_);
}

void DvdController::serializeState(JsonDocument &doc) const {
  doc["device"] = deviceType();
  doc["power"] = state_.power;
  doc["muted"] = state_.muted;
  doc["brand"] = remoteBrand_;
  doc["type"] = remoteType_;
  doc["index"] = remoteIndex_;
  doc["updatedAt"] = millis();
}

bool DvdController::handleCommand(JsonObjectConst cmd,
                                  JsonDocument &stateDoc) {
  if (cmd["brand"].is<const char *>())
    remoteBrand_ = cmd["brand"].as<const char *>();
  if (cmd["type"].is<const char *>())
    remoteType_ = cmd["type"].as<const char *>();
  if (cmd["index"].is<uint16_t>())
    remoteIndex_ = cmd["index"].as<uint16_t>();

  String action = cmd["cmd"].as<String>();
  if (action.isEmpty()) {
    Serial.println(F("[DVD] Missing command name"));
    return false;
  }

  bool updated = false;

  if (action.equalsIgnoreCase("key")) {
    const String key = canonicalizeKey(cmd["key"].as<String>());
    if (key.isEmpty()) {
      Serial.println(F("[DVD] Missing key name"));
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
      Serial.printf("[DVD][IR] No IR mapping for brand=%s key=%s\n",
                    remoteBrand_.c_str(), key.c_str());
    }
  }

  if (!updated) {
    return false;
  }

  return applyState(stateDoc);
}

bool DvdController::sendKey(const String &key) {
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
  if (cmd->protocol == decode_type_t::RC6) {
    if (rc6Toggle_) value = irSend_.toggleRC6(value, cmd->nbits);
    rc6Toggle_ = !rc6Toggle_;
  }
  irSend_.send(cmd->protocol, value, cmd->nbits);
  Serial.printf("[DVD][IR] Sent key=%s protocol=%d value=0x%llX bits=%u\n",
                key.c_str(), static_cast<int>(cmd->protocol),
                static_cast<unsigned long long>(value), cmd->nbits);
  return true;
}

bool DvdController::learnKey(const String &key, decode_type_t protocol,
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

void DvdController::saveLearnedCommand(const String &key,
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

bool DvdController::sendLearnedKey(const String &key) {
  const String normalizedKey = canonicalizeKey(key);
  for (const auto &entry : learnedCommands_) {
    if (normalizedKey.equalsIgnoreCase(entry.key) ||
        canonicalizeKey(entry.key).equalsIgnoreCase(normalizedKey)) {
      if (!entry.raw.empty() && entry.nbits > 64) {
        irSend_.send(entry.protocol, entry.raw.data(),
                     static_cast<uint16_t>(entry.raw.size()));
      } else {
        uint64_t value = entry.value;
        if (entry.protocol == decode_type_t::RC6) {
          if (rc6Toggle_) value = irSend_.toggleRC6(value, entry.nbits);
          rc6Toggle_ = !rc6Toggle_;
        }
        irSend_.send(entry.protocol, value, entry.nbits);
      }
      Serial.printf(
          "[DVD][IR] Sent learned key=%s protocol=%d value=0x%llX bits=%u\n",
          normalizedKey.c_str(), static_cast<int>(entry.protocol),
          static_cast<unsigned long long>(entry.value), entry.nbits);
      return true;
    }
  }
  return false;
}

bool DvdController::applyKeyEffects(const String &key) {
  bool changed = false;
  if (key.equalsIgnoreCase("POWER")) {
    state_.power = !state_.power;
    changed = true;
  } else if (key.equalsIgnoreCase("MUTE")) {
    state_.muted = !state_.muted;
    changed = true;
  }
  return changed;
}

bool DvdController::applyState(JsonDocument &stateDoc) {
  // Stateless: do not publish updates
  stateDoc.clear();
  return false;
}

const DvdController::RemoteConfig *DvdController::findRemote(
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

const DvdController::KeyCommand *DvdController::findKey(
    const RemoteConfig *remote, const String &key) {
  if (remote == nullptr) return nullptr;
  for (size_t i = 0; i < remote->commandCount; ++i) {
    if (key.equalsIgnoreCase(remote->commands[i].key)) {
      return &remote->commands[i];
    }
  }
  return nullptr;
}
