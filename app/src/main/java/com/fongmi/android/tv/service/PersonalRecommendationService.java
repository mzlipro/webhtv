package com.fongmi.android.tv.service;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.AudioConfig;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.bean.ShortDramaConfig;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.bean.TmdbConfig;
import com.fongmi.android.tv.bean.TmdbItem;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.db.AppDatabase;
import com.fongmi.android.tv.setting.Setting;
import com.fongmi.android.tv.ui.helper.TmdbMatcher;
import com.github.catvod.crawler.SpiderDebug;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;

public class PersonalRecommendationService {

    private static final int MAX_RESULTS = 12;
    private static final int MAX_TMDB_HISTORY_SEEDS = 4;
    private static final int MAX_DOUBAN_SEEDS = 8;
    private static final String DOUBAN_SUGGEST_URL = "https://movie.douban.com/j/subject_suggest";
    private static final String DOUBAN_REFERER = "https://movie.douban.com/";
    private static final String DOUBAN_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    private static final Pattern YEAR = Pattern.compile("(19\\d{2}|20\\d{2})");

    private final TmdbService tmdbService;
    private final TmdbConfig tmdbConfig;
    private final TmdbMatcher tmdbMatcher;

    public PersonalRecommendationService() {
        this(new TmdbService(), TmdbConfig.objectFrom(Setting.getTmdbConfig()));
    }

    public PersonalRecommendationService(TmdbService tmdbService, TmdbConfig tmdbConfig) {
        this.tmdbService = tmdbService == null ? new TmdbService() : tmdbService;
        this.tmdbConfig = tmdbConfig == null ? new TmdbConfig() : tmdbConfig;
        this.tmdbMatcher = new TmdbMatcher(this.tmdbService, this.tmdbConfig);
    }

    public Recommendations load(@Nullable Vod currentVod, @Nullable TmdbItem currentItem, @Nullable JsonObject currentDetail) {
        if (!Setting.isPersonalRecommendation()) return Recommendations.empty();
        return new Recommendations(loadTmdb(currentVod, currentItem, currentDetail), loadDouban(currentVod));
    }

    public List<TmdbItem> loadTmdb(@Nullable Vod currentVod, @Nullable TmdbItem currentItem, @Nullable JsonObject currentDetail) {
        if (!Setting.isPersonalRecommendation() || !tmdbConfig.isReady()) return new ArrayList<>();
        return loadFromTmdb(currentVod, currentItem, currentDetail);
    }

    public List<TmdbItem> loadDouban(@Nullable Vod currentVod) {
        if (!Setting.isPersonalRecommendation()) return new ArrayList<>();
        return loadFromDouban(currentVod);
    }

    private List<TmdbItem> loadFromTmdb(@Nullable Vod currentVod, @Nullable TmdbItem currentItem, @Nullable JsonObject currentDetail) {
        LinkedHashMap<String, TmdbItem> results = new LinkedHashMap<>();
        Set<String> blockedTitles = blockedTitles(currentVod);
        String currentTitle = currentTitle(currentVod, currentItem);
        if (!isBlank(currentTitle)) blockedTitles.add(normalizeTitle(currentTitle));

        int seedCount = 0;
        for (String seed : historySeeds(currentTitle, MAX_TMDB_HISTORY_SEEDS, true)) {
            try {
                TmdbItem seedItem = tmdbMatcher.searchAndMatch(seed);
                if (seedItem == null) continue;
                JsonObject detail = tmdbService.detail(seedItem, tmdbConfig);
                addTmdbItems(results, blockedTitles, tmdbService.recommendations(detail, tmdbConfig), currentItem);
                addTmdbItems(results, blockedTitles, tmdbService.similar(detail, tmdbConfig), currentItem);
                seedCount++;
                if (results.size() >= MAX_RESULTS || seedCount >= MAX_TMDB_HISTORY_SEEDS) break;
            } catch (Throwable e) {
                SpiderDebug.log("personal-rec", "TMDB seed failed title=%s error=%s", seed, e.getMessage());
            }
        }

        if (currentDetail != null && results.size() < MAX_RESULTS) {
            addTmdbItems(results, blockedTitles, tmdbService.recommendations(currentDetail, tmdbConfig), currentItem);
            addTmdbItems(results, blockedTitles, tmdbService.similar(currentDetail, tmdbConfig), currentItem);
        }

        return new ArrayList<>(results.values());
    }

    private List<TmdbItem> loadFromDouban(@Nullable Vod currentVod) {
        LinkedHashMap<String, TmdbItem> results = new LinkedHashMap<>();
        String currentTitle = currentTitle(currentVod, null);
        Set<String> sourceTitles = blockedTitles(currentVod);
        if (!isBlank(currentTitle)) sourceTitles.add(normalizeTitle(currentTitle));

        for (String seed : doubanSeeds(currentTitle)) {
            for (DoubanSubject subject : fetchDoubanSuggest(seed)) {
                String normalized = normalizeTitle(subject.title);
                if (isBlank(normalized) || sourceTitles.contains(normalized)) continue;
                results.putIfAbsent(doubanKey(subject), subject.toTmdbItem());
                if (results.size() >= MAX_RESULTS) return new ArrayList<>(results.values());
            }
        }

        // Douban suggest is search-oriented. If it does not return adjacent titles,
        // fall back to recent watched titles enriched with posters instead of showing nothing.
        if (results.isEmpty()) {
        for (String seed : historySeeds(currentTitle, MAX_DOUBAN_SEEDS, false)) {
                for (DoubanSubject subject : fetchDoubanSuggest(seed)) {
                    if (isBlank(subject.title) || normalizeTitle(subject.title).equals(normalizeTitle(currentTitle))) continue;
                    results.putIfAbsent(doubanKey(subject), subject.toTmdbItem());
                    break;
                }
                if (results.size() >= MAX_RESULTS) break;
            }
        }

        return new ArrayList<>(results.values());
    }

    private void addTmdbItems(LinkedHashMap<String, TmdbItem> results, Set<String> blockedTitles, List<TmdbItem> items, TmdbItem currentItem) {
        if (items == null || results.size() >= MAX_RESULTS) return;
        for (TmdbItem item : items) {
            if (item == null || isBlank(item.getTitle())) continue;
            if (sameTmdbItem(item, currentItem)) continue;
            if (blockedTitles.contains(normalizeTitle(item.getTitle()))) continue;
            results.putIfAbsent(tmdbKey(item), item);
            if (results.size() >= MAX_RESULTS) return;
        }
    }

    private List<String> doubanSeeds(String currentTitle) {
        List<String> seeds = new ArrayList<>();
        addSeed(seeds, currentTitle, MAX_DOUBAN_SEEDS);
        for (String seed : historySeeds(currentTitle, MAX_DOUBAN_SEEDS, false)) addSeed(seeds, seed, MAX_DOUBAN_SEEDS);
        return seeds;
    }

    private List<String> historySeeds(String currentTitle, int maxSeeds, boolean tmdbTarget) {
        List<String> seeds = new ArrayList<>();
        SourceClassifier classifier = sourceClassifier();
        for (History history : safeHistory()) {
            if (!shouldUseHistorySeed(history, tmdbTarget, classifier)) continue;
            addSeed(seeds, history == null ? "" : history.getVodName(), maxSeeds);
            if (seeds.size() >= maxSeeds) break;
        }
        String normalizedCurrent = normalizeTitle(currentTitle);
        seeds.removeIf(seed -> normalizeTitle(seed).equals(normalizedCurrent));
        return seeds;
    }

    private SourceClassifier sourceClassifier() {
        AudioConfig audioConfig = AudioConfig.objectFrom(Setting.getAudioConfig());
        ShortDramaConfig shortDramaConfig = ShortDramaConfig.objectFrom(Setting.getShortDramaConfig());
        return new SourceClassifier() {
            @Override
            public boolean isAudio(String siteKey, String siteName) {
                return audioConfig.isSiteEnabled(siteKey, siteName);
            }

            @Override
            public boolean isShortDrama(String siteKey, String siteName) {
                return shortDramaConfig.isSiteEnabled(siteKey, siteName);
            }
        };
    }

    private boolean shouldUseHistorySeed(History history, boolean tmdbTarget, SourceClassifier classifier) {
        if (history == null) return false;
        SourceInfo source = sourceInfo(history);
        return shouldUseHistorySeed(source.key, source.name, tmdbTarget, classifier);
    }

    static boolean shouldUseHistorySeed(String siteKey, String siteName, boolean tmdbTarget, SourceClassifier classifier) {
        if (classifier == null) return true;
        if (classifier.isAudio(siteKey, siteName)) return false;
        return !tmdbTarget || !classifier.isShortDrama(siteKey, siteName);
    }

    private SourceInfo sourceInfo(History history) {
        String siteKey = historySiteKey(history);
        return new SourceInfo(siteKey, siteName(siteKey));
    }

    private static String historySiteKey(History history) {
        String key = history == null ? "" : Objects.toString(history.getKey(), "");
        int index = key.indexOf(AppDatabase.SYMBOL);
        return index <= 0 ? key : key.substring(0, index);
    }

    private String siteName(String siteKey) {
        if (isBlank(siteKey)) return "";
        try {
            Site site = VodConfig.get().getSite(siteKey);
            return site == null ? "" : site.getName();
        } catch (Throwable e) {
            return "";
        }
    }

    private void addSeed(List<String> seeds, String title, int maxSeeds) {
        if (seeds.size() >= maxSeeds || isBlank(title)) return;
        String normalized = normalizeTitle(title);
        if (isBlank(normalized)) return;
        for (String seed : seeds) if (normalizeTitle(seed).equals(normalized)) return;
        seeds.add(title.trim());
    }

    private Set<String> blockedTitles(Vod currentVod) {
        Set<String> titles = new HashSet<>();
        if (currentVod != null) titles.add(normalizeTitle(currentVod.getName()));
        for (History history : safeHistory()) {
            if (history == null) continue;
            String title = normalizeTitle(history.getVodName());
            if (!isBlank(title)) titles.add(title);
        }
        return titles;
    }

    private List<History> safeHistory() {
        try {
            return History.get();
        } catch (Throwable e) {
            SpiderDebug.log("personal-rec", "history read failed: %s", e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<DoubanSubject> fetchDoubanSuggest(String keyword) {
        if (isBlank(keyword)) return new ArrayList<>();
        HttpUrl base = HttpUrl.parse(DOUBAN_SUGGEST_URL);
        if (base == null) return new ArrayList<>();
        HttpUrl url = base.newBuilder().addQueryParameter("q", keyword).build();
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", DOUBAN_UA)
                .header("Referer", DOUBAN_REFERER)
                .build();
        try (Response response = com.github.catvod.net.OkHttp.client().newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) return new ArrayList<>();
            return parseDoubanSubjects(response.body().string());
        } catch (Throwable e) {
            SpiderDebug.log("personal-rec", "Douban suggest failed title=%s error=%s", keyword, e.getMessage());
            return new ArrayList<>();
        }
    }

    static List<DoubanSubject> parseDoubanSubjects(String body) {
        List<DoubanSubject> subjects = new ArrayList<>();
        if (isBlank(body)) return subjects;
        try {
            JsonElement element = JsonParser.parseString(body);
            if (element == null || !element.isJsonArray()) return subjects;
            JsonArray array = element.getAsJsonArray();
            for (JsonElement item : array) {
                if (!item.isJsonObject()) continue;
                DoubanSubject subject = DoubanSubject.from(item.getAsJsonObject());
                if (subject != null) subjects.add(subject);
            }
        } catch (Throwable ignored) {
            return new ArrayList<>();
        }
        return subjects;
    }

    private static String currentTitle(Vod currentVod, TmdbItem currentItem) {
        if (currentItem != null && !isBlank(currentItem.getTitle())) return currentItem.getTitle();
        return currentVod == null ? "" : currentVod.getName();
    }

    private static boolean sameTmdbItem(TmdbItem first, TmdbItem second) {
        return first != null && second != null && first.getTmdbId() > 0 && first.getTmdbId() == second.getTmdbId() && Objects.equals(first.getMediaType(), second.getMediaType());
    }

    private static String tmdbKey(TmdbItem item) {
        if (item.getTmdbId() > 0) return item.getMediaType() + ":" + item.getTmdbId();
        return "title:" + normalizeTitle(item.getTitle());
    }

    private static String doubanKey(DoubanSubject subject) {
        return !isBlank(subject.id) ? "douban:" + subject.id : "douban-title:" + normalizeTitle(subject.title);
    }

    static String normalizeTitle(String text) {
        return Objects.toString(text, "").replaceAll("[\\s·•・._\\-/\\\\|()（）\\[\\]【】《》<>:：,，.。]+", "").trim().toLowerCase(Locale.ROOT);
    }

    private static boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }

    interface SourceClassifier {
        boolean isAudio(String siteKey, String siteName);

        boolean isShortDrama(String siteKey, String siteName);
    }

    private static final class SourceInfo {

        private final String key;
        private final String name;

        private SourceInfo(String key, String name) {
            this.key = Objects.toString(key, "");
            this.name = Objects.toString(name, "");
        }
    }

    public static final class Recommendations {

        private final List<TmdbItem> tmdb;
        private final List<TmdbItem> douban;

        Recommendations(List<TmdbItem> tmdb, List<TmdbItem> douban) {
            this.tmdb = tmdb == null ? new ArrayList<>() : tmdb;
            this.douban = douban == null ? new ArrayList<>() : douban;
        }

        static Recommendations empty() {
            return new Recommendations(new ArrayList<>(), new ArrayList<>());
        }

        public List<TmdbItem> getTmdb() {
            return tmdb;
        }

        public List<TmdbItem> getDouban() {
            return douban;
        }

        public boolean isEmpty() {
            return tmdb.isEmpty() && douban.isEmpty();
        }
    }

    static final class DoubanSubject {

        final String id;
        final String title;
        final String mediaType;
        final int year;
        final String posterUrl;

        private DoubanSubject(String id, String title, String mediaType, int year, String posterUrl) {
            this.id = nullToEmpty(id);
            this.title = nullToEmpty(title);
            this.mediaType = nullToEmpty(mediaType);
            this.year = year;
            this.posterUrl = nullToEmpty(posterUrl);
        }

        static DoubanSubject from(@NonNull JsonObject object) {
            String title = string(object, "title", "name");
            if (isBlank(title)) return null;
            String type = mediaType(string(object, "type"));
            int year = firstYear(string(object, "year", "sub_title"));
            String poster = highResPoster(string(object, "img"));
            return new DoubanSubject(string(object, "id"), title, type, year, poster);
        }

        TmdbItem toTmdbItem() {
            return new TmdbItem(doubanIntId(), mediaType, title, subtitle(), "", posterUrl, "", "", 0.0);
        }

        private int doubanIntId() {
            if (!isBlank(id)) {
                try {
                    long value = Long.parseLong(id);
                    if (value > 0 && value <= Integer.MAX_VALUE) return (int) -value;
                } catch (NumberFormatException ignored) {
                }
            }
            return -Math.abs((title + year).hashCode());
        }

        private String subtitle() {
            List<String> parts = new ArrayList<>();
            parts.add("tv".equals(mediaType) ? "剧集" : "电影");
            if (year > 0) parts.add(String.valueOf(year));
            return String.join(" · ", parts);
        }

        private static String mediaType(String value) {
            String type = nullToEmpty(value).toLowerCase(Locale.ROOT);
            if (type.contains("tv") || type.contains("series") || type.contains("电视剧") || type.contains("劇集")) return "tv";
            return "movie";
        }

        private static int firstYear(String text) {
            Matcher matcher = YEAR.matcher(Objects.toString(text, ""));
            return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
        }

        private static String highResPoster(String url) {
            String value = nullToEmpty(url);
            if (value.contains("s_ratio_poster")) return value.replace("s_ratio_poster", "m_ratio_poster");
            return value;
        }

        private static String string(JsonObject object, String... keys) {
            for (String key : keys) {
                if (object == null || !object.has(key) || object.get(key).isJsonNull()) continue;
                String value = object.get(key).getAsString();
                if (!isBlank(value)) return value.trim();
            }
            return "";
        }

        private static String nullToEmpty(String value) {
            return value == null ? "" : value.trim();
        }
    }
}
