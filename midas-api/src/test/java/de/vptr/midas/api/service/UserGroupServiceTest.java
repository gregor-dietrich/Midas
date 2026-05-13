package de.vptr.midas.api.service;

import static de.vptr.midas.api.util.ServiceTestDataBuilder.createUniqueUserGroupDto;
import static de.vptr.midas.api.util.ServiceTestUtil.assertServiceNotNull;
import static de.vptr.midas.api.util.ServiceTestUtil.generateUniqueTestSuffix;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Objects;

import org.junit.jupiter.api.Test;

import de.vptr.midas.api.util.PaginationUtil;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class UserGroupServiceTest {
    @Inject
    UserGroupService userGroupService;

    @Inject
    UserService userService;

    @Test
    void testServiceNotNull() {
        assertServiceNotNull(this.userGroupService);
    }

    @Test
    void testGetAllGroups() {
        final var groups = this.userGroupService.getAllGroups();
        assertNotNull(groups);
    }

    @Test
    @TestTransaction
    void testCreateGroup() {
        final var newGroupDto = createUniqueUserGroupDto();

        final var createdGroup = this.userGroupService.createGroup(newGroupDto);

        assertNotNull(createdGroup);
        assertNotNull(createdGroup.publicId);
        assertEquals(newGroupDto.name, createdGroup.name);
    }

    @Test
    @TestTransaction
    void testUpdateGroup() {
        // First create a group
        final var newGroupDto = createUniqueUserGroupDto();
        final var createdGroup = this.userGroupService.createGroup(newGroupDto);

        // Update the group
        final var updateDto = createUniqueUserGroupDto();

        final var updatedGroup =
                this.userGroupService.updateGroup(Objects.requireNonNull(createdGroup.publicId), updateDto);

        assertNotNull(updatedGroup);
        assertEquals(updateDto.name, updatedGroup.name);
    }

    @Test
    @TestTransaction
    void testDeleteGroup() {
        // First create a group
        final var newGroupDto = createUniqueUserGroupDto();
        final var createdGroup = this.userGroupService.createGroup(newGroupDto);

        final String groupId = Objects.requireNonNull(createdGroup.publicId);

        final var deleted = this.userGroupService.deleteGroup(groupId);

        assertTrue(deleted);
        final var deletedGroup = this.userGroupService.findById(groupId);
        assertTrue(deletedGroup.isEmpty());
    }

    @Test
    void testDeleteNonExistentGroup() {
        final var deleted = this.userGroupService.deleteGroup("non-existent-id");
        assertFalse(deleted);
    }

    @Test
    @TestTransaction
    void testFindById() {
        // First create a group
        final var newGroupDto = createUniqueUserGroupDto();
        final var createdGroup = this.userGroupService.createGroup(newGroupDto);

        final var foundGroup = this.userGroupService.findById(Objects.requireNonNull(createdGroup.publicId));

        assertTrue(foundGroup.isPresent());
        assertEquals(createdGroup.publicId, foundGroup.get().publicId);
        assertEquals(createdGroup.name, foundGroup.get().name);
    }

    @Test
    void testFindByIdNonExistent() {
        final var foundGroup = this.userGroupService.findById("non-existent-id");
        assertTrue(foundGroup.isEmpty());
    }

    @Test
    void testSearchGroupsWithNullOrBlankQueryUsesDefaultListing() {
        final var expected = this.userGroupService.getAllGroups();

        final var nullResults = this.userGroupService.searchGroups("");
        final var blankResults = this.userGroupService.searchGroups("   ");

        assertEquals(expected.stream().map(group -> group.publicId).toList(),
                nullResults.stream().map(group -> group.publicId).toList());
        assertEquals(expected.stream().map(group -> group.publicId).toList(),
                blankResults.stream().map(group -> group.publicId).toList());
    }

    @Test
    @TestTransaction
    void testSearchGroupsAppliesSharedResultLimit() {
        final String token = "group-limit-" + generateUniqueTestSuffix();
        for (int i = 0; i < PaginationUtil.MAX_SIZE + 5; i++) {
            final var group = createUniqueUserGroupDto();
            group.name = token + "-" + i;
            this.userGroupService.createGroup(group);
        }

        final var results = this.userGroupService.searchGroups(token);

        assertEquals(PaginationUtil.MAX_SIZE, results.size());
    }
}
