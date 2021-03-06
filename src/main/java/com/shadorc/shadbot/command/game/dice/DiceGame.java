package com.shadorc.shadbot.command.game.dice;

import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.game.MultiplayerGame;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.data.Telemetry;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.message.UpdatableMessage;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.FormatUtils;
import com.shadorc.shadbot.utils.ShadbotUtils;
import com.shadorc.shadbot.utils.TimeUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class DiceGame extends MultiplayerGame<DiceCmd, DicePlayer> {

    private final long bet;
    private final UpdatableMessage updatableMessage;

    private long startTime;
    private String results;

    public DiceGame(DiceCmd gameCmd, Context context, long bet) {
        super(gameCmd, context, Duration.ofSeconds(30));
        this.bet = bet;
        this.updatableMessage = new UpdatableMessage(context.getClient(), context.getChannelId());
    }

    @Override
    public Mono<Void> start() {
        return Mono.fromRunnable(() -> {
            this.schedule(this.end());
            this.startTime = System.currentTimeMillis();
            DiceInputs.create(this.getContext().getClient(), this).listen();
        });
    }

    @Override
    public Mono<Void> end() {
        final int winningNum = ThreadLocalRandom.current().nextInt(1, 7);
        return Flux.fromIterable(this.getPlayers().values())
                .flatMap(player -> Mono.zip(Mono.just(player), player.getUsername(this.getContext().getClient())))
                .flatMap(TupleUtils.function((player, username) -> {
                    if (player.getNumber() == winningNum) {
                        final long gains = Math.min((long) (this.bet * (this.getPlayers().size() + Constants.WIN_MULTIPLICATOR)),
                                Config.MAX_COINS);
                        Telemetry.DICE_SUMMARY.labels("win").observe(gains);
                        return player.win(gains)
                                .thenReturn(String.format("**%s** (Gains: **%s**)", username, FormatUtils.coins(gains)));
                    } else {
                        Telemetry.DICE_SUMMARY.labels("loss").observe(this.bet);
                        return Mono.just(String.format("**%s** (Losses: **%s**)", username, FormatUtils.coins(this.bet)));
                    }
                }))
                .collectList()
                .doOnNext(list -> this.results = String.join("\n", list))
                .then(this.getContext().getChannel())
                .flatMap(channel -> DiscordUtils.sendMessage(
                        String.format(Emoji.DICE + " The dice is rolling... **%s** !", winningNum), channel))
                .then(this.show())
                .then(Mono.fromRunnable(this::stop));
    }

    @Override
    public Mono<Void> show() {
        return Flux.fromIterable(this.getPlayers().values())
                .flatMap(player -> player.getUsername(this.getContext().getClient()))
                .collectList()
                .map(usernames -> ShadbotUtils.getDefaultEmbed()
                        .andThen(embed -> {
                            embed.setAuthor("Dice Game", null, this.getContext().getAvatarUrl())
                                    .setThumbnail("https://i.imgur.com/XgOilIW.png")
                                    .setDescription(String.format("**Use `%s%s <num>` to join the game.**%n**Bet:** %s",
                                            this.getContext().getPrefix(), this.getContext().getCommandName(),
                                            FormatUtils.coins(this.bet)))
                                    .addField("Player", String.join("\n", usernames), true)
                                    .addField("Number", this.getPlayers().values().stream()
                                            .map(DicePlayer::getNumber)
                                            .map(Object::toString)
                                            .collect(Collectors.joining("\n")), true);

                            if (this.results != null) {
                                embed.addField("Results", this.results, false);
                            }

                            if (this.isScheduled()) {
                                final Duration remainingDuration = this.getDuration()
                                        .minusMillis(TimeUtils.getMillisUntil(this.startTime));
                                embed.setFooter(
                                        String.format("You have %d seconds to make your bets. Use %scancel to force the stop.",
                                                remainingDuration.toSeconds(), this.getContext().getPrefix()), null);
                            } else {
                                embed.setFooter("Finished.", null);
                            }
                        }))
                .map(this.updatableMessage::setEmbed)
                .flatMap(UpdatableMessage::send)
                .then();
    }

    public long getBet() {
        return this.bet;
    }

}
