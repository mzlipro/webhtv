package com.fongmi.android.tv.utils;

import android.text.TextUtils;

import com.fongmi.android.tv.bean.Episode;
import com.fongmi.android.tv.bean.Flag;
import com.fongmi.android.tv.bean.Vod;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TmdbEpisodeSorter {

    private static final Pattern SOURCE_SEASON = Pattern.compile("(?i)(?:第\\s*([零〇一二三四五六七八九十两0-9]+)\\s*[季部]|season\\s*([0-9]{1,2})|s([0-9]{1,2})(?:[-._\\s]*e[0-9]{1,3})?)");

    private TmdbEpisodeSorter() {
    }

    public static void sort(Vod vod) {
        if (vod == null || vod.getFlags() == null) return;
        for (Flag flag : vod.getFlags()) sort(flag);
    }

    private static void sort(Flag flag) {
        if (flag == null || flag.getEpisodes() == null || flag.getEpisodes().size() < 2) return;
        List<IndexedEpisode> indexed = new ArrayList<>();
        int recognized = 0;
        for (int i = 0; i < flag.getEpisodes().size(); i++) {
            Episode episode = flag.getEpisodes().get(i);
            if (episode != null && episode.getNumber() > 0) recognized++;
            indexed.add(new IndexedEpisode(episode, sourceSeasonNumber(episode), number(episode), i));
        }
        if (recognized < 2 || isSorted(indexed)) return;
        indexed.sort(TmdbEpisodeSorter::compare);
        flag.getEpisodes().clear();
        for (IndexedEpisode item : indexed) flag.getEpisodes().add(item.episode());
    }

    private static boolean isSorted(List<IndexedEpisode> episodes) {
        for (int i = 1; i < episodes.size(); i++) {
            if (compare(episodes.get(i - 1), episodes.get(i)) > 0) return false;
        }
        return true;
    }

    private static int compare(IndexedEpisode left, IndexedEpisode right) {
        if (left.number() > 0 && right.number() > 0) {
            if (left.season() > 0 && right.season() > 0 && left.season() != right.season()) return Integer.compare(left.season(), right.season());
            int result = Integer.compare(left.number(), right.number());
            return result != 0 ? result : Integer.compare(left.index(), right.index());
        }
        if (left.number() > 0) return -1;
        if (right.number() > 0) return 1;
        return Integer.compare(left.index(), right.index());
    }

    private static int number(Episode episode) {
        return episode == null ? -1 : episode.getNumber();
    }

    private static int sourceSeasonNumber(Episode episode) {
        if (episode == null || TextUtils.isEmpty(episode.getName())) return -1;
        Matcher matcher = SOURCE_SEASON.matcher(episode.getName());
        while (matcher.find()) {
            int number = normalizeSourceNumber(firstNonEmptyGroup(matcher, 1, 2, 3));
            if (number > 0) return number;
        }
        return -1;
    }

    private static String firstNonEmptyGroup(Matcher matcher, int... groups) {
        if (matcher == null || groups == null) return "";
        for (int group : groups) {
            String value = matcher.group(group);
            if (!TextUtils.isEmpty(value)) return value;
        }
        return "";
    }

    private static int normalizeSourceNumber(String value) {
        if (TextUtils.isEmpty(value)) return -1;
        value = value.trim();
        try {
            if (value.matches("\\d+")) return Integer.parseInt(value.replaceFirst("^0+(?!$)", ""));
        } catch (Exception ignored) {
            return -1;
        }
        int number = parseSmallChineseNumber(value);
        return number > 0 ? number : -1;
    }

    private static int parseSmallChineseNumber(String value) {
        if (TextUtils.isEmpty(value)) return 0;
        value = value.replace("两", "二").replace("零", "").replace("〇", "");
        if (value.matches("[一二三四五六七八九]")) return chineseDigit(value.charAt(0));
        int tenIndex = value.indexOf("十");
        if (tenIndex >= 0) {
            int tens = tenIndex == 0 ? 1 : chineseDigit(value.charAt(tenIndex - 1));
            int ones = tenIndex == value.length() - 1 ? 0 : chineseDigit(value.charAt(tenIndex + 1));
            return tens * 10 + ones;
        }
        return 0;
    }

    private static int chineseDigit(char value) {
        return switch (value) {
            case '一' -> 1;
            case '二' -> 2;
            case '三' -> 3;
            case '四' -> 4;
            case '五' -> 5;
            case '六' -> 6;
            case '七' -> 7;
            case '八' -> 8;
            case '九' -> 9;
            default -> 0;
        };
    }

    private record IndexedEpisode(Episode episode, int season, int number, int index) {
    }
}
