#!/bin/sh
echo ************************** running *************************
[ -z "$RUN_ENV" ] && RUN_ENV=dev
echo "RUN_EVN = $RUN_ENV"
mvn clean spring-boot:run -e -P$RUN_ENV -Dmaven.test.skip=true
