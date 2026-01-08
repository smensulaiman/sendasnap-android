package com.sendajapan.sendasnap.services;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.OpenableColumns;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class FirebaseStorageService {
    private static final String TAG = "FirebaseStorageService";
    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    
    private static FirebaseStorageService instance;
    private final FirebaseStorage storage;
    
    private FirebaseStorageService() {
        storage = FirebaseStorage.getInstance();
    }
    
    public static synchronized FirebaseStorageService getInstance() {
        if (instance == null) {
            instance = new FirebaseStorageService();
        }
        return instance;
    }
    
    /**
     * Upload image to Firebase Storage
     * @param context Context for accessing ContentResolver
     * @param imageUri Local URI of the image
     * @param chatId Chat ID for organizing images
     * @param callback Callback for upload result
     */
    public void uploadImage(Context context, Uri imageUri, String chatId, StorageCallback callback) {
        try {
            Bitmap bitmap;
            try (InputStream inputStream = context.getContentResolver().openInputStream(imageUri)) {
                if (inputStream == null) {
                    callback.onFailure(new Exception("Failed to open image stream"));
                    return;
                }
                bitmap = BitmapFactory.decodeStream(inputStream);
            }
            
            if (bitmap == null) {
                callback.onFailure(new Exception("Failed to decode image"));
                return;
            }
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int quality = 85;
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
            
            while (baos.toByteArray().length > MAX_IMAGE_SIZE && quality > 50) {
                baos.reset();
                quality -= 10;
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
            }
            
            byte[] imageData = baos.toByteArray();
            String fileName = "img_" + System.currentTimeMillis() + ".jpg";
            StorageReference imageRef = storage.getReference()
                    .child("chat_images")
                    .child(chatId)
                    .child(fileName);
            
            UploadTask uploadTask = imageRef.putBytes(imageData);
            
            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    imageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri downloadUri) {
                            callback.onSuccess(downloadUri.toString());
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(Exception e) {
                            callback.onFailure(e);
                        }
                    });
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(Exception e) {
                    callback.onFailure(e);
                }
            });
            
        } catch (Exception e) {
            callback.onFailure(e);
        }
    }
    
    /**
     * Upload file to Firebase Storage
     * @param context Context for accessing ContentResolver
     * @param fileUri Local URI of the file
     * @param chatId Chat ID for organizing files
     * @param fileName Original file name
     * @param callback Callback for upload result
     */
    public void uploadFile(Context context, Uri fileUri, String chatId, String fileName, StorageCallback callback) {
        try {
            long fileSize = getFileSize(context, fileUri);
            if (fileSize > MAX_FILE_SIZE) {
                callback.onFailure(new Exception("File size exceeds maximum limit (10MB)"));
                return;
            }
            
            String timestamp = String.valueOf(System.currentTimeMillis());
            String extension = "";
            int lastDot = fileName.lastIndexOf('.');
            if (lastDot > 0) {
                extension = fileName.substring(lastDot);
            }
            String storageFileName = "file_" + timestamp + extension;
            
            StorageReference fileRef = storage.getReference()
                    .child("chat_files")
                    .child(chatId)
                    .child(storageFileName);
            
            UploadTask uploadTask = fileRef.putFile(fileUri);
            
            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    fileRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri downloadUri) {
                            callback.onSuccess(downloadUri.toString());
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(Exception e) {
                            callback.onFailure(e);
                        }
                    });
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(Exception e) {
                    callback.onFailure(e);
                }
            });
            
        } catch (Exception e) {
            callback.onFailure(e);
        }
    }
    
    private long getFileSize(Context context, Uri uri) {
        try (android.database.Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (sizeIndex >= 0) {
                    return cursor.getLong(sizeIndex);
                }
            }
        } catch (Exception e) {
        }
        return 0;
    }
    
    /**
     * Delete file from Firebase Storage
     */
    public void deleteFile(String fileUrl, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        try {
            StorageReference fileRef = storage.getReferenceFromUrl(fileUrl);
            fileRef.delete()
                    .addOnSuccessListener(onSuccess)
                    .addOnFailureListener(onFailure);
        } catch (Exception e) {
            if (onFailure != null) {
                onFailure.onFailure(e);
            }
        }
    }
    
    public interface StorageCallback {
        void onSuccess(String downloadUrl);
        void onFailure(Exception exception);
    }
}

