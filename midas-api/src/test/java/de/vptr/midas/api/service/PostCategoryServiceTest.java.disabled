package de.vptr.midas.api.service;

import static de.vptr.midas.api.util.ServiceTestUtil.assertServiceNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import de.vptr.midas.api.entity.PostCategoryEntity;
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
        // Create parent category first
        final var parentCategory = ServiceTestDataBuilder.createUniquePostCategoryDto();
        final var createdParent = this.postCategoryService.createCategory(parentCategory);

        // Create child category
        final var childCategory = ServiceTestDataBuilder.createUniquePostCategoryDto();
        // Need to get the actual entity to set as parent
        final var parentEntity = (PostCategoryEntity) PostCategoryEntity.findById(createdParent.id);
        childCategory.parent = parentEntity;

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

        // Set a non-existent parent
        final var fakeParent = new PostCategoryEntity();
        fakeParent.id = 999999L;
        category.parent = fakeParent;

        assertThrows(WebApplicationException.class, () -> {
            this.postCategoryService.createCategory(category);
        });
    }

    @Test
    @TestTransaction
    void testCreateCategoryWithBlankName() {
        final var category = new PostCategoryEntity();
        category.name = ""; // blank name

        assertThrows(ValidationException.class, () -> {
            this.postCategoryService.createCategory(category);
        });
    }

    @Test
    @TestTransaction
    void testCreateCategoryWithNullName() {
        final var category = new PostCategoryEntity();
        category.name = null;

        assertThrows(ValidationException.class, () -> {
            this.postCategoryService.createCategory(category);
        });
    }

    @Test
    @TestTransaction
    void testCreateCategoryWithWhitespaceOnlyName() {
        final var category = new PostCategoryEntity();
        category.name = "   "; // whitespace only

        assertThrows(ValidationException.class, () -> {
            this.postCategoryService.createCategory(category);
        });
    }

    @Test
    @TestTransaction
    void testFindById() {
        // Create a category first
        final var category = ServiceTestDataBuilder.createUniquePostCategoryDto();
        final var createdCategory = this.postCategoryService.createCategory(category);

        final var foundCategory = this.postCategoryService.findById(createdCategory.id);

        assertTrue(foundCategory.isPresent());
        assertEquals(createdCategory.id, foundCategory.get().id);
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
        // Create a root category
        final var rootCategory = ServiceTestDataBuilder.createUniquePostCategoryDto();
        final var createdRoot = this.postCategoryService.createCategory(rootCategory);

        // Create a child category
        final var childCategory = ServiceTestDataBuilder.createUniquePostCategoryDto();
        // Need to get the actual entity to set as parent
        final var rootEntity = (PostCategoryEntity) PostCategoryEntity.findById(createdRoot.id);
        childCategory.parent = rootEntity;
        this.postCategoryService.createCategory(childCategory);

        final var rootCategories = this.postCategoryService.findRootCategories();

        assertNotNull(rootCategories);
        assertTrue(rootCategories.stream().anyMatch(cat -> cat.id.equals(createdRoot.id)));
        // Verify that child category is not in root categories
        assertTrue(rootCategories.stream().noneMatch(cat -> cat.parentId != null));
    }

    @Test
    @TestTransaction
    void testFindByParentId() {
        // Create parent category first
        final var parentCategory = ServiceTestDataBuilder.createUniquePostCategoryDto();
        final var createdParent = this.postCategoryService.createCategory(parentCategory);

        // Create multiple child categories
        final var childCategory1 = ServiceTestDataBuilder.createUniquePostCategoryDto();
        // Need to get the actual entity to set as parent
        final var parentEntity = (PostCategoryEntity) PostCategoryEntity.findById(createdParent.id);
        childCategory1.parent = parentEntity;
        final var createdChild1 = this.postCategoryService.createCategory(childCategory1);

        final var childCategory2 = ServiceTestDataBuilder.createUniquePostCategoryDto();
        childCategory2.parent = parentEntity;
        final var createdChild2 = this.postCategoryService.createCategory(childCategory2);

        final var childCategories = this.postCategoryService.findByParentId(createdParent.id);

        assertNotNull(childCategories);
        assertEquals(2, childCategories.size());
        assertTrue(childCategories.stream().anyMatch(cat -> cat.id.equals(createdChild1.id)));
        assertTrue(childCategories.stream().anyMatch(cat -> cat.id.equals(createdChild2.id)));
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
        // Create a category first
        final var category = ServiceTestDataBuilder.createUniquePostCategoryDto();
        final var createdCategory = this.postCategoryService.createCategory(category);

        // Update the category
        final var updateCategory = new PostCategoryEntity();
        updateCategory.id = createdCategory.id;
        updateCategory.name = "Updated Category " + ServiceTestUtil.generateUniqueTestSuffix();

        final var updatedCategory = this.postCategoryService.updateCategory(updateCategory);

        assertNotNull(updatedCategory);
        assertEquals(createdCategory.id, updatedCategory.id);
        assertEquals(updateCategory.name, updatedCategory.name);
    }

    @Test
    @TestTransaction
    void testUpdateCategoryWithBlankName() {
        // Create a category first
        final var category = ServiceTestDataBuilder.createUniquePostCategoryDto();
        final var createdCategory = this.postCategoryService.createCategory(category);

        // Try to update with blank name
        final var updateCategory = new PostCategoryEntity();
        updateCategory.id = createdCategory.id;
        updateCategory.name = ""; // blank name

        assertThrows(ValidationException.class, () -> {
            this.postCategoryService.updateCategory(updateCategory);
        });
    }

    @Test
    @TestTransaction
    void testUpdateCategoryWithNullName() {
        // Create a category first
        final var category = ServiceTestDataBuilder.createUniquePostCategoryDto();
        final var createdCategory = this.postCategoryService.createCategory(category);

        // Try to update with null name
        final var updateCategory = new PostCategoryEntity();
        updateCategory.id = createdCategory.id;
        updateCategory.name = null;

        assertThrows(ValidationException.class, () -> {
            this.postCategoryService.updateCategory(updateCategory);
        });
    }

    @Test
    @TestTransaction
    void testUpdateNonExistentCategory() {
        final var updateCategory = new PostCategoryEntity();
        updateCategory.id = 999999L;
        updateCategory.name = "Updated Category";

        assertThrows(WebApplicationException.class, () -> {
            this.postCategoryService.updateCategory(updateCategory);
        });
    }

    @Test
    @TestTransaction
    void testPatchCategory() {
        // Create a category first
        final var category = ServiceTestDataBuilder.createUniquePostCategoryDto();
        final var createdCategory = this.postCategoryService.createCategory(category);

        // Patch the category
        final var patchCategory = new PostCategoryEntity();
        patchCategory.id = createdCategory.id;
        patchCategory.name = "Patched Category " + ServiceTestUtil.generateUniqueTestSuffix();

        final var patchedCategory = this.postCategoryService.patchCategory(patchCategory);

        assertNotNull(patchedCategory);
        assertEquals(createdCategory.id, patchedCategory.id);
        assertEquals(patchCategory.name, patchedCategory.name);
    }

    @Test
    @TestTransaction
    void testPatchCategoryWithNullName() {
        // Create a category first
        final var category = ServiceTestDataBuilder.createUniquePostCategoryDto();
        final var createdCategory = this.postCategoryService.createCategory(category);

        // Patch with null name (should not update name)
        final var patchCategory = new PostCategoryEntity();
        patchCategory.id = createdCategory.id;
        patchCategory.name = null; // null should not update

        final var patchedCategory = this.postCategoryService.patchCategory(patchCategory);

        assertNotNull(patchedCategory);
        assertEquals(createdCategory.id, patchedCategory.id);
        assertEquals(createdCategory.name, patchedCategory.name); // name should remain unchanged
    }

    @Test
    @TestTransaction
    void testPatchNonExistentCategory() {
        final var patchCategory = new PostCategoryEntity();
        patchCategory.id = 999999L;
        patchCategory.name = "Patched Category";

        assertThrows(WebApplicationException.class, () -> {
            this.postCategoryService.patchCategory(patchCategory);
        });
    }

    @Test
    @TestTransaction
    void testDeleteCategory() {
        // Create a category first
        final var category = ServiceTestDataBuilder.createUniquePostCategoryDto();
        final var createdCategory = this.postCategoryService.createCategory(category);

        final boolean deleted = this.postCategoryService.deleteCategory(createdCategory.id);

        assertTrue(deleted);

        // Verify the category is deleted
        final var deletedCategory = this.postCategoryService.findById(createdCategory.id);
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
        // Create a root category
        final var rootCategory = ServiceTestDataBuilder.createUniquePostCategoryDto();
        final var createdRoot = this.postCategoryService.createCategory(rootCategory);

        // Create a child category
        final var childCategory = ServiceTestDataBuilder.createUniquePostCategoryDto();
        // Need to get the actual entity to set as parent
        final var rootEntity = (PostCategoryEntity) PostCategoryEntity.findById(createdRoot.id);
        childCategory.parent = rootEntity;
        final var createdChild = this.postCategoryService.createCategory(childCategory);

        // Create a grandchild category
        final var grandchildCategory = ServiceTestDataBuilder.createUniquePostCategoryDto();
        // Need to get the actual entity to set as parent
        final var childEntity = (PostCategoryEntity) PostCategoryEntity.findById(createdChild.id);
        grandchildCategory.parent = childEntity;
        final var createdGrandchild = this.postCategoryService.createCategory(grandchildCategory);

        // Verify hierarchical structure
        assertNull(createdRoot.parentId);
        assertEquals(createdRoot.id, createdChild.parentId);
        assertEquals(createdChild.id, createdGrandchild.parentId);

        // Test finding by parent ID at different levels
        final var rootChildren = this.postCategoryService.findByParentId(createdRoot.id);
        assertEquals(1, rootChildren.size());
        assertEquals(createdChild.id, rootChildren.get(0).id);

        final var childChildren = this.postCategoryService.findByParentId(createdChild.id);
        assertEquals(1, childChildren.size());
        assertEquals(createdGrandchild.id, childChildren.get(0).id);

        final var grandchildChildren = this.postCategoryService.findByParentId(createdGrandchild.id);
        assertTrue(grandchildChildren.isEmpty());
    }
}