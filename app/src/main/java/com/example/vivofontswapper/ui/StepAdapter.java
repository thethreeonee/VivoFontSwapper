package com.example.vivofontswapper.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vivofontswapper.R;
import com.example.vivofontswapper.model.Step;

import java.util.ArrayList;
import java.util.List;

public class StepAdapter extends RecyclerView.Adapter<StepAdapter.StepViewHolder> {

    private List<Step> steps = new ArrayList<>();

    public void setSteps(List<Step> steps) {
        this.steps = steps;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public StepViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_step, parent, false);
        return new StepViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StepViewHolder holder, int position) {
        Step step = steps.get(position);
        holder.bind(step);
    }

    @Override
    public int getItemCount() {
        return steps.size();
    }

    static class StepViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvIndex;
        private final TextView tvTitle;
        private final TextView tvStatus;

        public StepViewHolder(@NonNull View itemView) {
            super(itemView);
            tvIndex = itemView.findViewById(R.id.tv_step_index);
            tvTitle = itemView.findViewById(R.id.tv_step_title);
            tvStatus = itemView.findViewById(R.id.tv_step_status);
        }

        public void bind(Step step) {
            tvIndex.setText(String.valueOf(step.getIndex() + 1));
            tvTitle.setText(step.getTitle());

            int mainColor;
            int statusColor;
            String statusText;
            switch (step.getStatus()) {
                case RUNNING:
                    mainColor = 0xFF1F57B9;
                    statusColor = 0xFF1F57B9;
                    statusText = "执行中";
                    break;
                case SUCCESS:
                    mainColor = 0xFF0F6E3E;
                    statusColor = 0xFF0F6E3E;
                    statusText = "执行成功";
                    break;
                case FAILED:
                    mainColor = 0xFFC62828;
                    statusColor = 0xFFC62828;
                    statusText = "执行失败";
                    break;
                default:
                    mainColor = 0xFF4E4E4E;
                    statusColor = 0xFF8E8E95;
                    statusText = "等待执行";
                    break;
            }

            tvTitle.setTextColor(mainColor);
            tvIndex.setTextColor(mainColor);
            tvStatus.setTextColor(statusColor);
            tvStatus.setText(statusText);
        }
    }
}
