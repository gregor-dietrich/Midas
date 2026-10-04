package de.vptr.midas.gui.util;

import java.time.LocalDateTime;

import de.vptr.midas.gui.dto.AccountDto;
import de.vptr.midas.gui.dto.AccountResponseDto;
import de.vptr.midas.gui.dto.PostCategoryDto;
import de.vptr.midas.gui.dto.PostCategoryResponseDto;
import de.vptr.midas.gui.dto.PostCommentDto;
import de.vptr.midas.gui.dto.UserGroupDto;
import de.vptr.midas.gui.dto.UserRankDto;
import de.vptr.midas.gui.dto.UserRankResponseDto;
import de.vptr.midas.gui.dto.UserResponseDto;

/** Creates DTOs with default values for GUI tests. */
public final class MockDataUtil {

    private static final String TEST_PUBLIC_ID = "1";
    private static final String TEST_NAME = "Test Name";

    private MockDataUtil() {
        // Utility class
    }

    /** Creates an account form. */
    public static AccountDto createAccountDto() {
        final var account = new AccountDto(TEST_NAME);
        account.setPublicId(TEST_PUBLIC_ID);
        return account;
    }

    /** Creates an account as the backend returns it. */
    public static AccountResponseDto createAccountResponseDto(final Long id, final String name) {
        return new AccountResponseDto().publicId(String.valueOf(id)).name(name);
    }

    /** Creates a user as the backend returns it. */
    public static UserResponseDto createUserResponseDto(final String id, final String username, final String email) {
        return new UserResponseDto().publicId(id).username(username).email(email).rankPublicId("1").rankName("User")
                .activated(true).banned(false).created(LocalDateTime.of(2026, 1, 1, 12, 0));
    }

    /** Creates a group form without an id. */
    public static UserGroupDto createUserGroupDto() {
        return new UserGroupDto(null, "Test Group");
    }

    /** Creates a rank as the backend returns it. */
    public static UserRankResponseDto createUserRankResponseDto() {
        return new UserRankResponseDto().publicId(TEST_PUBLIC_ID).name(TEST_NAME).userAdd(true).userEdit(true)
                .userDelete(false).userAccountEdit(true).postEdit(true).postCommentEdit(false);
    }

    /** Creates a rank form. */
    public static UserRankDto createUserRankDto() {
        final var rank = new UserRankDto(TEST_NAME);
        rank.setPublicId(TEST_PUBLIC_ID);
        rank.setUserAdd(true);
        rank.setUserEdit(true);
        rank.setUserDelete(false);
        rank.setPostAdd(true);
        rank.setPostEdit(false);
        rank.setPostDelete(false);
        return rank;
    }

    /** Creates a category form. */
    public static PostCategoryDto createPostCategoryDto() {
        final var category = new PostCategoryDto();
        category.setPublicId(TEST_PUBLIC_ID);
        category.setName(TEST_NAME);
        return category;
    }

    /** Creates a category as the backend returns it. */
    public static PostCategoryResponseDto createPostCategoryResponseDto(final String id, final String name,
            final String parentName, final int childrenCount, final int postsCount, final boolean isRootCategory) {
        return new PostCategoryResponseDto().publicId(id).name(name).parentName(parentName).childrenCount(childrenCount)
                .postsCount(postsCount).isRootCategory(isRootCategory);
    }

    /** Creates a comment form. */
    public static PostCommentDto createPostCommentDto() {
        final var comment = new PostCommentDto();
        comment.setPublicId(TEST_PUBLIC_ID);
        comment.setContent("Test Content");
        comment.setPostPublicId(TEST_PUBLIC_ID);
        comment.setPostTitle("Test Title");
        comment.setUserPublicId(TEST_PUBLIC_ID);
        comment.setUsername("testuser");
        comment.setCreated(LocalDateTime.of(2026, 1, 1, 12, 0));
        return comment;
    }
}
