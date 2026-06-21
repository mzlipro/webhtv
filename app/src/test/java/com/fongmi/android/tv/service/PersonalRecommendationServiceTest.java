package com.fongmi.android.tv.service;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PersonalRecommendationServiceTest {

    @Test
    public void parseDoubanSubjects_readsStructuredSuggestItems() {
        String body = "[{\"id\":\"1291843\",\"title\":\"The Matrix\",\"type\":\"movie\",\"year\":\"1999\",\"img\":\"https://img1.doubanio.com/view/photo/s_ratio_poster/public/p451926968.jpg\"}]";

        List<PersonalRecommendationService.DoubanSubject> subjects = PersonalRecommendationService.parseDoubanSubjects(body);

        assertEquals(1, subjects.size());
        PersonalRecommendationService.DoubanSubject subject = subjects.get(0);
        assertEquals("1291843", subject.id);
        assertEquals("The Matrix", subject.title);
        assertEquals("movie", subject.mediaType);
        assertEquals(1999, subject.year);
        assertTrue(subject.posterUrl.contains("m_ratio_poster"));
    }

    @Test
    public void normalizeTitle_removesCommonSeparators() {
        assertEquals("thematrix1999", PersonalRecommendationService.normalizeTitle("The Matrix (1999)"));
    }

    @Test
    public void shouldUseHistorySeed_filtersAudioSourcesForTmdbAndDouban() {
        PersonalRecommendationService.SourceClassifier classifier = new PersonalRecommendationService.SourceClassifier() {
            @Override
            public boolean isAudio(String siteKey, String siteName) {
                return true;
            }

            @Override
            public boolean isShortDrama(String siteKey, String siteName) {
                return false;
            }
        };

        assertFalse(PersonalRecommendationService.shouldUseHistorySeed("fm", "凤凰FM[听]", false, classifier));
        assertFalse(PersonalRecommendationService.shouldUseHistorySeed("fm", "凤凰FM[听]", true, classifier));
    }

    @Test
    public void shouldUseHistorySeed_filtersShortDramaSourcesOnlyForTmdb() {
        PersonalRecommendationService.SourceClassifier classifier = new PersonalRecommendationService.SourceClassifier() {
            @Override
            public boolean isAudio(String siteKey, String siteName) {
                return false;
            }

            @Override
            public boolean isShortDrama(String siteKey, String siteName) {
                return true;
            }
        };

        assertTrue(PersonalRecommendationService.shouldUseHistorySeed("mini", "荐片[APP]", false, classifier));
        assertFalse(PersonalRecommendationService.shouldUseHistorySeed("mini", "荐片[APP]", true, classifier));
    }
}
