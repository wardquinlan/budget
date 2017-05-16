#!/bin/sh

./DDL.sh

BUDGETDIR=/home/ward/c/budget
BUDGETFILE=A2015Q2.BDG
rm -f CREATE_ACCOUNTS.sql
rm -f CREATE_TRANSACTIONS.sql

# Create accounts
echo Generating create accounts...
br --summary --csv $BUDGETDIR/$BUDGETFILE >/dev/null
awk -F, < $BUDGETDIR/$BUDGETFILE.csv '{print $2;}' | grep -v General | awk '{print "insert into account(ownerid,name,pub) values (1,\x27"$1"\x27,0);";}' > CREATE_ACCOUNTS.sql

# Insert transactions
ACCOUNTS=`awk -F, < $BUDGETDIR/$BUDGETFILE.csv '{print $2;}' | grep -v General`
for A in $ACCOUNTS
do
  echo Generating insert transactions into $A...
  br --csv --account $A $BUDGETDIR/$BUDGETFILE >/dev/null
  sed 's/\x27/\x27\x27/g' $BUDGETDIR/$BUDGETFILE.csv | awk -F, '{print "insert into transact(ts,accountid,amount,balance,note,uuid) values(\x27"$3"\x27,(select a.id from account a where a.name = \x27"$2"\x27),100 * "$4",100 * "$5",\x27"$6"\x27,\x27NNNNNNNN-0000-0000-0000-000000000000\x27);";}' >> CREATE_TRANSACTIONS.sql
done

echo Executing create accounts...
psql --username=budgettest --file=CREATE_ACCOUNTS.sql budgettest

echo Executing transaction inserts...
psql --username=budgettest --file=CREATE_TRANSACTIONS.sql budgettest

echo Resetting account ownerships...
psql --username=budgettest budgettest << EOF
  update account set ownerid=(select id from users where name='peter') where name = 'Peter';
  update account set ownerid=(select id from users where name='katie') where name = 'Kaitlyn';
  update account set ownerid=(select id from users where name='jamie') where name = 'Jamie';
EOF

echo Done
