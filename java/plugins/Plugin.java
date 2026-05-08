package plugins;

/**
 * Base interface for all plugins
 */
public interface Plugin {
    /**
     * Get plugin name
     * 
     * @return Plugin name
     */
    String getName();

    /**
     * Get plugin version
     * 
     * @return Version string (e.g., "1.0.0")
     */
    String getVersion();

    /**
     * Called when plugin is enabled
     */
    void onEnable();

    /**
     * Called when plugin is disabled
     */
    void onDisable();

    /**
     * Get plugin priority (lower = load first)
     * 
     * @return Priority value (default: 100)
     */
    default int getPriority() {
        return 100;
    }
}
