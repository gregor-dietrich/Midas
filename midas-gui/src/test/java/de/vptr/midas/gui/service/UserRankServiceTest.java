package de.vptr.midas.gui.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.vaadin.flow.server.VaadinSession;

import de.vptr.midas.gui.client.UserRankClient;
import de.vptr.midas.gui.dto.UserRankDto;
import de.vptr.midas.gui.util.AbstractSessionServiceTestBase;
import de.vptr.midas.gui.util.MockDataUtil;
import jakarta.ws.rs.WebApplicationException;

/** Covers the session cache around {@link UserRankService#getCurrentUserRank()}; HTTP behaviour is pinned elsewhere. */
@SuppressWarnings("NullAway")
class UserRankServiceTest extends AbstractSessionServiceTestBase {

    private static final String RANK = "current.user.rank";
    private static final String RANK_TIMESTAMP = "current.user.rank.timestamp";

    @Mock
    @RestClient
    UserRankClient userRankClient;

    @Mock
    AuthService authService;

    @Mock
    UserService userService;

    @InjectMocks
    UserRankService userRankService;

    private void givenCurrentUserWithRank(final String rankPublicId) {
        when(this.userService.getCurrentUser()).thenReturn(Optional.of(
                MockDataUtil.createUserResponseDto("1", "testuser", "test@example.com").rankPublicId(rankPublicId)));
    }

    @Test
    void getCurrentUserRank_returnsCachedRank_whileCacheIsValid() {
        final var cachedRank = MockDataUtil.createUserRankDto();
        when(this.vaadinSession.getAttribute(RANK)).thenReturn(cachedRank);
        when(this.vaadinSession.getAttribute(RANK_TIMESTAMP)).thenReturn(System.currentTimeMillis());

        assertThat(this.userRankService.getCurrentUserRank()).isSameAs(cachedRank);

        verifyNoInteractions(this.userService, this.userRankClient);
    }

    @Test
    void getCurrentUserRank_clearsCorruptedCache_andFetchesAgain() {
        when(this.vaadinSession.getAttribute(RANK)).thenReturn("corrupted");
        when(this.vaadinSession.getAttribute(RANK_TIMESTAMP)).thenReturn("corrupted");
        when(this.userService.getCurrentUser()).thenReturn(Optional.empty());

        assertThat(this.userRankService.getCurrentUserRank().getPublicId()).isNull();

        verify(this.vaadinSession).setAttribute(RANK, null);
        verify(this.vaadinSession).setAttribute(RANK_TIMESTAMP, null);
    }

    @Test
    void getCurrentUserRank_replacesExpiredCache_withMappedFreshRank() {
        when(this.vaadinSession.getAttribute(RANK)).thenReturn(MockDataUtil.createUserRankDto());
        when(this.vaadinSession.getAttribute(RANK_TIMESTAMP)).thenReturn(System.currentTimeMillis() - 10 * 60 * 1000L);
        givenCurrentUserWithRank("fresh-rank");
        when(this.userRankClient.getRank("fresh-rank"))
                .thenReturn(MockDataUtil.createUserRankResponseDto().publicId("fresh-rank").adminView(true));

        final var result = this.userRankService.getCurrentUserRank();

        assertThat(result.getPublicId()).isEqualTo("fresh-rank");
        assertThat(result.canAdminView()).isTrue();
        assertThat(result.canDeleteUser()).isFalse();
        final ArgumentCaptor<Object> cached = ArgumentCaptor.forClass(Object.class);
        verify(this.vaadinSession, times(2)).setAttribute(eq(RANK), cached.capture());
        assertThat(cached.getAllValues()).containsExactly(null, result);
        verify(this.vaadinSession).setAttribute(eq(RANK_TIMESTAMP), anyLong());
    }

    @Test
    void getCurrentUserRank_cachesFreshRank_whenCacheIsEmpty() {
        givenCurrentUserWithRank("1");
        when(this.userRankClient.getRank("1")).thenReturn(MockDataUtil.createUserRankResponseDto());

        final var result = this.userRankService.getCurrentUserRank();

        verify(this.vaadinSession).setAttribute(RANK, result);
        verify(this.vaadinSession, never()).setAttribute(RANK, null);
    }

    @Test
    void getCurrentUserRank_discardsCachedRank_withoutTimestamp() {
        when(this.vaadinSession.getAttribute(RANK)).thenReturn(MockDataUtil.createUserRankDto());
        when(this.userService.getCurrentUser()).thenReturn(Optional.empty());

        assertThat(this.userRankService.getCurrentUserRank().getName()).isNull();

        verify(this.vaadinSession).setAttribute(RANK, null);
    }

    @Test
    void getCurrentUserRank_fetchesWithoutCaching_whenThereIsNoSession() {
        this.vaadinSessionMock.when(VaadinSession::getCurrent).thenReturn(null);
        givenCurrentUserWithRank("1");
        when(this.userRankClient.getRank("1")).thenReturn(MockDataUtil.createUserRankResponseDto());

        assertThat(this.userRankService.getCurrentUserRank().canAddUser()).isTrue();
    }

    @Test
    void getCurrentUserRank_returnsEmptyRank_whenUserHasNoRank() {
        when(this.userService.getCurrentUser()).thenReturn(Optional
                .of(MockDataUtil.createUserResponseDto("1", "testuser", "test@example.com").rankPublicId(null)));

        assertThat(this.userRankService.getCurrentUserRank()).isInstanceOf(UserRankDto.class)
                .extracting(UserRankDto::getPublicId).isNull();
        verifyNoInteractions(this.userRankClient);
    }

    @Test
    void getCurrentUserRank_returnsEmptyRank_whenRankLookupFails() {
        givenCurrentUserWithRank("1");
        when(this.userRankClient.getRank("1")).thenThrow(new WebApplicationException(500));

        assertThat(this.userRankService.getCurrentUserRank().hasAnyPermission()).isFalse();
    }

    @Test
    void clearCachedUserRank_removesCachedValues() {
        this.userRankService.clearCachedUserRank();

        verify(this.vaadinSession).setAttribute(RANK, null);
        verify(this.vaadinSession).setAttribute(RANK_TIMESTAMP, null);
    }

    @Test
    void clearCachedUserRank_toleratesMissingSession() {
        this.vaadinSessionMock.when(VaadinSession::getCurrent).thenReturn(null);

        this.userRankService.clearCachedUserRank();

        verifyNoInteractions(this.vaadinSession);
    }
}
