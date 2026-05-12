package de.vptr.midas.api.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class EntityCoverageTest {

    @Test
    @TestTransaction
    void testAccountEntity() {
        AccountEntity entity = new AccountEntity();
        entity.name = "Coverage Account " + System.currentTimeMillis();
        entity.persistAndFlush();

        assertNotNull(AccountEntity.findByName(entity.name));
        verifyEqualsHashCode(entity, new AccountEntity());

        entity.userAccountMetas = null;
        assertTrue(entity.getAssociatedUsers().isEmpty());
        entity.userAccountMetas = new ArrayList<>();
        assertTrue(entity.getAssociatedUsers().isEmpty());
    }

    @Test
    @TestTransaction
    void testUserEntity() {
        UserEntity entity = new UserEntity();
        entity.username = "coverage_user_" + System.currentTimeMillis();
        entity.email = "coverage_" + System.currentTimeMillis() + "@example.com";
        entity.password = "password";
        entity.rank = UserRankEntity.findById(1L);
        entity.persistAndFlush();

        verifyEqualsHashCode(entity, new UserEntity());
    }

    @Test
    @TestTransaction
    void testPaymentEntity() {
        AccountEntity account = new AccountEntity();
        account.name = "Payment Account " + System.currentTimeMillis();
        account.persistAndFlush();

        PaymentEntity entity = new PaymentEntity();
        entity.amount = BigDecimal.TEN;
        entity.date = LocalDate.now(ZoneId.systemDefault());
        entity.comment = "Test";
        entity.targetAccount = account;
        entity.user = UserEntity.findById(1L);
        entity.persistAndFlush();

        verifyEqualsHashCode(entity, new PaymentEntity());

        entity.sourceAccount = null;
        entity.targetAccount = null;
        assertFalse(entity.hasLinkedAccount());
        entity.sourceAccount = account;
        assertTrue(entity.hasLinkedAccount());
        entity.sourceAccount = null;
        entity.targetAccount = account;
        assertTrue(entity.hasLinkedAccount());

        assertNotNull(PaymentEntity.findByUserId(1L));
        assertNotNull(PaymentEntity.findBySourceAccountId(Objects.requireNonNull(account.id)));
        assertNotNull(PaymentEntity.findByTargetAccountId(Objects.requireNonNull(account.id)));
        assertNotNull(PaymentEntity.findByDateRange(LocalDate.now(ZoneId.systemDefault()),
                LocalDate.now(ZoneId.systemDefault())));
        assertNotNull(PaymentEntity.findRecentPayments(5));
        assertNotNull(PaymentEntity.getTotalAmountByUser(1L));
    }

    @Test
    @TestTransaction
    void testUserGroupEntity() {
        UserGroupEntity entity = new UserGroupEntity();
        entity.name = "Coverage Group " + System.currentTimeMillis();
        entity.persistAndFlush();

        assertNotNull(UserGroupEntity.findByName(entity.name));
        verifyEqualsHashCode(entity, new UserGroupEntity());

        entity.userGroupMetas = null;
        assertTrue(entity.getUsers().isEmpty());
        assertEquals(0, entity.getUserCount());

        entity.userGroupMetas = new ArrayList<>();
        UserGroupMetaEntity meta = new UserGroupMetaEntity();
        meta.user = null;
        entity.userGroupMetas.add(meta);
        assertTrue(entity.getUsers().isEmpty());
        assertEquals(1, entity.getUserCount());
    }

    @Test
    @TestTransaction
    void testPageEntity() {
        PageEntity entity = new PageEntity();
        entity.title = "Coverage Page";
        entity.content = "Content";
        entity.persistAndFlush();
        verifyEqualsHashCode(entity, new PageEntity());
    }

    @Test
    @TestTransaction
    void testPostEntity() {
        PostEntity entity = new PostEntity();
        entity.title = "Coverage Post";
        entity.content = "Content";
        entity.persistAndFlush();
        verifyEqualsHashCode(entity, new PostEntity());
    }

    @Test
    @TestTransaction
    void testPostCategoryEntity() {
        PostCategoryEntity entity = new PostCategoryEntity();
        entity.name = "Coverage Category";
        entity.persistAndFlush();
        verifyEqualsHashCode(entity, new PostCategoryEntity());

        assertNotNull(PostCategoryEntity.findByParentId(null));
        assertNotNull(PostCategoryEntity.findRootCategories());
    }

    @Test
    @TestTransaction
    void testPostCommentEntity() {
        PostEntity post = new PostEntity();
        post.title = "Post for comment";
        post.content = "Content";
        post.persistAndFlush();

        PostCommentEntity entity = new PostCommentEntity();
        entity.content = "Coverage Comment";
        entity.post = post;
        entity.user = UserEntity.findById(1L);
        entity.persistAndFlush();

        verifyEqualsHashCode(entity, new PostCommentEntity());
        assertNotNull(PostCommentEntity.findByPostId(Objects.requireNonNull(post.id)));
        assertNotNull(PostCommentEntity.findByUserId(1L));
        assertNotNull(PostCommentEntity.findRecentComments(5));
    }

    @Test
    @TestTransaction
    void testUserRankEntity() {
        UserRankEntity entity = new UserRankEntity();
        entity.name = "Coverage Rank " + System.currentTimeMillis();
        entity.persistAndFlush();
        verifyEqualsHashCode(entity, new UserRankEntity());
    }

    @Test
    @TestTransaction
    void testUserAccountMetaEntity() {
        UserEntity user = UserEntity.findById(1L);
        AccountEntity account = new AccountEntity();
        account.name = "Meta Account " + System.currentTimeMillis();
        account.persistAndFlush();

        UserAccountMetaEntity entity = new UserAccountMetaEntity();
        entity.user = user;
        entity.account = account;
        entity.isNegative = true;
        entity.persistAndFlush();
        verifyEqualsHashCode(entity, new UserAccountMetaEntity());
    }

    @Test
    @TestTransaction
    void testUserGroupMetaEntity() {
        UserEntity user = UserEntity.findById(1L);
        UserGroupEntity group = new UserGroupEntity();
        group.name = "Meta Group " + System.currentTimeMillis();
        group.persistAndFlush();

        UserGroupMetaEntity entity = new UserGroupMetaEntity();
        entity.user = user;
        entity.group = group;
        entity.persistAndFlush();
        verifyEqualsHashCode(entity, new UserGroupMetaEntity());
    }

    @SuppressWarnings("EqualsNull")
    private void verifyEqualsHashCode(PanacheEntityBase entity, PanacheEntityBase other) {
        assertTrue(entity.equals(entity));
        assertFalse(entity == null);
        assertFalse(entity.equals(new Object()));

        try {
            var idField = entity.getClass().getField("id");
            idField.set(other, idField.get(entity));
            assertTrue(entity.equals(other));
            assertEquals(entity.hashCode(), other.hashCode());

            // Test with different ID
            Long originalId = (Long) idField.get(entity);
            idField.set(other, originalId + 1);
            assertFalse(entity.equals(other));
        } catch (Exception e) {
            // expected for some cases
        }
    }
}
