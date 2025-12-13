.mvn/maven.config:
   Starting with Maven 3.3.1+, this can be solved by putting this options to a script but this can now simple being done by defining
   maven.config file which contains the configuration options for the mvn command line.
   For example things like -T3 -U --fail-at-end. So you only have to call Maven just by using mvn clean package
   instead of mvn -T3 -U --fail-at-end clean package and not to miss the -T3 -U --fail-at-end options on every call.
.mvn/jvm.config:
   Starting with Maven 3.3.1+ you can define JVM configuration via jvm.config file which means you can define the options for your
   build on a per-project basis. This file will become part of your project and will be checked in along with your project.
   So for example if you put the following JVM options into the jvm.config file: -Xmx2048m -Xms1024m -XX:MaxPermSize=512m
