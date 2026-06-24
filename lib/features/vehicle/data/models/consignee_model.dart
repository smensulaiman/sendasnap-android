class ConsigneeModel {
  final String? name;
  final String? address;
  final String? phone;
  final String? email;

  const ConsigneeModel({this.name, this.address, this.phone, this.email});

  factory ConsigneeModel.fromJson(Map<String, dynamic> json) => ConsigneeModel(
        name: json['name'] as String?,
        address: json['address'] as String?,
        phone: json['phone'] as String?,
        email: json['email'] as String?,
      );

  Map<String, dynamic> toJson() => {
        'name': name,
        'address': address,
        'phone': phone,
        'email': email,
      };
}
