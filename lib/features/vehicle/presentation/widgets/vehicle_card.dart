import 'package:flutter/material.dart';
import 'package:cached_network_image/cached_network_image.dart';
import 'package:gap/gap.dart';
import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_dimens.dart';
import '../../../../core/constants/app_text_styles.dart';
import '../../../../core/utils/formatters.dart';
import '../../data/models/vehicle_model.dart';

class VehicleCard extends StatelessWidget {
  final VehicleModel vehicle;
  final VoidCallback? onTap;

  const VehicleCard({super.key, required this.vehicle, this.onTap});

  @override
  Widget build(BuildContext context) {
    final hasImage = vehicle.images != null && vehicle.images!.isNotEmpty;
    final imageUrl = hasImage ? vehicle.images!.first : null;

    return Container(
      margin: const EdgeInsets.only(bottom: AppDimens.md),
      decoration: BoxDecoration(
        color: AppColors.white,
        borderRadius: BorderRadius.circular(AppDimens.cardRadius),
        boxShadow: [
          BoxShadow(
            color: AppColors.primary.withValues(alpha: 0.07),
            blurRadius: 10,
            offset: const Offset(0, 3),
          ),
        ],
      ),
      child: Material(
        color: Colors.transparent,
        borderRadius: BorderRadius.circular(AppDimens.cardRadius),
        child: InkWell(
          borderRadius: BorderRadius.circular(AppDimens.cardRadius),
          onTap: onTap,
          child: Padding(
            padding: const EdgeInsets.all(AppDimens.md),
            child: Row(
              children: [
                _Thumbnail(imageUrl: imageUrl),
                const Gap(AppDimens.md),
                Expanded(child: _VehicleInfo(vehicle: vehicle)),
                const Icon(
                  Icons.chevron_right_rounded,
                  color: AppColors.textHint,
                  size: 20,
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _Thumbnail extends StatelessWidget {
  final String? imageUrl;
  const _Thumbnail({this.imageUrl});

  @override
  Widget build(BuildContext context) {
    if (imageUrl != null) {
      return ClipRRect(
        borderRadius: BorderRadius.circular(AppDimens.sm + 2),
        child: CachedNetworkImage(
          imageUrl: imageUrl!,
          width: 72,
          height: 72,
          fit: BoxFit.cover,
          errorWidget: (_, __, ___) => _placeholder(),
          placeholder: (_, __) => _placeholder(),
        ),
      );
    }
    return _placeholder();
  }

  Widget _placeholder() {
    return Container(
      width: 72,
      height: 72,
      decoration: BoxDecoration(
        color: AppColors.primaryLight,
        borderRadius: BorderRadius.circular(AppDimens.sm + 2),
      ),
      child: const Icon(
        Icons.directions_car_rounded,
        color: AppColors.primary,
        size: 32,
      ),
    );
  }
}

class _VehicleInfo extends StatelessWidget {
  final VehicleModel vehicle;
  const _VehicleInfo({required this.vehicle});

  @override
  Widget build(BuildContext context) {
    final make = vehicle.make ?? '—';
    final model = vehicle.model ?? '';
    final year = vehicle.year;
    final chassis = vehicle.serialNumber;
    final price = vehicle.buyingPrice;
    final imageCount = vehicle.images?.length ?? 0;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          '$make $model'.trim(),
          style: AppTextStyles.titleMedium,
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
        ),
        const Gap(4),
        Row(
          children: [
            if (year != null) ...[
              _Badge(label: year, color: AppColors.primary),
              const Gap(6),
            ],
            if (imageCount > 0)
              _Badge(
                label: '$imageCount photos',
                color: AppColors.textSecondary,
              ),
          ],
        ),
        if (chassis != null) ...[
          const Gap(4),
          Text(
            chassis,
            style: AppTextStyles.monospace.copyWith(fontSize: 11),
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
          ),
        ],
        if (price != null && price.isNotEmpty) ...[
          const Gap(6),
          Text(
            Formatters.currency(price),
            style: AppTextStyles.titleMedium.copyWith(
              color: AppColors.primary,
              fontWeight: FontWeight.w700,
            ),
          ),
        ],
      ],
    );
  }
}

class _Badge extends StatelessWidget {
  final String label;
  final Color color;
  const _Badge({required this.label, required this.color});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(4),
      ),
      child: Text(
        label,
        style: AppTextStyles.labelSmall.copyWith(
          color: color,
          fontWeight: FontWeight.w600,
          fontSize: 10,
        ),
      ),
    );
  }
}
