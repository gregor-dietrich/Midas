package de.vptr.midas.api.service;

import java.util.List;
import java.util.Optional;

import de.vptr.midas.api.dto.PageDto;
import de.vptr.midas.api.dto.PageResponseDto;
import de.vptr.midas.api.entity.PageEntity;
import de.vptr.midas.api.security.HtmlSanitizerService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

/** Service for page business logic. */
@ApplicationScoped
public class PageService {

    @Inject
    HtmlSanitizerService htmlSanitizer;

    /** Returns all pages with default pagination. */
    public List<PageResponseDto> getAllPages() {
        return this.getAllPages(0, 20);
    }

    /** Returns all pages with the given pagination. */
    public List<PageResponseDto> getAllPages(final int page, final int size) {
        return PageEntity.findAll().page(page, size).list().stream()
                .map(entity -> new PageResponseDto((PageEntity) entity)).toList();
    }

    /** Returns the page with the given publicId. */
    public Optional<PageResponseDto> findById(final String publicId) {
        return PageEntity.find("publicId", publicId).firstResultOptional()
                .map(entity -> new PageResponseDto((PageEntity) entity));
    }

    /** Returns all pages matching the given search term. */
    public List<PageResponseDto> searchPages(final String searchTerm) {
        return PageEntity.searchPages(searchTerm).stream().map(PageResponseDto::new).toList();
    }

    /** Creates a new page. */
    @Transactional
    public PageResponseDto createPage(final PageDto pageDto) {
        final PageEntity page = new PageEntity();
        page.title = pageDto.title;
        page.content = this.htmlSanitizer.sanitizeRichContent(pageDto.content);
        page.persistAndFlush();
        return new PageResponseDto(page);
    }

    /** Updates the page with the given publicId. */
    @Transactional
    public PageResponseDto updatePage(final String publicId, final PageDto pageDto) {
        final PageEntity existingPage = PageEntity.find("publicId", publicId).firstResult();
        if (existingPage == null) {
            throw new WebApplicationException("Page not found", Response.Status.NOT_FOUND);
        }

        // Complete replacement (PUT semantics)
        existingPage.title = pageDto.title;
        existingPage.content = this.htmlSanitizer.sanitizeRichContent(pageDto.content);
        existingPage.persistAndFlush();
        return new PageResponseDto(existingPage);
    }

    /** Partially updates the page with the given publicId. */
    @Transactional
    public PageResponseDto patchPage(final String publicId, final PageDto pageDto) {
        final PageEntity existingPage = PageEntity.find("publicId", publicId).firstResult();
        if (existingPage == null) {
            throw new WebApplicationException("Page not found", Response.Status.NOT_FOUND);
        }

        // Partial update (PATCH semantics) - only update provided fields
        if (pageDto.title != null && !pageDto.title.trim().isEmpty()) {
            existingPage.title = pageDto.title;
        }
        if (pageDto.content != null && !pageDto.content.trim().isEmpty()) {
            existingPage.content = this.htmlSanitizer.sanitizeRichContent(pageDto.content);
        }

        existingPage.persistAndFlush();
        return new PageResponseDto(existingPage);
    }

    /** Deletes the page with the given publicId. */
    @Transactional
    public boolean deletePage(final String publicId) {
        return PageEntity.delete("publicId", publicId) > 0;
    }
}
