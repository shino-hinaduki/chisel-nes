FROM openjdk:8u322-jre

# install git, sbt, graphviz
RUN apt update && apt install -y --no-install-recommends git

# clone diagrammer
RUN git clone https://github.com/freechipsproject/diagrammer /diagrammer
WORKDIR /diagrammer

# install graphviz
RUN apt update && apt install -y --no-install-recommends \
    graphviz \
    graphviz-dev

# install sbt
RUN echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" | tee /etc/apt/sources.list.d/sbt.list
RUN echo "deb https://repo.scala-sbt.org/scalasbt/debian /" | tee /etc/apt/sources.list.d/sbt_old.list
RUN curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | apt-key add
RUN apt update && apt install -y --no-install-recommends sbt

CMD [ "/bin/bash" ]