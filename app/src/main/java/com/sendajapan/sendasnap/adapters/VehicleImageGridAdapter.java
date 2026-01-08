package com.sendajapan.sendasnap.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.sendajapan.sendasnap.R;

import java.util.List;

public class VehicleImageGridAdapter extends RecyclerView.Adapter<VehicleImageGridAdapter.ImageViewHolder> {

    private List<String> imageUrls;
    private OnImageClickListener onImageClickListener;

    public interface OnImageClickListener {
        void onImageClick(String imageUrl, int position);
    }

    public VehicleImageGridAdapter(List<String> imageUrls) {
        this.imageUrls = imageUrls != null ? imageUrls : new java.util.ArrayList<>();
    }

    public void setOnImageClickListener(OnImageClickListener listener) {
        this.onImageClickListener = listener;
    }

    public void updateImages(List<String> newImageUrls) {
        if (newImageUrls == null) {
            newImageUrls = new java.util.ArrayList<>();
        }
        int oldSize = this.imageUrls.size();
        this.imageUrls = newImageUrls;
        int newSize = this.imageUrls.size();
        
        if (newSize > oldSize) {
            notifyItemRangeInserted(oldSize, newSize - oldSize);
        } else if (newSize < oldSize) {
            notifyItemRangeRemoved(newSize, oldSize - newSize);
        }
        notifyItemRangeChanged(0, Math.min(oldSize, newSize));
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_vehicle_image_grid, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        holder.bind(imageUrls.get(position), position);
    }

    @Override
    public int getItemCount() {
        return imageUrls.size();
    }

    class ImageViewHolder extends RecyclerView.ViewHolder {
        private ImageView imgVehiclePhoto;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imgVehiclePhoto = itemView.findViewById(R.id.imgVehiclePhoto);
        }

        public void bind(String imageUrl, int position) {
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(imageUrl)
                        .placeholder(R.drawable.car_placeholder)
                        .error(R.drawable.car_placeholder)
                        .apply(RequestOptions.bitmapTransform(new RoundedCorners(16)))
                        .into(imgVehiclePhoto);
            } else {
                imgVehiclePhoto.setImageResource(R.drawable.car_placeholder);
            }

            itemView.setOnClickListener(v -> {
                if (onImageClickListener != null) {
                    onImageClickListener.onImageClick(imageUrl, position);
                }
            });
        }
    }
}
