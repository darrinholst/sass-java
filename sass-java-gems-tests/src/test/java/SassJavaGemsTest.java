import org.jruby.embed.ScriptingContainer;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SassJavaGemsTest {
    @Test
    public void correctSassIsPackaged() {
        assertEquals("3.4.4", new ScriptingContainer().runScriptlet("require 'sass';Sass.version[:number]"));
    }

    @Test
    public void correctCompassIsPackaged() {
        assertEquals("1.0.1", new ScriptingContainer().runScriptlet("require 'compass';Compass::VERSION"));
    }
}
