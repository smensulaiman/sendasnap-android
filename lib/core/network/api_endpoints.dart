class ApiEndpoints {
  ApiEndpoints._();

  static const String baseUrl = 'https://snap.senda.fit/';

  static const String login = 'api/v1/auth/login';
  static const String me = 'api/v1/auth/me';
  static const String changePassword = 'api/v1/auth/change-password';
  static const String vehicleSearch = 'api/v1/vehicles/search';
  static const String vehicleYard = 'api/v1/vehicles/yard';
  static const String vehicleUploadImages = 'api/v1/vehicles/upload-images';
}
