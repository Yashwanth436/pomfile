package sortpom.wrapper;

import org.apache.maven.plugin.MojoFailureException;
import org.jdom.Text;
import org.junit.Before;
import org.junit.Test;
import sortpom.parameter.PluginParametersBuilder;

import static org.junit.Assert.assertEquals;

/**
 * @author bjorn
 * @since 2012-06-19
 */
public class TextWrapperCreatorTest {
    private TextWrapperCreator textWrapperCreator = new TextWrapperCreator();

    @Before
    public void setup() throws MojoFailureException {
        textWrapperCreator.setup(new PluginParametersBuilder().setEncoding("UTF-8")
                .setFormatting("\n", true, true).createPluginParameters());
    }

    @Test
    public void testIsEmptyLine() {
        assertEquals(false, textWrapperCreator.isBlankLineOrLines(new Text("\n      sortpom\n  ")));
        assertEquals(false, textWrapperCreator.isBlankLineOrLines(new Text("sortpom")));
        assertEquals(true, textWrapperCreator.isBlankLineOrLines(new Text("\n  ")));
        assertEquals(true, textWrapperCreator.isBlankLineOrLines(new Text("  \n  ")));
        assertEquals(true, textWrapperCreator.isBlankLineOrLines(new Text("  \n\n  ")));
        assertEquals(true, textWrapperCreator.isBlankLineOrLines(new Text("\n\n")));
        assertEquals(true, textWrapperCreator.isBlankLineOrLines(new Text("  \r  ")));
        assertEquals(true, textWrapperCreator.isBlankLineOrLines(new Text("  \r\r  ")));
        assertEquals(true, textWrapperCreator.isBlankLineOrLines(new Text("\r\r")));
        assertEquals(true, textWrapperCreator.isBlankLineOrLines(new Text("  \r\n  ")));
        assertEquals(true, textWrapperCreator.isBlankLineOrLines(new Text("  \r\n\r\n  ")));
        assertEquals(true, textWrapperCreator.isBlankLineOrLines(new Text("\r\n\r\n")));
        assertEquals(false, textWrapperCreator.isBlankLineOrLines(new Text("  ")));
    }
}
