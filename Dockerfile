FROM java:8

RUN wget -q https://services.gradle.org/distributions/gradle-2.12-bin.zip \
  && unzip -q gradle-2.12-bin.zip -d /opt \
  && rm gradle-2.12-bin.zip
  
ENV GRADLE_HOME /opt/gradle-2.12
ENV PATH $GRADLE_HOME/bin:$PATH

# Prepare a user account for use at runtime.  boot2docker uses uid 1000.
RUN useradd --uid 1000 -m vagrant
USER vagrant
WORKDIR /home/vagrant

CMD ["bash"]
