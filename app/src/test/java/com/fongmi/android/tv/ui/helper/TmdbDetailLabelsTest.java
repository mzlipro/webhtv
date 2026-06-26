package com.fongmi.android.tv.ui.helper;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TmdbDetailLabelsTest {

    @Test
    public void certificationLabel_appendsPlusToNumericAgeRatings() {
        assertEquals("12+", TmdbDetailLabels.certificationLabel("12"));
        assertEquals("16+", TmdbDetailLabels.certificationLabel(" 16 "));
    }

    @Test
    public void certificationLabel_preservesNamedRatings() {
        assertEquals("TV-14", TmdbDetailLabels.certificationLabel("TV-14"));
        assertEquals("PG-13", TmdbDetailLabels.certificationLabel("PG-13"));
        assertEquals("", TmdbDetailLabels.certificationLabel(null));
    }

    @Test
    public void headerSubtitle_usesOnlyReleaseDate() {
        assertEquals("2026-06-15", TmdbDetailLabels.headerSubtitle("2026-06-15"));
        assertEquals("", TmdbDetailLabels.headerSubtitle(null));
    }
}
