#include "WifiKnownNetworks.h"

#include <ArduinoJson.h>
#include <Preferences.h>
#include <WiFi.h>
#include <vector>

namespace WifiKnownNetworks {
namespace {

constexpr const char *kPrefsNamespace = "wifi_known";
constexpr const char *kPrefsKeyList = "list";
constexpr size_t kMaxNetworks = 8;

Preferences prefs;
bool initialized = false;

struct Stored {
  String ssid;
  String password;
};

std::vector<Stored> cached;

String normalizeSsid(const String &ssid) {
  String out = ssid;
  out.trim();
  return out;
}

void loadFromPrefs() {
  cached.clear();
  const String json = prefs.getString(kPrefsKeyList, "");
  if (json.isEmpty()) return;

  JsonDocument doc;
  if (deserializeJson(doc, json)) return;
  JsonArray arr = doc.as<JsonArray>();
  for (JsonVariant v : arr) {
    const String ssid = normalizeSsid(String(v["ssid"] | ""));
    if (ssid.isEmpty()) continue;
    const String password = String(v["pw"] | "");
    cached.push_back(Stored{ssid, password});
    if (cached.size() >= kMaxNetworks) break;
  }
}

void saveToPrefs() {
  JsonDocument doc;
  JsonArray arr = doc.to<JsonArray>();
  for (const auto &n : cached) {
    JsonObject o = arr.add<JsonObject>();
    o["ssid"] = n.ssid;
    o["pw"] = n.password;
  }
  String out;
  serializeJson(doc, out);
  prefs.putString(kPrefsKeyList, out);
}

int findIndexBySsid(const String &ssid) {
  const String needle = normalizeSsid(ssid);
  if (needle.isEmpty()) return -1;
  for (size_t i = 0; i < cached.size(); ++i) {
    if (cached[i].ssid == needle) return static_cast<int>(i);
  }
  return -1;
}

void moveToFront(size_t index) {
  if (index == 0 || index >= cached.size()) return;
  auto item = cached[index];
  cached.erase(cached.begin() + index);
  cached.insert(cached.begin(), item);
}

}  // namespace

void begin() {
  if (initialized) return;
  initialized = true;
  prefs.begin(kPrefsNamespace, false);
  loadFromPrefs();
}

void upsert(const String &ssid, const String &password) {
  begin();
  const String normalized = normalizeSsid(ssid);
  if (normalized.isEmpty()) return;

  const int idx = findIndexBySsid(normalized);
  if (idx >= 0) {
    if (password.length() > 0) {
      cached[static_cast<size_t>(idx)].password = password;
    }
    moveToFront(static_cast<size_t>(idx));
  } else {
    cached.insert(cached.begin(), Stored{normalized, password});
    if (cached.size() > kMaxNetworks) {
      cached.resize(kMaxNetworks);
    }
  }
  saveToPrefs();
}

void markUsed(const String &ssid) {
  begin();
  const int idx = findIndexBySsid(ssid);
  if (idx < 0) return;
  moveToFront(static_cast<size_t>(idx));
  saveToPrefs();
}

bool selectBestFromScan(Network &out) {
  begin();
  if (cached.empty()) return false;

  const int n = WiFi.scanNetworks(/*async=*/false, /*hidden=*/true);
  if (n <= 0) {
    WiFi.scanDelete();
    return false;
  }

  int bestRssi = -10000;
  int bestScanIndex = -1;
  int bestKnownIndex = -1;

  for (int i = 0; i < n; ++i) {
    const String ssid = normalizeSsid(WiFi.SSID(i));
    if (ssid.isEmpty()) continue;
    const int knownIdx = findIndexBySsid(ssid);
    if (knownIdx < 0) continue;

    const int rssi = WiFi.RSSI(i);
    if (bestScanIndex == -1 || rssi > bestRssi) {
      bestRssi = rssi;
      bestScanIndex = i;
      bestKnownIndex = knownIdx;
    }
  }

  if (bestKnownIndex < 0) {
    WiFi.scanDelete();
    return false;
  }
  const auto &picked = cached[static_cast<size_t>(bestKnownIndex)];
  out.ssid = picked.ssid;
  out.password = picked.password;
  WiFi.scanDelete();
  return true;
}

size_t count() {
  begin();
  return cached.size();
}

}  // namespace WifiKnownNetworks
