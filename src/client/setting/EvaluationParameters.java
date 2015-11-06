package client.setting;

import configuration.Parameters;
import math.Range;

/**
 * Created by samuelkolb on 06/11/15.
 *
 * @author Samuel Kolb
 */
public class EvaluationParameters extends Parameters{

	public final Key<Range<Double>> splitSize = makeKey();

	public final Key<Range<Double>> prefSize = makeKey();

	public final Key<Range<Double>> errorSize = makeKey();
}
