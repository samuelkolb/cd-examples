package client.setting;

import clausal_discovery.core.score.ClauseFunction;
import org.json.simple.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Created by samuelkolb on 03/11/15.
 *
 * @author Samuel Kolb
 */
public class Parameters {

	public class Key<T> {

		/**
		 * Sets the parameter to the value specified in the given JSONObject
		 * @param object	The object
		 * @param name		The name of the parameter
		 * @return True iff a value could be found for the given name
		 */
		public boolean setFromJson(JSONObject object, String name) {
			if(object.containsKey(name)) {
				//noinspection unchecked
				set((T) object.get(name));
				return true;
			}
			return false;
		}

		/**
		 * Sets the parameter to the given value
		 * @param value	The value
		 */
		public void set(T value) {
			values.put(this, value);
		}

		/**
		 * Retrieves the parameter
		 * @return	The parameter value
		 * @throws java.util.NoSuchElementException Iff no value was set
		 */
		public T get() throws NoSuchElementException {
			if(!values.containsKey(this)) {
				throw new NoSuchElementException();
			}
			//noinspection unchecked
			return (T) values.get(this);
		}
	}

	private Map<Key, Object> values = new HashMap<>();
}
