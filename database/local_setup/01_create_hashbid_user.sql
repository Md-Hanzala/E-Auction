-- ============================================================================
-- Script 01: Create Dedicated HASHBID User and Schema in Oracle
-- Target Database: Oracle Database 10g Express Edition (XE) (also 11g/19c/21c XE)
-- Execution Role: Run this script while connected as SYSTEM or SYS AS SYSDBA
-- ============================================================================
-- SAFETY & TRANSACTION SEMANTICS:
-- 1. DDL IMPLICIT COMMIT NOTICE:
--    In Oracle, DDL statements (CREATE USER, GRANT, ALTER USER) issue an implicit
--    COMMIT before and after execution. "WHENEVER SQLERROR EXIT FAILURE ROLLBACK"
--    halts execution on failure, but any previously executed DDL statement remains
--    committed in the database.
-- 2. NON-DESTRUCTIVE GUARD:
--    This script checks "all_users" and aborts immediately if user HASHBID already
--    exists. It will never overwrite, alter, or drop an existing account.
-- 3. INTERACTIVE CREDENTIALS:
--    Replace <REPLACE_WITH_YOUR_LOCAL_PASSWORD> with your preferred local password.
--    When logging in via SQL*Plus, connect without inline passwords to keep them
--    out of terminal history.
-- ============================================================================

-- Halt execution immediately on any SQL or OS error
WHENEVER SQLERROR EXIT FAILURE ROLLBACK;
WHENEVER OSERROR EXIT FAILURE;

SET SERVEROUTPUT ON;
SET ECHO OFF;
SET FEEDBACK OFF;

PROMPT --------------------------------------------------------
PROMPT Step 1: Pre-execution safety check...
PROMPT --------------------------------------------------------

-- Abort if user HASHBID already exists in Oracle dictionary
DECLARE
    v_user_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_user_count
    FROM all_users
    WHERE username = 'HASHBID';

    IF v_user_count > 0 THEN
        RAISE_APPLICATION_ERROR(
            -20001,
            'ABORTING: User HASHBID already exists. This script is non-destructive and will not overwrite or alter an existing user account.'
        );
    END IF;
END;
/

PROMPT Safety check passed: User HASHBID does not exist.
PROMPT
PROMPT --------------------------------------------------------
PROMPT Step 2: Creating dedicated HASHBID application user...
PROMPT --------------------------------------------------------

-- IMPORTANT: Replace the placeholder below with your chosen local password before running.
-- Example: CREATE USER HASHBID IDENTIFIED BY "hashbid123"
CREATE USER HASHBID IDENTIFIED BY "<YOUR_LOCAL_PASSWORD>"
    DEFAULT TABLESPACE USERS
    TEMPORARY TABLESPACE TEMP
    QUOTA UNLIMITED ON USERS;

PROMPT --------------------------------------------------------
PROMPT Step 3: Granting least-privilege permissions...
PROMPT --------------------------------------------------------

-- Minimum required privileges for application operation:
-- CREATE SESSION : Allows login via SQL*Plus and JDBC connections
-- CREATE TABLE   : Allows creating application tables and constraints
-- CREATE SEQUENCE: Allows creating ID generators (USER_SEQ, etc.)
-- CREATE VIEW    : Allows creating application views if required
GRANT CREATE SESSION TO HASHBID;
GRANT CREATE TABLE TO HASHBID;
GRANT CREATE SEQUENCE TO HASHBID;
GRANT CREATE VIEW TO HASHBID;

SET FEEDBACK ON;

PROMPT
PROMPT ========================================================
PROMPT SUCCESS: User HASHBID created and granted required privileges.
PROMPT ========================================================
PROMPT Next steps:
PROMPT   1. Connect interactively as HASHBID (SQL*Plus will prompt for password):
PROMPT      CONNECT HASHBID@XE;
PROMPT   2. Execute the schema initialization script:
PROMPT      @02_create_hashbid_schema_and_data.sql
PROMPT ========================================================
