cp ../jboss-setup/server-jboss-proxy.xml $JBOSS_HOME/server/default/deploy/jboss-web.deployer/server.xml
cp ../jboss-setup/jboss-context.xml $JBOSS_HOME/server/default/deploy/jboss-web.deployer/context.xml
cp ../jboss-setup/jboss-service.xml $JBOSS_HOME/server/default/deploy/jboss-web.deployer/META-INF/jboss-service.xml
cp ../jboss-setup/webserver-xmbean.xml $JBOSS_HOME/server/default/deploy/jboss-web.deployer/META-INF/webserver-xmbean.xml
cp ../jboss-setup/log4j.xml $JBOSS_HOME/server/default/conf/jboss-log4j.xml
mvn clean install -f ../../../../sip-servlets-examples/location-service/pom.xml
cp ../../../../sip-servlets-examples/location-service/target/location-service-1.0.war $JBOSS_HOME/server/default/deploy
cp ../../../../sip-servlets-examples/location-service/locationservice-dar.properties $JBOSS_HOME/server/default/conf/dars/locationservice-dar.properties
