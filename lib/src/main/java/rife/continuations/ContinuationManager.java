/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.continuations;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.*;

/**
 * Manages a collection of {@code ContinuationContext} instances.
 * <p>A {@code ContinuationManager} instance is typically associated with
 * a specific context, like for example a {@link rife.engine.Site}
 * for RIFE2's web engine. It's up to you to provide an API to your users if
 * you want them to be able to interact with the appropriate continuations
 * manager.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @see ContinuationManager
 * @since 1.0
 */
public class ContinuationManager {
    private final Map<String, ContinuationContext> contexts_;
    private final ContinuationConfigRuntime config_;

    private final ReadWriteLock lock_ = new ReentrantReadWriteLock();
    final Lock readLock_ = lock_.readLock();
    final Lock writeLock_ = lock_.writeLock();

    /**
     * Instantiates a new continuation manager and uses the default values for
     * the continuations duration and purging.
     *
     * @param config the runtime configuration that will be used be this
     *               manager
     * @since 1.0
     */
    public ContinuationManager(ContinuationConfigRuntime config) {
        config_ = config;
        contexts_ = new WeakHashMap<>();
    }

    /**
     * Retrieves the runtime configuration that was provided to the manager
     * at instantiation.
     *
     * @return this manager's runtime configuration
     * @since 1.0
     */
    public ContinuationConfigRuntime getConfigRuntime() {
        return config_;
    }

    /**
     * Checks if a particular continuation context is expired.
     *
     * @param context the context that needs to be verified
     * @return {@code true} if the continuation context is expired; and
     * <p>{@code false} otherwise
     * @since 1.0
     */
    public boolean isExpired(ContinuationContext context) {
        return context.getStart() <= System.currentTimeMillis() - config_.getContinuationDuration();
    }

    /**
     * Adds a particular {@code ContinuationContext} to this manager.
     *
     * @param context the context that will be added
     * @since 1.0
     */
    public void addContext(ContinuationContext context) {
        if (null == context) {
            return;
        }

        writeLock_.lock();
        try {
            contexts_.put(context.getId(), context);
        } finally {
            writeLock_.unlock();
        }
    }

    /**
     * Removes a {@link ContinuationContext} instance from this continuation
     * manager.
     *
     * @param id the unique string that identifies the
     *           {@code ContinuationContext} instance that will be removed
     * @see #getContext
     * @since 1.0
     */
    public void removeContext(String id) {
        if (null == id) {
            return;
        }

        writeLock_.lock();
        try {
            contexts_.remove(id);
        } finally {
            writeLock_.unlock();
        }
    }

    /**
     * Creates a new {@code ContinuationContext} from an existing one so that
     * the execution can be resumed.
     * <p>If the existing continuation context couldn't be found, no new one
     * is created. However, if it could be found, the result of
     * {@link ContinuationConfigRuntime#cloneContinuations} will determine
     * whether the existing continuation context will be cloned to create
     * the new one, or if its state will be reused.
     * <p>The new continuation context will have its own unique ID.
     *
     * @param id the ID of the existing continuation context
     * @return the new {@code ContinuationContext}; or
     * <p>{@code null} if the existing continuation context couldn't be found
     * @throws CloneNotSupportedException when the continuable couldn't be cloned
     * @since 1.0
     */
    public ContinuationContext resumeContext(String id)
    throws CloneNotSupportedException {
        ContinuationContext result = null;

        purgeContinuations();

        writeLock_.lock();
        try {
            var context = getContext(id);
            if (context != null &&
                context.isPaused()) {
                Object continuable = context.getContinuable();
                if (continuable instanceof CloneableContinuable &&
                    config_.cloneContinuations(continuable)) {
                    result = cloneContext(context);
                } else {
                    result = reuseContext(context);
                }
            }
        } finally {
            writeLock_.unlock();
        }

        return result;
    }

    /**
     * Retrieves a {@link ContinuationContext} instance from this continuation
     * manager.
     *
     * @param id the unique string that identifies the
     *           {@code ContinuationContext} instance that has to be retrieved
     * @return the {@code ContinuationContext} instance that corresponds
     * to the provided identifier; or
     * <p>{@code null} if the identifier isn't known by the continuation
     * manager.
     * @see #removeContext
     * @since 1.0
     */
    public ContinuationContext getContext(String id) {
        ContinuationContext context;
        readLock_.lock();
        try {
            context = contexts_.get(id);
        } finally {
            readLock_.unlock();
        }

        if (context != null) {
            if (isExpired(context)) {
                context = null;
                removeContext(id);
            }
        }
        return context;
    }

    private ContinuationContext reuseContext(ContinuationContext context) {
        contexts_.remove(context.getId());
        context.resetId();
        addContext(context);

        return context;
    }

    private ContinuationContext cloneContext(ContinuationContext context)
    throws CloneNotSupportedException {
        var new_context = context.clone();
        new_context.resetId();
        addContext(new_context);

        return new_context;
    }

    private void purgeContinuations() {
        var purge_decision = ThreadLocalRandom.current().nextInt(config_.getContinuationPurgeScale());
        if (purge_decision <= config_.getContinuationPurgeFrequency()) {
            new PurgeContinuations().start();
        }
    }

    private class PurgeContinuations extends Thread {
        public void run() {
            purge();
        }

        private void purge() {
            try {
                var stale_continuations = new ArrayList<String>();
                for (var context : contexts_.values()) {
                    if (context != null &&
                        isExpired(context)) {
                        stale_continuations.add(context.getId());
                    }
                }

                if (!stale_continuations.isEmpty()) {
                    writeLock_.lock();
                    try {
                        for (var id : stale_continuations) {
                            contexts_.remove(id);
                        }
                    } finally {
                        writeLock_.unlock();
                    }
                }

            } catch (ConcurrentModificationException e) {
                // Oops, something changed while we were looking.
                // Lock the context and try again.
                // Set our priority high while we have the sessions locked
                var old_priority = Thread.currentThread().getPriority();
                Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
                try {
                    writeLock_.lock();
                    try {
                        contexts_.values().removeIf(context -> context != null && isExpired(context));
                    } finally {
                        writeLock_.unlock();
                    }
                } finally {
                    Thread.currentThread().setPriority(old_priority);
                }
            }
        }
    }
}
