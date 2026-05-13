package de.vptr.midas.api.service;

import static de.vptr.midas.api.util.ServiceTestUtil.assertServiceNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Objects;

import org.junit.jupiter.api.Test;

import de.vptr.midas.api.dto.PostCategoryDto;
import de.vptr.midas.api.util.ServiceTestDataBuilder;
import de.vptr.midas.api.util.ServiceTestUtil;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.validation.ValidationException;
import jakarta.ws.rs.WebApplicationException;

@QuarkusTest
class PostCategoryServiceTest {
    @Inject
    PostCategoryService postCategoryService;

    @Test
    void testServiceNotNull() {
        assertServiceNotNull(this.postCategoryService);
    }

    @Test
    void testGetAllCategories() {
        final var categories = this.postCategoryService.getAllCategories();
        assertNotNull(categories);
    }

    @Test
    @TestTransaction
    void testCreateCategory() {
        final var category = ServiceTestDataBuilder.createUniquePostCategoryDto();

        final var createdCategory = this.postCategoryService.createCategory(category);

        assertNotNull(createdCategory);
        assertNotNull(createdCategory.publicId);
        assertEquals(category.name, createdCategory.name);
        assertNull(createdCategory.parentPublicId);
    }

    @Test
    @TestTransaction
    void testCreateCategoryWithParent() {
        final var parentCategory = ServiceTestDataBuilder.createUniquePostCategoryDto();
        final var createdParent = this.postCategoryService.createCategory(parentCategory);

        final var childCategory = ServiceTestDataBuilder.createUniquePostCategoryDto();
        childCategory.parentPublicId = createdParent.publicId;

        final var createdChild = this.postCategoryService.createCategory(childCategory);

        assertNotNull(createdChild);
        assertNotNull(createdChild.publicId);
        assertEquals(childCategory.name, createdChild.name);
        assertNotNull(createdChild.parentPublicId);
        assertEquals(createdParent.publicId, createdChild.parentPublicId);
    }

    @Test
    @TestTransaction
    void testCreateCategoryWithNonExistentParent() {
        final var category = ServiceTestDataBuilder.createUniquePostCategoryDto();
        category.parentPublicId = "999999";

        assertThrows(WebApplicationException.class, () -> {
            this.postCategoryService.createCategory(category);
        });
    }

    @Test
    @TestTransaction
    void testCreateCategoryWithBlankName() {
        final var category = new PostCategoryDto();
        category.name = "";

        assertThrows(ValidationException.class, () -> {
            this.postCategoryService.createCategory(category);
        });
    }

    @Test
    @TestTransaction
    void testCreateCategoryWithNullName() {
        final var category = new PostCategoryDto();
        category.name = null;

        assertThrows(ValidationException.class, () -> {
            this.postCategoryService.createCategory(category);
        });
    }

    @Test
    @TestTransaction
    void testCreateCategoryWithWhitespaceOnlyName() {
        final var category = new PostCategoryDto();
        category.name = "   ";

        assertThrows(ValidationException.class, () -> {
            this.postCategoryService.createCategory(category);
        });
    }

    @Test
    @TestTransaction
    void testFindById() {
        final var category = ServiceTestDataBuilder.createUniquePostCategoryDto();
        final var createdCategory = this.postCategoryService.createCategory(category);

        final String catPublicId = Objects.requireNonNull(createdCategory.publicId);
        final var foundCategory = this.postCategoryService.findById(catPublicId);

        assertTrue(foundCategory.isPresent());
        assertEquals(catPublicId, foundCategory.get().publicId);
        assertEquals(createdCategory.name, foundCategory.get().name);
    }

    @Test
    void testFindByIdNonExistent() {
        final var foundCategory = this.postCategoryService.findById("999999");
        assertTrue(foundCategory.isEmpty());
    }

    @Test
    @TestTransaction
    void testFindRootCategories() {
        final var rootCategory = ServiceTestDataBuilder.createUniquePostCategoryDto();
        final var createdRoot = this.postCategoryService.createCategory(rootCategory);

        final var childCategory = ServiceTestDataBuilder.createUniquePostCategoryDto();
        childCategory.parentPublicId = createdRoot.publicId;
        this.postCategoryService.createCategory(childCategory);

        final var rootCategories = this.postCategoryService.findRootCategories();

        assertNotNull(rootCategories);
        assertTrue(rootCategories.stream().anyMatch(cat -> Objects.equals(cat.publicId, createdRoot.publicId)));
        assertTrue(rootCategories.stream().noneMatch(cat -> cat.parentPublicId != null));
    }

    @Test
    @TestTransaction
    void testFindByParentId() {
        final var parentCategory = ServiceTestDataBuilder.createUniquePostCategoryDto();
        final var createdParent = this.postCategoryService.createCategory(parentCategory);

        final var childCategory1 = ServiceTestDataBuilder.createUniquePostCategoryDto();
        childCategory1.parentPublicId = createdParent.publicId;
        final var createdChild1 = this.postCategoryService.createCategory(childCategory1);

        final var childCategory2 = ServiceTestDataBuilder.createUniquePostCategoryDto();
        childCategory2.parentPublicId = createdParent.publicId;
        final var createdChild2 = this.postCategoryService.createCategory(childCategory2);

        final String parentPublicId = Objects.requireNonNull(createdParent.publicId);
        final var childCategories = this.postCategoryService.findByParentPublicId(parentPublicId);

        assertNotNull(childCategories);
        assertEquals(2, childCategories.size());
        assertTrue(childCategories.stream().anyMatch(cat -> Objects.equals(cat.publicId, createdChild1.publicId)));
        assertTrue(childCategories.stream().anyMatch(cat -> Objects.equals(cat.publicId, createdChild2.publicId)));
    }

    @Test
    void testFindByParentIdNonExistent() {
        final var childCategories = this.postCategoryService.findByParentPublicId("999999");
        assertNotNull(childCategories);
        assertTrue(childCategories.isEmpty());
    }

    @Test
    @TestTransaction
    void testUpdateCategory() {
        final var category = ServiceTestDataBuilder.createUniquePostCategoryDto();
        final var createdCategory = this.postCategoryService.createCategory(category);

        final String catPublicId = Objects.requireNonNull(createdCategory.publicId);
        final var updateCategory = new PostCategoryDto();
        updateCategory.name = "Updated Category " + ServiceTestUtil.generateUniqueTestSuffix();

        final var updatedCategory = this.postCategoryService.updateCategory(catPublicId, updateCategory);

        assertNotNull(updatedCategory);
        assertEquals(catPublicId, updatedCategory.publicId);
        assertEquals(updateCategory.name, updatedCategory.name);
    }

    @Test
    @TestTransaction
    void testUpdateCategoryWithBlankName() {
        final var category = ServiceTestDataBuilder.createUniquePostCategoryDto();
        final var createdCategory = this.postCategoryService.createCategory(category);

        final String catPublicId = Objects.requireNonNull(createdCategory.publicId);
        final var updateCategory = new PostCategoryDto();
        updateCategory.name = "";

        assertThrows(ValidationException.class, () -> {
            this.postCategoryService.updateCategory(catPublicId, updateCategory);
        });
    }

    @Test
    @TestTransaction
    void testUpdateCategoryWithNullName() {
        final var category = ServiceTestDataBuilder.createUniquePostCategoryDto();
        final var createdCategory = this.postCategoryService.createCategory(category);

        final String catPublicId = Objects.requireNonNull(createdCategory.publicId);
        final var updateCategory = new PostCategoryDto();
        updateCategory.name = null;

        assertThrows(ValidationException.class, () -> {
            this.postCategoryService.updateCategory(catPublicId, updateCategory);
        });
    }

    @Test
    @TestTransaction
    void testUpdateNonExistentCategory() {
        final var updateCategory = new PostCategoryDto();
        updateCategory.name = "Updated Category";

        assertThrows(WebApplicationException.class, () -> {
            this.postCategoryService.updateCategory("999999", updateCategory);
        });
    }

    @Test
    @TestTransaction
    void testPatchCategory() {
        final var category = ServiceTestDataBuilder.createUniquePostCategoryDto();
        final var createdCategory = this.postCategoryService.createCategory(category);

        final String catPublicId = Objects.requireNonNull(createdCategory.publicId);
        final var patchCategory = new PostCategoryDto();
        patchCategory.name = "Patched Category " + ServiceTestUtil.generateUniqueTestSuffix();

        final var patchedCategory = this.postCategoryService.patchCategory(catPublicId, patchCategory);

        assertNotNull(patchedCategory);
        assertEquals(catPublicId, patchedCategory.publicId);
        assertEquals(patchCategory.name, patchedCategory.name);
    }

    @Test
    @TestTransaction
    void testPatchCategoryWithNullName() {
        final var category = ServiceTestDataBuilder.createUniquePostCategoryDto();
        final var createdCategory = this.postCategoryService.createCategory(category);

        final String catPublicId = Objects.requireNonNull(createdCategory.publicId);
        final var patchCategory = new PostCategoryDto();
        patchCategory.name = null;

        final var patchedCategory = this.postCategoryService.patchCategory(catPublicId, patchCategory);

        assertNotNull(patchedCategory);
        assertEquals(catPublicId, patchedCategory.publicId);
        assertEquals(createdCategory.name, patchedCategory.name);
    }

    @Test
    @TestTransaction
    void testPatchNonExistentCategory() {
        final var patchCategory = new PostCategoryDto();
        patchCategory.name = "Patched Category";

        assertThrows(WebApplicationException.class, () -> {
            this.postCategoryService.patchCategory("999999", patchCategory);
        });
    }

    @Test
    @TestTransaction
    void testDeleteCategory() {
        final var category = ServiceTestDataBuilder.createUniquePostCategoryDto();
        final var createdCategory = this.postCategoryService.createCategory(category);

        final String catPublicId = Objects.requireNonNull(createdCategory.publicId);
        final boolean deleted = this.postCategoryService.deleteCategory(catPublicId);

        assertTrue(deleted);

        final var deletedCategory = this.postCategoryService.findById(catPublicId);
        assertTrue(deletedCategory.isEmpty());
    }

    @Test
    void testDeleteNonExistentCategory() {
        final boolean deleted = this.postCategoryService.deleteCategory("999999");
        assertFalse(deleted);
    }

    @Test
    @TestTransaction
    void testHierarchicalStructure() {
        final var rootCategory = ServiceTestDataBuilder.createUniquePostCategoryDto();
        final var createdRoot = this.postCategoryService.createCategory(rootCategory);

        final var childCategory = ServiceTestDataBuilder.createUniquePostCategoryDto();
        childCategory.parentPublicId = createdRoot.publicId;
        final var createdChild = this.postCategoryService.createCategory(childCategory);

        final var grandchildCategory = ServiceTestDataBuilder.createUniquePostCategoryDto();
        grandchildCategory.parentPublicId = createdChild.publicId;
        final var createdGrandchild = this.postCategoryService.createCategory(grandchildCategory);

        assertNull(createdRoot.parentPublicId);
        assertEquals(createdRoot.publicId, createdChild.parentPublicId);
        assertEquals(createdChild.publicId, createdGrandchild.parentPublicId);

        final String rootPublicId = Objects.requireNonNull(createdRoot.publicId);
        final String childPublicId = Objects.requireNonNull(createdChild.publicId);
        final String grandchildPublicId = Objects.requireNonNull(createdGrandchild.publicId);

        final var rootChildren = this.postCategoryService.findByParentPublicId(rootPublicId);
        assertEquals(1, rootChildren.size());
        assertEquals(childPublicId, rootChildren.get(0).publicId);

        final var childChildren = this.postCategoryService.findByParentPublicId(childPublicId);
        assertEquals(1, childChildren.size());
        assertEquals(grandchildPublicId, childChildren.get(0).publicId);

        final var grandchildChildren = this.postCategoryService.findByParentPublicId(grandchildPublicId);
        assertTrue(grandchildChildren.isEmpty());
    }
}
