package de.vptr.midas.gui.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import de.vptr.midas.gui.dto.AccountDto;
import de.vptr.midas.gui.dto.AccountResponseDto;
import de.vptr.midas.gui.dto.PageDto;
import de.vptr.midas.gui.dto.PageResponseDto;
import de.vptr.midas.gui.dto.PaymentResponseDto;
import de.vptr.midas.gui.dto.PostCategoryDto;
import de.vptr.midas.gui.dto.PostCategoryResponseDto;
import de.vptr.midas.gui.dto.PostCommentDto;
import de.vptr.midas.gui.dto.PostCommentResponseDto;
import de.vptr.midas.gui.dto.PostDto;
import de.vptr.midas.gui.dto.PostResponseDto;
import de.vptr.midas.gui.dto.UserDto;
import de.vptr.midas.gui.dto.UserGroupDto;
import de.vptr.midas.gui.dto.UserRankDto;
import de.vptr.midas.gui.dto.UserRankResponseDto;
import de.vptr.midas.gui.dto.UserResponseDto;

@SuppressWarnings("NullAway")
/**
 * Utility class for creating mock data objects for GUI tests
 */
public class MockDataUtil {

    private MockDataUtil() {
        // Utility class
    }

    /**
     * Common test constants
     */
    public static class TestConstants {
        public static final Long TEST_ID = 1L;
        public static final String TEST_USERNAME = "testuser";
        public static final String TEST_EMAIL = "test@example.com";
        public static final String TEST_PASSWORD = "password123";
        public static final String TEST_TITLE = "Test Title";
        public static final String TEST_CONTENT = "Test Content";
        public static final String TEST_NAME = "Test Name";
        public static final String TEST_COMMENT = "Test Comment";
        public static final BigDecimal TEST_AMOUNT = new BigDecimal("100.00");
        public static final String GROUP_NAME = "Test Group";

        private TestConstants() {
            // Constants class
        }
    }

    /**
     * Create a simple UserDto for testing
     */
    public static UserDto createUserDto() {
        final var user = new UserDto(TestConstants.TEST_USERNAME, TestConstants.TEST_EMAIL);
        user.setId(TestConstants.TEST_ID);
        return user;
    }

    /**
     * Create a simple UserResponseDto for testing
     */
    public static UserResponseDto createUserResponseDto() {
        final var user = new UserResponseDto();
        user.setId(TestConstants.TEST_ID);
        user.setUsername(TestConstants.TEST_USERNAME);
        user.setEmail(TestConstants.TEST_EMAIL);
        user.setRankId(1L);
        user.setRankName("User");
        user.setActivated(true);
        user.setBanned(false);
        user.setCreated(LocalDateTime.now(ZoneId.systemDefault()));
        return user;
    }

    /**
     * Create a UserResponseDto with specific values
     */
    public static UserResponseDto createUserResponseDto(final Long id, final String username, final String email) {
        final var user = new UserResponseDto();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setRankId(1L);
        user.setRankName("User");
        user.setActivated(true);
        user.setBanned(false);
        user.setCreated(LocalDateTime.now(ZoneId.systemDefault()));
        return user;
    }

    /**
     * Create a simple AccountDto for testing
     */
    public static AccountDto createAccountDto() {
        final var account = new AccountDto(TestConstants.TEST_NAME);
        account.setId(TestConstants.TEST_ID);
        return account;
    }

    /**
     * Create an AccountDto with specific values
     */
    public static AccountDto createAccountDto(final Long id, final String name) {
        final var account = new AccountDto(name);
        account.setId(id);
        return account;
    }

    /**
     * Create a simple AccountResponseDto for testing
     */
    public static AccountResponseDto createAccountResponseDto() {
        final var account = new AccountResponseDto();
        account.setId(TestConstants.TEST_ID);
        account.setName(TestConstants.TEST_NAME);
        return account;
    }

    /**
     * Create an AccountResponseDto with specific values
     */
    public static AccountResponseDto createAccountResponseDto(final Long id, final String name) {
        final var account = new AccountResponseDto();
        account.setId(id);
        account.setName(name);
        return account;
    }

    /**
     * Create a simple PostDto for testing
     */
    public static PostDto createPostDto() {
        final var post = new PostDto();
        post.setId(TestConstants.TEST_ID);
        post.setTitle(TestConstants.TEST_TITLE);
        post.setContent(TestConstants.TEST_CONTENT);
        post.setUserId(TestConstants.TEST_ID);
        post.setUsername(TestConstants.TEST_USERNAME);
        post.setCategoryId(1L);
        post.setCategoryName("Test Category");
        post.setPublished(true);
        return post;
    }

    /**
     * Create a simple PostResponseDto for testing
     */
    public static PostResponseDto createPostResponseDto() {
        final var post = new PostResponseDto();
        post.setId(TestConstants.TEST_ID);
        post.setTitle(TestConstants.TEST_TITLE);
        post.setContent(TestConstants.TEST_CONTENT);
        post.setUserId(TestConstants.TEST_ID);
        post.setUsername(TestConstants.TEST_USERNAME);
        post.setCategoryId(1L);
        post.setCategoryName("Test Category");
        post.setPublished(true);
        post.setCommentable(true);
        post.setCreated(LocalDateTime.now(ZoneId.systemDefault()));
        post.setLastEdit(LocalDateTime.now(ZoneId.systemDefault()));
        return post;
    }

    /**
     * Create a PaymentResponseDto for testing
     */
    public static PaymentResponseDto createPaymentResponseDto() {
        final var payment = new PaymentResponseDto();
        payment.setId(TestConstants.TEST_ID);
        payment.setUserId(TestConstants.TEST_ID);
        payment.setUsername(TestConstants.TEST_USERNAME);
        payment.setSourceAccountId(1L);
        payment.setSourceAccountName("Source Account");
        payment.setTargetAccountId(2L);
        payment.setTargetAccountName("Target Account");
        payment.setAmount(TestConstants.TEST_AMOUNT);
        payment.setComment(TestConstants.TEST_COMMENT);
        payment.setDate(LocalDate.now(ZoneId.systemDefault()));
        payment.setCreated(LocalDateTime.now(ZoneId.systemDefault()));
        payment.setLastEdit(LocalDateTime.now(ZoneId.systemDefault()));
        return payment;
    }

    /**
     * Create a PaymentResponseDto with specific values
     */
    public static PaymentResponseDto createPaymentResponseDto(final Long id, final String username, final Long sourceId,
            final String sourceAccountName, final Long targetId, final String targetAccountName,
            final BigDecimal amount, final String comment) {
        final var payment = new PaymentResponseDto();
        payment.setId(id);
        payment.setUserId(TestConstants.TEST_ID);
        payment.setUsername(username);
        payment.setSourceAccountId(sourceId);
        payment.setSourceAccountName(sourceAccountName);
        payment.setTargetAccountId(targetId);
        payment.setTargetAccountName(targetAccountName);
        payment.setAmount(amount);
        payment.setComment(comment);
        payment.setDate(LocalDate.now(ZoneId.systemDefault()));
        payment.setCreated(LocalDateTime.now(ZoneId.systemDefault()));
        payment.setLastEdit(LocalDateTime.now(ZoneId.systemDefault()));
        return payment;
    }

    /**
     * Create a simple PageDto for testing
     */
    public static PageDto createPageDto() {
        final var page = new PageDto();
        page.setId(TestConstants.TEST_ID);
        page.setTitle(TestConstants.TEST_TITLE);
        page.setContent(TestConstants.TEST_CONTENT);
        return page;
    }

    /**
     * Create a UserGroupDto for testing
     */
    public static UserGroupDto createUserGroupDto() {
        return new UserGroupDto(null, TestConstants.GROUP_NAME);
    }

    /**
     * Create a UserRankResponseDto for testing
     */
    public static UserRankResponseDto createUserRankResponseDto() {
        final var rank = new UserRankResponseDto();
        rank.setId(TestConstants.TEST_ID);
        rank.setName(TestConstants.TEST_NAME);
        rank.setUserAdd(true);
        rank.setUserEdit(true);
        rank.setUserDelete(false);
        rank.setUserAccountEdit(true);
        rank.setPostEdit(true);
        rank.setPostCommentEdit(false);
        return rank;
    }

    /**
     * Create a UserRankDto for testing
     */
    public static UserRankDto createUserRankDto() {
        final var rank = new UserRankDto(TestConstants.TEST_NAME);
        rank.setId(TestConstants.TEST_ID);
        rank.setUserAdd(true);
        rank.setUserEdit(true);
        rank.setUserDelete(false);
        rank.setPostAdd(true);
        rank.setPostEdit(false);
        rank.setPostDelete(false);
        return rank;
    }

    /**
     * Create a PostCategoryDto for testing
     */
    public static PostCategoryDto createPostCategoryDto() {
        final var category = new PostCategoryDto();
        category.setId(TestConstants.TEST_ID);
        category.setName(TestConstants.TEST_NAME);
        return category;
    }

    /**
     * Create a mock PostCommentDto with default values
     */
    public static PostCommentDto createPostCommentDto() {
        final var post = createPostDto();
        final var user = createUserDto();
        final var comment = new PostCommentDto();
        comment.setId(TestConstants.TEST_ID);
        comment.setContent(TestConstants.TEST_CONTENT);
        comment.setPostId(post.getId());
        comment.setPostTitle(post.getTitle());
        comment.setUserId(user.getId());
        comment.setUsername(user.getUsername());
        comment.setCreated(LocalDateTime.now(ZoneId.systemDefault()));
        return comment;
    }

    /**
     * Create a PostCommentDto for testing
     */
    public static PostCommentResponseDto createPostCommentResponseDto(final Long id, final String content,
            final Long postId, final String postTitle, final Long userId, final String username) {
        final var dto = new PostCommentResponseDto();
        dto.setId(id);
        dto.setContent(content);
        dto.setPostId(postId);
        dto.setPostTitle(postTitle);
        dto.setUserId(userId);
        dto.setUsername(username);
        dto.setCreated(LocalDateTime.now(ZoneId.systemDefault()));
        return dto;
    }

    /**
     * Create a PageResponseDto for testing
     */
    public static PageResponseDto createPageResponseDto(final Long id, final String title) {
        final var dto = new PageResponseDto();
        dto.setId(id);
        dto.setTitle(title);
        return dto;
    }

    /**
     * Create a PostCategoryResponseDto for testing
     */
    public static PostCategoryResponseDto createPostCategoryResponseDto(final Long id, final String name,
            final String parentName, final int childrenCount, final int postsCount, final boolean isRootCategory) {
        final var dto = new PostCategoryResponseDto();
        dto.setId(id);
        dto.setName(name);
        dto.setParentName(parentName);
        dto.setChildrenCount(childrenCount);
        dto.setPostsCount(postsCount);
        dto.setRootCategory(isRootCategory);
        return dto;
    }
}
