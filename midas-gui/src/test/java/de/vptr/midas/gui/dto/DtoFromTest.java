package de.vptr.midas.gui.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

/** The editable forms copy what the views edit from the backend's responses. */
class DtoFromTest {

    private static final LocalDateTime CREATED = LocalDateTime.of(2026, 1, 2, 3, 4);
    private static final LocalDateTime EDITED = CREATED.plusDays(1);

    @Test
    void account_copiesIdAndName() {
        final var dto = AccountDto.from(new AccountResponseDto().publicId("a1").name("Cash").userCount(2L));

        assertThat(dto).extracting(AccountDto::getPublicId, AccountDto::getName).containsExactly("a1", "Cash");
    }

    @Test
    void page_copiesIdTitleAndContent() {
        final var dto = PageDto.from(new PageResponseDto().publicId("p1").title("Imprint").content("Body"));

        assertThat(dto).extracting(PageDto::getPublicId, PageDto::getTitle, PageDto::getContent).containsExactly("p1",
                "Imprint", "Body");
    }

    @Test
    void payment_copiesEveryEditableField() {
        final var dto = PaymentDto.from(new PaymentResponseDto().publicId("pay1").sourceAccountPublicId("s1")
                .sourceAccountName("Bank").targetAccountPublicId("t1").targetAccountName("Cash").username("member")
                .comment("Rent").date(LocalDate.of(2026, 2, 1)).amount(new BigDecimal("12.50")).created(CREATED)
                .lastEdit(EDITED));

        assertThat(dto)
                .extracting(PaymentDto::getPublicId, PaymentDto::getSourceAccountPublicId,
                        PaymentDto::getSourceAccountName, PaymentDto::getTargetAccountPublicId,
                        PaymentDto::getTargetAccountName, PaymentDto::getUsername, PaymentDto::getComment,
                        PaymentDto::getDate, PaymentDto::getAmount, PaymentDto::getCreated, PaymentDto::getLastEdit)
                .containsExactly("pay1", "s1", "Bank", "t1", "Cash", "member", "Rent", LocalDate.of(2026, 2, 1),
                        new BigDecimal("12.50"), CREATED, EDITED);
    }

    @Test
    void category_buildsMinimalParent_whenItHasOne() {
        final var dto = PostCategoryDto.from(new PostCategoryResponseDto().publicId("c1").name("Hardware")
                .parentPublicId("p1").parentName("Topics"));

        assertThat(dto.getName()).isEqualTo("Hardware");
        assertThat(dto.getParentPublicId()).isEqualTo("p1");
        assertThat(dto.getParent()).extracting(PostCategoryDto::getName).isEqualTo("Topics");
        assertThat(dto.isRootCategory()).isFalse();
    }

    @Test
    void category_staysRoot_withoutParent() {
        final var dto = PostCategoryDto.from(new PostCategoryResponseDto().publicId("c1").name("Topics"));

        assertThat(dto.getParent()).isNull();
        assertThat(dto.isRootCategory()).isTrue();
    }

    @Test
    void comment_copiesEveryEditableField() {
        final var dto = PostCommentDto.from(new PostCommentResponseDto().publicId("c1").content("Thanks")
                .postPublicId("p1").postTitle("News").userPublicId("u1").username("reader").created(CREATED));

        assertThat(dto)
                .extracting(PostCommentDto::getPublicId, PostCommentDto::getContent, PostCommentDto::getPostPublicId,
                        PostCommentDto::getPostTitle, PostCommentDto::getUserPublicId, PostCommentDto::getUsername,
                        PostCommentDto::getCreated)
                .containsExactly("c1", "Thanks", "p1", "News", "u1", "reader", CREATED);
    }

    @Test
    void post_copiesEveryEditableField() {
        final var dto = PostDto.from(new PostResponseDto().publicId("p1").title("News").content("Body")
                .username("author").categoryPublicId("c1").categoryName("Topics").published(true).commentable(false)
                .created(CREATED).lastEdit(EDITED).commentsCount(3L));

        assertThat(dto)
                .extracting(PostDto::getPublicId, PostDto::getTitle, PostDto::getContent, PostDto::getUsername,
                        PostDto::getCategoryPublicId, PostDto::getCategoryName, PostDto::isPublished,
                        PostDto::isCommentable, PostDto::getCreated, PostDto::getLastEdit, PostDto::getCommentsCount)
                .containsExactly("p1", "News", "Body", "author", "c1", "Topics", true, false, CREATED, EDITED, 3L);
    }

    @Test
    void user_copiesEveryFieldTheContractCarries() {
        final var dto = UserDto.from(new UserResponseDto().publicId("u1").username("alice").email("a@example.com")
                .rankPublicId("r1").rankName("Admin").banned(false).activated(true).created(CREATED).lastLogin(EDITED)
                .postsCount(4L).commentsCount(5L));

        assertThat(dto)
                .extracting(UserDto::getPublicId, UserDto::getUsername, UserDto::getEmail, UserDto::getRankPublicId,
                        UserDto::getRankName, UserDto::isBanned, UserDto::isActivated, UserDto::getCreated,
                        UserDto::getLastLogin, UserDto::getPostsCount, UserDto::getCommentsCount, UserDto::getLastIp)
                .containsExactly("u1", "alice", "a@example.com", "r1", "Admin", false, true, CREATED, EDITED, 4L, 5L,
                        null);
    }

    @Test
    void group_copiesIdAndName() {
        final var dto = UserGroupDto.from(new UserGroupResponseDto().publicId("g1").name("Editors").userCount(5L));

        assertThat(dto).extracting(UserGroupDto::getPublicId, UserGroupDto::getName).containsExactly("g1", "Editors");
    }

    @Test
    void rank_copiesPermissions_treatingAbsentAsDenied() {
        final var dto = UserRankDto.from(new UserRankResponseDto().publicId("r1").name("Moderator").adminView(true)
                .pageAdd(true).pageEdit(false).postAdd(true).postCategoryEdit(true).postCommentDelete(true)
                .userEdit(true).userGroupAdd(true).userAccountDelete(true).userRankEdit(true));

        assertThat(dto).extracting(UserRankDto::getPublicId, UserRankDto::getName).containsExactly("r1", "Moderator");
        assertThat(dto)
                .extracting(UserRankDto::canAdminView, UserRankDto::canAddPage, UserRankDto::canEditPage,
                        UserRankDto::canDeletePage, UserRankDto::canAddPost, UserRankDto::canEditPostCategory,
                        UserRankDto::canDeletePostComment, UserRankDto::canEditUser, UserRankDto::canAddUserGroup,
                        UserRankDto::canDeleteUserAccount, UserRankDto::canEditUserRank, UserRankDto::canDeleteUserRank)
                .containsExactly(true, true, false, false, true, true, true, true, true, true, true, false);
        assertThat(dto.isPageDelete()).isFalse();
    }
}
