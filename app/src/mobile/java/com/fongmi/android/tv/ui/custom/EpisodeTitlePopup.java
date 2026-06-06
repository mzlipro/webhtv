package com.fongmi.android.tv.ui.custom;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;

import com.fongmi.android.tv.utils.ResUtil;
import com.google.android.material.textview.MaterialTextView;

public class EpisodeTitlePopup {

    private static final Handler HANDLER = new Handler(Looper.getMainLooper());
    private static PopupWindow popupWindow;

    public static boolean show(View anchor, CharSequence title) {
        if (anchor == null || TextUtils.isEmpty(title)) return false;
        dismiss();
        MaterialTextView textView = createTextView(anchor.getContext(), title);
        PopupWindow popup = new PopupWindow(textView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, false);
        popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popup.setOutsideTouchable(true);
        popup.setClippingEnabled(true);
        popupWindow = popup;
        showAtAnchor(anchor, textView, popup);
        HANDLER.postDelayed(EpisodeTitlePopup::dismiss, 6000);
        return true;
    }

    public static void dismiss() {
        HANDLER.removeCallbacksAndMessages(null);
        if (popupWindow == null) return;
        popupWindow.dismiss();
        popupWindow = null;
    }

    private static MaterialTextView createTextView(Context context, CharSequence title) {
        MaterialTextView textView = new MaterialTextView(context);
        textView.setText(title);
        textView.setTextColor(Color.WHITE);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        textView.setGravity(Gravity.CENTER_VERTICAL);
        textView.setLineSpacing(ResUtil.dp2px(3), 1.0f);
        textView.setSingleLine(false);
        textView.setMaxLines(8);
        textView.setEllipsize(TextUtils.TruncateAt.END);
        textView.setPadding(ResUtil.dp2px(16), ResUtil.dp2px(10), ResUtil.dp2px(16), ResUtil.dp2px(10));
        textView.setMinWidth(ResUtil.dp2px(200));
        textView.setMaxWidth(Math.min(ResUtil.dp2px(520), (int) (ResUtil.getScreenWidth(context) * 0.78f)));
        textView.setBackground(background());
        return textView;
    }

    private static GradientDrawable background() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(0xF0222730);
        drawable.setCornerRadius(ResUtil.dp2px(8));
        drawable.setStroke(ResUtil.dp2px(1), 0x66FFFFFF);
        return drawable;
    }

    private static void showAtAnchor(View anchor, View content, PopupWindow popup) {
        int margin = ResUtil.dp2px(12);
        int gap = ResUtil.dp2px(8);
        int screenWidth = ResUtil.getScreenWidth(anchor.getContext());
        int screenHeight = ResUtil.getScreenHeight(anchor.getContext());
        int maxWidth = Math.min(ResUtil.dp2px(520), screenWidth - margin * 2);
        int maxHeight = screenHeight - margin * 2;
        content.measure(View.MeasureSpec.makeMeasureSpec(maxWidth, View.MeasureSpec.AT_MOST), View.MeasureSpec.makeMeasureSpec(maxHeight, View.MeasureSpec.AT_MOST));
        int popupWidth = Math.min(Math.max(content.getMeasuredWidth(), Math.min(anchor.getWidth(), maxWidth)), maxWidth);
        int popupHeight = Math.min(content.getMeasuredHeight(), maxHeight);
        popup.setWidth(popupWidth);
        popup.setHeight(popupHeight);

        int[] location = new int[2];
        anchor.getLocationOnScreen(location);
        int x = location[0] + (anchor.getWidth() - popupWidth) / 2;
        int y = location[1] - popupHeight - gap;
        if (y < margin) y = location[1] + anchor.getHeight() + gap;
        if (y + popupHeight > screenHeight - margin) y = screenHeight - popupHeight - margin;
        x = Math.max(margin, Math.min(x, screenWidth - popupWidth - margin));
        popup.showAtLocation(anchor.getRootView(), Gravity.NO_GRAVITY, x, Math.max(margin, y));
    }
}
