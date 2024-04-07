package com.vexsoftware.votifier.velocity;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.api.scheduler.Scheduler;
import com.vexsoftware.votifier.platform.scheduler.ScheduledVotifierTask;
import com.vexsoftware.votifier.platform.scheduler.VotifierScheduler;

import java.util.concurrent.TimeUnit;

class VelocityScheduler implements VotifierScheduler {
    private final ProxyServer server;
    private final VotifierPlugin plugin;

    public VelocityScheduler(ProxyServer server, VotifierPlugin plugin) {
        this.server = server;
        this.plugin = plugin;
    }

    private Scheduler.TaskBuilder builder(Runnable runnable) {
        return server.getScheduler().buildTask(plugin, runnable);
    }

    @Override
    public ScheduledVotifierTask delayedOnPool(Runnable runnable, long delay, TimeUnit unit) {
        return new TaskWrapper(builder(runnable).delay(delay, unit).schedule());
    }

    @Override
    public ScheduledVotifierTask repeatOnPool(Runnable runnable, long delay, long repeat, TimeUnit unit) {
        return new TaskWrapper(builder(runnable).delay(delay, unit).repeat(repeat, unit).schedule());
    }

    private static class TaskWrapper implements ScheduledVotifierTask {
        private final ScheduledTask task;

        private TaskWrapper(ScheduledTask task) {
            this.task = task;
        }

        @Override
        public void cancel() {
            task.cancel();
        }
    }
}
