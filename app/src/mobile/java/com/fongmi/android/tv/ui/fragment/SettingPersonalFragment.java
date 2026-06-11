package com.fongmi.android.tv.ui.fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.FragmentSettingPersonalBinding;
import com.fongmi.android.tv.setting.PlayerSetting;
import com.fongmi.android.tv.setting.Setting;
import com.fongmi.android.tv.ui.base.BaseFragment;
import com.fongmi.android.tv.ui.dialog.DisplayDialog;

public class SettingPersonalFragment extends BaseFragment {

    private FragmentSettingPersonalBinding mBinding;
    private String[] searchUi;
    private String[] searchColumn;
    private String[] siteColumn;
    private String[] tmdbMatchMode;

    public static SettingPersonalFragment newInstance() {
        return new SettingPersonalFragment();
    }

    private String getSwitch(boolean value) {
        return getString(value ? R.string.setting_on : R.string.setting_off);
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return mBinding = FragmentSettingPersonalBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        setText();
    }

    @Override
    protected void initEvent() {
        mBinding.playBackToDetail.setOnClickListener(this::setPlayBackToDetail);
        mBinding.tmdbMatchMode.setOnClickListener(this::setTmdbMatchMode);
        mBinding.tmdbBackdropSlide.setOnClickListener(this::setTmdbBackdropSlide);
        mBinding.searchUi.setOnClickListener(this::setSearchUi);
        mBinding.searchColumn.setOnClickListener(this::setSearchColumn);
        mBinding.siteColumn.setOnClickListener(this::setSiteColumn);
        mBinding.display.setOnClickListener(this::onDisplay);
    }

    private void setText() {
        mBinding.playBackToDetailText.setText(getSwitch(Setting.isPlayBackToDetail()));
        mBinding.tmdbMatchModeText.setText((tmdbMatchMode = getResources().getStringArray(R.array.select_tmdb_match_mode))[Setting.getTmdbMatchMode()]);
        mBinding.tmdbBackdropSlideText.setText(getSwitch(Setting.isTmdbDetailBackdropSlide()));
        mBinding.searchUiText.setText((searchUi = getResources().getStringArray(R.array.select_search_ui))[Setting.getSearchUi()]);
        mBinding.searchColumnText.setText((searchColumn = getResources().getStringArray(R.array.select_search_column))[Setting.getSearchColumn()]);
        mBinding.siteColumnText.setText((siteColumn = getResources().getStringArray(R.array.select_site_column))[Setting.getSiteColumn() - 1]);
        mBinding.displayText.setText(getDisplayText());
    }

    private String getDisplayText() {
        StringBuilder builder = new StringBuilder();
        addDisplay(builder, PlayerSetting.isDisplayTime(), R.string.display_time);
        addDisplay(builder, PlayerSetting.isDisplayTraffic(), R.string.display_traffic);
        addDisplay(builder, PlayerSetting.isDisplaySize(), R.string.display_size);
        addDisplay(builder, PlayerSetting.isDisplayProgress(), R.string.display_progress);
        addDisplay(builder, PlayerSetting.isDisplayMini(), R.string.display_mini);
        addDisplay(builder, PlayerSetting.isDisplayTitle(), R.string.display_title);
        return builder.length() == 0 ? getString(R.string.setting_off) : builder.toString();
    }

    private void addDisplay(StringBuilder builder, boolean selected, int resId) {
        if (!selected) return;
        if (builder.length() > 0) builder.append(" / ");
        builder.append(getString(resId));
    }

    private void setPlayBackToDetail(View view) {
        Setting.putPlayBackToDetail(!Setting.isPlayBackToDetail());
        setText();
    }

    private void setTmdbMatchMode(View view) {
        Setting.putTmdbMatchMode((Setting.getTmdbMatchMode() + 1) % tmdbMatchMode.length);
        setText();
    }

    private void setTmdbBackdropSlide(View view) {
        Setting.putTmdbDetailBackdropSlide(!Setting.isTmdbDetailBackdropSlide());
        setText();
    }

    private void setSearchUi(View view) {
        Setting.putSearchUi((Setting.getSearchUi() + 1) % searchUi.length);
        setText();
    }

    private void setSearchColumn(View view) {
        Setting.putSearchColumn((Setting.getSearchColumn() + 1) % searchColumn.length);
        setText();
    }

    private void setSiteColumn(View view) {
        Setting.putSiteColumn(Setting.getSiteColumn() == 1 ? 2 : 1);
        setText();
    }

    private void onDisplay(View view) {
        DisplayDialog.show(requireActivity(), this::setText);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        if (!hidden) setText();
    }
}
