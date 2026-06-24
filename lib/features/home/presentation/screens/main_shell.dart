import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_text_styles.dart';
import '../../../../shared/widgets/connectivity_banner.dart';

class MainShell extends ConsumerWidget {
  final Widget child;

  const MainShell({super.key, required this.child});

  static const _tabs = [
    '/main/home',
    '/main/task',
    '/main/chat',
    '/main/profile',
  ];

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final location = GoRouterState.of(context).matchedLocation;
    final currentIndex = _tabs.indexWhere((t) => location.startsWith(t));

    return Scaffold(
      body: Column(
        children: [
          const ConnectivityBanner(),
          Expanded(child: child),
        ],
      ),
      bottomNavigationBar: Container(
        decoration: BoxDecoration(
          color: AppColors.white,
          borderRadius: const BorderRadius.vertical(top: Radius.circular(24)),
          boxShadow: [
            BoxShadow(
              color: const Color(0xFF0D3C6E).withValues(alpha: 0.10),
              blurRadius: 20,
              offset: const Offset(0, -4),
            ),
          ],
        ),
        child: SafeArea(
          top: false,
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 10),
            child: SizedBox(
              height: 56,
              child: Stack(
                children: [
                  // Sliding light-blue background that moves to the selected tab
                  if (currentIndex >= 0)
                    AnimatedAlign(
                      alignment: Alignment(
                        _tabs.length == 1
                            ? 0
                            : -1 + 2 * currentIndex / (_tabs.length - 1),
                        0,
                      ),
                      duration: const Duration(milliseconds: 320),
                      curve: Curves.easeOutCubic,
                      child: FractionallySizedBox(
                        widthFactor: 1 / _tabs.length,
                        heightFactor: 1,
                        child: Container(
                          margin: const EdgeInsets.symmetric(horizontal: 6),
                          decoration: BoxDecoration(
                            color: AppColors.primaryLight,
                            borderRadius: BorderRadius.circular(18),
                          ),
                        ),
                      ),
                    ),
                  Row(
                    children: [
                      _NavItem(
                        icon: Icons.home_outlined,
                        label: 'Home',
                        selected: currentIndex == 0,
                        onTap: () => context.go('/main/home'),
                      ),
                      _NavItem(
                        icon: Icons.task_alt_outlined,
                        label: 'Task',
                        selected: currentIndex == 1,
                        onTap: () => context.go('/main/task'),
                      ),
                      _NavItem(
                        icon: Icons.chat_bubble_outline_rounded,
                        label: 'Chat',
                        selected: currentIndex == 2,
                        onTap: () => context.go('/main/chat'),
                      ),
                      _NavItem(
                        icon: Icons.person_outline_rounded,
                        label: 'Profile',
                        selected: currentIndex == 3,
                        onTap: () => context.go('/main/profile'),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _NavItem extends StatelessWidget {
  final IconData icon;
  final String label;
  final bool selected;
  final VoidCallback onTap;

  const _NavItem({
    required this.icon,
    required this.label,
    required this.selected,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    const duration = Duration(milliseconds: 300);
    const curve = Curves.easeOutCubic;
    return Expanded(
      child: GestureDetector(
        onTap: onTap,
        behavior: HitTestBehavior.opaque,
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            // Icon scales up + colour cross-fades smoothly on selection
            AnimatedScale(
              scale: selected ? 1.15 : 1.0,
              duration: duration,
              curve: Curves.easeOutBack,
              child: TweenAnimationBuilder<Color?>(
                duration: duration,
                curve: curve,
                tween: ColorTween(
                  end: selected ? AppColors.primary : AppColors.textSecondary,
                ),
                builder: (_, color, __) => Icon(icon, color: color, size: 24),
              ),
            ),
            const SizedBox(height: 3),
            AnimatedDefaultTextStyle(
              duration: duration,
              curve: curve,
              style: AppTextStyles.labelSmall.copyWith(
                color: selected ? AppColors.primary : AppColors.textSecondary,
                fontWeight: selected ? FontWeight.w700 : FontWeight.w500,
              ),
              child: Text(label),
            ),
          ],
        ),
      ),
    );
  }
}
