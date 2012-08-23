package sortpom.parameter;

import org.apache.maven.plugin.MojoFailureException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import sortpom.util.SortPomImplUtil;

public class BackupFileExtensionParameterTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public final void emptyBackupFileExtensionShouldNotWork() throws Exception {
        thrown.expect(MojoFailureException.class);
        thrown.expectMessage("Could not create backup file, extension name was empty");

        SortPomImplUtil.create()
                .backupFileExtension("")
                .defaultOrderFileName("difforder/differentOrder.xml")
                .lineSeparator("\n")
                .testFiles("/full_unsorted_input.xml", "/sortOrderFiles/sorted_differentOrder.xml");
    }

    @Test
    public final void emptyBackupFileExtensionShouldNotWork2() throws Exception {
        thrown.expect(MojoFailureException.class);
        thrown.expectMessage("Could not create backup file, extension name was empty");

        SortPomImplUtil.create()
                .backupFileExtension("")
                .defaultOrderFileName("difforder/differentOrder.xml")
                .lineSeparator("\n")
                .testFiles("/full_unsorted_input.xml", "/sortOrderFiles/sorted_differentOrder.xml");
    }

}
