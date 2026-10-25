package com.extendedclip.deluxemenus.scheduler;

import com.extendedclip.deluxemenus.DeluxeMenus;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class DeluxeMenusScheduler {

    private static final boolean IS_FOLIA;

    static {
        boolean folia = false;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            folia = true;
        } catch (ClassNotFoundException ignored) {
        }
        IS_FOLIA = folia;
    }

    private final DeluxeMenus plugin;

    public DeluxeMenusScheduler(final @NotNull DeluxeMenus plugin) {
        this.plugin = plugin;
    }

    public boolean isFolia() {
        return IS_FOLIA;
    }

    public void cancelTasks() {
        if (IS_FOLIA) {
            try {
                Bukkit.getAsyncScheduler().cancelTasks(plugin);
            } catch (Throwable ignored) {
            }
            try {
                Bukkit.getGlobalRegionScheduler().cancelTasks(plugin);
            } catch (Throwable ignored) {
            }
        }
        Bukkit.getScheduler().cancelTasks(plugin);
    }

    public void runAsync(final @NotNull Runnable runnable) {
        if (IS_FOLIA) {
            Bukkit.getAsyncScheduler().runNow(plugin, task -> runnable.run());
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
    }

    public void runGlobal(final @NotNull Runnable runnable) {
        if (IS_FOLIA) {
            Bukkit.getGlobalRegionScheduler().run(plugin, task -> runnable.run());
            return;
        }
        Bukkit.getScheduler().runTask(plugin, runnable);
    }

    public void runAtEntity(final @Nullable Entity entity, final @NotNull Runnable runnable) {
        if (IS_FOLIA && entity != null) {
            entity.getScheduler().run(plugin, task -> runnable.run(), null);
            return;
        }
        Bukkit.getScheduler().runTask(plugin, runnable);
    }

    public void runAtEntityDelayed(final @Nullable Entity entity, final @NotNull Runnable runnable, final long delayTicks) {
        if (IS_FOLIA && entity != null) {
            if (delayTicks <= 0) {
                entity.getScheduler().run(plugin, task -> runnable.run(), null);
            } else {
                entity.getScheduler().runDelayed(plugin, task -> runnable.run(), null, delayTicks);
            }
            return;
        }

        if (delayTicks <= 0) {
            Bukkit.getScheduler().runTask(plugin, runnable);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, runnable, delayTicks);
        }
    }

    public @NotNull TaskHandle runAsyncTimer(final @NotNull Runnable runnable, final long delayTicks, final long periodTicks) {
        if (IS_FOLIA) {
            final long initialDelayMillis = Math.max(1, delayTicks) * 50L;
            final long periodMillis = Math.max(1, periodTicks) * 50L;
            final ScheduledTask task = Bukkit.getAsyncScheduler().runAtFixedRate(
                    plugin,
                    scheduledTask -> runnable.run(),
                    initialDelayMillis,
                    periodMillis,
                    TimeUnit.MILLISECONDS
            );
            return task::cancel;
        }

        final BukkitTask task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, runnable, delayTicks, periodTicks);
        return task::cancel;
    }

    public <T> T callSync(final @NotNull Callable<T> callable) throws Exception {
        if (IS_FOLIA) {
            final CompletableFuture<T> future = new CompletableFuture<>();
            Bukkit.getGlobalRegionScheduler().run(plugin, task -> {
                try {
                    future.complete(callable.call());
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
            return future.get();
        }

        if (Bukkit.isPrimaryThread()) {
            return callable.call();
        }
        return Bukkit.getScheduler().callSyncMethod(plugin, callable).get();
    }
}
