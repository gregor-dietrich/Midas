package de.vptr.midas.gui.service;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import de.vptr.midas.gui.client.PageClient;
import de.vptr.midas.gui.dto.PageDto;
import de.vptr.midas.gui.dto.PageResponseDto;
import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.exception.ServiceException;
import de.vptr.midas.gui.util.PaginationParamUtil;
import de.vptr.midas.gui.util.ServiceHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

/** Component. */
@Timeout(value = 5, unit = ChronoUnit.SECONDS)
@ApplicationScoped
public class PageService {

    private static final Logger LOG = Logger.getLogger(PageService.class);

    @Inject
    @RestClient
    PageClient pageClient;

    @Inject
    AuthService authService;

    /** Returns the all pages. */
    @Retry(maxRetries = 2, delay = 500, delayUnit = ChronoUnit.MILLIS, abortOn = AuthenticationException.class)
    public List<PageResponseDto> getAllPages(final String authHeader) throws AuthenticationException, ServiceException {
        LOG.debug("Fetching all pages");
        if (authHeader == null) {
            LOG.warn("No authentication header available");
            throw new AuthenticationException("Authentication required");
        }
        return ServiceHelper.execute(
                () -> this.pageClient.getAllPages(PaginationParamUtil.DEFAULT_PAGE, PaginationParamUtil.MAX_SIZE,
                        authHeader),
                this.authService, LOG, "fetching pages");
    }

    /** Returns the page by id. */
    @Retry(maxRetries = 2, delay = 500, delayUnit = ChronoUnit.MILLIS, abortOn = AuthenticationException.class)
    public Optional<PageResponseDto> getPageById(final Long id) throws AuthenticationException, ServiceException {
        LOG.infof("Getting page by id: %s", id);
        if (!this.authService.isAuthenticated()) {
            LOG.warn("User is not authenticated");
            throw new AuthenticationException("Authentication required");
        }
        return ServiceHelper.executeWithResponse(() -> {
            final var authHeader = this.authService.getAuthorizationHeader();
            return this.pageClient.getPage(id, authHeader);
        }, this.authService, LOG, "getting page " + id, response -> {
            if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                final var page = ServiceHelper.readEntity(response, PageResponseDto.class, "page response");
                LOG.infof("Successfully retrieved page: %s", page.getTitle());
                return Optional.of(page);
            } else if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
                LOG.debugf("Page %s not found", id);
                return Optional.empty();
            } else {
                throw ServiceHelper.backendError(response);
            }
        });
    }

    /** Creates a new page. */
    public PageDto createPage(final PageDto page) throws AuthenticationException, ServiceException {
        LOG.infof("Creating page: %s", page.getTitle());
        if (!this.authService.isAuthenticated()) {
            throw new AuthenticationException("Authentication required");
        }
        return ServiceHelper.executeWithResponse(() -> {
            final var authHeader = this.authService.getAuthorizationHeader();
            return this.pageClient.createPage(page, authHeader);
        }, this.authService, LOG, "creating page", "Failed to create page", response -> {
            if (response.getStatus() == Response.Status.CREATED.getStatusCode()) {
                final var responseDto = ServiceHelper.readEntity(response, PageResponseDto.class,
                        "created page response");
                final var createdPage = responseDto.toPageDto();
                LOG.infof("Successfully created page: %s", createdPage.getTitle());
                return createdPage;
            }
            throw ServiceHelper.responseFailure("Failed to create page", response);
        });
    }

    /** Updates the page. */
    public PageDto updatePage(final PageDto page) throws AuthenticationException, ServiceException {
        LOG.infof("Updating page: %s (%s)", page.getTitle(), page.getId());
        if (!this.authService.isAuthenticated()) {
            throw new AuthenticationException("Authentication required");
        }
        return ServiceHelper.executeWithResponse(() -> {
            final var authHeader = this.authService.getAuthorizationHeader();
            return this.pageClient.updatePage(page.getId(), page, authHeader);
        }, this.authService, LOG, "updating page " + page.getId(), "Failed to update page", response -> {
            if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                final var responseDto = ServiceHelper.readEntity(response, PageResponseDto.class,
                        "updated page response");
                final var updatedPage = responseDto.toPageDto();
                LOG.infof("Successfully updated page: %s", updatedPage.getTitle());
                return updatedPage;
            }
            throw ServiceHelper.responseFailure("Failed to update page", response);
        });
    }

    /** Handles the delete page operation. */
    public boolean deletePage(final Long id) throws AuthenticationException, ServiceException {
        LOG.infof("Deleting page: %s", id);
        if (!this.authService.isAuthenticated()) {
            throw new AuthenticationException("Authentication required");
        }
        return ServiceHelper.executeWithResponse(() -> {
            final var authHeader = this.authService.getAuthorizationHeader();
            return this.pageClient.deletePage(id, authHeader);
        }, this.authService, LOG, "deleting page " + id, response -> {
            if (response.getStatus() == Response.Status.NO_CONTENT.getStatusCode()
                    || response.getStatus() == Response.Status.OK.getStatusCode()) {
                LOG.infof("Successfully deleted page: %s", id);
                return true;
            } else {
                return false;
            }
        });
    }

    /** Handles the search content operation. */
    @Retry(maxRetries = 2, delay = 500, delayUnit = ChronoUnit.MILLIS, abortOn = AuthenticationException.class)
    public List<PageResponseDto> searchContent(final String searchTerm) {
        LOG.debugf("Searching page content: %s", searchTerm);
        return ServiceHelper.execute(() -> {
            final var authHeader = this.authService.getAuthorizationHeader();
            if (authHeader == null) {
                LOG.warn("No authentication header available");
                throw new AuthenticationException("Authentication required");
            }
            return this.pageClient.searchPages(searchTerm, authHeader);
        }, this.authService, LOG, "searching page content");
    }

    /** Handles the search pages operation. */
    @Retry(maxRetries = 2, delay = 500, delayUnit = ChronoUnit.MILLIS, abortOn = AuthenticationException.class)
    public List<PageResponseDto> searchPages(final String query, final String authHeader) {
        LOG.debugf("Searching pages with query: %s", query);
        if (authHeader == null) {
            LOG.warn("No authentication header available");
            throw new AuthenticationException("Authentication required");
        }
        return ServiceHelper.execute(
                () -> this.pageClient.searchPages(query, authHeader),
                this.authService, LOG, "searching pages");
    }
}
