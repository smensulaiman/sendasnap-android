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
    with SingleTickerProviderStateMixin {
  final _formKey = GlobalKey<FormState>();
  final _emailCtrl = TextEditingController();
  final _passCtrl = TextEditingController();
  final _passFocus = FocusNode();
  bool _rememberMe = false;
  bool _isLoading = false;

  late final AnimationController _entryCtrl;
  late final Animation<double> _fadeAnim;
  late final Animation<Offset> _slideAnim;

  @override
  void initState() {
    super.initState();
    _entryCtrl = AnimationController(
      duration: const Duration(milliseconds: 800),
      vsync: this,
    );
    _fadeAnim = CurvedAnimation(parent: _entryCtrl, curve: Curves.easeOut);
    _slideAnim = Tween<Offset>(
      begin: const Offset(0, 0.08),
      end: Offset.zero,
    ).animate(CurvedAnimation(parent: _entryCtrl, curve: Curves.easeOut));
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
    _passFocus.dispose();
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
          .login(_emailCtrl.text.trim(), _passCtrl.text,
              rememberMe: _rememberMe);
      if (mounted) context.go('/main/home');
    } catch (e) {
      if (mounted) {
        AppSnackbar.error(
            context, e.toString().replaceFirst('Exception: ', ''));
      }
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      // Standard Flutter pattern: scaffold shrinks, scroll view handles the rest.
      resizeToAvoidBottomInset: true,
      body: Container(
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [Color(0xFF1565C0), AppColors.primary, Color(0xFF0D47A1)],
            stops: [0.0, 0.5, 1.0],
          ),
        ),
        child: SafeArea(
          child: FadeTransition(
            opacity: _fadeAnim,
            child: SlideTransition(
              position: _slideAnim,
              child: SingleChildScrollView(
                keyboardDismissBehavior:
                    ScrollViewKeyboardDismissBehavior.onDrag,
                padding: const EdgeInsets.symmetric(
                    horizontal: AppDimens.xxl, vertical: AppDimens.lg),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.center,
                  children: [
                    const Gap(AppDimens.xxl),

                    // ── Logo ────────────────────────────────────────────
                    Container(
                      padding: const EdgeInsets.all(20),
                      decoration: BoxDecoration(
                        color: Colors.white,
                        borderRadius: BorderRadius.circular(28),
                        boxShadow: [
                          BoxShadow(
                            color: Colors.black.withValues(alpha: 0.20),
                            blurRadius: 40,
                            offset: const Offset(0, 12),
                          ),
                        ],
                      ),
                      child: Image.asset(
                        'assets/images/logo.png',
                        width: 110,
                        height: 110,
                      ),
                    ),
                    const Gap(AppDimens.lg),
                    Text(
                      AppStrings.tagline,
                      style: AppTextStyles.bodySmall.copyWith(
                        color: Colors.white.withValues(alpha: 0.70),
                        letterSpacing: 0.5,
                      ),
                    ),

                    const Gap(AppDimens.xxxl),

                    // ── Form card ───────────────────────────────────────
                    Container(
                      width: double.infinity,
                      padding: const EdgeInsets.all(AppDimens.xxl),
                      decoration: BoxDecoration(
                        color: Colors.white.withValues(alpha: 0.10),
                        borderRadius: BorderRadius.circular(24),
                        border: Border.all(
                          color: Colors.white.withValues(alpha: 0.18),
                        ),
                      ),
                      child: Form(
                        key: _formKey,
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              'Sign in',
                              style: AppTextStyles.headlineMedium.copyWith(
                                color: Colors.white,
                                fontSize: 24,
                                fontWeight: FontWeight.w800,
                              ),
                            ),
                            const Gap(4),
                            Text(
                              'Enter your credentials to continue',
                              style: AppTextStyles.bodySmall.copyWith(
                                color: Colors.white.withValues(alpha: 0.65),
                              ),
                            ),
                            const Gap(AppDimens.xl),

                            _GlassField(
                              hint: AppStrings.emailLabel,
                              controller: _emailCtrl,
                              keyboardType: TextInputType.emailAddress,
                              prefixIcon: Icons.email_outlined,
                              validator: Validators.email,
                              textInputAction: TextInputAction.next,
                              onSubmitted: (_) => FocusScope.of(context)
                                  .requestFocus(_passFocus),
                            ),
                            const Gap(AppDimens.md),

                            _GlassPasswordField(
                              hint: AppStrings.passwordLabel,
                              controller: _passCtrl,
                              focusNode: _passFocus,
                              validator: Validators.required,
                            ),
                            const Gap(AppDimens.md),

                            _RememberMeRow(
                              value: _rememberMe,
                              onChanged: (v) =>
                                  setState(() => _rememberMe = v ?? false),
                            ),
                            const Gap(AppDimens.xl),

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
                                      Colors.white.withValues(alpha: 0.4),
                                  shape: RoundedRectangleBorder(
                                    borderRadius: BorderRadius.circular(14),
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
                                        style:
                                            AppTextStyles.titleMedium.copyWith(
                                          color: AppColors.primary,
                                          fontWeight: FontWeight.w700,
                                          fontSize: 15,
                                        ),
                                      ),
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),

                    const Gap(AppDimens.xxl),
                    Text(
                      AppStrings.copyright,
                      style: AppTextStyles.labelSmall.copyWith(
                        color: Colors.white.withValues(alpha: 0.35),
                      ),
                      textAlign: TextAlign.center,
                    ),
                    const Gap(AppDimens.lg),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

// ── Glass-style input field ────────────────────────────────────────────────

InputDecoration _glassDecoration({
  required String hint,
  required IconData prefixIcon,
  Widget? suffixIcon,
}) {
  return InputDecoration(
    hintText: hint,
    hintStyle: AppTextStyles.bodySmall
        .copyWith(color: Colors.white.withValues(alpha: 0.55)),
    prefixIcon:
        Icon(prefixIcon, color: Colors.white.withValues(alpha: 0.8), size: 20),
    suffixIcon: suffixIcon,
    filled: true,
    fillColor: Colors.white.withValues(alpha: 0.12),
    floatingLabelBehavior: FloatingLabelBehavior.never,
    border: OutlineInputBorder(
      borderRadius: BorderRadius.circular(14),
      borderSide: BorderSide(color: Colors.white.withValues(alpha: 0.25)),
    ),
    enabledBorder: OutlineInputBorder(
      borderRadius: BorderRadius.circular(14),
      borderSide: BorderSide(color: Colors.white.withValues(alpha: 0.25)),
    ),
    focusedBorder: OutlineInputBorder(
      borderRadius: BorderRadius.circular(14),
      borderSide: const BorderSide(color: Colors.white, width: 1.5),
    ),
    errorBorder: OutlineInputBorder(
      borderRadius: BorderRadius.circular(14),
      borderSide: const BorderSide(color: Color(0xFFFF8A80)),
    ),
    focusedErrorBorder: OutlineInputBorder(
      borderRadius: BorderRadius.circular(14),
      borderSide: const BorderSide(color: Color(0xFFFF8A80), width: 1.5),
    ),
    contentPadding: const EdgeInsets.symmetric(
      horizontal: AppDimens.lg,
      vertical: AppDimens.md + 2,
    ),
  );
}

class _GlassField extends StatelessWidget {
  final String hint;
  final TextEditingController? controller;
  final TextInputType keyboardType;
  final IconData prefixIcon;
  final String? Function(String?)? validator;
  final TextInputAction textInputAction;
  final ValueChanged<String>? onSubmitted;

  const _GlassField({
    required this.hint,
    this.controller,
    this.keyboardType = TextInputType.text,
    required this.prefixIcon,
    this.validator,
    this.textInputAction = TextInputAction.next,
    this.onSubmitted,
  });

  @override
  Widget build(BuildContext context) {
    return TextFormField(
      controller: controller,
      keyboardType: keyboardType,
      validator: validator,
      textInputAction: textInputAction,
      onFieldSubmitted: onSubmitted,
      style: AppTextStyles.bodyMedium.copyWith(color: Colors.white),
      decoration: _glassDecoration(hint: hint, prefixIcon: prefixIcon),
    );
  }
}

class _GlassPasswordField extends StatefulWidget {
  final String hint;
  final TextEditingController? controller;
  final FocusNode? focusNode;
  final String? Function(String?)? validator;

  const _GlassPasswordField({
    required this.hint,
    this.controller,
    this.focusNode,
    this.validator,
  });

  @override
  State<_GlassPasswordField> createState() => _GlassPasswordFieldState();
}

class _GlassPasswordFieldState extends State<_GlassPasswordField> {
  bool _obscure = true;

  @override
  Widget build(BuildContext context) {
    return TextFormField(
      controller: widget.controller,
      focusNode: widget.focusNode,
      obscureText: _obscure,
      validator: widget.validator,
      textInputAction: TextInputAction.done,
      style: AppTextStyles.bodyMedium.copyWith(color: Colors.white),
      decoration: _glassDecoration(
        hint: widget.hint,
        prefixIcon: Icons.lock_outline_rounded,
        suffixIcon: IconButton(
          icon: Icon(
            _obscure ? Icons.visibility_off_outlined : Icons.visibility_outlined,
            color: Colors.white.withValues(alpha: 0.7),
            size: 20,
          ),
          onPressed: () => setState(() => _obscure = !_obscure),
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
