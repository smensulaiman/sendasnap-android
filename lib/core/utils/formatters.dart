import 'package:intl/intl.dart';

class Formatters {
  Formatters._();

  static String currency(dynamic amount) {
    if (amount == null) return '¥—';
    final num = double.tryParse(amount.toString()) ?? 0;
    final formatter = NumberFormat('#,###', 'en_US');
    return '¥${formatter.format(num)}';
  }

  static String date(String? raw) {
    if (raw == null || raw.isEmpty) return '—';
    try {
      final dt = DateTime.parse(raw);
      return DateFormat('yyyy/MM/dd').format(dt);
    } catch (_) {
      return raw;
    }
  }

  static String dimension(String? value, String unit) {
    if (value == null || value.isEmpty) return '—';
    return '$value $unit';
  }

  static String initials(String name) {
    final parts = name.trim().split(RegExp(r'\s+'));
    if (parts.isEmpty) return '?';
    if (parts.length == 1) return parts[0][0].toUpperCase();
    return '${parts[0][0]}${parts.last[0]}'.toUpperCase();
  }
}
