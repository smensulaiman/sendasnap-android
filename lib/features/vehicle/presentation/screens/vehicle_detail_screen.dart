import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:cached_network_image/cached_network_image.dart';
import 'package:gap/gap.dart';
import 'package:go_router/go_router.dart';
import 'package:image_picker/image_picker.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:photo_view/photo_view.dart';
import 'package:photo_view/photo_view_gallery.dart';
import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_dimens.dart';
import '../../../../core/constants/app_strings.dart';
import '../../../../core/constants/app_text_styles.dart';
import '../../../../core/utils/formatters.dart';
import '../../../../core/utils/image_helpers.dart';
import '../../../../shared/widgets/app_button.dart';
import '../../../../shared/widgets/app_snackbar.dart';
import '../providers/vehicle_provider.dart';
import '../../data/models/vehicle_model.dart';
import '../widgets/label_value_row.dart';

class VehicleDetailScreen extends ConsumerStatefulWidget {
  final VehicleModel vehicle;

  const VehicleDetailScreen({super.key, required this.vehicle});

  @override
  ConsumerState<VehicleDetailScreen> createState() =>
      _VehicleDetailScreenState();
}

class _VehicleDetailScreenState
    extends ConsumerState<VehicleDetailScreen> {
  final _picker = ImagePicker();

  @override
  Widget build(BuildContext context) {
    final detailState =
        ref.watch(vehicleDetailProvider(widget.vehicle));
    final notifier =
        ref.read(vehicleDetailProvider(widget.vehicle).notifier);
    final v = detailState.vehicle;

    // Handle messages
    ref.listen(vehicleDetailProvider(widget.vehicle), (prev, next) {
      if (next.successMessage != null && mounted) {
        AppSnackbar.success(context, next.successMessage!);
        notifier.clearMessages();
      }
      if (next.error != null && mounted) {
        AppSnackbar.error(context, next.error!);
        notifier.clearMessages();
      }
    });

    return Scaffold(
      backgroundColor: AppColors.scaffold,
      appBar: AppBar(
        backgroundColor: AppColors.white,
        surfaceTintColor: AppColors.white,
        elevation: 0,
        title: Text(AppStrings.vehicleDetails,
            style: AppTextStyles.appBarTitle),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_ios_new_rounded,
              color: AppColors.textPrimary, size: 20),
          onPressed: () => context.pop(),
        ),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(AppDimens.lg),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _HeaderCard(vehicle: v),
            const DetailSectionHeader(title: AppStrings.basicInfo),
            InfoCard(children: [
              LabelValueRow(label: AppStrings.make, value: v.make),
              LabelValueRow(label: AppStrings.model, value: v.model),
              LabelValueRow(
                  label: AppStrings.chassisModel, value: v.chassisModel),
              LabelValueRow(label: AppStrings.year, value: v.year),
              LabelValueRow(label: AppStrings.color, value: v.color),
              LabelValueRow(label: AppStrings.engineCc, value: v.cc),
              LabelValueRow(
                  label: AppStrings.plateNumber, value: v.plateNumber),
            ]),
            const DetailSectionHeader(
                title: AppStrings.dimensionsWeight),
            InfoCard(children: [
              LabelValueRow(
                  label: AppStrings.length,
                  value: v.length != null ? '${v.length} mm' : null),
              LabelValueRow(
                  label: AppStrings.width,
                  value: v.width != null ? '${v.width} mm' : null),
              LabelValueRow(
                  label: AppStrings.height,
                  value: v.height != null ? '${v.height} mm' : null),
              LabelValueRow(
                  label: AppStrings.netWeight,
                  value:
                      v.netWeight != null ? '${v.netWeight} kg' : null),
              LabelValueRow(label: AppStrings.area, value: v.area),
            ]),
            const DetailSectionHeader(
                title: AppStrings.purchaseDetails),
            InfoCard(children: [
              LabelValueRow(
                  label: AppStrings.buyDate,
                  value: Formatters.date(v.vehicleBuyDate)),
              LabelValueRow(
                  label: AppStrings.buyingPrice,
                  value: Formatters.currency(v.buyingPrice)),
              LabelValueRow(
                  label: AppStrings.auctionShipNumber,
                  value: v.auctionShipNumber),
            ]),
            const DetailSectionHeader(title: AppStrings.yardInfo),
            InfoCard(children: [
              LabelValueRow(
                  label: AppStrings.expectedYardDate,
                  value: Formatters.date(v.expectedYardDate)),
              LabelValueRow(
                  label: AppStrings.riksoFrom, value: v.riksoFrom),
              LabelValueRow(
                  label: AppStrings.riksoTo, value: v.riksoTo),
              LabelValueRow(
                  label: AppStrings.riksoCost,
                  value: Formatters.currency(v.riksoCost)),
              LabelValueRow(
                  label: AppStrings.riksoCompany,
                  value: v.riksoCompany),
            ]),
            if (v.consigneeDetails != null &&
                v.consigneeDetails!.isNotEmpty) ...[
              const DetailSectionHeader(
                  title: AppStrings.consigneeDetails),
              ...v.consigneeDetails!.map((c) => _ConsigneeCard(c: c)),
            ],
            DetailSectionHeader(
              title: AppStrings.photosLabel(v.images?.length ?? 0),
            ),
            _ImagesGrid(
              images: v.images ?? [],
              onTap: (index) =>
                  _openPhotoViewer(context, v.images!, index),
            ),
            if (detailState.pendingImages.isNotEmpty) ...[
              DetailSectionHeader(
                title: AppStrings.pendingUploadLabel(
                    detailState.pendingImages.length),
              ),
              _PendingImagesGrid(
                images: detailState.pendingImages,
                onRemove: notifier.removePendingImage,
              ),
              const Gap(AppDimens.lg),
              AppButton(
                label: AppStrings.uploadPhotos,
                onPressed:
                    detailState.isUploading ? null : notifier.uploadImages,
                isLoading: detailState.isUploading,
                icon: const Icon(Icons.cloud_upload_outlined,
                    size: 18, color: AppColors.white),
              ),
            ],
            const Gap(AppDimens.xxl),
            AppButton(
              label: AppStrings.addPhotos,
              isOutlined: true,
              onPressed: () => _showImageSourceSheet(context, notifier),
              icon: const Icon(Icons.add_a_photo_outlined,
                  size: 18, color: AppColors.primary),
            ),
            const Gap(AppDimens.xxxl),
          ],
        ),
      ),
    );
  }

  void _showImageSourceSheet(
      BuildContext context, VehicleDetailNotifier notifier) {
    showModalBottomSheet(
      context: context,
      backgroundColor: Colors.transparent,
      builder: (_) => _ImageSourceSheet(
        onCamera: () => _pickImages(notifier, ImageSource.camera),
        onGallery: () => _pickImages(notifier, ImageSource.gallery),
      ),
    );
  }

  Future<void> _pickImages(
      VehicleDetailNotifier notifier, ImageSource source) async {
    final permission = source == ImageSource.camera
        ? Permission.camera
        : Permission.photos;
    final status = await permission.request();
    if (!status.isGranted) return;

    List<XFile> picked = [];
    if (source == ImageSource.camera) {
      final file = await _picker.pickImage(source: ImageSource.camera);
      if (file != null) picked = [file];
    } else {
      picked = await _picker.pickMultiImage();
    }
    if (picked.isEmpty) return;

    final compressed = await Future.wait(
      picked.map((f) => ImageHelpers.compressIfNeeded(File(f.path))),
    );
    notifier.addPendingImages(compressed);
  }

  void _openPhotoViewer(
      BuildContext context, List<String> images, int initialIndex) {
    Navigator.of(context).push(MaterialPageRoute(
      builder: (_) => _PhotoViewerScreen(
        images: images,
        initialIndex: initialIndex,
      ),
    ));
  }
}

// ── Header card ────────────────────────────────────────────────────────────

class _HeaderCard extends StatelessWidget {
  final VehicleModel vehicle;
  const _HeaderCard({required this.vehicle});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(AppDimens.lg),
      decoration: BoxDecoration(
        color: AppColors.primaryLight,
        borderRadius: BorderRadius.circular(AppDimens.cardRadius),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            '${vehicle.make ?? ''} ${vehicle.model ?? ''}'.trim(),
            style: AppTextStyles.headlineMedium.copyWith(fontSize: 22),
          ),
          const Gap(AppDimens.sm),
          Container(
            padding: const EdgeInsets.symmetric(
                horizontal: AppDimens.md, vertical: AppDimens.xs),
            decoration: BoxDecoration(
              color: AppColors.white,
              borderRadius: BorderRadius.circular(AppDimens.sm),
              border: Border.all(color: AppColors.border),
            ),
            child: Text(
              vehicle.serialNumber ?? vehicle.id,
              style: AppTextStyles.monospace,
            ),
          ),
          const Gap(AppDimens.sm),
          Wrap(
            spacing: AppDimens.sm,
            children: [
              if (vehicle.year != null)
                _Tag(label: vehicle.year!),
              if (vehicle.color != null)
                _Tag(label: vehicle.color!),
            ],
          ),
        ],
      ),
    );
  }
}

class _Tag extends StatelessWidget {
  final String label;
  const _Tag({required this.label});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(
          horizontal: AppDimens.sm, vertical: 4),
      decoration: BoxDecoration(
        color: AppColors.primary.withOpacity(0.1),
        borderRadius: BorderRadius.circular(20),
      ),
      child: Text(label,
          style: AppTextStyles.labelSmall
              .copyWith(color: AppColors.primary, fontWeight: FontWeight.w600)),
    );
  }
}

// ── Images grid ────────────────────────────────────────────────────────────

class _ImagesGrid extends StatelessWidget {
  final List<String> images;
  final ValueChanged<int> onTap;

  const _ImagesGrid({required this.images, required this.onTap});

  @override
  Widget build(BuildContext context) {
    if (images.isEmpty) {
      return Container(
        height: 100,
        alignment: Alignment.center,
        decoration: BoxDecoration(
          color: AppColors.primaryLight,
          borderRadius: BorderRadius.circular(AppDimens.cardRadius),
        ),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(Icons.photo_library_outlined,
                color: AppColors.textHint, size: 32),
            const Gap(AppDimens.xs),
            Text(AppStrings.noPhotosYet,
                style: AppTextStyles.bodySmall),
          ],
        ),
      );
    }

    return GridView.builder(
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
        crossAxisCount: 3,
        mainAxisSpacing: AppDimens.sm,
        crossAxisSpacing: AppDimens.sm,
        childAspectRatio: 1,
      ),
      itemCount: images.length,
      itemBuilder: (_, i) => GestureDetector(
        onTap: () => onTap(i),
        child: ClipRRect(
          borderRadius: BorderRadius.circular(AppDimens.sm),
          child: Hero(
            tag: 'vehicle_image_$i',
            child: CachedNetworkImage(
              imageUrl: images[i],
              fit: BoxFit.cover,
              placeholder: (_, __) => Container(
                  color: AppColors.primaryLight),
              errorWidget: (_, __, ___) =>
                  Container(
                color: AppColors.primaryLight,
                child: const Icon(Icons.broken_image_outlined,
                    color: AppColors.textHint),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

// ── Pending images grid ────────────────────────────────────────────────────

class _PendingImagesGrid extends StatelessWidget {
  final List<File> images;
  final ValueChanged<int> onRemove;

  const _PendingImagesGrid(
      {required this.images, required this.onRemove});

  @override
  Widget build(BuildContext context) {
    return GridView.builder(
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
        crossAxisCount: 3,
        mainAxisSpacing: AppDimens.sm,
        crossAxisSpacing: AppDimens.sm,
        childAspectRatio: 1,
      ),
      itemCount: images.length,
      itemBuilder: (_, i) => Stack(
        children: [
          ClipRRect(
            borderRadius: BorderRadius.circular(AppDimens.sm),
            child: Image.file(images[i],
                width: double.infinity,
                height: double.infinity,
                fit: BoxFit.cover),
          ),
          Positioned(
            top: 4,
            right: 4,
            child: GestureDetector(
              onTap: () => onRemove(i),
              child: Container(
                width: 22,
                height: 22,
                decoration: const BoxDecoration(
                  color: AppColors.error,
                  shape: BoxShape.circle,
                ),
                child: const Icon(Icons.close_rounded,
                    color: AppColors.white, size: 14),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

// ── Consignee card ─────────────────────────────────────────────────────────

class _ConsigneeCard extends StatelessWidget {
  final dynamic c;
  const _ConsigneeCard({required this.c});

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(bottom: AppDimens.sm),
      padding: const EdgeInsets.all(AppDimens.md),
      decoration: BoxDecoration(
        color: AppColors.primaryLight,
        borderRadius: BorderRadius.circular(AppDimens.cardRadius),
        border: Border.all(color: AppColors.border),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (c.name != null)
            Text(c.name!, style: AppTextStyles.titleMedium),
          if (c.address != null) ...[
            const Gap(2),
            Text(c.address!, style: AppTextStyles.bodySmall),
          ],
          if (c.phone != null) ...[
            const Gap(2),
            Row(
              children: [
                const Icon(Icons.phone_outlined,
                    size: 12, color: AppColors.textSecondary),
                const Gap(4),
                Text(c.phone!, style: AppTextStyles.bodySmall),
              ],
            ),
          ],
          if (c.email != null) ...[
            const Gap(2),
            Row(
              children: [
                const Icon(Icons.email_outlined,
                    size: 12, color: AppColors.textSecondary),
                const Gap(4),
                Text(c.email!, style: AppTextStyles.bodySmall),
              ],
            ),
          ],
        ],
      ),
    );
  }
}

// ── Image source sheet ─────────────────────────────────────────────────────

class _ImageSourceSheet extends StatelessWidget {
  final VoidCallback onCamera;
  final VoidCallback onGallery;

  const _ImageSourceSheet(
      {required this.onCamera, required this.onGallery});

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: const BoxDecoration(
        color: AppColors.white,
        borderRadius: BorderRadius.vertical(
            top: Radius.circular(AppDimens.bottomSheetRadius)),
      ),
      child: SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              margin: const EdgeInsets.only(top: 12),
              width: 36,
              height: 4,
              decoration: BoxDecoration(
                color: AppColors.border,
                borderRadius: BorderRadius.circular(2),
              ),
            ),
            Padding(
              padding: const EdgeInsets.all(AppDimens.lg),
              child: Text(AppStrings.addVehiclePhotos,
                  style: AppTextStyles.titleLarge),
            ),
            const Divider(color: AppColors.divider, height: 1),
            ListTile(
              leading: const Icon(Icons.camera_alt_rounded,
                  color: AppColors.primary),
              title: Text(AppStrings.takePhoto,
                  style: AppTextStyles.bodyMedium),
              onTap: () {
                Navigator.pop(context);
                onCamera();
              },
            ),
            ListTile(
              leading: const Icon(Icons.photo_library_rounded,
                  color: AppColors.primary),
              title: Text(AppStrings.chooseGallery,
                  style: AppTextStyles.bodyMedium),
              onTap: () {
                Navigator.pop(context);
                onGallery();
              },
            ),
            Padding(
              padding: const EdgeInsets.all(AppDimens.lg),
              child: AppButton(
                label: AppStrings.cancel,
                isOutlined: true,
                onPressed: () => Navigator.pop(context),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

// ── Full-screen photo viewer ───────────────────────────────────────────────

class _PhotoViewerScreen extends StatefulWidget {
  final List<String> images;
  final int initialIndex;

  const _PhotoViewerScreen(
      {required this.images, required this.initialIndex});

  @override
  State<_PhotoViewerScreen> createState() => _PhotoViewerScreenState();
}

class _PhotoViewerScreenState extends State<_PhotoViewerScreen> {
  late int _current;

  @override
  void initState() {
    super.initState();
    _current = widget.initialIndex;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      appBar: AppBar(
        backgroundColor: Colors.black,
        foregroundColor: AppColors.white,
        title: Text(
          '${_current + 1} / ${widget.images.length}',
          style: AppTextStyles.bodyMedium.copyWith(color: AppColors.white),
        ),
      ),
      body: PhotoViewGallery.builder(
        itemCount: widget.images.length,
        pageController: PageController(initialPage: widget.initialIndex),
        onPageChanged: (i) => setState(() => _current = i),
        builder: (_, i) => PhotoViewGalleryPageOptions(
          imageProvider:
              CachedNetworkImageProvider(widget.images[i]),
          heroAttributes: PhotoViewHeroAttributes(
              tag: 'vehicle_image_$i'),
          minScale: PhotoViewComputedScale.contained,
          maxScale: PhotoViewComputedScale.covered * 3,
        ),
        backgroundDecoration:
            const BoxDecoration(color: Colors.black),
      ),
    );
  }
}
