package com.sendajapan.sendasnap.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sendajapan.sendasnap.R;
import com.sendajapan.sendasnap.models.shipment.Schedule;
import com.sendajapan.sendasnap.utils.DateFormatter;

import java.util.List;

public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder> {

    private final List<Schedule> schedules;
    private final OnScheduleClickListener listener;
    private OnScheduleLongClickListener longClickListener;

    public interface OnScheduleClickListener {
        void onScheduleClick(Schedule schedule);
    }

    public interface OnScheduleLongClickListener {
        void onScheduleLongClick(Schedule schedule);
    }

    public ScheduleAdapter(List<Schedule> schedules, OnScheduleClickListener listener) {
        this.schedules = schedules;
        this.listener = listener;
    }

    public void setOnScheduleLongClickListener(OnScheduleLongClickListener longClickListener) {
        this.longClickListener = longClickListener;
    }

    @NonNull
    @Override
    public ScheduleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_schedule_card, parent, false);
        return new ScheduleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ScheduleViewHolder holder, int position) {
        Schedule schedule = schedules.get(position);
        holder.bind(schedule);
    }

    @Override
    public int getItemCount() {
        return schedules.size();
    }

    class ScheduleViewHolder extends RecyclerView.ViewHolder {

        private final TextView txtVesselName;
        private final TextView txtVoyageNo;
        private final TextView txtStartPort;
        private final TextView txtEndPort;
        private final TextView txtCreatedAt;
        private final ImageView imgRightArrow;

        public ScheduleViewHolder(@NonNull View itemView) {
            super(itemView);
            
            txtVesselName = itemView.findViewById(R.id.txtVesselName);
            txtVoyageNo = itemView.findViewById(R.id.txtVoyageNo);
            txtStartPort = itemView.findViewById(R.id.txtStartPort);
            txtEndPort = itemView.findViewById(R.id.txtEndPort);
            txtCreatedAt = itemView.findViewById(R.id.txtCreatedAt);
            imgRightArrow = itemView.findViewById(R.id.imgRightArrow);

            itemView.setOnClickListener(v -> {
                if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onScheduleClick(schedules.get(getAdapterPosition()));
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (longClickListener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    longClickListener.onScheduleLongClick(schedules.get(getAdapterPosition()));
                    return true;
                }
                return false;
            });
        }

        public void bind(Schedule schedule) {
            if (txtVesselName != null) {
                txtVesselName.setText(schedule.getVesselName());
            }

            if (txtVoyageNo != null) {
                txtVoyageNo.setText(schedule.getVoyageNo());
            }

            if (txtStartPort != null) {
                if (schedule.getStartPort() != null) {
                    txtStartPort.setText(schedule.getStartPort().getPortName());
                } else {
                    txtStartPort.setText("");
                }
            }

            if (txtEndPort != null) {
                if (schedule.getEndPort() != null) {
                    txtEndPort.setText(schedule.getEndPort().getPortName());
                } else {
                    txtEndPort.setText("");
                }
            }

            if (txtCreatedAt != null && schedule.getCreatedAt() != null) {
                String formattedDate = DateFormatter.formatDateForDisplay(schedule.getCreatedAt());
                txtCreatedAt.setText("Created: " + formattedDate);
            } else if (txtCreatedAt != null) {
                txtCreatedAt.setText("");
            }
        }
    }
}
