package com.example.vivofontswapper.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
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
        private final TextView tvDetail;
        private final ImageView ivStatus;
        private final ProgressBar progressBar;

        public StepViewHolder(@NonNull View itemView) {
            super(itemView);
            tvIndex = itemView.findViewById(R.id.tv_step_index);
            tvTitle = itemView.findViewById(R.id.tv_step_title);
            tvDetail = itemView.findViewById(R.id.tv_step_detail);
            ivStatus = itemView.findViewById(R.id.iv_step_status);
            progressBar = itemView.findViewById(R.id.pb_step);
        }

        public void bind(Step step) {
            tvIndex.setText(String.valueOf(step.getIndex() + 1));

            // 根据状态显示不同颜色
            int textColor;
            int bgColor;
            switch (step.getStatus()) {
                case RUNNING:
                    textColor = 0xFF1976D2; // blue
                    bgColor = 0xFFE3F2FD;
                    break;
                case SUCCESS:
                    textColor = 0xFF388E3C; // green
                    bgColor = 0xFFE8F5E9;
                    break;
                case FAILED:
                    textColor = 0xFFD32F2F; // red
                    bgColor = 0xFFFFEBEE;
                    break;
                default:
                    textColor = 0xFF757575; // gray
                    bgColor = 0xFFF5F5F5;
                    break;
            }

            tvTitle.setTextColor(textColor);
            tvIndex.setTextColor(textColor);
            itemView.setBackgroundColor(bgColor);

            tvTitle.setText(step.getTitle());

            String detail = step.getDetail();
            if (detail != null && !detail.isEmpty()) {
                tvDetail.setVisibility(View.VISIBLE);
                tvDetail.setText(detail);
                tvDetail.setTextColor(textColor);
            } else {
                tvDetail.setVisibility(View.GONE);
            }

            // 状态图标 & 进度条
            switch (step.getStatus()) {
                case RUNNING:
                    ivStatus.setVisibility(View.GONE);
                    progressBar.setVisibility(View.VISIBLE);
                    break;
                case SUCCESS:
                    ivStatus.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                    ivStatus.setImageResource(android.R.drawable.checkbox_on_background);
                    break;
                case FAILED:
                    ivStatus.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                    ivStatus.setImageResource(android.R.drawable.ic_delete);
                    break;
                default:
                    ivStatus.setVisibility(View.GONE);
                    progressBar.setVisibility(View.GONE);
                    break;
            }
        }
    }
}
