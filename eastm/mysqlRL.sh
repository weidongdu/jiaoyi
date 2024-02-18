#!/bin/bash

# 指定目录路径
directory="/Users/dwd/dev/GitHub/jiaoyi/kline"

# 计算7天前的时间戳
seven_days_ago=$(date -v-7d +%s)

# 遍历目录中的文件和子目录
for file in "$directory"/*; do
    # 检查文件或目录的修改时间
    if [[ -d "$file" ]]; then
        # 如果是目录
        modified_time=$(stat -f %m "$file")
    else
        # 如果是文件
        modified_time=$(stat -f %m "$file")
    fi

    # 比较修改时间与7天前的时间戳
    if [[ "$modified_time" -lt "$seven_days_ago" ]]; then
        # 删除目录或文件
        rm -rf "$file"
    fi
done




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