package com.sass_lang;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.servlet.*;
import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.sass_lang.SassCompilingFilter.*;
import static java.util.Arrays.asList;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SassCompilingFilterTest {
    private static final String SOME_OTHER_DIRECTORY = "someOtherDirectory";
    private static final String ONLY_RUN_KEY = "unit_test_environment";
    @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock ServletRequest servletRequest;
    @Mock ServletResponse servletResponse;
    @Mock ServletContext servletContext;
    @Mock FilterConfig filterConfig;
    @Mock FilterChain filterChain;
    private String webAppRoot;
    private FakeClock clock;

    private SassCompilingFilter filter;

    @Before
    public void setup() throws Exception {
        webAppRoot = temporaryFolder.newFolder("").getAbsolutePath();
        Clock.setDelegate(clock = new FakeClock());
        when(filterConfig.getServletContext()).thenReturn(servletContext);
        when(servletContext.getRealPath("/")).thenReturn(webAppRoot);

        filter = new SassCompilingFilter();
    }

    @After
    public void teardown() throws Exception {
        System.clearProperty(ONLY_RUN_KEY);
    }

    @Test
    public void compileFromAndToDefaultLocations() throws Exception {
        setupDefaultDirectories();
        addScssFileTo(fullPathOf(DEFAULT_TEMPLATE_LOCATION), "foo");

        initAndRunFilter();

        assertEquals(asList("foo.css"), directoryListing(DEFAULT_CSS_LOCATION));
    }

    @Test
    public void compilesFromAndToDefaultLocationsWhenRootPathEndsWithSlash() throws Exception {
        webAppRoot += File.separator;
        setupDefaultDirectories();
        addScssFileTo(fullPathOf(DEFAULT_TEMPLATE_LOCATION), "foo");

        initAndRunFilter();

        assertEquals(asList("foo.css"), directoryListing(DEFAULT_CSS_LOCATION));
    }

    @Test
    public void cachesToDefaultLocation() throws Exception {
        setupDefaultDirectories();
        addScssFileTo(fullPathOf(DEFAULT_TEMPLATE_LOCATION), "foo");

        initAndRunFilter();

        assertDirectoryNotEmpty(DEFAULT_CACHE_LOCATION);
    }

    @Test
    public void compileFromSpecifiedDirectory() throws Exception {
        setupDirectories(DEFAULT_CACHE_LOCATION, DEFAULT_CSS_LOCATION, SOME_OTHER_DIRECTORY);
        addScssFileTo(fullPathOf(SOME_OTHER_DIRECTORY), "foo");
        when(filterConfig.getInitParameter("templateLocation")).thenReturn(SOME_OTHER_DIRECTORY);

        initAndRunFilter();

        assertEquals(asList("foo.css"), directoryListing(DEFAULT_CSS_LOCATION));
    }

    @Test
    public void compileToSpecifiedDirectory() throws Exception {
        setupDirectories(DEFAULT_CACHE_LOCATION, SOME_OTHER_DIRECTORY, DEFAULT_TEMPLATE_LOCATION);
        addScssFileTo(fullPathOf(DEFAULT_TEMPLATE_LOCATION), "foo");
        when(filterConfig.getInitParameter("cssLocation")).thenReturn(SOME_OTHER_DIRECTORY);

        initAndRunFilter();

        assertEquals(asList("foo.css"), directoryListing(SOME_OTHER_DIRECTORY));
    }

    @Test
    public void cachesToSpecifiedDirectory() throws Exception {
        setupDirectories(SOME_OTHER_DIRECTORY, DEFAULT_CSS_LOCATION, DEFAULT_TEMPLATE_LOCATION);
        addScssFileTo(fullPathOf(DEFAULT_TEMPLATE_LOCATION), "foo");
        when(filterConfig.getInitParameter("cacheLocation")).thenReturn(SOME_OTHER_DIRECTORY);

        initAndRunFilter();

        assertDirectoryNotEmpty(SOME_OTHER_DIRECTORY);
    }

    @Test
    public void doesNotRunIfLastRunWasLessThanTwoSecondsAgo() throws Exception {
        setupDefaultDirectories();
        addScssFileTo(fullPathOf(DEFAULT_TEMPLATE_LOCATION), "foo");

        initAndRunFilter();

        assertDirectoryNotEmpty(DEFAULT_CSS_LOCATION);
        clearDirectory(DEFAULT_CSS_LOCATION);
        assertDirectoryEmpty(DEFAULT_CSS_LOCATION);

        runFilter();

        assertDirectoryEmpty(DEFAULT_CSS_LOCATION);
    }

    @Test
    public void runsIfLastRunWasGreaterThanTwoSecondsAgo() throws Exception {
        setupDefaultDirectories();
        addScssFileTo(fullPathOf(DEFAULT_TEMPLATE_LOCATION), "foo");

        initAndRunFilter();

        assertDirectoryNotEmpty(DEFAULT_CSS_LOCATION);
        clearDirectory(DEFAULT_CSS_LOCATION);
        assertDirectoryEmpty(DEFAULT_CSS_LOCATION);
        clock.incrementSeconds(2);

        runFilter();

        assertDirectoryNotEmpty(DEFAULT_CSS_LOCATION);
    }

    @Test
    public void runsOnlyInDevelopmentMode() throws Exception {
        setupDefaultDirectories();
        addScssFileTo(fullPathOf(DEFAULT_TEMPLATE_LOCATION), "foo");
        when(filterConfig.getInitParameter(ONLY_RUN_KEY_PARAM)).thenReturn(ONLY_RUN_KEY);
        when(filterConfig.getInitParameter(ONLY_RUN_VALUE_PARAM)).thenReturn("development");
        System.setProperty(ONLY_RUN_KEY, "development");

        initAndRunFilter();

        assertDirectoryNotEmpty(DEFAULT_CSS_LOCATION);
    }

    @Test
    public void doesNotRunInProductionMode() throws Exception {
        setupDefaultDirectories();
        addScssFileTo(fullPathOf(DEFAULT_TEMPLATE_LOCATION), "foo");
        when(filterConfig.getInitParameter(ONLY_RUN_KEY_PARAM)).thenReturn(ONLY_RUN_KEY);
        when(filterConfig.getInitParameter(ONLY_RUN_VALUE_PARAM)).thenReturn("development");
        System.setProperty(ONLY_RUN_KEY, "production");

        initAndRunFilter();

        assertDirectoryEmpty(DEFAULT_CSS_LOCATION);
    }

    private void initAndRunFilter() throws ServletException, IOException {
        filter.init(filterConfig);
        runFilter();
    }

    private void runFilter() throws ServletException, IOException {
        filter.doFilter(servletRequest, servletResponse, filterChain);
    }

    private void addScssFileTo(File directory, String name) throws Exception {
        assertTrue(new File(directory, name + ".scss").createNewFile());
    }

    private void setupDefaultDirectories() throws Exception {
        setupDirectories(DEFAULT_CACHE_LOCATION, DEFAULT_CSS_LOCATION, DEFAULT_TEMPLATE_LOCATION);
    }

    private void setupDirectories(String cacheLocation, String cssLocation, String templateLocation) {
        assertTrue(fullPathOf(cacheLocation).mkdirs());
        assertTrue(fullPathOf(cssLocation).mkdirs());
        assertTrue(fullPathOf(templateLocation).mkdirs());
    }

    private File fullPathOf(String directory) {
        return new File(webAppRoot, directory);
    }

    private void assertDirectoryNotEmpty(String directoryName) {
        assertTrue(directoryName + " should not have been empty", directoryListing(directoryName).size() > 0);
    }

    private void assertDirectoryEmpty(String directoryName) {
        List list = directoryListing(directoryName);
        assertTrue("didn't expect " + list.toString() + " to be in " + directoryName, list.size() == 0);
    }

    private void clearDirectory(String directoryName) {
        List<String> filenames = directoryListing(directoryName);

        for (String filename : filenames) {
            File file = new File(fullPathOf(directoryName), filename);
            assertTrue("trying to delete " + filename, file.delete());
        }
    }

    private List<String> directoryListing(String directoryName) {
        return asList(fullPathOf(directoryName).list());
    }
}
