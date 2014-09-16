package com.darrinholst;

import javax.servlet.FilterConfig;
import java.io.File;

public class Config {
    private FilterConfig filterConfig;

    public Config(FilterConfig filterConfig) {
        this.filterConfig = filterConfig;
    }

    public File getRootPath() {
        return new File(filterConfig.getServletContext().getRealPath("/"));
    }

    public String getString(String parameterName) {
        return getString(parameterName, null);
    }

    public String getString(String parameterName, String defaultValue) {
        String value = filterConfig.getInitParameter(parameterName);

        if (value == null) {
            value = defaultValue;
        }

        return value;
    }

    public boolean getBoolean(String parameterName, boolean defaultValue) {
        String value = filterConfig.getInitParameter(parameterName);

        if (value == null) {
            return defaultValue;
        } else {
            return Boolean.parseBoolean(value);
        }
    }
}
