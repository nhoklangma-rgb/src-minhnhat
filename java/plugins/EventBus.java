package plugins;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Event bus for plugin event system
 */
public class EventBus {
    private static final Map<Class<?>, List<EventHandler>> handlers = new ConcurrentHashMap<>();

    /**
     * Register an object as event listener
     * 
     * @param listener Object with @EventListener methods
     */
    public static void register(Object listener) {
        for (Method method : listener.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(EventListener.class)) {
                Class<?>[] params = method.getParameterTypes();
                if (params.length == 1 && Event.class.isAssignableFrom(params[0])) {
                    Class<?> eventType = params[0];
                    EventListener annotation = method.getAnnotation(EventListener.class);

                    handlers.computeIfAbsent(eventType, k -> new ArrayList<>())
                            .add(new EventHandler(listener, method, annotation.priority()));

                    // Sort by priority
                    handlers.get(eventType).sort(Comparator.comparingInt(h -> h.priority));
                }
            }
        }
    }

    /**
     * Unregister an event listener
     * 
     * @param listener Object to unregister
     */
    public static void unregister(Object listener) {
        handlers.values().forEach(list -> list.removeIf(h -> h.listener == listener));
    }

    /**
     * Fire an event to all registered listeners
     * 
     * @param event Event to fire
     */
    public static void fire(Event event) {
        List<EventHandler> eventHandlers = handlers.get(event.getClass());
        if (eventHandlers != null) {
            for (EventHandler handler : eventHandlers) {
                try {
                    handler.method.setAccessible(true);
                    handler.method.invoke(handler.listener, event);

                    if (event.isCancelled()) {
                        break;
                    }
                } catch (Exception e) {
                    core.GameLogger.error("[EVENT] Error handling event: " + event.getClass().getSimpleName());
                    core.GameLogger.printStackTrace(e);
                }
            }
        }
    }

    private static class EventHandler {
        final Object listener;
        final Method method;
        final int priority;

        EventHandler(Object listener, Method method, int priority) {
            this.listener = listener;
            this.method = method;
            this.priority = priority;
        }
    }
}
