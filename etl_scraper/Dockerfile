FROM openjdk:11

ENV SBT_VERSION 1.8.3

RUN curl https://downloads.lightbend.com/scala/2.13.10/scala-2.13.10.tgz | tar xfz - -C /usr/share && \
    mv /usr/share/scala-2.13.10 /usr/share/scala

RUN curl -L -o sbt-$SBT_VERSION.zip https://github.com/sbt/sbt/releases/download/v$SBT_VERSION/sbt-$SBT_VERSION.zip
RUN unzip sbt-$SBT_VERSION.zip -d ops

WORKDIR /etl
ADD . /etl

EXPOSE 8080

RUN /ops/sbt/bin/sbt assembly
CMD /usr/share/scala/bin/scala "target/scala-2.13/etl_scraper-assembly-1.0.jar"