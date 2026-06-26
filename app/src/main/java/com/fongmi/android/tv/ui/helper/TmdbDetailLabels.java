package com.fongmi.android.tv.ui.helper;

public final class TmdbDetailLabels {

    private TmdbDetailLabels() {
    }

    public static String certificationLabel(String value) {
        if (value == null) return "";
        String label = value.trim();
        return label.matches("\\d+") ? label + "+" : label;
    }

    public static String headerSubtitle(String releaseDate) {
        return releaseDate == null ? "" : releaseDate.trim();
    }
}
