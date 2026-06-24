import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../data/datasources/auth_remote_data_source.dart';
import '../../data/models/user_model.dart';
import '../../data/models/vendor_model.dart';
import '../../../../core/storage/local_storage.dart';

class AuthState {
  final bool isLoggedIn;
  final UserModel? user;
  final VendorModel? vendor;
  final bool isLoading;
  final String? error;

  const AuthState({
    this.isLoggedIn = false,
    this.user,
    this.vendor,
    this.isLoading = false,
    this.error,
  });

  AuthState copyWith({
    bool? isLoggedIn,
    UserModel? user,
    VendorModel? vendor,
    bool? isLoading,
    String? error,
    bool clearError = false,
  }) =>
      AuthState(
        isLoggedIn: isLoggedIn ?? this.isLoggedIn,
        user: user ?? this.user,
        vendor: vendor ?? this.vendor,
        isLoading: isLoading ?? this.isLoading,
        error: clearError ? null : (error ?? this.error),
      );
}

class AuthNotifier extends StateNotifier<AuthState> {
  final LocalStorage _storage;
  final AuthRemoteDataSource _dataSource;

  AuthNotifier({
    required LocalStorage storage,
    required AuthRemoteDataSource dataSource,
  })  : _storage = storage,
        _dataSource = dataSource,
        super(const AuthState()) {
    _restoreSession();
  }

  void _restoreSession() {
    final loggedIn = _storage.isLoggedIn();
    if (!loggedIn) return;

    final userJson = _storage.getUserJson();
    final vendorJson = _storage.getVendorJson();

    if (userJson == null) return;

    state = AuthState(
      isLoggedIn: true,
      user: UserModel.fromJson(userJson),
      vendor: vendorJson != null ? VendorModel.fromJson(vendorJson) : null,
    );
  }

  Future<void> login(
    String email,
    String password, {
    bool rememberMe = false,
  }) async {
    state = state.copyWith(isLoading: true, clearError: true);
    try {
      final result = await _dataSource.login(email, password);
      await _storage.saveToken(result.token);
      await _storage.setLoggedIn(true);
      await _storage.saveUserJson(result.user.toJson());
      if (result.vendor != null) {
        await _storage.saveVendorJson(result.vendor!.toJson());
      }
      await _storage.saveRememberMe(
        remember: rememberMe,
        email: rememberMe ? email : null,
        password: rememberMe ? password : null,
      );

      state = AuthState(
        isLoggedIn: true,
        user: result.user,
        vendor: result.vendor,
      );
    } catch (e) {
      state = state.copyWith(
        isLoading: false,
        error: e.toString().replaceFirst('Exception: ', ''),
      );
      rethrow;
    }
  }

  Future<void> logout() async {
    await _storage.clearAll();
    state = const AuthState();
  }

  Future<void> changePassword({
    required String currentPassword,
    required String newPassword,
    required String confirmPassword,
  }) async {
    state = state.copyWith(isLoading: true, clearError: true);
    try {
      await _dataSource.changePassword(
        currentPassword: currentPassword,
        newPassword: newPassword,
        confirmPassword: confirmPassword,
      );
      state = state.copyWith(isLoading: false);
    } catch (e) {
      state = state.copyWith(
        isLoading: false,
        error: e.toString().replaceFirst('Exception: ', ''),
      );
      rethrow;
    }
  }

  void triggerHaptic() {
    if (_storage.isHapticEnabled()) HapticFeedback.lightImpact();
  }

  void clearError() => state = state.copyWith(clearError: true);

  void updateSettings({bool? haptic, bool? notifications}) {
    if (haptic != null) _storage.setHapticEnabled(haptic);
    if (notifications != null) _storage.setNotificationsEnabled(notifications);
  }

  bool get hapticEnabled => _storage.isHapticEnabled();
  bool get notificationsEnabled => _storage.isNotificationsEnabled();
}

// ── Providers ──────────────────────────────────────────────────────────────

final localStorageProvider = Provider<LocalStorage>((ref) {
  throw UnimplementedError('Must be overridden in ProviderScope');
});

final authDataSourceProvider = Provider<AuthRemoteDataSource>((ref) {
  throw UnimplementedError('Must be overridden in ProviderScope');
});

final authProvider = StateNotifierProvider<AuthNotifier, AuthState>((ref) {
  return AuthNotifier(
    storage: ref.read(localStorageProvider),
    dataSource: ref.read(authDataSourceProvider),
  );
});
