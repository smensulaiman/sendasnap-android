import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:gap/gap.dart';
import 'package:go_router/go_router.dart';
import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_dimens.dart';
import '../../../../core/constants/app_strings.dart';
import '../../../../core/constants/app_text_styles.dart';
import '../../../../core/utils/validators.dart';
import '../../../../shared/widgets/app_button.dart';
import '../../../../shared/widgets/app_snackbar.dart';
import '../providers/vehicle_provider.dart';
import '../../data/models/vehicle_model.dart';

// ── Yard data ──────────────────────────────────────────────────────────────

class YardInfo {
  final String name;
  final int id;
  final String company;
  const YardInfo(this.name, this.id, this.company);
}

const _yards = [
  YardInfo(AppStrings.menzaiSugaya, 437, 'acjl'),
  YardInfo(AppStrings.hanyaYard, 1861, 'acjl'),
  YardInfo(AppStrings.acjKobe, 2385, 'acjl'),
  YardInfo(AppStrings.sugayaYard, 805, 'karmen'),
  YardInfo(AppStrings.hanyaYardKarmen, 1384, 'karmen'),
];

// ── Sheet ──────────────────────────────────────────────────────────────────

class VehicleSearchSheet extends ConsumerStatefulWidget {
  final int initialTab;
  const VehicleSearchSheet({super.key, this.initialTab = 0});

  @override
  ConsumerState<VehicleSearchSheet> createState() => _VehicleSearchSheetState();
}

class _VehicleSearchSheetState extends ConsumerState<VehicleSearchSheet>
    with SingleTickerProviderStateMixin {
  late final TabController _tabCtrl;

  @override
  void initState() {
    super.initState();
    _tabCtrl = TabController(
      length: 2,
      vsync: this,
      initialIndex: widget.initialTab,
    );
  }

  @override
  void dispose() {
    _tabCtrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: const BoxDecoration(
        color: AppColors.scaffold,
        borderRadius: BorderRadius.vertical(
          top: Radius.circular(AppDimens.bottomSheetRadius),
        ),
      ),
      child: DraggableScrollableSheet(
        initialChildSize: 0.65,
        maxChildSize: 0.92,
        minChildSize: 0.4,
        expand: false,
        builder: (_, scrollCtrl) => Column(
          children: [
            const _Handle(),
            Padding(
              padding: const EdgeInsets.fromLTRB(
                AppDimens.xxl,
                AppDimens.lg,
                AppDimens.xxl,
                0,
              ),
              child: Row(
                children: [
                  const Icon(
                    Icons.search_rounded,
                    color: AppColors.primary,
                    size: 22,
                  ),
                  const Gap(AppDimens.sm),
                  Text('Find Vehicle', style: AppTextStyles.headlineMedium),
                ],
              ),
            ),
            const Gap(AppDimens.md),
            TabBar(
              controller: _tabCtrl,
              labelStyle: AppTextStyles.titleMedium,
              unselectedLabelStyle: AppTextStyles.bodyMedium,
              labelColor: AppColors.primary,
              unselectedLabelColor: AppColors.textSecondary,
              indicatorColor: AppColors.primary,
              indicatorSize: TabBarIndicatorSize.label,
              tabs: const [
                Tab(text: AppStrings.search),
                Tab(text: AppStrings.yard),
              ],
            ),
            const Divider(height: 1, color: AppColors.divider),
            Expanded(
              child: TabBarView(
                controller: _tabCtrl,
                children: [
                  _SearchTab(scrollCtrl: scrollCtrl),
                  _YardTab(scrollCtrl: scrollCtrl),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

// ── Search tab ─────────────────────────────────────────────────────────────

class _SearchTab extends ConsumerStatefulWidget {
  final ScrollController scrollCtrl;
  const _SearchTab({required this.scrollCtrl});

  @override
  ConsumerState<_SearchTab> createState() => _SearchTabState();
}

class _SearchTabState extends ConsumerState<_SearchTab> {
  final _formKey = GlobalKey<FormState>();
  final _queryCtrl = TextEditingController();
  String _company = 'acjl';
  String _searchType = 'vehicle_id';

  @override
  void dispose() {
    _queryCtrl.dispose();
    super.dispose();
  }

  Future<void> _search() async {
    if (!(_formKey.currentState?.validate() ?? false)) return;

    // Search first, then act — keeps context valid for error snackbars
    final notifier = ref.read(vehicleSearchProvider.notifier);
    await notifier.search(
      searchType: _searchType,
      query: _queryCtrl.text.trim(),
      company: _company,
    );

    if (!mounted) return;
    final state = ref.read(vehicleSearchProvider);

    if (state.error != null) {
      AppSnackbar.error(context, state.error!);
      return;
    }
    if (state.results.isEmpty) {
      AppSnackbar.error(context, AppStrings.noVehiclesFound);
      return;
    }

    final router = GoRouter.of(context);
    final results = state.results;

    if (results.length == 1) {
      // Single result: pop sheet then navigate
      if (mounted) Navigator.of(context).pop();
      router.push('/vehicles/detail', extra: results.first);
    } else {
      // Multiple results: show picker on top (context is still valid here)
      if (mounted) _showVehiclePickerSheet(context, results);
    }
  }

  void _showVehiclePickerSheet(
    BuildContext context,
    List<VehicleModel> vehicles,
  ) {
    showModalBottomSheet(
      context: context,
      backgroundColor: Colors.transparent,
      isScrollControlled: true,
      builder: (_) => _VehiclePickerSheet(vehicles: vehicles),
    );
  }

  @override
  Widget build(BuildContext context) {
    final isLoading = ref.watch(vehicleSearchProvider).isLoading;

    return SingleChildScrollView(
      controller: widget.scrollCtrl,
      padding: const EdgeInsets.all(AppDimens.xxl),
      child: Form(
        key: _formKey,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Gap(AppDimens.sm),
            Text(
              AppStrings.company,
              style: AppTextStyles.labelSmall.copyWith(
                color: AppColors.textSecondary,
              ),
            ),
            const Gap(AppDimens.xs),
            _DropdownField<String>(
              value: _company,
              items: const [
                DropdownMenuItem(
                  value: 'acjl',
                  child: Text(AppStrings.autocraftJapan),
                ),
                DropdownMenuItem(
                  value: 'karmen',
                  child: Text(AppStrings.karmen),
                ),
              ],
              onChanged: (v) => setState(() => _company = v ?? 'acjl'),
            ),
            const Gap(AppDimens.lg),
            Text(
              AppStrings.searchType,
              style: AppTextStyles.labelSmall.copyWith(
                color: AppColors.textSecondary,
              ),
            ),
            const Gap(AppDimens.xs),
            _DropdownField<String>(
              value: _searchType,
              items: const [
                DropdownMenuItem(
                  value: 'vehicle_id',
                  child: Text(AppStrings.vehicleIdType),
                ),
                DropdownMenuItem(
                  value: 'veh_chassis_number',
                  child: Text(AppStrings.chassisNumberType),
                ),
              ],
              onChanged: (v) => setState(() => _searchType = v ?? 'vehicle_id'),
            ),
            const Gap(AppDimens.lg),
            TextFormField(
              controller: _queryCtrl,
              validator: Validators.searchQuery,
              style: AppTextStyles.bodyMedium,
              decoration: InputDecoration(
                labelText: 'Search Query',
                hintText: AppStrings.searchQueryHint,
                labelStyle: AppTextStyles.bodySmall,
                hintStyle: AppTextStyles.bodySmall.copyWith(
                  color: AppColors.textHint,
                ),
                prefixIcon: const Icon(
                  Icons.search_rounded,
                  color: AppColors.textSecondary,
                  size: 20,
                ),
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(AppDimens.inputRadius),
                  borderSide: const BorderSide(color: AppColors.border),
                ),
                enabledBorder: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(AppDimens.inputRadius),
                  borderSide: const BorderSide(color: AppColors.border),
                ),
                focusedBorder: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(AppDimens.inputRadius),
                  borderSide: const BorderSide(
                    color: AppColors.primary,
                    width: 1.5,
                  ),
                ),
                filled: true,
                fillColor: AppColors.white,
                contentPadding: const EdgeInsets.symmetric(
                  horizontal: AppDimens.lg,
                  vertical: AppDimens.md,
                ),
              ),
            ),
            const Gap(AppDimens.xxl),
            AppButton(
              label: AppStrings.search,
              onPressed: isLoading ? null : _search,
              isLoading: isLoading,
            ),
            const Gap(AppDimens.md),
            AppButton(
              label: AppStrings.cancel,
              isOutlined: true,
              onPressed: () => Navigator.pop(context),
            ),
          ],
        ),
      ),
    );
  }
}

// ── Yard tab ───────────────────────────────────────────────────────────────

class _YardTab extends ConsumerStatefulWidget {
  final ScrollController scrollCtrl;
  const _YardTab({required this.scrollCtrl});

  @override
  ConsumerState<_YardTab> createState() => _YardTabState();
}

class _YardTabState extends ConsumerState<_YardTab> {
  YardInfo? _selected;

  Future<void> _showVehicles() async {
    if (_selected == null) return;

    // Load vehicles first (sheet stays open showing loading state)
    await ref
        .read(yardVehiclesProvider.notifier)
        .loadYard(yardId: _selected!.id, company: _selected!.company);

    if (!mounted) return;
    final router = GoRouter.of(context);
    final yardName = _selected!.name;
    final company = _selected!.company;

    // Pop sheet then navigate
    Navigator.of(context).pop();
    router.push(
      '/vehicles/list',
      extra: {'yardName': yardName, 'company': company},
    );
  }

  @override
  Widget build(BuildContext context) {
    final isLoading = ref.watch(yardVehiclesProvider).isLoading;

    return SingleChildScrollView(
      controller: widget.scrollCtrl,
      padding: const EdgeInsets.all(AppDimens.xxl),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Gap(AppDimens.sm),
          _YardGroup(
            groupName: AppStrings.autocraftJapanLtd,
            yards: _yards.where((y) => y.company == 'acjl').toList(),
            selected: _selected,
            onSelect: (y) => setState(() => _selected = y),
          ),
          const Gap(AppDimens.md),
          _YardGroup(
            groupName: AppStrings.karmenLabel,
            yards: _yards.where((y) => y.company == 'karmen').toList(),
            selected: _selected,
            onSelect: (y) => setState(() => _selected = y),
          ),
          const Gap(AppDimens.xxl),
          AppButton(
            label: AppStrings.showVehicles,
            onPressed: (_selected == null || isLoading) ? null : _showVehicles,
            isLoading: isLoading,
          ),
          const Gap(AppDimens.md),
          AppButton(
            label: AppStrings.cancel,
            isOutlined: true,
            onPressed: () => Navigator.pop(context),
          ),
        ],
      ),
    );
  }
}

class _YardGroup extends StatelessWidget {
  final String groupName;
  final List<YardInfo> yards;
  final YardInfo? selected;
  final ValueChanged<YardInfo> onSelect;

  const _YardGroup({
    required this.groupName,
    required this.yards,
    required this.selected,
    required this.onSelect,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.symmetric(vertical: AppDimens.sm),
          child: Row(
            children: [
              const Expanded(child: Divider(color: AppColors.border)),
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: AppDimens.sm),
                child: Text(
                  groupName,
                  style: AppTextStyles.labelSmall.copyWith(
                    color: AppColors.textSecondary,
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ),
              const Expanded(child: Divider(color: AppColors.border)),
            ],
          ),
        ),
        ...yards.map(
          (yard) => _YardItem(
            yard: yard,
            isSelected: selected?.id == yard.id,
            onTap: () => onSelect(yard),
          ),
        ),
      ],
    );
  }
}

class _YardItem extends StatelessWidget {
  final YardInfo yard;
  final bool isSelected;
  final VoidCallback onTap;

  const _YardItem({
    required this.yard,
    required this.isSelected,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return Material(
      color: isSelected ? AppColors.primaryLight : Colors.transparent,
      borderRadius: BorderRadius.circular(AppDimens.md),
      child: InkWell(
        borderRadius: BorderRadius.circular(AppDimens.md),
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.symmetric(
            horizontal: AppDimens.lg,
            vertical: AppDimens.md,
          ),
          child: Row(
            children: [
              Icon(
                Icons.warehouse_outlined,
                color: isSelected ? AppColors.primary : AppColors.textSecondary,
                size: 20,
              ),
              const Gap(AppDimens.md),
              Expanded(
                child: Text(
                  yard.name,
                  style: AppTextStyles.bodyMedium.copyWith(
                    color: isSelected
                        ? AppColors.primary
                        : AppColors.textPrimary,
                    fontWeight: isSelected ? FontWeight.w600 : FontWeight.w500,
                  ),
                ),
              ),
              if (isSelected)
                const Icon(
                  Icons.check_circle_rounded,
                  color: AppColors.primary,
                  size: 18,
                ),
            ],
          ),
        ),
      ),
    );
  }
}

// ── Vehicle picker sheet ───────────────────────────────────────────────────

class _VehiclePickerSheet extends StatelessWidget {
  final List<VehicleModel> vehicles;
  const _VehiclePickerSheet({required this.vehicles});

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: const BoxDecoration(
        color: AppColors.scaffold,
        borderRadius: BorderRadius.vertical(
          top: Radius.circular(AppDimens.bottomSheetRadius),
        ),
      ),
      child: DraggableScrollableSheet(
        initialChildSize: 0.6,
        maxChildSize: 0.9,
        minChildSize: 0.4,
        expand: false,
        builder: (_, ctrl) => Column(
          children: [
            const _Handle(),
            Padding(
              padding: const EdgeInsets.all(AppDimens.lg),
              child: Text(
                AppStrings.selectVehicle,
                style: AppTextStyles.headlineMedium,
              ),
            ),
            Expanded(
              child: ListView.separated(
                controller: ctrl,
                padding: const EdgeInsets.symmetric(horizontal: AppDimens.lg),
                itemCount: vehicles.length,
                separatorBuilder: (_, __) => const Gap(AppDimens.sm),
                itemBuilder: (_, i) {
                  final v = vehicles[i];
                  return Material(
                    color: AppColors.primaryLight,
                    borderRadius: BorderRadius.circular(AppDimens.md),
                    child: InkWell(
                      borderRadius: BorderRadius.circular(AppDimens.md),
                      onTap: () {
                        Navigator.pop(context);
                        context.push('/vehicles/detail', extra: v);
                      },
                      child: Padding(
                        padding: const EdgeInsets.all(AppDimens.md),
                        child: Row(
                          children: [
                            const Icon(
                              Icons.directions_car_rounded,
                              color: AppColors.primary,
                              size: 24,
                            ),
                            const Gap(AppDimens.md),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(
                                    '${v.make ?? ''} ${v.model ?? ''}'.trim(),
                                    style: AppTextStyles.titleMedium,
                                  ),
                                  Text(
                                    v.serialNumber ?? v.id,
                                    style: AppTextStyles.bodySmall,
                                  ),
                                ],
                              ),
                            ),
                            const Icon(
                              Icons.chevron_right_rounded,
                              color: AppColors.textSecondary,
                            ),
                          ],
                        ),
                      ),
                    ),
                  );
                },
              ),
            ),
          ],
        ),
      ),
    );
  }
}

// ── Helpers ────────────────────────────────────────────────────────────────

class _Handle extends StatelessWidget {
  const _Handle();
  @override
  Widget build(BuildContext context) => Container(
    margin: const EdgeInsets.only(top: 12),
    width: 36,
    height: 4,
    decoration: BoxDecoration(
      color: AppColors.border,
      borderRadius: BorderRadius.circular(2),
    ),
  );
}

class _DropdownField<T> extends StatelessWidget {
  final T value;
  final List<DropdownMenuItem<T>> items;
  final ValueChanged<T?> onChanged;

  const _DropdownField({
    required this.value,
    required this.items,
    required this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    return DropdownButtonFormField<T>(
      initialValue: value,
      items: items,
      onChanged: onChanged,
      style: AppTextStyles.bodyMedium,
      decoration: InputDecoration(
        filled: true,
        fillColor: AppColors.white,
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(AppDimens.inputRadius),
          borderSide: const BorderSide(color: AppColors.border),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(AppDimens.inputRadius),
          borderSide: const BorderSide(color: AppColors.border),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(AppDimens.inputRadius),
          borderSide: const BorderSide(color: AppColors.primary, width: 1.5),
        ),
        contentPadding: const EdgeInsets.symmetric(
          horizontal: AppDimens.lg,
          vertical: AppDimens.md,
        ),
      ),
    );
  }
}
