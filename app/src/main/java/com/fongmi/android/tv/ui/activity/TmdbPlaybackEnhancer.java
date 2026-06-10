package com.fongmi.android.tv.ui.activity;

import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.fongmi.android.tv.bean.TmdbConfig;
import com.fongmi.android.tv.bean.TmdbItem;
import com.fongmi.android.tv.bean.TmdbMatchCache;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.service.TmdbService;
import com.fongmi.android.tv.setting.Setting;
import com.fongmi.android.tv.utils.Task;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TmdbPlaybackEnhancer {

    private static final Pattern SOURCE_SEASON = Pattern.compile("(?i)(?:第\\s*([零〇一二三四五六七八九十两0-9]+)\\s*[季部]|season\\s*([0-9]{1,2})|s([0-9]{1,2})(?:[-._\\s]*e[0-9]{1,3})?)");

    public interface Host {
        String getKey();

        String getId();

        String getName();

        void runOnUiThread(Runnable action);

        void applyTmdbVod(Vod vod);

        default void applyTmdbArtwork(String title, String subtitle, String overview, String poster, String backdrop) {
        }
    }

    private final TmdbService tmdbService = new TmdbService();
    private final TmdbConfig tmdbConfig;
    private final Host host;
    private boolean applied;

    public TmdbPlaybackEnhancer(Host host) {
        this.host = host;
        this.tmdbConfig = TmdbConfig.objectFrom(Setting.getTmdbConfig());
    }

    public void onDetailReady(Vod item) {
        if (applied || item == null || !tmdbConfig.isReady()) return;
        applied = true;
        Task.execute(() -> {
            TmdbUpdate update = loadTmdbUpdate(item);
            if (update != null) host.runOnUiThread(() -> {
                host.applyTmdbVod(update.apply(item));
                host.applyTmdbArtwork(update.title, update.subtitle, update.overview, update.poster, update.backdrop);
            });
        });
    }

    @Nullable
    private TmdbUpdate loadTmdbUpdate(Vod vod) {
        try {
            TmdbItem item = getCachedTmdbMatch();
            if (item == null) {
                String title = searchTitle(vod);
                String query = cleanTmdbSearchQuery(title);
                int year = sourceYear(vod, title);
                List<TmdbItem> items = tmdbService.search(query, tmdbConfig);
                item = chooseTmdbMatch(items, query, year, title, vod);
                if (item == null) {
                    SplitYearQuery split = splitYearQuery(title, vod);
                    if (split != null) item = chooseTmdbMatch(tmdbService.search(split.query, tmdbConfig), split.query, split.year, title, vod);
                }
                if (item != null) saveTmdbMatch(item);
            }
            if (item == null) return null;
            JsonObject detail = tmdbService.detail(item, tmdbConfig);
            return TmdbUpdate.from(tmdbService, tmdbConfig, item, detail);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private TmdbItem getCachedTmdbMatch() {
        return Setting.getTmdbMatchCache().find(host.getKey(), host.getId());
    }

    private void saveTmdbMatch(TmdbItem item) {
        if (item == null || item.getTmdbId() <= 0) return;
        TmdbMatchCache cache = Setting.getTmdbMatchCache();
        cache.put(host.getKey(), host.getId(), item);
        Setting.putTmdbMatchCache(cache);
    }

    @Nullable
    private TmdbItem chooseTmdbMatch(List<TmdbItem> items, String title, int year, String sourceTitle, Vod vod) {
        if (items == null || items.isEmpty()) return null;
        String key = normalize(title);
        int season = sourceSeasonNumber(sourceTitle, vod);
        for (TmdbItem item : items) if (normalize(item.getTitle()).equals(key) && (yearMatches(item, year) || tmdbSeasonYearMatches(item, season, year))) return item;
        if (year > 0) return null;
        for (TmdbItem item : items) if (normalize(item.getTitle()).contains(key) || key.contains(normalize(item.getTitle()))) return item;
        return items.get(0);
    }

    private String searchTitle(Vod vod) {
        if (vod != null && !TextUtils.isEmpty(vod.getName())) return vod.getName();
        return host.getName();
    }

    private String normalize(String text) {
        return Objects.toString(text, "").replaceAll("[\\s·•:：\\-_/\\\\|()（）\\[\\]【】]+", "").trim().toLowerCase(Locale.ROOT);
    }

    private String cleanTmdbSearchQuery(String text) {
        String raw = Objects.toString(text, "").trim();
        if (TextUtils.isEmpty(raw)) return "";
        String cleaned = raw;
        cleaned = cleaned.replaceAll("(?i)\\.(mkv|mp4|avi|mov|wmv|flv|rmvb|ts|m2ts)$", "");
        cleaned = cleaned.replaceAll("(?i)\\b(S\\d{1,2}|Season\\s*\\d{1,2})\\b", " ");
        cleaned = cleaned.replaceAll("第\\s*[一二三四五六七八九十百零〇两0-9]+\\s*[季部]", " ");
        cleaned = cleaned.replaceAll("第\\s*[一二三四五六七八九十百零〇两0-9]+\\s*[集话話]", " ");
        cleaned = cleaned.replaceAll("[._\\-+]+", " ");
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        cleaned = cleaned.replaceAll("^[\\s:：,，.。·|/\\\\]+|[\\s:：,，.。·|/\\\\]+$", "");
        return TextUtils.isEmpty(cleaned) ? raw : cleaned;
    }

    private boolean yearMatches(TmdbItem item, int year) {
        if (year <= 0) return true;
        return tmdbItemYear(item) == year;
    }

    private int sourceYear(Vod vod, String title) {
        int year = vod == null ? 0 : firstYear(vod.getYear());
        if (year > 0) return year;
        year = vod == null ? 0 : firstYear(vod.getName());
        if (year > 0) return year;
        year = firstYear(host.getName());
        return year > 0 ? year : firstYear(title);
    }

    private int sourceSeasonNumber(String title, Vod vod) {
        int number = vod == null ? -1 : sourceSeasonNumber(vod.getName());
        if (number > 0) return number;
        number = sourceSeasonNumber(host.getName());
        return number > 0 ? number : sourceSeasonNumber(title);
    }

    private int sourceSeasonNumber(String text) {
        if (TextUtils.isEmpty(text)) return -1;
        Matcher matcher = SOURCE_SEASON.matcher(text);
        while (matcher.find()) {
            int number = normalizeSourceNumber(firstNonEmptyGroup(matcher, 1, 2, 3));
            if (number > 0) return number;
        }
        return -1;
    }

    @Nullable
    private SplitYearQuery splitYearQuery(String title, Vod vod) {
        int year = sourceYear(vod, title);
        if (year <= 0) return null;
        String source = !TextUtils.isEmpty(title) && firstYear(title) == year ? title : vod == null ? "" : vod.getName();
        if (firstYear(source) != year) source = host.getName();
        if (firstYear(source) != year) return null;
        String query = removeYearFromTitle(source, year);
        if (TextUtils.isEmpty(query) || normalize(query).equals(normalize(source))) return null;
        return new SplitYearQuery(query, year);
    }

    private String removeYearFromTitle(String text, int year) {
        String cleaned = Objects.toString(text, "").replaceAll("(?<!\\d)" + year + "(?!\\d)", " ");
        cleaned = cleaned.replaceAll("[\\[【「『(（]\\s*[\\]】」』)）]", " ");
        cleaned = cleaned.replaceAll("[._\\-+]+", " ");
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        cleaned = cleaned.replaceAll("^[\\s:：,，.。·|/\\\\]+|[\\s:：,，.。·|/\\\\]+$", "");
        return cleanTmdbSearchQuery(cleaned);
    }

    private int tmdbItemYear(TmdbItem item) {
        int year = firstYear(item.getSubtitle());
        return year > 0 ? year : firstYear(item.getTitle());
    }

    private boolean tmdbSeasonYearMatches(TmdbItem item, int seasonNumber, int sourceYear) {
        if (item == null || seasonNumber <= 0 || sourceYear <= 0 || !"tv".equalsIgnoreCase(item.getMediaType())) return false;
        try {
            JsonObject detail = tmdbService.detail(item, tmdbConfig);
            return tmdbSeasonYear(detail, seasonNumber) == sourceYear;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private int tmdbSeasonYear(JsonObject detail, int seasonNumber) {
        for (JsonElement element : array(detail, "seasons")) {
            if (!element.isJsonObject()) continue;
            JsonObject object = element.getAsJsonObject();
            if (!object.has("season_number") || object.get("season_number").isJsonNull()) continue;
            if (object.get("season_number").getAsInt() != seasonNumber) continue;
            int year = firstYear(string(object, "air_date"));
            return year > 0 ? year : firstYear(string(object, "name"));
        }
        return 0;
    }

    private int firstYear(String text) {
        Matcher matcher = Pattern.compile("(19\\d{2}|20\\d{2})").matcher(Objects.toString(text, ""));
        while (matcher.find()) {
            int year = Integer.parseInt(matcher.group(1));
            if (year >= 1900 && year <= 2099) return year;
        }
        return 0;
    }

    private String firstNonEmptyGroup(Matcher matcher, int... groups) {
        if (matcher == null || groups == null) return "";
        for (int group : groups) {
            String value = matcher.group(group);
            if (!TextUtils.isEmpty(value)) return value;
        }
        return "";
    }

    private int normalizeSourceNumber(String value) {
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

    private int parseSmallChineseNumber(String value) {
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

    private int chineseDigit(char value) {
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

    private JsonArray array(JsonObject object, String... keys) {
        JsonElement current = object;
        for (String key : keys) {
            if (current == null || !current.isJsonObject()) return new JsonArray();
            JsonObject currentObject = current.getAsJsonObject();
            if (!currentObject.has(key) || currentObject.get(key).isJsonNull()) return new JsonArray();
            current = currentObject.get(key);
        }
        return current != null && current.isJsonArray() ? current.getAsJsonArray() : new JsonArray();
    }

    private String string(JsonObject object, String... keys) {
        for (String key : keys) {
            if (object != null && object.has(key) && !object.get(key).isJsonNull()) {
                String value = object.get(key).getAsString();
                if (!TextUtils.isEmpty(value)) return value.trim();
            }
        }
        return "";
    }

    private static class SplitYearQuery {

        private final String query;
        private final int year;

        private SplitYearQuery(String query, int year) {
            this.query = query;
            this.year = year;
        }
    }

    private static class TmdbUpdate {

        private final String title;
        private final String overview;
        private final String poster;
        private final String backdrop;
        private final String subtitle;
        private final String director;
        private final String actor;
        private final String year;
        private final String area;
        private final String type;

        private TmdbUpdate(String title, String overview, String poster, String backdrop, String subtitle, String director, String actor, String year, String area, String type) {
            this.title = title;
            this.overview = overview;
            this.poster = poster;
            this.backdrop = backdrop;
            this.subtitle = subtitle;
            this.director = director;
            this.actor = actor;
            this.year = year;
            this.area = area;
            this.type = type;
        }

        static TmdbUpdate from(TmdbService service, TmdbConfig config, TmdbItem item, JsonObject detail) {
            String poster = coalesce(item.getPosterUrl(), service.image(config.getImageBase(), string(detail, "poster_path")));
            String backdrop = coalesce(item.getBackdropUrl(), service.image(config.getBackdropBase(), string(detail, "backdrop_path")));
            String year = dateYear(string(detail, "release_date", "first_air_date"));
            String area = firstCountry(detail);
            String type = firstGenre(detail);
            String subtitle = subtitle(item, detail, year, area, type);
            return new TmdbUpdate(
                    coalesce(string(detail, "title", "name"), item.getTitle()),
                    coalesce(string(detail, "overview"), item.getOverview()),
                    poster,
                    backdrop,
                    subtitle,
                    firstCrew(detail, "Director"),
                    castNames(detail),
                    year,
                    area,
                    type);
        }

        Vod apply(Vod vod) {
            Vod update = new Vod();
            update.setId(vod.getId());
            update.setPlayFrom(vod.getPlayFrom());
            update.setPlayUrl(vod.getPlayUrl());
            update.setFlags(vod.getFlags());
            update.setSite(vod.getSite());
            update.setName(vod.getName());
            update.setPic(vod.getPic());
            update.setContent(vod.getContent());
            update.setDirector(vod.getDirector());
            update.setYear(vod.getYear());
            update.setArea(vod.getArea());
            update.setTypeName(vod.getTypeName());
            update.setRemarks(vod.getRemarks());
            if (!TextUtils.isEmpty(title)) update.setName(title);
            if (!TextUtils.isEmpty(overview)) update.setContent(overview);
            if (!TextUtils.isEmpty(coalesce(backdrop, poster))) update.setPic(coalesce(backdrop, poster));
            if (!TextUtils.isEmpty(director)) update.setDirector(director);
            if (!TextUtils.isEmpty(actor)) update.setActor(actor);
            if (!TextUtils.isEmpty(year)) update.setYear(year);
            if (!TextUtils.isEmpty(area)) update.setArea(area);
            if (!TextUtils.isEmpty(type)) update.setTypeName(type);
            return update;
        }

        private static String castNames(JsonObject detail) {
            StringBuilder builder = new StringBuilder();
            int count = 0;
            for (JsonElement element : array(detail, "credits", "cast")) {
                if (!element.isJsonObject()) continue;
                String name = string(element.getAsJsonObject(), "name");
                if (TextUtils.isEmpty(name) || builder.toString().contains(name)) continue;
                if (builder.length() > 0) builder.append(" / ");
                builder.append(name);
                if (++count >= 8) break;
            }
            return builder.toString();
        }

        private static String firstCrew(JsonObject detail, String job) {
            JsonArray crew = array(detail, "credits", "crew");
            for (JsonElement element : crew) {
                if (!element.isJsonObject()) continue;
                JsonObject object = element.getAsJsonObject();
                if (job.equalsIgnoreCase(string(object, "job"))) return string(object, "name");
            }
            return "";
        }

        private static String firstGenre(JsonObject detail) {
            JsonArray genres = array(detail, "genres");
            for (JsonElement element : genres) if (element.isJsonObject()) return string(element.getAsJsonObject(), "name");
            return "";
        }

        private static String firstCountry(JsonObject detail) {
            JsonArray countries = array(detail, "production_countries");
            for (JsonElement element : countries) if (element.isJsonObject()) return string(element.getAsJsonObject(), "name", "iso_3166_1");
            return "";
        }

        private static String dateYear(String date) {
            if (TextUtils.isEmpty(date) || date.length() < 4) return "";
            return date.substring(0, 4);
        }

        private static String subtitle(TmdbItem item, JsonObject detail, String year, String area, String type) {
            String media = "tv".equalsIgnoreCase(item.getMediaType()) ? "TV" : "Movie";
            String rating = "";
            if (detail != null && detail.has("vote_average") && !detail.get("vote_average").isJsonNull()) {
                rating = String.format(Locale.US, "%.1f", detail.get("vote_average").getAsDouble());
            }
            StringBuilder builder = new StringBuilder(media);
            if (!TextUtils.isEmpty(year)) builder.append(" · ").append(year);
            if (!TextUtils.isEmpty(rating)) builder.append(" · Rating ").append(rating);
            if (!TextUtils.isEmpty(area)) builder.append(" · ").append(area);
            if (!TextUtils.isEmpty(type)) builder.append(" · ").append(type);
            return builder.toString();
        }

        private static JsonArray array(JsonObject object, String... keys) {
            JsonElement current = object;
            for (String key : keys) {
                if (current == null || !current.isJsonObject()) return new JsonArray();
                JsonObject currentObject = current.getAsJsonObject();
                if (!currentObject.has(key) || currentObject.get(key).isJsonNull()) return new JsonArray();
                current = currentObject.get(key);
            }
            return current != null && current.isJsonArray() ? current.getAsJsonArray() : new JsonArray();
        }

        private static String string(JsonObject object, String... keys) {
            for (String key : keys) {
                if (object != null && object.has(key) && !object.get(key).isJsonNull()) {
                    String value = object.get(key).getAsString();
                    if (!TextUtils.isEmpty(value)) return value.trim();
                }
            }
            return "";
        }

        private static String coalesce(String... values) {
            for (String value : values) if (!TextUtils.isEmpty(value)) return value;
            return "";
        }
    }
}
