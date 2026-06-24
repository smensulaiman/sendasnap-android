import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:gap/gap.dart';
import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_dimens.dart';
import '../../../../core/constants/app_strings.dart';
import '../../../../core/constants/app_text_styles.dart';
import '../../../auth/presentation/providers/auth_provider.dart';
import '../../../vehicle/presentation/providers/vehicle_provider.dart';
import '../../../vehicle/presentation/widgets/vehicle_card.dart';
import '../../../vehicle/presentation/widgets/vehicle_search_sheet.dart';
import '../../../../shared/widgets/avatar_widget.dart';
import '../../../../shared/widgets/empty_state.dart';
import '../../../../shared/widgets/shimmer_card.dart';

class HomeScreen extends ConsumerStatefulWidget {
  const HomeScreen({super.key});

  @override
  ConsumerState<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends ConsumerState<HomeScreen> {
  bool _showShimmer = true;

  @override
  void initState() {
    super.initState();
    Future.delayed(
      const Duration(milliseconds: 700),
      () => mounted ? setState(() => _showShimmer = false) : null,
    );
  }

  void _openSearchSheet(BuildContext context, {bool yardTab = false}) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (_) => VehicleSearchSheet(initialTab: yardTab ? 1 : 0),
    );
  }

  @override
  Widget build(BuildContext context) {
    final authState = ref.watch(authProvider);
    final user = authState.user;
    final vendor = authState.vendor;
    final recentVehicles = ref.watch(recentVehiclesProvider);

    return Scaffold(
      backgroundColor: AppColors.scaffold,
      appBar: AppBar(
        backgroundColor: AppColors.white,
        surfaceTintColor: AppColors.white,
        elevation: 0,
        title: Text(AppStrings.appName, style: AppTextStyles.appBarTitle),
        actions: [
          IconButton(
            icon: const Icon(
              Icons.notifications_outlined,
              color: AppColors.textPrimary,
            ),
            onPressed: () =>
                context.push('/coming-soon', extra: 'Notifications'),
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () => _openSearchSheet(context),
        backgroundColor: AppColors.primary,
        foregroundColor: AppColors.white,
        child: const Icon(Icons.search_rounded),
      ),
      body: RefreshIndicator(
        color: AppColors.primary,
        onRefresh: () async {
          setState(() {});
        },
        child: SingleChildScrollView(
          physics: const AlwaysScrollableScrollPhysics(),
          padding: const EdgeInsets.all(AppDimens.lg),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Greeting card
              _GreetingCard(
                name: user?.name ?? '',
                email: user?.email ?? '',
                role: user?.role ?? '',
                avatarUrl: user?.avatarUrl,
                vendorName: vendor?.name,
                vendorAddress: vendor?.formattedAddress,
              ),
              const Gap(AppDimens.xxl),

              // Quick search
              Text(AppStrings.quickSearch, style: AppTextStyles.sectionHeader),
              const Gap(AppDimens.md),
              Row(
                children: [
                  Expanded(
                    child: _QuickActionCard(
                      icon: Icons.search_rounded,
                      label: AppStrings.searchVehicle,
                      onTap: () => _openSearchSheet(context),
                    ),
                  ),
                  const Gap(AppDimens.md),
                  Expanded(
                    child: _QuickActionCard(
                      icon: Icons.warehouse_rounded,
                      label: AppStrings.browseYard,
                      onTap: () => _openSearchSheet(context, yardTab: true),
                    ),
                  ),
                ],
              ),
              const Gap(AppDimens.xxl),

              // Recent vehicles
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    AppStrings.recentlyViewed,
                    style: AppTextStyles.sectionHeader,
                  ),
                  if (recentVehicles.isNotEmpty)
                    TextButton(
                      onPressed: () => _showAllRecent(context, recentVehicles),
                      child: Text(
                        AppStrings.seeAll,
                        style: AppTextStyles.bodySmall.copyWith(
                          color: AppColors.primary,
                        ),
                      ),
                    ),
                ],
              ),
              const Gap(AppDimens.md),
              if (_showShimmer)
                Column(
                  children: List.generate(3, (_) => const ShimmerVehicleCard()),
                )
              else if (recentVehicles.isEmpty)
                const EmptyState(
                  icon: Icons.directions_car_outlined,
                  title: AppStrings.noRecentVehicles,
                  subtitle: AppStrings.noRecentVehiclesSubtitle,
                )
              else
                ListView.builder(
                  shrinkWrap: true,
                  physics: const NeverScrollableScrollPhysics(),
                  itemCount: recentVehicles.length > 5
                      ? 5
                      : recentVehicles.length,
                  itemBuilder: (_, i) => VehicleCard(
                    vehicle: recentVehicles[i],
                    onTap: () => context.push(
                      '/vehicles/detail',
                      extra: recentVehicles[i],
                    ),
                  ),
                ),
              const Gap(80),
            ],
          ),
        ),
      ),
    );
  }

  void _showAllRecent(BuildContext context, List<dynamic> recentVehicles) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (_) => _AllRecentSheet(vehicles: recentVehicles),
    );
  }
}

// ── Greeting card ─────────────────────────────────────────────────────────

class _GreetingCard extends StatelessWidget {
  final String name;
  final String email;
  final String role;
  final String? avatarUrl;
  final String? vendorName;
  final String? vendorAddress;

  const _GreetingCard({
    required this.name,
    required this.email,
    required this.role,
    this.avatarUrl,
    this.vendorName,
    this.vendorAddress,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(AppDimens.lg),
      decoration: BoxDecoration(
        color: AppColors.primaryLight,
        borderRadius: BorderRadius.circular(AppDimens.cardRadius),
      ),
      child: Row(
        children: [
          AvatarWidget(
            imageUrl: avatarUrl,
            name: name.isNotEmpty ? name : 'U',
            size: AppDimens.avatarMedium,
          ),
          const Gap(AppDimens.md),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  name.isNotEmpty ? '$name san' : 'Welcome',
                  style: AppTextStyles.titleLarge,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
                const Gap(2),
                Text(
                  email,
                  style: AppTextStyles.bodySmall,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
                const Gap(AppDimens.xs),
                Wrap(
                  spacing: AppDimens.xs,
                  runSpacing: AppDimens.xs,
                  children: [
                    if (role.isNotEmpty) _Chip(label: role.toUpperCase()),
                    if (vendorName != null) _Chip(label: vendorName!),
                  ],
                ),
                if (vendorAddress != null && vendorAddress!.isNotEmpty) ...[
                  const Gap(2),
                  Text(
                    vendorAddress!,
                    style: AppTextStyles.labelSmall,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                ],
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _Chip extends StatelessWidget {
  final String label;
  const _Chip({required this.label});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(
        horizontal: AppDimens.sm,
        vertical: 2,
      ),
      decoration: BoxDecoration(
        color: AppColors.primary.withValues(alpha: 0.12),
        borderRadius: BorderRadius.circular(20),
      ),
      child: Text(label, style: AppTextStyles.chipLabel.copyWith(fontSize: 9)),
    );
  }
}

// ── Quick action card ─────────────────────────────────────────────────────

class _QuickActionCard extends StatelessWidget {
  final IconData icon;
  final String label;
  final VoidCallback onTap;

  const _QuickActionCard({
    required this.icon,
    required this.label,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return Material(
      color: AppColors.primaryLight,
      borderRadius: BorderRadius.circular(AppDimens.cardRadius),
      child: InkWell(
        borderRadius: BorderRadius.circular(AppDimens.cardRadius),
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(AppDimens.lg),
          child: Column(
            children: [
              Container(
                width: 48,
                height: 48,
                decoration: BoxDecoration(
                  color: AppColors.primary.withValues(alpha: 0.12),
                  shape: BoxShape.circle,
                ),
                child: Icon(icon, color: AppColors.primary, size: 24),
              ),
              const Gap(AppDimens.sm),
              Text(
                label,
                style: AppTextStyles.titleMedium,
                textAlign: TextAlign.center,
              ),
            ],
          ),
        ),
      ),
    );
  }
}

// ── All recent bottom sheet ───────────────────────────────────────────────

class _AllRecentSheet extends StatelessWidget {
  final List<dynamic> vehicles;
  const _AllRecentSheet({required this.vehicles});

  @override
  Widget build(BuildContext context) {
    return DraggableScrollableSheet(
      initialChildSize: 0.7,
      maxChildSize: 0.95,
      minChildSize: 0.4,
      builder: (context, scrollCtrl) => Container(
        decoration: const BoxDecoration(
          color: AppColors.scaffold,
          borderRadius: BorderRadius.vertical(
            top: Radius.circular(AppDimens.bottomSheetRadius),
          ),
        ),
        child: Column(
          children: [
            const _SheetHandle(),
            Padding(
              padding: const EdgeInsets.all(AppDimens.lg),
              child: Text(
                AppStrings.recentlyViewed,
                style: AppTextStyles.headlineMedium,
              ),
            ),
            Expanded(
              child: ListView.builder(
                controller: scrollCtrl,
                padding: const EdgeInsets.symmetric(horizontal: AppDimens.lg),
                itemCount: vehicles.length,
                itemBuilder: (_, i) => VehicleCard(
                  vehicle: vehicles[i],
                  onTap: () {
                    Navigator.pop(context);
                    context.push('/vehicles/detail', extra: vehicles[i]);
                  },
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _SheetHandle extends StatelessWidget {
  const _SheetHandle();

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(top: 12),
      width: 36,
      height: 4,
      decoration: BoxDecoration(
        color: AppColors.border,
        borderRadius: BorderRadius.circular(2),
      ),
    );
  }
}
