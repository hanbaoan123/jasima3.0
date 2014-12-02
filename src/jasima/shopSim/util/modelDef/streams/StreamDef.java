package jasima.shopSim.util.modelDef.streams;

import jasima.core.random.continuous.DblStream;
import jasima.shopSim.util.modelDef.PropertySupport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

public abstract class StreamDef extends PropertySupport implements Cloneable {

	public interface StreamDefFact {

		public String getTypeString();

		public StreamDef stringToStreamDef(String params, List<String> errors);

	}

	public StreamDef() {
		super();
	}

	public abstract DblStream createStream();

	public static StreamDef parseDblStream(String s, List<String> errors) {
		StringTokenizer sst = new StringTokenizer(s, "()", false);
		ArrayList<String> ss = new ArrayList<String>();
		while (sst.hasMoreTokens()) {
			ss.add(sst.nextToken().trim());
		}
		if (ss.size() != 2) {
			errors.add("invalid stream configuration '" + s + "'");
			return null;
		}

		String type = ss.get(0);
		String parms = ss.get(1);

		StreamDefFact fact = streamFactoryReg.get(type);
		if (fact == null) {
			errors.add(String.format(
					"Invalid stream type '%s'. Supported types are: '%s'.",
					type,
					streamFactoryReg.keySet().toString()
							.replaceAll("[\\[\\]]", "")));
			return null;
		}

		StreamDef res = fact.stringToStreamDef(parms, errors);
		return res;
	}

	@Override
	public StreamDef clone() throws CloneNotSupportedException {
		return (StreamDef) super.clone();
	}

	private static HashMap<String, StreamDefFact> streamFactoryReg;

	public static void registerStreamFactory(StreamDefFact fact) {
		streamFactoryReg.put(fact.getTypeString(), fact);
	}

	static {
		streamFactoryReg = new HashMap<String, StreamDefFact>();

		@SuppressWarnings("unused")
		Class<?> c;

		// trigger class load, so sub-classes can register themselves
		c = DblConstDef.class;
		c = DblExponentialDef.class;
		c = DblUniformDef.class;
		c = DblTriangularDef.class;
		c = IntUniformDef.class;
		c = IntEmpDef.class;
		c = IntConstDef.class;
	}

}
