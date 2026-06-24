import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../features/auth/presentation/providers/auth_provider.dart';
import '../../features/auth/presentation/screens/splash_screen.dart';
import '../../features/auth/presentation/screens/login_screen.dart';
import '../../features/auth/presentation/screens/change_password_screen.dart';
import '../../features/home/presentation/screens/main_shell.dart';
import '../../features/home/presentation/screens/home_screen.dart';
import '../../features/vehicle/presentation/screens/vehicle_list_screen.dart';
import '../../features/vehicle/presentation/screens/vehicle_detail_screen.dart';
import '../../features/vehicle/data/models/vehicle_model.dart';
import '../../features/profile/presentation/screens/profile_screen.dart';
import '../../features/coming_soon/presentation/screens/coming_soon_screen.dart';

class _RouterNotifier extends ChangeNotifier {
  final Ref _ref;

  _RouterNotifier(this._ref) {
    _ref.listen(authProvider, (_, __) => notifyListeners());
  }

  String? redirect(BuildContext context, GoRouterState state) {
    final isLoggedIn = _ref.read(authProvider).isLoggedIn;
    final loc = state.matchedLocation;

    if (loc == '/splash') return null;

    if (!isLoggedIn && loc != '/login') return '/login';
    if (isLoggedIn && loc == '/login') return '/main/home';
    return null;
  }
}

final appRouterProvider = Provider<GoRouter>((ref) {
  final notifier = _RouterNotifier(ref);

  return GoRouter(
    initialLocation: '/splash',
    refreshListenable: notifier,
    redirect: notifier.redirect,
    routes: [
      GoRoute(
        path: '/splash',
        builder: (_, __) => const SplashScreen(),
      ),
      GoRoute(
        path: '/login',
        builder: (_, __) => const LoginScreen(),
      ),
      ShellRoute(
        builder: (_, __, child) => MainShell(child: child),
        routes: [
          GoRoute(
            path: '/main/home',
            pageBuilder: (_, __) =>
                const NoTransitionPage(child: HomeScreen()),
          ),
          GoRoute(
            path: '/main/task',
            pageBuilder: (_, __) => const NoTransitionPage(
                child: ComingSoonScreen(featureName: 'Task')),
          ),
          GoRoute(
            path: '/main/chat',
            pageBuilder: (_, __) => const NoTransitionPage(
                child: ComingSoonScreen(featureName: 'Chat')),
          ),
          GoRoute(
            path: '/main/profile',
            pageBuilder: (_, __) =>
                const NoTransitionPage(child: ProfileScreen()),
          ),
        ],
      ),
      GoRoute(
        path: '/vehicles/list',
        builder: (_, state) {
          final extra = state.extra as Map<String, dynamic>? ?? {};
          return VehicleListScreen(
            yardName: extra['yardName'] as String? ?? 'Yard Vehicles',
            company: extra['company'] as String? ?? 'acjl',
          );
        },
      ),
      GoRoute(
        path: '/vehicles/detail',
        builder: (_, state) {
          final vehicle = state.extra as VehicleModel;
          return VehicleDetailScreen(vehicle: vehicle);
        },
      ),
      GoRoute(
        path: '/profile/change-password',
        builder: (_, __) => const ChangePasswordScreen(),
      ),
      GoRoute(
        path: '/coming-soon',
        builder: (_, state) => ComingSoonScreen(
          featureName: state.extra as String?,
          showAppBar: true,
        ),
      ),
    ],
  );
});

