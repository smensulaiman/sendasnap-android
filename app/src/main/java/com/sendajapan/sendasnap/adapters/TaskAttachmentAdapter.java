package com.sendajapan.sendasnap.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.sendajapan.sendasnap.R;
import com.sendajapan.sendasnap.models.TaskAttachment;

import java.io.File;
import java.util.List;

public class TaskAttachmentAdapter extends RecyclerView.Adapter<TaskAttachmentAdapter.AttachmentViewHolder> {

    private final List<TaskAttachment> attachments;
    private OnAttachmentRemoveListener removeListener;
    public OnAttachmentActionListener actionListener; // Made public for debugging
    private boolean showRemoveButton;
    private Context context;

    public interface OnAttachmentRemoveListener {
        void onRemove(int position, TaskAttachment attachment);
    }

    public interface OnAttachmentActionListener {
        void onOpen(int position, TaskAttachment attachment);
    }

    public TaskAttachmentAdapter(List<TaskAttachment> attachments, boolean showRemoveButton) {
        this.attachments = attachments;
        this.showRemoveButton = showRemoveButton;
    }

    public void setOnAttachmentRemoveListener(OnAttachmentRemoveListener listener) {
        this.removeListener = listener;
    }

    public void setOnAttachmentActionListener(OnAttachmentActionListener listener) {
        this.actionListener = listener;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public AttachmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task_attachment, parent, false);
        return new AttachmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AttachmentViewHolder holder, int position) {
        TaskAttachment attachment = attachments.get(position);
        holder.bind(attachment, position);
    }

    @Override
    public int getItemCount() {
        return attachments.size();
    }

    class AttachmentViewHolder extends RecyclerView.ViewHolder {
        private ImageView imgFileIcon;
        private TextView txtFileName;
        private TextView txtFileSize;
        private MaterialButton btnAction;
        private MaterialButton btnRemoveFile;

        AttachmentViewHolder(@NonNull View itemView) {
            super(itemView);
            imgFileIcon = itemView.findViewById(R.id.imgFileIcon);
            txtFileName = itemView.findViewById(R.id.txtFileName);
            txtFileSize = itemView.findViewById(R.id.txtFileSize);
            btnAction = itemView.findViewById(R.id.btnAction);
            btnRemoveFile = itemView.findViewById(R.id.btnRemoveFile);
        }

        void bind(TaskAttachment attachment, int position) {
            if (attachment != null) {
                txtFileName.setText(attachment.getFileName() != null ? attachment.getFileName() : "");
                // Only show file size if it's greater than 0
                if (attachment.getFileSize() > 0) {
                    txtFileSize.setText(attachment.getFormattedFileSize());
                    txtFileSize.setVisibility(View.VISIBLE);
                } else {
                    txtFileSize.setVisibility(View.GONE);
                }

                // Use itemView's context as fallback if context is not set
                Context ctx = context != null ? context : itemView.getContext();

                // Determine if file is a URL (from server) or local file
                // Prefer file_url if available, otherwise use file_path
                String filePathOrUrl = attachment.getFileUrl();
                if (filePathOrUrl == null || filePathOrUrl.isEmpty()) {
                    filePathOrUrl = attachment.getFilePath();
                }
                
                boolean isUrl = isUrl(filePathOrUrl);
                boolean isRelativePath = isRelativeServerPath(filePathOrUrl);
                boolean isLocalUri = filePathOrUrl != null && 
                                    (filePathOrUrl.startsWith("content://") || filePathOrUrl.startsWith("file://"));
                boolean isLocalFile = !isUrl && !isRelativePath && !isLocalUri && 
                                     filePathOrUrl != null && 
                                     isFileInAppStorage(filePathOrUrl);


                if (isUrl || isRelativePath) {
                    btnAction.setVisibility(View.VISIBLE);
                    btnAction.setEnabled(true);
                    btnAction.setClickable(true);
                    btnAction.setText("Open");
                    btnAction.setTextColor(ctx.getResources().getColor(R.color.primary, null));
                    btnAction.setIcon(ctx.getResources().getDrawable(R.drawable.ic_file, null));
                    btnAction.setIconTint(ctx.getResources().getColorStateList(R.color.primary, null));
                    // Store reference to adapter in the click listener to avoid null issues
                    final TaskAttachmentAdapter adapterRef = TaskAttachmentAdapter.this;
                    btnAction.setOnClickListener(v -> {
                        OnAttachmentActionListener listener = actionListener;
                        if (listener == null) {
                            listener = adapterRef.actionListener;
                        }
                        if (listener == null) {
                            View parent = (View) itemView.getParent();
                            if (parent != null && parent instanceof RecyclerView) {
                                RecyclerView.Adapter<?> rvAdapter = ((RecyclerView) parent).getAdapter();
                                if (rvAdapter instanceof TaskAttachmentAdapter) {
                                    listener = ((TaskAttachmentAdapter) rvAdapter).actionListener;
                                }
                            }
                        }
                        if (listener != null) {
                            listener.onOpen(position, attachment);
                        } else {
                            String fileUrl = attachment.getFileUrl();
                            if (fileUrl == null || fileUrl.isEmpty()) {
                                fileUrl = attachment.getFilePath();
                            }
                            if (fileUrl != null && !fileUrl.isEmpty()) {
                                if (!fileUrl.startsWith("http://") && !fileUrl.startsWith("https://")) {
                                    if (fileUrl.startsWith("/")) {
                                        fileUrl = "https://snap.senda.fit" + fileUrl;
                                    } else {
                                        fileUrl = "https://snap.senda.fit/storage/" + fileUrl;
                                    }
                                }
                                try {
                                    Intent intent = new Intent(Intent.ACTION_VIEW);
                                    intent.setData(Uri.parse(fileUrl));
                                    ctx.startActivity(Intent.createChooser(intent, "Open file"));
                                } catch (Exception e) {
                                }
                            }
                        }
                    });
                } else if (isLocalFile || isLocalUri) {
                    // Local file - show "View" button
                    btnAction.setVisibility(View.VISIBLE);
                    btnAction.setEnabled(true);
                    btnAction.setClickable(true);
                    btnAction.setText("View");
                    btnAction.setTextColor(ctx.getResources().getColor(R.color.primary, null));
                    btnAction.setIcon(ctx.getResources().getDrawable(R.drawable.ic_file, null));
                    btnAction.setIconTint(ctx.getResources().getColorStateList(R.color.primary, null));
                    btnAction.setOnClickListener(v -> {
                        if (actionListener != null) {
                            actionListener.onOpen(position, attachment);
                        }
                    });
                } else {
                    btnAction.setVisibility(View.GONE);
                }

                // Show/hide remove button
                btnRemoveFile.setVisibility(showRemoveButton ? View.VISIBLE : View.GONE);

                // Set remove button click listener
                if (showRemoveButton) {
                    btnRemoveFile.setOnClickListener(v -> {
                        if (removeListener != null) {
                            removeListener.onRemove(position, attachment);
                        }
                    });
                }
            }
        }

        private boolean isUrl(String filePath) {
            if (filePath == null) return false;
            return filePath.startsWith("http://") || filePath.startsWith("https://");
        }

        private boolean isRelativeServerPath(String filePath) {
            if (filePath == null) return false;
            // If it's not a URL, not a content URI, and not a file URI, it's likely a server path
            // Server paths can be relative (task-attachments/file.jpg) or absolute from root (/storage/...)
            boolean isLocalUri = filePath.startsWith("content://") || filePath.startsWith("file://");
            boolean isAbsoluteLocalPath = !isLocalUri && isFileInAppStorage(filePath);
            
            return !filePath.startsWith("http://") && 
                   !filePath.startsWith("https://") && 
                   !isLocalUri &&
                   !isAbsoluteLocalPath;
        }

        private boolean isFileInAppStorage(String filePath) {
            if (filePath == null || context == null) return false;
            try {
                File file = new File(filePath);
                if (!file.exists() || !file.isFile()) {
                    return false;
                }
                
                // Only access files in app-specific directories
                File cacheDir = context.getCacheDir();
                File filesDir = context.getFilesDir();
                File externalFilesDir = context.getExternalFilesDir(null);
                
                return (cacheDir != null && filePath.startsWith(cacheDir.getAbsolutePath())) ||
                       (filesDir != null && filePath.startsWith(filesDir.getAbsolutePath())) ||
                       (externalFilesDir != null && filePath.startsWith(externalFilesDir.getAbsolutePath()));
            } catch (Exception e) {
                return false;
            }
        }

    }
}

