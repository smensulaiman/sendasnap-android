import 'dart:math' as math;
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_dimens.dart';
import '../../../../core/constants/app_strings.dart';
import '../../../../core/constants/app_text_styles.dart';
import '../../../../core/utils/validators.dart';
import '../../../../shared/widgets/app_button.dart';
import '../../../../shared/widgets/app_text_field.dart';
import '../../../../shared/widgets/app_snackbar.dart';
import '../providers/auth_provider.dart';

class LoginScreen extends ConsumerStatefulWidget {
  const LoginScreen({super.key});

  @override
  ConsumerState<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends ConsumerState<LoginScreen>
    with TickerProviderStateMixin {
  final _formKey = GlobalKey<FormState>();
  final _emailCtrl = TextEditingController();
  final _passCtrl = TextEditingController();
  bool _rememberMe = false;
  bool _isLoading = false;

  late final AnimationController _gradientCtrl;
  late final Animation<double> _gradientAnim;

  @override
  void initState() {
    super.initState();
    _gradientCtrl = AnimationController(
      duration: const Duration(seconds: 4),
      vsync: this,
    )..repeat(reverse: true);
    _gradientAnim = CurvedAnimation(
      parent: _gradientCtrl,
      curve: Curves.easeInOut,
    );
    _prefill();
  }

  Future<void> _prefill() async {
    final storage = ref.read(localStorageProvider);
    if (!storage.isRememberMe()) return;
    final email = storage.getSavedEmail();
    final pass = await storage.getSavedPassword();
    if (email != null) _emailCtrl.text = email;
    if (pass != null) _passCtrl.text = pass;
    setState(() => _rememberMe = true);
  }

  @override
  void dispose() {
    _gradientCtrl.dispose();
    _emailCtrl.dispose();
    _passCtrl.dispose();
    super.dispose();
  }

  Future<void> _login() async {
    if (!(_formKey.currentState?.validate() ?? false)) return;
    setState(() => _isLoading = true);
    try {
      await ref.read(authProvider.notifier).login(
            _emailCtrl.text.trim(),
            _passCtrl.text,
            rememberMe: _rememberMe,
          );
      if (mounted) context.go('/main/home');
    } catch (e) {
      if (mounted) {
        AppSnackbar.error(
          context,
          e.toString().replaceFirst('Exception: ', ''),
        );
      }
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final size = MediaQuery.of(context).size;

    return Scaffold(
      backgroundColor: AppColors.scaffold,
      body: SingleChildScrollView(
        child: SizedBox(
          height: math.max(size.height, 600),
          child: Column(
            children: [
              _AnimatedHeader(
                animation: _gradientAnim,
                height: size.height * 0.42,
              ),
              Expanded(
                child: _FormCard(
                  formKey: _formKey,
                  emailCtrl: _emailCtrl,
                  passCtrl: _passCtrl,
                  rememberMe: _rememberMe,
                  isLoading: _isLoading,
                  onRememberMeChanged: (v) =>
                      setState(() => _rememberMe = v ?? false),
                  onLogin: _login,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

// ── Animated gradient header ───────────────────────────────────────────────

class _AnimatedHeader extends StatelessWidget {
  final Animation<double> animation;
  final double height;

  const _AnimatedHeader({
    required this.animation,
    required this.height,
  });

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: animation,
      builder: (context, child) {
        final t = animation.value;
        return Container(
          height: height,
          width: double.infinity,
          decoration: BoxDecoration(
            gradient: LinearGradient(
              begin: Alignment(-1 + t * 0.5, -1),
              end: Alignment(1 - t * 0.3, 1),
              colors: const [
                AppColors.primaryDark,
                AppColors.primary,
                Color(0xFF1976D2),
              ],
              stops: [0.0, 0.5 + t * 0.2, 1.0],
            ),
          ),
          child: child,
        );
      },
      child: Stack(
        children: [
          // Decorative circles
          Positioned(
            top: -40,
            right: -40,
            child: Container(
              width: 160,
              height: 160,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: AppColors.white.withOpacity(0.06),
              ),
            ),
          ),
          Positioned(
            bottom: 20,
            left: -30,
            child: Container(
              width: 120,
              height: 120,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: AppColors.white.withOpacity(0.05),
              ),
            ),
          ),
          // Content
          SafeArea(
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: AppDimens.xxl),
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  const SizedBox(height: AppDimens.xxl),
                  Container(
                    width: 72,
                    height: 72,
                    decoration: BoxDecoration(
                      color: AppColors.white.withOpacity(0.15),
                      shape: BoxShape.circle,
                      border: Border.all(
                          color: AppColors.white.withOpacity(0.4), width: 2),
                    ),
                    child: const Icon(
                      Icons.local_shipping_rounded,
                      color: AppColors.white,
                      size: 36,
                    ),
                  ),
                  const SizedBox(height: AppDimens.lg),
                  Text(
                    AppStrings.appName,
                    style: AppTextStyles.displayLarge.copyWith(
                      color: AppColors.white,
                      fontWeight: FontWeight.w800,
                      letterSpacing: 0.5,
                    ),
                  ),
                  const SizedBox(height: AppDimens.xs),
                  Text(
                    AppStrings.tagline,
                    style: AppTextStyles.bodyMedium.copyWith(
                      color: AppColors.white.withOpacity(0.75),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}

// ── Form card ──────────────────────────────────────────────────────────────

class _FormCard extends StatelessWidget {
  final GlobalKey<FormState> formKey;
  final TextEditingController emailCtrl;
  final TextEditingController passCtrl;
  final bool rememberMe;
  final bool isLoading;
  final ValueChanged<bool?> onRememberMeChanged;
  final VoidCallback onLogin;

  const _FormCard({
    required this.formKey,
    required this.emailCtrl,
    required this.passCtrl,
    required this.rememberMe,
    required this.isLoading,
    required this.onRememberMeChanged,
    required this.onLogin,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: const BoxDecoration(
        color: AppColors.scaffold,
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      padding: const EdgeInsets.fromLTRB(
          AppDimens.xxl, AppDimens.xxl, AppDimens.xxl, AppDimens.lg),
      child: Form(
        key: formKey,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Welcome back', style: AppTextStyles.headlineMedium),
            const SizedBox(height: AppDimens.xs),
            Text('Sign in to continue',
                style: AppTextStyles.bodySmall),
            const SizedBox(height: AppDimens.xxl),
            AppTextField(
              label: AppStrings.emailLabel,
              hint: AppStrings.emailHint,
              controller: emailCtrl,
              keyboardType: TextInputType.emailAddress,
              validator: Validators.email,
              textInputAction: TextInputAction.next,
            ),
            const SizedBox(height: AppDimens.lg),
            AppTextField(
              label: AppStrings.passwordLabel,
              controller: passCtrl,
              isPassword: true,
              validator: Validators.required,
              textInputAction: TextInputAction.done,
            ),
            const SizedBox(height: AppDimens.md),
            _RememberMeRow(
              value: rememberMe,
              onChanged: onRememberMeChanged,
            ),
            const SizedBox(height: AppDimens.xxl),
            AppButton(
              label: isLoading ? AppStrings.loggingIn : AppStrings.login,
              onPressed: isLoading ? null : onLogin,
              isLoading: isLoading,
            ),
            const Spacer(),
            Center(
              child: Text(
                AppStrings.copyright,
                style: AppTextStyles.labelSmall,
                textAlign: TextAlign.center,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _RememberMeRow extends StatelessWidget {
  final bool value;
  final ValueChanged<bool?> onChanged;

  const _RememberMeRow({required this.value, required this.onChanged});

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        SizedBox(
          width: 20,
          height: 20,
          child: Checkbox(
            value: value,
            onChanged: onChanged,
            activeColor: AppColors.primary,
            shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(4)),
          ),
        ),
        const SizedBox(width: AppDimens.sm),
        Text(AppStrings.rememberMe, style: AppTextStyles.bodySmall),
      ],
    );
  }
}
