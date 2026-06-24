import 'dart:io';
import 'package:dio/dio.dart';
import '../../../../core/errors/app_exception.dart';
import '../../../../core/network/api_endpoints.dart';
import '../../../../core/network/dio_client.dart';
import '../models/vehicle_model.dart';

class VehicleRemoteDataSource {
  final DioClient _client;

  VehicleRemoteDataSource({required this._client});

  Future<List<VehicleModel>> searchVehicles({
    required String searchType,
    required String searchQuery,
    required String company,
  }) async {
    try {
      final res = await _client.get(
        ApiEndpoints.vehicleSearch,
        queryParameters: {
          'search_type': searchType,
          'search_query': searchQuery,
          'company': company,
        },
      );
      final data =
          (res.data as Map<String, dynamic>)['data'] as Map<String, dynamic>? ??
          {};
      final companyValue = data['company'] as String? ?? company;
      final list = data['vehicles'] as List? ?? [];
      return list
          .map(
            (v) => VehicleModel.fromJson({
              ...(v as Map<String, dynamic>),
              'company': companyValue,
            }),
          )
          .toList();
    } on DioException catch (e) {
      throw _toAppException(e, 'Search failed');
    }
  }

  Future<List<VehicleModel>> getYardVehicles({
    required int yardId,
    required String company,
  }) async {
    try {
      final res = await _client.post(
        ApiEndpoints.vehicleYard,
        data: {'yard_id': yardId, 'company': company},
      );
      final data =
          (res.data as Map<String, dynamic>)['data'] as Map<String, dynamic>? ??
          {};
      final list = data['vehicles'] as List? ?? [];
      return list
          .map(
            (v) => VehicleModel.fromJson({
              ...(v as Map<String, dynamic>),
              'company': company,
            }),
          )
          .toList();
    } on DioException catch (e) {
      throw _toAppException(e, 'Failed to load yard vehicles');
    }
  }

  Future<VehicleModel> uploadImages({
    required String vehicleId,
    required String company,
    required List<File> images,
  }) async {
    try {
      final formData = FormData();
      formData.fields
        ..add(MapEntry('vehicle_id', vehicleId))
        ..add(MapEntry('company', company));
      for (final file in images) {
        final name = file.path.split('/').last;
        formData.files.add(
          MapEntry(
            'images[]',
            await MultipartFile.fromFile(file.path, filename: name),
          ),
        );
      }
      final res = await _client.post(
        ApiEndpoints.vehicleUploadImages,
        data: formData,
        options: Options(contentType: 'multipart/form-data'),
      );
      final data =
          (res.data as Map<String, dynamic>)['data'] as Map<String, dynamic>? ??
          {};
      final vehicleData = data['vehicle'] as Map<String, dynamic>? ?? {};
      return VehicleModel.fromJson({...vehicleData, 'company': company});
    } on DioException catch (e) {
      throw _toAppException(e, 'Upload failed');
    }
  }

  AppException _toAppException(DioException e, String fallback) {
    final isNetworkError = e.response == null ||
        e.type == DioExceptionType.connectionError ||
        e.type == DioExceptionType.connectionTimeout ||
        e.type == DioExceptionType.receiveTimeout ||
        e.type == DioExceptionType.sendTimeout;

    if (isNetworkError) {
      return const NetworkException(
        message: 'No internet connection. Please check your network.',
      );
    }
    return ServerException(
      message: _extractMessage(e) ?? fallback,
      statusCode: e.response?.statusCode,
    );
  }

  String? _extractMessage(DioException e) {
    try {
      final data = e.response?.data;
      if (data is Map<String, dynamic>) return data['message'] as String?;
    } catch (_) {}
    return null;
  }
}
