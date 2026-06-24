import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:gap/gap.dart';
import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_dimens.dart';
import '../../../../core/constants/app_strings.dart';
import '../../../../core/constants/app_text_styles.dart';
import '../../../../core/utils/validators.dart';
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

  late final AnimationController _entryCtrl;
  late final Animation<double> _logoFade;
  late final Animation<Offset> _cardSlide;
  late final Animation<double> _cardFade;

  @override
  void initState() {
    super.initState();
    _entryCtrl = AnimationController(
      duration: const Duration(milliseconds: 900),
      vsync: this,
    );
    _logoFade = CurvedAnimation(
      parent: _entryCtrl,
      curve: const Interval(0.0, 0.6, curve: Curves.easeOut),
    );
    _cardSlide = Tween<Offset>(
      begin: const Offset(0, 0.12),
      end: Offset.zero,
    ).animate(CurvedAnimation(
      parent: _entryCtrl,
      curve: const Interval(0.3, 1.0, curve: Curves.easeOut),
    ));
    _cardFade = CurvedAnimation(
      parent: _entryCtrl,
      curve: const Interval(0.3, 0.9, curve: Curves.easeOut),
    );
    _entryCtrl.forward();
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
    _entryCtrl.dispose();
    _emailCtrl.dispose();
    _passCtrl.dispose();
    super.dispose();
  }

  Future<void> _login() async {
    if (!(_formKey.currentState?.validate() ?? false)) return;
    setState(() => _isLoading = true);
    try {
      await ref
          .read(authProvider.notifier)
          .login(_emailCtrl.text.trim(), _passCtrl.text, rememberMe: _rememberMe);
      if (mounted) context.go('/main/home');
    } catch (e) {
      if (mounted) {
        AppSnackbar.error(context, e.toString().replaceFirst('Exception: ', ''));
      }
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final screenHeight = MediaQuery.sizeOf(context).height;
    final topPad = MediaQuery.paddingOf(context).top;
    final keyboardVisible = MediaQuery.viewInsetsOf(context).bottom > 80;

    return Scaffold(
      backgroundColor: Colors.white,
      resizeToAvoidBottomInset: true,
      body: Column(
        children: [
          // ── Logo / compact header — animates between states ────────
          AnimatedContainer(
            duration: const Duration(milliseconds: 280),
            curve: Curves.easeInOut,
            height: keyboardVisible ? topPad + 64 : screenHeight * 0.42,
            color: Colors.white,
            child: Stack(
              clipBehavior: Clip.none,
              children: [
                // Decorative blobs — only visible in expanded state
                if (!keyboardVisible) ...[
                  Positioned(
                    top: -70,
                    right: -70,
                    child: _Blob(size: 220, opacity: 0.07),
                  ),
                  Positioned(
                    top: topPad + 30,
                    left: -50,
                    child: _Blob(size: 110, opacity: 0.04),
                  ),
                ],

                // ── Expanded: big centered logo ──────────────────────
                AnimatedOpacity(
                  opacity: keyboardVisible ? 0 : 1,
                  duration: const Duration(milliseconds: 200),
                  child: IgnorePointer(
                    ignoring: keyboardVisible,
                    child: FadeTransition(
                      opacity: _logoFade,
                      child: Center(
                        child: Column(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            Container(
                              padding: const EdgeInsets.all(AppDimens.lg),
                              decoration: BoxDecoration(
                                color: Colors.white,
                                borderRadius: BorderRadius.circular(28),
                                boxShadow: [
                                  BoxShadow(
                                    color: AppColors.primary
                                        .withValues(alpha: 0.10),
                                    blurRadius: 32,
                                    spreadRadius: 2,
                                    offset: const Offset(0, 8),
                                  ),
                                  BoxShadow(
                                    color: Colors.black.withValues(alpha: 0.04),
                                    blurRadius: 8,
                                    offset: const Offset(0, 2),
                                  ),
                                ],
                              ),
                              child: Image.asset(
                                'assets/images/logo.png',
                                width: 130,
                                height: 130,
                              ),
                            ),
                            const Gap(AppDimens.lg),
                            Row(
                              mainAxisSize: MainAxisSize.min,
                              children: [
                                Container(
                                  width: 20,
                                  height: 2,
                                  decoration: BoxDecoration(
                                    color: AppColors.primary
                                        .withValues(alpha: 0.4),
                                    borderRadius: BorderRadius.circular(1),
                                  ),
                                ),
                                const Gap(AppDimens.sm),
                                Text(
                                  AppStrings.tagline,
                                  style: AppTextStyles.bodySmall.copyWith(
                                    color: AppColors.primary,
                                    letterSpacing: 0.4,
                                    fontWeight: FontWeight.w600,
                                  ),
                                ),
                                const Gap(AppDimens.sm),
                                Container(
                                  width: 20,
                                  height: 2,
                                  decoration: BoxDecoration(
                                    color: AppColors.primary
                                        .withValues(alpha: 0.4),
                                    borderRadius: BorderRadius.circular(1),
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

                // ── Compact: small logo top-left in AppBar row ────────
                AnimatedOpacity(
                  opacity: keyboardVisible ? 1 : 0,
                  duration: const Duration(milliseconds: 200),
                  child: IgnorePointer(
                    ignoring: !keyboardVisible,
                    child: Padding(
                      padding: EdgeInsets.only(top: topPad),
                      child: SizedBox(
                        height: 64,
                        child: Row(
                          children: [
                            const Gap(AppDimens.lg),
                            ClipRRect(
                              borderRadius: BorderRadius.circular(10),
                              child: Image.asset(
                                'assets/images/logo.png',
                                width: 40,
                                height: 40,
                              ),
                            ),
                            const Gap(AppDimens.md),
                            Text(
                              'Sign in',
                              style: AppTextStyles.headlineMedium.copyWith(
                                color: AppColors.textPrimary,
                                fontSize: 20,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ),

          // ── Blue form card ─────────────────────────────────────────
          Expanded(
            child: SlideTransition(
              position: _cardSlide,
              child: FadeTransition(
                opacity: _cardFade,
                child: AnimatedContainer(
                  duration: const Duration(milliseconds: 280),
                  curve: Curves.easeInOut,
                  decoration: BoxDecoration(
                    color: AppColors.primary,
                    borderRadius: BorderRadius.vertical(
                      top: Radius.circular(keyboardVisible ? 20 : 36),
                    ),
                  ),
                  child: Stack(
                    children: [
                      Positioned(
                        top: -60,
                        right: -60,
                        child: Container(
                          width: 180,
                          height: 180,
                          decoration: BoxDecoration(
                            shape: BoxShape.circle,
                            color: Colors.white.withValues(alpha: 0.05),
                          ),
                        ),
                      ),
                      Positioned(
                        bottom: -40,
                        left: -40,
                        child: Container(
                          width: 140,
                          height: 140,
                          decoration: BoxDecoration(
                            shape: BoxShape.circle,
                            color: Colors.white.withValues(alpha: 0.04),
                          ),
                        ),
                      ),
                      SafeArea(
                        top: false,
                        child: SingleChildScrollView(
                          keyboardDismissBehavior:
                              ScrollViewKeyboardDismissBehavior.onDrag,
                          padding: EdgeInsets.fromLTRB(
                            AppDimens.xxl,
                            keyboardVisible
                                ? AppDimens.lg
                                : AppDimens.xxl,
                            AppDimens.xxl,
                            AppDimens.lg,
                          ),
                          child: Form(
                            key: _formKey,
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                // Handle bar — hidden when keyboard is up
                                if (!keyboardVisible)
                                  Center(
                                    child: Container(
                                      width: 36,
                                      height: 4,
                                      margin: const EdgeInsets.only(
                                          bottom: AppDimens.lg),
                                      decoration: BoxDecoration(
                                        color: Colors.white
                                            .withValues(alpha: 0.3),
                                        borderRadius:
                                            BorderRadius.circular(2),
                                      ),
                                    ),
                                  ),
                                if (!keyboardVisible) ...[
                                  Text(
                                    'Sign in',
                                    style: AppTextStyles.headlineMedium
                                        .copyWith(
                                      color: Colors.white,
                                      fontSize: 26,
                                      fontWeight: FontWeight.w800,
                                    ),
                                  ),
                                  const Gap(4),
                                  Text(
                                    'Enter your credentials to continue',
                                    style: AppTextStyles.bodySmall.copyWith(
                                      color:
                                          Colors.white.withValues(alpha: 0.7),
                                    ),
                                  ),
                                  const Gap(AppDimens.xxl),
                                ],

                                _WhiteField(
                                  hint: AppStrings.emailLabel,
                                  subHint: AppStrings.emailHint,
                                  controller: _emailCtrl,
                                  keyboardType: TextInputType.emailAddress,
                                  prefixIcon: Icons.email_outlined,
                                  validator: Validators.email,
                                  textInputAction: TextInputAction.next,
                                ),
                                const Gap(AppDimens.md),

                                _WhitePasswordField(
                                  hint: AppStrings.passwordLabel,
                                  controller: _passCtrl,
                                  validator: Validators.required,
                                ),
                                const Gap(AppDimens.md),

                                _RememberMeRow(
                                  value: _rememberMe,
                                  onChanged: (v) => setState(
                                      () => _rememberMe = v ?? false),
                                ),
                                const Gap(AppDimens.xxl),

                                SizedBox(
                                  width: double.infinity,
                                  height: AppDimens.buttonHeight,
                                  child: ElevatedButton(
                                    onPressed: _isLoading ? null : _login,
                                    style: ElevatedButton.styleFrom(
                                      backgroundColor: Colors.white,
                                      foregroundColor: AppColors.primary,
                                      elevation: 0,
                                      disabledBackgroundColor:
                                          Colors.white.withValues(alpha: 0.5),
                                      shape: RoundedRectangleBorder(
                                        borderRadius:
                                            BorderRadius.circular(14),
                                      ),
                                    ),
                                    child: _isLoading
                                        ? const SizedBox(
                                            width: 20,
                                            height: 20,
                                            child: CircularProgressIndicator(
                                              strokeWidth: 2,
                                              color: AppColors.primary,
                                            ),
                                          )
                                        : Text(
                                            AppStrings.login,
                                            style: AppTextStyles.titleMedium
                                                .copyWith(
                                              color: AppColors.primary,
                                              fontWeight: FontWeight.w700,
                                              fontSize: 15,
                                            ),
                                          ),
                                  ),
                                ),
                                const Gap(AppDimens.xxl),

                                Center(
                                  child: Text(
                                    AppStrings.copyright,
                                    style: AppTextStyles.labelSmall.copyWith(
                                      color:
                                          Colors.white.withValues(alpha: 0.4),
                                    ),
                                    textAlign: TextAlign.center,
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
              ),
            ),
          ),
        ],
      ),
    );
  }
}

// ── Decorative blob ────────────────────────────────────────────────────────

class _Blob extends StatelessWidget {
  final double size;
  final double opacity;
  const _Blob({required this.size, required this.opacity});

  @override
  Widget build(BuildContext context) {
    return Container(
      width: size,
      height: size,
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        color: AppColors.primary.withValues(alpha: opacity),
      ),
    );
  }
}

// ── Input field — hint only, no floating label ─────────────────────────────

InputDecoration _whiteDecoration({
  required String hint,
  String? subHint,
  required IconData prefixIcon,
  Widget? suffixIcon,
}) {
  return InputDecoration(
    hintText: hint,
    hintStyle: AppTextStyles.bodySmall.copyWith(color: AppColors.textSecondary),
    helperText: subHint,
    helperStyle:
        AppTextStyles.labelSmall.copyWith(color: AppColors.textHint),
    prefixIcon: Icon(prefixIcon, color: AppColors.primary, size: 20),
    suffixIcon: suffixIcon,
    filled: true,
    fillColor: Colors.white,
    floatingLabelBehavior: FloatingLabelBehavior.never,
    border: OutlineInputBorder(
      borderRadius: BorderRadius.circular(14),
      borderSide: BorderSide.none,
    ),
    enabledBorder: OutlineInputBorder(
      borderRadius: BorderRadius.circular(14),
      borderSide: BorderSide.none,
    ),
    focusedBorder: OutlineInputBorder(
      borderRadius: BorderRadius.circular(14),
      borderSide: const BorderSide(color: AppColors.primary, width: 2),
    ),
    errorBorder: OutlineInputBorder(
      borderRadius: BorderRadius.circular(14),
      borderSide: const BorderSide(color: AppColors.error),
    ),
    focusedErrorBorder: OutlineInputBorder(
      borderRadius: BorderRadius.circular(14),
      borderSide: const BorderSide(color: AppColors.error, width: 2),
    ),
    contentPadding: const EdgeInsets.symmetric(
      horizontal: AppDimens.lg,
      vertical: AppDimens.md + 2,
    ),
  );
}

class _WhiteField extends StatelessWidget {
  final String hint;
  final String? subHint;
  final TextEditingController? controller;
  final TextInputType keyboardType;
  final IconData prefixIcon;
  final String? Function(String?)? validator;
  final TextInputAction textInputAction;

  const _WhiteField({
    required this.hint,
    this.subHint,
    this.controller,
    this.keyboardType = TextInputType.text,
    required this.prefixIcon,
    this.validator,
    this.textInputAction = TextInputAction.next,
  });

  @override
  Widget build(BuildContext context) {
    return TextFormField(
      controller: controller,
      keyboardType: keyboardType,
      validator: validator,
      textInputAction: textInputAction,
      style: AppTextStyles.bodyMedium.copyWith(color: AppColors.textPrimary),
      decoration: _whiteDecoration(hint: hint, subHint: subHint, prefixIcon: prefixIcon),
    );
  }
}

class _WhitePasswordField extends StatefulWidget {
  final String hint;
  final TextEditingController? controller;
  final String? Function(String?)? validator;

  const _WhitePasswordField({
    required this.hint,
    this.controller,
    this.validator,
  });

  @override
  State<_WhitePasswordField> createState() => _WhitePasswordFieldState();
}

class _WhitePasswordFieldState extends State<_WhitePasswordField> {
  bool _obscure = true;

  @override
  Widget build(BuildContext context) {
    return TextFormField(
      controller: widget.controller,
      obscureText: _obscure,
      validator: widget.validator,
      textInputAction: TextInputAction.done,
      style: AppTextStyles.bodyMedium.copyWith(color: AppColors.textPrimary),
      decoration: _whiteDecoration(
        hint: widget.hint,
        prefixIcon: Icons.lock_outline_rounded,
        suffixIcon: IconButton(
          icon: Icon(
            _obscure ? Icons.visibility_off_outlined : Icons.visibility_outlined,
            color: AppColors.textSecondary,
            size: 20,
          ),
          onPressed: () => setState(() => _obscure = !_obscure),
        ),
      ),
    );
  }
}

// ── Remember me row ────────────────────────────────────────────────────────

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
            activeColor: Colors.white,
            checkColor: AppColors.primary,
            side: BorderSide(
              color: Colors.white.withValues(alpha: 0.5),
              width: 1.5,
            ),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(4),
            ),
          ),
        ),
        const Gap(AppDimens.sm),
        Text(
          AppStrings.rememberMe,
          style: AppTextStyles.bodySmall.copyWith(
            color: Colors.white.withValues(alpha: 0.8),
          ),
        ),
      ],
    );
  }
}
