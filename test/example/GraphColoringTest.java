package example;

import static org.junit.Assert.*;

import basic.FileUtil;
import client.FileClient;
import org.junit.Test;

import java.io.File;
import java.util.Optional;

/**
 * Created by samuelkolb on 17/11/15.
 *
 * @author Samuel Kolb
 */
public class GraphColoringTest {

	@Test
	public void testGraphColoring() {
		Optional<String[]> tasks = Optional.of(new String[]{"learn(minimal)"});
		File file = FileUtil.getLocalFile(getClass().getResource("/examples/coloring/coloring.run"));
		new FileClient(file, tasks).run();
	}
}
