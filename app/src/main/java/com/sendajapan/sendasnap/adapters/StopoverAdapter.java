package com.sendajapan.sendasnap.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sendajapan.sendasnap.R;
import com.sendajapan.sendasnap.databinding.ItemStopoverBinding;
import com.sendajapan.sendasnap.models.shipment.Stopover;
import com.sendajapan.sendasnap.utils.DateFormatter;

import java.util.List;

public class StopoverAdapter extends RecyclerView.Adapter<StopoverAdapter.StopoverViewHolder> {

    private final List<Stopover> stopovers;
    private final OnStopoverClickListener listener;
    private OnStopoverLongClickListener longClickListener;

    public interface OnStopoverClickListener {
        void onStopoverClick(Stopover stopover);
    }

    public interface OnStopoverLongClickListener {
        void onStopoverLongClick(Stopover stopover);
    }

    public StopoverAdapter(List<Stopover> stopovers, OnStopoverClickListener listener) {
        this.stopovers = stopovers;
        this.listener = listener;
    }

    public void setOnStopoverLongClickListener(OnStopoverLongClickListener longClickListener) {
        this.longClickListener = longClickListener;
    }

    @NonNull
    @Override
    public StopoverViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemStopoverBinding binding = ItemStopoverBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new StopoverViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull StopoverViewHolder holder, int position) {
        Stopover stopover = stopovers.get(position);
        holder.bind(stopover);
    }

    @Override
    public int getItemCount() {
        return stopovers.size();
    }

    class StopoverViewHolder extends RecyclerView.ViewHolder {

        private final ItemStopoverBinding binding;

        public StopoverViewHolder(@NonNull ItemStopoverBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            itemView.setOnClickListener(v -> {
                if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onStopoverClick(stopovers.get(getAdapterPosition()));
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (longClickListener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    longClickListener.onStopoverLongClick(stopovers.get(getAdapterPosition()));
                    return true;
                }
                return false;
            });
        }

        public void bind(Stopover stopover) {
            if (stopover.getPort() != null) {
                String portName = stopover.getPort().getPortName();
                if (stopover.getPort().getPortType() != null) {
                    portName += " (" + stopover.getPort().getPortType() + ")";
                }
                binding.txtPortName.setText(portName);
            }

            if (stopover.getStopoverEta() != null && !stopover.getStopoverEta().isEmpty()) {
                binding.txtArrival.setText(DateFormatter.formatDateTimeForDisplay(stopover.getStopoverEta()));
                binding.txtArrival.setVisibility(View.VISIBLE);
            } else {
                binding.txtArrival.setVisibility(View.GONE);
            }

            if (stopover.getStopoverEtd() != null && !stopover.getStopoverEtd().isEmpty()) {
                binding.txtDeparture.setText(DateFormatter.formatDateTimeForDisplay(stopover.getStopoverEtd()));
                binding.txtDeparture.setVisibility(View.VISIBLE);
            } else {
                binding.txtDeparture.setVisibility(View.GONE);
            }
        }
    }
}
