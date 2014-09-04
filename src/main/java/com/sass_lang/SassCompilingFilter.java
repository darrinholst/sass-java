package com.sass_lang;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class SassCompilingFilter implements Filter {
    private static final Logger LOG = LoggerFactory.getLogger(SassCompilingFilter.class);
    private static final int DWELL = 1000;
    protected static final String RETHROW_EXCEPTIONS_PARAM = "rethrowExceptions";
    protected static final String ONLY_RUN_KEY_PARAM = "onlyRunWhenKey";
    protected static final String ONLY_RUN_VALUE_PARAM = "onlyRunWhenValue";
    protected static final String CONFIG_LOCATION_PARAM = "configLocation";
    protected static final String DEFAULT_CONFIG_LOCATION = "WEB-INF" + File.separator + "sass" + File.separator + "config.rb";

    private long lastRun;
    private String onlyRunWhenKey;
    private String onlyRunWhenValue;
    private boolean rethrowExceptions;
    private Compiler compiler = new Compiler();
    private AtomicBoolean compiling = new AtomicBoolean(false);

    public void init(FilterConfig filterConfig) throws ServletException {
        Config config = new Config(filterConfig);
        onlyRunWhenKey = config.getString(ONLY_RUN_KEY_PARAM);
        onlyRunWhenValue = config.getString(ONLY_RUN_VALUE_PARAM);
        rethrowExceptions = config.getBoolean(RETHROW_EXCEPTIONS_PARAM, false);

        compiler.setConfigLocation(new File(
                config.getRootPath(),
                config.getString(CONFIG_LOCATION_PARAM, DEFAULT_CONFIG_LOCATION)
        ));

        if (environmentAllowsRunning()) compiler.compile();

    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        if (shouldRun()) {
            run();
        }

        while (compiling.get()) waitABit();

        filterChain.doFilter(servletRequest, servletResponse);
    }

    private void waitABit() {
        try {
            Thread.sleep(100L);
        } catch (InterruptedException e) {

        }
    }

    private void run() {
        LOG.debug("compiling sass");

        try {
            compiling.set(true);
            compiler.compile();
        } catch (Exception e) {
            LOG.warn("exception thrown while compiling sass", e);

            if (rethrowExceptions) {
                throw new RuntimeException(e);
            }
        } finally {
            compiling.set(false);
        }
    }

    private boolean shouldRun() {
        return !compiling.get() && environmentAllowsRunning() && timeToRun();
    }

    private boolean environmentAllowsRunning() {
        if (onlyRunWhenKey != null) {
            String value = System.getProperty(onlyRunWhenKey, System.getenv(onlyRunWhenKey));
            return onlyRunWhenValue.equalsIgnoreCase(value);
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

    public void destroy() {
    }

    public void setCompiler(Compiler compiler) {
        this.compiler = compiler;
    }
}
