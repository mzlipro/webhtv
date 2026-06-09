package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.AdapterFileBinding;
import com.fongmi.android.tv.utils.ResUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.ViewHolder> {

    private final OnClickListener mListener;
    private final List<File> mItems;
    private final boolean wallpaperMode;

    public FileAdapter(OnClickListener listener) {
        this(listener, false);
    }

    public FileAdapter(OnClickListener listener, boolean wallpaperMode) {
        mListener = listener;
        mItems = new ArrayList<>();
        this.wallpaperMode = wallpaperMode;
    }

    public void addAll(List<File> items) {
        mItems.clear();
        mItems.addAll(items);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(AdapterFileBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        File file = mItems.get(position);
        holder.binding.name.setText(file.getName());
        bindImage(holder, file);
        holder.binding.getRoot().setOnClickListener(v -> mListener.onItemClick(file));
        holder.binding.getRoot().setOnFocusChangeListener((view, focused) -> {
            if (focused) mListener.onItemFocus(file);
        });
    }

    private void bindImage(@NonNull ViewHolder holder, File file) {
        ImageView image = holder.binding.image;
        setImageSize(image);
        Glide.with(image).clear(image);
        image.setPadding(0, 0, 0, 0);
        if (wallpaperMode && !file.isDirectory() && isPreviewable(file)) {
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Glide.with(image).load(file).centerCrop().placeholder(R.drawable.ic_file).error(R.drawable.ic_file).into(image);
            return;
        }
        image.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        image.setPadding(ResUtil.dp2px(10), ResUtil.dp2px(10), ResUtil.dp2px(10), ResUtil.dp2px(10));
        image.setImageResource(file.isDirectory() ? R.drawable.ic_folder : R.drawable.ic_file);
    }

    private void setImageSize(ImageView image) {
        ViewGroup.LayoutParams params = image.getLayoutParams();
        params.width = ResUtil.dp2px(wallpaperMode ? 112 : 48);
        params.height = ResUtil.dp2px(wallpaperMode ? 64 : 48);
        image.setLayoutParams(params);
    }

    public static boolean isPreviewable(File file) {
        if (file == null || file.isDirectory()) return false;
        String name = file.getName().toLowerCase(Locale.US);
        return name.endsWith(".jpg")
                || name.endsWith(".jpeg")
                || name.endsWith(".png")
                || name.endsWith(".webp")
                || name.endsWith(".bmp")
                || name.endsWith(".gif")
                || name.endsWith(".mp4")
                || name.endsWith(".m4v")
                || name.endsWith(".mkv")
                || name.endsWith(".mov")
                || name.endsWith(".webm")
                || name.endsWith(".avi")
                || name.endsWith(".ts")
                || name.endsWith(".m2ts");
    }

    public interface OnClickListener {

        void onItemClick(File file);

        default void onItemFocus(File file) {
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final AdapterFileBinding binding;

        ViewHolder(@NonNull AdapterFileBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
