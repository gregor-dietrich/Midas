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
        assertNotNull(createdCategory.id);
        assertEquals(category.name, createdCategory.name);
        assertNull(createdCategory.parentId);
    }

    @Test
    @TestTransaction
    void testCreateCategoryWithParent() {
        final var parentCategory = ServiceTestDataBuilder.createUniquePostCategoryDto();
        final var createdParent = this.postCategoryService.createCategory(parentCategory);

        final var childCategory = ServiceTestDataBuilder.createUniquePostCategoryDto();
        childCategory.parentId = createdParent.id;

        final var createdChild = this.postCategoryService.createCategory(childCategory);

        assertNotNull(createdChild);
        assertNotNull(createdChild.id);
        assertEquals(childCategory.name, createdChild.name);
        assertNotNull(createdChild.parentId);
        assertEquals(createdParent.id, createdChild.parentId);
    }

    @Test
    @TestTransaction
    void testCreateCategoryWithNonExistentParent() {
        final var category = ServiceTestDataBuilder.createUniquePostCategoryDto();
        category.parentId = 999999L;

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

        final Long catId = Objects.requireNonNull(createdCategory.id);
        final var foundCategory = this.postCategoryService.findById(catId);

        assertTrue(foundCategory.isPresent());
        assertEquals(catId, foundCategory.get().id);
        assertEquals(createdCategory.name, foundCategory.get().name);
    }

    @Test
    void testFindByIdNonExistent() {
        final var foundCategory = this.postCategoryService.findById(999999L);
        assertTrue(foundCategory.isEmpty());
    }

    @Test
    @TestTransaction
    void testFindRootCategories() {
        final var rootCategory = ServiceTestDataBuilder.createUniquePostCategoryDto();
        final var createdRoot = this.postCategoryService.createCategory(rootCategory);

        final var childCategory = ServiceTestDataBuilder.createUniquePostCategoryDto();
        childCategory.parentId = createdRoot.id;
        this.postCategoryService.createCategory(childCategory);

        final var rootCategories = this.postCategoryService.findRootCategories();

        assertNotNull(rootCategories);
        assertTrue(rootCategories.stream().anyMatch(cat -> Objects.equals(cat.id, createdRoot.id)));
        assertTrue(rootCategories.stream().noneMatch(cat -> cat.parentId != null));
    }

    @Test
    @TestTransaction
    void testFindByParentId() {
        final var parentCategory = ServiceTestDataBuilder.createUniquePostCategoryDto();
        final var createdParent = this.postCategoryService.createCategory(parentCategory);

        final var childCategory1 = ServiceTestDataBuilder.createUniquePostCategoryDto();
        childCategory1.parentId = createdParent.id;
        final var createdChild1 = this.postCategoryService.createCategory(childCategory1);

        final var childCategory2 = ServiceTestDataBuilder.createUniquePostCategoryDto();
        childCategory2.parentId = createdParent.id;
        final var createdChild2 = this.postCategoryService.createCategory(childCategory2);

        final Long parentId = Objects.requireNonNull(createdParent.id);
        final var childCategories = this.postCategoryService.findByParentId(parentId);

        assertNotNull(childCategories);
        assertEquals(2, childCategories.size());
        assertTrue(childCategories.stream().anyMatch(cat -> Objects.equals(cat.id, createdChild1.id)));
        assertTrue(childCategories.stream().anyMatch(cat -> Objects.equals(cat.id, createdChild2.id)));
    }

    @Test
    void testFindByParentIdNonExistent() {
        final var childCategories = this.postCategoryService.findByParentId(999999L);
        assertNotNull(childCategories);
        assertTrue(childCategories.isEmpty());
    }

    @Test
    @TestTransaction
    void testUpdateCategory() {
        final var category = ServiceTestDataBuilder.createUniquePostCategoryDto();
        final var createdCategory = this.postCategoryService.createCategory(category);

        final Long catId = Objects.requireNonNull(createdCategory.id);
        final var updateCategory = new PostCategoryDto();
        updateCategory.name = "Updated Category " + ServiceTestUtil.generateUniqueTestSuffix();

        final var updatedCategory = this.postCategoryService.updateCategory(catId, updateCategory);

        assertNotNull(updatedCategory);
        assertEquals(catId, updatedCategory.id);
        assertEquals(updateCategory.name, updatedCategory.name);
    }

    @Test
    @TestTransaction
    void testUpdateCategoryWithBlankName() {
        final var category = ServiceTestDataBuilder.createUniquePostCategoryDto();
        final var createdCategory = this.postCategoryService.createCategory(category);

        final Long catId = Objects.requireNonNull(createdCategory.id);
        final var updateCategory = new PostCategoryDto();
        updateCategory.name = "";

        assertThrows(ValidationException.class, () -> {
            this.postCategoryService.updateCategory(catId, updateCategory);
        });
    }

    @Test
    @TestTransaction
    void testUpdateCategoryWithNullName() {
        final var category = ServiceTestDataBuilder.createUniquePostCategoryDto();
        final var createdCategory = this.postCategoryService.createCategory(category);

        final Long catId = Objects.requireNonNull(createdCategory.id);
        final var updateCategory = new PostCategoryDto();
        updateCategory.name = null;

        assertThrows(ValidationException.class, () -> {
            this.postCategoryService.updateCategory(catId, updateCategory);
        });
    }

    @Test
    @TestTransaction
    void testUpdateNonExistentCategory() {
        final var updateCategory = new PostCategoryDto();
        updateCategory.name = "Updated Category";

        assertThrows(WebApplicationException.class, () -> {
            this.postCategoryService.updateCategory(999999L, updateCategory);
        });
    }

    @Test
    @TestTransaction
    void testPatchCategory() {
        final var category = ServiceTestDataBuilder.createUniquePostCategoryDto();
        final var createdCategory = this.postCategoryService.createCategory(category);

        final Long catId = Objects.requireNonNull(createdCategory.id);
        final var patchCategory = new PostCategoryDto();
        patchCategory.name = "Patched Category " + ServiceTestUtil.generateUniqueTestSuffix();

        final var patchedCategory = this.postCategoryService.patchCategory(catId, patchCategory);

        assertNotNull(patchedCategory);
        assertEquals(catId, patchedCategory.id);
        assertEquals(patchCategory.name, patchedCategory.name);
    }

    @Test
    @TestTransaction
    void testPatchCategoryWithNullName() {
        final var category = ServiceTestDataBuilder.createUniquePostCategoryDto();
        final var createdCategory = this.postCategoryService.createCategory(category);

        final Long catId = Objects.requireNonNull(createdCategory.id);
        final var patchCategory = new PostCategoryDto();
        patchCategory.name = null;

        final var patchedCategory = this.postCategoryService.patchCategory(catId, patchCategory);

        assertNotNull(patchedCategory);
        assertEquals(catId, patchedCategory.id);
        assertEquals(createdCategory.name, patchedCategory.name);
    }

    @Test
    @TestTransaction
    void testPatchNonExistentCategory() {
        final var patchCategory = new PostCategoryDto();
        patchCategory.name = "Patched Category";

        assertThrows(WebApplicationException.class, () -> {
            this.postCategoryService.patchCategory(999999L, patchCategory);
        });
    }

    @Test
    @TestTransaction
    void testDeleteCategory() {
        final var category = ServiceTestDataBuilder.createUniquePostCategoryDto();
        final var createdCategory = this.postCategoryService.createCategory(category);

        final Long catId = Objects.requireNonNull(createdCategory.id);
        final boolean deleted = this.postCategoryService.deleteCategory(catId);

        assertTrue(deleted);

        final var deletedCategory = this.postCategoryService.findById(catId);
        assertTrue(deletedCategory.isEmpty());
    }

    @Test
    void testDeleteNonExistentCategory() {
        final boolean deleted = this.postCategoryService.deleteCategory(999999L);
        assertFalse(deleted);
    }

    @Test
    @TestTransaction
    void testHierarchicalStructure() {
        final var rootCategory = ServiceTestDataBuilder.createUniquePostCategoryDto();
        final var createdRoot = this.postCategoryService.createCategory(rootCategory);

        final var childCategory = ServiceTestDataBuilder.createUniquePostCategoryDto();
        childCategory.parentId = createdRoot.id;
        final var createdChild = this.postCategoryService.createCategory(childCategory);

        final var grandchildCategory = ServiceTestDataBuilder.createUniquePostCategoryDto();
        grandchildCategory.parentId = createdChild.id;
        final var createdGrandchild = this.postCategoryService.createCategory(grandchildCategory);

        assertNull(createdRoot.parentId);
        assertEquals(createdRoot.id, createdChild.parentId);
        assertEquals(createdChild.id, createdGrandchild.parentId);

        final Long rootId = Objects.requireNonNull(createdRoot.id);
        final Long childId = Objects.requireNonNull(createdChild.id);
        final Long grandchildId = Objects.requireNonNull(createdGrandchild.id);

        final var rootChildren = this.postCategoryService.findByParentId(rootId);
        assertEquals(1, rootChildren.size());
        assertEquals(childId, rootChildren.get(0).id);

        final var childChildren = this.postCategoryService.findByParentId(childId);
        assertEquals(1, childChildren.size());
        assertEquals(grandchildId, childChildren.get(0).id);

        final var grandchildChildren = this.postCategoryService.findByParentId(grandchildId);
        assertTrue(grandchildChildren.isEmpty());
    }
}
