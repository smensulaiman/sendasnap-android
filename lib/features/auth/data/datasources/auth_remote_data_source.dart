import 'package:dio/dio.dart';
import '../../../../core/errors/app_exception.dart';
import '../../../../core/network/api_endpoints.dart';
import '../../../../core/network/dio_client.dart';
import '../models/user_model.dart';
import '../models/vendor_model.dart';

class AuthResult {
  final UserModel user;
  final VendorModel? vendor;
  final String token;

  const AuthResult({required this.user, this.vendor, required this.token});
}

class AuthRemoteDataSource {
  final DioClient _client;

  AuthRemoteDataSource({required this._client});

  Future<AuthResult> login(String email, String password) async {
    try {
      final res = await _client.post(
        ApiEndpoints.login,
        data: {'email': email, 'password': password},
      );
      return _parseAuthResult(res.data as Map<String, dynamic>);
    } on DioException catch (e) {
      final msg = _extractMessage(e) ?? 'Login failed';
      throw AuthException(message: msg, statusCode: e.response?.statusCode);
    }
  }

  Future<AuthResult> getMe() async {
    try {
      final res = await _client.get(ApiEndpoints.me);
      return _parseAuthResult(res.data as Map<String, dynamic>);
    } on DioException catch (e) {
      final msg = _extractMessage(e) ?? 'Failed to get profile';
      throw ServerException(message: msg, statusCode: e.response?.statusCode);
    }
  }

  Future<void> changePassword({
    required String currentPassword,
    required String newPassword,
    required String confirmPassword,
  }) async {
    try {
      await _client.post(
        ApiEndpoints.changePassword,
        data: {
          'current_password': currentPassword,
          'new_password': newPassword,
          'new_password_confirmation': confirmPassword,
        },
      );
    } on DioException catch (e) {
      final msg = _extractMessage(e) ?? 'Failed to change password';
      throw ServerException(message: msg, statusCode: e.response?.statusCode);
    }
  }

  AuthResult _parseAuthResult(Map<String, dynamic> body) {
    final data = body['data'] as Map<String, dynamic>? ?? {};
    final userData = data['user'] as Map<String, dynamic>? ?? {};
    final vendorData = data['vendor'] as Map<String, dynamic>?;
    final token = data['token'] as String? ?? '';

    return AuthResult(
      user: UserModel.fromJson(userData),
      vendor: vendorData != null ? VendorModel.fromJson(vendorData) : null,
      token: token,
    );
  }

  String? _extractMessage(DioException e) {
    try {
      final data = e.response?.data;
      if (data is Map<String, dynamic>) return data['message'] as String?;
    } catch (_) {}
    return e.message;
  }
}
