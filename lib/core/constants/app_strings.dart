class AppStrings {
  AppStrings._();

  static const String appName = 'SendaSnap';
  static const String tagline = "Japan's Logistics Companion";
  static const String copyright = '© 2025 SendaSnap. All rights reserved.';

  // Auth
  static const String login = 'Login';
  static const String logout = 'Logout';
  static const String emailLabel = 'Email Address';
  static const String emailHint = 'user@example.com';
  static const String passwordLabel = 'Password';
  static const String rememberMe = 'Remember Me';
  static const String loggingIn = 'Logging in...';
  static const String changePassword = 'Change Password';
  static const String currentPassword = 'Current Password';
  static const String newPassword = 'New Password';
  static const String confirmPassword = 'Confirm New Password';
  static const String updatePassword = 'Update Password';
  static const String logoutConfirmTitle = 'Logout';
  static const String logoutConfirmMessage = 'Are you sure you want to logout?';
  static const String confirm = 'Confirm';
  static const String cancel = 'Cancel';

  // Navigation
  static const String home = 'Home';
  static const String vehicles = 'Vehicles';
  static const String profile = 'Profile';
  static const String more = 'More';

  // Home
  static const String quickSearch = 'Quick Search';
  static const String recentlyViewed = 'Recently Viewed';
  static const String seeAll = 'See All';
  static const String searchVehicle = 'Search Vehicle';
  static const String browseYard = 'Browse Yard';
  static const String noRecentVehicles = 'No recent vehicles';
  static const String noRecentVehiclesSubtitle = 'Search for a vehicle to get started';

  // Search
  static const String search = 'Search';
  static const String yard = 'Yard';
  static const String company = 'Company';
  static const String searchType = 'Search Type';
  static const String searchQueryHint = 'Min. 3 characters';
  static const String vehicleIdType = 'Vehicle ID';
  static const String chassisNumberType = 'Chassis Number';
  static const String showVehicles = 'Show Vehicles';
  static const String autocraftJapan = 'Autocraft Japan Ltd';
  static const String karmen = 'Karmen';
  static const String noVehiclesFound = 'No vehicles found matching your search';
  static const String selectVehicle = 'Select Vehicle';

  // Yards
  static const String menzaiSugaya = 'MENZAI SUGAYA';
  static const String hanyaYard = 'HANYA YARD';
  static const String acjKobe = 'ACJ KOBE';
  static const String sugayaYard = 'Sugaya Yard';
  static const String hanyaYardKarmen = 'Hanya yard';
  static const String autocraftJapanLtd = 'AUTOCRAFT JAPAN LTD';
  static const String karmenLabel = 'KARMEN';

  // Vehicle Detail
  static const String vehicleDetails = 'Vehicle Details';
  static const String basicInfo = 'Basic Information';
  static const String dimensionsWeight = 'Dimensions & Weight';
  static const String purchaseDetails = 'Purchase Details';
  static const String yardInfo = 'Yard Information';
  static const String consigneeDetails = 'Consignee Details';
  static const String photos = 'Photos';
  static const String pendingUpload = 'Pending Upload';
  static const String noPhotosYet = 'No photos yet';
  static const String addPhotos = 'Add Photos';
  static const String uploadPhotos = 'Upload Photos';
  static const String addVehiclePhotos = 'Add Vehicle Photos';
  static const String takePhoto = 'Take Photo (Camera)';
  static const String chooseGallery = 'Choose from Gallery';
  static const String uploadingImages = 'Uploading images, please wait...';
  static const String imagesUploaded = 'Images uploaded successfully';

  // Vehicle fields
  static const String make = 'Make';
  static const String model = 'Model';
  static const String chassisModel = 'Chassis Model';
  static const String year = 'Year';
  static const String color = 'Color';
  static const String engineCc = 'Engine (CC)';
  static const String plateNumber = 'Plate Number';
  static const String length = 'Length';
  static const String width = 'Width';
  static const String height = 'Height';
  static const String netWeight = 'Net Weight';
  static const String area = 'Area';
  static const String buyDate = 'Buy Date';
  static const String buyingPrice = 'Buying Price';
  static const String auctionShipNumber = 'Auction Ship Number';
  static const String expectedYardDate = 'Expected Yard Date';
  static const String riksoFrom = 'Rikso From';
  static const String riksoTo = 'Rikso To';
  static const String riksoCost = 'Rikso Cost';
  static const String riksoCompany = 'Rikso Company';
  static const String chassis = 'Chassis';

  // Profile
  static const String myProfile = 'Profile';
  static const String accountInfo = 'Account Information';
  static const String settings = 'Settings';
  static const String notifications = 'Notifications';
  static const String hapticFeedback = 'Haptic Feedback';
  static const String cacheSection = 'Cache';
  static const String recentlyViewedVehicles = 'Recently Viewed Vehicles';
  static const String clearCache = 'Clear Cache';
  static const String vehiclesCached = 'vehicles cached';
  static const String fullName = 'Full Name';
  static const String role = 'Role';
  static const String phone = 'Phone';
  static const String avisId = 'Avis ID';
  static const String companyName = 'Company';
  static const String address = 'Address';

  // Coming Soon
  static const String comingSoon = 'Coming Soon';
  static const String moreFeatures = 'Tasks, Schedules, Notifications & Chat';

  // Errors
  static const String networkError = 'Network error. Please try again.';
  static const String noInternet = 'No internet connection';
  static const String sessionExpired = 'Session expired. Please login again.';
  static const String somethingWentWrong = 'Something went wrong. Please try again.';
  static const String fieldRequired = 'This field is required';
  static const String invalidEmail = 'Please enter a valid email address';
  static const String passwordTooShort = 'Password must be at least 8 characters';
  static const String passwordsDoNotMatch = 'Passwords do not match';
  static const String minSearchLength = 'Please enter at least 3 characters';

  static String comingSoonSubtitle(String feature) =>
      "We're working hard to bring you $feature. Stay tuned!";

  static String vehiclesCachedLabel(int count) => '$count $vehiclesCached';
  static String photosLabel(int count) => '$photos ($count)';
  static String pendingUploadLabel(int count) => '$pendingUpload ($count)';
  static String vehiclesShownOf(int shown, int total) =>
      '$shown of $total vehicles';
}
