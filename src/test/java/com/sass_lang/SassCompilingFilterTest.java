package com.sass_lang;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.servlet.*;
import java.io.File;

import static java.util.Arrays.asList;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static com.sass_lang.SassCompilingFilter.*;

@RunWith(MockitoJUnitRunner.class)
public class SassCompilingFilterTest {
    @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock ServletRequest servletRequest;
    @Mock ServletResponse servletResponse;
    @Mock ServletContext servletContext;
    @Mock FilterConfig filterConfig;
    @Mock FilterChain filterChain;
    private String webAppRoot;

    private SassCompilingFilter filter;

    @Before
    public void setup() throws Exception {
        webAppRoot = temporaryFolder.newFolder("").getAbsolutePath();
        when(filterConfig.getServletContext()).thenReturn(servletContext);
        when(servletContext.getRealPath("/")).thenReturn(webAppRoot);

        filter = new SassCompilingFilter();
    }

    @Test
    public void compileFromDefaultLocations() throws Exception {
        setupDirectories();
        addScssFileTo(directory(DEFAULT_TEMPLATE_LOCATION), "foo");

        filter.init(filterConfig);
        filter.doFilter(servletRequest, servletResponse, filterChain);

        assertEquals(asList("foo.css"), asList(directory(DEFAULT_CSS_LOCATION).list()));
        assertTrue(directory(DEFAULT_CACHE_LOCATION).list().length > 0);
    }

    @Test
    public void compileFromSpecifiedLocations() throws Exception {
        String cacheLocation = "cache";
        String cssLocation = "css";
        String templateLocation = "template";

        when(filterConfig.getInitParameter("cacheLocation")).thenReturn(cacheLocation);
        when(filterConfig.getInitParameter("cssLocation")).thenReturn(cssLocation);
        when(filterConfig.getInitParameter("templateLocation")).thenReturn(templateLocation);
        setupDirectories(cacheLocation, cssLocation, templateLocation);
        addScssFileTo(directory(templateLocation), "foo");

        filter.init(filterConfig);
        filter.doFilter(servletRequest, servletResponse, filterChain);

        assertEquals(asList("foo.css"), asList(directory(cssLocation).list()));
        assertTrue(directory(cacheLocation).list().length > 0);
    }

    private void addScssFileTo(File directory, String name) throws Exception {
        assertTrue(new File(directory, name + ".scss").createNewFile());
    }

    private void setupDirectories() throws Exception {
        setupDirectories(DEFAULT_CACHE_LOCATION, DEFAULT_CSS_LOCATION, DEFAULT_TEMPLATE_LOCATION);
    }

    private void setupDirectories(String cacheLocation, String cssLocation, String templateLocation) {
        assertTrue(directory(cacheLocation).mkdirs());
        assertTrue(directory(cssLocation).mkdirs());
        assertTrue(directory(templateLocation).mkdirs());
    }

    private File directory(String sub) {
        return new File(webAppRoot, sub);
    }
}
