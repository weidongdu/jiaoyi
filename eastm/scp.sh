#!/bin/sh

scp target/eastm-0.0.1-SNAPSHOT.jar ubuntu@iz01:~/upload
#scp /Users/dwd/dev/GitHub/jiaoyi/tmp/target/tmp-0.0.1-SNAPSHOT.jar ubuntu@iz01:/home/ubuntu/run
# scp src/main/resources/static/html/line-race.html ubuntu@iz01:~/html/theme.html

ssh ubuntu@iz01