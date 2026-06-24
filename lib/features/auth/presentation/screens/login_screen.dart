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

class _LoginScreenState extends ConsumerState<LoginScreen>
    with TickerProviderStateMixin {
  final _formKey = GlobalKey<FormState>();
  final _emailCtrl = TextEditingController();
  final _passCtrl = TextEditingController();
  final _passFocus = FocusNode();
  bool _rememberMe = false;
  bool _isLoading = false;

  late final AnimationController _entryCtrl;
  late final AnimationController _bgCtrl;
  late final Animation<double> _fadeAnim;
  late final Animation<Offset> _slideAnim;

  @override
  void initState() {
    super.initState();
    _entryCtrl = AnimationController(
      duration: const Duration(milliseconds: 800),
      vsync: this,
    );
    _bgCtrl = AnimationController(
      duration: const Duration(seconds: 12),
      vsync: this,
    )..repeat(reverse: true);
    _fadeAnim = CurvedAnimation(parent: _entryCtrl, curve: Curves.easeOut);
    _slideAnim = Tween<Offset>(
      begin: const Offset(0, 0.10),
      end: Offset.zero,
    ).animate(
        CurvedAnimation(parent: _entryCtrl, curve: Curves.easeOutCubic));
    _entryCtrl.forward();
    _prefill();
  }

  Future<void> _prefill() async {
    // Guarded so the widget preview (which has no real provider overrides)
    // doesn't throw on startup.
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
    _entryCtrl.dispose();
    _bgCtrl.dispose();
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
    return AnnotatedRegion<SystemUiOverlayStyle>(
      value: SystemUiOverlayStyle.light,
      child: Scaffold(
        backgroundColor: const Color(0xFF071A33),
        resizeToAvoidBottomInset: true,
        body: SizedBox.expand(
          child: Stack(
            children: [
              // ── Layered gradient backdrop ───────────────────────────
              const DecoratedBox(
                decoration: BoxDecoration(
                  gradient: LinearGradient(
                    begin: Alignment.topCenter,
                    end: Alignment.bottomCenter,
                    colors: [
                      Color(0xFF1565C0),
                      Color(0xFF0D3C6E),
                      Color(0xFF071A33),
                    ],
                    stops: [0.0, 0.55, 1.0],
                  ),
                ),
                child: SizedBox.expand(),
              ),

              // ── Floating decorative orbs ────────────────────────────
              AnimatedBuilder(
                animation: _bgCtrl,
                builder: (context, _) {
                  final t = _bgCtrl.value;
                  return Stack(
                    children: [
                      Positioned(
                        top: -90 + t * 26,
                        right: -70,
                        child: _Orb(
                          size: 240,
                          colors: const [Color(0x3360A5FF), Color(0x00000000)],
                        ),
                      ),
                      Positioned(
                        top: 160 - t * 34,
                        left: -100,
                        child: _Orb(
                          size: 220,
                          colors: const [Color(0x2A4FC3F7), Color(0x00000000)],
                        ),
                      ),
                      Positioned(
                        bottom: -110 + t * 22,
                        right: -50,
                        child: _Orb(
                          size: 280,
                          colors: const [Color(0x2A1E88E5), Color(0x00000000)],
                        ),
                      ),
                    ],
                  );
                },
              ),

              // ── Content ─────────────────────────────────────────────
              FadeTransition(
                opacity: _fadeAnim,
                child: SlideTransition(
                  position: _slideAnim,
                  child: SingleChildScrollView(
                    keyboardDismissBehavior:
                        ScrollViewKeyboardDismissBehavior.onDrag,
                    child: Padding(
                      padding: EdgeInsets.fromLTRB(
                        24,
                        MediaQuery.paddingOf(context).top + 56,
                        24,
                        MediaQuery.paddingOf(context).bottom + 24,
                      ),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.stretch,
                        children: [
                          // Welcome header
                          const Text(
                            'Welcome\nback',
                            style: TextStyle(
                              color: Colors.white,
                              fontSize: 38,
                              height: 1.05,
                              fontWeight: FontWeight.w800,
                              letterSpacing: -1.0,
                            ),
                          ),
                          const Gap(10),
                          Text(
                            AppStrings.tagline,
                            style: TextStyle(
                              color: Colors.white.withValues(alpha: 0.72),
                              fontSize: 14,
                              letterSpacing: 0.3,
                            ),
                          ),

                          const Gap(36),

                          // Card with floating logo badge overlapping top
                          Stack(
                            clipBehavior: Clip.none,
                            alignment: Alignment.topCenter,
                            children: [
                              Container(
                                margin: const EdgeInsets.only(top: 36),
                                decoration: BoxDecoration(
                                  color: Colors.white,
                                  borderRadius: BorderRadius.circular(28),
                                  boxShadow: [
                                    BoxShadow(
                                      color: const Color(0xFF071A33)
                                          .withValues(alpha: 0.30),
                                      blurRadius: 40,
                                      offset: const Offset(0, 20),
                                    ),
                                  ],
                                ),
                                padding:
                                    const EdgeInsets.fromLTRB(24, 52, 24, 28),
                                child: Form(
                                  key: _formKey,
                                  child: Column(
                                    crossAxisAlignment:
                                        CrossAxisAlignment.stretch,
                                    children: [
                                      const Center(
                                        child: Text(
                                          'Sign in to continue',
                                          style: TextStyle(
                                            fontSize: 15,
                                            fontWeight: FontWeight.w600,
                                            color: Color(0xFF111827),
                                          ),
                                        ),
                                      ),
                                      const Gap(24),

                                      _FieldLabel('Email'),
                                      const Gap(7),
                                      _InputField(
                                        hint: AppStrings.emailHint,
                                        controller: _emailCtrl,
                                        keyboardType:
                                            TextInputType.emailAddress,
                                        prefixIcon: Icons.alternate_email_rounded,
                                        validator: Validators.email,
                                        textInputAction: TextInputAction.next,
                                        onSubmitted: (_) =>
                                            FocusScope.of(context)
                                                .requestFocus(_passFocus),
                                      ),
                                      const Gap(18),

                                      _FieldLabel('Password'),
                                      const Gap(7),
                                      _PasswordField(
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
                                              onChanged: (v) => setState(() =>
                                                  _rememberMe = v ?? false),
                                              activeColor: AppColors.primary,
                                              shape: RoundedRectangleBorder(
                                                borderRadius:
                                                    BorderRadius.circular(6),
                                              ),
                                              side: const BorderSide(
                                                color: Color(0xFFCBD5E1),
                                                width: 1.5,
                                              ),
                                              materialTapTargetSize:
                                                  MaterialTapTargetSize
                                                      .shrinkWrap,
                                            ),
                                          ),
                                          const Gap(8),
                                          const Text(
                                            'Remember me',
                                            style: TextStyle(
                                              fontSize: 13,
                                              color: Color(0xFF64748B),
                                            ),
                                          ),
                                        ],
                                      ),

                                      const Gap(24),

                                      _GradientButton(
                                        isLoading: _isLoading,
                                        onPressed: _login,
                                      ),
                                    ],
                                  ),
                                ),
                              ),

                              // Floating logo badge
                              Container(
                                width: 72,
                                height: 72,
                                padding: const EdgeInsets.all(11),
                                decoration: BoxDecoration(
                                  color: Colors.white,
                                  borderRadius: BorderRadius.circular(20),
                                  boxShadow: [
                                    BoxShadow(
                                      color: const Color(0xFF071A33)
                                          .withValues(alpha: 0.35),
                                      blurRadius: 20,
                                      offset: const Offset(0, 8),
                                    ),
                                  ],
                                ),
                                child:
                                    Image.asset('assets/images/logo.png'),
                              ),
                            ],
                          ),

                          const Gap(28),
                          Center(
                            child: Text(
                              AppStrings.copyright,
                              style: TextStyle(
                                fontSize: 11,
                                color: Colors.white.withValues(alpha: 0.45),
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
        ),
      ),
    );
  }
}

// ── Decorative glowing orb ──────────────────────────────────────────────────

class _Orb extends StatelessWidget {
  final double size;
  final List<Color> colors;
  const _Orb({required this.size, required this.colors});

  @override
  Widget build(BuildContext context) {
    return Container(
      width: size,
      height: size,
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        gradient: RadialGradient(colors: colors),
      ),
    );
  }
}

// ── Field label ─────────────────────────────────────────────────────────────

class _FieldLabel extends StatelessWidget {
  final String text;
  const _FieldLabel(this.text);

  @override
  Widget build(BuildContext context) {
    return Text(
      text,
      style: const TextStyle(
        fontSize: 13,
        fontWeight: FontWeight.w600,
        color: Color(0xFF334155),
        letterSpacing: 0.1,
      ),
    );
  }
}

// ── Inputs ──────────────────────────────────────────────────────────────────

class _InputField extends StatelessWidget {
  final String hint;
  final TextEditingController? controller;
  final TextInputType keyboardType;
  final IconData prefixIcon;
  final String? Function(String?)? validator;
  final TextInputAction textInputAction;
  final ValueChanged<String>? onSubmitted;

  const _InputField({
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

class _PasswordField extends StatefulWidget {
  final TextEditingController? controller;
  final FocusNode? focusNode;
  final String? Function(String?)? validator;

  const _PasswordField({this.controller, this.focusNode, this.validator});

  @override
  State<_PasswordField> createState() => _PasswordFieldState();
}

class _PasswordFieldState extends State<_PasswordField> {
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
        hint: '••••••••',
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

// ── Gradient button ──────────────────────────────────────────────────────────

class _GradientButton extends StatelessWidget {
  final bool isLoading;
  final VoidCallback onPressed;
  const _GradientButton({required this.isLoading, required this.onPressed});

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.transparent,
      child: InkWell(
        borderRadius: BorderRadius.circular(14),
        onTap: isLoading ? null : onPressed,
        child: Ink(
          height: 52,
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(14),
            gradient: const LinearGradient(
              colors: [Color(0xFF1E88E5), Color(0xFF1565C0)],
            ),
            boxShadow: [
              BoxShadow(
                color: const Color(0xFF1565C0).withValues(alpha: 0.42),
                blurRadius: 16,
                offset: const Offset(0, 8),
              ),
            ],
          ),
          child: Center(
            child: isLoading
                ? const SizedBox(
                    width: 22,
                    height: 22,
                    child: CircularProgressIndicator(
                      strokeWidth: 2.2,
                      color: Colors.white,
                    ),
                  )
                : const Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Text(
                        'Sign In',
                        style: TextStyle(
                          color: Colors.white,
                          fontSize: 16,
                          fontWeight: FontWeight.w700,
                          letterSpacing: 0.3,
                        ),
                      ),
                      Gap(8),
                      Icon(Icons.arrow_forward_rounded,
                          color: Colors.white, size: 20),
                    ],
                  ),
          ),
        ),
      ),
    );
  }
}

// ── Shared input style ───────────────────────────────────────────────────────

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
  const idle = OutlineInputBorder(
    borderRadius: radius,
    borderSide: BorderSide(color: Color(0xFFE2E8F0)),
  );
  return InputDecoration(
    hintText: hint,
    hintStyle: const TextStyle(color: Color(0xFFCBD5E1), fontSize: 15),
    prefixIcon: Icon(prefixIcon, color: const Color(0xFF94A3B8), size: 20),
    suffixIcon: suffixIcon != null
        ? Padding(
            padding: const EdgeInsets.only(right: 14),
            child: suffixIcon,
          )
        : null,
    suffixIconConstraints:
        const BoxConstraints(minWidth: 46, minHeight: 46),
    filled: true,
    fillColor: const Color(0xFFF8FAFC),
    floatingLabelBehavior: FloatingLabelBehavior.never,
    border: idle,
    enabledBorder: idle,
    focusedBorder: const OutlineInputBorder(
      borderRadius: radius,
      borderSide: BorderSide(color: AppColors.primary, width: 1.6),
    ),
    errorBorder: const OutlineInputBorder(
      borderRadius: radius,
      borderSide: BorderSide(color: AppColors.error),
    ),
    focusedErrorBorder: const OutlineInputBorder(
      borderRadius: radius,
      borderSide: BorderSide(color: AppColors.error, width: 1.6),
    ),
    contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
  );
}
