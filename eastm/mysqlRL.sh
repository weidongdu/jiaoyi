#!/bin/bash

# Remote MySQL details
remote_user="stock"
remote_password="mysqlstock@Dwd25"
remote_host="8.142.9.14"
remote_db="jiaoyi"

# Local MySQL details
local_user="root"
local_password="12345678"
local_host="localhost"
local_db="jiaoyi"

# Table to dump
table="t_close_market"
# Dump table from remote MySQL and import into local MySQL
mysqldump -u $remote_user -p$remote_password -h $remote_host $remote_db $table | mysql -u $local_user -p$local_password -h $local_host $local_db