#!/bin/sh

#scp /Users/dwd/dev/GitHub/jiaoyi/tmp/target/tmp-0.0.1-SNAPSHOT.jar ubuntu@iz01:/home/ubuntu/run
#scp src/main/resources/static/html/line-race.html ubuntu@iz01:~/html/theme.html

# 设置 java 17 环境变量
JAVA_HOME17=/Library/Java/JavaVirtualMachines/jdk-17.0.5.jdk/Contents/Home

#设置maven 使用 java 17
JAVA_HOME=$JAVA_HOME17

PATH=$JAVA_HOME/bin:$PATH

# 先对 common 进行 install
cd /Users/dwd/dev/GitHub/jiaoyi/common || exit
mvn install

# 然后对 eastm 进行操作
cd /Users/dwd/dev/GitHub/jiaoyi/eastm || exit
mvn clean package -Dmaven.test.skip=true

scp target/eastm-0.0.1-SNAPSHOT.jar ubuntu@iz01:~/upload

ssh ubuntu@iz01 << EOF
sudo supervisorctl restart eastm
EOF


loge