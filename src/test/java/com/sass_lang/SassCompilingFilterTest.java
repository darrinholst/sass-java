package com.sass_lang;

import org.apache.commons.io.FileUtils;
import org.junit.*;
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
    private static final String CSS_LOCATION = "css";
    private static final String SASS_LOCATION = "WEB-INF/sass";
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    ServletRequest servletRequest;
    @Mock
    ServletResponse servletResponse;
    @Mock
    ServletContext servletContext;
    @Mock
    FilterConfig filterConfig;
    @Mock
    FilterChain filterChain;
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
    public void requestsShouldBlockUntilTheCompilingHasCompleted() throws Exception {
        ArgumentCaptor<ServletRequest> captor = ArgumentCaptor.forClass(ServletRequest.class);
        filter.setCompiler(new StubCompiler(2000L, 1L));
        final ServletRequest request = mock(ServletRequest.class, "request");
        ServletRequest otherRequest = mock(ServletRequest.class, "otherRequest");

        setupDefaultDirectoriesAndConfigFile();
        initFilter();

        Thread thread = processRequestOnAnotherThread(request);
        runFilter(otherRequest);

        thread.join();

        verify(filterChain, times(2)).doFilter(captor.capture(), eq(servletResponse));
        assertEquals(Arrays.asList(request, otherRequest), captor.getAllValues());
    }

    @Test
    public void compiles() throws Exception {
        setupDefaultDirectoriesAndConfigFile();
        addScssFileTo(fullPathOf(SASS_LOCATION), "foo");

        initAndRunFilter();

        assertEquals(asList("foo.css"), directoryListing(CSS_LOCATION));
    }

    @Test
    public void doesNotRunIfLastRunWasLessThanOneSecondAgo() throws Exception {
        setupDefaultDirectoriesAndConfigFile();
        addScssFileTo(fullPathOf(SASS_LOCATION), "foo");

        initAndRunFilter();

        assertDirectoryNotEmpty(CSS_LOCATION);
        clearDirectory(CSS_LOCATION);
        assertDirectoryEmpty(CSS_LOCATION);

        runFilter();

        assertDirectoryEmpty(CSS_LOCATION);
    }

    @Test
    public void runsIfLastRunWasGreaterThanOneSecondAgo() throws Exception {
        setupDefaultDirectoriesAndConfigFile();
        addScssFileTo(fullPathOf(SASS_LOCATION), "foo");

        initAndRunFilter();

        assertDirectoryNotEmpty(CSS_LOCATION);
        clearDirectory(CSS_LOCATION);
        assertDirectoryEmpty(CSS_LOCATION);
        clock.incrementSeconds(1);

        runFilter();

        assertDirectoryNotEmpty(CSS_LOCATION);
    }

    @Test
    public void runsOnlyInDevelopmentMode() throws Exception {
        setupDefaultDirectoriesAndConfigFile();
        addScssFileTo(fullPathOf(SASS_LOCATION), "foo");
        when(filterConfig.getInitParameter(ONLY_RUN_KEY_PARAM)).thenReturn(ONLY_RUN_KEY);
        when(filterConfig.getInitParameter(ONLY_RUN_VALUE_PARAM)).thenReturn("development");
        System.setProperty(ONLY_RUN_KEY, "development");

        initAndRunFilter();

        assertDirectoryNotEmpty(CSS_LOCATION);
    }

    @Test
    public void doesNotRunInProductionMode() throws Exception {
        setupDefaultDirectoriesAndConfigFile();
        addScssFileTo(fullPathOf(SASS_LOCATION), "foo");
        when(filterConfig.getInitParameter(ONLY_RUN_KEY_PARAM)).thenReturn(ONLY_RUN_KEY);
        when(filterConfig.getInitParameter(ONLY_RUN_VALUE_PARAM)).thenReturn("development");
        System.setProperty(ONLY_RUN_KEY, "production");

        initAndRunFilter();

        assertDirectoryEmpty(CSS_LOCATION);
    }

    @Test
    public void importWorks() throws Exception {
        setupDefaultDirectoriesAndConfigFile();
        addScssFileTo(fullPathOf(SASS_LOCATION), "_other", "body {color: #000}");
        addScssFileTo(fullPathOf(SASS_LOCATION), "base", "@import 'other';");

        initAndRunFilter();

        assertEquals(asList("base.css"), directoryListing(CSS_LOCATION));
        String expected = "body{color:#000}";
        assertEquals(expected, contentsOf(fullPathOf(CSS_LOCATION), "base.css").trim());
    }

    @Test
    public void multipleThreads() throws Exception {
        setupDefaultDirectoriesAndConfigFile();
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

    private void setupDefaultDirectoriesAndConfigFile() throws Exception {
        setupDirectories(CSS_LOCATION, SASS_LOCATION);

        File file = new File(fullPathOf(SASS_LOCATION), "config.rb");
        assertTrue(file.createNewFile());
        FileOutputStream output = new FileOutputStream(file);
        output.write(("" +
                "css_dir = '../../" + CSS_LOCATION + "'\n" +
                "sass_dir = '.'\n" +
                "line_comments = false\n" +
                "output_style = :compressed\n").getBytes());
        output.close();
    }

    private void setupDirectories(String cssLocation, String sassLocation) {
        assertTrue(fullPathOf(cssLocation).mkdirs());
        assertTrue(fullPathOf(sassLocation).mkdirs());
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
    }
}
