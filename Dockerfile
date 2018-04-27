FROM nexus.mgmt.health-dev.dwpcloud.uk:5000/dwp/base:develop
EXPOSE 9101
RUN mkdir /opt/ms-fitnote-controller
ADD ./target /opt/ms-fitnote-controller
WORKDIR /opt/ms-fitnote-controller
ENV CLASSPATH=/opt/ms-fitnote-controller:/opt/ms-fitnote-controller/.:/opt/ms-fitnote-controller/*
CMD [ "/bin/java", "uk.gov.dwp.health.fitnotecontroller.application.FitnoteControllerApplication", "server", "/opt/ms-fitnote-controller/config.yml" ]
