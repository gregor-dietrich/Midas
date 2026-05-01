package de.vptr.midas.gui.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class UserRankDtoTest {

        static Stream<Arguments> permissionFields() {
                return Stream.of(
                                Arguments.of("adminView",
                                                (BiConsumer<UserRankDto, Boolean>) UserRankDto::setAdminView,
                                                (Function<UserRankDto, Boolean>) UserRankDto::canAdminView),
                                Arguments.of("pageAdd",
                                                (BiConsumer<UserRankDto, Boolean>) UserRankDto::setPageAdd,
                                                (Function<UserRankDto, Boolean>) UserRankDto::canAddPage),
                                Arguments.of("pageDelete",
                                                (BiConsumer<UserRankDto, Boolean>) UserRankDto::setPageDelete,
                                                (Function<UserRankDto, Boolean>) UserRankDto::canDeletePage),
                                Arguments.of("pageEdit",
                                                (BiConsumer<UserRankDto, Boolean>) UserRankDto::setPageEdit,
                                                (Function<UserRankDto, Boolean>) UserRankDto::canEditPage),
                                Arguments.of("postAdd",
                                                (BiConsumer<UserRankDto, Boolean>) UserRankDto::setPostAdd,
                                                (Function<UserRankDto, Boolean>) UserRankDto::canAddPost),
                                Arguments.of("postDelete",
                                                (BiConsumer<UserRankDto, Boolean>) UserRankDto::setPostDelete,
                                                (Function<UserRankDto, Boolean>) UserRankDto::canDeletePost),
                                Arguments.of("postEdit",
                                                (BiConsumer<UserRankDto, Boolean>) UserRankDto::setPostEdit,
                                                (Function<UserRankDto, Boolean>) UserRankDto::canEditPost),
                                Arguments.of("postCategoryAdd",
                                                (BiConsumer<UserRankDto, Boolean>) UserRankDto::setPostCategoryAdd,
                                                (Function<UserRankDto, Boolean>) UserRankDto::canAddPostCategory),
                                Arguments.of("postCategoryDelete",
                                                (BiConsumer<UserRankDto, Boolean>) UserRankDto::setPostCategoryDelete,
                                                (Function<UserRankDto, Boolean>) UserRankDto::canDeletePostCategory),
                                Arguments.of("postCategoryEdit",
                                                (BiConsumer<UserRankDto, Boolean>) UserRankDto::setPostCategoryEdit,
                                                (Function<UserRankDto, Boolean>) UserRankDto::canEditPostCategory),
                                Arguments.of("postCommentAdd",
                                                (BiConsumer<UserRankDto, Boolean>) UserRankDto::setPostCommentAdd,
                                                (Function<UserRankDto, Boolean>) UserRankDto::canAddPostComment),
                                Arguments.of("postCommentDelete",
                                                (BiConsumer<UserRankDto, Boolean>) UserRankDto::setPostCommentDelete,
                                                (Function<UserRankDto, Boolean>) UserRankDto::canDeletePostComment),
                                Arguments.of("postCommentEdit",
                                                (BiConsumer<UserRankDto, Boolean>) UserRankDto::setPostCommentEdit,
                                                (Function<UserRankDto, Boolean>) UserRankDto::canEditPostComment),
                                Arguments.of("userAdd",
                                                (BiConsumer<UserRankDto, Boolean>) UserRankDto::setUserAdd,
                                                (Function<UserRankDto, Boolean>) UserRankDto::canAddUser),
                                Arguments.of("userDelete",
                                                (BiConsumer<UserRankDto, Boolean>) UserRankDto::setUserDelete,
                                                (Function<UserRankDto, Boolean>) UserRankDto::canDeleteUser),
                                Arguments.of("userEdit",
                                                (BiConsumer<UserRankDto, Boolean>) UserRankDto::setUserEdit,
                                                (Function<UserRankDto, Boolean>) UserRankDto::canEditUser),
                                Arguments.of("userGroupAdd",
                                                (BiConsumer<UserRankDto, Boolean>) UserRankDto::setUserGroupAdd,
                                                (Function<UserRankDto, Boolean>) UserRankDto::canAddUserGroup),
                                Arguments.of("userGroupDelete",
                                                (BiConsumer<UserRankDto, Boolean>) UserRankDto::setUserGroupDelete,
                                                (Function<UserRankDto, Boolean>) UserRankDto::canDeleteUserGroup),
                                Arguments.of("userGroupEdit",
                                                (BiConsumer<UserRankDto, Boolean>) UserRankDto::setUserGroupEdit,
                                                (Function<UserRankDto, Boolean>) UserRankDto::canEditUserGroup),
                                Arguments.of("userAccountAdd",
                                                (BiConsumer<UserRankDto, Boolean>) UserRankDto::setUserAccountAdd,
                                                (Function<UserRankDto, Boolean>) UserRankDto::canAddUserAccount),
                                Arguments.of("userAccountDelete",
                                                (BiConsumer<UserRankDto, Boolean>) UserRankDto::setUserAccountDelete,
                                                (Function<UserRankDto, Boolean>) UserRankDto::canDeleteUserAccount),
                                Arguments.of("userAccountEdit",
                                                (BiConsumer<UserRankDto, Boolean>) UserRankDto::setUserAccountEdit,
                                                (Function<UserRankDto, Boolean>) UserRankDto::canEditUserAccount),
                                Arguments.of("userRankAdd",
                                                (BiConsumer<UserRankDto, Boolean>) UserRankDto::setUserRankAdd,
                                                (Function<UserRankDto, Boolean>) UserRankDto::canAddUserRank),
                                Arguments.of("userRankDelete",
                                                (BiConsumer<UserRankDto, Boolean>) UserRankDto::setUserRankDelete,
                                                (Function<UserRankDto, Boolean>) UserRankDto::canDeleteUserRank),
                                Arguments.of("userRankEdit",
                                                (BiConsumer<UserRankDto, Boolean>) UserRankDto::setUserRankEdit,
                                                (Function<UserRankDto, Boolean>) UserRankDto::canEditUserRank));
        }

        @Test
        void defaultConstructor_shouldInitializeAllPermissionsToFalse() {
                // When
                final UserRankDto dto = new UserRankDto();

                // Then
                assertThat(dto.getId()).isNull();
                assertThat(dto.getName()).isNull();
                assertThat(dto.getAdminView()).isFalse();
                assertThat(dto.getPageAdd()).isFalse();
                assertThat(dto.getPageDelete()).isFalse();
                assertThat(dto.getPageEdit()).isFalse();
                assertThat(dto.getPostAdd()).isFalse();
                assertThat(dto.getPostDelete()).isFalse();
                assertThat(dto.getPostEdit()).isFalse();
                assertThat(dto.getPostCategoryAdd()).isFalse();
                assertThat(dto.getPostCategoryDelete()).isFalse();
                assertThat(dto.getPostCategoryEdit()).isFalse();
                assertThat(dto.getPostCommentAdd()).isFalse();
                assertThat(dto.getPostCommentDelete()).isFalse();
                assertThat(dto.getPostCommentEdit()).isFalse();
                assertThat(dto.getUserAdd()).isFalse();
                assertThat(dto.getUserDelete()).isFalse();
                assertThat(dto.getUserEdit()).isFalse();
                assertThat(dto.getUserGroupAdd()).isFalse();
                assertThat(dto.getUserGroupDelete()).isFalse();
                assertThat(dto.getUserGroupEdit()).isFalse();
                assertThat(dto.getUserAccountAdd()).isFalse();
                assertThat(dto.getUserAccountDelete()).isFalse();
                assertThat(dto.getUserAccountEdit()).isFalse();
                assertThat(dto.getUserRankAdd()).isFalse();
                assertThat(dto.getUserRankDelete()).isFalse();
                assertThat(dto.getUserRankEdit()).isFalse();
        }

        @Test
        void nameConstructor_shouldSetName() {
                // When
                final UserRankDto dto = new UserRankDto("Admin");

                // Then
                assertThat(dto.getName()).isEqualTo("Admin");
                assertThat(dto.getId()).isNull();
        }

        @Test
        void setterGetter_idAndName() {
                // Given
                final UserRankDto dto = new UserRankDto();

                // When
                dto.setId(42L);
                dto.setName("Moderator");

                // Then
                assertThat(dto.getId()).isEqualTo(42L);
                assertThat(dto.getName()).isEqualTo("Moderator");
        }

        @ParameterizedTest(name = "{0}: null should return false")
        @MethodSource("permissionFields")
        void convenienceMethod_withNull_shouldReturnFalse(final String fieldName,
                        final BiConsumer<UserRankDto, Boolean> setter,
                        final Function<UserRankDto, Boolean> checker) {
                // Given
                final UserRankDto dto = new UserRankDto();
                setter.accept(dto, null);

                // Then
                assertThat(checker.apply(dto))
                                .as("can* method for %s with null", fieldName)
                                .isFalse();
        }

        @ParameterizedTest(name = "{0}: false should return false")
        @MethodSource("permissionFields")
        void convenienceMethod_withFalse_shouldReturnFalse(final String fieldName,
                        final BiConsumer<UserRankDto, Boolean> setter,
                        final Function<UserRankDto, Boolean> checker) {
                // Given
                final UserRankDto dto = new UserRankDto();
                setter.accept(dto, false);

                // Then
                assertThat(checker.apply(dto))
                                .as("can* method for %s with false", fieldName)
                                .isFalse();
        }

        @ParameterizedTest(name = "{0}: true should return true")
        @MethodSource("permissionFields")
        void convenienceMethod_withTrue_shouldReturnTrue(final String fieldName,
                        final BiConsumer<UserRankDto, Boolean> setter,
                        final Function<UserRankDto, Boolean> checker) {
                // Given
                final UserRankDto dto = new UserRankDto();
                setter.accept(dto, true);

                // Then
                assertThat(checker.apply(dto))
                                .as("can* method for %s with true", fieldName)
                                .isTrue();
        }

        @Test
        void hasAnyPermission_withNoPermissions_shouldReturnFalse() {
                // When
                final UserRankDto dto = new UserRankDto();

                // Then
                assertThat(dto.hasAnyPermission()).isFalse();
        }

        @Test
        void hasAnyPermission_withAdminView_shouldReturnTrue() {
                // Given
                final UserRankDto dto = new UserRankDto();
                dto.setAdminView(true);

                // Then
                assertThat(dto.hasAnyPermission()).isTrue();
        }

        @Test
        void hasAnyPermission_withPostAdd_shouldReturnTrue() {
                // Given
                final UserRankDto dto = new UserRankDto();
                dto.setPostAdd(true);

                // Then
                assertThat(dto.hasAnyPermission()).isTrue();
        }

        @Test
        void hasAnyPermission_withUserRankEdit_shouldReturnTrue() {
                // Given
                final UserRankDto dto = new UserRankDto();
                dto.setUserRankEdit(true);

                // Then
                assertThat(dto.hasAnyPermission()).isTrue();
        }

        @Test
        void hasAnyPermission_withUserAccountDelete_shouldReturnTrue() {
                // Given
                final UserRankDto dto = new UserRankDto();
                dto.setUserAccountDelete(true);

                // Then
                assertThat(dto.hasAnyPermission()).isTrue();
        }

}
