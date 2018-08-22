/*
 * Config
 *
 * $Id: Config.java 7868 2010-10-21 23:05:34Z jmfee $
 * $URL: https://ehptools.cr.usgs.gov/svn/ProductDistribution/trunk/src/gov/usgs/earthquake/distribution/Config.java $
 */
package gov.usgs.util;

import java.util.Properties;
import java.util.Map;
import java.util.HashMap;

import gov.usgs.util.Ini;

import java.lang.ref.WeakReference;

/**
 * The configuration object used for distribution.
 *
 * This object holds a singleton reference to the global configuration. Users
 * should use the static method getConfig() to retrieve the loaded
 * configuration.
 *
 * Objects are loaded from Config objects using the property "type". The value
 * of the property "type" should be a fully qualified class name. This can be
 * used for Any type of object that has a constructor with no arguments. If the
 * loaded object implements the Configurable interface, its "configure" method
 * is called using the Config object.
 *
 * <pre>
 * [objectName]
 * type = fully.qualified.Classname
 * ... other object properties
 *
 * [anotherObject]
 * ...
 * </pre>
 *
 * Some objects refer to other named configured objects. These named objects
 * correspond to sections in the global configuration object. These objects are
 * loaded using the getObject() method:
 *
 * <pre>
 * Classname obj = (Classname) Config.getConfig().getObject(&quot;objectName&quot;);
 * </pre>
 *
 * or
 *
 * <pre>
 * Classname obj = (Classname) new Config(Config.getConfig().getSection(
 * 		&quot;objectName&quot;)).getObject();
 * </pre>
 *
 */
public class Config extends Ini {

	/** Serialization version number. */
	private static final long serialVersionUID = 1L;

	/** Property name that specifies the object type. */
	public static final String OBJECT_TYPE_PROPERTY = "type";

	/** Map from short configuration types to fully qualified class names. */
	public static final Map<String, String> OBJECT_TYPE_MAP = new HashMap<String, String>();

	/** Singleton configuration object. */
	private static Config SINGLETON = null;

	/**
	 * Weak references to already loaded objects. WeakReferences are cleared
	 * more aggressively than SoftReferences, the idea is finalize as soon as no
	 * one else holds a reference.
	 */
	private Map<String, WeakReference<Object>> loadedObjects = new HashMap<String, WeakReference<Object>>();

	/**
	 * Create an empty Config object.
	 */
	public Config() {
		super();
	}

	/**
	 * Create a Config object using properties for defaults.
	 *
	 * @param properties
	 *            the default properties.
	 */
	public Config(Properties properties) {
		super(properties);
	}

	/**
	 * Load a configured object.
	 *
	 * Uses the property "type" to determine which type of object to load. If
	 * the loaded object implements the Configurable interface, its configure
	 * method is called before returning.
	 *
	 * @return the loaded object
	 * @throws Exception
	 *             if any errors occur.
	 */
	public Object getObject() throws Exception {
		String className = getProperty(OBJECT_TYPE_PROPERTY);
		// must specify type
		if (className == null) {
			return null;
		}

		// config defines shortnames that can be used for common classes
		if (OBJECT_TYPE_MAP.containsKey(className)) {
			className = OBJECT_TYPE_MAP.get(className);
		}

		Object obj = Class.forName(className)
				.getConstructor().newInstance();
		if (obj instanceof Configurable) {
			((Configurable) obj).configure(this);
		}
		return obj;
	}

	/**
	 * Get a section as a Config object.
	 *
	 * @param name
	 *            name of config section.
	 * @return section properties as a Config object.
	 * @throws Exception
	 *             if any errors occur.
	 */
	public Object getObject(final String name) throws Exception {
		Object object = null;

		// check if already loaded
		WeakReference<Object> ref = loadedObjects.get(name);
		if (ref != null) {
			// already loaded, if not still in memory this returns null
			object = ref.get();
		}

		if (object == null) {
			// not loaded yet
			Properties props = getSections().get(name);
			if (props != null) {
				object = new Config(props).getObject();
				if (object instanceof Configurable) {
					((Configurable)object).setName(name);
				}
				loadedObjects.put(name, new WeakReference<Object>(object));
			}
		}

		return object;
	}

	/**
	 * Get the global configuration object.
	 *
	 * Users are encouraged to not maintain a reference to the returned object,
	 * in case it is updated.
	 *
	 * @return the global configuration object.
	 */
	public static Config getConfig() {
		return SINGLETON;
	}

	/**
	 * Set the global configuration object.
	 *
	 * @param config
	 *            the new global configuration object.
	 */
	public static void setConfig(final Config config) {
		SINGLETON = config;
	}

}
