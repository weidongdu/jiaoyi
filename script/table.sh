#!/bin/bash

file="e.txt"

# 从 e.txt 文件中提取 data.diff 部分
json_data=$(cat $file | jq '.data.diff')

# 输出表头
echo "Index | f2    | f3    | f8   | f12"

# 遍历 data.diff 中的 JSON 对象
for index in $(echo "$json_data" | jq 'keys[]'); do
  row=$(echo "$json_data" | jq ".[$index]")
  f2=$(echo "$row" | jq '.f2')
  f3=$(echo "$row" | jq '.f3')
  f8=$(echo "$row" | jq '.f8')
  f12=$(echo "$row" | jq -r '.f12') # 使用 -r 选项以去除双引号
  printf "%-5s | %-5s | %-5s | %-4s | %s\n" "$index" "$f2" "$f3" "$f8" "$f12"
done
