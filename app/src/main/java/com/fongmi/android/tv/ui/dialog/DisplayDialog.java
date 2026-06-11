package com.fongmi.android.tv.ui.dialog;

import android.view.View;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.databinding.DialogDisplayBinding;
import com.fongmi.android.tv.setting.PlayerSetting;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class DisplayDialog extends BaseAlertDialog {

    private static final float DIALOG_WIDTH_LANDSCAPE = 0.42f;
    private static final float DIALOG_WIDTH_PORTRAIT = 0.92f;

    private DialogDisplayBinding binding;
    private Runnable callback;

    public static void show(FragmentActivity activity, Runnable callback) {
        for (Fragment fragment : activity.getSupportFragmentManager().getFragments()) if (fragment instanceof DisplayDialog) return;
        new DisplayDialog().callback(callback).show(activity.getSupportFragmentManager(), null);
    }

    private DisplayDialog callback(Runnable callback) {
        this.callback = callback;
        return this;
    }

    @Override
    protected ViewBinding getBinding() {
        return binding = DialogDisplayBinding.inflate(getLayoutInflater());
    }

    @Override
    protected MaterialAlertDialogBuilder getBuilder() {
        return builder().setView(getBinding().getRoot());
    }

    @Override
    protected void initView() {
        binding.displayTime.setSelected(PlayerSetting.isDisplayTime());
        binding.displayTraffic.setSelected(PlayerSetting.isDisplayTraffic());
        binding.displaySize.setSelected(PlayerSetting.isDisplaySize());
        binding.displayProgress.setSelected(PlayerSetting.isDisplayProgress());
        binding.displayMini.setSelected(PlayerSetting.isDisplayMini());
        binding.displayTitle.setSelected(PlayerSetting.isDisplayTitle());
    }

    @Override
    protected void initEvent() {
        binding.displayTime.setOnClickListener(v -> setDisplay(binding.displayTime, Display.TIME));
        binding.displayTraffic.setOnClickListener(v -> setDisplay(binding.displayTraffic, Display.TRAFFIC));
        binding.displaySize.setOnClickListener(v -> setDisplay(binding.displaySize, Display.SIZE));
        binding.displayProgress.setOnClickListener(v -> setDisplay(binding.displayProgress, Display.PROGRESS));
        binding.displayMini.setOnClickListener(v -> setDisplay(binding.displayMini, Display.MINI));
        binding.displayTitle.setOnClickListener(v -> setDisplay(binding.displayTitle, Display.TITLE));
    }

    private void setDisplay(TextView view, Display display) {
        boolean selected = !view.isSelected();
        view.setSelected(selected);
        switch (display) {
            case TIME:
                PlayerSetting.putDisplayTime(selected);
                break;
            case TRAFFIC:
                PlayerSetting.putDisplayTraffic(selected);
                break;
            case SIZE:
                PlayerSetting.putDisplaySize(selected);
                break;
            case PROGRESS:
                PlayerSetting.putDisplayProgress(selected);
                break;
            case MINI:
                PlayerSetting.putDisplayMini(selected);
                break;
            case TITLE:
                PlayerSetting.putDisplayTitle(selected);
                break;
        }
        if (callback != null) callback.run();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        setWidth(ResUtil.isLand(requireContext()) ? DIALOG_WIDTH_LANDSCAPE : DIALOG_WIDTH_PORTRAIT);
    }

    private enum Display {
        TIME, TRAFFIC, SIZE, PROGRESS, MINI, TITLE
    }
}
