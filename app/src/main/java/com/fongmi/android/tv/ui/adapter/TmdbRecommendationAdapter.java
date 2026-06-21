package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.TmdbItem;
import com.fongmi.android.tv.utils.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * TMDB 推荐影片横向滚动适配器。
 */
public class TmdbRecommendationAdapter extends RecyclerView.Adapter<TmdbRecommendationAdapter.ViewHolder> {

    private final List<TmdbItem> items = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(TmdbItem item);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<TmdbItem> recommendations) {
        items.clear();
        if (recommendations != null) items.addAll(recommendations);
        notifyDataSetChanged();
    }

    public void appendItems(List<TmdbItem> recommendations) {
        if (recommendations == null || recommendations.isEmpty()) return;
        int start = items.size();
        for (TmdbItem item : recommendations) {
            if (item == null || contains(item)) continue;
            items.add(item);
        }
        if (items.size() > start) notifyItemRangeInserted(start, items.size() - start);
    }

    public List<TmdbItem> getItems() {
        return new ArrayList<>(items);
    }

    private boolean contains(TmdbItem target) {
        for (TmdbItem item : items) {
            if (sameItem(item, target)) return true;
        }
        return false;
    }

    private boolean sameItem(TmdbItem first, TmdbItem second) {
        if (first == null || second == null) return false;
        if (first.getTmdbId() > 0 && second.getTmdbId() > 0) {
            return first.getTmdbId() == second.getTmdbId() && first.getMediaType().equals(second.getMediaType());
        }
        return normalizedTitle(first).equals(normalizedTitle(second));
    }

    private String normalizedTitle(TmdbItem item) {
        String title = item == null ? "" : item.getTitle();
        return title.replaceAll("[\\s·•・._\\-/\\\\|()（）\\[\\]【】《》<>:：,，.。]+", "").trim().toLowerCase(Locale.ROOT);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_tmdb_recommendation, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ImageView poster;
        private final TextView title;
        private final TextView rating;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            if (!Util.isLeanback()) {
                itemView.setFocusable(false);
                itemView.setFocusableInTouchMode(false);
            }
            poster = itemView.findViewById(R.id.poster);
            title = itemView.findViewById(R.id.title);
            rating = itemView.findViewById(R.id.rating);
        }

        void bind(TmdbItem item, OnItemClickListener listener) {
            title.setText(item.getTitle());

            if (item.getPosterUrl() != null && !item.getPosterUrl().isEmpty()) {
                Glide.with(poster.getContext())
                        .load(item.getPosterUrl())
                        .override(300, 450)   // 限制加载尺寸
                        .thumbnail(0.1f)      // 缩略图
                        .dontAnimate()        // 禁用动画
                        .into(poster);
            } else {
                poster.setImageResource(R.color.black);
            }

            double vote = item.getRating();
            if (vote > 0) {
                rating.setText(String.format(Locale.US, "★ %.1f", vote));
                rating.setVisibility(View.VISIBLE);
            } else {
                rating.setVisibility(View.GONE);
            }

            if (listener != null) {
                itemView.setOnClickListener(v -> listener.onItemClick(item));
            }
        }
    }
}
