import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
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

    return AnnotatedRegion<SystemUiOverlayStyle>(
      value: SystemUiOverlayStyle.light,
      child: Scaffold(
        backgroundColor: AppColors.scaffold,
        body: Column(
          children: [
            // ── Gradient header ──────────────────────────────────────
            _GradientHeader(
              name: user?.name ?? '',
              email: user?.email ?? '',
              role: user?.role ?? '',
              avatarUrl: user?.avatarUrl,
              vendorName: vendor?.name,
              vendorAddress: vendor?.formattedAddress,
              onNotifications: () =>
                  context.push('/coming-soon', extra: 'Notifications'),
            ),

            // ── Scrollable content ───────────────────────────────────
            Expanded(
              child: RefreshIndicator(
                color: AppColors.primary,
                onRefresh: () async => setState(() {}),
                child: SingleChildScrollView(
                  physics: const AlwaysScrollableScrollPhysics(),
                  padding: const EdgeInsets.fromLTRB(
                      AppDimens.lg, AppDimens.lg, AppDimens.lg, 100),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      // Quick actions
                      Text(AppStrings.quickSearch,
                          style: AppTextStyles.sectionHeader),
                      const Gap(AppDimens.md),
                      Row(
                        children: [
                          Expanded(
                            child: _ActionTile(
                              icon: Icons.search_rounded,
                              label: AppStrings.searchVehicle,
                              subtitle: 'By chassis or name',
                              background: const Color(0xFFCFE3FF),
                              accent: const Color(0xFF1457A8),
                              onTap: () => _openSearchSheet(context),
                            ),
                          ),
                          const Gap(AppDimens.md),
                          Expanded(
                            child: _ActionTile(
                              icon: Icons.warehouse_rounded,
                              label: AppStrings.browseYard,
                              subtitle: 'Browse by yard',
                              background: const Color(0xFFCFEEDF),
                              accent: const Color(0xFF12815A),
                              onTap: () =>
                                  _openSearchSheet(context, yardTab: true),
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
                              onPressed: () =>
                                  _showAllRecent(context, recentVehicles),
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
                          children: List.generate(
                              3, (_) => const ShimmerVehicleCard()),
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
                    ],
                  ),
                ),
              ),
            ),
          ],
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

// ── Gradient header ───────────────────────────────────────────────────────

class _GradientHeader extends StatelessWidget {
  final String name;
  final String email;
  final String role;
  final String? avatarUrl;
  final String? vendorName;
  final String? vendorAddress;
  final VoidCallback onNotifications;

  const _GradientHeader({
    required this.name,
    required this.email,
    required this.role,
    required this.onNotifications,
    this.avatarUrl,
    this.vendorName,
    this.vendorAddress,
  });

  @override
  Widget build(BuildContext context) {
    final topPad = MediaQuery.paddingOf(context).top;
    return Container(
      padding: EdgeInsets.fromLTRB(
          AppDimens.lg, topPad + AppDimens.sm, AppDimens.lg, AppDimens.lg),
      decoration: const BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [Color(0xFF1E88E5), AppColors.primary, Color(0xFF0D3C6E)],
          stops: [0.0, 0.55, 1.0],
        ),
        borderRadius: BorderRadius.vertical(bottom: Radius.circular(24)),
        boxShadow: [
          BoxShadow(
            color: Color(0x331565C0),
            blurRadius: 20,
            offset: Offset(0, 8),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                padding: const EdgeInsets.all(2.5),
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  border: Border.all(
                    color: Colors.white.withValues(alpha: 0.45),
                    width: 1.5,
                  ),
                ),
                child: AvatarWidget(
                  imageUrl: avatarUrl,
                  name: name.isNotEmpty ? name : 'U',
                  size: AppDimens.avatarSmall,
                ),
              ),
              const Gap(AppDimens.md),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Welcome back',
                      style: TextStyle(
                        color: Colors.white.withValues(alpha: 0.75),
                        fontSize: 12,
                      ),
                    ),
                    const Gap(1),
                    Text(
                      name.isNotEmpty ? '$name san' : 'Welcome',
                      style: const TextStyle(
                        color: Colors.white,
                        fontSize: 18,
                        fontWeight: FontWeight.w800,
                        letterSpacing: -0.3,
                      ),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ],
                ),
              ),
              _CircleIconButton(
                icon: Icons.notifications_outlined,
                onTap: onNotifications,
              ),
            ],
          ),
          const Gap(AppDimens.sm),
          Wrap(
            spacing: AppDimens.xs,
            runSpacing: AppDimens.xs,
            crossAxisAlignment: WrapCrossAlignment.center,
            children: [
              if (role.isNotEmpty) _GlassChip(label: role.toUpperCase()),
              if (vendorName != null) _GlassChip(label: vendorName!),
            ],
          ),
          if (vendorAddress != null && vendorAddress!.isNotEmpty) ...[
            const Gap(AppDimens.sm),
            Row(
              children: [
                Icon(Icons.location_on_outlined,
                    size: 14, color: Colors.white.withValues(alpha: 0.7)),
                const Gap(4),
                Expanded(
                  child: Text(
                    vendorAddress!,
                    style: TextStyle(
                      color: Colors.white.withValues(alpha: 0.7),
                      fontSize: 12,
                    ),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
              ],
            ),
          ],
        ],
      ),
    );
  }
}

class _CircleIconButton extends StatelessWidget {
  final IconData icon;
  final VoidCallback onTap;
  const _CircleIconButton({required this.icon, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.white.withValues(alpha: 0.18),
      shape: const CircleBorder(),
      child: InkWell(
        customBorder: const CircleBorder(),
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(10),
          child: Icon(icon, color: Colors.white, size: 22),
        ),
      ),
    );
  }
}

class _GlassChip extends StatelessWidget {
  final String label;
  const _GlassChip({required this.label});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: AppDimens.sm, vertical: 3),
      decoration: BoxDecoration(
        color: Colors.white.withValues(alpha: 0.18),
        borderRadius: BorderRadius.circular(20),
      ),
      child: Text(
        label,
        style: const TextStyle(
          color: Colors.white,
          fontSize: 10,
          fontWeight: FontWeight.w600,
          letterSpacing: 0.3,
        ),
      ),
    );
  }
}

// ── Light action tile ─────────────────────────────────────────────────────

class _ActionTile extends StatelessWidget {
  final IconData icon;
  final String label;
  final String subtitle;
  final Color background;
  final Color accent;
  final VoidCallback onTap;

  const _ActionTile({
    required this.icon,
    required this.label,
    required this.subtitle,
    required this.background,
    required this.accent,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    const radius = BorderRadius.all(Radius.circular(20));
    return Material(
      color: background,
      borderRadius: radius,
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(AppDimens.lg),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                width: 44,
                height: 44,
                decoration: BoxDecoration(
                  color: accent,
                  borderRadius: BorderRadius.circular(14),
                ),
                child: Icon(icon, color: AppColors.white, size: 22),
              ),
              const Gap(AppDimens.md),
              Text(
                label,
                style: TextStyle(
                  color: accent,
                  fontSize: 15,
                  fontWeight: FontWeight.w700,
                ),
              ),
              const Gap(2),
              Text(
                subtitle,
                style: TextStyle(
                  color: accent.withValues(alpha: 0.65),
                  fontSize: 11,
                ),
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
