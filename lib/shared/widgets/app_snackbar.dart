import 'package:flutter/material.dart';
import '../../core/constants/app_colors.dart';
import '../../core/constants/app_dimens.dart';
import '../../core/constants/app_text_styles.dart';

class AppSnackbar {
  AppSnackbar._();

  static void show(
    BuildContext context, {
    required String message,
    bool isError = false,
    bool isSuccess = false,
    Duration duration = const Duration(seconds: 3),
  }) {
    final color = isError
        ? AppColors.error
        : isSuccess
            ? AppColors.success
            : AppColors.primary;
    final bgColor = isError
        ? AppColors.errorLight
        : isSuccess
            ? AppColors.successLight
            : AppColors.primaryLight;

    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message,
            style: AppTextStyles.bodyMedium.copyWith(color: color)),
        backgroundColor: bgColor,
        behavior: SnackBarBehavior.floating,
        margin: const EdgeInsets.all(AppDimens.lg),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(AppDimens.md),
          side: BorderSide(color: color.withOpacity(0.3)),
        ),
        duration: duration,
      ),
    );
  }

  static void error(BuildContext context, String message) =>
      show(context, message: message, isError: true);

  static void success(BuildContext context, String message) =>
      show(context, message: message, isSuccess: true);
}
