#!/bin/bash

# 查找jieba_server进程的PID
pid=$(ps aux | grep '[j]ieba_server.py' | awk '{print $2}')

if [ -n "$pid" ]; then
  # 终止jieba_server进程
  kill $pid
  echo "jieba_server stopped."
else
  echo "jieba_server is not running."
fi