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
    private static final int DWELL = 2000;
    protected static final String RETHROW_EXCEPTIONS_PARAM = "rethrowExceptions";
    protected static final String ONLY_RUN_KEY_PARAM = "onlyRunWhenKey";
    protected static final String ONLY_RUN_VALUE_PARAM = "onlyRunWhenValue";
    protected static final String TEMPLATE_LOCATION_PARAM = "templateLocation";
    protected static final String CSS_LOCATION_PARAM = "cssLocation";
    protected static final String CACHE_LOCATION_PARAM = "cacheLocation";
    protected static final String CACHE_PARAM = "cache";
    protected static final String DEFAULT_TEMPLATE_LOCATION = "WEB-INF" + File.separator + "sass";
    protected static final String DEFAULT_CSS_LOCATION = "stylesheets";
    protected static final String DEFAULT_CACHE_LOCATION = "WEB-INF" + File.separator + ".sass-cache";

    private String rootWebPath;
    private String updateScript;
    private long lastRun;
    private String onlyRunWhenKey;
    private String onlyRunWhenValue;
    private boolean rethrowExceptions;
    private String templateLocation;
    private String cssLocation;
    private String cacheLocation;
    private boolean cache;

    public void init(FilterConfig filterConfig) throws ServletException {
        rootWebPath = new File(filterConfig.getServletContext().getRealPath("/")).getAbsolutePath();

        Config config = new Config(filterConfig);
        templateLocation = config.getString(TEMPLATE_LOCATION_PARAM, DEFAULT_TEMPLATE_LOCATION);
        cssLocation = config.getString(CSS_LOCATION_PARAM, DEFAULT_CSS_LOCATION);
        cacheLocation = config.getString(CACHE_LOCATION_PARAM, DEFAULT_CACHE_LOCATION);
        onlyRunWhenKey = config.getString(ONLY_RUN_KEY_PARAM);
        onlyRunWhenValue = config.getString(ONLY_RUN_VALUE_PARAM);
        rethrowExceptions = config.getBoolean(RETHROW_EXCEPTIONS_PARAM, false);
        cache = config.getBoolean(CACHE_PARAM, true);
        updateScript = buildUpdateScript();
    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        if(shouldRun()) {
            run();
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }

    private void run() {
        LOG.debug("compiling sass");

        try {
            new ScriptingContainer().runScriptlet(updateScript);
        } catch(Exception e) {
            LOG.warn("exception thrown while compiling sass", e);

            if(rethrowExceptions) {
                throw new RuntimeException(e);
            }
        }
    }

    private boolean shouldRun() {
        return environmentAllowsRunning() && timeToRun();
    }

    private boolean environmentAllowsRunning() {
        if(onlyRunWhenKey != null) {
            String value = System.getProperty(onlyRunWhenKey, System.getenv(onlyRunWhenKey));
            return value.equals(onlyRunWhenValue);
        }

        return true;
    }

    private synchronized boolean timeToRun() {
        long now = Clock.now().getTime();

        if(now - lastRun >= DWELL) {
            lastRun = Clock.now().getTime();
            return true;
        } else {
            return false;
        }
    }

    private String buildUpdateScript() {
        StringWriter raw = new StringWriter();
        PrintWriter script = new PrintWriter(raw);

        script.println("  require 'rubygems'                                       ");
        script.println("  require 'sass/plugin'                                    ");
        script.println("  Sass::Plugin.options.merge!(                             ");
        script.println("    :template_location => '" + getTemplateLocation() + "', ");
        script.println("    :css_location => '" + getCssLocation() + "',           ");
        script.println("    :cache => " + wantsCaching() + ",                      ");
        script.println("    :cache_store => nil,                                   ");
        script.println("    :cache_location => '" + getCacheLocation() + "'        ");
        script.println("  )                                                        ");
        script.println("  Sass::Plugin.check_for_updates                           ");
        script.flush();

        return raw.toString();
    }

    protected boolean wantsCaching() {
        return cache;
    }

    private String getCacheLocation() {
        return replaceSlashes(fullPath(cacheLocation));
    }

    private String getCssLocation() {
        return replaceSlashes(fullPath(cssLocation));
    }

    private String getTemplateLocation() {
        return replaceSlashes(fullPath(templateLocation));
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
