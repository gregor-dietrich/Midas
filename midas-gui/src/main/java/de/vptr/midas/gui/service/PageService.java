package de.vptr.midas.gui.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import de.vptr.midas.gui.client.PageClient;
import de.vptr.midas.gui.client.PageClient.DeletePageRequest;
import de.vptr.midas.gui.client.PageClient.GetAllPagesRequest;
import de.vptr.midas.gui.client.PageClient.GetPageRequest;
import de.vptr.midas.gui.client.PageClient.SearchPagesRequest;
import de.vptr.midas.gui.client.PageClient.UpdatePageRequest;
import de.vptr.midas.gui.dto.PageDto;
import de.vptr.midas.gui.dto.PageResponseDto;
import de.vptr.midas.gui.util.PaginationParamUtil;
import de.vptr.midas.gui.util.ServiceHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

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
        return ServiceHelper.execute(
                () -> this.pageClient.getAllPages(GetAllPagesRequest.newInstance()
                        .page(PaginationParamUtil.DEFAULT_PAGE).size(PaginationParamUtil.MAX_SIZE)),
                this.authService, LOG, "fetching pages");
    }

    /** Returns the all pages paginated. */
    public List<PageResponseDto> getAllPages(final Integer page, final Integer size) {
        LOG.debugf("Fetching all pages page %s size %s", page, size);
        final Integer safePage = PaginationParamUtil.sanitizePage(page);
        final Integer safeSize = PaginationParamUtil.sanitizeSize(size);
        return ServiceHelper.execute(
                () -> this.pageClient.getAllPages(GetAllPagesRequest.newInstance().page(safePage).size(safeSize)),
                this.authService, LOG, "fetching pages");
    }

    /** Returns the page by id, or empty if it does not exist. */
    public Optional<PageResponseDto> getPageById(final String id) {
        LOG.infof("Getting page by id: %s", id);
        return ServiceHelper.executeOptional(() -> this.pageClient.getPage(GetPageRequest.newInstance().publicId(id)),
                this.authService, LOG, "getting page " + id);
    }

    /** Creates a new page. */
    public PageDto createPage(final PageDto page) {
        LOG.infof("Creating page: %s", page.getTitle());
        return PageDto.from(ServiceHelper.execute(() -> this.pageClient.createPage(page), this.authService, LOG,
                "creating page", "Failed to create page"));
    }

    /** Updates the page. */
    public PageDto updatePage(final PageDto page) {
        final String publicId = Objects.requireNonNull(page.getPublicId(), "page.publicId");
        LOG.infof("Updating page: %s (%s)", page.getTitle(), publicId);
        return PageDto.from(ServiceHelper.execute(
                () -> this.pageClient.updatePage(UpdatePageRequest.newInstance().publicId(publicId), page),
                this.authService, LOG, "updating page " + publicId, "Failed to update page"));
    }

    /** Handles the delete page operation. */
    public void deletePage(final String id) {
        LOG.infof("Deleting page: %s", id);
        ServiceHelper.executeVoid(() -> this.pageClient.deletePage(DeletePageRequest.newInstance().publicId(id)),
                this.authService, LOG, "deleting page " + id, "Page not found");
    }

    /** Handles the search content operation. */
    public List<PageResponseDto> searchContent(final String searchTerm) {
        LOG.debugf("Searching page content: %s", searchTerm);
        return ServiceHelper.execute(
                () -> this.pageClient.searchPages(SearchPagesRequest.newInstance().query(searchTerm)), this.authService,
                LOG, "searching page content");
    }

    /** Handles the search pages operation. */
    public List<PageResponseDto> searchPages(final String query) {
        LOG.debugf("Searching pages with query: %s", query);
        return ServiceHelper.execute(() -> this.pageClient.searchPages(SearchPagesRequest.newInstance().query(query)),
                this.authService, LOG, "searching pages");
    }
}
