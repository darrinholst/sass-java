# Overview
Compile sass to css on-the-fly via a j2ee servlet filter

## Usage

Add the dependency to `pom.xml`

```xml
<dependency>
    <groupId>com.darrinholst</groupId>
    <artifactId>sass-java</artifactId>
    <version>3.4.4</version>
</dependency>
```

Add the filter to `web.xml`

```xml
<filter>
    <filter-name>SassCompiler</filter-name>
    <filter-class>com.darrinholst.sass_java.SassCompilingFilter</filter-class>
</filter>

<filter-mapping>
    <filter-name>SassCompiler</filter-name>
    <url-pattern>*.css</url-pattern>
</filter-mapping>
```

Create a `WEB-INF/sass/config.rb`

```ruby
css_dir = "../../stylesheets"
sass_dir = "."
```
    
Put your sass templates in `WEB-INF/sass` and each request for a css
file will regenerate as needed.

##Versioning
The first 3 nodes of the version will match the version of sass that is being used. Current versions of sass and compass can be found in the [Gemfile](https://github.com/darrinholst/sass-java/blob/master/sass-java-gems/Gemfile).

##Configuring
Configuration is done through a combination of filter init parameters and the `config.rb` file. The following filter init parameters are available to control the execution of the filter:

* **configLocation** - the location of the config.rb (default WEB-INF/sass/config.rb)
* **onlyRunWhenKey** - the system property or environment variable to check to see if sass compilation should run, use this to turn sass generation off in production
* **onlyRunWhenValue** - the corresponding value to check to see if sass compilation should run

A common practice is to turn sass generation off in production and precompile in your build process. An example of how to do this based off a system property is:

```xml
<filter>
    <filter-name>SassCompiler</filter-name>
    <filter-class>com.darrinholst.sass_java.SassCompilingFilter</filter-class>
    <init-param>
        <param-name>onlyRunWhenKey</param-name>
        <param-value>RUNTIME_ENVIRONMENT</param-value>
    </init-param>
    <init-param>
        <param-name>onlyRunWhenValue</param-name>
        <param-value>local</param-value>
    </init-param>
</filter>
```

With this configuration the filter will check a system property or environment variable called RUNTIME_ENVIRONMENT and only run the sass compilation if that value is equal to local

See the [compass config documentation](http://compass-style.org/help/documentation/configuration-reference/) to find out about all the wonderful things you can put in `config.rb`. For those config options that reference a file or directory, the working directory that compass will be executed in is the directory that contains `config.rb`.

##Precompiling
Use the [maven plugin](https://github.com/darrinholst/sass-java/blob/master/sass-java-maven/README.md)

##Development
The magic behind how this works comes from packaging up the sass gems into a jar that the filter can then use via jruby. The process to jar up the gems is described [here](http://blog.nicksieger.com/articles/2009/01/10/jruby-1-1-6-gems-in-a-jar/). That process is mavenized [here](https://github.com/darrinholst/sass-java/blob/master/sass-java-gems/pom.xml#L34). So to change sass or compass versions All Ya Gotta Do™ is update the [Gemfile](https://github.com/darrinholst/sass-java/blob/master/sass-java-gems/Gemfile) and `mvn clean install`.

##Releasing
First time? [Read this](http://central.sonatype.org/pages/working-with-pgp-signatures.html)  
`mvn release:prepare release:peform`  
That worked? [Read this](http://central.sonatype.org/pages/releasing-the-deployment.html)  

##Try it out
1. git clone https://github.com/darrinholst/sass-java.git
2. cd sass-java
3. mvn install
4. mvn jetty:run -pl sass-java
5. open http://localhost:8080
6. change src/test/sample-webapp/WEB-INF/sass/application.scss
7. refresh
