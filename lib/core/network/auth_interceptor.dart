import 'package:dio/dio.dart';
import '../storage/local_storage.dart';
import '../network/api_endpoints.dart';

class AuthInterceptor extends Interceptor {
  final LocalStorage _storage;
  final void Function() _onUnauthorized;

  AuthInterceptor({required this._storage, required this._onUnauthorized});

  @override
  Future<void> onRequest(
    RequestOptions options,
    RequestInterceptorHandler handler,
  ) async {
    final isLoginPath = options.path.contains(ApiEndpoints.login);
    if (!isLoginPath) {
      final token = await _storage.getToken();
      if (token != null) {
        options.headers['Authorization'] = 'Bearer $token';
      }
    }
    handler.next(options);
  }

  @override
  void onError(DioException err, ErrorInterceptorHandler handler) {
    if (err.response?.statusCode == 401) {
      _onUnauthorized();
    }
    handler.next(err);
  }
}
