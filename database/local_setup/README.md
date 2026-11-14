# HASHbid Local Oracle Database Setup Guide

This directory provides scripts to reconstruct a clean, isolated local development database for **HASHbid** on **Oracle Database 10g Express Edition (XE)** (also compatible with Oracle 11g, 19c, and 21c XE).

---

## 1. Safety & Oracle Transaction Semantics

### Non-Atomic DDL & Implicit Commits
* In Oracle Database, **every DDL statement (`CREATE TABLE`, `CREATE INDEX`, `CREATE SEQUENCE`, `CREATE USER`, `GRANT`) executes an implicit `COMMIT` before and after execution.**
* Therefore, the schema initialization process is **NOT a single atomic transaction**.
* The directive `WHENEVER SQLERROR EXIT FAILURE ROLLBACK;` halts execution immediately upon any error and rolls back uncommitted `INSERT` (DML) statements. However, **any DDL objects created prior to the failure point will remain committed in the data dictionary.**

### Strict Non-Destructive Policy
* Neither script contains `DROP TABLE`, `DROP SEQUENCE`, or `DROP USER` statements.
* Scripts will **never** silently overwrite, delete, or drop existing objects.
* Both scripts run pre-flight dictionary validation (`all_users`, `user_tables`, `user_sequences`). If any application object already exists, execution aborts immediately with an application error (`ORA-20001`, `ORA-20002`, `ORA-20003`).

### Pre-Execution Validation
* Script 02 validates that the active session user is `HASHBID` (preventing accidental creation under `SYSTEM`).
* Script 02 validates that sample timestamp formats parse successfully under the active session NLS settings before any `CREATE TABLE` statement is executed.

---

## 2. Overview of Scripts

| File | Purpose | Execution Role |
| :--- | :--- | :--- |
| **`01_create_hashbid_user.sql`** | Performs dictionary check, creates the dedicated `HASHBID` user with tablespace quota, and grants minimum required privileges (`CREATE SESSION`, `CREATE TABLE`, `CREATE SEQUENCE`, `CREATE VIEW`). | Run as **`SYSTEM`** or **`SYS AS SYSDBA`** |
| **`02_create_hashbid_schema_and_data.sql`** | Pre-validates session user & timestamp parsing, creates all 5 tables in relational dependency order, creates foreign key indexes, sets up sequences initialized beyond existing IDs, and imports verified sample data. | Run as **`HASHBID`** |

---

## 3. Step-by-Step Setup Instructions

### Step 1: Configure Password and Create the `HASHBID` User

1. Open `01_create_hashbid_user.sql` in a text editor.
2. Replace `<REPLACE_WITH_YOUR_LOCAL_PASSWORD>` with your chosen password (e.g., `hashbid123`).
3. Open a command prompt, navigate to this directory, and start SQL\*Plus **interactively** (do not type the password in the command line):
   ```cmd
   cd "d:\Owais EAuctoin\EAuctionSystem-20261008T144836Z-1-001\EAuctionSystem\database\local_setup"
   sqlplus system@XE
   ```
4. When prompted by SQL\*Plus:
   ```
   Enter password: <type your system password securely>
   ```
5. Run the user creation script:
   ```sql
   @01_create_hashbid_user.sql
   ```
6. You should see:
   ```
   SUCCESS: User HASHBID created and granted required privileges.
   ```

---

### Step 2: Connect as `HASHBID` and Initialize Schema & Data

1. In the same SQL\*Plus session, connect interactively as the newly created user:
   ```sql
   CONNECT HASHBID@XE;
   ```
2. When prompted:
   ```
   Enter password: <type your HASHBID password securely>
   ```
3. Verify you are connected as the intended application user:
   ```sql
   SHOW USER;
   -- Expected output: USER is "HASHBID"
   ```
4. Execute the schema initialization script:
   ```sql
   @02_create_hashbid_schema_and_data.sql
   ```
5. The script validates the environment, creates all tables, indexes, sequences, inserts all sample data, and issues `COMMIT;`.

---

## 4. Manual Recovery Procedure (If Script 02 Fails Midway)

Because Oracle DDL causes implicit commits, if `02_create_hashbid_schema_and_data.sql` encounters an error midway (for example, if disk quota is exceeded during table creation):
1. Execution halts immediately and rolls back uncommitted `INSERT` statements.
2. However, any tables or sequences created *before* the error remain in the database.
3. Because of the non-destructive guard, re-running `02_create_hashbid_schema_and_data.sql` directly will stop with `ORA-20002` (table already exists).

### Safe Manual Recovery Steps:
To start fresh without guessing which individual objects succeeded:

> [!WARNING]
> **Permanent Data Loss Warning:**
> Running `DROP USER HASHBID CASCADE;` will **permanently delete** the `HASHBID` account and **all** tables, sequences, indexes, constraints, and data stored within that schema.
> Use this command **only** if the initial setup failed midway and you intend to wipe the incomplete schema and start completely fresh.

1. Connect to SQL\*Plus as **DBA** (`SYSTEM`):
   ```cmd
   sqlplus system@XE
   ```
2. Manually drop the incomplete application user and all its objects:
   ```sql
   DROP USER HASHBID CASCADE;
   ```
3. Re-run Step 1:
   ```sql
   @01_create_hashbid_user.sql
   ```
4. Connect as `HASHBID` and re-run Step 2:
   ```sql
   CONNECT HASHBID@XE;
   @02_create_hashbid_schema_and_data.sql
   ```

> [!NOTE]
> This recovery procedure is strictly manual. The scripts themselves will **never** automatically drop or purge schemas or tables.

---

## 5. Verification Commands

Run these queries in SQL\*Plus while connected as `HASHBID` to verify the installation:

### A. Verify Tables
```sql
SELECT table_name FROM user_tables ORDER BY table_name;
```
**Expected Output:**
```
TABLE_NAME
------------------------------
AUCTIONS
BIDS
PRODUCTS
SETTLEMENTS
USERS
```

### B. Verify Sequences and Starting Values
```sql
SELECT sequence_name, last_number, increment_by FROM user_sequences ORDER BY sequence_name;
```
**Expected Output:**
```
SEQUENCE_NAME                  LAST_NUMBER INCREMENT_BY
------------------------------ ----------- ------------
AUCTION_SEQ                              8            1
BID_SEQ                                 11            1
PRODUCT_SEQ                              8            1
USER_SEQ                                 5            1
```

### C. Verify Imported Record Counts
```sql
SELECT 'USERS' AS table_name, COUNT(*) AS record_count FROM USERS
UNION ALL
SELECT 'PRODUCTS', COUNT(*) FROM PRODUCTS
UNION ALL
SELECT 'AUCTIONS', COUNT(*) FROM AUCTIONS
UNION ALL
SELECT 'BIDS', COUNT(*) FROM BIDS
UNION ALL
SELECT 'SETTLEMENTS', COUNT(*) FROM SETTLEMENTS;
```
**Expected Output:**
```
TABLE_NAME   RECORD_COUNT
------------ ------------
USERS                   4
PRODUCTS                7
AUCTIONS                7
BIDS                    9
SETTLEMENTS             0
```

---

## 6. Security & Privacy Notice (Plaintext Test Passwords)

> [!WARNING]
> The sample `USERS` records imported by `02_create_hashbid_schema_and_data.sql` contain **unhashed test passwords** (`admin123`, `pass123`, `buyer123`, `far123`) and personal contact details from the original college project export.
>
> * **DO NOT** commit real production credentials to Git.
> * **DO NOT** publish these setup scripts or database dumps to a public GitHub/GitLab repository without redacting or changing the test passwords and contact information.

---

## 7. Product Image Inventory

The sample product records reference relative image paths stored under `uploads/`:

| Product ID | Product Name | Stored `IMAGE_PATH` | Repository File Status |
| :---: | :--- | :--- | :--- |
| **1** | DEAR | `uploads/dear.jpg` | **Present** in `src/main/webapp/uploads/dear.jpg` |
| **2** | Sony Alpha 7 IV Mirrorless Camera | `uploads/1788521613542_133929306356491186.jpg` | **Missing** (uploaded during original testing) |
| **3** | pahad | `uploads/1788526515280_133955724542961058.jpg` | **Missing** |
| **4** | mountain | `uploads/1788526791561_133955724542961058.jpg` | **Missing** |
| **5** | HIGHWAY | `uploads/1788590840703_133955724725886807.jpg` | **Missing** |
| **6** | JUNGLE | `uploads/1788863415079_image_3.jpg` | **Missing** |
| **7** | APPLE | `uploads/1794410004319_download__3_.jpeg` | **Missing** |

The database retains these paths intact. For local testing, any image can be copied and renamed to match the missing filenames in `src/main/webapp/uploads/`, or you can upload new products using the Seller dashboard (`/add-product`).

---

## 8. How to Configure Application Database Connection

Once the `HASHBID` user and schema are created and verified locally, configure the connection properties using the local configuration file:

1. Copy the example configuration template:
   ```cmd
   copy src\main\resources\db.properties.example src\main\resources\db.properties
   ```
   *(Or create `db.properties` in the project root directory)*

2. Open `src/main/resources/db.properties` and enter your local credentials:
   ```properties
   db.url=jdbc:oracle:thin:@localhost:1521:XE
   db.username=HASHBID
   db.password=YOUR_LOCAL_PASSWORD
   ```

3. `DBConnection.java` dynamically loads credentials from `db.properties`. The actual `db.properties` file is excluded from Git version control via `.gitignore`, ensuring your database passwords remain private.
