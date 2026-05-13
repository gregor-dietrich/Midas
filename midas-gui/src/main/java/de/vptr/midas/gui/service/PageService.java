package de.vptr.midas.gui.service;

import java.util.List;
import java.util.Optional;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import de.vptr.midas.gui.client.PageClient;
import de.vptr.midas.gui.dto.PageDto;
import de.vptr.midas.gui.dto.PageResponseDto;
import de.vptr.midas.gui.exception.ServiceException;
import de.vptr.midas.gui.util.PaginationParamUtil;
import de.vptr.midas.gui.util.ServiceHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

/** Service for page operations. */
@ApplicationScoped
public class PageService {

    private static final Logger LOG = Logger.getLogger(PageService.class);

    @Inject
    @RestClient
    PageClient pageClient;

    @Inject
    AuthService authService;

    /** Returns the all pages. */
    public List<PageResponseDto> getAllPages() {
        LOG.debug("Fetching all pages");
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.execute(() -> this.pageClient.getAllPages(PaginationParamUtil.DEFAULT_PAGE,
                PaginationParamUtil.MAX_SIZE, authHeader), this.authService, LOG, "fetching pages");
    }

    /** Returns the all pages paginated. */
    public List<PageResponseDto> getAllPages(final Integer page, final Integer size) {
        LOG.debugf("Fetching all pages page %s size %s", page, size);
        final var authHeader = this.authService.requireAuthorizationHeader();
        final Integer safePage = PaginationParamUtil.sanitizePage(page);
        final Integer safeSize = PaginationParamUtil.sanitizeSize(size);
        return ServiceHelper.execute(() -> this.pageClient.getAllPages(safePage, safeSize, authHeader),
                this.authService, LOG, "fetching pages");
    }

    /** Returns the page by id. */
    public Optional<PageResponseDto> getPageById(final String id) {
        LOG.infof("Getting page by id: %s", id);
        return ServiceHelper.executeWithResponse(() -> {
            final var authHeader = this.authService.requireAuthorizationHeader();
            return this.pageClient.getPage(id, authHeader);
        }, this.authService, LOG, "getting page " + id, response -> ServiceHelper.readOptionalEntity(response,
                PageResponseDto.class, dto -> dto, "page response"));
    }

    /** Creates a new page. */
    public PageDto createPage(final PageDto page) {
        LOG.infof("Creating page: %s", page.getTitle());
        return ServiceHelper.executeWithResponse(() -> {
            final var authHeader = this.authService.requireAuthorizationHeader();
            return this.pageClient.createPage(page, authHeader);
        }, this.authService, LOG, "creating page", "Failed to create page",
                response -> ServiceHelper.readExpectedStatusEntity(response, 201, PageResponseDto.class,
                        PageResponseDto::toPageDto, "created page response", "Failed to create page"));
    }

    /** Updates the page. */
    public PageDto updatePage(final PageDto page) {
        LOG.infof("Updating page: %s (%s)", page.getTitle(), page.getPublicId());
        return ServiceHelper.executeWithResponse(() -> {
            final var authHeader = this.authService.requireAuthorizationHeader();
            return this.pageClient.updatePage(page.getPublicId(), page, authHeader);
        }, this.authService, LOG, "updating page " + page.getPublicId(), "Failed to update page",
                response -> ServiceHelper.readExpectedStatusEntity(response, 200, PageResponseDto.class,
                        PageResponseDto::toPageDto, "updated page response", "Failed to update page"));
    }

    /** Handles the delete page operation. */
    public void deletePage(final String id) {
        LOG.infof("Deleting page: %s", id);
        ServiceHelper.executeWithResponse(() -> {
            final var authHeader = this.authService.requireAuthorizationHeader();
            return this.pageClient.deletePage(id, authHeader);
        }, this.authService, LOG, "deleting page " + id, response -> {
            if (response.getStatus() == Response.Status.NO_CONTENT.getStatusCode()) {
                LOG.infof("Successfully deleted page: %s", id);
                return null;
            } else if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
                throw new ServiceException("Page not found");
            }
            throw ServiceHelper.responseFailure("Failed to delete page", response);
        });
    }

    /** Handles the search content operation. */
    public List<PageResponseDto> searchContent(final String searchTerm) {
        LOG.debugf("Searching page content: %s", searchTerm);
        return ServiceHelper.execute(() -> {
            final var authHeader = this.authService.requireAuthorizationHeader();
            return this.pageClient.searchPages(searchTerm, authHeader);
        }, this.authService, LOG, "searching page content");
    }

    /** Handles the search pages operation. */
    public List<PageResponseDto> searchPages(final String query) {
        LOG.debugf("Searching pages with query: %s", query);
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.execute(() -> this.pageClient.searchPages(query, authHeader), this.authService, LOG,
                "searching pages");
    }
}
