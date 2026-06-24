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
    final hasImage =
        vehicle.images != null && vehicle.images!.isNotEmpty;
    final imageUrl = hasImage ? vehicle.images!.first : null;

    return Material(
      color: AppColors.primaryLight,
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
            ],
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
        borderRadius: BorderRadius.circular(AppDimens.sm),
        child: CachedNetworkImage(
          imageUrl: imageUrl!,
          width: 60,
          height: 60,
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
      width: 60,
      height: 60,
      decoration: BoxDecoration(
        color: AppColors.primary.withOpacity(0.12),
        borderRadius: BorderRadius.circular(AppDimens.sm),
      ),
      child: const Icon(Icons.directions_car_rounded,
          color: AppColors.primary, size: 28),
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
    final year = vehicle.year ?? '—';
    final chassis = vehicle.serialNumber ?? '—';
    final color = vehicle.color ?? '—';
    final cc = vehicle.cc ?? '—';
    final price = vehicle.buyingPrice;
    final imageCount = vehicle.images?.length ?? 0;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Expanded(
              child: Text(
                '$make $model'.trim(),
                style: AppTextStyles.titleMedium,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
              ),
            ),
          ],
        ),
        const Gap(2),
        Text(
          '$make · $year',
          style: AppTextStyles.bodySmall,
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
        ),
        Text(
          'Chassis: $chassis',
          style: AppTextStyles.bodySmall,
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
        ),
        Text(
          'Color: $color   CC: $cc',
          style: AppTextStyles.bodySmall,
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
        ),
        const Gap(AppDimens.xs),
        const Divider(color: AppColors.border, height: 1),
        const Gap(AppDimens.xs),
        Row(
          children: [
            if (price != null && price.isNotEmpty)
              Expanded(
                child: Text(
                  Formatters.currency(price),
                  style: AppTextStyles.titleMedium
                      .copyWith(color: AppColors.primary),
                ),
              ),
            if (imageCount > 0)
              Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  const Icon(Icons.photo_library_outlined,
                      size: 14, color: AppColors.textSecondary),
                  const Gap(2),
                  Text(
                    '$imageCount',
                    style: AppTextStyles.labelSmall,
                  ),
                ],
              ),
          ],
        ),
      ],
    );
  }
}
