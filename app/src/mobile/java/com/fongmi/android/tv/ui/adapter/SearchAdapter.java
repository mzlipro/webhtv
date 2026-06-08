package com.fongmi.android.tv.ui.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.AdapterSearchBinding;
import com.fongmi.android.tv.databinding.AdapterSearchGridBinding;
import com.fongmi.android.tv.utils.ImgUtil;
import com.fongmi.android.tv.utils.ResUtil;

import java.util.ArrayList;
import java.util.List;

public class SearchAdapter extends BaseDiffAdapter<Vod, RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_LIST = 1;
    private static final int VIEW_TYPE_GRID = 2;
    private static final int LIST_NAME_MAX = 120;
    private static final int GRID_NAME_MAX = 48;
    private static final int REMARK_MAX = 40;
    private static final int SITE_MAX = 24;
    private static final int YEAR_MAX = 8;

    private final OnClickListener listener;
    private final List<Vod> items;
    private int columnCount = 2;
    private int gridWidth;
    private boolean loadImages = true;

    public SearchAdapter(OnClickListener listener) {
        this.listener = listener;
        this.items = new ArrayList<>();
    }

    public interface OnClickListener {

        void onItemClick(Vod item);
    }

    @Override
    public Vod getItem(int position) {
        return items.get(position);
    }

    @Override
    public List<Vod> getItems() {
        return items;
    }

    @Override
    public void setItems(List<Vod> items) {
        setItems(items, () -> {});
    }

    @Override
    public void setItems(List<Vod> items, Runnable runnable) {
        this.items.clear();
        if (items != null) this.items.addAll(items);
        notifyDataSetChanged();
        if (runnable != null) runnable.run();
    }

    @Override
    public void setItems(List<Vod> items, Callback callback) {
        setItems(items, () -> {
            if (callback != null) callback.onUpdateFinished(true);
        });
    }

    @Override
    public void addAll(List<Vod> items) {
        addAll(items, null);
    }

    @Override
    public void addAll(List<Vod> items, Runnable runnable) {
        if (items == null || items.isEmpty()) {
            if (runnable != null) runnable.run();
            return;
        }
        int position = this.items.size();
        this.items.addAll(items);
        notifyItemRangeInserted(position, items.size());
        if (runnable != null) runnable.run();
    }

    @Override
    public void clear(Runnable runnable) {
        int count = items.size();
        items.clear();
        if (count > 0) notifyItemRangeRemoved(0, count);
        if (runnable != null) runnable.run();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void setColumnCount(int columnCount) {
        int count = Math.max(1, columnCount);
        if (this.columnCount == count) return;
        this.columnCount = count;
        notifyDataSetChanged();
    }

    public void setGridWidth(int gridWidth) {
        int width = Math.max(0, gridWidth);
        if (this.gridWidth == width) return;
        this.gridWidth = width;
        if (isGrid()) notifyDataSetChanged();
    }

    private boolean isGrid() {
        return columnCount > 1;
    }

    public boolean isGridMode() {
        return isGrid();
    }

    public void setLoadImages(boolean loadImages) {
        this.loadImages = loadImages;
    }

    public boolean isLoadImages() {
        return loadImages;
    }

    public void loadImage(@NonNull RecyclerView.ViewHolder holder) {
        int position = holder.getBindingAdapterPosition();
        if (position == RecyclerView.NO_POSITION || position >= getItemCount()) return;
        Vod item = getItem(position);
        boolean load = loadImages;
        loadImages = true;
        try {
            if (holder instanceof GridViewHolder grid) {
                grid.bindImage(item);
                return;
            }
            if (holder instanceof ListViewHolder list) list.bindImage(item);
        } finally {
            loadImages = load;
        }
    }

    private void loadGridImage(Vod item, ImageView image) {
        loadGridImage(item, image, displayName(item, GRID_NAME_MAX));
    }

    private void loadGridImage(Vod item, ImageView image, String name) {
        if (!loadImages) {
            ImgUtil.hold(item.getPic(), image, true);
            return;
        }
        int width = image.getWidth();
        int height = image.getHeight();
        if (width <= 0 && image.getParent() instanceof View parent) width = parent.getWidth();
        if (height <= 0) height = image.getLayoutParams().height;
        ImgUtil.loadThumb(name, item.getPic(), image, width, height);
    }

    private void loadListImage(Vod item, ImageView image) {
        loadListImage(item, image, displayName(item, LIST_NAME_MAX));
    }

    private void loadListImage(Vod item, ImageView image, String name) {
        if (!loadImages) {
            ImgUtil.hold(item.getPic(), image, true);
            return;
        }
        ImgUtil.load(name, item.getPic(), image);
    }

    private static String displayName(Vod item, int maxLength) {
        return displayText(item.getName(), maxLength);
    }

    private static String displayText(String text, int maxLength) {
        if (TextUtils.isEmpty(text) || maxLength <= 0) return "";
        String value = text.trim();
        if (value.isEmpty()) return "";
        StringBuilder builder = new StringBuilder(Math.min(value.length(), maxLength + 3));
        boolean lastSpace = false;
        boolean truncated = false;
        for (int i = 0; i < value.length(); ) {
            int codePoint = value.codePointAt(i);
            i += Character.charCount(codePoint);
            if (Character.isWhitespace(codePoint) || Character.isISOControl(codePoint)) {
                if (lastSpace || builder.length() == 0) continue;
                codePoint = ' ';
                lastSpace = true;
            } else {
                lastSpace = false;
            }
            if (builder.length() + Character.charCount(codePoint) > maxLength) {
                truncated = true;
                break;
            }
            builder.appendCodePoint(codePoint);
        }
        int length = builder.length();
        if (length > 0 && builder.charAt(length - 1) == ' ') builder.deleteCharAt(length - 1);
        if (truncated) builder.append("...");
        return builder.toString();
    }

    @Override
    public int getItemViewType(int position) {
        return isGrid() ? VIEW_TYPE_GRID : VIEW_TYPE_LIST;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_GRID) return new GridViewHolder(AdapterSearchGridBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false)).size(parent, columnCount);
        return new ListViewHolder(AdapterSearchBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Vod item = getItem(position);
        if (holder instanceof GridViewHolder grid) {
            grid.bind(item);
            return;
        }
        ((ListViewHolder) holder).bind(item);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position);
            return;
        }
        Vod item = getItem(position);
        if (holder instanceof GridViewHolder grid) {
            grid.bindImage(item);
            return;
        }
        ((ListViewHolder) holder).bindImage(item);
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        clearImage(holder);
    }

    private void clearImage(@NonNull RecyclerView.ViewHolder holder) {
        if (holder instanceof GridViewHolder grid) {
            ImgUtil.clear(grid.binding.image);
            return;
        }
        if (holder instanceof ListViewHolder list) ImgUtil.clear(list.binding.image);
    }

    public class ListViewHolder extends RecyclerView.ViewHolder {

        private final AdapterSearchBinding binding;

        ListViewHolder(@NonNull AdapterSearchBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        private void bind(Vod item) {
            String name = displayName(item, LIST_NAME_MAX);
            String site = displayText(item.getSiteName(), SITE_MAX);
            String remark = displayText(item.getRemarks(), REMARK_MAX);
            binding.name.setText(name);
            binding.site.setText(site);
            binding.remark.setText(remark);
            binding.site.setVisibility(TextUtils.isEmpty(site) ? View.GONE : View.VISIBLE);
            binding.remark.setVisibility(TextUtils.isEmpty(remark) ? View.GONE : View.VISIBLE);
            binding.getRoot().setOnClickListener(v -> listener.onItemClick(item));
            bindImage(item, name);
        }

        private void bindImage(Vod item) {
            bindImage(item, displayName(item, LIST_NAME_MAX));
        }

        private void bindImage(Vod item, String name) {
            loadListImage(item, binding.image, name);
        }
    }

    public class GridViewHolder extends RecyclerView.ViewHolder {

        private final AdapterSearchGridBinding binding;

        GridViewHolder(@NonNull AdapterSearchGridBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        private GridViewHolder size(ViewGroup parent, int columnCount) {
            int count = Math.max(1, columnCount);
            int margin = ResUtil.dp2px(16);
            int available = gridWidth;
            if (available <= 0) available = parent.getWidth() - parent.getPaddingStart() - parent.getPaddingEnd();
            if (available <= 0) return this;
            int width = Math.max(ResUtil.dp2px(96), (available - margin * count) / count);
            int height = (int) (width / 0.75f);
            if (binding.getRoot().getLayoutParams().width != width) binding.getRoot().getLayoutParams().width = width;
            if (binding.image.getLayoutParams().height != height) binding.image.getLayoutParams().height = height;
            return this;
        }

        private void bind(Vod item) {
            if (binding.getRoot().getParent() instanceof ViewGroup parent) size(parent, columnCount);
            String name = displayName(item, GRID_NAME_MAX);
            String site = displayText(item.getSiteName(), SITE_MAX);
            String remark = displayText(item.getRemarks(), REMARK_MAX);
            String year = displayText(item.getYear(), YEAR_MAX);
            binding.name.setText(name);
            binding.site.setText(site);
            binding.remark.setText(remark);
            binding.year.setText(year);
            binding.year.setVisibility(item.getYearVisible() == View.VISIBLE && !TextUtils.isEmpty(year) ? View.VISIBLE : View.GONE);
            binding.site.setVisibility(TextUtils.isEmpty(site) ? View.GONE : View.VISIBLE);
            binding.name.setVisibility(TextUtils.isEmpty(name) ? View.GONE : View.VISIBLE);
            binding.remark.setVisibility(TextUtils.isEmpty(remark) ? View.GONE : View.VISIBLE);
            binding.getRoot().setOnClickListener(v -> listener.onItemClick(item));
            bindImage(item, name);
        }

        private void bindImage(Vod item) {
            bindImage(item, displayName(item, GRID_NAME_MAX));
        }

        private void bindImage(Vod item, String name) {
            loadGridImage(item, binding.image, name);
        }
    }
}
