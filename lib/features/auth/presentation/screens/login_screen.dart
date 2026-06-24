import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter/widget_previews.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:gap/gap.dart';
import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_strings.dart';
import '../../../../core/utils/validators.dart';
import '../../../../shared/widgets/app_snackbar.dart';
import '../providers/auth_provider.dart';

@Preview(name: 'Login screen')
Widget loginPreview() => const ProviderScope(child: LoginScreen());

class LoginScreen extends ConsumerStatefulWidget {
  const LoginScreen({super.key});

  @override
  ConsumerState<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends ConsumerState<LoginScreen> {
  final _formKey = GlobalKey<FormState>();
  final _emailCtrl = TextEditingController();
  final _passCtrl = TextEditingController();
  final _passFocus = FocusNode();
  bool _rememberMe = false;
  bool _isLoading = false;

  @override
  void initState() {
    super.initState();
    _prefill();
  }

  Future<void> _prefill() async {
    try {
      final storage = ref.read(localStorageProvider);
      if (!storage.isRememberMe()) return;
      final email = storage.getSavedEmail();
      final pass = await storage.getSavedPassword();
      if (email != null) _emailCtrl.text = email;
      if (pass != null) _passCtrl.text = pass;
      if (mounted) setState(() => _rememberMe = true);
    } catch (_) {
      // No-op in preview / uninitialised storage.
    }
  }

  @override
  void dispose() {
    _passFocus.dispose();
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
            context, e.toString().replaceFirst('Exception: ', ''));
      }
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final mq = MediaQuery.of(context);
    final screenH = mq.size.height;
    final topPad = mq.padding.top;
    final bottomPad = mq.padding.bottom;
    final kb = mq.viewInsets.bottom; // keyboard height (animates per-frame)

    // Progress 0 → 1 as the keyboard slides in. Drives the sheet expansion
    // smoothly without any setState/focus handling — that's the whole trick.
    final p = kb <= 0 ? 0.0 : (kb / (screenH * 0.36)).clamp(0.0, 1.0);

    // Sheet's top edge: low (≈48%) when closed, near the status bar when open.
    final collapsedTop = screenH * 0.48;
    final expandedTop = topPad + 12;
    final sheetTop = collapsedTop + (expandedTop - collapsedTop) * p;

    return AnnotatedRegion<SystemUiOverlayStyle>(
      value: SystemUiOverlayStyle.dark,
      child: Scaffold(
        // Never resize — the sheet positions itself on the keyboard manually.
        resizeToAvoidBottomInset: false,
        backgroundColor: Colors.white,
        body: Stack(
          children: [
            // ── White backdrop with soft blue blobs ────────────────────
            const Positioned(
              top: -90,
              right: -70,
              child: _Blob(size: 240, color: Color(0x222196F3)),
            ),
            const Positioned(
              top: 60,
              left: -90,
              child: _Blob(size: 200, color: Color(0x1442A5F5)),
            ),
            const Positioned(
              top: 260,
              right: -40,
              child: _Blob(size: 120, color: Color(0x1A64B5F6)),
            ),

            // ── Logo in the white area (fades out as keyboard opens) ───
            Positioned(
              top: topPad + 48,
              left: 0,
              right: 0,
              child: Opacity(
                opacity: (1 - p * 1.4).clamp(0.0, 1.0),
                child: Column(
                  children: [
                    Image.asset(
                      'assets/images/logo.png',
                      width: 96,
                      height: 96,
                    ),
                    const Gap(14),
                    Text(
                      AppStrings.tagline,
                      style: TextStyle(
                        color: AppColors.primary.withValues(alpha: 0.65),
                        fontSize: 13,
                        letterSpacing: 0.4,
                        fontWeight: FontWeight.w500,
                      ),
                    ),
                  ],
                ),
              ),
            ),

            // ── Blue sheet — bottom edge rests on the keyboard ─────────
            Positioned(
              left: 0,
              right: 0,
              top: sheetTop,
              bottom: kb, // sits exactly on top of the keyboard
              child: Container(
                decoration: const BoxDecoration(
                  color: AppColors.primary,
                  borderRadius: BorderRadius.vertical(
                    top: Radius.circular(32),
                  ),
                  boxShadow: [
                    BoxShadow(
                      color: Color(0x33000000),
                      blurRadius: 30,
                      offset: Offset(0, -8),
                    ),
                  ],
                ),
                child: ClipRRect(
                  borderRadius: const BorderRadius.vertical(
                    top: Radius.circular(32),
                  ),
                  child: SingleChildScrollView(
                    keyboardDismissBehavior:
                        ScrollViewKeyboardDismissBehavior.onDrag,
                    padding: EdgeInsets.fromLTRB(
                      28,
                      18,
                      28,
                      (kb > 0 ? 24 : bottomPad + 24),
                    ),
                    child: Form(
                      key: _formKey,
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.stretch,
                        children: [
                          // Grab handle
                          Center(
                            child: Container(
                              width: 40,
                              height: 4,
                              decoration: BoxDecoration(
                                color: Colors.white.withValues(alpha: 0.35),
                                borderRadius: BorderRadius.circular(2),
                              ),
                            ),
                          ),
                          const Gap(22),

                          const Text(
                            'Sign in',
                            style: TextStyle(
                              color: Colors.white,
                              fontSize: 26,
                              fontWeight: FontWeight.w800,
                              letterSpacing: -0.4,
                            ),
                          ),
                          const Gap(6),
                          Text(
                            'Enter your credentials to continue',
                            style: TextStyle(
                              color: Colors.white.withValues(alpha: 0.70),
                              fontSize: 14,
                            ),
                          ),
                          const Gap(26),

                          _WhiteField(
                            hint: AppStrings.emailLabel,
                            controller: _emailCtrl,
                            keyboardType: TextInputType.emailAddress,
                            prefixIcon: Icons.alternate_email_rounded,
                            validator: Validators.email,
                            textInputAction: TextInputAction.next,
                            onSubmitted: (_) => FocusScope.of(context)
                                .requestFocus(_passFocus),
                          ),
                          const Gap(14),

                          _WhitePasswordField(
                            controller: _passCtrl,
                            focusNode: _passFocus,
                            validator: Validators.required,
                          ),
                          const Gap(16),

                          Row(
                            children: [
                              SizedBox(
                                width: 22,
                                height: 22,
                                child: Checkbox(
                                  value: _rememberMe,
                                  onChanged: (v) =>
                                      setState(() => _rememberMe = v ?? false),
                                  activeColor: Colors.white,
                                  checkColor: AppColors.primary,
                                  side: BorderSide(
                                    color: Colors.white.withValues(alpha: 0.55),
                                    width: 1.5,
                                  ),
                                  shape: RoundedRectangleBorder(
                                    borderRadius: BorderRadius.circular(6),
                                  ),
                                  materialTapTargetSize:
                                      MaterialTapTargetSize.shrinkWrap,
                                ),
                              ),
                              const Gap(8),
                              Text(
                                AppStrings.rememberMe,
                                style: TextStyle(
                                  color: Colors.white.withValues(alpha: 0.85),
                                  fontSize: 13,
                                ),
                              ),
                            ],
                          ),
                          const Gap(26),

                          SizedBox(
                            height: 52,
                            child: ElevatedButton(
                              onPressed: _isLoading ? null : _login,
                              style: ElevatedButton.styleFrom(
                                backgroundColor: Colors.white,
                                foregroundColor: AppColors.primary,
                                elevation: 0,
                                disabledBackgroundColor:
                                    Colors.white.withValues(alpha: 0.6),
                                shape: RoundedRectangleBorder(
                                  borderRadius: BorderRadius.circular(14),
                                ),
                              ),
                              child: _isLoading
                                  ? const SizedBox(
                                      width: 22,
                                      height: 22,
                                      child: CircularProgressIndicator(
                                        strokeWidth: 2.2,
                                        color: AppColors.primary,
                                      ),
                                    )
                                  : const Row(
                                      mainAxisAlignment:
                                          MainAxisAlignment.center,
                                      children: [
                                        Text(
                                          'Sign In',
                                          style: TextStyle(
                                            fontSize: 16,
                                            fontWeight: FontWeight.w700,
                                            letterSpacing: 0.2,
                                          ),
                                        ),
                                        Gap(8),
                                        Icon(Icons.arrow_forward_rounded,
                                            size: 20),
                                      ],
                                    ),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

// ── Soft decorative blob ────────────────────────────────────────────────────

class _Blob extends StatelessWidget {
  final double size;
  final Color color;
  const _Blob({required this.size, required this.color});

  @override
  Widget build(BuildContext context) {
    return Container(
      width: size,
      height: size,
      decoration: BoxDecoration(shape: BoxShape.circle, color: color),
    );
  }
}

// ── White inputs ────────────────────────────────────────────────────────────

class _WhiteField extends StatelessWidget {
  final String hint;
  final TextEditingController? controller;
  final TextInputType keyboardType;
  final IconData prefixIcon;
  final String? Function(String?)? validator;
  final TextInputAction textInputAction;
  final ValueChanged<String>? onSubmitted;

  const _WhiteField({
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
      style: _inputStyle,
      decoration: _decoration(hint: hint, prefixIcon: prefixIcon),
    );
  }
}

class _WhitePasswordField extends StatefulWidget {
  final TextEditingController? controller;
  final FocusNode? focusNode;
  final String? Function(String?)? validator;

  const _WhitePasswordField({this.controller, this.focusNode, this.validator});

  @override
  State<_WhitePasswordField> createState() => _WhitePasswordFieldState();
}

class _WhitePasswordFieldState extends State<_WhitePasswordField> {
  bool _obscure = true;

  @override
  Widget build(BuildContext context) {
    return TextFormField(
      controller: widget.controller,
      focusNode: widget.focusNode,
      obscureText: _obscure,
      validator: widget.validator,
      textInputAction: TextInputAction.done,
      style: _inputStyle,
      decoration: _decoration(
        hint: AppStrings.passwordLabel,
        prefixIcon: Icons.lock_outline_rounded,
        suffixIcon: GestureDetector(
          onTap: () => setState(() => _obscure = !_obscure),
          child: Icon(
            _obscure
                ? Icons.visibility_off_outlined
                : Icons.visibility_outlined,
            color: const Color(0xFF94A3B8),
            size: 20,
          ),
        ),
      ),
    );
  }
}

const _inputStyle = TextStyle(
  fontSize: 15,
  color: Color(0xFF0F172A),
  fontWeight: FontWeight.w500,
);

InputDecoration _decoration({
  required String hint,
  required IconData prefixIcon,
  Widget? suffixIcon,
}) {
  const radius = BorderRadius.all(Radius.circular(14));
  return InputDecoration(
    hintText: hint,
    hintStyle: const TextStyle(color: Color(0xFF94A3B8), fontSize: 15),
    prefixIcon: Icon(prefixIcon, color: AppColors.primary, size: 20),
    suffixIcon: suffixIcon != null
        ? Padding(
            padding: const EdgeInsets.only(right: 14),
            child: suffixIcon,
          )
        : null,
    suffixIconConstraints: const BoxConstraints(minWidth: 46, minHeight: 46),
    filled: true,
    fillColor: Colors.white,
    floatingLabelBehavior: FloatingLabelBehavior.never,
    border: const OutlineInputBorder(
      borderRadius: radius,
      borderSide: BorderSide.none,
    ),
    enabledBorder: const OutlineInputBorder(
      borderRadius: radius,
      borderSide: BorderSide.none,
    ),
    focusedBorder: const OutlineInputBorder(
      borderRadius: radius,
      borderSide: BorderSide(color: Color(0xFF0D3C6E), width: 2),
    ),
    errorBorder: const OutlineInputBorder(
      borderRadius: radius,
      borderSide: BorderSide(color: AppColors.error),
    ),
    focusedErrorBorder: const OutlineInputBorder(
      borderRadius: radius,
      borderSide: BorderSide(color: AppColors.error, width: 2),
    ),
    contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
  );
}
