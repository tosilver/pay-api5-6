#!/bin/sh
APP_NAME=pay-api-prod
echo "应用名[$APP_NAME]"
echo '*********更新代码*********'
git checkout master
git fetch --all
git reset --hard origin/master
git pull

echo '*********打包代码*********' && mvn clean package -e -Pdocker -Dmaven.test.skip \
  && echo '*********打包镜像*********' && docker build --no-cache -t ${APP_NAME} .

echo '*********检测已运行容器*********'
CONTAINER_ID=`docker ps -a | grep ${APP_NAME} | grep -v grep | awk '{print $1}'`
if [ -n "${CONTAINER_ID}" ]; then
  echo "*********停止容器[${CONTAINER_ID}]*********" && docker stop ${CONTAINER_ID} \
    && echo "*********移除容器[${CONTAINER_ID}]*********" && docker rm ${CONTAINER_ID}
fi

for IMAGE_ID in $(docker images | grep none | grep -v grep | awk '{print $3 }'); do
  echo "*********移除空镜像[${IMAGE_ID}]*********" && docker rmi ${IMAGE_ID}
done

echo '*********运行服务*********' && docker run -d -p 8090:9988 --name ${APP_NAME} ${APP_NAME} \
  && echo '*********输出日志*********' && docker logs -f ${APP_NAME}
