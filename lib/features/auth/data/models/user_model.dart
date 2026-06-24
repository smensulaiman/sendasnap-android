class UserModel {
  final int id;
  final String name;
  final String email;
  final String? emailVerifiedAt;
  final String role;
  final String? phone;
  final String? avisId;
  final String? avatar;
  final String? avatarUrl;
  final String? createdAt;
  final String? updatedAt;

  const UserModel({
    required this.id,
    required this.name,
    required this.email,
    this.emailVerifiedAt,
    required this.role,
    this.phone,
    this.avisId,
    this.avatar,
    this.avatarUrl,
    this.createdAt,
    this.updatedAt,
  });

  factory UserModel.fromJson(Map<String, dynamic> json) => UserModel(
        id: json['id'] as int,
        name: json['name'] as String? ?? '',
        email: json['email'] as String? ?? '',
        emailVerifiedAt: json['email_verified_at'] as String?,
        role: json['role'] as String? ?? '',
        phone: json['phone'] as String?,
        avisId: json['avis_id'] as String?,
        avatar: json['avatar'] as String?,
        avatarUrl: json['avatar_url'] as String?,
        createdAt: json['created_at'] as String?,
        updatedAt: json['updated_at'] as String?,
      );

  Map<String, dynamic> toJson() => {
        'id': id,
        'name': name,
        'email': email,
        'email_verified_at': emailVerifiedAt,
        'role': role,
        'phone': phone,
        'avis_id': avisId,
        'avatar': avatar,
        'avatar_url': avatarUrl,
        'created_at': createdAt,
        'updated_at': updatedAt,
      };

  UserModel copyWith({
    int? id,
    String? name,
    String? email,
    String? emailVerifiedAt,
    String? role,
    String? phone,
    String? avisId,
    String? avatar,
    String? avatarUrl,
    String? createdAt,
    String? updatedAt,
  }) =>
      UserModel(
        id: id ?? this.id,
        name: name ?? this.name,
        email: email ?? this.email,
        emailVerifiedAt: emailVerifiedAt ?? this.emailVerifiedAt,
        role: role ?? this.role,
        phone: phone ?? this.phone,
        avisId: avisId ?? this.avisId,
        avatar: avatar ?? this.avatar,
        avatarUrl: avatarUrl ?? this.avatarUrl,
        createdAt: createdAt ?? this.createdAt,
        updatedAt: updatedAt ?? this.updatedAt,
      );

  @override
  bool operator ==(Object other) =>
      identical(this, other) || other is UserModel && id == other.id;

  @override
  int get hashCode => id.hashCode;
}
