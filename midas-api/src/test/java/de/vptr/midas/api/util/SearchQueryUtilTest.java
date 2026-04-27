package de.vptr.midas.api.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SearchQueryUtilTest {

    @Test
    void toContainsLikePatternTrimsAndNormalizesCase() {
        assertEquals("%mixed query%", SearchQueryUtil.toContainsLikePattern("  MiXeD Query  "));
    }

    @Test
    void toContainsLikePatternEscapesLikeMetacharactersAndBackslashes() {
        assertEquals("%50\\%\\_\\\\value%", SearchQueryUtil.toContainsLikePattern("50%_\\value"));
    }
}
