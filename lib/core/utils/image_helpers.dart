import 'dart:io';
import 'package:flutter_image_compress/flutter_image_compress.dart';
import 'package:path_provider/path_provider.dart';

class ImageHelpers {
  ImageHelpers._();

  static const int _maxSizeBytes = 2 * 1024 * 1024; // 2 MB
  static const int _maxDimension = 1920;
  static const int _fallbackDimension = 1200;

  static Future<File> compressIfNeeded(File file) async {
    final size = await file.length();
    if (size <= _maxSizeBytes) return file;

    final dir = await getTemporaryDirectory();
    final ext = file.path.split('.').last;
    final target = '${dir.path}/compressed_${DateTime.now().millisecondsSinceEpoch}.$ext';

    // Try reducing quality first: 85 → 30 in steps of 10
    for (int quality = 85; quality >= 30; quality -= 10) {
      final result = await FlutterImageCompress.compressAndGetFile(
        file.absolute.path,
        target,
        quality: quality,
        minWidth: _maxDimension,
        minHeight: _maxDimension,
        keepExif: false,
      );
      if (result != null) {
        final compressed = File(result.path);
        if (await compressed.length() <= _maxSizeBytes) return compressed;
      }
    }

    // Still too large — resize to max 1200px
    final result = await FlutterImageCompress.compressAndGetFile(
      file.absolute.path,
      '${dir.path}/resized_${DateTime.now().millisecondsSinceEpoch}.$ext',
      quality: 70,
      minWidth: _fallbackDimension,
      minHeight: _fallbackDimension,
      keepExif: false,
    );
    return result != null ? File(result.path) : file;
  }
}
