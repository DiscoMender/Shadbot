package com.shadorc.shadbot;

import com.shadorc.shadbot.api.BotListStats;
import com.shadorc.shadbot.data.Telemetry;
import com.shadorc.shadbot.object.ExceptionHandler;
import com.shadorc.shadbot.utils.LogUtil;
import com.shadorc.shadbot.utils.ProcessUtil;
import com.shadorc.shadbot.utils.ShadbotUtil;
import discord4j.core.GatewayDiscordClient;
import discord4j.gateway.GatewayClient;
import discord4j.gateway.GatewayClientGroup;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.util.Logger;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TaskManager {

    private static final Scheduler DEFAULT_SCHEDULER = Schedulers.boundedElastic();
    private static final Logger LOGGER = LogUtil.getLogger(TaskManager.class);

    private final List<Disposable> tasks;

    public TaskManager() {
        this.tasks = new ArrayList<>(4);
    }

    public void schedulePresenceUpdates(GatewayDiscordClient gateway) {
        LOGGER.info("Scheduling presence updates");
        final Disposable task = Flux.interval(Duration.ofMinutes(15), Duration.ofMinutes(15), DEFAULT_SCHEDULER)
                .map(__ -> ShadbotUtil.getRandomStatus())
                .flatMap(gateway::updatePresence)
                .subscribe(null, ExceptionHandler::handleUnknownError);
        this.tasks.add(task);
    }

    /*public void scheduleLottery(GatewayDiscordClient gateway) {
        LOGGER.info("Starting lottery (next draw in {})", FormatUtil.formatDurationWords(LotteryCmd.getDelay()));
        final Disposable task = Flux.interval(LotteryCmd.getDelay(), Duration.ofDays(7), DEFAULT_SCHEDULER)
                .flatMap(__ -> LotteryCmd.draw(gateway)
                        .onErrorResume(err -> Mono.fromCallable(() -> ExceptionHandler.handleUnknownError(err))))
                .subscribe(null, ExceptionHandler::handleUnknownError);
        this.tasks.add(task);
    }*/

    public void schedulePeriodicStats(GatewayDiscordClient gateway) {
        LOGGER.info("Scheduling periodic stats log");

        final GatewayClientGroup group = gateway.getGatewayClientGroup();
        final Mono<Map<Integer, Long>> getResponseTimes = Flux.range(0, group.getShardCount())
                .flatMap(i -> Mono.justOrEmpty(group.find(i))
                        .map(GatewayClient::getResponseTime)
                        .map(Duration::toMillis)
                        .map(millis -> Tuples.of(i, millis)))
                .collectMap(Tuple2::getT1, Tuple2::getT2);

        final Disposable task = Flux.interval(Duration.ZERO, Duration.ofSeconds(15), DEFAULT_SCHEDULER)
                .then(gateway.getGuilds().count())
                .doOnNext(guildCount -> {
                    Telemetry.RAM_USAGE_GAUGE.set(ProcessUtil.getMemoryUsed());
                    Telemetry.CPU_USAGE_GAUGE.set(ProcessUtil.getCpuUsage());
                    Telemetry.THREAD_COUNT_GAUGE.set(Thread.activeCount());
                    Telemetry.GC_COUNT_GAUGE.set(ProcessUtil.getGCCount());
                    Telemetry.GC_TIME_GAUGE.set(ProcessUtil.getGCTime());
                    Telemetry.GUILD_COUNT_GAUGE.set(guildCount);
                })
                .flatMap(__ -> getResponseTimes)
                .doOnNext(responseTimeMap -> responseTimeMap
                        .forEach((key, value) -> Telemetry.RESPONSE_TIME_GAUGE.labels(key.toString()).set(value)))
                .subscribe(null, ExceptionHandler::handleUnknownError);

        this.tasks.add(task);
    }

    public void schedulePostStats(BotListStats botListStats) {
        LOGGER.info("Starting bot list stats scheduler");
        final Disposable task = Flux.interval(Duration.ofMinutes(15), Duration.ofHours(3), DEFAULT_SCHEDULER)
                .flatMap(__ -> botListStats.postStats())
                .subscribe(null, ExceptionHandler::handleUnknownError);
        this.tasks.add(task);
    }

    public void stop() {
        this.tasks.forEach(Disposable::dispose);
    }

}
