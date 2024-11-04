#!/usr/bin/bash
export PGPASSWORD='password'
export host='localhost'
export user='root'
export port=5432
psql -h $host -U $user -d postgres -p $port -f enable-crypto-extension.sql
psql -h $host -U $user -d postgres -p $port -f role.sql
psql -h $host -U $user -d postgres -p $port -f year.sql
psql -h $host -U $user -d postgres -p $port -f semester.sql
psql -h $host -U $user -d postgres -p $port -f department.sql
psql -h $host -U $user -d postgres -p $port -f subject.sql
psql -h $host -U $user -d postgres -p $port -f user.sql
psql -h $host -U $user -d postgres -p $port -f post.sql
psql -h $host -U $user -d postgres -p $port -f comment.sql

