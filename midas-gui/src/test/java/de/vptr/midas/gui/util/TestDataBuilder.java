package de.vptr.midas.gui.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import de.vptr.midas.gui.dto.AccountDto;
import de.vptr.midas.gui.dto.AccountResponseDto;
import de.vptr.midas.gui.dto.PageDto;
import de.vptr.midas.gui.dto.PaymentDto;
import de.vptr.midas.gui.dto.PostCategoryDto;
import de.vptr.midas.gui.dto.PostCommentDto;
import de.vptr.midas.gui.dto.PostDto;
import de.vptr.midas.gui.dto.UserDto;
import de.vptr.midas.gui.dto.UserGroupDto;
import de.vptr.midas.gui.dto.UserRankDto;

/**
 * Builder utility for creating test data objects for GUI service tests
 */
public class TestDataBuilder {

    private TestDataBuilder() {
        // Utility class
    }

    /**
     * Create a unique UserDto for testing
     */
    public static UserDto createUniqueUser() {
        final var suffix = TestUtil.generateUniqueTestSuffix();
        final var user = new UserDto(TestUtil.createUniqueTestUsername("testuser"),
                TestUtil.createUniqueTestEmail("test"));
        user.setId(Long.valueOf(suffix));
        user.setActivated(false);
        user.setBanned(false);
        user.setCreated(LocalDateTime.now());
        user.setLastLogin(LocalDateTime.now());
        return user;
    }

    /**
     * Create a UserDto with specific values
     */
    public static UserDto createUser(final String username, final String email) {
        final var user = new UserDto(username, email);
        user.setId(Long.valueOf(TestUtil.generateUniqueTestSuffix()));
        return user;
    }

    /**
     * Create a unique AccountDto for testing
     */
    public static AccountDto createUniqueAccount() {
        final var suffix = TestUtil.generateUniqueTestSuffix();
        final var account = new AccountDto(TestUtil.createUniqueTestName("Test Account"));
        account.setId(Long.valueOf(suffix));
        return account;
    }

    /**
     * Create an AccountResponseDto for testing
     */
    public static AccountResponseDto createAccountResponse(final Long id, final String name) {
        final var response = new AccountResponseDto();
        response.setId(id);
        response.setName(name);
        response.setUserCount(0);
        response.setAssociatedUserIds(List.of());
        return response;
    }

    /**
     * Create a unique UserGroupDto for testing
     */
    public static UserGroupDto createUniqueUserGroup() {
        final var suffix = TestUtil.generateUniqueTestSuffix();
        final var group = new UserGroupDto(Long.valueOf(suffix),
                TestUtil.createUniqueTestName("Test Group"));
        return group;
    }

    /**
     * Create a unique UserRankDto for testing
     */
    public static UserRankDto createUniqueUserRank() {
        final var suffix = TestUtil.generateUniqueTestSuffix();
        final var rank = new UserRankDto(TestUtil.createUniqueTestName("Test Rank"));
        rank.setId(Long.valueOf(suffix));
        rank.setUserAdd(true);
        rank.setUserEdit(true);
        rank.setUserDelete(false);
        rank.setPostAdd(true);
        rank.setPostEdit(false);
        rank.setPostDelete(false);
        return rank;
    }

    /**
     * Create a unique PostDto for testing
     */
    public static PostDto createUniquePost() {
        final var suffix = TestUtil.generateUniqueTestSuffix();
        final var post = new PostDto();
        post.setId(Long.valueOf(suffix));
        post.setTitle(TestUtil.createUniqueTestName("Test Post"));
        post.setContent(TestUtil.createUniqueTestName("Test content"));
        post.setPublished(false);
        post.setCommentable(false);
        post.setCreated(LocalDateTime.now());
        post.setLastEdit(LocalDateTime.now());
        final var user = createUniqueUser();
        post.setUserId(user.getId());
        post.setUsername(user.getUsername());
        return post;
    }

    /**
     * Create a unique PostCommentDto for testing
     */
    public static PostCommentDto createUniquePostComment() {
        final var suffix = TestUtil.generateUniqueTestSuffix();
        final var user = createUniqueUser();
        final var post = createUniquePost();
        final var comment = new PostCommentDto();
        comment.setId(Long.valueOf(suffix));
        comment.setContent(TestUtil.createUniqueTestName("Test comment"));
        comment.setPostId(post.getId());
        comment.setPostTitle(post.getTitle());
        comment.setUserId(user.getId());
        comment.setUsername(user.getUsername());
        comment.setCreated(LocalDateTime.now());
        return comment;
    }

    /**
     * Create a unique PaymentDto for testing
     */
    public static PaymentDto createUniquePayment() {
        final var suffix = TestUtil.generateUniqueTestSuffix();
        final var user = createUniqueUser();
        final var fromAccountId = Long.valueOf(suffix + "1");
        final var toAccountId = Long.valueOf(suffix + "2");
        final var amount = new BigDecimal("100.00");
        final var date = LocalDate.now();
        final var comment = TestUtil.createUniqueTestName("Test payment");
        final var created = LocalDateTime.now();
        final var updated = LocalDateTime.now();

        final var payment = new PaymentDto(user.getId(), fromAccountId, toAccountId,
                amount, date, comment, created, updated);
        payment.setId(Long.valueOf(suffix));
        return payment;
    }

    /**
     * Create a unique PageDto for testing
     */
    public static PageDto createUniquePage() {
        final var suffix = TestUtil.generateUniqueTestSuffix();
        final var page = new PageDto();
        page.setId(Long.valueOf(suffix));
        page.setTitle(TestUtil.createUniqueTestName("Test Page"));
        page.setContent(TestUtil.createUniqueTestName("Test page content"));
        return page;
    }

    /**
     * Create a unique PostCategoryDto for testing
     */
    public static PostCategoryDto createUniquePostCategory() {
        final var suffix = TestUtil.generateUniqueTestSuffix();
        final var category = new PostCategoryDto();
        category.setId(Long.valueOf(suffix));
        category.setName(TestUtil.createUniqueTestName("Test Category"));
        return category;
    }
}
