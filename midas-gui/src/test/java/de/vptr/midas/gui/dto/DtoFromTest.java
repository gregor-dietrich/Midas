package de.vptr.midas.gui.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

/** The editable forms copy what the views edit from the backend's responses. */
class DtoFromTest {

    private static final LocalDateTime CREATED = LocalDateTime.of(2026, 1, 2, 3, 4);
    private static final LocalDateTime EDITED = CREATED.plusDays(1);

    private static final List<Permission> PERMISSIONS = List.of(
            new Permission("AdminView", UserRankResponseDto::setAdminView, UserRankDto::isAdminView,
                    UserRankDto::canAdminView),
            new Permission("PageAdd", UserRankResponseDto::setPageAdd, UserRankDto::isPageAdd, UserRankDto::canAddPage),
            new Permission("PageDelete", UserRankResponseDto::setPageDelete, UserRankDto::isPageDelete,
                    UserRankDto::canDeletePage),
            new Permission("PageEdit", UserRankResponseDto::setPageEdit, UserRankDto::isPageEdit,
                    UserRankDto::canEditPage),
            new Permission("PostAdd", UserRankResponseDto::setPostAdd, UserRankDto::isPostAdd, UserRankDto::canAddPost),
            new Permission("PostDelete", UserRankResponseDto::setPostDelete, UserRankDto::isPostDelete,
                    UserRankDto::canDeletePost),
            new Permission("PostEdit", UserRankResponseDto::setPostEdit, UserRankDto::isPostEdit,
                    UserRankDto::canEditPost),
            new Permission("PostCategoryAdd", UserRankResponseDto::setPostCategoryAdd, UserRankDto::isPostCategoryAdd,
                    UserRankDto::canAddPostCategory),
            new Permission("PostCategoryDelete", UserRankResponseDto::setPostCategoryDelete,
                    UserRankDto::isPostCategoryDelete, UserRankDto::canDeletePostCategory),
            new Permission("PostCategoryEdit", UserRankResponseDto::setPostCategoryEdit,
                    UserRankDto::isPostCategoryEdit, UserRankDto::canEditPostCategory),
            new Permission("PostCommentAdd", UserRankResponseDto::setPostCommentAdd, UserRankDto::isPostCommentAdd,
                    UserRankDto::canAddPostComment),
            new Permission("PostCommentDelete", UserRankResponseDto::setPostCommentDelete,
                    UserRankDto::isPostCommentDelete, UserRankDto::canDeletePostComment),
            new Permission("PostCommentEdit", UserRankResponseDto::setPostCommentEdit, UserRankDto::isPostCommentEdit,
                    UserRankDto::canEditPostComment),
            new Permission("UserAdd", UserRankResponseDto::setUserAdd, UserRankDto::isUserAdd, UserRankDto::canAddUser),
            new Permission("UserDelete", UserRankResponseDto::setUserDelete, UserRankDto::isUserDelete,
                    UserRankDto::canDeleteUser),
            new Permission("UserEdit", UserRankResponseDto::setUserEdit, UserRankDto::isUserEdit,
                    UserRankDto::canEditUser),
            new Permission("UserGroupAdd", UserRankResponseDto::setUserGroupAdd, UserRankDto::isUserGroupAdd,
                    UserRankDto::canAddUserGroup),
            new Permission("UserGroupDelete", UserRankResponseDto::setUserGroupDelete, UserRankDto::isUserGroupDelete,
                    UserRankDto::canDeleteUserGroup),
            new Permission("UserGroupEdit", UserRankResponseDto::setUserGroupEdit, UserRankDto::isUserGroupEdit,
                    UserRankDto::canEditUserGroup),
            new Permission("UserAccountAdd", UserRankResponseDto::setUserAccountAdd, UserRankDto::isUserAccountAdd,
                    UserRankDto::canAddUserAccount),
            new Permission("UserAccountDelete", UserRankResponseDto::setUserAccountDelete,
                    UserRankDto::isUserAccountDelete, UserRankDto::canDeleteUserAccount),
            new Permission("UserAccountEdit", UserRankResponseDto::setUserAccountEdit, UserRankDto::isUserAccountEdit,
                    UserRankDto::canEditUserAccount),
            new Permission("UserRankAdd", UserRankResponseDto::setUserRankAdd, UserRankDto::isUserRankAdd,
                    UserRankDto::canAddUserRank),
            new Permission("UserRankDelete", UserRankResponseDto::setUserRankDelete, UserRankDto::isUserRankDelete,
                    UserRankDto::canDeleteUserRank),
            new Permission("UserRankEdit", UserRankResponseDto::setUserRankEdit, UserRankDto::isUserRankEdit,
                    UserRankDto::canEditUserRank));

    /** A rank permission: how the response carries it and how the editable form exposes and checks it. */
    private record Permission(String name, BiConsumer<UserRankResponseDto, Boolean> grant,
            Function<UserRankDto, Boolean> read, Predicate<UserRankDto> can) {
    }

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

    /**
     * Grants each permission alone, leaving the others alternately denied and absent, so a mapping that reads the wrong
     * flag or keeps an absent one as {@code null} shows up as a mismatch.
     */
    @Test
    void rank_copiesPermissions_treatingAbsentAsDenied() {
        assertThat(PERMISSIONS).hasSize(25);
        for (final var granted : PERMISSIONS) {
            final var response = new UserRankResponseDto().publicId("r1").name("Moderator");
            for (int i = 0; i < PERMISSIONS.size(); i++) {
                final var permission = PERMISSIONS.get(i);
                if (permission.equals(granted)) {
                    permission.grant().accept(response, Boolean.TRUE);
                } else if (i % 2 == 0) {
                    permission.grant().accept(response, Boolean.FALSE);
                }
            }

            final var dto = UserRankDto.from(response);

            assertThat(dto).extracting(UserRankDto::getPublicId, UserRankDto::getName).containsExactly("r1",
                    "Moderator");
            assertThat(PERMISSIONS).allSatisfy(permission -> {
                assertThat(permission.read().apply(dto)).as("%s while granting %s", permission.name(), granted.name())
                        .isEqualTo(permission.equals(granted));
                assertThat(permission.can().test(dto)).as("can%s while granting %s", permission.name(), granted.name())
                        .isEqualTo(permission.equals(granted));
            });
        }
    }
}
