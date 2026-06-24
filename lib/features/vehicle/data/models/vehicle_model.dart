import 'consignee_model.dart';

class VehicleModel {
  final String id;
  final String? serialNumber;
  final String? make;
  final String? model;
  final String? chassisModel;
  final String? cc;
  final String? year;
  final String? color;
  final String? vehicleBuyDate;
  final String? auctionShipNumber;
  final String? netWeight;
  final String? area;
  final String? length;
  final String? width;
  final String? height;
  final String? plateNumber;
  final String? buyingPrice;
  final String? expectedYardDate;
  final String? riksoFrom;
  final String? riksoTo;
  final String? riksoCost;
  final String? riksoCompany;
  final List<String>? images;
  final String? auctionSheet;
  final String? tohonCopy;
  final List<ConsigneeModel>? consigneeDetails;
  final String? company;

  const VehicleModel({
    required this.id,
    this.serialNumber,
    this.make,
    this.model,
    this.chassisModel,
    this.cc,
    this.year,
    this.color,
    this.vehicleBuyDate,
    this.auctionShipNumber,
    this.netWeight,
    this.area,
    this.length,
    this.width,
    this.height,
    this.plateNumber,
    this.buyingPrice,
    this.expectedYardDate,
    this.riksoFrom,
    this.riksoTo,
    this.riksoCost,
    this.riksoCompany,
    this.images,
    this.auctionSheet,
    this.tohonCopy,
    this.consigneeDetails,
    this.company,
  });

  // Parses both the Search endpoint (camelCase) and Yard endpoint (snake_case).
  factory VehicleModel.fromJson(Map<String, dynamic> json) {
    String? str(dynamic v) => v?.toString();

    final rawImages = json['images'];
    List<String>? images;
    if (rawImages is List) {
      images = rawImages.map((e) => e.toString()).toList();
    }

    final rawConsignees = json['consigneeDetails'] ?? json['consignee_details'];
    List<ConsigneeModel>? consignees;
    if (rawConsignees is List) {
      consignees = rawConsignees
          .map((e) => ConsigneeModel.fromJson(e as Map<String, dynamic>))
          .toList();
    }

    return VehicleModel(
      id: str(json['id'] ?? json['vehicle_id']) ?? '',
      serialNumber: str(json['serialNumber'] ?? json['chassis_number']),
      make: str(json['make']),
      model: str(json['model']),
      chassisModel: str(json['chassisModel'] ?? json['chassis_model']),
      cc: str(json['cc'] ?? json['veh_cc']),
      year: str(json['year'] ?? json['veh_year']),
      color: str(json['color'] ?? json['veh_color']),
      vehicleBuyDate: str(json['vehicleBuyDate'] ?? json['veh_buy_date']),
      auctionShipNumber: str(
        json['auctionShipNumber'] ?? json['veh_auc_ship_number'],
      ),
      netWeight: str(json['netWeight'] ?? json['veh_net_weight']),
      area: str(json['area']),
      length: str(json['length'] ?? json['veh_l']),
      width: str(json['width'] ?? json['veh_w']),
      height: str(json['height'] ?? json['veh_h']),
      plateNumber: str(json['plateNumber'] ?? json['plate_number']),
      buyingPrice: str(json['buyingPrice'] ?? json['veh_buy_price']),
      expectedYardDate: str(json['expectedYardDate'] ?? json['yard_date_in']),
      riksoFrom: str(json['riksoFrom'] ?? json['rikso_from_place_id']),
      riksoTo: str(json['riksoTo'] ?? json['rikso_to_place_id']),
      riksoCost: str(json['riksoCost'] ?? json['rikso_cost']),
      riksoCompany: str(json['riksoCompany'] ?? json['rikso_company']),
      images: images,
      auctionSheet: str(json['auctionSheet'] ?? json['auction_sheet']),
      tohonCopy: str(json['tohonCopy'] ?? json['tohon_copy']),
      consigneeDetails: consignees,
      company: str(json['company']),
    );
  }

  Map<String, dynamic> toJson() => {
    'id': id,
    'serialNumber': serialNumber,
    'make': make,
    'model': model,
    'chassisModel': chassisModel,
    'cc': cc,
    'year': year,
    'color': color,
    'vehicleBuyDate': vehicleBuyDate,
    'auctionShipNumber': auctionShipNumber,
    'netWeight': netWeight,
    'area': area,
    'length': length,
    'width': width,
    'height': height,
    'plateNumber': plateNumber,
    'buyingPrice': buyingPrice,
    'expectedYardDate': expectedYardDate,
    'riksoFrom': riksoFrom,
    'riksoTo': riksoTo,
    'riksoCost': riksoCost,
    'riksoCompany': riksoCompany,
    'images': images,
    'auctionSheet': auctionSheet,
    'tohonCopy': tohonCopy,
    'consigneeDetails': consigneeDetails?.map((c) => c.toJson()).toList(),
    'company': company,
  };

  VehicleModel copyWith({
    String? id,
    String? serialNumber,
    String? make,
    String? model,
    String? chassisModel,
    String? cc,
    String? year,
    String? color,
    String? vehicleBuyDate,
    String? auctionShipNumber,
    String? netWeight,
    String? area,
    String? length,
    String? width,
    String? height,
    String? plateNumber,
    String? buyingPrice,
    String? expectedYardDate,
    String? riksoFrom,
    String? riksoTo,
    String? riksoCost,
    String? riksoCompany,
    List<String>? images,
    String? auctionSheet,
    String? tohonCopy,
    List<ConsigneeModel>? consigneeDetails,
    String? company,
  }) => VehicleModel(
    id: id ?? this.id,
    serialNumber: serialNumber ?? this.serialNumber,
    make: make ?? this.make,
    model: model ?? this.model,
    chassisModel: chassisModel ?? this.chassisModel,
    cc: cc ?? this.cc,
    year: year ?? this.year,
    color: color ?? this.color,
    vehicleBuyDate: vehicleBuyDate ?? this.vehicleBuyDate,
    auctionShipNumber: auctionShipNumber ?? this.auctionShipNumber,
    netWeight: netWeight ?? this.netWeight,
    area: area ?? this.area,
    length: length ?? this.length,
    width: width ?? this.width,
    height: height ?? this.height,
    plateNumber: plateNumber ?? this.plateNumber,
    buyingPrice: buyingPrice ?? this.buyingPrice,
    expectedYardDate: expectedYardDate ?? this.expectedYardDate,
    riksoFrom: riksoFrom ?? this.riksoFrom,
    riksoTo: riksoTo ?? this.riksoTo,
    riksoCost: riksoCost ?? this.riksoCost,
    riksoCompany: riksoCompany ?? this.riksoCompany,
    images: images ?? this.images,
    auctionSheet: auctionSheet ?? this.auctionSheet,
    tohonCopy: tohonCopy ?? this.tohonCopy,
    consigneeDetails: consigneeDetails ?? this.consigneeDetails,
    company: company ?? this.company,
  );

  @override
  bool operator ==(Object other) =>
      identical(this, other) || other is VehicleModel && id == other.id;

  @override
  int get hashCode => id.hashCode;
}
