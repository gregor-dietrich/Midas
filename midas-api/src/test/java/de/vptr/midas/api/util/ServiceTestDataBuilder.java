package de.vptr.midas.api.util;

import java.math.BigDecimal;
import java.time.LocalDate;

import de.vptr.midas.api.dto.PageDto;
import de.vptr.midas.api.dto.PaymentDto;
import de.vptr.midas.api.dto.PostCategoryDto;
import de.vptr.midas.api.dto.PostCommentDto;
import de.vptr.midas.api.dto.PostDto;
import de.vptr.midas.api.dto.UserDto;
import de.vptr.midas.api.dto.UserGroupDto;
import de.vptr.midas.api.dto.UserRankDto;
import de.vptr.midas.api.entity.AccountEntity;
import de.vptr.midas.api.entity.PageEntity;
import de.vptr.midas.api.entity.PostCategoryEntity;
import de.vptr.midas.api.entity.PostCommentEntity;
import de.vptr.midas.api.entity.PostEntity;
import de.vptr.midas.api.entity.UserEntity;
import jakarta.annotation.Nullable;

/**
 * Builder utility for creating test data objects for service tests
 */
public final class ServiceTestDataBuilder {

    private ServiceTestDataBuilder() {
        // Utility class
    }

    /**
     * Creates a unique UserDto for testing
     */
    public static UserDto createUniqueUserDto() {
        final var suffix = ServiceTestUtil.generateUniqueTestSuffix();
        final var user = new UserDto();
        user.username = "testuser_" + suffix;
        user.email = "test_" + suffix + "@example.com";
        user.password = "password123";
        return user;
    }

    /**
     * Creates a unique UserDto with custom prefix
     */
    public static UserDto createUniqueUserDto(final String usernamePrefix, final String emailPrefix) {
        final var suffix = ServiceTestUtil.generateUniqueTestSuffix();
        final var user = new UserDto();
        user.username = usernamePrefix + "_" + suffix;
        user.email = emailPrefix + "_" + suffix + "@example.com";
        user.password = "password123";
        return user;
    }

    /**
     * Creates a unique PostDto for testing
     */
    public static PostDto createUniquePostDto(@Nullable final String userPublicId,
            @Nullable final String categoryPublicId) {
        final var suffix = ServiceTestUtil.generateUniqueTestSuffix();
        final var post = new PostDto();
        post.title = "Test Post " + suffix;
        post.content = "Test content " + suffix;
        post.userPublicId = userPublicId;
        post.categoryPublicId = categoryPublicId;
        post.published = false;
        post.commentable = false;
        return post;
    }

    /**
     * Creates a unique AccountEntity for testing
     */
    public static AccountEntity createUniqueAccountEntity() {
        final var account = new AccountEntity();
        account.name = ServiceTestUtil.createUniqueTestName("Test Account");
        return account;
    }

    /**
     * Creates a unique PostCategoryEntity for testing
     */
    public static PostCategoryEntity createUniquePostCategoryEntity() {
        final var category = new PostCategoryEntity();
        category.name = ServiceTestUtil.createUniqueTestName("Test Category");
        return category;
    }

    /**
     * Creates a unique PageEntity for testing
     */
    public static PageEntity createUniquePageEntity() {
        final var suffix = ServiceTestUtil.generateUniqueTestSuffix();
        final var page = new PageEntity();
        page.title = "Test Page " + suffix;
        page.content = "Test content " + suffix;
        return page;
    }

    /**
     * Creates a UserDto for update operations (without password)
     */
    public static UserDto createUserUpdateDto(final String usernamePrefix, final String emailPrefix) {
        final var suffix = ServiceTestUtil.generateUniqueTestSuffix();
        final var user = new UserDto();
        user.username = usernamePrefix + "_" + suffix;
        user.email = emailPrefix + "_" + suffix + "@example.com";
        user.activated = true;
        return user;
    }

    /**
     * Creates a unique UserRankDto for testing with all permissions enabled
     */
    public static UserRankDto createUniqueUserRankDto() {
        final var suffix = ServiceTestUtil.generateUniqueTestSuffix();
        final var rank = new UserRankDto();
        rank.name = "Test Rank " + suffix;

        // Set all permissions to true for testing
        rank.pageAdd = true;
        rank.pageEdit = true;
        rank.pageDelete = true;
        rank.postAdd = true;
        rank.postEdit = true;
        rank.postDelete = true;
        rank.postCategoryAdd = true;
        rank.postCategoryEdit = true;
        rank.postCategoryDelete = true;
        rank.postCommentAdd = true;
        rank.postCommentEdit = true;
        rank.postCommentDelete = true;
        rank.userAdd = true;
        rank.userEdit = true;
        rank.userDelete = true;
        rank.userGroupAdd = true;
        rank.userGroupEdit = true;
        rank.userGroupDelete = true;
        rank.userRankAdd = true;
        rank.userRankEdit = true;
        rank.userRankDelete = true;
        rank.userAccountAdd = true;
        rank.userAccountEdit = true;
        rank.userAccountDelete = true;

        return rank;
    }

    /**
     * Creates a unique UserGroupDto for testing
     */
    public static UserGroupDto createUniqueUserGroupDto() {
        final var suffix = ServiceTestUtil.generateUniqueTestSuffix();
        final var group = new UserGroupDto();
        group.name = "Test Group " + suffix;
        return group;
    }

    /**
     * Creates a UserGroupDto for update operations with a custom name
     */
    public static UserGroupDto createUserGroupUpdateDto(final String name) {
        final var group = new UserGroupDto();
        group.name = name;
        return group;
    }

    /**
     * Creates a PaymentDto for update operations
     */
    public static PaymentDto createPaymentUpdateDto(@Nullable final String sourceAccountPublicId,
            @Nullable final String targetAccountPublicId, @Nullable final String userPublicId, final String comment,
            final LocalDate date, final BigDecimal amount) {
        final var dto = new PaymentDto();
        dto.sourceAccountPublicId = sourceAccountPublicId;
        dto.targetAccountPublicId = targetAccountPublicId;
        dto.userPublicId = userPublicId;
        dto.comment = comment;
        dto.date = date;
        dto.amount = amount;
        return dto;
    }

    /**
     * Creates a PostCommentEntity for update operations with custom content
     */
    public static PostCommentEntity createPostCommentUpdateEntity(final String content) {
        final var entity = new PostCommentEntity();
        entity.content = content;
        return entity;
    }

    /**
     * Creates a PostDto for update operations with custom title and content
     */
    public static PostDto createPostUpdateDto(final String title, final String content, final String userPublicId,
            final String categoryPublicId) {
        final var dto = new PostDto();
        dto.title = title;
        dto.content = content;
        dto.userPublicId = userPublicId;
        dto.categoryPublicId = categoryPublicId;
        return dto;
    }

    /**
     * Creates a UserRankDto for update operations with custom permissions
     */
    public static UserRankDto createUserRankUpdateDto(final String name, final boolean userAdd, final boolean userEdit,
            final boolean userDelete, final boolean postAdd, final boolean postEdit, final boolean postDelete) {
        final var dto = new UserRankDto();
        dto.name = name;
        dto.userAdd = userAdd;
        dto.userEdit = userEdit;
        dto.userDelete = userDelete;
        dto.postAdd = postAdd;
        dto.postEdit = postEdit;
        dto.postDelete = postDelete;
        return dto;
    }

    /**
     * Creates a PaymentDto with all fields set
     */
    public static PaymentDto createPaymentDto(@Nullable final String sourceAccountPublicId,
            @Nullable final String targetAccountPublicId, @Nullable final String userPublicId, final String comment,
            final LocalDate date, final BigDecimal amount) {
        return createPaymentUpdateDto(sourceAccountPublicId, targetAccountPublicId, userPublicId, comment, date,
                amount);
    }

    /**
     * Creates a PostCommentEntity with all fields set
     */
    public static PostCommentEntity createPostCommentEntity(final String content, final UserEntity user,
            final PostEntity post) {
        final var entity = createPostCommentUpdateEntity(content);
        entity.user = user;
        entity.post = post;
        return entity;
    }

    /**
     * Creates a UserRankDto with all main permissions set
     */
    public static UserRankDto createUserRankDto(final String name, final boolean userAdd, final boolean userEdit,
            final boolean userDelete, final boolean postAdd, final boolean postEdit, final boolean postDelete) {
        return createUserRankUpdateDto(name, userAdd, userEdit, userDelete, postAdd, postEdit, postDelete);
    }

    /**
     * Creates a PostDto with all fields set
     */
    public static PostDto createPostDto(@Nullable final String title, @Nullable final String content,
            final boolean published, final boolean commentable, @Nullable final String userPublicId,
            @Nullable final String categoryPublicId) {
        final var dto = new PostDto();
        dto.title = title;
        dto.content = content;
        dto.published = published;
        dto.commentable = commentable;
        dto.userPublicId = userPublicId;
        dto.categoryPublicId = categoryPublicId;
        return dto;
    }

    /**
     * Creates a unique PageDto for testing
     */
    public static PageDto createUniquePageDto() {
        final var suffix = ServiceTestUtil.generateUniqueTestSuffix();
        final var dto = new PageDto();
        dto.title = "Test Page " + suffix;
        dto.content = "Test content " + suffix;
        return dto;
    }

    /**
     * Creates a PageDto with specified values
     */
    public static PageDto createPageDto(final String title, final String content) {
        final var dto = new PageDto();
        dto.title = title;
        dto.content = content;
        return dto;
    }

    /**
     * Creates a unique PostCategoryDto for testing
     */
    public static PostCategoryDto createUniquePostCategoryDto() {
        final var dto = new PostCategoryDto();
        dto.name = ServiceTestUtil.createUniqueTestName("Test Category");
        return dto;
    }

    /**
     * Creates a unique PostCommentDto for testing
     */
    public static PostCommentDto createUniquePostCommentDto(final String content, @Nullable final String postPublicId) {
        final var dto = new PostCommentDto();
        dto.content = content;
        dto.postPublicId = postPublicId;
        return dto;
    }
}
