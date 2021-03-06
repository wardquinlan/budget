#!/bin/sh

DATABASE=budgettest
USER=budgettest
psql --username=$USER $DATABASE << EOF

ALTER TABLE ACCOUNT ADD COLUMN HIDE INTEGER;
UPDATE ACCOUNT SET HIDE=0;
ALTER TABLE ACCOUNT ALTER COLUMN HIDE SET NOT NULL;
ALTER TABLE ACCOUNT ADD CONSTRAINT CC2 CHECK(HIDE = 0 OR HIDE = 1);

ALTER TABLE USERS RENAME COLUMN HIDE TO SHOW;

EOF

