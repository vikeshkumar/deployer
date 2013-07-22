README

Usage 
deployer:deploy => Fresh deployment of a war file
deployer:undeploy => Undeploys a already deployed application.
deployer:update => Undeploys and Deploys (Re-deployment) of the application.

There are sensible defaults.
1. If appName is not provided then the project's artifactId is taken as one.
2. By default deploys to localhost.
3. Default scheme for authentication with tomcat is HTTP. Can be changed to HTTPS. Any other value will give error.
4. The default war file is the generated war file in target directory.
5. By default does not uses any proxy.
6. Default tomcat port is 8080, if you are running tomcat on different port then please provide one.
7. Default tomcatVersion is tomcat6, provide tomcat7 if using that one.
8. Default proxy port is 80 - change if required.

Example Configuration

```xml
<plugin>
    <groupId>com.github.vikesh.maven</groupId>
    <artifactId>deployer</artifactId>
    <version>1.0-SNAPSHOT</version>
    <configuration>
        <tomcatPort>80</tomcatPort>
        <tomcatVersion>tomcat7</tomcatVersion>
        <tomcatURL>192.168.159.128</tomcatURL>
        <scriptUser>username</scriptUser>
        <scriptPass>password</scriptPass>
        <appName>Rental</appName>
        <proxyHost>192.168.2.100</proxyHost>
        <proxyPort>8080</proxyHost>
        <warFile>target/whateverWar.war</warFile>
    </configuration>
</plugin>
```

Can put the above configuration in pom.xml => Highly not recommended as credentials are with pom file.

To get around this. add a profile in settings.xml file. and add these values there - following is just an

Example

```xml
<profile>
  <id>someProfile</id>
  <property>
        <tomcat.port>80</tomcat.port>
        <tomcat.version>tomcat7</tomcat.version>
        <tomcat.url>192.168.159.128</tomcat.url>
        <script.user>username</script.user>
        <script.pass>password</script.pass>
        <app.name>Rental</app.name>
        <proxy.host>192.168.2.100</proxy.host>
        <proxy.port>8080</proxy.port>
        <war.file>target/whateverWar.war</war.file>
  </property>
</profile>
```

And in your POM.xml file

Example

```xml
<plugin>
    <groupId>com.github.vikesh.maven</groupId>
    <artifactId>deployer</artifactId>
    <version>1.0-SNAPSHOT</version>
    <configuration>
        <tomcatPort>${tomcat.port}</tomcatPort>
        <tomcatVersion>${tomcat.version}</tomcatVersion>
        <tomcatURL>${tomcat.url}</tomcatURL>
        <scriptUser>${script.user}</scriptUser>
        <scriptPass>${script.pass}</scriptPass>
        <appName>${app.name}</appName>
        <proxyHost>${proxy.host}</proxyHost>
        <proxyPort>${proxy.port}</proxyHost>
        <warFile>${war.file}</warFile>
    </configuration>
</plugin>
```

During execution specify the profile name using -P
