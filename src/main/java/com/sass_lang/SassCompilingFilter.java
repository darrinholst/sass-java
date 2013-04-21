package com.sass_lang;

import org.jruby.embed.ScriptingContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public class SassCompilingFilter implements Filter {
    private static final Logger LOG = LoggerFactory.getLogger(SassCompilingFilter.class);
    private static final int DWELL = 1000;
    protected static final String RETHROW_EXCEPTIONS_PARAM = "rethrowExceptions";
    protected static final String ONLY_RUN_KEY_PARAM = "onlyRunWhenKey";
    protected static final String ONLY_RUN_VALUE_PARAM = "onlyRunWhenValue";
    protected static final String CONFIG_LOCATION_PARAM = "configLocation";
    protected static final String DEFAULT_CONFIG_LOCATION = "WEB-INF" + File.separator + "sass" + File.separator + "config.rb";

    private String rootWebPath;
    private long lastRun;
    private String onlyRunWhenKey;
    private String onlyRunWhenValue;
    private boolean rethrowExceptions;
    private String configLocation;
    private boolean initialized;

    public void init(FilterConfig filterConfig) throws ServletException {
        rootWebPath = new File(filterConfig.getServletContext().getRealPath("/")).getAbsolutePath();

        Config config = new Config(filterConfig);
        onlyRunWhenKey = config.getString(ONLY_RUN_KEY_PARAM);
        onlyRunWhenValue = config.getString(ONLY_RUN_VALUE_PARAM);
        rethrowExceptions = config.getBoolean(RETHROW_EXCEPTIONS_PARAM, false);
        configLocation = config.getString(CONFIG_LOCATION_PARAM, DEFAULT_CONFIG_LOCATION);
    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        if (shouldRun()) {
            run();
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }

    private void run() {
        LOG.debug("compiling sass");

        try {
            initialize();
            compile();
        } catch (Exception e) {
            LOG.warn("exception thrown while compiling sass", e);

            if (rethrowExceptions) {
                throw new RuntimeException(e);
            }
        }
    }

    private void initialize() {
        if (!initialized) {
            new ScriptingContainer().runScriptlet(buildInitializationScript());
            initialized = true;
        }
    }

    private void compile() {
        new ScriptingContainer().runScriptlet(buildCompileScript());
    }

    private boolean shouldRun() {
        return environmentAllowsRunning() && timeToRun();
    }

    private boolean environmentAllowsRunning() {
        if (onlyRunWhenKey != null) {
            String value = System.getProperty(onlyRunWhenKey, System.getenv(onlyRunWhenKey));
            return value.equals(onlyRunWhenValue);
        }

        return true;
    }

    private synchronized boolean timeToRun() {
        long now = Clock.now().getTime();

        if (now - lastRun >= DWELL) {
            lastRun = Clock.now().getTime();
            return true;
        } else {
            return false;
        }
    }

    private String buildInitializationScript() {
        StringWriter raw = new StringWriter();
        PrintWriter script = new PrintWriter(raw);

        script.println("require 'rubygems'                                                         ");
        script.println("require 'compass'                                                          ");
        script.println("frameworks = Dir.new(Compass::Frameworks::DEFAULT_FRAMEWORKS_PATH).path    ");
        script.println("Compass::Frameworks.register_directory(File.join(frameworks, 'compass'))   ");
        script.println("Compass::Frameworks.register_directory(File.join(frameworks, 'blueprint')) ");
        script.println("Compass.add_project_configuration '" + getConfigLocation() + "'            ");
        script.println("Compass.configure_sass_plugin!                                             ");
        script.flush();

        return raw.toString();
    }

    private String buildCompileScript() {
        StringWriter raw = new StringWriter();
        PrintWriter script = new PrintWriter(raw);

        script.println("Dir.chdir(File.dirname('" + getConfigLocation() + "')) do ");
        script.println("  Compass.compiler.run                                    ");
        script.println("end                                                       ");
        script.flush();

        return raw.toString();
    }

    private String getConfigLocation() {
        return replaceSlashes(fullPath(configLocation));
    }

    private String fullPath(String directory) {
        return rootWebPath + File.separator + directory;
    }

    private String replaceSlashes(String path) {
        return path.replaceAll("\\\\", "/");
    }

    public void destroy() {
    }
}
