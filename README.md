**NOTE**: *As of version 3.2.7 we are using compass to compile your sass!*

# sass-java
Compiles sass to css on-the-fly with [compass](http://compass-style.org/) via a j2ee servlet filter


## Usage
Add this to your `web.xml`

    <filter>
        <filter-name>SassCompiler</filter-name>
        <filter-class>com.sass_lang.SassCompilingFilter</filter-class>
    </filter>

    <filter-mapping>
        <filter-name>SassCompiler</filter-name>
        <url-pattern>*.css</url-pattern>
    </filter-mapping>

Create a `WEB-INF/sass/config.rb` file that looks something like

    css_dir = "../../stylesheets"
    sass_dir = "."
    
Put your sass templates in `WEB-INF/sass` and each request for a css
file will regenerate as needed.

**Maven**

Since getting stuff in central is a PITA, add the following to your
pom.xml

    <repositories>
        <repository>
            <id>sass-java</id>
            <url>http://sass-java.googlecode.com/svn/repo/</url>
        </repository>
    </repositories>

    <dependencies>
        <dependency>
            <groupId>com.sass-lang</groupId>
            <artifactId>sass-java</artifactId>
            <version>3.2.7</version>
        </dependency>
    </dependencies>

**Ant**

Download the [jar with depenencies](http://sass-java.googlecode.com/svn/repo/com/sass-lang/sass-java/3.2.7/sass-java-3.2.7-jar-with-dependencies.jar)

##Config
Configuration is done through a combination of filter init parameters and the `config.rb` file. The following filter init parameters are available to control the execution of the filter:

* **configLocation** - the location of the config.rb (default WEB-INF/sass/config.rb)
* **onlyRunWhenKey** - the system property or environment variable to check to see if sass compilation should run, use this to turn sass generation off in production
* **onlyRunWhenValue** - the corresponding value to check to see if sass compilation should run

You probably only want the css generation to run in development mode and then commit the generated css to source control. This is achieved by the `onlyRunWhenKey` and `onlyRunWhenValue` filter init parameters.

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

With this configuration the filter will check a system property or environment variable called RUNTIME_ENVIRONMENT and only run the sass compilation if that value is equal to local

See the [compass config documentation](http://compass-style.org/help/tutorials/configuration-reference/) to find out about all the wonderful things you can put in `config.rb`. For those config options that reference a file or directory, the working directory that compass will be executed in is the directory that contains `config.rb`.

##Try it out
1. git clone https://github.com/darrinholst/sass-java.git
2. cd sass-java
3. mvn jetty:run
4. wait for maven to download the internet
5. open http://localhost:8080/sass-java
6. change src/test/sample-webapp/WEB-INF/sass/application.scss
7. repeat step 5
8. be amazed
