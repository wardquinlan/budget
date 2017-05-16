#!/bin/sh

DATABASE=budgettest
USER=budgettest
PASSWORD=budgettest00

if [ `whoami` != 'postgres' ]; then
  echo CREATE_DB.sh: must run as \'postgres\'
  exit 1
fi

echo CREATE_DB.sh: when prompted, enter the \'postgres\' password

echo CREATE_DB.sh: attempting to drop database \'$DATABASE\'
dropdb $DATABASE 2>/dev/null
if [ $? != 1 ]; then
  echo CREATE_DB.sh: database \'$DATABASE\' not found';' skipping drop
fi

echo CREATE_DB.sh: attempting to create database \'$DATABASE\'
createdb $DATABASE
if [ $? != 0 ]; then
  echo CREATE_DB.sh: could not create database \'$DATABASE\'
  exit 1
fi

echo CREATE_DB.sh: attempting to create user \'$USER\'
psql << EOF
  DROP USER IF EXISTS $USER;
  CREATE USER $USER WITH PASSWORD '$PASSWORD';
EOF

