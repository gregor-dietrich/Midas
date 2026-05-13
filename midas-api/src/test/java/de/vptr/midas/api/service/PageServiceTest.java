package de.vptr.midas.api.service;

import static de.vptr.midas.api.util.ServiceTestUtil.assertServiceNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Objects;

import org.junit.jupiter.api.Test;

import de.vptr.midas.api.dto.PageDto;
import de.vptr.midas.api.util.ServiceTestDataBuilder;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.validation.ValidationException;
import jakarta.ws.rs.WebApplicationException;

@QuarkusTest
class PageServiceTest {
    @Inject
    PageService pageService;

    @Test
    void testServiceNotNull() {
        assertServiceNotNull(this.pageService);
    }

    @Test
    void testGetAllPages() {
        final var pages = this.pageService.getAllPages();
        assertNotNull(pages);
    }

    @Test
    @TestTransaction
    void testCreatePage() {
        final var pageDto = ServiceTestDataBuilder.createUniquePageDto();

        final var createdPage = this.pageService.createPage(pageDto);

        assertNotNull(createdPage);
        assertNotNull(createdPage.publicId);
        assertEquals(pageDto.title, createdPage.title);
        assertEquals(pageDto.content, createdPage.content);
    }

    @Test
    @TestTransaction
    void testCreatePageWithBlankTitle() {
        final var pageDto = new PageDto();
        pageDto.title = "";
        pageDto.content = "Test content";

        assertThrows(ValidationException.class, () -> {
            this.pageService.createPage(pageDto);
        });
    }

    @Test
    @TestTransaction
    void testCreatePageWithNullTitle() {
        final var pageDto = new PageDto();
        pageDto.title = null;
        pageDto.content = "Test content";

        assertThrows(ValidationException.class, () -> {
            this.pageService.createPage(pageDto);
        });
    }

    @Test
    @TestTransaction
    void testCreatePageWithBlankContent() {
        final var pageDto = new PageDto();
        pageDto.title = "Test Title";
        pageDto.content = "";

        assertThrows(ValidationException.class, () -> {
            this.pageService.createPage(pageDto);
        });
    }

    @Test
    @TestTransaction
    void testCreatePageWithNullContent() {
        final var pageDto = new PageDto();
        pageDto.title = "Test Title";
        pageDto.content = null;

        assertThrows(ValidationException.class, () -> {
            this.pageService.createPage(pageDto);
        });
    }

    @Test
    @TestTransaction
    void testFindById() {
        final var pageDto = ServiceTestDataBuilder.createUniquePageDto();
        final var createdPage = this.pageService.createPage(pageDto);

        final String pagePublicId = Objects.requireNonNull(createdPage.publicId);
        final var foundPage = this.pageService.findById(pagePublicId);

        assertTrue(foundPage.isPresent());
        assertEquals(pagePublicId, foundPage.get().publicId);
        assertEquals(createdPage.title, foundPage.get().title);
        assertEquals(createdPage.content, foundPage.get().content);
    }

    @Test
    void testFindByIdNonExistent() {
        final var foundPage = this.pageService.findById("999999");
        assertTrue(foundPage.isEmpty());
    }

    @Test
    @TestTransaction
    void testUpdatePage() {
        final var pageDto = ServiceTestDataBuilder.createUniquePageDto();
        final var createdPage = this.pageService.createPage(pageDto);

        final String pagePublicId = Objects.requireNonNull(createdPage.publicId);
        final var updateDto = ServiceTestDataBuilder.createPageDto("Updated Title", "Updated Content");
        final var updatedPage = this.pageService.updatePage(pagePublicId, updateDto);

        assertNotNull(updatedPage);
        assertEquals(pagePublicId, updatedPage.publicId);
        assertEquals("Updated Title", updatedPage.title);
        assertEquals("Updated Content", updatedPage.content);
    }

    @Test
    @TestTransaction
    void testUpdatePageWithBlankTitle() {
        final var pageDto = ServiceTestDataBuilder.createUniquePageDto();
        final var createdPage = this.pageService.createPage(pageDto);

        final String pagePublicId = Objects.requireNonNull(createdPage.publicId);
        final var updateDto = new PageDto();
        updateDto.title = "";
        updateDto.content = "Updated Content";

        assertThrows(ValidationException.class, () -> {
            this.pageService.updatePage(pagePublicId, updateDto);
        });
    }

    @Test
    @TestTransaction
    void testUpdatePageWithBlankContent() {
        final var pageDto = ServiceTestDataBuilder.createUniquePageDto();
        final var createdPage = this.pageService.createPage(pageDto);

        final String pagePublicId = Objects.requireNonNull(createdPage.publicId);
        final var updateDto = new PageDto();
        updateDto.title = "Updated Title";
        updateDto.content = "";

        assertThrows(ValidationException.class, () -> {
            this.pageService.updatePage(pagePublicId, updateDto);
        });
    }

    @Test
    @TestTransaction
    void testUpdateNonExistentPage() {
        final var updateDto = ServiceTestDataBuilder.createPageDto("Updated Title", "Updated Content");

        assertThrows(WebApplicationException.class, () -> {
            this.pageService.updatePage("999999", updateDto);
        });
    }

    @Test
    @TestTransaction
    void testPatchPage() {
        final var pageDto = ServiceTestDataBuilder.createUniquePageDto();
        final var createdPage = this.pageService.createPage(pageDto);

        final String pagePublicId = Objects.requireNonNull(createdPage.publicId);
        final var patchDto = new PageDto();
        patchDto.title = "Patched Title";

        final var patchedPage = this.pageService.patchPage(pagePublicId, patchDto);

        assertNotNull(patchedPage);
        assertEquals(pagePublicId, patchedPage.publicId);
        assertEquals("Patched Title", patchedPage.title);
        assertEquals(createdPage.content, patchedPage.content);
    }

    @Test
    @TestTransaction
    void testPatchPageContent() {
        final var pageDto = ServiceTestDataBuilder.createUniquePageDto();
        final var createdPage = this.pageService.createPage(pageDto);

        final String pagePublicId = Objects.requireNonNull(createdPage.publicId);
        final var patchDto = new PageDto();
        patchDto.content = "Patched Content";

        final var patchedPage = this.pageService.patchPage(pagePublicId, patchDto);

        assertNotNull(patchedPage);
        assertEquals(pagePublicId, patchedPage.publicId);
        assertEquals(createdPage.title, patchedPage.title);
        assertEquals("Patched Content", patchedPage.content);
    }

    @Test
    @TestTransaction
    void testPatchNonExistentPage() {
        final var patchDto = new PageDto();
        patchDto.title = "Patched Title";

        assertThrows(WebApplicationException.class, () -> {
            this.pageService.patchPage("999999", patchDto);
        });
    }

    @Test
    @TestTransaction
    void testDeletePage() {
        final var pageDto = ServiceTestDataBuilder.createUniquePageDto();
        final var createdPage = this.pageService.createPage(pageDto);

        final String pagePublicId = Objects.requireNonNull(createdPage.publicId);
        final boolean deleted = this.pageService.deletePage(pagePublicId);

        assertTrue(deleted);

        final var deletedPage = this.pageService.findById(pagePublicId);
        assertTrue(deletedPage.isEmpty());
    }

    @Test
    void testDeleteNonExistentPage() {
        final boolean deleted = this.pageService.deletePage("999999");
        assertFalse(deleted);
    }
}
