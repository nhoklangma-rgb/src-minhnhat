package plugins;

/**
 * Base class for all events
 */
public class Event {
    private boolean cancelled = false;

    /**
     * Check if event is cancelled
     * 
     * @return true if cancelled
     */
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Set event cancelled status
     * 
     * @param cancelled true to cancel event
     */
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
