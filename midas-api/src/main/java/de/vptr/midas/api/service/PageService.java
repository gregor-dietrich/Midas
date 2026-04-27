package de.vptr.midas.api.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import de.vptr.midas.api.dto.PageDto;
import de.vptr.midas.api.dto.PageResponseDto;
import de.vptr.midas.api.entity.PageEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.validation.ValidationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

/** Service for page business logic. */
@ApplicationScoped
public class PageService {

    /** Returns all pages with default pagination. */
    public List<PageResponseDto> getAllPages() {
        return getAllPages(0, 20);
    }

    /** Returns all pages with the given pagination. */
    public List<PageResponseDto> getAllPages(final int page, final int size) {
        return PageEntity.findAll()
                .page(page, size)
                .list()
                .stream()
                .map(entity -> new PageResponseDto((PageEntity) entity))
                .toList();
    }

    /** Returns the page with the given id. */
    public Optional<PageResponseDto> findById(final Long id) {
        return PageEntity.findByIdOptional(id)
                .map(entity -> new PageResponseDto((PageEntity) entity));
    }

    /** Returns all pages matching the given search term. */
    public List<PageResponseDto> searchPages(final String searchTerm) {
        return PageEntity.searchPages(searchTerm).stream()
                .map(PageResponseDto::new)
                .toList();
    }

    /** Creates a new page. */
    @Transactional
    public PageResponseDto createPage(final PageDto pageDto) {
        if (pageDto.title == null || pageDto.title.trim().isEmpty()) {
            throw new ValidationException("Title is required");
        }
        if (pageDto.content == null || pageDto.content.trim().isEmpty()) {
            throw new ValidationException("Content is required");
        }

        final PageEntity page = new PageEntity();
        page.title = pageDto.title;
        page.content = pageDto.content;
        page.created = LocalDateTime.now();
        page.lastEdit = page.created;
        page.persist();

        return new PageResponseDto(page);
    }

    /** Updates the page with the given id. */
    @Transactional
    public PageResponseDto updatePage(final Long id, final PageDto pageDto) {
        if (pageDto.title == null || pageDto.title.trim().isEmpty()) {
            throw new ValidationException("Title is required");
        }
        if (pageDto.content == null || pageDto.content.trim().isEmpty()) {
            throw new ValidationException("Content is required");
        }

        final PageEntity existingPage = PageEntity.findById(id);
        if (existingPage == null) {
            throw new WebApplicationException("Page not found", Response.Status.NOT_FOUND);
        }

        // Complete replacement (PUT semantics)
        existingPage.title = pageDto.title;
        existingPage.content = pageDto.content;
        existingPage.lastEdit = LocalDateTime.now();
        existingPage.persist();

        return new PageResponseDto(existingPage);
    }

    /** Partially updates the page with the given id. */
    @Transactional
    public PageResponseDto patchPage(final Long id, final PageDto pageDto) {
        final PageEntity existingPage = PageEntity.findById(id);
        if (existingPage == null) {
            throw new WebApplicationException("Page not found", Response.Status.NOT_FOUND);
        }

        // Partial update (PATCH semantics) - only update provided fields
        if (pageDto.title != null && !pageDto.title.trim().isEmpty()) {
            existingPage.title = pageDto.title;
        }
        if (pageDto.content != null && !pageDto.content.trim().isEmpty()) {
            existingPage.content = pageDto.content;
        }

        existingPage.lastEdit = LocalDateTime.now();
        existingPage.persist();
        return new PageResponseDto(existingPage);
    }

    /** Deletes the page with the given id. */
    @Transactional
    public boolean deletePage(final Long id) {
        return PageEntity.deleteById(id);
    }
}
