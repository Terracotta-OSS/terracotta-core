/**
@COPYRIGHT@
 */
import com.tc.newconfig.source.ConfigSource;
import com.tc.newconfig.source.XMLFileSource;
import com.tc.object.config.DSOConfig;
import com.tc.object.config.DSOConfigAssembler;
import com.tc.object.config.DSOL1ConfigAssembler;
import com.tc.object.config.L1DSOConfig;
import com.tc.object.config.LockDefinition;
import com.tc.object.lockmanager.api.LockLevel;

import java.io.File;
import java.lang.reflect.Modifier;

import junit.framework.TestCase;

public class ConfigTest extends TestCase {
	ConfigSource[] sources;

	public void setUp() throws Exception {
		File projectRoot = new File(System.getProperty("user.dir"));
		File configDir = new File(projectRoot, "etc");
		System.out.println("configDir: " + configDir);
		File configFile = new File(configDir, "terracotta-config.xml");
		System.out.println("Config file: " + configFile);
		sources = new ConfigSource[] { new XMLFileSource(configFile) };
	}

	public void tests() throws Exception {
		DSOConfig dsoConfig = new DSOConfigAssembler(sources, null).dsoConfig();
		L1DSOConfig config = new DSOL1ConfigAssembler(sources, null)
				.l1DSOConfig();
		assertEquals("localhost", dsoConfig.getServerHost());
		assertEquals(9510, dsoConfig.getServerPort());

		classname = "demo.coordination.Main";
		fieldname = "participants";
		assertEquals("SharedDocumentRoot", config.rootNameFor(classname,
				fieldname));

		String[] classes = new String[] { classname };

		for (int i = 0; i < classes.length; i++) {
			if (!config.isAdaptable(classes[i])) {
				fail(classes[i] + " is not adaptable but should be");
			}
		}
	}
}
