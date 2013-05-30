package com.sass_lang;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class SassCompilingFilter implements Filter {
    private static final Logger LOG = LoggerFactory.getLogger(SassCompilingFilter.class);
    private static final int DWELL = 2000;
    protected static final String ONLY_RUN_KEY_PARAM = "onlyRunWhenKey";
    protected static final String ONLY_RUN_VALUE_PARAM = "onlyRunWhenValue";
    protected static final String RETHROW_EXCEPTIONS_PARAM = "rethrowExceptions";
    protected static final String CACHE_LOCATION_PARAM = "cacheLocation";
    protected static final String CSS_LOCATION_PARAM = "cssLocation";
    protected static final String TEMPLATE_LOCATION_PARAM = "templateLocation";
    protected static final String CACHE_PARAM = "cache";

    protected static final String DEFAULT_CACHE_LOCATION = "WEB-INF" + File.separator + ".sass-cache";
    protected static final String DEFAULT_CSS_LOCATION = "stylesheets";
    protected static final String DEFAULT_TEMPLATE_LOCATION = "WEB-INF" + File.separator + "sass";


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

        compiler.setCacheLocation(getCacheLocation(config));
        compiler.setCssLocation(getCssLocation(config));
        compiler.setTemplateLocation(getTemplateLocation(config));
        compiler.setCache(config.getBoolean(CACHE_PARAM, true));
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

    private File getTemplateLocation(Config config) {
        return fromRoot(config, config.getString(TEMPLATE_LOCATION_PARAM, DEFAULT_TEMPLATE_LOCATION));
    }

    private File fromRoot(Config config, String directory) {
        return new File(config.getRootPath(), directory);
    }

    private File getCssLocation(Config config) {
        return fromRoot(config, config.getString(CSS_LOCATION_PARAM, DEFAULT_CSS_LOCATION));
    }

    private File getCacheLocation(Config config) {
        return fromRoot(config, config.getString(CACHE_LOCATION_PARAM, DEFAULT_CACHE_LOCATION));
    }

    public void destroy() {
    }

    public void setCompiler(Compiler compiler) {
        this.compiler = compiler;
    }
}
