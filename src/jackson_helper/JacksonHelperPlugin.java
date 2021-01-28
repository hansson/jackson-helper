package jackson_helper;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class JacksonHelperPlugin extends AbstractUIPlugin {

	public static final String CUSTOM_TYPES_PREFERENCE = "json_types";

	// The plug-in ID
	public static final String PLUGIN_ID = "jackson-helper"; //$NON-NLS-1$

	// The shared instance
	private static JacksonHelperPlugin plugin;

	/**
	 * The constructor
	 */
	public JacksonHelperPlugin() {
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static JacksonHelperPlugin getDefault() {
		return plugin;
	}

	public List<String> getCustomTypesPreference() {
		return Arrays.asList(getPreferenceStore().getString(CUSTOM_TYPES_PREFERENCE).split("\n"));
	}

	public void setCustomTypesPreference(List<String> elements) {
		getPreferenceStore().setValue(CUSTOM_TYPES_PREFERENCE, elements.stream().collect(Collectors.joining("\n")));
	}

}
