#!/bin/sh

scp target/eastm-0.0.1-SNAPSHOT.jar ubuntu@iz01:~/upload

#scp /Users/dwd/dev/GitHub/jiaoyi/tmp/target/tmp-0.0.1-SNAPSHOT.jar ubuntu@iz01:/home/ubuntu/run
# scp src/main/resources/static/html/line-race.html ubuntu@iz01:~/html/theme.html

# 登录到远程服务器 并执行命令 sudo supervisorctl restart  eastm
ssh ubuntu@iz01 << EOF

# 执行需要在远程服务器上运行的命令
sudo supervisorctl restart eastm

EOF