/*
package com.shadorc.shadbot.command.owner;

import com.shadorc.shadbot.Shadbot;
import com.shadorc.shadbot.object.Inputs;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

public class ConfirmInputs extends Inputs {

    private final Mono<Void> task;
    private final AtomicBoolean isCancelled;

    private ConfirmInputs(GatewayDiscordClient gateway, Duration timeout, Snowflake channelId, Mono<Void> task) {
        super(gateway, timeout, channelId);
        this.task = task;
        this.isCancelled = new AtomicBoolean(false);
    }

    public static ConfirmInputs create(GatewayDiscordClient gateway, Duration timeout, Snowflake channelId, Mono<Void> task) {
        return new ConfirmInputs(gateway, timeout, channelId, task);
    }

    @Override
    public Mono<Boolean> isValidEvent(MessageCreateEvent event) {
        return Mono.justOrEmpty(event.getMessage().getAuthor())
                .map(User::getId)
                .filter(Shadbot.getOwnerId()::equals)
                .map(__ -> event.getMessage().getContent())
                .map(content -> {
                    if ("n".equalsIgnoreCase(content) || "no".equalsIgnoreCase(content)) {
                        this.isCancelled.set(true);
                    }
                    return "y".equalsIgnoreCase(content) || "yes".equalsIgnoreCase(content);
                });
    }

    @Override
    public boolean takeEventWile(MessageCreateEvent event) {
        return !this.isCancelled.get();
    }

    @Override
    public Mono<Void> processEvent(MessageCreateEvent event) {
        return this.task;
    }
}
*/
