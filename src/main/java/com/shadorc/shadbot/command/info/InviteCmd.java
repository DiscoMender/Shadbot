package com.shadorc.shadbot.command.info;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.ShadbotUtil;
import reactor.core.publisher.Mono;

public class InviteCmd extends BaseCmd {

    public InviteCmd() {
        super(CommandCategory.INFO, "invite", "Explain how to invite the bot in a server");
    }

    @Override
    public Mono<?> execute(Context context) {
        return context.createFollowupMessage(ShadbotUtil.getDefaultEmbed(
                embed -> embed.setAuthor("Links", Config.INVITE_URL, context.getAuthorAvatarUrl())
                        .setDescription("I'm glad you're willing to invite **Shadbot** in your own server, thank you!" +
                                "\nHere are some useful links for you." +
                                "\nIf you have any questions or issues, **do not hesitate to join the Support Server and ask!**" +
                                "\nIf you want to help keep running the bot, you can also follow the **Donation** link to get more " +
                                "information. Even small donations are really helpful. " + Emoji.HEARTS)
                        .addField("Invite", Config.INVITE_URL, false)
                        .addField("Support Server", Config.SUPPORT_SERVER_URL, false)
                        .addField("Donation", Config.PATREON_URL, false)));
    }

}
