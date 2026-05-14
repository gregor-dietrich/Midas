package de.vptr.midas.gui.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
@SuppressWarnings("NullAway")
public final class TestDataBuilder {

    private TestDataBuilder() {
        // Utility class
    }

    /**
     * Create a unique UserDto for testing
     */
    public static UserDto createUniqueUser() {
        final var suffix = TestUtil.generateUniqueTestSuffix();
        final var user =
                new UserDto(TestUtil.createUniqueTestUsername("testuser"), TestUtil.createUniqueTestEmail("test"));
        user.setPublicId(suffix);
        user.setActivated(false);
        user.setBanned(false);
        user.setCreated(LocalDateTime.now(ZoneId.systemDefault()));
        user.setLastLogin(LocalDateTime.now(ZoneId.systemDefault()));
        return user;
    }

    /**
     * Create a UserDto with specific values
     */
    public static UserDto createUser(final String username, final String email) {
        final var user = new UserDto(username, email);
        user.setPublicId(TestUtil.generateUniqueTestSuffix());
        return user;
    }

    /**
     * Create a unique AccountDto for testing
     */
    public static AccountDto createUniqueAccount() {
        final var suffix = TestUtil.generateUniqueTestSuffix();
        final var account = new AccountDto(TestUtil.createUniqueTestName("Test Account"));
        account.setPublicId(suffix);
        return account;
    }

    /**
     * Create an AccountResponseDto for testing
     */
    public static AccountResponseDto createAccountResponse(final String publicId, final String name) {
        final var response = new AccountResponseDto();
        response.setPublicId(publicId);
        response.setName(name);
        response.setUserCount(0);
        response.setAssociatedUserPublicIds(List.of());
        return response;
    }

    /**
     * Create a unique UserGroupDto for testing
     */
    public static UserGroupDto createUniqueUserGroup() {
        final var suffix = TestUtil.generateUniqueTestSuffix();
        return new UserGroupDto(suffix, TestUtil.createUniqueTestName("Test Group"));
    }

    /**
     * Create a unique UserRankDto for testing
     */
    public static UserRankDto createUniqueUserRank() {
        final var suffix = TestUtil.generateUniqueTestSuffix();
        final var rank = new UserRankDto(TestUtil.createUniqueTestName("Test Rank"));
        rank.setPublicId(suffix);
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
        post.setPublicId(suffix);
        post.setTitle(TestUtil.createUniqueTestName("Test Post"));
        post.setContent(TestUtil.createUniqueTestName("Test content"));
        post.setPublished(false);
        post.setCommentable(false);
        post.setCreated(LocalDateTime.now(ZoneId.systemDefault()));
        post.setLastEdit(LocalDateTime.now(ZoneId.systemDefault()));
        final var user = createUniqueUser();
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
        comment.setPublicId(suffix);
        comment.setContent(TestUtil.createUniqueTestName("Test comment"));
        comment.setPostPublicId(post.getPublicId());
        comment.setPostTitle(post.getTitle());
        comment.setUserPublicId(user.getPublicId());
        comment.setUsername(user.getUsername());
        comment.setCreated(LocalDateTime.now(ZoneId.systemDefault()));
        return comment;
    }

    /**
     * Create a unique PaymentDto for testing
     */
    public static PaymentDto createUniquePayment() {
        final var suffix = TestUtil.generateUniqueTestSuffix();
        final var fromAccountId = suffix + "1";
        final var toAccountId = suffix + "2";
        final var amount = new BigDecimal("100.00");
        final var date = LocalDate.now(ZoneId.systemDefault());
        final var comment = TestUtil.createUniqueTestName("Test payment");
        final var created = LocalDateTime.now(ZoneId.systemDefault());
        final var updated = LocalDateTime.now(ZoneId.systemDefault());

        final var payment =
                new PaymentDto(fromAccountId, toAccountId, amount, date, comment, created, updated);
        payment.setPublicId(suffix);
        return payment;
    }

    /**
     * Create a unique PageDto for testing
     */
    public static PageDto createUniquePage() {
        final var suffix = TestUtil.generateUniqueTestSuffix();
        final var page = new PageDto();
        page.setPublicId(suffix);
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
        category.setPublicId(suffix);
        category.setName(TestUtil.createUniqueTestName("Test Category"));
        return category;
    }
}
