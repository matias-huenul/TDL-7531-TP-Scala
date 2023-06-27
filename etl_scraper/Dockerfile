FROM openjdk:11

ENV SBT_VERSION 1.8.3

RUN curl -L -o sbt-$SBT_VERSION.zip https://github.com/sbt/sbt/releases/download/v$SBT_VERSION/sbt-$SBT_VERSION.zip
RUN unzip sbt-$SBT_VERSION.zip -d ops

WORKDIR /etl
ADD . /etl

RUN /ops/sbt/bin/sbt assembly
CMD scala "target/scala-2.13/etl_scraper-assembly-1.0.jar"
