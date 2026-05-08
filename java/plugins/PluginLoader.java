package plugins;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Plugin loader for loading and managing plugins
 */
public class PluginLoader {
    private static final Map<String, Plugin> loadedPlugins = new LinkedHashMap<>();
    private static final String PLUGIN_DIR = "../plugins/";

    /**
     * Load all plugins from plugins directory
     */
    public static void loadAllPlugins() {
        File pluginFolder = new File(PLUGIN_DIR);
        if (!pluginFolder.exists()) {
            pluginFolder.mkdirs();
            core.GameLogger.info("[PLUGIN] Created plugins folder");
            return;
        }

        File[] jarFiles = pluginFolder.listFiles((dir, name) -> name.endsWith(".jar"));
        if (jarFiles == null || jarFiles.length == 0) {
            core.GameLogger.info("[PLUGIN] No plugins found");
            return;
        }

        List<Plugin> plugins = new ArrayList<>();

        // Load all plugins
        for (File jarFile : jarFiles) {
            try {
                Plugin plugin = loadPlugin(jarFile);
                if (plugin != null) {
                    plugins.add(plugin);
                }
            } catch (Exception e) {
                core.GameLogger.error("[PLUGIN] Failed to load: " + jarFile.getName());
                core.GameLogger.printStackTrace(e);
            }
        }

        // Sort by priority
        plugins.sort(Comparator.comparingInt(Plugin::getPriority));

        // Enable all plugins
        for (Plugin plugin : plugins) {
            try {
                plugin.onEnable();
                loadedPlugins.put(plugin.getName(), plugin);
                core.GameLogger.info("[PLUGIN] Enabled: " + plugin.getName() + " v" + plugin.getVersion());
            } catch (Exception e) {
                core.GameLogger.error("[PLUGIN] Failed to enable: " + plugin.getName());
                core.GameLogger.printStackTrace(e);
            }
        }
    }

    private static Plugin loadPlugin(File jarFile) throws Exception {
        URLClassLoader classLoader = new URLClassLoader(
                new URL[] { jarFile.toURI().toURL() },
                PluginLoader.class.getClassLoader());

        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();

                if (name.endsWith(".class")) {
                    String className = name.replace("/", ".").replace(".class", "");

                    try {
                        Class<?> clazz = classLoader.loadClass(className);

                        if (Plugin.class.isAssignableFrom(clazz) && !clazz.isInterface()) {
                            return (Plugin) clazz.getDeclaredConstructor().newInstance();
                        }
                    } catch (ClassNotFoundException ignored) {
                        // Not a plugin class
                    }
                }
            }
        }

        return null;
    }

    /**
     * Disable all plugins
     */
    public static void disableAllPlugins() {
        for (Plugin plugin : loadedPlugins.values()) {
            try {
                plugin.onDisable();
                core.GameLogger.info("[PLUGIN] Disabled: " + plugin.getName());
            } catch (Exception e) {
                core.GameLogger.error("[PLUGIN] Error disabling: " + plugin.getName());
                core.GameLogger.printStackTrace(e);
            }
        }
        loadedPlugins.clear();
    }

    /**
     * Reload all plugins
     */
    public static void reloadAll() {
        disableAllPlugins();
        loadAllPlugins();
    }

    /**
     * Get a loaded plugin by name
     * 
     * @param name Plugin name
     * @return Plugin instance or null
     */
    public static Plugin getPlugin(String name) {
        return loadedPlugins.get(name);
    }
}
