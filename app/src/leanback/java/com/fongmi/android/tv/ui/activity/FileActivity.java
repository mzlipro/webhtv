package com.fongmi.android.tv.ui.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.viewbinding.ViewBinding;

import com.bumptech.glide.Glide;
import com.fongmi.android.tv.databinding.ActivityFileBinding;
import com.fongmi.android.tv.ui.adapter.FileAdapter;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.utils.FileChooser;
import com.fongmi.android.tv.utils.PermissionUtil;
import com.fongmi.android.tv.utils.ResUtil;
import com.github.catvod.utils.Path;

import java.io.File;
import java.util.List;

public class FileActivity extends BaseActivity implements FileAdapter.OnClickListener {

    private ActivityFileBinding mBinding;
    private FileAdapter mAdapter;
    private File previewFile;
    private File dir;
    private boolean wallpaperMode;

    private boolean isRoot() {
        return Path.root().equals(dir);
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityFileBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        wallpaperMode = getIntent().getBooleanExtra(FileChooser.EXTRA_WALLPAPER, false);
        mBinding.wallpaperHint.setVisibility(wallpaperMode ? View.VISIBLE : View.GONE);
        setRecyclerView();
        checkPermission();
    }

    private void setRecyclerView() {
        mBinding.recycler.setHasFixedSize(true);
        mBinding.recycler.setVerticalSpacing(ResUtil.dp2px(16));
        mBinding.recycler.setAdapter(mAdapter = new FileAdapter(this, wallpaperMode));
    }

    private void checkPermission() {
        PermissionUtil.requestFile(this, allGranted -> update(Path.root()));
    }

    private void update(File dir) {
        mBinding.recycler.setSelectedPosition(0);
        List<File> items = Path.list(this.dir = dir);
        mAdapter.addAll(items);
        if (wallpaperMode) {
            previewFile = null;
            if (items.isEmpty()) hideWallpaperPreview();
            else onItemFocus(items.get(0));
        }
        mBinding.progressLayout.showContent(true, mAdapter.getItemCount());
    }

    @Override
    public void onItemClick(File file) {
        if (file.isDirectory()) {
            update(file);
        } else {
            setResult(RESULT_OK, new Intent().setData(Uri.fromFile(file)));
            finish();
        }
    }

    @Override
    public void onItemFocus(File file) {
        if (!wallpaperMode) return;
        if (!FileAdapter.isPreviewable(file)) {
            hideWallpaperPreview();
            return;
        }
        if (file.equals(previewFile)) return;
        previewFile = file;
        mBinding.wallpaperPreview.setVisibility(View.VISIBLE);
        mBinding.wallpaperScrim.setVisibility(View.VISIBLE);
        mBinding.wallpaperPreview.setAlpha(0f);
        Glide.with(mBinding.wallpaperPreview)
                .load(file)
                .centerCrop()
                .into(mBinding.wallpaperPreview);
        mBinding.wallpaperPreview.animate().alpha(1f).setDuration(160).start();
    }

    private void hideWallpaperPreview() {
        previewFile = null;
        Glide.with(mBinding.wallpaperPreview).clear(mBinding.wallpaperPreview);
        mBinding.wallpaperPreview.setVisibility(View.GONE);
        mBinding.wallpaperScrim.setVisibility(View.GONE);
    }

    @Override
    protected void onBackInvoked() {
        if (isRoot()) {
            super.onBackInvoked();
        } else {
            update(dir.getParentFile());
        }
    }
}
