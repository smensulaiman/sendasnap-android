package com.sendajapan.sendasnap.services;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.OpenableColumns;

import com.google.gson.Gson;
import com.sendajapan.sendasnap.models.ErrorResponse;
import com.sendajapan.sendasnap.models.Vehicle;
import com.sendajapan.sendasnap.models.VehicleImageUploadResponse;
import com.sendajapan.sendasnap.networking.ApiService;
import com.sendajapan.sendasnap.networking.RetrofitClient;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VehicleImageUploadService {

    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024;

    private final ApiService apiService;
    private final Context context;

    public interface UploadCallback {
        void onSuccess(Vehicle vehicle);

        void onError(String errorMessage, int errorCode);
    }

    public VehicleImageUploadService(Context context) {
        this.context = context.getApplicationContext();
        this.apiService = RetrofitClient.getInstance(context).getApiService();
    }

    public void uploadImages(int vehicleId, String company, List<Uri> imageUris, UploadCallback callback) {
        if (imageUris == null || imageUris.isEmpty()) {
            callback.onError("No images to upload", 422);
            return;
        }

        List<MultipartBody.Part> imageParts = new ArrayList<>();
        List<File> imageFiles = new ArrayList<>();
        List<File> tempFiles = new ArrayList<>();

        for (Uri imageUri : imageUris) {
            File imageFile = getFileFromUri(imageUri);
            if (imageFile == null || !imageFile.exists() || !imageFile.isFile()) {
                cleanupTempFiles(tempFiles);
                callback.onError("Image file not found or cannot be accessed", 422);
                return;
            }

            File fileToUpload = imageFile;
            if (imageFile.length() > MAX_FILE_SIZE) {
                try {
                    fileToUpload = compressImage(imageFile);
                    if (fileToUpload != null && fileToUpload.exists()) {
                        tempFiles.add(fileToUpload);
                    } else {
                        fileToUpload = imageFile;
                    }
                } catch (Exception e) {
                    fileToUpload = imageFile;
                }
            }

            if (fileToUpload.length() > MAX_FILE_SIZE) {
                cleanupTempFiles(tempFiles);
                callback.onError("Image file too large (max 2MB) even after compression", 422);
                return;
            }

            imageFiles.add(fileToUpload);
        }

        // Create multipart parts for images
        for (File imageFile : imageFiles) {
            RequestBody requestFile = RequestBody.create(
                    MediaType.parse("image/*"), imageFile);
            MultipartBody.Part imagePart = MultipartBody.Part.createFormData(
                    "images[]", imageFile.getName(), requestFile);
            imageParts.add(imagePart);
        }

        RequestBody vehicleIdBody = RequestBody.create(
                MediaType.parse("text/plain"), String.valueOf(vehicleId));

        RequestBody companyBody = RequestBody.create(
                MediaType.parse("text/plain"), company != null ? company : "");

        Call<VehicleImageUploadResponse> call = apiService.uploadVehicleImagesNew(
                vehicleIdBody, companyBody, imageParts);

        call.enqueue(new Callback<VehicleImageUploadResponse>() {
            @Override
            public void onResponse(Call<VehicleImageUploadResponse> call,
                    Response<VehicleImageUploadResponse> response) {
                // Clean up temporary compressed files
                cleanupTempFiles(tempFiles);

                if (response.isSuccessful() && response.body() != null) {
                    VehicleImageUploadResponse uploadResponse = response.body();

                    if (uploadResponse.getSuccess() != null && uploadResponse.getSuccess()) {
                        // Success
                        Vehicle vehicle = uploadResponse.getData() != null
                                ? uploadResponse.getData().getVehicle()
                                : null;
                        callback.onSuccess(vehicle);
                    } else {
                        // API returned success: false
                        String errorMessage = uploadResponse.getMessage() != null
                                ? uploadResponse.getMessage()
                                : "Upload failed";
                        callback.onError(errorMessage, response.code());
                    }
                } else {
                    // Handle error response
                    String errorMessage = parseErrorMessage(response);
                    callback.onError(errorMessage, response.code());
                }
            }

            @Override
            public void onFailure(Call<VehicleImageUploadResponse> call, Throwable t) {
                // Clean up temporary compressed files
                cleanupTempFiles(tempFiles);

                String errorMessage = "Network error. Please check your connection and try again.";
                if (t.getMessage() != null) {
                    errorMessage = t.getMessage();
                }
                callback.onError(errorMessage, 0);
            }
        });
    }

    /**
     * Parse error message from error response
     */
    private String parseErrorMessage(Response<VehicleImageUploadResponse> response) {
        String defaultMessage = "An error occurred. Please try again.";

        if (response.errorBody() != null) {
            try {
                String errorBodyStr = response.errorBody().string();

                Gson gson = new Gson();
                ErrorResponse errorResponse = gson.fromJson(errorBodyStr, ErrorResponse.class);

                if (errorResponse != null) {
                    // Use formatted error message if available
                    if (errorResponse.getErrors() != null && !errorResponse.getErrors().isEmpty()) {
                        return errorResponse.getFormattedErrorMessage();
                    } else if (errorResponse.getMessage() != null) {
                        return errorResponse.getMessage();
                    }
                }
            } catch (Exception e) {
            }
        }

        // Fallback to HTTP status code message
        if (response.code() == 404) {
            return "Vehicle not found in external database";
        } else if (response.code() == 422) {
            return "Validation failed. Please check your input.";
        } else if (response.code() == 502) {
            return "External database query failed";
        }

        return defaultMessage;
    }

    /**
     * Get File from Uri, handling both content:// and file:// URIs
     * Uses MediaStore API and ContentResolver for content URIs
     */
    private File getFileFromUri(Uri uri) {
        if (uri == null) {
            return null;
        }

        if ("file".equals(uri.getScheme())) {
            String path = uri.getPath();
            if (path != null) {
                File file = new File(path);
                // Only access files in app-specific directories
                File cacheDir = context.getCacheDir();
                File filesDir = context.getFilesDir();
                File externalFilesDir = context.getExternalFilesDir(null);

                boolean isInAppStorage = (cacheDir != null && path.startsWith(cacheDir.getAbsolutePath())) ||
                        (filesDir != null && path.startsWith(filesDir.getAbsolutePath())) ||
                        (externalFilesDir != null && path.startsWith(externalFilesDir.getAbsolutePath()));

                if (isInAppStorage && file.exists() && file.isFile()) {
                    return file;
                }
            }
            return null;
        } else if ("content".equals(uri.getScheme())) {
            try {
                // Use MediaStore API to get file name
                String fileName = getFileNameFromUri(uri);
                if (fileName == null || fileName.isEmpty()) {
                    fileName = "temp_image_" + System.currentTimeMillis() + ".jpg";
                }
                
                File tempFile = new File(context.getCacheDir(), fileName);
                try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
                     FileOutputStream outputStream = new FileOutputStream(tempFile)) {
                    if (inputStream == null) {
                        return null;
                    }
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                }
                return tempFile;
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Get file name from URI using MediaStore API
     */
    private String getFileNameFromUri(Uri uri) {
        String fileName = null;
        try {
            // Method 1: Try OpenableColumns.DISPLAY_NAME (works for all content URIs)
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex != -1) {
                    fileName = cursor.getString(nameIndex);
                }
                cursor.close();
            }
        } catch (Exception e) {
            // Fall through to alternative method
        }

        try {
            // Method 2: Try MediaStore.MediaColumns.DISPLAY_NAME (works for media files)
            if (fileName == null || fileName.isEmpty()) {
                Cursor cursor = context.getContentResolver().query(
                        uri,
                        new String[] { MediaStore.MediaColumns.DISPLAY_NAME },
                        null,
                        null,
                        null);
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex);
                    }
                    cursor.close();
                }
            }
        } catch (Exception e) {
            // Ignore
        }

        return fileName;
    }

    /**
     * Compress image file if it's larger than 2MB
     * 
     * @param imageFile Original image file
     * @return Compressed image file, or null if compression fails
     */
    private File compressImage(File imageFile) {
        try {
            // Decode bitmap with reduced sample size to save memory
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);

            // Calculate sample size to reduce memory usage
            int sampleSize = calculateInSampleSize(options, 1920, 1920); // Max dimensions
            options.inJustDecodeBounds = false;
            options.inSampleSize = sampleSize;

            Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);
            if (bitmap == null) {
                return null;
            }

            // Compress bitmap
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int quality = 85;
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);

            // Reduce quality if still too large
            while (baos.toByteArray().length > MAX_FILE_SIZE && quality > 30) {
                baos.reset();
                quality -= 10;
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
            }

            // If still too large, resize the bitmap
            if (baos.toByteArray().length > MAX_FILE_SIZE) {
                int maxDimension = Math.max(bitmap.getWidth(), bitmap.getHeight());
                int targetSize = 1200; // Target max dimension

                if (maxDimension > targetSize) {
                    float scale = (float) targetSize / maxDimension;
                    int newWidth = (int) (bitmap.getWidth() * scale);
                    int newHeight = (int) (bitmap.getHeight() * scale);

                    Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
                    bitmap.recycle();
                    bitmap = resizedBitmap;

                    // Try compressing again with resized bitmap
                    baos.reset();
                    quality = 80;
                    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);

                    // Reduce quality further if needed
                    while (baos.toByteArray().length > MAX_FILE_SIZE && quality > 30) {
                        baos.reset();
                        quality -= 10;
                        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
                    }
                }
            }

            // Create temporary file for compressed image
            File compressedFile = new File(context.getCacheDir(),
                    "compressed_" + System.currentTimeMillis() + "_" + imageFile.getName());

            // Write compressed bitmap to file
            FileOutputStream fos = new FileOutputStream(compressedFile);
            fos.write(baos.toByteArray());
            fos.flush();
            fos.close();

            // Clean up
            bitmap.recycle();
            baos.close();

            return compressedFile;

        } catch (IOException e) {
            return null;
        } catch (OutOfMemoryError e) {
            return null;
        }
    }

    /**
     * Calculate sample size for bitmap decoding to reduce memory usage
     */
    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    /**
     * Clean up temporary compressed files
     */
    private void cleanupTempFiles(List<File> tempFiles) {
        for (File tempFile : tempFiles) {
            try {
                if (tempFile != null && tempFile.exists()) {
                    tempFile.delete();
                }
            } catch (Exception e) {
            }
        }
        tempFiles.clear();
    }
}
