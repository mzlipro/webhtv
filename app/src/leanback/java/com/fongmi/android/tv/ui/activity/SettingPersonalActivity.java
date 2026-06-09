package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.HomeButton;
import com.fongmi.android.tv.databinding.ActivitySettingPersonalBinding;
import com.fongmi.android.tv.setting.PlayerSetting;
import com.fongmi.android.tv.setting.Setting;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.dialog.DisplayDialog;
import com.fongmi.android.tv.ui.dialog.HomeButtonDialog;

public class SettingPersonalActivity extends BaseActivity {

    private ActivitySettingPersonalBinding mBinding;
    private String[] fullscreenMenuKey;
    private String[] homeMenuKey;
    private String[] searchUi;
    private String[] searchColumn;

    public static void start(Activity activity) {
        activity.startActivity(new Intent(activity, SettingPersonalActivity.class));
    }

    private String getSwitch(boolean value) {
        return getString(value ? R.string.setting_on : R.string.setting_off);
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivitySettingPersonalBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        mBinding.homeVodAutoLoad.requestFocus();
        setText();
    }

    @Override
    protected void initEvent() {
        mBinding.homeVodAutoLoad.setOnClickListener(this::setHomeVodAutoLoad);
        mBinding.homeButtons.setOnClickListener(this::onHomeButtons);
        mBinding.fullscreenMenuKey.setOnClickListener(this::setFullscreenMenuKey);
        mBinding.homeMenuKey.setOnClickListener(this::setHomeMenuKey);
        mBinding.playBackToDetail.setOnClickListener(this::setPlayBackToDetail);
        mBinding.homeHistory.setOnClickListener(this::setHomeHistory);
        mBinding.searchUi.setOnClickListener(this::setSearchUi);
        mBinding.searchColumn.setOnClickListener(this::setSearchColumn);
        mBinding.display.setOnClickListener(this::onDisplay);
    }

    private void setText() {
        mBinding.homeVodAutoLoadText.setText(getSwitch(Setting.isHomeVodAutoLoad()));
        mBinding.homeButtonsText.setText(getString(R.string.home_buttons_selected, HomeButton.getButtons().size(), HomeButton.all().size()));
        mBinding.fullscreenMenuKeyText.setText((fullscreenMenuKey = getResources().getStringArray(R.array.select_fullscreen_menu_key))[Setting.getFullscreenMenuKey()]);
        mBinding.homeMenuKeyText.setText((homeMenuKey = getResources().getStringArray(R.array.select_home_menu_key))[Setting.getHomeMenuKey()]);
        mBinding.playBackToDetailText.setText(getSwitch(Setting.isPlayBackToDetail()));
        mBinding.homeHistoryText.setText(getSwitch(Setting.isHomeHistory()));
        mBinding.searchUiText.setText((searchUi = getResources().getStringArray(R.array.select_search_ui))[Setting.getSearchUi()]);
        mBinding.searchColumnText.setText(getSearchColumnText());
        mBinding.displayText.setText(getDisplayText());
    }

    private String getSearchColumnText() {
        searchColumn = getResources().getStringArray(R.array.select_search_column);
        return searchColumn[Setting.getSearchColumn() == 1 ? 1 : 0];
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

    private void setHomeVodAutoLoad(View view) {
        Setting.putHomeVodAutoLoad(!Setting.isHomeVodAutoLoad());
        setText();
    }

    private void onHomeButtons(View view) {
        HomeButtonDialog.show(this, this::setText);
    }

    private void setFullscreenMenuKey(View view) {
        Setting.putFullscreenMenuKey((Setting.getFullscreenMenuKey() + 1) % fullscreenMenuKey.length);
        setText();
    }

    private void setHomeMenuKey(View view) {
        Setting.putHomeMenuKey((Setting.getHomeMenuKey() + 1) % homeMenuKey.length);
        setText();
    }

    private void setPlayBackToDetail(View view) {
        Setting.putPlayBackToDetail(!Setting.isPlayBackToDetail());
        setText();
    }

    private void setHomeHistory(View view) {
        Setting.putHomeHistory(!Setting.isHomeHistory());
        setText();
    }

    private void setSearchUi(View view) {
        Setting.putSearchUi((Setting.getSearchUi() + 1) % searchUi.length);
        setText();
    }

    private void setSearchColumn(View view) {
        Setting.putSearchColumn(Setting.getSearchColumn() == 1 ? 0 : 1);
        setText();
    }

    private void onDisplay(View view) {
        DisplayDialog.show(this, this::setText);
    }
}
