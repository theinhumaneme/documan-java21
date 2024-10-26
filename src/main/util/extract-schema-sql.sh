#!/bin/bash
docker exec postgres pg_dump -U root -s postgres > schema.sql