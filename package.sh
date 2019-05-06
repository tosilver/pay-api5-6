#!/bin/sh
if [ $RUN_ENV ];then
    echo "RUN_EVN = $RUN_ENV"
else
    echo "RUN_EVN not exists"
fi

[ -z "$RUN_ENV" ] && RUN_ENV=dev

echo '更新代码...' && git pull \
 && echo '打包代码...' && mvn clean package -e -P${RUN_ENV} -Dmaven.test.skip=true

#echo ************************** package *************************
#mvn clean package -e -Dmaven.test.skip=true
