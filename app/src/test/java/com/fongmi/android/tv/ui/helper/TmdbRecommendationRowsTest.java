package com.fongmi.android.tv.ui.helper;

import com.fongmi.android.tv.bean.TmdbItem;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class TmdbRecommendationRowsTest {

    @Test
    public void rankedRelated_mergesRecommendationsAndSimilarWithContextRanking() {
        JsonObject detail = JsonParser.parseString("{"
                + "\"genres\":[{\"id\":9648,\"name\":\"悬疑\"}],"
                + "\"original_language\":\"ko\","
                + "\"origin_country\":[\"KR\"]"
                + "}").getAsJsonObject();
        TmdbItem highRated = item(1, "High Rated", 9.6, "en", "US", List.of(35));
        TmdbItem contextual = item(2, "Contextual Similar", 7.0, "ko", "KR", List.of(9648));

        List<TmdbItem> ranked = TmdbRecommendationRows.rankedRelated(detail, List.of(highRated), List.of(contextual));

        assertEquals(List.of("Contextual Similar", "High Rated"), titles(ranked));
    }

    @Test
    public void personalRows_removeDuplicatesAgainstRelatedAndProviderRows() {
        TmdbItem related = item(1, "重叠作品");
        TmdbItem tmdbOnly = item(2, "TMDB 个性");
        TmdbItem doubanOnly = item(-3, "豆瓣个性");

        List<TmdbItem> personalTmdb = TmdbRecommendationRows.personalTmdb(List.of(related, tmdbOnly), List.of(related));
        List<TmdbItem> personalDouban = TmdbRecommendationRows.personalDouban(List.of(tmdbOnly, doubanOnly), List.of(related), personalTmdb);

        assertEquals(List.of("TMDB 个性"), titles(personalTmdb));
        assertEquals(List.of("豆瓣个性"), titles(personalDouban));
    }

    @Test
    public void personalTmdb_keepsRowWhenEveryItemAlsoExistsInRelated() {
        TmdbItem related = item(1, "重叠作品");

        List<TmdbItem> personalTmdb = TmdbRecommendationRows.personalTmdb(List.of(related), List.of(related));

        assertEquals(List.of("重叠作品"), titles(personalTmdb));
    }

    private static TmdbItem item(int id, String title) {
        return item(id, title, 0.0, "", "", new ArrayList<>());
    }

    private static TmdbItem item(int id, String title, double rating, String language, String country, List<Integer> genreIds) {
        return new TmdbItem(id, "movie", title, "", "", "", "", "", rating, language, country, genreIds);
    }

    private static List<String> titles(List<TmdbItem> items) {
        return items.stream().map(TmdbItem::getTitle).toList();
    }
}
