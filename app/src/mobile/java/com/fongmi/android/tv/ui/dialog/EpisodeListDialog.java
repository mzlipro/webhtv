package com.fongmi.android.tv.ui.dialog;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.bean.Episode;
import com.fongmi.android.tv.databinding.DialogEpisodeListBinding;
import com.fongmi.android.tv.ui.adapter.EpisodeAdapter;
import com.fongmi.android.tv.ui.base.ViewType;
import com.fongmi.android.tv.ui.custom.EpisodeTitlePopup;
import com.fongmi.android.tv.utils.ResUtil;

import java.util.List;

public class EpisodeListDialog extends BaseSideSheetDialog implements EpisodeAdapter.OnClickListener {

    private DialogEpisodeListBinding binding;
    private EpisodeAdapter adapter;
    private List<Episode> episodes;

    public static EpisodeListDialog create() {
        return new EpisodeListDialog();
    }

    public EpisodeListDialog episodes(List<Episode> episodes) {
        this.episodes = episodes;
        return this;
    }

    public void show(FragmentActivity activity) {
        for (Fragment f : activity.getSupportFragmentManager().getFragments()) if (f instanceof EpisodeListDialog) return;
        show(activity.getSupportFragmentManager(), null);
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return binding = DialogEpisodeListBinding.inflate(inflater, container, false);
    }

    @Override
    protected int getWidth() {
        int minWidth = ResUtil.dp2px(200);
        int screenWidth = ResUtil.getScreenWidth(requireActivity());
        int maxWidth = ResUtil.isLand(requireActivity()) ? (int) (screenWidth * 0.42f) : screenWidth / 3;
        for (Episode item : episodes) minWidth = Math.max(minWidth, ResUtil.getTextWidth(EpisodeAdapter.getTitle(item), 13) + ResUtil.dp2px(32));
        return Math.min(minWidth, maxWidth);
    }

    @Override
    public void onDestroyView() {
        EpisodeTitlePopup.dismiss();
        super.onDestroyView();
    }

    @Override
    protected void initView() {
        setRecyclerView();
        adapter.addAll(episodes);
        binding.recycler.scrollToPosition(adapter.getPosition());
    }

    private void setRecyclerView() {
        binding.recycler.setHasFixedSize(true);
        binding.recycler.setItemAnimator(null);
        binding.recycler.setAdapter(adapter = new EpisodeAdapter(this, ViewType.GRID));
    }

    @Override
    public void onItemClick(Episode item) {
        ((EpisodeAdapter.OnClickListener) requireActivity()).onItemClick(item);
        dismiss();
    }
}
