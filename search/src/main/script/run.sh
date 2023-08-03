#!/bin/bash

# 检查jieba_server是否在运行中
if ps aux | grep -q '[j]ieba_server.py'; then
  echo "jieba_server is already running."
else
  # 启动jieba_server
  nohup python3 jieba_server.py > jieba_server.out &
  echo "jieba_server started."
fi