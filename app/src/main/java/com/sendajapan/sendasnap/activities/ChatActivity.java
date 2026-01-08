package com.sendajapan.sendasnap.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.sendajapan.sendasnap.MyApplication;
import com.sendajapan.sendasnap.R;
import com.sendajapan.sendasnap.adapters.MessageAdapter;
import com.sendajapan.sendasnap.databinding.ActivityChatBinding;
import com.sendajapan.sendasnap.databinding.BottomSheetImagePickerBinding;
import com.sendajapan.sendasnap.models.Message;
import com.sendajapan.sendasnap.models.UserData;
import com.sendajapan.sendasnap.services.ChatService;
import com.sendajapan.sendasnap.services.FirebaseStorageService;
import com.sendajapan.sendasnap.utils.CookieBarToastHelper;
import com.sendajapan.sendasnap.utils.FirebaseUtils;
import com.sendajapan.sendasnap.utils.HapticFeedbackHelper;
import com.sendajapan.sendasnap.utils.SharedPrefsManager;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ChatActivity extends AppCompatActivity {

    private static final int MAX_AVATARS = 5;
    private static final int AVATAR_SIZE_DP = 18;
    private static final int OVERLAP_OFFSET_DP = -4;
    private static final int SCROLL_DELAY_MS = 100;
    private static final int KEYBOARD_THRESHOLD_DP = 200;
    private static final int KEYBOARD_MARGIN_OFFSET_DP = 80;
    private static final int DEFAULT_MARGIN_DP = 8;
    private static final String IMAGE_MIME_TYPE = "image/*";
    private static final String ALL_FILES_MIME_TYPE = "*/*";
    private static final String FILE_PROVIDER_AUTHORITY_SUFFIX = ".fileprovider";
    private static final String CAMERA_IMAGE_PREFIX = "chat_image_";
    private static final String CAMERA_IMAGE_SUFFIX = ".jpg";
    private static final String DEFAULT_FILE_NAME = "file";

    private boolean isGroupChat = false;

    private ActivityChatBinding binding;
    private MessageAdapter messageAdapter;

    private ChatService chatService;
    private FirebaseStorageService storageService;
    private HapticFeedbackHelper hapticHelper;
    private SharedPrefsManager prefsManager;

    private String chatId;
    private String currentUserId;
    private String otherUserId;
    private String otherUserName;
    private String taskTitle;

    private List<UserData> participants = new ArrayList<>();
    private long lastMessageTimestamp = 0L;

    private ActivityResultLauncher<String> filePickerLauncher;
    private ActivityResultLauncher<String> galleryLauncher;
    private ActivityResultLauncher<String[]> permissionLauncher;
    private ActivityResultLauncher<Uri> cameraLauncher;

    private Uri cameraImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        MyApplication.applyWindowInsets(binding.getRoot());

        initHelpers();
        getIntentData();
        setupToolbar();
        setupKeyboardHandling();
        setupMemberAvatars();
        setupRecyclerView();
        setupClickListeners();
        setupActivityResultLaunchers();
        initializeCurrentUser();
        loadMessages();
        markMessagesAsSeen();
    }

    private void initHelpers() {
        chatService = ChatService.getInstance();
        storageService = FirebaseStorageService.getInstance();
        hapticHelper = HapticFeedbackHelper.getInstance(this);
        prefsManager = SharedPrefsManager.getInstance(this);
        currentUserId = FirebaseUtils.getCurrentUserId(this);
    }

    public String getChatId() {
        return chatId;
    }

    private void initializeCurrentUser() {
        try {
            chatService.initializeUser(this);
        } catch (Exception e) {
        }
    }

    @SuppressWarnings("unchecked")
    private void getIntentData() {
        chatId = getIntent().getStringExtra("chatId");
        isGroupChat = getIntent().getBooleanExtra("isGroupChat", false);
        taskTitle = getIntent().getStringExtra("taskTitle");
        otherUserId = getIntent().getStringExtra("otherUserId");
        otherUserName = getIntent().getStringExtra("otherUserName");

        Serializable participantsSerializable = getIntent().getSerializableExtra("participants");
        if (participantsSerializable instanceof List) {
            try {
                participants = (List<UserData>) participantsSerializable;
            } catch (ClassCastException e) {
                participants = new ArrayList<>();
            }
        }
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        String toolbarTitle = isGroupChat && taskTitle != null 
                ? taskTitle 
                : (otherUserName != null ? otherUserName : "Chat");
        binding.toolbar.setTitle(toolbarTitle);

        binding.toolbar.post(() -> {
            TextView titleView = findTitleTextView(binding.toolbar);
            if (titleView != null) {
                titleView.setMaxLines(2);
                titleView.setEllipsize(TextUtils.TruncateAt.END);
                titleView.setSingleLine(false);
            }
        });

        binding.toolbar.setNavigationOnClickListener(v -> {
            hapticHelper.vibrateClick();
            finish();
        });
    }

    private TextView findTitleTextView(ViewGroup parent) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            if (child instanceof TextView) {
                TextView textView = (TextView) child;
                String toolbarTitle = binding.toolbar.getTitle() != null 
                        ? binding.toolbar.getTitle().toString() 
                        : "";
                String textViewText = textView.getText() != null 
                        ? textView.getText().toString() 
                        : "";
                if (textViewText.equals(toolbarTitle) && textView.getVisibility() == View.VISIBLE) {
                    return textView;
                }
            } else if (child instanceof ViewGroup) {
                TextView found = findTitleTextView((ViewGroup) child);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private void setupKeyboardHandling() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.cardInput, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.ime());

            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) binding.cardInput
                    .getLayoutParams();
            float density = getResources().getDisplayMetrics().density;
            
            if (insets.bottom > KEYBOARD_THRESHOLD_DP) {
                params.bottomMargin = insets.bottom;
            } else {
                params.bottomMargin = (int) (DEFAULT_MARGIN_DP * density);
            }

            binding.cardInput.setLayoutParams(params);
            return windowInsets;
        });
    }

    @SuppressLint("SetTextI18n")
    private void setupMemberAvatars() {
        if (!isGroupChat || participants == null || participants.isEmpty()) {
            binding.layoutMemberAvatars.setVisibility(View.GONE);
            return;
        }

        binding.layoutMemberAvatars.setVisibility(View.VISIBLE);
        binding.layoutMemberAvatars.removeAllViews();

        float density = getResources().getDisplayMetrics().density;
        int avatarSize = (int) (AVATAR_SIZE_DP * density);
        int overlapOffset = (int) (OVERLAP_OFFSET_DP * density);
        int displayCount = Math.min(participants.size(), MAX_AVATARS);

        for (int i = 0; i < displayCount; i++) {
            UserData user = participants.get(i);
            ImageView avatarView = createAvatarView(avatarSize, overlapOffset, i);
            loadAvatarImage(user, avatarView);
            binding.layoutMemberAvatars.addView(avatarView);
        }

        if (participants.size() > MAX_AVATARS) {
            TextView moreTextView = createMoreTextView(avatarSize, overlapOffset);
            binding.layoutMemberAvatars.addView(moreTextView);
        }
    }

    private ImageView createAvatarView(int avatarSize, int overlapOffset, int index) {
        ImageView avatarView = new ImageView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(avatarSize, avatarSize);
        if (index > 0) {
            params.setMargins(overlapOffset, 0, 0, 0);
        }
        avatarView.setLayoutParams(params);
        avatarView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        avatarView.setBackgroundResource(R.drawable.avater_placeholder);
        return avatarView;
    }

    private void loadAvatarImage(UserData user, ImageView avatarView) {
        String avatarUrl = user.getAvatarUrl() != null ? user.getAvatarUrl() : user.getAvatar();
        if (avatarUrl != null && !avatarUrl.isEmpty() && isValidUrl(avatarUrl)) {
            Glide.with(this)
                    .load(avatarUrl)
                    .placeholder(R.drawable.avater_placeholder)
                    .error(R.drawable.avater_placeholder)
                    .circleCrop()
                    .into(avatarView);
        } else {
            Glide.with(this)
                    .load(R.drawable.avater_placeholder)
                    .circleCrop()
                    .into(avatarView);
        }
    }

    private TextView createMoreTextView(int avatarSize, int overlapOffset) {
        TextView moreTextView = new TextView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(avatarSize, avatarSize);
        params.setMargins(overlapOffset, 0, 0, 0);
        moreTextView.setLayoutParams(params);
        moreTextView.setText("+" + (participants.size() - MAX_AVATARS));
        moreTextView.setTextSize(10);
        moreTextView.setTextColor(getColor(R.color.white));
        moreTextView.setGravity(Gravity.CENTER);
        moreTextView.setBackgroundResource(R.drawable.avater_placeholder);
        moreTextView.setBackgroundTintList(getColorStateList(R.color.primary));
        return moreTextView;
    }

    private boolean isValidUrl(String url) {
        return url != null && (url.startsWith("http://") || url.startsWith("https://"));
    }

    private void setupRecyclerView() {
        messageAdapter = new MessageAdapter(currentUserId, isGroupChat);
        messageAdapter.setOnImageClickListener(imageUrl -> {
            Toast.makeText(this, "Image viewer coming soon", Toast.LENGTH_SHORT).show();
        });
        messageAdapter.setOnFileClickListener((fileUrl, fileName) -> {
            Toast.makeText(this, "File download coming soon", Toast.LENGTH_SHORT).show();
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        binding.recyclerViewMessages.setLayoutManager(layoutManager);
        binding.recyclerViewMessages.setAdapter(messageAdapter);
    }

    private void setupClickListeners() {
        binding.btnSend.setOnClickListener(v -> {
            hapticHelper.vibrateClick();
            sendTextMessage();
        });

        binding.btnAttachment.setOnClickListener(v -> {
            hapticHelper.vibrateClick();
            showAttachmentBottomSheet();
        });

        binding.etMessage.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendTextMessage();
                return true;
            }
            return false;
        });

        binding.etMessage.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                binding.recyclerViewMessages.postDelayed(this::scrollToBottom, SCROLL_DELAY_MS);
            }
        });
    }

    private void setupActivityResultLaunchers() {
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && cameraImageUri != null) {
                        uploadImage(cameraImageUri);
                    }
                });

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        uploadImage(uri);
                    }
                });

        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        uploadFile(uri);
                    }
                });

        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                permissions -> {
                });
    }

    private void sendTextMessage() {
        String messageText = binding.etMessage.getText() != null 
                ? binding.etMessage.getText().toString().trim() 
                : "";

        if (TextUtils.isEmpty(messageText)) {
            return;
        }

        binding.etMessage.setText("");

        ChatService.MessageCallback callback = createMessageCallback();
        if (isGroupChat) {
            chatService.sendGroupMessage(chatId, messageText, callback);
        } else {
            chatService.sendMessage(chatId, otherUserId, messageText, callback);
        }
    }

    private ChatService.MessageCallback createMessageCallback() {
        return new ChatService.MessageCallback() {
            @Override
            public void onSuccess(Message message) {
                scrollToBottom();
            }

            @Override
            public void onFailure(Exception e) {
                CookieBarToastHelper.showError(ChatActivity.this, "Error",
                        "Failed to send message", CookieBarToastHelper.LONG_DURATION);
            }
        };
    }

    private void showAttachmentBottomSheet() {
        BottomSheetDialog bottomSheet = new BottomSheetDialog(this);
        BottomSheetImagePickerBinding bottomSheetBinding = BottomSheetImagePickerBinding.inflate(getLayoutInflater());

        bottomSheetBinding.layoutCamera.setOnClickListener(v -> {
            bottomSheet.dismiss();
            openCamera();
        });

        bottomSheetBinding.layoutGallery.setOnClickListener(v -> {
            bottomSheet.dismiss();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                openGallery();
            }
        });

        bottomSheetBinding.layoutFile.setOnClickListener(v -> {
            bottomSheet.dismiss();
            openFilePicker();
        });

        bottomSheet.setContentView(bottomSheetBinding.getRoot());
        bottomSheet.show();
    }

    private void openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
                != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(new String[] { Manifest.permission.CAMERA });
            return;
        }

        try {
            File photoFile = new File(getExternalFilesDir(null), 
                    CAMERA_IMAGE_PREFIX + System.currentTimeMillis() + CAMERA_IMAGE_SUFFIX);
            cameraImageUri = FileProvider.getUriForFile(this,
                    getPackageName() + FILE_PROVIDER_AUTHORITY_SUFFIX, photoFile);
            cameraLauncher.launch(cameraImageUri);
        } catch (Exception e) {
            Toast.makeText(this, "Failed to open camera", Toast.LENGTH_SHORT).show();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void openGallery() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(new String[] { Manifest.permission.READ_MEDIA_IMAGES });
            return;
        }

        galleryLauncher.launch(IMAGE_MIME_TYPE);
    }

    private void openFilePicker() {
        filePickerLauncher.launch(ALL_FILES_MIME_TYPE);
    }

    private void uploadImage(Uri imageUri) {
        CookieBarToastHelper.showInfo(this, "Uploading", "Uploading image...",
                CookieBarToastHelper.SHORT_DURATION);

        storageService.uploadImage(this, imageUri, chatId, new FirebaseStorageService.StorageCallback() {
            @Override
            public void onSuccess(String downloadUrl) {
                chatService.sendImageMessage(chatId, otherUserId, downloadUrl,
                        createMessageCallback());
            }

            @Override
            public void onFailure(Exception exception) {
                CookieBarToastHelper.showError(ChatActivity.this, "Error",
                        "Failed to upload image", CookieBarToastHelper.LONG_DURATION);
            }
        });
    }

    private void uploadFile(Uri fileUri) {
        String fileName = getFileName(fileUri);
        CookieBarToastHelper.showInfo(this, "Uploading", "Uploading file...",
                CookieBarToastHelper.SHORT_DURATION);

        storageService.uploadFile(this, fileUri, chatId, fileName, new FirebaseStorageService.StorageCallback() {
            @Override
            public void onSuccess(String downloadUrl) {
                chatService.sendFileMessage(chatId, otherUserId, downloadUrl, fileName,
                        createMessageCallback());
            }

            @Override
            public void onFailure(Exception exception) {
                CookieBarToastHelper.showError(ChatActivity.this, "Error",
                        "Failed to upload file", CookieBarToastHelper.LONG_DURATION);
            }
        });
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (Objects.equals(uri.getScheme(), "content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME);
                    if (nameIndex >= 0) {
                        result = cursor.getString(nameIndex);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            if (result != null) {
                int cut = result.lastIndexOf('/');
                if (cut != -1) {
                    result = result.substring(cut + 1);
                }
            }
        }
        return result != null ? result : DEFAULT_FILE_NAME;
    }

    private void loadMessages() {
        chatService.getChatMessages(chatId, new ChatService.MessagesCallback() {
            @Override
            public void onSuccess(List<Message> messages) {
                if (isFinishing() || binding == null || messageAdapter == null) {
                    return;
                }

                try {
                    checkAndTriggerNewMessageFeedback(messages);
                    messageAdapter.updateMessages(messages);
                    updateEmptyState(messages.isEmpty());
                    scrollToBottom();
                } catch (Exception e) {
                }
            }

            @Override
            public void onFailure(Exception e) {
                if (!isFinishing() && binding != null) {
                    updateEmptyState(true);
                }
            }
        });
    }

    private void checkAndTriggerNewMessageFeedback(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }

        Message latestMessage = messages.get(messages.size() - 1);
        if (latestMessage == null) {
            return;
        }

        long messageTimestamp = latestMessage.getTimestamp();
        
        if (lastMessageTimestamp == 0L) {
            lastMessageTimestamp = messageTimestamp;
            return;
        }

        if (messageTimestamp <= lastMessageTimestamp) {
            return;
        }

        if (latestMessage.getSenderId() != null && latestMessage.getSenderId().equals(currentUserId)) {
            lastMessageTimestamp = messageTimestamp;
            return;
        }

        lastMessageTimestamp = messageTimestamp;

        markMessagesAsSeen();
    }

    private void updateEmptyState(boolean isEmpty) {
        if (binding == null) {
            return;
        }

        if (isEmpty) {
            binding.layoutEmptyState.setVisibility(View.VISIBLE);
            binding.recyclerViewMessages.setVisibility(View.GONE);
        } else {
            binding.layoutEmptyState.setVisibility(View.GONE);
            binding.recyclerViewMessages.setVisibility(View.VISIBLE);
        }
    }

    private void markMessagesAsSeen() {
        if (isGroupChat) {
            chatService.markGroupChatAsSeen(chatId, currentUserId);
        } else {
            chatService.markAsSeen(chatId, currentUserId);
        }
    }

    private void scrollToBottom() {
        if (isFinishing() || binding == null || messageAdapter == null) {
            return;
        }

        binding.recyclerViewMessages.post(() -> {
            if (isFinishing() || binding == null || messageAdapter == null) {
                return;
            }

            int itemCount = messageAdapter.getItemCount();
            if (itemCount <= 0) {
                return;
            }

            try {
                binding.recyclerViewMessages.smoothScrollToPosition(itemCount - 1);
            } catch (Exception e) {
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        chatService.updateLastSeen(this);
        markMessagesAsSeen();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
