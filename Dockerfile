# VERSION 0.0.1
FROM java:8-jre
# 签名
MAINTAINER YK "kalirys@gmail.com"

#设置时区
#ENV TIME_ZONE Asiz/Shanghai
RUN cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
  && echo 'Asia/Shanghai' > /etc/timezone && date

#RUN cat /etc/resolv.conf
#RUN echo 'nameserver 223.5.5.5\r\nnameserver 223.6.6.6' >> /etc/resolv.conf \
#  && cat /etc/resolv.conf

RUN cat /proc/version
#RUN sed 1'i\172.17.0.2 weixin-web.services.xxx.cn' /etc/hosts
RUN cat /etc/hosts

ADD target/*.jar app.jar

EXPOSE 9988
CMD ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
