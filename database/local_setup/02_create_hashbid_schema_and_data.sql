-- ============================================================================
-- Script 02: One-Time Initialization of HASHbid Schema, Sequences, and Data
-- Target Database: Oracle Database 10g Express Edition (XE) (also 11g/19c/21c XE)
-- Execution Role: Run this script while connected as HASHBID
-- Prerequisites: Run 01_create_hashbid_user.sql first as SYSTEM/DBA
-- ============================================================================
-- SAFETY & TRANSACTION SEMANTICS (ORACLE DDL BEHAVIOUR):
-- 1. NON-ATOMIC SCHEMA INITIALIZATION:
--    In Oracle, every DDL statement (CREATE TABLE, CREATE INDEX, CREATE SEQUENCE)
--    causes an IMPLICIT COMMIT before and after execution. Therefore, the overall
--    initialization script is NOT a single atomic transaction.
-- 2. PARTIAL FAILURE BEHAVIOUR:
--    "WHENEVER SQLERROR EXIT FAILURE ROLLBACK" guarantees that if any statement
--    fails, execution halts immediately and any uncommitted INSERT (DML) is
--    rolled back. However, any DDL objects created prior to the failure point
--    REMAIN COMMITTED in the Oracle data dictionary.
-- 3. STRICT NON-DESTRUCTIVE GUARD:
--    This script contains NO "DROP TABLE" or "DROP SEQUENCE" statements. If any
--    target table or sequence already exists, it aborts immediately.
-- 4. MANUAL RECOVERY PROCEDURE:
--    If this script stops midway due to an error, DO NOT attempt to run it again
--    directly (it will halt because earlier tables already exist). Instead, follow
--    the documented manual recovery procedure in README.md: connect as SYSTEM and
--    re-create a clean, empty HASHBID schema before retrying.
-- ============================================================================

-- Halt execution immediately on error; rollback uncommitted DML
WHENEVER SQLERROR EXIT FAILURE ROLLBACK;
WHENEVER OSERROR EXIT FAILURE;

SET SERVEROUTPUT ON;
SET DEFINE OFF;
SET SQLBLANKLINES ON;
SET ECHO OFF;
SET FEEDBACK OFF;

-- Standardize timestamp and date language to ensure predictable parsing
ALTER SESSION SET NLS_DATE_LANGUAGE = 'AMERICAN';
ALTER SESSION SET NLS_TIMESTAMP_FORMAT = 'YYYY-MM-DD HH24:MI:SS.FF';

PROMPT ========================================================
PROMPT HASHbid One-Time Schema Initialization
PROMPT ========================================================

-- ----------------------------------------------------------------------------
-- STEP 1: PRE-EXECUTION VALIDATION AND SAFETY CHECKS
-- Runs entirely in memory BEFORE any DDL statements execute
-- ----------------------------------------------------------------------------
PROMPT Running pre-execution validation checks...

DECLARE
    v_table_count NUMBER;
    v_seq_count   NUMBER;
    v_test_ts     TIMESTAMP(6);
BEGIN
    -- 1. Verify connection is as HASHBID, preventing accidental execution as SYSTEM/SYS
    IF USER != 'HASHBID' THEN
        RAISE_APPLICATION_ERROR(
            -20001,
            'ABORTING: You must run this script while connected as HASHBID. Current user is: ' || USER
        );
    END IF;

    -- 2. Check for pre-existing application tables
    SELECT COUNT(*) INTO v_table_count
    FROM user_tables
    WHERE table_name IN ('USERS', 'PRODUCTS', 'AUCTIONS', 'BIDS', 'SETTLEMENTS');

    IF v_table_count > 0 THEN
        RAISE_APPLICATION_ERROR(
            -20002,
            'ABORTING: One or more HASHbid tables (USERS, PRODUCTS, AUCTIONS, BIDS, SETTLEMENTS) already exist in this schema. This initialization script is non-destructive and will not overwrite or alter existing data.'
        );
    END IF;

    -- 3. Check for pre-existing application sequences
    SELECT COUNT(*) INTO v_seq_count
    FROM user_sequences
    WHERE sequence_name IN ('USER_SEQ', 'PRODUCT_SEQ', 'AUCTION_SEQ', 'BID_SEQ');

    IF v_seq_count > 0 THEN
        RAISE_APPLICATION_ERROR(
            -20003,
            'ABORTING: One or more HASHbid sequences (USER_SEQ, PRODUCT_SEQ, AUCTION_SEQ, BID_SEQ) already exist in this schema. Initialization stopped.'
        );
    END IF;

    -- 4. Pre-validate timestamp parsing formats used in sample data inserts
    BEGIN
        v_test_ts := TO_TIMESTAMP('02-09-26 6:51:13.712000000 PM', 'DD-MM-RR fmHH12:fmMI:SSXFF AM');
        v_test_ts := TO_TIMESTAMP('11-11-26 8:49:03.873000000 PM', 'DD-MM-RR fmHH12:fmMI:SSXFF AM');
    EXCEPTION
        WHEN OTHERS THEN
            RAISE_APPLICATION_ERROR(
                -20004,
                'ABORTING: Timestamp format validation failed under current session NLS settings. ' || SQLERRM
            );
    END;
END;
/

PROMPT Pre-execution validation passed: Schema is clean and ready.
PROMPT

-- ----------------------------------------------------------------------------
-- STEP 2: CREATE APPLICATION TABLES (IN RELATIONAL DEPENDENCY ORDER)
-- Notice: Each CREATE TABLE causes an implicit COMMIT in Oracle
-- ----------------------------------------------------------------------------

PROMPT [1/5] Creating table: USERS...
CREATE TABLE USERS (
    USER_ID     NUMBER,
    FULL_NAME   VARCHAR2(100) NOT NULL,
    EMAIL       VARCHAR2(120) NOT NULL,
    PHONE       VARCHAR2(20),
    PASSWORD    VARCHAR2(255) NOT NULL,
    ROLE        VARCHAR2(20)  NOT NULL,
    STATUS      VARCHAR2(20)  DEFAULT 'PENDING' NOT NULL,
    ADDRESS     VARCHAR2(400),
    CREATED_AT  TIMESTAMP(6)  DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT PK_USERS PRIMARY KEY (USER_ID),
    CONSTRAINT UQ_USERS_EMAIL UNIQUE (EMAIL)
);

PROMPT [2/5] Creating table: PRODUCTS...
CREATE TABLE PRODUCTS (
    PRODUCT_ID      NUMBER,
    SELLER_ID       NUMBER         NOT NULL,
    PRODUCT_NAME    VARCHAR2(150)  NOT NULL,
    CATEGORY        VARCHAR2(60)   NOT NULL,
    DESCRIPTION     VARCHAR2(1000),
    STARTING_PRICE  NUMBER(12,2)   NOT NULL,
    IMAGE_PATH      VARCHAR2(255),
    STATUS          VARCHAR2(20)   DEFAULT 'PENDING' NOT NULL,
    CREATED_AT      TIMESTAMP(6)   DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT PK_PRODUCTS PRIMARY KEY (PRODUCT_ID),
    CONSTRAINT FK_PRODUCTS_SELLER FOREIGN KEY (SELLER_ID) REFERENCES USERS (USER_ID)
);

PROMPT [3/5] Creating table: AUCTIONS...
CREATE TABLE AUCTIONS (
    AUCTION_ID          NUMBER,
    PRODUCT_ID          NUMBER        NOT NULL,
    START_TIME          TIMESTAMP(6)  NOT NULL,
    END_TIME            TIMESTAMP(6)  NOT NULL,
    CURRENT_BID         NUMBER(12,2)  NOT NULL,
    HIGHEST_BIDDER_ID   NUMBER,
    MIN_INCREMENT       NUMBER(10,2)  DEFAULT 50.00 NOT NULL,
    EXECUTION_MODE      VARCHAR2(20)  DEFAULT 'AUTOMATIC' NOT NULL,
    STATUS              VARCHAR2(20)  DEFAULT 'ACTIVE' NOT NULL,
    CONSTRAINT PK_AUCTIONS PRIMARY KEY (AUCTION_ID),
    CONSTRAINT FK_AUCTIONS_PRODUCT FOREIGN KEY (PRODUCT_ID) REFERENCES PRODUCTS (PRODUCT_ID),
    CONSTRAINT FK_AUCTIONS_BIDDER  FOREIGN KEY (HIGHEST_BIDDER_ID) REFERENCES USERS (USER_ID)
);

PROMPT [4/5] Creating table: BIDS...
CREATE TABLE BIDS (
    BID_ID      NUMBER,
    AUCTION_ID  NUMBER        NOT NULL,
    BIDDER_ID   NUMBER        NOT NULL,
    BID_AMOUNT  NUMBER(12,2)  NOT NULL,
    BID_TIME    TIMESTAMP(6)  DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT PK_BIDS PRIMARY KEY (BID_ID),
    CONSTRAINT FK_BIDS_AUCTION FOREIGN KEY (AUCTION_ID) REFERENCES AUCTIONS (AUCTION_ID),
    CONSTRAINT FK_BIDS_BIDDER  FOREIGN KEY (BIDDER_ID) REFERENCES USERS (USER_ID)
);

PROMPT [5/5] Creating table: SETTLEMENTS...
CREATE TABLE SETTLEMENTS (
    SETTLEMENT_ID   NUMBER,
    AUCTION_ID      NUMBER        NOT NULL,
    GROSS_AMOUNT    NUMBER(12,2)  NOT NULL,
    PLATFORM_FEE    NUMBER(12,2)  NOT NULL,
    SELLER_PAYOUT   NUMBER(12,2)  NOT NULL,
    PAYMENT_STATUS  VARCHAR2(20)  DEFAULT 'PENDING' NOT NULL,
    SETTLED_AT      TIMESTAMP(6)  DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT PK_SETTLEMENTS PRIMARY KEY (SETTLEMENT_ID),
    CONSTRAINT FK_SETTLEMENTS_AUCTION FOREIGN KEY (AUCTION_ID) REFERENCES AUCTIONS (AUCTION_ID)
);

-- ----------------------------------------------------------------------------
-- STEP 3: CREATE PERFORMANCE INDEXES FOR FOREIGN KEYS
-- ----------------------------------------------------------------------------
PROMPT Creating foreign key indexes...
CREATE INDEX IDX_PRODUCTS_SELLER     ON PRODUCTS(SELLER_ID);
CREATE INDEX IDX_AUCTIONS_PRODUCT    ON AUCTIONS(PRODUCT_ID);
CREATE INDEX IDX_AUCTIONS_BIDDER     ON AUCTIONS(HIGHEST_BIDDER_ID);
CREATE INDEX IDX_BIDS_AUCTION        ON BIDS(AUCTION_ID);
CREATE INDEX IDX_BIDS_BIDDER         ON BIDS(BIDDER_ID);
CREATE INDEX IDX_SETTLEMENTS_AUCTION ON SETTLEMENTS(AUCTION_ID);

-- ----------------------------------------------------------------------------
-- STEP 4: CREATE APPLICATION SEQUENCES
-- Reconstructed local development sequences initialized after sample IDs
-- ----------------------------------------------------------------------------
PROMPT Creating application sequences...
-- Existing sample USERS: max ID = 4 -> Sequence starts at 5
CREATE SEQUENCE USER_SEQ
    START WITH 5
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

-- Existing sample PRODUCTS: max ID = 7 -> Sequence starts at 8
CREATE SEQUENCE PRODUCT_SEQ
    START WITH 8
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

-- Existing sample AUCTIONS: max ID = 7 -> Sequence starts at 8
CREATE SEQUENCE AUCTION_SEQ
    START WITH 8
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

-- Existing sample BIDS: max ID = 10 -> Sequence starts at 11
CREATE SEQUENCE BID_SEQ
    START WITH 11
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

-- ----------------------------------------------------------------------------
-- STEP 5: IMPORT VERIFIED HASHBID SAMPLE DATA
-- Data source: Verified export from EAuction_Database.sql
-- Relational order: USERS -> PRODUCTS -> AUCTIONS -> BIDS -> SETTLEMENTS
-- ----------------------------------------------------------------------------

PROMPT Importing sample demonstration USERS (4 records)...
-- Note: All sample user accounts, phone numbers, and addresses are fictional testing fixtures.
-- These demo credentials are provided strictly for local development evaluation.
INSERT INTO USERS (USER_ID, FULL_NAME, EMAIL, PHONE, PASSWORD, ROLE, STATUS, ADDRESS, CREATED_AT)
VALUES (1, 'System Administrator', 'admin@hashbid.com', '+91 90000 00001', 'admin123', 'ADMIN', 'APPROVED', 'HASHbid Central Operations HQ', TO_TIMESTAMP('02-09-26 6:51:13.712000000 PM', 'DD-MM-RR fmHH12:fmMI:SSXFF AM'));

INSERT INTO USERS (USER_ID, FULL_NAME, EMAIL, PHONE, PASSWORD, ROLE, STATUS, ADDRESS, CREATED_AT)
VALUES (2, 'Zaid Merchant', 'zaid.seller@example.com', '+91 90000 00002', 'seller123', 'SELLER', 'APPROVED', '123 Commercial Avenue, Bangalore', TO_TIMESTAMP('02-09-26 7:27:44.094000000 PM', 'DD-MM-RR fmHH12:fmMI:SSXFF AM'));

INSERT INTO USERS (USER_ID, FULL_NAME, EMAIL, PHONE, PASSWORD, ROLE, STATUS, ADDRESS, CREATED_AT)
VALUES (3, 'Aman Buyer', 'aman.buyer@example.com', '+91 90000 00003', 'buyer123', 'BUYER', 'APPROVED', 'Flat 4B, Green Glen Layout, Bangalore', TO_TIMESTAMP('02-09-26 7:40:12.904000000 PM', 'DD-MM-RR fmHH12:fmMI:SSXFF AM'));

INSERT INTO USERS (USER_ID, FULL_NAME, EMAIL, PHONE, PASSWORD, ROLE, STATUS, ADDRESS, CREATED_AT)
VALUES (4, 'Farhan Contestant', 'farhan.buyer@example.com', '+91 90000 00004', 'buyer123', 'BUYER', 'APPROVED', '45 Market Street, Suite 2B, Kolkata', TO_TIMESTAMP('04-09-26 5:50:49.827000000 PM', 'DD-MM-RR fmHH12:fmMI:SSXFF AM'));

PROMPT Importing sample PRODUCTS (7 records)...
INSERT INTO PRODUCTS (PRODUCT_ID, SELLER_ID, PRODUCT_NAME, CATEGORY, DESCRIPTION, STARTING_PRICE, IMAGE_PATH, STATUS, CREATED_AT)
VALUES (1, 2, 'DEAR', 'Art & Paintings', 'goood', 2000, 'uploads/dear.jpg', 'SOLD', TO_TIMESTAMP('02-09-26 8:23:48.101000000 PM', 'DD-MM-RR fmHH12:fmMI:SSXFF AM'));

INSERT INTO PRODUCTS (PRODUCT_ID, SELLER_ID, PRODUCT_NAME, CATEGORY, DESCRIPTION, STARTING_PRICE, IMAGE_PATH, STATUS, CREATED_AT)
VALUES (2, 2, 'Sony Alpha 7 IV Mirrorless Camera', 'Electronics & Cameras',
'Professional-grade 33MP full-frame hybrid camera in mint condition with an ultra-low shutter count of under 4,500 actuations. Designed for both high-end still photography and cinematic video production, featuring 4K 60p 10-bit 4:2:2 recording, advanced real-time AI Eye AF (Human, Animal, Bird), and 5.5-stop in-body optical image stabilization. Always stored in a humidity-controlled dry cabinet with a tempered glass screen protector applied from day one. Sensor: 33MP Full-Frame Exmor R BSI CMOS. Video Capabilities: 4K 60p, S-Cinetone, S-Log3, 10-bit internal recording. Shutter Count: ~4,200 actuations. Cosmetic & Working Condition: 9.8/10. What''s Included: Original packaging and manual, original Sony NP-FZ100 battery, body cap, camera strap, and USB-C power adapter.',
120000, 'uploads/1788521613542_133929306356491186.jpg', 'SOLD', TO_TIMESTAMP('04-09-26 5:03:33.632000000 PM', 'DD-MM-RR fmHH12:fmMI:SSXFF AM'));

INSERT INTO PRODUCTS (PRODUCT_ID, SELLER_ID, PRODUCT_NAME, CATEGORY, DESCRIPTION, STARTING_PRICE, IMAGE_PATH, STATUS, CREATED_AT)
VALUES (3, 2, 'pahad', 'Art & Paintings', 'wow', 3000, 'uploads/1788526515280_133955724542961058.jpg', 'EXPIRED', TO_TIMESTAMP('04-09-26 6:25:15.329000000 PM', 'DD-MM-RR fmHH12:fmMI:SSXFF AM'));

INSERT INTO PRODUCTS (PRODUCT_ID, SELLER_ID, PRODUCT_NAME, CATEGORY, DESCRIPTION, STARTING_PRICE, IMAGE_PATH, STATUS, CREATED_AT)
VALUES (4, 2, 'mountain', 'Art & Paintings', 'wowww', 1000, 'uploads/1788526791561_133955724542961058.jpg', 'SOLD', TO_TIMESTAMP('04-09-26 6:29:51.599000000 PM', 'DD-MM-RR fmHH12:fmMI:SSXFF AM'));

INSERT INTO PRODUCTS (PRODUCT_ID, SELLER_ID, PRODUCT_NAME, CATEGORY, DESCRIPTION, STARTING_PRICE, IMAGE_PATH, STATUS, CREATED_AT)
VALUES (5, 2, 'HIGHWAY', 'Art & Paintings', 'SO BEAUTIFUL SO ELEGENT , JUST LOOKING LIKE A WOW!', 10000, 'uploads/1788590840703_133955724725886807.jpg', 'SOLD', TO_TIMESTAMP('05-09-26 12:17:20.807000000 PM', 'DD-MM-RR fmHH12:fmMI:SSXFF AM'));

INSERT INTO PRODUCTS (PRODUCT_ID, SELLER_ID, PRODUCT_NAME, CATEGORY, DESCRIPTION, STARTING_PRICE, IMAGE_PATH, STATUS, CREATED_AT)
VALUES (6, 2, 'JUNGLE', 'Art & Paintings', 'He made a painting', 2022, 'uploads/1788863415079_image_3.jpg', 'SOLD', TO_TIMESTAMP('08-09-26 4:00:15.192000000 PM', 'DD-MM-RR fmHH12:fmMI:SSXFF AM'));

INSERT INTO PRODUCTS (PRODUCT_ID, SELLER_ID, PRODUCT_NAME, CATEGORY, DESCRIPTION, STARTING_PRICE, IMAGE_PATH, STATUS, CREATED_AT)
VALUES (7, 2, 'APPLE', 'Art & Paintings', 'VERY DELICIOUS', 1000, 'uploads/1794410004319_download__3_.jpeg', 'SOLD', TO_TIMESTAMP('11-11-26 8:43:24.805000000 PM', 'DD-MM-RR fmHH12:fmMI:SSXFF AM'));

PROMPT Importing sample AUCTIONS (7 records)...
INSERT INTO AUCTIONS (AUCTION_ID, PRODUCT_ID, START_TIME, END_TIME, CURRENT_BID, HIGHEST_BIDDER_ID, MIN_INCREMENT, EXECUTION_MODE, STATUS)
VALUES (1, 1, TO_TIMESTAMP('02-09-26 10:31:16.412000000 PM', 'DD-MM-RR fmHH12:fmMI:SSXFF AM'), TO_TIMESTAMP('02-09-26 11:23:32.034000000 PM', 'DD-MM-RR fmHH12:fmMI:SSXFF AM'), 3000, 3, 50, 'AUTOMATIC', 'CLOSED');

INSERT INTO AUCTIONS (AUCTION_ID, PRODUCT_ID, START_TIME, END_TIME, CURRENT_BID, HIGHEST_BIDDER_ID, MIN_INCREMENT, EXECUTION_MODE, STATUS)
VALUES (2, 2, TO_TIMESTAMP('04-09-26 5:04:33.512000000 PM', 'DD-MM-RR fmHH12:fmMI:SSXFF AM'), TO_TIMESTAMP('04-09-26 5:28:48.726000000 PM', 'DD-MM-RR fmHH12:fmMI:SSXFF AM'), 120060, 3, 50, 'AUTOMATIC', 'CLOSED');

INSERT INTO AUCTIONS (AUCTION_ID, PRODUCT_ID, START_TIME, END_TIME, CURRENT_BID, HIGHEST_BIDDER_ID, MIN_INCREMENT, EXECUTION_MODE, STATUS)
VALUES (3, 3, TO_TIMESTAMP('04-09-26 6:25:48.646000000 PM', 'DD-MM-RR fmHH12:fmMI:SSXFF AM'), TO_TIMESTAMP('04-09-26 6:28:41.589000000 PM', 'DD-MM-RR fmHH12:fmMI:SSXFF AM'), 3000, NULL, 50, 'AUTOMATIC', 'CLOSED');

INSERT INTO AUCTIONS (AUCTION_ID, PRODUCT_ID, START_TIME, END_TIME, CURRENT_BID, HIGHEST_BIDDER_ID, MIN_INCREMENT, EXECUTION_MODE, STATUS)
VALUES (4, 4, TO_TIMESTAMP('04-09-26 6:30:35.094000000 PM', 'DD-MM-RR fmHH12:fmMI:SSXFF AM'), TO_TIMESTAMP('04-09-26 6:43:46.449000000 PM', 'DD-MM-RR fmHH12:fmMI:SSXFF AM'), 10000, 4, 50, 'AUTOMATIC', 'CLOSED');

INSERT INTO AUCTIONS (AUCTION_ID, PRODUCT_ID, START_TIME, END_TIME, CURRENT_BID, HIGHEST_BIDDER_ID, MIN_INCREMENT, EXECUTION_MODE, STATUS)
VALUES (5, 5, TO_TIMESTAMP('05-09-26 12:18:02.841000000 PM', 'DD-MM-RR fmHH12:fmMI:SSXFF AM'), TO_TIMESTAMP('06-09-26 12:18:02.841000000 PM', 'DD-MM-RR fmHH12:fmMI:SSXFF AM'), 11000, 3, 50, 'AUTOMATIC', 'CLOSED');

INSERT INTO AUCTIONS (AUCTION_ID, PRODUCT_ID, START_TIME, END_TIME, CURRENT_BID, HIGHEST_BIDDER_ID, MIN_INCREMENT, EXECUTION_MODE, STATUS)
VALUES (6, 6, TO_TIMESTAMP('08-09-26 4:02:21.701000000 PM', 'DD-MM-RR fmHH12:fmMI:SSXFF AM'), TO_TIMESTAMP('08-09-26 5:02:21.701000000 PM', 'DD-MM-RR fmHH12:fmMI:SSXFF AM'), 2100, 3, 50, 'AUTOMATIC', 'CLOSED');

INSERT INTO AUCTIONS (AUCTION_ID, PRODUCT_ID, START_TIME, END_TIME, CURRENT_BID, HIGHEST_BIDDER_ID, MIN_INCREMENT, EXECUTION_MODE, STATUS)
VALUES (7, 7, TO_TIMESTAMP('11-11-26 8:45:56.785000000 PM', 'DD-MM-RR fmHH12:fmMI:SSXFF AM'), TO_TIMESTAMP('11-11-26 8:49:03.873000000 PM', 'DD-MM-RR fmHH12:fmMI:SSXFF AM'), 1100, 3, 100, 'AUTOMATIC', 'CLOSED');

PROMPT Importing sample BIDS (9 records)...
INSERT INTO BIDS (BID_ID, AUCTION_ID, BIDDER_ID, BID_AMOUNT, BID_TIME)
VALUES (2, 4, 4, 1050, TO_TIMESTAMP('04-09-26 6:41:06.415000000 PM', 'DD-MM-RR fmHH12:fmMI:SSXFF AM'));

INSERT INTO BIDS (BID_ID, AUCTION_ID, BIDDER_ID, BID_AMOUNT, BID_TIME)
VALUES (3, 4, 3, 1100, TO_TIMESTAMP('04-09-26 6:41:43.916000000 PM', 'DD-MM-RR fmHH12:fmMI:SSXFF AM'));

INSERT INTO BIDS (BID_ID, AUCTION_ID, BIDDER_ID, BID_AMOUNT, BID_TIME)
VALUES (4, 4, 3, 1150, TO_TIMESTAMP('04-09-26 6:41:56.553000000 PM', 'DD-MM-RR fmHH12:fmMI:SSXFF AM'));

INSERT INTO BIDS (BID_ID, AUCTION_ID, BIDDER_ID, BID_AMOUNT, BID_TIME)
VALUES (5, 4, 3, 1200, TO_TIMESTAMP('04-09-26 6:42:02.534000000 PM', 'DD-MM-RR fmHH12:fmMI:SSXFF AM'));

INSERT INTO BIDS (BID_ID, AUCTION_ID, BIDDER_ID, BID_AMOUNT, BID_TIME)
VALUES (6, 4, 4, 10000, TO_TIMESTAMP('04-09-26 6:42:42.380000000 PM', 'DD-MM-RR fmHH12:fmMI:SSXFF AM'));

INSERT INTO BIDS (BID_ID, AUCTION_ID, BIDDER_ID, BID_AMOUNT, BID_TIME)
VALUES (7, 5, 3, 10050, TO_TIMESTAMP('05-09-26 12:18:46.019000000 PM', 'DD-MM-RR fmHH12:fmMI:SSXFF AM'));

INSERT INTO BIDS (BID_ID, AUCTION_ID, BIDDER_ID, BID_AMOUNT, BID_TIME)
VALUES (8, 5, 3, 11000, TO_TIMESTAMP('05-09-26 12:54:22.817000000 PM', 'DD-MM-RR fmHH12:fmMI:SSXFF AM'));

INSERT INTO BIDS (BID_ID, AUCTION_ID, BIDDER_ID, BID_AMOUNT, BID_TIME)
VALUES (9, 6, 3, 2100, TO_TIMESTAMP('08-09-26 4:03:35.550000000 PM', 'DD-MM-RR fmHH12:fmMI:SSXFF AM'));

INSERT INTO BIDS (BID_ID, AUCTION_ID, BIDDER_ID, BID_AMOUNT, BID_TIME)
VALUES (10, 7, 3, 1100, TO_TIMESTAMP('11-11-26 8:48:07.329000000 PM', 'DD-MM-RR fmHH12:fmMI:SSXFF AM'));

-- Commit all imported records atomically
COMMIT;

SET FEEDBACK ON;

PROMPT
PROMPT ========================================================
PROMPT SUCCESS: HASHbid database initialization completed!
PROMPT ========================================================
PROMPT Reconstructed Data Summary:
PROMPT   - USERS       : 4 records imported
PROMPT   - PRODUCTS    : 7 records imported
PROMPT   - AUCTIONS    : 7 records imported
PROMPT   - BIDS        : 9 records imported
PROMPT   - SETTLEMENTS : 0 records (table created and ready)
PROMPT
PROMPT Reconstructed Application Sequences:
PROMPT   - USER_SEQ    (Next value: 5)
PROMPT   - PRODUCT_SEQ (Next value: 8)
PROMPT   - AUCTION_SEQ (Next value: 8)
PROMPT   - BID_SEQ     (Next value: 11)
PROMPT ========================================================
