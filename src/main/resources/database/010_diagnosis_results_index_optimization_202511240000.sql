-- =====================================================
-- Migration: Diagnosis Results Index Optimization
-- Date: 2025-11-24
-- Purpose: Optimize queries for concurrent diagnosis creation and race condition prevention
-- =====================================================

-- =====================================================
-- 1. Drop existing indexes that will be replaced with optimized versions
-- =====================================================
-- Note: idx_diagnosis_member_status already exists and will be kept

-- =====================================================
-- 2. Add optimized composite index for findInProgressDiagnosisWithLock query
-- =====================================================
-- This index optimizes the query:
-- SELECT * FROM diagnosis_results WHERE member_id = ? AND status IN (?, ?, ?) ORDER BY created_at DESC
-- The index order (member_id, status, created_at DESC) matches the query pattern perfectly
CREATE INDEX IF NOT EXISTS idx_diagnosis_member_status_created
ON diagnosis_results(member_id, status, created_at DESC);

-- =====================================================
-- 3. Concurrency Control Notes
-- =====================================================
-- MariaDB Limitation:
-- - MariaDB does not support partial/filtered indexes (PostgreSQL's WHERE clause in CREATE INDEX)
-- - Cannot create unique constraint only for active diagnoses (IN_PROGRESS, PENDING, AWAITING_USER_INPUT)
--
-- Race Condition Prevention Strategy (Application Level):
-- 1. Pessimistic locking (SELECT ... FOR UPDATE) in findInProgressDiagnosisWithLock()
-- 2. Direct creation with IN_PROGRESS status (skip PENDING state)
-- 3. saveAndFlush() for immediate database commit
-- 4. DataIntegrityViolationException handling as fallback
--
-- Alternative (if needed in future):
-- - Add virtual column: is_active BOOLEAN AS (status IN ('IN_PROGRESS', 'PENDING', 'AWAITING_USER_INPUT'))
-- - Create unique index: CREATE UNIQUE INDEX idx_diagnosis_member_active ON diagnosis_results(member_id, is_active)
-- - However, this adds complexity and virtual columns have performance overhead
--
-- Current approach (pessimistic lock + saveAndFlush) is sufficient for the expected load.
-- =====================================================

-- =====================================================
-- 4. Verify Index Creation
-- =====================================================
-- Run this query to verify the new index was created:
-- SHOW INDEX FROM diagnosis_results WHERE Key_name = 'idx_diagnosis_member_status_created';
