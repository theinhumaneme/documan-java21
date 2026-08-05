-- One-off migration for databases created before votes and favourites moved to explicit join
-- tables with denormalised counters.
--
-- `ddl-auto: update` only ever adds; it will create the new tables and columns but will not move
-- the existing rows or drop the old @ManyToMany join tables. Run this once, after starting the
-- application against an existing database so the new tables exist.
--
-- Safe to run on a fresh database: every step is guarded and skips when the legacy tables are
-- absent. Safe to re-run: inserts are ON CONFLICT DO NOTHING and counters are recomputed, not
-- incremented.

BEGIN;

-- ------------------------------------------------------------------ column rename
-- `@Column(name = "isVerified")` folded to `isverified` in PostgreSQL; the column is now is_verified.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'documan_user' AND column_name = 'isverified')
       AND EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'documan_user' AND column_name = 'is_verified')
    THEN
        UPDATE documan_user SET is_verified = isverified;
        ALTER TABLE documan_user DROP COLUMN isverified;
    ELSIF EXISTS (SELECT 1 FROM information_schema.columns
                  WHERE table_name = 'documan_user' AND column_name = 'isverified')
    THEN
        ALTER TABLE documan_user RENAME COLUMN isverified TO is_verified;
    END IF;
END $$;

-- ------------------------------------------------------------------ post votes
-- Upvotes are inserted first so that a user who somehow appears in both legacy tables (the old
-- code could leave that state behind) resolves to an upvote rather than failing the unique
-- constraint.
DO $$
BEGIN
    IF to_regclass('public.upvoted_posts') IS NOT NULL THEN
        INSERT INTO post_vote (post_id, user_id, vote_type, date_created)
        SELECT post_id, user_id, 'UPVOTE', now() FROM upvoted_posts
        ON CONFLICT ON CONSTRAINT uq_post_vote_post_user DO NOTHING;
    END IF;

    IF to_regclass('public.downvoted_posts') IS NOT NULL THEN
        INSERT INTO post_vote (post_id, user_id, vote_type, date_created)
        SELECT post_id, user_id, 'DOWNVOTE', now() FROM downvoted_posts
        ON CONFLICT ON CONSTRAINT uq_post_vote_post_user DO NOTHING;
    END IF;

    IF to_regclass('public.favourite_posts') IS NOT NULL THEN
        INSERT INTO favourite_post (post_id, user_id, date_created)
        SELECT post_id, user_id, now() FROM favourite_posts
        ON CONFLICT ON CONSTRAINT uq_favourite_post_post_user DO NOTHING;
    END IF;

    IF to_regclass('public.upvoted_comments') IS NOT NULL THEN
        INSERT INTO comment_vote (comment_id, user_id, vote_type, date_created)
        SELECT comment_id, user_id, 'UPVOTE', now() FROM upvoted_comments
        ON CONFLICT ON CONSTRAINT uq_comment_vote_comment_user DO NOTHING;
    END IF;

    IF to_regclass('public.downvoted_comments') IS NOT NULL THEN
        INSERT INTO comment_vote (comment_id, user_id, vote_type, date_created)
        SELECT comment_id, user_id, 'DOWNVOTE', now() FROM downvoted_comments
        ON CONFLICT ON CONSTRAINT uq_comment_vote_comment_user DO NOTHING;
    END IF;

    IF to_regclass('public.favourite_files') IS NOT NULL THEN
        INSERT INTO favourite_file (file_id, user_id, date_created)
        SELECT file_id, user_id, now() FROM favourite_files
        ON CONFLICT ON CONSTRAINT uq_favourite_file_file_user DO NOTHING;
    END IF;
END $$;

-- ------------------------------------------------------------------ counter backfill
-- Recomputed from the join tables rather than incremented, so this is idempotent.
UPDATE post p
   SET upvote_count    = COALESCE(v.up, 0),
       downvote_count  = COALESCE(v.down, 0),
       favourite_count = COALESCE(f.favourites, 0)
  FROM (SELECT id AS post_id,
               (SELECT count(*) FROM post_vote pv
                 WHERE pv.post_id = post.id AND pv.vote_type = 'UPVOTE')   AS up,
               (SELECT count(*) FROM post_vote pv
                 WHERE pv.post_id = post.id AND pv.vote_type = 'DOWNVOTE') AS down
          FROM post) v
  LEFT JOIN (SELECT post_id, count(*) AS favourites FROM favourite_post GROUP BY post_id) f
         ON f.post_id = v.post_id
 WHERE p.id = v.post_id;

UPDATE comment c
   SET upvote_count   = COALESCE(v.up, 0),
       downvote_count = COALESCE(v.down, 0)
  FROM (SELECT id AS comment_id,
               (SELECT count(*) FROM comment_vote cv
                 WHERE cv.comment_id = comment.id AND cv.vote_type = 'UPVOTE')   AS up,
               (SELECT count(*) FROM comment_vote cv
                 WHERE cv.comment_id = comment.id AND cv.vote_type = 'DOWNVOTE') AS down
          FROM comment) v
 WHERE c.id = v.comment_id;

UPDATE file fl
   SET favourite_count = COALESCE(f.favourites, 0)
  FROM (SELECT id AS file_id FROM file) ids
  LEFT JOIN (SELECT file_id, count(*) AS favourites FROM favourite_file GROUP BY file_id) f
         ON f.file_id = ids.file_id
 WHERE fl.id = ids.file_id;

-- ------------------------------------------------------------------ drop the legacy tables
DROP TABLE IF EXISTS upvoted_posts;
DROP TABLE IF EXISTS downvoted_posts;
DROP TABLE IF EXISTS favourite_posts;
DROP TABLE IF EXISTS upvoted_comments;
DROP TABLE IF EXISTS downvoted_comments;
DROP TABLE IF EXISTS favourite_files;

COMMIT;
