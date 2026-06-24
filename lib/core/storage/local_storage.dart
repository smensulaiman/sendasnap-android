import 'dart:convert';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:shared_preferences/shared_preferences.dart';

class LocalStorage {
  final FlutterSecureStorage _secure;
  final SharedPreferences _prefs;

  LocalStorage({required this._secure, required this._prefs});

  // ── Keys ──────────────────────────────────────────────────────────────────
  static const _keyToken = 'auth_token';
  static const _keySavedPassword = 'saved_password';
  static const _keyIsLoggedIn = 'is_logged_in';
  static const _keyUserData = 'user_data';
  static const _keyVendorData = 'vendor_data';
  static const _keyRecentVehicles = 'recent_vehicles';
  static const _keyCacheSize = 'cache_size';
  static const _keyHapticEnabled = 'haptic_enabled';
  static const _keyNotificationsEnabled = 'notifications_enabled';
  static const _keyRememberMe = 'remember_me';
  static const _keySavedEmail = 'saved_email';

  // ── Auth token ─────────────────────────────────────────────────────────────
  Future<void> saveToken(String token) async =>
      _secure.write(key: _keyToken, value: token);

  Future<String?> getToken() async => _secure.read(key: _keyToken);

  Future<void> deleteToken() async => _secure.delete(key: _keyToken);

  // ── Login state ────────────────────────────────────────────────────────────
  Future<void> setLoggedIn(bool value) async =>
      _prefs.setBool(_keyIsLoggedIn, value);

  bool isLoggedIn() => _prefs.getBool(_keyIsLoggedIn) ?? false;

  // ── User & Vendor ──────────────────────────────────────────────────────────
  Future<void> saveUserJson(Map<String, dynamic> json) async =>
      _prefs.setString(_keyUserData, jsonEncode(json));

  Map<String, dynamic>? getUserJson() {
    final raw = _prefs.getString(_keyUserData);
    if (raw == null) return null;
    return jsonDecode(raw) as Map<String, dynamic>;
  }

  Future<void> saveVendorJson(Map<String, dynamic> json) async =>
      _prefs.setString(_keyVendorData, jsonEncode(json));

  Map<String, dynamic>? getVendorJson() {
    final raw = _prefs.getString(_keyVendorData);
    if (raw == null) return null;
    return jsonDecode(raw) as Map<String, dynamic>;
  }

  // ── Recent vehicles ────────────────────────────────────────────────────────
  List<Map<String, dynamic>> getRecentVehiclesJson() {
    final raw = _prefs.getString(_keyRecentVehicles);
    if (raw == null) return [];
    final list = jsonDecode(raw) as List;
    return list.cast<Map<String, dynamic>>();
  }

  Future<void> addRecentVehicleJson(Map<String, dynamic> vehicleJson) async {
    final id = vehicleJson['id']?.toString() ?? '';
    final list = getRecentVehiclesJson();
    list.removeWhere((v) => v['id']?.toString() == id);
    list.insert(0, vehicleJson);
    if (list.length > 100) list.removeLast();
    await _prefs.setString(_keyRecentVehicles, jsonEncode(list));
    await _prefs.setInt(_keyCacheSize, list.length);
  }

  Future<void> clearRecentVehicles() async {
    await _prefs.remove(_keyRecentVehicles);
    await _prefs.setInt(_keyCacheSize, 0);
  }

  int getCacheSize() => _prefs.getInt(_keyCacheSize) ?? 0;

  // ── Settings ───────────────────────────────────────────────────────────────
  bool isHapticEnabled() => _prefs.getBool(_keyHapticEnabled) ?? true;
  bool isNotificationsEnabled() =>
      _prefs.getBool(_keyNotificationsEnabled) ?? true;

  Future<void> setHapticEnabled(bool value) async =>
      _prefs.setBool(_keyHapticEnabled, value);

  Future<void> setNotificationsEnabled(bool value) async =>
      _prefs.setBool(_keyNotificationsEnabled, value);

  // ── Remember Me ────────────────────────────────────────────────────────────
  bool isRememberMe() => _prefs.getBool(_keyRememberMe) ?? false;
  String? getSavedEmail() => _prefs.getString(_keySavedEmail);

  Future<String?> getSavedPassword() async =>
      _secure.read(key: _keySavedPassword);

  Future<void> saveRememberMe({
    required bool remember,
    String? email,
    String? password,
  }) async {
    await _prefs.setBool(_keyRememberMe, remember);
    if (remember && email != null) {
      await _prefs.setString(_keySavedEmail, email);
    }
    if (remember && password != null) {
      await _secure.write(key: _keySavedPassword, value: password);
    }
    if (!remember) {
      await _prefs.remove(_keySavedEmail);
      await _secure.delete(key: _keySavedPassword);
    }
  }

  // ── Clear all ──────────────────────────────────────────────────────────────
  Future<void> clearAll() async {
    await _prefs.clear();
    await _secure.deleteAll();
  }
}
