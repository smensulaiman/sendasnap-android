import 'dart:io';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../data/datasources/vehicle_remote_data_source.dart';
import '../../data/models/vehicle_model.dart';
import '../../../../core/errors/app_exception.dart';
import '../../../../core/storage/local_storage.dart';
import '../../../auth/presentation/providers/auth_provider.dart';

String _errorMessage(Object e) =>
    e is AppException ? e.message : e.toString().replaceFirst('Exception: ', '');

// ── Vehicle search ────────────────────────────────────────────────────────

class VehicleSearchState {
  final bool isLoading;
  final List<VehicleModel> results;
  final String? error;

  const VehicleSearchState({
    this.isLoading = false,
    this.results = const [],
    this.error,
  });

  VehicleSearchState copyWith({
    bool? isLoading,
    List<VehicleModel>? results,
    String? error,
    bool clearError = false,
  }) => VehicleSearchState(
    isLoading: isLoading ?? this.isLoading,
    results: results ?? this.results,
    error: clearError ? null : (error ?? this.error),
  );
}

class VehicleSearchNotifier extends StateNotifier<VehicleSearchState> {
  final VehicleRemoteDataSource _dataSource;

  VehicleSearchNotifier({required this._dataSource})
    : super(const VehicleSearchState());

  Future<void> search({
    required String searchType,
    required String query,
    required String company,
  }) async {
    state = state.copyWith(isLoading: true, clearError: true, results: []);
    try {
      final results = await _dataSource.searchVehicles(
        searchType: searchType,
        searchQuery: query,
        company: company,
      );
      state = state.copyWith(isLoading: false, results: results);
    } catch (e) {
      state = state.copyWith(
        isLoading: false,
        error: _errorMessage(e),
      );
    }
  }

  void clear() => state = const VehicleSearchState();
}

// ── Yard vehicles ─────────────────────────────────────────────────────────

class YardVehiclesState {
  final bool isLoading;
  final List<VehicleModel> vehicles;
  final List<VehicleModel> filtered;
  final String? error;
  final String? filterMake;
  final String? filterModel;
  final String? filterYear;
  final String? filterChassis;

  const YardVehiclesState({
    this.isLoading = false,
    this.vehicles = const [],
    this.filtered = const [],
    this.error,
    this.filterMake,
    this.filterModel,
    this.filterYear,
    this.filterChassis,
  });

  YardVehiclesState copyWith({
    bool? isLoading,
    List<VehicleModel>? vehicles,
    List<VehicleModel>? filtered,
    String? error,
    bool clearError = false,
    String? filterMake,
    String? filterModel,
    String? filterYear,
    String? filterChassis,
    bool clearMake = false,
    bool clearModel = false,
    bool clearYear = false,
    bool clearChassis = false,
  }) => YardVehiclesState(
    isLoading: isLoading ?? this.isLoading,
    vehicles: vehicles ?? this.vehicles,
    filtered: filtered ?? this.filtered,
    error: clearError ? null : (error ?? this.error),
    filterMake: clearMake ? null : (filterMake ?? this.filterMake),
    filterModel: clearModel ? null : (filterModel ?? this.filterModel),
    filterYear: clearYear ? null : (filterYear ?? this.filterYear),
    filterChassis: clearChassis ? null : (filterChassis ?? this.filterChassis),
  );
}

class YardVehiclesNotifier extends StateNotifier<YardVehiclesState> {
  final VehicleRemoteDataSource _dataSource;

  YardVehiclesNotifier({required this._dataSource})
    : super(const YardVehiclesState());

  Future<void> loadYard({required int yardId, required String company}) async {
    state = const YardVehiclesState(isLoading: true);
    try {
      final vehicles = await _dataSource.getYardVehicles(
        yardId: yardId,
        company: company,
      );
      state = state.copyWith(
        isLoading: false,
        vehicles: vehicles,
        filtered: vehicles,
      );
    } catch (e) {
      state = state.copyWith(
        isLoading: false,
        error: _errorMessage(e),
      );
    }
  }

  void applyFilter({
    String? make,
    String? model,
    String? year,
    String? chassis,
    bool clearMake = false,
    bool clearModel = false,
    bool clearYear = false,
    bool clearChassis = false,
  }) {
    final next = state.copyWith(
      filterMake: make,
      filterModel: model,
      filterYear: year,
      filterChassis: chassis,
      clearMake: clearMake,
      clearModel: clearModel,
      clearYear: clearYear,
      clearChassis: clearChassis,
    );
    final filtered = _applyFilters(state.vehicles, next);
    state = next.copyWith(filtered: filtered);
  }

  List<VehicleModel> _applyFilters(
    List<VehicleModel> all,
    YardVehiclesState s,
  ) {
    return all.where((v) {
      if (s.filterMake != null &&
          (v.make?.toLowerCase() != s.filterMake!.toLowerCase())) {
        return false;
      }
      if (s.filterModel != null &&
          (v.model?.toLowerCase() != s.filterModel!.toLowerCase())) {
        return false;
      }
      if (s.filterYear != null && v.year != s.filterYear) return false;
      if (s.filterChassis != null &&
          !(v.serialNumber?.toLowerCase().contains(
                s.filterChassis!.toLowerCase(),
              ) ??
              false)) {
        return false;
      }
      return true;
    }).toList();
  }

  List<String> getDistinct(String Function(VehicleModel) selector) =>
      state.vehicles.map(selector).where((s) => s.isNotEmpty).toSet().toList()
        ..sort();
}

// ── Vehicle detail / image upload ─────────────────────────────────────────

class VehicleDetailState {
  final VehicleModel vehicle;
  final List<File> pendingImages;
  final bool isUploading;
  final String? error;
  final String? successMessage;

  const VehicleDetailState({
    required this.vehicle,
    this.pendingImages = const [],
    this.isUploading = false,
    this.error,
    this.successMessage,
  });

  VehicleDetailState copyWith({
    VehicleModel? vehicle,
    List<File>? pendingImages,
    bool? isUploading,
    String? error,
    bool clearError = false,
    String? successMessage,
    bool clearSuccess = false,
  }) => VehicleDetailState(
    vehicle: vehicle ?? this.vehicle,
    pendingImages: pendingImages ?? this.pendingImages,
    isUploading: isUploading ?? this.isUploading,
    error: clearError ? null : (error ?? this.error),
    successMessage: clearSuccess
        ? null
        : (successMessage ?? this.successMessage),
  );
}

class VehicleDetailNotifier extends StateNotifier<VehicleDetailState> {
  final VehicleRemoteDataSource _dataSource;
  final LocalStorage _storage;

  VehicleDetailNotifier({
    required VehicleModel vehicle,
    required this._dataSource,
    required this._storage,
  }) : super(VehicleDetailState(vehicle: vehicle)) {
    _cacheVehicle(vehicle);
  }

  void _cacheVehicle(VehicleModel v) {
    _storage.addRecentVehicleJson(v.toJson());
  }

  void addPendingImages(List<File> files) {
    state = state.copyWith(pendingImages: [...state.pendingImages, ...files]);
  }

  void removePendingImage(int index) {
    final list = List<File>.from(state.pendingImages)..removeAt(index);
    state = state.copyWith(pendingImages: list);
  }

  Future<void> uploadImages() async {
    if (state.pendingImages.isEmpty) return;
    state = state.copyWith(isUploading: true, clearError: true);
    try {
      final company = state.vehicle.company ?? 'acjl';
      final updated = await _dataSource.uploadImages(
        vehicleId: state.vehicle.id,
        company: company,
        images: state.pendingImages,
      );
      _cacheVehicle(updated);
      state = state.copyWith(
        vehicle: updated,
        pendingImages: [],
        isUploading: false,
        successMessage: 'Images uploaded successfully',
      );
    } catch (e) {
      state = state.copyWith(
        isUploading: false,
        error: _errorMessage(e),
      );
    }
  }

  void clearMessages() =>
      state = state.copyWith(clearError: true, clearSuccess: true);
}

// ── Providers ──────────────────────────────────────────────────────────────

final vehicleDataSourceProvider = Provider<VehicleRemoteDataSource>((ref) {
  throw UnimplementedError('Must be overridden in ProviderScope');
});

final vehicleSearchProvider =
    StateNotifierProvider<VehicleSearchNotifier, VehicleSearchState>((ref) {
      return VehicleSearchNotifier(
        dataSource: ref.read(vehicleDataSourceProvider),
      );
    });

final yardVehiclesProvider =
    StateNotifierProvider<YardVehiclesNotifier, YardVehiclesState>((ref) {
      return YardVehiclesNotifier(
        dataSource: ref.read(vehicleDataSourceProvider),
      );
    });

final vehicleDetailProvider =
    StateNotifierProvider.family<
      VehicleDetailNotifier,
      VehicleDetailState,
      VehicleModel
    >((ref, vehicle) {
      return VehicleDetailNotifier(
        vehicle: vehicle,
        dataSource: ref.read(vehicleDataSourceProvider),
        storage: ref.read(localStorageProvider),
      );
    });

final recentVehiclesProvider = Provider<List<VehicleModel>>((ref) {
  final storage = ref.read(localStorageProvider);
  return storage.getRecentVehiclesJson().map(VehicleModel.fromJson).toList();
});
