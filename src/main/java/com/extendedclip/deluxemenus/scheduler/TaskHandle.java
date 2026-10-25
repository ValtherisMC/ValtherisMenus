package com.extendedclip.deluxemenus.scheduler;

@FunctionalInterface
public interface TaskHandle {

    /**
     * Cancels the scheduled task.
     */
    void cancel();
}
