package com.sass_lang;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.servlet.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.sass_lang.SassCompilingFilter.*;
import static java.util.Arrays.asList;
import static junit.framework.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

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
        when(filterConfig.getInitParameter(RETHROW_EXCEPTIONS_PARAM)).thenReturn("true");
        when(servletContext.getRealPath("/")).thenReturn(webAppRoot);

        filter = new SassCompilingFilter();
    }

    @After
    public void teardown() throws Exception {
        System.clearProperty(ONLY_RUN_KEY);
    }

    @Test
    public void initShouldCompileFiles() throws ServletException {
        Compiler compiler = mock(Compiler.class);

        filter.setCompiler(compiler);

        initFilter();

        verify(compiler).compile();
    }

    @Test
    public void compilerShouldNotBeInvokedIfItIsAlreadyCompiling() throws Exception {
        StubCompiler compiler = new StubCompiler(1L, 2000L, 1L);
        filter.setCompiler(compiler);

        setupDefaultDirectories();
        initFilter();

        Thread thread = processRequestOnAnotherThread(servletRequest);
        clock.incrementSeconds(4);
        runFilter(servletRequest);

        thread.join();

        assertEquals(2, compiler.getNumberOfCompiles());
    }

    @Test
    public void requestsShouldBlockUntilTheCompilingHasCompleted() throws Exception {
        ArgumentCaptor<ServletRequest> captor = ArgumentCaptor.forClass(ServletRequest.class);
        filter.setCompiler(new StubCompiler(2000L, 1L));
        final ServletRequest request = mock(ServletRequest.class, "request");
        ServletRequest otherRequest = mock(ServletRequest.class, "otherRequest");

        setupDefaultDirectories();
        initFilter();

        Thread thread = processRequestOnAnotherThread(request);
        runFilter(otherRequest);

        thread.join();

        verify(filterChain, times(2)).doFilter(captor.capture(), eq(servletResponse));
        assertEquals(Arrays.asList(request, otherRequest), captor.getAllValues());
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
    public void canTurnCachingOff() throws Exception {
        setupDefaultDirectories();
        addScssFileTo(fullPathOf(DEFAULT_TEMPLATE_LOCATION), "foo");

        initAndRunFilter(CACHE_PARAM, "false");

        assertDirectoryEmpty(DEFAULT_CACHE_LOCATION);
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

    @Test
    public void importWorks() throws Exception {
        setupDefaultDirectories();
        addScssFileTo(fullPathOf(DEFAULT_TEMPLATE_LOCATION), "_other", "body {color: #000}");
        addScssFileTo(fullPathOf(DEFAULT_TEMPLATE_LOCATION), "base", "@import 'other';");

        initAndRunFilter();

        assertEquals(asList("base.css"), directoryListing(DEFAULT_CSS_LOCATION));
        String expected = "body {" + System.getProperty("line.separator") + "  color: #000; }";
        assertEquals(expected, contentsOf(fullPathOf(DEFAULT_CSS_LOCATION), "base.css").trim());
    }

    @Test
    public void multipleThreads() throws Exception {
        setupDefaultDirectories();
        initFilter();

        CountDownLatch latch = new CountDownLatch(2);

        FilterThread thread1 = new FilterThread(latch);
        FilterThread thread2 = new FilterThread(latch);

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();

        assertFalse("exception thrown in 1st thread", thread1.exceptionThrown);
        assertFalse("exception thrown in 2nd thread", thread2.exceptionThrown);
    }

    private class FilterThread extends Thread {
        private CountDownLatch latch;
        public boolean exceptionThrown;

        public FilterThread(CountDownLatch countdown) {
            this.latch = countdown;
        }

        @Override
        public void run() {
            try {
                latch.countDown();
                latch.await();
                runFilter();
            } catch (Exception e) {
                exceptionThrown = true;
                throw new RuntimeException(e);
            }
        }
    }

    private Thread processRequestOnAnotherThread(final ServletRequest request) throws InterruptedException {
        final AtomicBoolean requestProcessing = new AtomicBoolean(false);
        Thread thread = new Thread(new Runnable() {
            public void run() {
                requestProcessing.set(true);
                runFilter(request);
            }
        });
        thread.start();

        while (!requestProcessing.get()) {
            Thread.sleep(10L);
        }

        return thread;
    }

    private String contentsOf(File directory, String filename) throws Exception {
        return FileUtils.readFileToString(new File(directory, filename));
    }

    private void initAndRunFilter(String... parameters) throws ServletException, IOException {
        initFilter(parameters);
        runFilter();
    }

    private void initFilter(String... parameters) throws ServletException {
        for (int i = 0; i < parameters.length; i += 2) {
            when(filterConfig.getInitParameter(parameters[i])).thenReturn(parameters[i + 1]);
        }

        filter.init(filterConfig);
    }

    private void runFilter() {
        runFilter(servletRequest);
    }

    private void runFilter(ServletRequest request) {
        try {
            filter.doFilter(request, servletResponse, filterChain);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void addScssFileTo(File directory, String name) throws Exception {
        addScssFileTo(directory, name, "");
    }

    private void addScssFileTo(File directory, String name, String content) throws Exception {
        File file = new File(directory, name + ".scss");
        assertTrue(file.createNewFile());
        FileOutputStream output = new FileOutputStream(file);
        output.write(content.getBytes());
        output.close();
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

    private class StubCompiler extends Compiler {
        private ArrayList<Long> compileTimes = new ArrayList<Long>();
        private int i;

        private StubCompiler(Long... compileTimes) {
            this.compileTimes.addAll(Arrays.asList(compileTimes));
        }

        @Override
        public void compile() {
            try {
                Thread.sleep(compileTimes.get(i++));
            } catch (InterruptedException e) {
            }
        }

        public int getNumberOfCompiles() {
            return i;
        }
    }
}
