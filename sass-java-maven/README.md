# Overview
Compile sass during compile phase of a maven build.

## Usage

Add the dependency to `pom.xml`

```xml
<plugin>
	<groupId>com.darrinholst</groupId>
	<artifactId>sass-java-maven-plugin</artifactId>
	<version>3.4.4.3</version>
	<executions>
		<execution>
			<goals>
				<goal>compile</goal>
			</goals>
		</execution>
	</executions>
</plugin>
```

Alternatively you can run the plugin from the command-line like this:
```
mvn com.darrinholst:sass-java-maven-plugin:compile
```

## Advanced configuration

To make running the plugin even more simple it is possible to add `com.darrinholst` to
the list of plugin-groups in your `[userhome]/.m2/settings.xml`.

```xml
<pluginGroups>
    <pluginGroup>com.darrinholst</pluginGroup>
</pluginGroups>
```

After that you'll be able to run the command as a short-hand. Even if the plugin has not
been configured for your project, even when a pom.xml isn't even available.

```
mvn sass-java:compile
```

### Config file location

The default for the location of the `config.rb` file is set to the same location as the
sass-java filter, being `${project.basedir}/src/main/webapp/WEB-INF/sass/config.rb`. You
can configure this location in the `pom.xml` using the configurations element.

```xml
<plugin>
    <groupId>com.darrinholst</groupId>
    <artifactId>sass-java-maven-plugin</artifactId>
    <version>3.4.4.3</version>
    <executions>
        <execution>
        	<configuration>
        		<config>${project.basedir}/src/main/webapp/WEB-INF/sass/config.rb</config>
        	</configuration>
            <goals>
                <goal>compile</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

Alternatively you can set the property on commandline as follows:

```
mvn -Dsass-java.configFile="${project.basedir}/src/main/webapp/WEB-INF/sass/config.rb" com.darrinholst:sass-java-maven-plugin:compile
```
