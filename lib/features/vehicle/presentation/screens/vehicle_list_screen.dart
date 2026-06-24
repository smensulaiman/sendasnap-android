import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:gap/gap.dart';
import 'package:go_router/go_router.dart';
import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_dimens.dart';
import '../../../../core/constants/app_strings.dart';
import '../../../../core/constants/app_text_styles.dart';
import '../../../../shared/widgets/empty_state.dart';
import '../../../../shared/widgets/shimmer_card.dart';
import '../providers/vehicle_provider.dart';
import '../widgets/vehicle_card.dart';

class VehicleListScreen extends ConsumerWidget {
  final String yardName;
  final String company;

  const VehicleListScreen({
    super.key,
    required this.yardName,
    required this.company,
  });

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(yardVehiclesProvider);
    final total = state.vehicles.length;
    final shown = state.filtered.length;

    return Scaffold(
      backgroundColor: AppColors.scaffold,
      appBar: AppBar(
        backgroundColor: AppColors.white,
        surfaceTintColor: AppColors.white,
        elevation: 0,
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(yardName, style: AppTextStyles.appBarTitle),
            if (!state.isLoading)
              Text(
                AppStrings.vehiclesShownOf(shown, total),
                style: AppTextStyles.labelSmall,
              ),
          ],
        ),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_ios_new_rounded,
              color: AppColors.textPrimary, size: 20),
          onPressed: () => context.pop(),
        ),
        bottom: PreferredSize(
          preferredSize: const Size.fromHeight(56),
          child: _FilterRow(company: company),
        ),
      ),
      body: _Body(state: state),
    );
  }
}

class _FilterRow extends ConsumerWidget {
  final String company;
  const _FilterRow({required this.company});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(yardVehiclesProvider);
    final notifier = ref.read(yardVehiclesProvider.notifier);

    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      padding:
          const EdgeInsets.symmetric(horizontal: AppDimens.lg, vertical: 8),
      child: Row(
        children: [
          _FilterChip(
            label: AppStrings.make,
            activeValue: state.filterMake,
            onTap: () => _showPicker(
              context,
              title: AppStrings.make,
              options: notifier.getDistinct((v) => v.make ?? ''),
              onSelected: (v) =>
                  notifier.applyFilter(make: v, clearMake: v == null),
            ),
          ),
          const Gap(AppDimens.sm),
          _FilterChip(
            label: AppStrings.model,
            activeValue: state.filterModel,
            onTap: () => _showPicker(
              context,
              title: AppStrings.model,
              options: notifier.getDistinct((v) => v.model ?? ''),
              onSelected: (v) =>
                  notifier.applyFilter(model: v, clearModel: v == null),
            ),
          ),
          const Gap(AppDimens.sm),
          _FilterChip(
            label: AppStrings.year,
            activeValue: state.filterYear,
            onTap: () => _showPicker(
              context,
              title: AppStrings.year,
              options: notifier.getDistinct((v) => v.year ?? ''),
              onSelected: (v) =>
                  notifier.applyFilter(year: v, clearYear: v == null),
            ),
          ),
          const Gap(AppDimens.sm),
          _FilterChip(
            label: AppStrings.chassis,
            activeValue: state.filterChassis,
            onTap: () => _showChassisFilter(context, notifier),
          ),
        ],
      ),
    );
  }

  void _showPicker(
    BuildContext context, {
    required String title,
    required List<String> options,
    required ValueChanged<String?> onSelected,
  }) {
    showModalBottomSheet(
      context: context,
      backgroundColor: Colors.transparent,
      builder: (_) => _OptionPickerSheet(
          title: title, options: options, onSelected: onSelected),
    );
  }

  void _showChassisFilter(
      BuildContext context, YardVehiclesNotifier notifier) {
    final ctrl = TextEditingController();
    showDialog(
      context: context,
      builder: (_) => AlertDialog(
        shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(AppDimens.cardRadius)),
        title: Text(AppStrings.chassis, style: AppTextStyles.titleLarge),
        content: TextField(
          controller: ctrl,
          decoration: const InputDecoration(hintText: 'Enter chassis...'),
          autofocus: true,
        ),
        actions: [
          TextButton(
            onPressed: () {
              notifier.applyFilter(clearChassis: true);
              Navigator.pop(context);
            },
            child: const Text('Clear'),
          ),
          TextButton(
            onPressed: () {
              if (ctrl.text.isNotEmpty) {
                notifier.applyFilter(chassis: ctrl.text);
              }
              Navigator.pop(context);
            },
            child: const Text('Apply'),
          ),
        ],
      ),
    );
  }
}

class _FilterChip extends StatelessWidget {
  final String label;
  final String? activeValue;
  final VoidCallback onTap;

  const _FilterChip({
    required this.label,
    this.activeValue,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final isActive = activeValue != null;
    return ActionChip(
      label: Text(
        isActive ? activeValue! : label,
        style: AppTextStyles.bodySmall.copyWith(
          color: isActive ? AppColors.primary : AppColors.textSecondary,
          fontWeight:
              isActive ? FontWeight.w600 : FontWeight.w500,
        ),
      ),
      backgroundColor: isActive ? AppColors.primaryLight : AppColors.white,
      side: BorderSide(
          color: isActive ? AppColors.primary : AppColors.border),
      onPressed: onTap,
      avatar: isActive
          ? const Icon(Icons.check_rounded,
              color: AppColors.primary, size: 14)
          : const Icon(Icons.keyboard_arrow_down_rounded,
              color: AppColors.textSecondary, size: 14),
    );
  }
}

class _Body extends ConsumerWidget {
  final YardVehiclesState state;
  const _Body({required this.state});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    if (state.isLoading) {
      return ListView(
        padding: const EdgeInsets.all(AppDimens.lg),
        children: List.generate(6, (_) => const ShimmerVehicleCard()),
      );
    }
    if (state.error != null) {
      return EmptyState(
        icon: Icons.error_outline_rounded,
        title: 'Error',
        subtitle: state.error,
      );
    }
    if (state.filtered.isEmpty) {
      return const EmptyState(
        icon: Icons.search_off_rounded,
        title: 'No vehicles found',
        subtitle: 'Try adjusting your filters',
      );
    }

    return ListView.separated(
      padding: const EdgeInsets.all(AppDimens.lg),
      itemCount: state.filtered.length,
      separatorBuilder: (_, __) => const Gap(AppDimens.sm),
      itemBuilder: (_, i) {
        final v = state.filtered[i];
        return VehicleCard(
          vehicle: v,
          onTap: () => context.push('/vehicles/detail', extra: v),
        );
      },
    );
  }
}

class _OptionPickerSheet extends StatelessWidget {
  final String title;
  final List<String> options;
  final ValueChanged<String?> onSelected;

  const _OptionPickerSheet({
    required this.title,
    required this.options,
    required this.onSelected,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: const BoxDecoration(
        color: AppColors.scaffold,
        borderRadius: BorderRadius.vertical(
            top: Radius.circular(AppDimens.bottomSheetRadius)),
      ),
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
            child:
                Text(title, style: AppTextStyles.headlineMedium),
          ),
          ListTile(
            title: Text('All', style: AppTextStyles.bodyMedium),
            leading: const Icon(Icons.clear_rounded,
                color: AppColors.textSecondary),
            onTap: () {
              onSelected(null);
              Navigator.pop(context);
            },
          ),
          const Divider(color: AppColors.divider, height: 1),
          Flexible(
            child: ListView.separated(
              shrinkWrap: true,
              itemCount: options.length,
              separatorBuilder: (_, __) =>
                  const Divider(color: AppColors.divider, height: 1),
              itemBuilder: (_, i) => ListTile(
                title: Text(options[i],
                    style: AppTextStyles.bodyMedium),
                onTap: () {
                  onSelected(options[i]);
                  Navigator.pop(context);
                },
              ),
            ),
          ),
        ],
      ),
    );
  }
}
