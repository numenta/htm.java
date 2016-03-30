FROM java:8

RUN wget -q https://services.gradle.org/distributions/gradle-2.12-bin.zip \
  && unzip -q gradle-2.12-bin.zip -d /opt \
  && rm gradle-2.12-bin.zip
  
ENV GRADLE_HOME /opt/gradle-2.12
ENV GRADLE_OPTS -Dorg.gradle.daemon=true
ENV PATH $GRADLE_HOME/bin:$PATH

RUN wget -q http://apache.mirrors.pair.com/maven/maven-3/3.3.9/binaries/apache-maven-3.3.9-bin.zip \
  && unzip -q apache-maven-3.3.9-bin.zip -d /opt \
  && rm apache-maven-3.3.9-bin.zip

ENV M2_HOME /opt/apache-maven-3.3.9
ENV PATH $M2_HOME/bin:$PATH

# Prepare a user account for use at runtime.  boot2docker uses uid 1000.
RUN useradd --uid 1000 -m vagrant
USER vagrant
WORKDIR /home/vagrant

CMD ["bash"]
