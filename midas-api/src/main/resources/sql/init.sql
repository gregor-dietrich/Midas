SET timezone = 'UTC';

-- Drop Tables
DROP TABLE IF EXISTS pages CASCADE;
DROP TABLE IF EXISTS post_categories CASCADE;
DROP TABLE IF EXISTS post_comments CASCADE;
DROP TABLE IF EXISTS posts CASCADE;
DROP TABLE IF EXISTS user_accounts CASCADE;
DROP TABLE IF EXISTS user_accounts_meta CASCADE;
DROP TABLE IF EXISTS user_groups CASCADE;
DROP TABLE IF EXISTS user_groups_meta CASCADE;
DROP TABLE IF EXISTS user_payments CASCADE;
DROP TABLE IF EXISTS user_ranks CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- Table: pages
CREATE TABLE pages (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_edit TIMESTAMP
);

-- Full-text search index for pages content
CREATE INDEX idx_pages_content_fts ON pages USING gin(to_tsvector('english', content));

-- Table: user_ranks
CREATE TABLE user_ranks (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) UNIQUE NOT NULL,
    admin_view BOOLEAN NOT NULL DEFAULT FALSE,
    page_add BOOLEAN NOT NULL DEFAULT FALSE,
    page_delete BOOLEAN NOT NULL DEFAULT FALSE,
    page_edit BOOLEAN NOT NULL DEFAULT FALSE,
    post_add BOOLEAN NOT NULL DEFAULT FALSE,
    post_delete BOOLEAN NOT NULL DEFAULT FALSE,
    post_edit BOOLEAN NOT NULL DEFAULT FALSE,
    post_category_add BOOLEAN NOT NULL DEFAULT FALSE,
    post_category_delete BOOLEAN NOT NULL DEFAULT FALSE,
    post_category_edit BOOLEAN NOT NULL DEFAULT FALSE,
    post_comment_add BOOLEAN NOT NULL DEFAULT FALSE,
    post_comment_delete BOOLEAN NOT NULL DEFAULT FALSE,
    post_comment_edit BOOLEAN NOT NULL DEFAULT FALSE,
    user_add BOOLEAN NOT NULL DEFAULT FALSE,
    user_delete BOOLEAN NOT NULL DEFAULT FALSE,
    user_edit BOOLEAN NOT NULL DEFAULT FALSE,
    user_group_add BOOLEAN NOT NULL DEFAULT FALSE,
    user_group_delete BOOLEAN NOT NULL DEFAULT FALSE,
    user_group_edit BOOLEAN NOT NULL DEFAULT FALSE,
    user_account_add BOOLEAN NOT NULL DEFAULT FALSE,
    user_account_delete BOOLEAN NOT NULL DEFAULT FALSE,
    user_account_edit BOOLEAN NOT NULL DEFAULT FALSE,
    user_rank_add BOOLEAN NOT NULL DEFAULT FALSE,
    user_rank_delete BOOLEAN NOT NULL DEFAULT FALSE,
    user_rank_edit BOOLEAN NOT NULL DEFAULT FALSE,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_edit TIMESTAMP
);

-- Table: users
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    rank_id BIGINT NOT NULL,
    email VARCHAR(255) UNIQUE DEFAULT NULL,
    banned BOOLEAN NOT NULL DEFAULT FALSE,
    activated BOOLEAN NOT NULL DEFAULT FALSE,
    activation_key VARCHAR(255) DEFAULT NULL,
    last_ip VARCHAR(45) DEFAULT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_edit TIMESTAMP,
    FOREIGN KEY (rank_id) REFERENCES user_ranks(id) ON UPDATE CASCADE
);

-- Table: post_categories
CREATE TABLE post_categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_edit TIMESTAMP,
    parent_id BIGINT DEFAULT NULL,
    FOREIGN KEY (parent_id) REFERENCES post_categories(id) ON DELETE SET NULL ON UPDATE CASCADE
);

-- Table: posts
CREATE TABLE posts (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    user_id BIGINT,
    category_id BIGINT,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    commentable BOOLEAN NOT NULL DEFAULT FALSE,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_edit TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL ON UPDATE CASCADE,
    FOREIGN KEY (category_id) REFERENCES post_categories(id) ON DELETE SET NULL ON UPDATE CASCADE
);

-- Full-text search index for posts content
CREATE INDEX idx_posts_content_fts ON posts USING gin(to_tsvector('english', content));

-- Table: post_comments
CREATE TABLE post_comments (
    id BIGSERIAL PRIMARY KEY,
    content TEXT NOT NULL,
    post_id BIGINT NOT NULL,
    user_id BIGINT,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_edit TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL ON UPDATE CASCADE,
    FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE ON UPDATE CASCADE
);

-- Full-text search index for post_comments content
CREATE INDEX idx_post_comments_content_fts ON post_comments USING gin(to_tsvector('english', content));

-- Table: user_accounts
CREATE TABLE user_accounts (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_edit TIMESTAMP
);

-- Table: user_accounts_meta
CREATE TABLE user_accounts_meta (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_edit TIMESTAMP,
    is_negative BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE(user_id, account_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (account_id) REFERENCES user_accounts(id) ON DELETE CASCADE ON UPDATE CASCADE
);

-- Table: user_groups
CREATE TABLE user_groups (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_edit TIMESTAMP
);

-- Table: user_groups_meta
CREATE TABLE user_groups_meta (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    group_id BIGINT NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_edit TIMESTAMP,
    UNIQUE(user_id, group_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (group_id) REFERENCES user_groups(id) ON DELETE CASCADE ON UPDATE CASCADE
);

-- Table: user_payments
CREATE TABLE user_payments (
    id BIGSERIAL PRIMARY KEY,
    target_id BIGINT,
    source_id BIGINT,
    user_id BIGINT,
    comment VARCHAR(255) NOT NULL,
    date DATE NOT NULL DEFAULT CURRENT_DATE,
    amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_edit TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL ON UPDATE CASCADE,
    FOREIGN KEY (source_id) REFERENCES user_accounts(id) ON DELETE SET NULL ON UPDATE CASCADE,
    FOREIGN KEY (target_id) REFERENCES user_accounts(id) ON DELETE SET NULL ON UPDATE CASCADE
);

-- Insert initial data for user_ranks
INSERT INTO user_ranks (id, name, admin_view, page_add, page_delete, page_edit, post_add, post_delete, post_edit, post_category_add, post_category_delete, post_category_edit, post_comment_add, post_comment_delete, post_comment_edit, user_add, user_delete, user_edit, user_group_add, user_group_delete, user_group_edit, user_account_add, user_account_delete, user_account_edit, user_rank_add, user_rank_delete, user_rank_edit) VALUES
(1, 'Administrator', TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE),
(2, 'User', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),
(3, 'Guest', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),
(4, 'Moderator', TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE);

-- Insert initial data for users
INSERT INTO users (id, username, password, rank_id, activated, banned, activation_key) VALUES
(1, 'admin', '$2a$10$RfPCTCppilPN9dR.aYkYr.nFWs2SiujkCXNRAiZtjClKkJqSMqJwW', 1, TRUE, FALSE, NULL),
(2, 'user', '$2a$10$0kzodxfrJWo27njgGaCEEO8SQZryAL1Cn6X1kQ1UbLbnUdmuQ0kkC', 2, TRUE, FALSE, NULL),
(3, 'guest', '$2a$10$wpRNBD1wYDcKraH8aAqvi.lgzDCq183EQ2/J131P5toJoCOVZvkIe', 3, TRUE, FALSE, NULL),
(4, 'moderator', '$2a$10$hIKFIOu0kaXeUx.o..D/d.FadO1ez7J4HV.mQjtxW3kU5zO.IdhV2', 4, TRUE, FALSE, NULL),
(5, 'bannedUser', '$2a$10$f3D8iQlSEvWRgZY49vV3se2yLlFxxIytj5c/kZfINGIBPV2YyXvti', 3, TRUE, TRUE, NULL),
(6, 'notActivatedUser', '$2a$10$4mLZfGWvIE1/x/AHyy2lLOqaWeL9mzpQ.vnNjxfsNUue4hkbPDIbC', 3, FALSE, FALSE, '21b8efb5-8eb0-4530-9d9a-cdbc33ba7164');

-- Update sequences to continue from current max values
SELECT setval('user_ranks_id_seq', 4);
SELECT setval('users_id_seq', 6);

-- Trigger function for automatic last_edit management
CREATE OR REPLACE FUNCTION update_last_edit() RETURNS TRIGGER LANGUAGE plpgsql AS 'BEGIN NEW.last_edit = clock_timestamp(); RETURN NEW; END';

CREATE OR REPLACE TRIGGER pages_set_last_edit
    BEFORE UPDATE ON pages
    FOR EACH ROW
    EXECUTE FUNCTION update_last_edit();

CREATE OR REPLACE TRIGGER post_categories_set_last_edit
    BEFORE UPDATE ON post_categories
    FOR EACH ROW
    EXECUTE FUNCTION update_last_edit();

CREATE OR REPLACE TRIGGER post_comments_set_last_edit
    BEFORE UPDATE ON post_comments
    FOR EACH ROW
    EXECUTE FUNCTION update_last_edit();

CREATE OR REPLACE TRIGGER posts_set_last_edit
    BEFORE UPDATE ON posts
    FOR EACH ROW
    EXECUTE FUNCTION update_last_edit();

CREATE OR REPLACE TRIGGER user_accounts_set_last_edit
    BEFORE UPDATE ON user_accounts
    FOR EACH ROW
    EXECUTE FUNCTION update_last_edit();

CREATE OR REPLACE TRIGGER user_payments_set_last_edit
    BEFORE UPDATE ON user_payments
    FOR EACH ROW
    EXECUTE FUNCTION update_last_edit();

CREATE OR REPLACE TRIGGER user_ranks_set_last_edit
    BEFORE UPDATE ON user_ranks
    FOR EACH ROW
    EXECUTE FUNCTION update_last_edit();

CREATE OR REPLACE TRIGGER users_set_last_edit
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_last_edit();

CREATE OR REPLACE TRIGGER user_groups_set_last_edit
    BEFORE UPDATE ON user_groups
    FOR EACH ROW
    EXECUTE FUNCTION update_last_edit();

CREATE OR REPLACE TRIGGER user_accounts_meta_set_last_edit
    BEFORE UPDATE ON user_accounts_meta
    FOR EACH ROW
    EXECUTE FUNCTION update_last_edit();

CREATE OR REPLACE TRIGGER user_groups_meta_set_last_edit
    BEFORE UPDATE ON user_groups_meta
    FOR EACH ROW
    EXECUTE FUNCTION update_last_edit();

-- --------------------------------------------------------
-- Performance optimization indexes
-- --------------------------------------------------------

-- Basic indexes for user account metadata queries
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_user_accounts_meta_user_id ON user_accounts_meta (user_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_user_accounts_meta_account_id ON user_accounts_meta (account_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_user_accounts_meta_user_account ON user_accounts_meta (user_id, account_id);

-- Indexes for payment queries (most critical for performance)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_user_payments_source_account ON user_payments (source_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_user_payments_target_account ON user_payments (target_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_user_payments_created ON user_payments (created DESC);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_user_payments_date ON user_payments (date DESC);

-- PostgreSQL-specific composite indexes for optimized balance calculations
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_user_payments_source_amount ON user_payments (source_id, amount);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_user_payments_target_amount ON user_payments (target_id, amount);

-- PostgreSQL covering index with INCLUDE clause for non-key columns
-- This is a PostgreSQL-specific feature that's very efficient for balance calculations
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_user_payments_balance_calc 
ON user_payments (target_id, source_id) 
INCLUDE (amount, created, date);

-- User authentication indexes
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_users_username ON users (username);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_user_accounts_name ON user_accounts (name);

-- User group metadata indexes
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_user_groups_meta_user_id ON user_groups_meta (user_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_user_groups_meta_group_id ON user_groups_meta (group_id);

-- PostgreSQL partial index for active users (very efficient)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_users_active 
ON users (id) 
WHERE activated = true AND banned = false;

-- --------------------------------------------------------
-- Full-text search indexes for commonly searched columns
-- --------------------------------------------------------

-- Trigram indexes support fast LIKE / ILIKE queries
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Post search indexes (title, content, username)
CREATE INDEX IF NOT EXISTS idx_post_title_trgm ON posts USING gin (LOWER(title) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_post_content_trgm ON posts USING gin (LOWER(content) gin_trgm_ops);

-- Post comment search indexes
CREATE INDEX IF NOT EXISTS idx_post_comment_content_trgm ON post_comments USING gin (LOWER(content) gin_trgm_ops);

-- Post category search index
CREATE INDEX IF NOT EXISTS idx_post_category_name_trgm ON post_categories USING gin (LOWER(name) gin_trgm_ops);

-- User search indexes
CREATE INDEX IF NOT EXISTS idx_user_username_trgm ON users USING gin (LOWER(username) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_user_email_trgm ON users USING gin (LOWER(email) gin_trgm_ops);

-- User group search index
CREATE INDEX IF NOT EXISTS idx_user_group_name_trgm ON user_groups USING gin (LOWER(name) gin_trgm_ops);

-- User rank search index
CREATE INDEX IF NOT EXISTS idx_user_rank_name_trgm ON user_ranks USING gin (LOWER(name) gin_trgm_ops);

-- Account search index
CREATE INDEX IF NOT EXISTS idx_account_name_trgm ON user_accounts USING gin (LOWER(name) gin_trgm_ops);

-- Payment search indexes (comment, account names joined)
CREATE INDEX IF NOT EXISTS idx_payment_comment_trgm ON user_payments USING gin (LOWER(comment) gin_trgm_ops);

-- Page search indexes
CREATE INDEX IF NOT EXISTS idx_page_title_trgm ON pages USING gin (LOWER(title) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_page_content_trgm ON pages USING gin (LOWER(content) gin_trgm_ops);
