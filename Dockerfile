FROM ubuntu:22.04

ARG USER_NAME
ARG GROUP_NAME
ARG USER_ID
ARG GROUP_ID

RUN apt-get update
RUN apt-get install -y curl vim zip unzip
RUN curl https://downloads.lightbend.com/scala/2.12.15/scala-2.12.15.tgz | tar xfz - -C /usr/share && \
    mv /usr/share/scala-2.12.15 /usr/share/scala
RUN apt-get install -y openjdk-8-jdk

RUN groupadd -g $GROUP_ID $GROUP_NAME && \
    useradd -u $USER_ID -g $GROUP_ID -ms /bin/bash $USER_NAME
ENV HOME=/home/$USER_NAME
WORKDIR $HOME
USER $USER_NAME

ENV SCALA_HOME=/usr/share/scala
ENV PATH=$PATH:$SCALA_HOME/bin

RUN curl -s "https://get.sdkman.io" | bash

SHELL ["/bin/bash", "-c"]
RUN source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk install sbt

WORKDIR $HOME/app

CMD ["tail", "-f", "/dev/null"]
