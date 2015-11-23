package example;

import basic.FileUtil;
import client.FileClient;
import org.junit.Test;

import java.io.File;
import java.util.Optional;

/**
 * Created by samuelkolb on 23/11/15.
 *
 * @author Samuel Kolb
 */
public class EnumTest {

	@Test
	public void testEnum() {
		Optional<String[]> tasks = Optional.of(new String[]{"learn(test)"});
		File file = FileUtil.getLocalFile(getClass().getResource("/examples/enum/enum.run"));
		new FileClient(file, tasks).run();
	}
}
