package com.shadorc.shadbot.object.help;

import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.utils.FormatUtil;
import com.shadorc.shadbot.utils.ShadbotUtil;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ImmutableEmbedFieldData;
import discord4j.discordjson.possible.Possible;
import reactor.util.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public abstract class HelpBuilder {

    protected final Context context;

    private final List<Argument> args;
    private final List<ImmutableEmbedFieldData> fields;

    @Nullable
    private String authorName;
    @Nullable
    private String authorUrl;
    @Nullable
    private String thumbnail;
    @Nullable
    private String description;
    @Nullable
    private String usage;
    @Nullable
    private String example;
    @Nullable
    private String source;
    @Nullable
    private String footer;

    protected HelpBuilder(Context context) {
        this.context = context;
        this.args = new ArrayList<>();
        this.fields = new ArrayList<>();
    }

    public HelpBuilder setAuthor(String authorName, String authorUrl) {
        this.authorName = authorName;
        this.authorUrl = authorUrl;
        return this;
    }

    public HelpBuilder setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
        return this;
    }

    public HelpBuilder setDescription(String description) {
        this.description = String.format("**%s**", description);
        return this;
    }

    public HelpBuilder setFullUsage(String usage) {
        this.usage = usage;
        return this;
    }

    public HelpBuilder setExample(String example) {
        this.example = example;
        return this;
    }

    public HelpBuilder setSource(String source) {
        this.source = source;
        return this;
    }

    public HelpBuilder setFooter(String footer) {
        this.footer = footer;
        return this;
    }

    public HelpBuilder addArg(String name, String desc, boolean isOptional) {
        this.args.add(new Argument(name, desc, isOptional));
        return this;
    }

    public HelpBuilder addArg(String name, boolean isOptional) {
        return this.addArg(name, null, isOptional);
    }

    public HelpBuilder addField(String name, String value, boolean inline) {
        this.fields.add(ImmutableEmbedFieldData.of(name, value, Possible.of(inline)));
        return this;
    }

    public Consumer<EmbedCreateSpec> build() {
        return ShadbotUtil.getDefaultEmbed(embed -> {
            if (this.authorName != null && !this.authorName.isBlank()) {
                embed.setAuthor(this.authorName, this.authorUrl, this.context.getAuthorAvatarUrl());
            }
            embed.addField("Usage", this.getUsage(), false);

            if (this.description != null && !this.description.isBlank()) {
                embed.setDescription(this.description);
            }

            if (this.thumbnail != null && !this.thumbnail.isBlank()) {
                embed.setThumbnail(this.thumbnail);
            }

            if (!this.getArguments().isEmpty()) {
                embed.addField("Arguments", this.getArguments(), false);
            }

            if (this.example != null && !this.example.isBlank()) {
                embed.addField("Example", this.example, false);
            }

            if (this.source != null && !this.source.isBlank()) {
                embed.addField("Source", this.source, false);
            }

            for (final ImmutableEmbedFieldData field : this.fields) {
                embed.addField(field.name(), field.value(), field.inline().get());
            }

            if (this.footer != null && !this.footer.isBlank()) {
                embed.setFooter(this.footer, null);
            }
        });
    }

    protected abstract String getCommandName();

    private String getUsage() {
        if (this.usage != null && !this.usage.isBlank()) {
            return String.format("`%s`", this.usage);
        }

        if (this.args.isEmpty()) {
            return String.format("`/%s`", this.getCommandName());
        }

        return String.format("`/%s %s`", this.getCommandName(),
                FormatUtil.format(this.args,
                        arg -> String.format(arg.isOptional() ? "[<%s>]" : "<%s>", arg.getName()), " "));
    }

    private String getArguments() {
        return this.args.stream()
                .filter(arg -> arg.getDescription() != null && !arg.getDescription().isBlank())
                .map(arg -> String.format("%n**%s** %s - %s", arg.getName(), arg.isOptional() ? "[optional] " : "", arg.getDescription()))
                .collect(Collectors.joining());
    }
}
