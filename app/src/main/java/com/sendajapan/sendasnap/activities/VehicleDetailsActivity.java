package com.sendajapan.sendasnap.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.sendajapan.sendasnap.MyApplication;
import com.sendajapan.sendasnap.R;
import com.sendajapan.sendasnap.adapters.PendingImageAdapter;
import com.sendajapan.sendasnap.adapters.VehicleImageGridAdapter;
import com.sendajapan.sendasnap.databinding.ActivityVehicleDetailsBinding;
import com.sendajapan.sendasnap.dialogs.LoadingDialog;
import com.sendajapan.sendasnap.models.Vehicle;
import com.sendajapan.sendasnap.networking.ApiService;
import com.sendajapan.sendasnap.networking.RetrofitClient;
import com.sendajapan.sendasnap.services.VehicleImageUploadService;
import com.sendajapan.sendasnap.utils.CookieBarToastHelper;
import com.sendajapan.sendasnap.utils.HapticFeedbackHelper;
import com.sendajapan.sendasnap.utils.SharedPrefsManager;
import com.sendajapan.sendasnap.utils.VehicleCache;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class VehicleDetailsActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST = 100;
    private static final int STORAGE_PERMISSION_REQUEST = 101;

    private ActivityVehicleDetailsBinding binding;

    private ApiService apiService;
    private HapticFeedbackHelper hapticHelper;
    private LoadingDialog loadingDialog;
    private SharedPrefsManager prefsManager;
    private VehicleCache vehicleCache;
    private VehicleImageUploadService imageUploadService;

    private ActivityResultLauncher<Uri> cameraLauncher;
    private ActivityResultLauncher<String> galleryLauncher;

    private PendingImageAdapter pendingImageAdapter;
    private VehicleImageGridAdapter vehicleImageAdapter;

    private Vehicle vehicle;
    private Uri cameraImageUri;
    private final List<Uri> pendingImageUris = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityVehicleDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        MyApplication.applyWindowInsets(binding.getRoot());

        initHelpers();
        getVehicleData();
        setupToolbar();
        setupClickListeners();
        populateVehicleData();
        setupImageGrids();
        setupImagePickers();
    }

    private void initHelpers() {
        hapticHelper = HapticFeedbackHelper.getInstance(this);
        apiService = RetrofitClient.getInstance(this).getApiService();
        imageUploadService = new VehicleImageUploadService(this);
        prefsManager = SharedPrefsManager.getInstance(this);
        vehicleCache = VehicleCache.getInstance(this);

        loadingDialog = new LoadingDialog.Builder(VehicleDetailsActivity.this)
                .setMessage("Uploading Images")
                .setSubtitle("Please wait while uploading images...")
                .setCancelable(false)
                .setShowProgressIndicator(true)
                .build();
    }

    private void getVehicleData() {
        vehicle = (Vehicle) getIntent().getSerializableExtra("vehicle");
        if (vehicle == null) {
            CookieBarToastHelper.showError(this, "Error", "Vehicle data not found",
                    CookieBarToastHelper.LONG_DURATION);
            finish();
        }
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> {
            hapticHelper.vibrateClick();
            finish();
        });
    }

    private void setupClickListeners() {
        binding.btnAddPhotos.setOnClickListener(v -> {
            hapticHelper.vibrateClick();
            showImagePickerBottomSheet();
        });

        binding.btnUploadPhotos.setOnClickListener(v -> {
            hapticHelper.vibrateClick();
            uploadPendingImages();
        });
    }

    private void populateVehicleData() {
        if (vehicle == null)
            return;

        binding.txtMake.setText(vehicle.getMake() != null ? vehicle.getMake() : "N/A");
        binding.txtModel.setText(vehicle.getModel() != null ? vehicle.getModel() : "N/A");
        binding.txtYear.setText(vehicle.getYear() != null ? vehicle.getYear() : "N/A");
        binding.txtColor.setText(vehicle.getColor() != null ? vehicle.getColor() : "N/A");
        binding.txtChassis.setText(vehicle.getSerialNumber() != null ? vehicle.getSerialNumber() : "N/A");

        binding.txtBuyDate.setText(vehicle.getVehicleBuyDate() != null ? vehicle.getVehicleBuyDate() : "N/A");
        binding.txtPrice.setText(vehicle.getBuyingPrice() != null ? "￥" + vehicle.getBuyingPrice() : "N/A");
        binding.txtAuctionShip.setText(vehicle.getAuctionShipNumber() != null ? vehicle.getAuctionShipNumber() : "N/A");

        binding.txtExpectedYardDate
                .setText(vehicle.getExpectedYardDate() != null ? vehicle.getExpectedYardDate() : "N/A");
        binding.txtRiksoFrom.setText(vehicle.getRiksoFrom() != null ? vehicle.getRiksoFrom() : "N/A");
        binding.txtRiksoTo.setText(vehicle.getRiksoTo() != null ? vehicle.getRiksoTo() : "N/A");
        binding.txtRiksoCost.setText(vehicle.getRiksoCost() != null ? "￥" + vehicle.getRiksoCost() : "N/A");
    }

    private void setupImageGrids() {
        List<String> vehiclePhotos = vehicle.getVehiclePhotos();
        if (vehiclePhotos == null) {
            vehiclePhotos = new ArrayList<>();
        }

        if (vehicleImageAdapter == null) {
            vehicleImageAdapter = new VehicleImageGridAdapter(vehiclePhotos);
            GridLayoutManager vehicleGridLayoutManager = new GridLayoutManager(this, 3);
            binding.recyclerViewVehicleImages.setLayoutManager(vehicleGridLayoutManager);
            binding.recyclerViewVehicleImages.setAdapter(vehicleImageAdapter);
        } else {
            vehicleImageAdapter.updateImages(vehiclePhotos);
        }

        // Update click listener with current photos list
        final List<String> finalVehiclePhotos = vehiclePhotos;
        vehicleImageAdapter.setOnImageClickListener((imageUrl, position) -> {
            hapticHelper.vibrateClick();
            showImagePreview(finalVehiclePhotos, position);
        });

        pendingImageAdapter = new PendingImageAdapter(convertUrisToStrings(pendingImageUris));
        GridLayoutManager pendingGridLayoutManager = new GridLayoutManager(this, 3);
        binding.recyclerViewPendingImages.setLayoutManager(pendingGridLayoutManager);
        binding.recyclerViewPendingImages.setAdapter(pendingImageAdapter);

        pendingImageAdapter.setOnDeleteClickListener(position -> {
            hapticHelper.vibrateClick();
            pendingImageUris.remove(position);
            if (pendingImageAdapter != null) {
                pendingImageAdapter.updateImages(convertUrisToStrings(pendingImageUris));
            }
            updatePendingImagesVisibility();
        });
    }

    private void setupImagePickers() {
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                result -> {
                    if (result && cameraImageUri != null) {
                        pendingImageUris.add(cameraImageUri);
                        if (pendingImageAdapter == null) {
                            setupImageGrids();
                        } else {
                            pendingImageAdapter.updateImages(convertUrisToStrings(pendingImageUris));
                        }
                        updatePendingImagesVisibility();
                    } else if (!result) {
                        CookieBarToastHelper.showInfo(this, "Cancelled", "Photo capture cancelled",
                                CookieBarToastHelper.SHORT_DURATION);
                    }
                });

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetMultipleContents(),
                uris -> {
                    if (uris != null && !uris.isEmpty()) {
                        pendingImageUris.addAll(uris);
                        if (pendingImageAdapter == null) {
                            setupImageGrids();
                        } else {
                            pendingImageAdapter.updateImages(convertUrisToStrings(pendingImageUris));
                        }
                        updatePendingImagesVisibility();
                    }
                });
    }

    private void updatePendingImagesVisibility() {
        if (pendingImageUris.isEmpty()) {
            binding.recyclerViewPendingImages.setVisibility(View.GONE);
            binding.btnUploadPhotos.setVisibility(View.GONE);
        } else {
            binding.recyclerViewPendingImages.setVisibility(View.VISIBLE);
            binding.btnUploadPhotos.setVisibility(View.VISIBLE);
        }
    }

    private void showImagePickerBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_image_picker, null);
        bottomSheetDialog.setContentView(bottomSheetView);

        View layoutCamera = bottomSheetView.findViewById(R.id.layoutCamera);
        View layoutGallery = bottomSheetView.findViewById(R.id.layoutGallery);

        layoutCamera.setOnClickListener(v -> {
            hapticHelper.vibrateClick();
            bottomSheetDialog.dismiss();
            openCamera();
        });

        layoutGallery.setOnClickListener(v -> {
            hapticHelper.vibrateClick();
            bottomSheetDialog.dismiss();
            openGallery();
        });

        bottomSheetDialog.show();
    }

    private void openCamera() {
        String[] permissions;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions = new String[] { Manifest.permission.CAMERA };
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                    CookieBarToastHelper.showInfo(this, "Permission Required",
                            "Camera permission is needed to take photos", CookieBarToastHelper.LONG_DURATION);
                }
                ActivityCompat.requestPermissions(this, permissions, CAMERA_PERMISSION_REQUEST);
                return;
            }
        } else {
            permissions = new String[] { Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE };
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                    ||
                    ContextCompat.checkSelfPermission(this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA) ||
                        ActivityCompat.shouldShowRequestPermissionRationale(this,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    CookieBarToastHelper.showInfo(this, "Permission Required",
                            "Camera and storage permissions are needed to take photos",
                            CookieBarToastHelper.LONG_DURATION);
                }
                ActivityCompat.requestPermissions(this, permissions, CAMERA_PERMISSION_REQUEST);
                return;
            }
        }

        try {
            cameraImageUri = createImageFile();
            if (cameraImageUri != null) {
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (cameraIntent.resolveActivity(getPackageManager()) != null) {
                    cameraLauncher.launch(cameraImageUri);
                } else {
                    CookieBarToastHelper.showError(this, "Error", "No camera app available on this device",
                            CookieBarToastHelper.LONG_DURATION);
                }
            } else {
                CookieBarToastHelper.showError(this, "Error", "Failed to create image file",
                        CookieBarToastHelper.LONG_DURATION);
            }
        } catch (Exception e) {
            CookieBarToastHelper.showError(this, "Error", "Failed to open camera: " + e.getMessage(),
                    CookieBarToastHelper.LONG_DURATION);
        }
    }

    private void openGallery() {
        String[] permissions;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions = new String[] { Manifest.permission.READ_MEDIA_IMAGES };
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_MEDIA_IMAGES)) {
                    CookieBarToastHelper.showInfo(this, "Permission Required",
                            "Storage permission is needed to access photos", CookieBarToastHelper.LONG_DURATION);
                }
                ActivityCompat.requestPermissions(this, permissions, STORAGE_PERMISSION_REQUEST);
                return;
            }
        } else {
            permissions = new String[] { Manifest.permission.READ_EXTERNAL_STORAGE };
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    CookieBarToastHelper.showInfo(this, "Permission Required",
                            "Storage permission is needed to access photos", CookieBarToastHelper.LONG_DURATION);
                }
                ActivityCompat.requestPermissions(this, permissions, STORAGE_PERMISSION_REQUEST);
                return;
            }
        }

        galleryLauncher.launch("image/*");
    }

    private Uri createImageFile() {
        try {
            String timeStamp = String.valueOf(System.currentTimeMillis());
            String imageFileName = "JPEG_" + timeStamp + "_";
            File storageDir = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES);

            if (storageDir != null && !storageDir.exists()) {
                storageDir.mkdirs();
            }

            File image = new File(storageDir, imageFileName + ".jpg");

            if (!image.exists()) {
                image.createNewFile();
            }

            return FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", image);
        } catch (Exception e) {
            CookieBarToastHelper.showError(this, "Error", "Failed to create image file: " + e.getMessage(),
                    CookieBarToastHelper.LONG_DURATION);
            return null;
        }
    }

    private List<String> convertUrisToStrings(List<Uri> uris) {
        List<String> paths = new ArrayList<>();
        for (Uri uri : uris) {
            if (uri.getScheme() != null && uri.getScheme().equals("file")) {
                paths.add(uri.getPath());
            } else {
                paths.add(uri.toString());
            }
        }
        return paths;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }
            if (allPermissionsGranted) {
                openCamera();
            } else {
                CookieBarToastHelper.showError(this, "Permission Denied", "Camera permission is required",
                        CookieBarToastHelper.LONG_DURATION);
            }
        } else if (requestCode == STORAGE_PERMISSION_REQUEST) {
            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }
            if (allPermissionsGranted) {
                openGallery();
            } else {
                CookieBarToastHelper.showError(this, "Permission Denied", "Storage permission is required",
                        CookieBarToastHelper.LONG_DURATION);
            }
        }
    }

    private void uploadPendingImages() {
        if (pendingImageUris.isEmpty()) {
            CookieBarToastHelper.showInfo(this, "No Photos", "No photos to upload",
                    CookieBarToastHelper.SHORT_DURATION);
            return;
        }

        if (vehicle == null || vehicle.getId() == null) {
            CookieBarToastHelper.showError(this, "Error", "Vehicle information not available",
                    CookieBarToastHelper.LONG_DURATION);
            return;
        }

        int vehicleId;
        try {
            vehicleId = Integer.parseInt(vehicle.getId());
        } catch (NumberFormatException e) {
            CookieBarToastHelper.showError(this, "Error", "Invalid vehicle ID", CookieBarToastHelper.LONG_DURATION);
            return;
        }

        if (!loadingDialog.isShowing()) {
            loadingDialog.show();
        }

        imageUploadService.uploadImages(vehicleId, new ArrayList<>(pendingImageUris),
                new VehicleImageUploadService.UploadCallback() {
                    @SuppressLint("NotifyDataSetChanged")
                    @Override
                    public void onSuccess(Vehicle updatedVehicle) {
                        runOnUiThread(() -> {
                            if (loadingDialog.isShowing()) {
                                loadingDialog.dismiss();
                            }

                            if (updatedVehicle != null) {
                                if (updatedVehicle.getId() == null || updatedVehicle.getId().isEmpty()) {
                                    if (vehicle != null && vehicle.getId() != null) {
                                        updatedVehicle.setId(vehicle.getId());
                                    }
                                }

                                prefsManager.addVehicleToCache(updatedVehicle);
                                vehicleCache.addVehicle(updatedVehicle);
                                vehicle = updatedVehicle;
                            }

                            pendingImageUris.clear();
                            pendingImageAdapter.notifyDataSetChanged();

                            populateVehicleData();

                            // Update vehicle photos in adapter
                            List<String> updatedPhotos = updatedVehicle.getVehiclePhotos();
                            if (updatedPhotos == null) {
                                updatedPhotos = new ArrayList<>();
                            }

                            if (vehicleImageAdapter != null) {
                                vehicleImageAdapter.updateImages(updatedPhotos);
                            } else {
                                setupImageGrids();
                            }

                            updatePendingImagesVisibility();

                            CookieBarToastHelper.showSuccess(VehicleDetailsActivity.this, "Success",
                                    "Images uploaded successfully",
                                    CookieBarToastHelper.SHORT_DURATION);
                        });
                    }

                    @Override
                    public void onError(String errorMessage, int errorCode) {
                        runOnUiThread(() -> {
                            if (loadingDialog.isShowing()) {
                                loadingDialog.dismiss();
                            }

                            CookieBarToastHelper.showError(VehicleDetailsActivity.this, "Upload Failed",
                                    errorMessage,
                                    CookieBarToastHelper.LONG_DURATION);
                        });
                    }
                });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void showImagePreview(List<String> imageUrls, int startPosition) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return;
        }

        Dialog previewDialog = new Dialog(this,
                android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        View previewView = getLayoutInflater().inflate(R.layout.dialog_image_preview, null);
        previewDialog.setContentView(previewView);

        ImageView imgPreviewFull = previewView.findViewById(R.id.imgPreviewFull);
        FloatingActionButton fabClosePreview = previewView
                .findViewById(R.id.fabClosePreview);
        TextView txtImageCounter = previewView.findViewById(R.id.txtImageCounter);

        // Use array to store current position so it can be modified in inner class
        final int[] currentPosition = { startPosition };

        // Show counter if multiple images
        if (imageUrls.size() > 1) {
            txtImageCounter.setVisibility(View.VISIBLE);
            updateImageCounter(txtImageCounter, currentPosition[0] + 1, imageUrls.size());
        } else {
            txtImageCounter.setVisibility(View.GONE);
        }

        // Load current image
        String currentImageUrl = imageUrls.get(currentPosition[0]);
        if (currentImageUrl != null && !currentImageUrl.isEmpty()) {
            Glide.with(this)
                    .load(currentImageUrl)
                    .placeholder(R.drawable.car_placeholder)
                    .error(R.drawable.car_placeholder)
                    .into(imgPreviewFull);
        } else {
            imgPreviewFull.setImageResource(R.drawable.car_placeholder);
        }

        // Close button
        fabClosePreview.setOnClickListener(v -> {
            hapticHelper.vibrateClick();
            previewDialog.dismiss();
        });

        // Swipe to change images (if multiple images)
        if (imageUrls.size() > 1) {
            imgPreviewFull.setOnTouchListener(new SwipeGestureListener() {
                @Override
                public void onSwipeLeft() {
                    // Next image
                    currentPosition[0] = (currentPosition[0] + 1) % imageUrls.size();
                    String nextImageUrl = imageUrls.get(currentPosition[0]);
                    if (nextImageUrl != null && !nextImageUrl.isEmpty()) {
                        Glide.with(VehicleDetailsActivity.this)
                                .load(nextImageUrl)
                                .placeholder(R.drawable.car_placeholder)
                                .error(R.drawable.car_placeholder)
                                .into(imgPreviewFull);
                    }
                    updateImageCounter(txtImageCounter, currentPosition[0] + 1, imageUrls.size());
                }

                @Override
                public void onSwipeRight() {
                    // Previous image
                    currentPosition[0] = (currentPosition[0] - 1 + imageUrls.size()) % imageUrls.size();
                    String prevImageUrl = imageUrls.get(currentPosition[0]);
                    if (prevImageUrl != null && !prevImageUrl.isEmpty()) {
                        Glide.with(VehicleDetailsActivity.this)
                                .load(prevImageUrl)
                                .placeholder(R.drawable.car_placeholder)
                                .error(R.drawable.car_placeholder)
                                .into(imgPreviewFull);
                    }
                    updateImageCounter(txtImageCounter, currentPosition[0] + 1, imageUrls.size());
                }
            });
        }

        previewDialog.show();
    }

    private void updateImageCounter(TextView counterView, int current, int total) {
        counterView.setText(current + " / " + total);
    }

    // Simple swipe gesture listener
    private abstract class SwipeGestureListener implements View.OnTouchListener {
        private float startX;
        private float startY;
        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;

        @Override
        public boolean onTouch(View v, android.view.MotionEvent event) {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    startX = event.getX();
                    startY = event.getY();
                    return true;
                case android.view.MotionEvent.ACTION_UP:
                    float endX = event.getX();
                    float endY = event.getY();
                    float diffX = endX - startX;
                    float diffY = endY - startY;

                    if (Math.abs(diffX) > Math.abs(diffY)) {
                        if (Math.abs(diffX) > SWIPE_THRESHOLD) {
                            if (diffX > 0) {
                                onSwipeRight();
                            } else {
                                onSwipeLeft();
                            }
                            return true;
                        }
                    }
                    return false;
            }
            return false;
        }

        public abstract void onSwipeLeft();

        public abstract void onSwipeRight();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
