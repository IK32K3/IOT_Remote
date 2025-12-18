#pragma once

#include <Arduino.h>

namespace WifiKnownNetworks {

struct Network {
  String ssid;
  String password;
};

void begin();

// Adds or updates an SSID/password entry and persists it.
void upsert(const String &ssid, const String &password);

// Reorders existing entry as most-recent (does not change password).
void markUsed(const String &ssid);

// Finds a known SSID from a Wi-Fi scan and returns the best candidate to
// connect to (by RSSI).
bool selectBestFromScan(Network &out);

// Debug helper.
size_t count();

}  // namespace WifiKnownNetworks

