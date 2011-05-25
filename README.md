# Sass for Java

Don't let those ruby devs have all the fun, with a simple filter you can
have sass in your java too.

## Usage

Add this to your web.xml

    <filter>
        <filter-name>SassCompiler</filter-name>
        <filter-class>com.sass_lang.SassCompilingFilter</filter-class>
    </filter>

    <filter-mapping>
        <filter-name>SassCompiler</filter-name>
        <url-pattern>*.css</url-pattern>
    </filter-mapping>

Put your sass templates in WEB-INF/sass and each request for a css
file will regenerate as needed.

**Maven**

Since getting stuff in central is a PITA, add the following to your
pom.xml

    <repositories>
        <repository>
            <id>darrinholst-maven</id>
            <url>http://darrinholst-maven.googlecode.com/svn/repo/</url>
        </repository>
    </repositories>

    <dependencies>
        <dependency>
            <groupId>com.sass-lang</groupId>
            <artifactId>sass-java</artifactId>
            <version>3.1.1.2</version>
        </dependency>
    </dependencies>

**Ant**

Download the [jar with depenencies](http://darrinholst-maven.googlecode.com/svn/repo/com/sass-lang/sass-java/3.1.1.2/sass-java-3.1.1.2-jar-with-dependencies.jar)

##Config

Configuration is done through filter init parameters

* **templateLocation** - the location where the sass templates will be
  found (default WEB-INF/sass)
* **cssLocation** - the location where the sass templates will be
  compiled to (default stylesheets)
* **cacheLocation** - the location that sass will use for compile
  caching (default WEB-INF/.sass-cache)
* **onlyRunWhenKey** - the system property or environment variable to
  check to see if sass compilation should run, use this to turn sass
  generation off in production
* **onlyRunWhenValue** - the corresponding value to check to see if sass
  compilation should run

You probably only want the css generation to run in development mode and
then commit the generated css to source control. This is achieved by the
`onlyRunWhenKey` and `onlyRunWhenValue` filter init parameters.

    <filter>
        <filter-name>SassCompiler</filter-name>
        <filter-class>com.sass_lang.SassCompilingFilter</filter-class>
        <init-param>
            <param-name>onlyRunWhenKey</param-name>
            <param-value>RUNTIME_ENVIRONMENT</param-value>
        </init-param>
        <init-param>
            <param-name>onlyRunWhenValue</param-name>
            <param-value>local</param-value>
        </init-param>
    </filter>

With this configuration the filter will check a system property or
environment variable called RUNTIME_ENVIRONMENT and only run the sass
compilation if that value is equal to local

##Try it out
1. git clone https://github.com/darrinholst/sass-java.git
2. cd sass-java
3. mvn jetty:run
4. wait for maven to download the internet
5. open http://localhost:8080/sass-java
6. change src/test/sample-webapp/WEB-INF/sass/application.scss
7. repeat step 5
8. be amazed
