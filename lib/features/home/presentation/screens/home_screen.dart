import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:gap/gap.dart';
import 'package:google_fonts/google_fonts.dart';
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
    Future.delayed(const Duration(milliseconds: 700),
        () => mounted ? setState(() => _showShimmer = false) : null);
  }

  String get _greeting {
    final h = DateTime.now().hour;
    if (h < 12) return 'Good morning,';
    if (h < 17) return 'Good afternoon,';
    return 'Good evening,';
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
      appBar: PreferredSize(
        preferredSize: const Size.fromHeight(90),
        child: Container(
          color: AppColors.white,
          child: SafeArea(
            bottom: false,
            child: Padding(
              padding: const EdgeInsets.fromLTRB(16, 8, 4, 12),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.center,
                children: [
                  Container(
                    padding: const EdgeInsets.all(2),
                    decoration: BoxDecoration(
                      shape: BoxShape.circle,
                      border: Border.all(
                          color: AppColors.primary.withValues(alpha: 0.4),
                          width: 2),
                    ),
                    child: AvatarWidget(
                      imageUrl: user?.avatarUrl,
                      name: user?.name.isNotEmpty == true ? user!.name : 'U',
                      size: 46,
                    ),
                  ),
                  const Gap(12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Text(
                          _greeting,
                          style: GoogleFonts.montserrat(
                            fontSize: 11,
                            fontWeight: FontWeight.w500,
                            color: AppColors.textSecondary,
                          ),
                        ),
                        const Gap(1),
                        Text(
                          user?.name ?? '—',
                          style: GoogleFonts.montserrat(
                            fontSize: 16,
                            fontWeight: FontWeight.w700,
                            color: AppColors.textPrimary,
                          ),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                        const Gap(4),
                        Row(
                          children: [
                            if (user?.role != null) ...[
                              _HeaderChip(label: user!.role.toUpperCase()),
                              const Gap(6),
                            ],
                            if (vendor != null)
                              Flexible(
                                  child: _HeaderChip(label: vendor.name)),
                          ],
                        ),
                      ],
                    ),
                  ),
                  Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Image.asset(
                        'assets/images/logo.png',
                        height: 32,
                      ),
                      const Gap(2),
                      Text(
                        'SendaSnap',
                        style: GoogleFonts.montserrat(
                          fontSize: 8,
                          fontWeight: FontWeight.w700,
                          color: AppColors.textSecondary,
                          letterSpacing: 1,
                        ),
                      ),
                    ],
                  ),
                  Stack(
                    children: [
                      IconButton(
                        icon: const Icon(Icons.notifications_outlined,
                            color: AppColors.textPrimary, size: 26),
                        onPressed: () => context.push(
                            '/coming-soon',
                            extra: 'Notifications'),
                      ),
                      Positioned(
                        right: 10,
                        top: 10,
                        child: Container(
                          width: 8,
                          height: 8,
                          decoration: const BoxDecoration(
                            color: Color(0xFFFF5252),
                            shape: BoxShape.circle,
                          ),
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ),
        ),
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
              // Quick search
              Container(
                decoration: BoxDecoration(
                  gradient: const LinearGradient(
                    begin: Alignment.topLeft,
                    end: Alignment.bottomRight,
                    colors: [AppColors.primary, AppColors.primaryDark],
                  ),
                  borderRadius: BorderRadius.circular(AppDimens.cardRadius),
                  boxShadow: [
                    BoxShadow(
                      color: AppColors.primary.withValues(alpha: 0.3),
                      blurRadius: 16,
                      offset: const Offset(0, 6),
                    ),
                  ],
                ),
                padding: const EdgeInsets.fromLTRB(20, 20, 20, 16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        const Icon(Icons.manage_search_rounded,
                            color: Colors.white, size: 20),
                        const Gap(8),
                        Text(
                          AppStrings.quickSearch,
                          style: GoogleFonts.montserrat(
                            fontSize: 14,
                            fontWeight: FontWeight.w700,
                            color: Colors.white,
                          ),
                        ),
                      ],
                    ),
                    const Gap(4),
                    Text(
                      'Find vehicles by ID, chassis or browse the yard',
                      style: GoogleFonts.montserrat(
                        fontSize: 11,
                        fontWeight: FontWeight.w400,
                        color: Colors.white.withValues(alpha: 0.7),
                      ),
                    ),
                    const Gap(16),
                    Row(
                      children: [
                        Expanded(
                          child: _QuickActionCard(
                            icon: Icons.search_rounded,
                            label: AppStrings.searchVehicle,
                            subtitle: 'By ID or chassis',
                            color: Colors.white,
                            onTap: () => _openSearchSheet(context),
                          ),
                        ),
                        const Gap(12),
                        Expanded(
                          child: _QuickActionCard(
                            icon: Icons.warehouse_rounded,
                            label: AppStrings.browseYard,
                            subtitle: 'View yard stock',
                            color: const Color(0xFFFFD54F),
                            onTap: () =>
                                _openSearchSheet(context, yardTab: true),
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
              const Gap(AppDimens.xxl),

              // Recent vehicles
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(AppStrings.recentlyViewed,
                      style: AppTextStyles.sectionHeader),
                  if (recentVehicles.isNotEmpty)
                    TextButton(
                      onPressed: () =>
                          _showAllRecent(context, recentVehicles),
                      child: Text(AppStrings.seeAll,
                          style: AppTextStyles.bodySmall
                              .copyWith(color: AppColors.primary)),
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
                EmptyState(
                  icon: Icons.directions_car_outlined,
                  title: AppStrings.noRecentVehicles,
                  subtitle: AppStrings.noRecentVehiclesSubtitle,
                )
              else
                ListView.builder(
                  shrinkWrap: true,
                  physics: const NeverScrollableScrollPhysics(),
                  itemCount:
                      recentVehicles.length > 5 ? 5 : recentVehicles.length,
                  itemBuilder: (_, i) => VehicleCard(
                    vehicle: recentVehicles[i],
                    onTap: () => context.push('/vehicles/detail',
                        extra: recentVehicles[i]),
                  ),
                ),
              const Gap(80),
            ],
          ),
        ),
      ),
    );
  }

  void _showAllRecent(
      BuildContext context, List<dynamic> recentVehicles) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (_) => _AllRecentSheet(vehicles: recentVehicles),
    );
  }
}

// ── Header chip ───────────────────────────────────────────────────────────

class _HeaderChip extends StatelessWidget {
  final String label;
  const _HeaderChip({required this.label});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
      decoration: BoxDecoration(
        color: AppColors.primaryLight,
        borderRadius: BorderRadius.circular(20),
      ),
      child: Text(
        label,
        style: GoogleFonts.montserrat(
          fontSize: 9,
          fontWeight: FontWeight.w600,
          color: AppColors.primary,
        ),
        maxLines: 1,
        overflow: TextOverflow.ellipsis,
      ),
    );
  }
}

// ── Quick action card ─────────────────────────────────────────────────────

class _QuickActionCard extends StatelessWidget {
  final IconData icon;
  final String label;
  final String subtitle;
  final Color color;
  final VoidCallback onTap;

  const _QuickActionCard({
    required this.icon,
    required this.label,
    required this.subtitle,
    required this.color,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.white.withValues(alpha: 0.12),
      borderRadius: BorderRadius.circular(12),
      child: InkWell(
        borderRadius: BorderRadius.circular(12),
        onTap: onTap,
        splashColor: Colors.white.withValues(alpha: 0.1),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 14),
          child: Row(
            children: [
              Container(
                width: 38,
                height: 38,
                decoration: BoxDecoration(
                  color: color.withValues(alpha: 0.15),
                  borderRadius: BorderRadius.circular(10),
                ),
                child: Icon(icon, color: color, size: 20),
              ),
              const Gap(10),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      label,
                      style: GoogleFonts.montserrat(
                        fontSize: 12,
                        fontWeight: FontWeight.w700,
                        color: Colors.white,
                      ),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                    Text(
                      subtitle,
                      style: GoogleFonts.montserrat(
                        fontSize: 9,
                        fontWeight: FontWeight.w400,
                        color: Colors.white.withValues(alpha: 0.65),
                      ),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ],
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
              top: Radius.circular(AppDimens.bottomSheetRadius)),
        ),
        child: Column(
          children: [
            const _SheetHandle(),
            Padding(
              padding: const EdgeInsets.all(AppDimens.lg),
              child: Text(AppStrings.recentlyViewed,
                  style: AppTextStyles.headlineMedium),
            ),
            Expanded(
              child: ListView.builder(
                controller: scrollCtrl,
                padding: const EdgeInsets.symmetric(
                    horizontal: AppDimens.lg),
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
