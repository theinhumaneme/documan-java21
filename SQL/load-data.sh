#!/usr/bin/bash
export PGPASSWORD='password'
psql -h localhost -U root -d postgres -f role.sql
psql -h localhost -U root -d postgres -f year.sql
psql -h localhost -U root -d postgres -f semester.sql
psql -h localhost -U root -d postgres -f department.sql
psql -h localhost -U root -d postgres -f user.sql
psql -h localhost -U root -d postgres -f post.sql
psql -h localhost -U root -d postgres -f comment.sql

