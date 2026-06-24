import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:gap/gap.dart';
import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_strings.dart';
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
      duration: const Duration(milliseconds: 700),
      vsync: this,
    );
    _fadeAnim = CurvedAnimation(parent: _entryCtrl, curve: Curves.easeOut);
    _slideAnim = Tween<Offset>(
      begin: const Offset(0, 0.06),
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
    // Make status bar icons white over the dark gradient
    return AnnotatedRegion<SystemUiOverlayStyle>(
      value: SystemUiOverlayStyle.light,
      child: Scaffold(
        // Match the darkest gradient stop so the scaffold bg itself is never
        // visible — this is what eliminates the white block at the bottom.
        backgroundColor: const Color(0xFF0D47A1),
        resizeToAvoidBottomInset: true,
        body: SizedBox.expand(
          child: DecoratedBox(
            decoration: const BoxDecoration(
              gradient: LinearGradient(
                begin: Alignment.topCenter,
                end: Alignment.bottomCenter,
                colors: [Color(0xFF1A73E8), Color(0xFF1565C0), Color(0xFF0D47A1)],
                stops: [0.0, 0.55, 1.0],
              ),
            ),
            child: FadeTransition(
              opacity: _fadeAnim,
              child: SlideTransition(
                position: _slideAnim,
                child: SingleChildScrollView(
                  keyboardDismissBehavior:
                      ScrollViewKeyboardDismissBehavior.onDrag,
                  child: Padding(
                    padding: EdgeInsets.fromLTRB(
                      24,
                      MediaQuery.paddingOf(context).top + 32,
                      24,
                      MediaQuery.paddingOf(context).bottom + 24,
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        // ── Logo + title ─────────────────────────────────
                        Center(
                          child: Column(
                            children: [
                              Container(
                                width: 88,
                                height: 88,
                                padding: const EdgeInsets.all(12),
                                decoration: BoxDecoration(
                                  color: Colors.white,
                                  borderRadius: BorderRadius.circular(24),
                                  boxShadow: [
                                    BoxShadow(
                                      color: Colors.black.withValues(alpha: 0.22),
                                      blurRadius: 24,
                                      offset: const Offset(0, 8),
                                    ),
                                  ],
                                ),
                                child: Image.asset('assets/images/logo.png'),
                              ),
                              const Gap(16),
                              Text(
                                AppStrings.tagline,
                                style: const TextStyle(
                                  color: Colors.white70,
                                  fontSize: 13,
                                  letterSpacing: 0.4,
                                ),
                              ),
                            ],
                          ),
                        ),

                        const Gap(40),

                        // ── Form card ─────────────────────────────────────
                        Container(
                          decoration: BoxDecoration(
                            color: Colors.white,
                            borderRadius: BorderRadius.circular(20),
                            boxShadow: [
                              BoxShadow(
                                color: Colors.black.withValues(alpha: 0.15),
                                blurRadius: 32,
                                offset: const Offset(0, 12),
                              ),
                            ],
                          ),
                          padding: const EdgeInsets.fromLTRB(24, 28, 24, 24),
                          child: Form(
                            key: _formKey,
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.stretch,
                              children: [
                                const Text(
                                  'Sign in',
                                  style: TextStyle(
                                    fontSize: 22,
                                    fontWeight: FontWeight.w800,
                                    color: Color(0xFF1A1A2E),
                                    letterSpacing: -0.3,
                                  ),
                                ),
                                const Gap(4),
                                const Text(
                                  'Welcome back — enter your credentials',
                                  style: TextStyle(
                                    fontSize: 13,
                                    color: Color(0xFF6B7280),
                                  ),
                                ),
                                const Gap(24),

                                _FormField(
                                  label: 'Email',
                                  hint: AppStrings.emailHint,
                                  controller: _emailCtrl,
                                  keyboardType: TextInputType.emailAddress,
                                  prefixIcon: Icons.email_outlined,
                                  validator: Validators.email,
                                  textInputAction: TextInputAction.next,
                                  onSubmitted: (_) => FocusScope.of(context)
                                      .requestFocus(_passFocus),
                                ),
                                const Gap(16),

                                _PasswordField(
                                  label: 'Password',
                                  controller: _passCtrl,
                                  focusNode: _passFocus,
                                  validator: Validators.required,
                                ),
                                const Gap(14),

                                Row(
                                  children: [
                                    SizedBox(
                                      width: 20,
                                      height: 20,
                                      child: Checkbox(
                                        value: _rememberMe,
                                        onChanged: (v) => setState(
                                            () => _rememberMe = v ?? false),
                                        activeColor: AppColors.primary,
                                        shape: RoundedRectangleBorder(
                                          borderRadius:
                                              BorderRadius.circular(4),
                                        ),
                                        side: const BorderSide(
                                          color: Color(0xFFD1D5DB),
                                          width: 1.5,
                                        ),
                                      ),
                                    ),
                                    const Gap(8),
                                    const Text(
                                      'Remember me',
                                      style: TextStyle(
                                        fontSize: 13,
                                        color: Color(0xFF6B7280),
                                      ),
                                    ),
                                  ],
                                ),

                                const Gap(24),

                                SizedBox(
                                  height: 50,
                                  child: ElevatedButton(
                                    onPressed: _isLoading ? null : _login,
                                    style: ElevatedButton.styleFrom(
                                      backgroundColor: AppColors.primary,
                                      foregroundColor: Colors.white,
                                      elevation: 0,
                                      disabledBackgroundColor:
                                          AppColors.primary.withValues(alpha: 0.5),
                                      shape: RoundedRectangleBorder(
                                        borderRadius: BorderRadius.circular(12),
                                      ),
                                    ),
                                    child: _isLoading
                                        ? const SizedBox(
                                            width: 20,
                                            height: 20,
                                            child: CircularProgressIndicator(
                                              strokeWidth: 2,
                                              color: Colors.white,
                                            ),
                                          )
                                        : const Text(
                                            'Sign In',
                                            style: TextStyle(
                                              fontSize: 15,
                                              fontWeight: FontWeight.w700,
                                              letterSpacing: 0.2,
                                            ),
                                          ),
                                  ),
                                ),
                              ],
                            ),
                          ),
                        ),

                        const Gap(24),
                        Text(
                          AppStrings.copyright,
                          textAlign: TextAlign.center,
                          style: const TextStyle(
                            fontSize: 11,
                            color: Colors.white38,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

// ── Form field ────────────────────────────────────────────────────────────

class _FormField extends StatelessWidget {
  final String label;
  final String hint;
  final TextEditingController? controller;
  final TextInputType keyboardType;
  final IconData prefixIcon;
  final String? Function(String?)? validator;
  final TextInputAction textInputAction;
  final ValueChanged<String>? onSubmitted;

  const _FormField({
    required this.label,
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
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(label, style: _labelStyle),
        const Gap(6),
        TextFormField(
          controller: controller,
          keyboardType: keyboardType,
          validator: validator,
          textInputAction: textInputAction,
          onFieldSubmitted: onSubmitted,
          style: _inputStyle,
          decoration: _decoration(hint: hint, prefixIcon: prefixIcon),
        ),
      ],
    );
  }
}

class _PasswordField extends StatefulWidget {
  final String label;
  final TextEditingController? controller;
  final FocusNode? focusNode;
  final String? Function(String?)? validator;

  const _PasswordField({
    required this.label,
    this.controller,
    this.focusNode,
    this.validator,
  });

  @override
  State<_PasswordField> createState() => _PasswordFieldState();
}

class _PasswordFieldState extends State<_PasswordField> {
  bool _obscure = true;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(widget.label, style: _labelStyle),
        const Gap(6),
        TextFormField(
          controller: widget.controller,
          focusNode: widget.focusNode,
          obscureText: _obscure,
          validator: widget.validator,
          textInputAction: TextInputAction.done,
          style: _inputStyle,
          decoration: _decoration(
            hint: '••••••••',
            prefixIcon: Icons.lock_outline_rounded,
            suffixIcon: GestureDetector(
              onTap: () => setState(() => _obscure = !_obscure),
              child: Icon(
                _obscure
                    ? Icons.visibility_off_outlined
                    : Icons.visibility_outlined,
                color: const Color(0xFF9CA3AF),
                size: 20,
              ),
            ),
          ),
        ),
      ],
    );
  }
}

// ── Shared styles ─────────────────────────────────────────────────────────

const _labelStyle = TextStyle(
  fontSize: 13,
  fontWeight: FontWeight.w600,
  color: Color(0xFF374151),
  letterSpacing: 0.1,
);

const _inputStyle = TextStyle(
  fontSize: 15,
  color: Color(0xFF1A1A2E),
);

InputDecoration _decoration({
  required String hint,
  required IconData prefixIcon,
  Widget? suffixIcon,
}) {
  const radius = BorderRadius.all(Radius.circular(12));
  const border = OutlineInputBorder(
    borderRadius: radius,
    borderSide: BorderSide(color: Color(0xFFE5E7EB)),
  );
  return InputDecoration(
    hintText: hint,
    hintStyle: const TextStyle(color: Color(0xFFD1D5DB), fontSize: 15),
    prefixIcon: Icon(prefixIcon, color: Color(0xFF9CA3AF), size: 20),
    suffixIcon: suffixIcon != null
        ? Padding(
            padding: const EdgeInsets.only(right: 12),
            child: suffixIcon,
          )
        : null,
    suffixIconConstraints:
        const BoxConstraints(minWidth: 44, minHeight: 44),
    filled: true,
    fillColor: const Color(0xFFF9FAFB),
    floatingLabelBehavior: FloatingLabelBehavior.never,
    border: border,
    enabledBorder: border,
    focusedBorder: const OutlineInputBorder(
      borderRadius: radius,
      borderSide: BorderSide(color: AppColors.primary, width: 1.5),
    ),
    errorBorder: const OutlineInputBorder(
      borderRadius: radius,
      borderSide: BorderSide(color: AppColors.error),
    ),
    focusedErrorBorder: const OutlineInputBorder(
      borderRadius: radius,
      borderSide: BorderSide(color: AppColors.error, width: 1.5),
    ),
    contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
  );
}
