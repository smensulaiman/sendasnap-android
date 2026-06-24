import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:gap/gap.dart';
import 'package:go_router/go_router.dart';
import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_dimens.dart';
import '../../../../core/constants/app_strings.dart';
import '../../../../core/constants/app_text_styles.dart';
import '../../../../core/storage/local_storage.dart';
import '../../../../shared/widgets/app_button.dart';
import '../../../../shared/widgets/avatar_widget.dart';
import '../../../auth/presentation/providers/auth_provider.dart';

class ProfileScreen extends ConsumerStatefulWidget {
  const ProfileScreen({super.key});

  @override
  ConsumerState<ProfileScreen> createState() => _ProfileScreenState();
}

class _ProfileScreenState extends ConsumerState<ProfileScreen> {
  late bool _haptic;
  late bool _notifications;

  @override
  void initState() {
    super.initState();
    final storage = ref.read(localStorageProvider);
    _haptic = storage.isHapticEnabled();
    _notifications = storage.isNotificationsEnabled();
  }

  Future<void> _logout() async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (_) => AlertDialog(
        shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(AppDimens.cardRadius)),
        title: Text(AppStrings.logoutConfirmTitle,
            style: AppTextStyles.titleLarge),
        content: Text(AppStrings.logoutConfirmMessage,
            style: AppTextStyles.bodyMedium),
        actions: [
          TextButton(
              onPressed: () => Navigator.pop(context, false),
              child: Text(AppStrings.cancel,
                  style: AppTextStyles.bodyMedium
                      .copyWith(color: AppColors.textSecondary))),
          TextButton(
              onPressed: () => Navigator.pop(context, true),
              child: Text(AppStrings.confirm,
                  style: AppTextStyles.bodyMedium
                      .copyWith(color: AppColors.error))),
        ],
      ),
    );
    if (confirm == true && mounted) {
      await ref.read(authProvider.notifier).logout();
      if (mounted) context.go('/login');
    }
  }

  @override
  Widget build(BuildContext context) {
    final authState = ref.watch(authProvider);
    final user = authState.user;
    final vendor = authState.vendor;
    final storage = ref.read(localStorageProvider);
    final cacheCount = storage.getCacheSize();

    return Scaffold(
      backgroundColor: AppColors.scaffold,
      appBar: AppBar(
        backgroundColor: AppColors.white,
        surfaceTintColor: AppColors.white,
        elevation: 0,
        title: Text(AppStrings.myProfile, style: AppTextStyles.appBarTitle),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(AppDimens.lg),
        child: Column(
          children: [
            // Header
            Container(
              padding: const EdgeInsets.all(AppDimens.xxl),
              decoration: BoxDecoration(
                color: AppColors.primaryLight,
                borderRadius: BorderRadius.circular(AppDimens.cardRadius),
              ),
              child: Column(
                children: [
                  AvatarWidget(
                    imageUrl: user?.avatarUrl,
                    name: user?.name ?? 'U',
                    size: AppDimens.avatarLarge,
                  ),
                  const Gap(AppDimens.md),
                  Text(user?.name ?? '—',
                      style: AppTextStyles.headlineMedium),
                  const Gap(AppDimens.xs),
                  if (user?.role != null)
                    Container(
                      padding: const EdgeInsets.symmetric(
                          horizontal: AppDimens.md, vertical: 4),
                      decoration: BoxDecoration(
                        color: AppColors.primary.withOpacity(0.12),
                        borderRadius: BorderRadius.circular(20),
                      ),
                      child: Text(
                        user!.role.toUpperCase(),
                        style: AppTextStyles.chipLabel,
                      ),
                    ),
                  const Gap(AppDimens.xs),
                  Text(user?.email ?? '—',
                      style: AppTextStyles.bodySmall),
                ],
              ),
            ),
            const Gap(AppDimens.lg),

            // Company
            if (vendor != null) ...[
              _SectionCard(title: 'Company', children: [
                _InfoRow(label: AppStrings.companyName, value: vendor.name),
                if (vendor.formattedAddress.isNotEmpty)
                  _InfoRow(
                      label: AppStrings.address,
                      value: vendor.formattedAddress),
              ]),
              const Gap(AppDimens.md),
            ],

            // Account Info
            _SectionCard(title: AppStrings.accountInfo, children: [
              _InfoRow(label: AppStrings.fullName, value: user?.name),
              _InfoRow(label: AppStrings.emailLabel, value: user?.email),
              _InfoRow(label: AppStrings.role, value: user?.role),
              if (user?.phone != null)
                _InfoRow(label: AppStrings.phone, value: user!.phone),
              if (user?.avisId != null)
                _InfoRow(label: AppStrings.avisId, value: user!.avisId),
            ]),
            const Gap(AppDimens.md),

            // Settings
            _SectionCard(title: AppStrings.settings, children: [
              _ToggleRow(
                label: AppStrings.notifications,
                icon: Icons.notifications_outlined,
                value: _notifications,
                onChanged: (v) {
                  setState(() => _notifications = v);
                  ref
                      .read(authProvider.notifier)
                      .updateSettings(notifications: v);
                },
              ),
              const Divider(color: AppColors.divider, height: 1),
              _ToggleRow(
                label: AppStrings.hapticFeedback,
                icon: Icons.vibration_rounded,
                value: _haptic,
                onChanged: (v) {
                  setState(() => _haptic = v);
                  ref
                      .read(authProvider.notifier)
                      .updateSettings(haptic: v);
                },
              ),
              const Divider(color: AppColors.divider, height: 1),
              ListTile(
                leading: const Icon(Icons.lock_outline_rounded,
                    color: AppColors.primary, size: 20),
                title: Text(AppStrings.changePassword,
                    style: AppTextStyles.bodyMedium),
                trailing: const Icon(Icons.chevron_right_rounded,
                    color: AppColors.textSecondary),
                onTap: () => context.push('/profile/change-password'),
                contentPadding: EdgeInsets.zero,
              ),
            ]),
            const Gap(AppDimens.md),

            // Cache
            _SectionCard(title: AppStrings.cacheSection, children: [
              ListTile(
                leading: const Icon(Icons.history_rounded,
                    color: AppColors.primary, size: 20),
                title: Text(AppStrings.recentlyViewedVehicles,
                    style: AppTextStyles.bodyMedium),
                trailing: Text(
                  AppStrings.vehiclesCachedLabel(cacheCount),
                  style: AppTextStyles.bodySmall,
                ),
                contentPadding: EdgeInsets.zero,
              ),
              const Divider(color: AppColors.divider, height: 1),
              ListTile(
                leading: const Icon(Icons.delete_outline_rounded,
                    color: AppColors.error, size: 20),
                title: Text(AppStrings.clearCache,
                    style: AppTextStyles.bodyMedium
                        .copyWith(color: AppColors.error)),
                onTap: () => _clearCache(context, storage),
                contentPadding: EdgeInsets.zero,
              ),
            ]),
            const Gap(AppDimens.xxl),

            // Logout
            AppButton(
              label: AppStrings.logout,
              backgroundColor: AppColors.error,
              onPressed: _logout,
              icon: const Icon(Icons.logout_rounded,
                  size: 18, color: AppColors.white),
            ),
            const Gap(AppDimens.xxxl),
          ],
        ),
      ),
    );
  }

  Future<void> _clearCache(
      BuildContext context, LocalStorage storage) async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (_) => AlertDialog(
        shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(AppDimens.cardRadius)),
        title: Text('Clear Cache', style: AppTextStyles.titleLarge),
        content: Text('This will remove all recently viewed vehicles.',
            style: AppTextStyles.bodyMedium),
        actions: [
          TextButton(
              onPressed: () => Navigator.pop(context, false),
              child: const Text('Cancel')),
          TextButton(
              onPressed: () => Navigator.pop(context, true),
              child: const Text('Clear',
                  style: TextStyle(color: AppColors.error))),
        ],
      ),
    );
    if (confirm == true) {
      await storage.clearRecentVehicles();
      if (mounted) setState(() {});
    }
  }
}

class _SectionCard extends StatelessWidget {
  final String title;
  final List<Widget> children;

  const _SectionCard({required this.title, required this.children});

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.only(
              left: AppDimens.xs, bottom: AppDimens.sm),
          child: Text(title, style: AppTextStyles.labelSmall.copyWith(
            color: AppColors.textSecondary,
            letterSpacing: 0.5,
          )),
        ),
        Container(
          padding: const EdgeInsets.symmetric(
              horizontal: AppDimens.lg, vertical: AppDimens.xs),
          decoration: BoxDecoration(
            color: AppColors.white,
            borderRadius: BorderRadius.circular(AppDimens.cardRadius),
            border: Border.all(color: AppColors.divider),
          ),
          child: Column(children: children),
        ),
      ],
    );
  }
}

class _InfoRow extends StatelessWidget {
  final String label;
  final String? value;

  const _InfoRow({required this.label, this.value});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: AppDimens.sm),
      child: Row(
        children: [
          SizedBox(
              width: 120,
              child: Text(label, style: AppTextStyles.bodySmall)),
          Expanded(
            child: Text(
              value?.isNotEmpty == true ? value! : '—',
              style: AppTextStyles.bodyMedium,
              textAlign: TextAlign.end,
            ),
          ),
        ],
      ),
    );
  }
}

class _ToggleRow extends StatelessWidget {
  final String label;
  final IconData icon;
  final bool value;
  final ValueChanged<bool> onChanged;

  const _ToggleRow({
    required this.label,
    required this.icon,
    required this.value,
    required this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    return ListTile(
      leading: Icon(icon, color: AppColors.primary, size: 20),
      title: Text(label, style: AppTextStyles.bodyMedium),
      trailing: Switch(
        value: value,
        onChanged: onChanged,
        activeColor: AppColors.primary,
      ),
      contentPadding: EdgeInsets.zero,
    );
  }
}
