#!/bin/sh

DATABASE=budgettest
USER=budgettest
psql --username=$USER $DATABASE << EOF

ALTER TABLE USERS ADD COLUMN LIM INTEGER;
UPDATE USERS SET LIM=0;
ALTER TABLE USERS ADD CONSTRAINT CC3 CHECK(LIM >= 0);
ALTER TABLE USERS ALTER COLUMN LIM SET NOT NULL;

EOF