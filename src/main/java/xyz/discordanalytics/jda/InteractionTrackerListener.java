package xyz.discordanalytics.jda;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.Interaction;
import net.dv8tion.jda.api.interactions.InteractionType;
import net.dv8tion.jda.api.interactions.commands.CommandAutoCompleteInteraction;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.components.ComponentInteraction;
import net.dv8tion.jda.api.interactions.modals.ModalInteraction;
import org.jetbrains.annotations.NotNull;
import xyz.discordanalytics.AnalyticsBase;
import xyz.discordanalytics.JDAAnalytics;
import xyz.discordanalytics.utilities.InteractionItem;
import xyz.discordanalytics.utilities.LocalesItems;

import java.util.*;

public class InteractionTrackerListener extends ListenerAdapter {
    private final JDA client;
    private final JDAAnalytics analytics;

    public InteractionTrackerListener(JDAAnalytics analytics) {
        this.client = analytics.getClient();
        this.analytics = analytics;
    }

    @Override
    public void onGenericInteractionCreate(@NotNull GenericInteractionCreateEvent event) {
        try {
            Number guildCount = analytics.getEventsToTrack().trackGuilds ? client.getGuilds().size() : null;
            Number userCount = analytics.getEventsToTrack().trackUserCount ? client.getUsers().size() : null;
            ArrayList<String> guildsLocales = (ArrayList<String>) analytics.getData().get("guildsLocales");
            ArrayList<String> locales = (ArrayList<String>) analytics.getData().get("locales");
            ArrayList<String> interactions = (ArrayList<String>) analytics.getData().get("interactions");

            String[] date = new Date().toString().split(" ");
            String dateString = date[5] + "-" + AnalyticsBase.monthToNumber(date[1]) + "-" + date[2];

            DiscordLocale guildLocale = event.getGuild() != null ? event.getGuildLocale() : null;
            String guildLocaleString = guildLocale != null ? guildLocale.getLocale() : null;
            if (guildLocaleString != null) {
                boolean isGLTracked = false;
                for (int i = 0; i < guildsLocales.size(); i++) {
                    LocalesItems item = analytics.parseStringToLocalesItems(guildsLocales.get(i));
                    if (item.locale.equals(guildLocaleString)) {
                        item.number++;
                        guildsLocales.set(i, item.toString());
                        isGLTracked = true;
                        break;
                    }
                }
                if (!isGLTracked) guildsLocales.add(new LocalesItems(guildLocaleString, 1).toString());
            }

            DiscordLocale userLocale = event.getUserLocale();
            String userLocaleString = userLocale.getLocale();
            boolean isULTracked = false;
            for (int i = 0; i < locales.size(); i++) {
                LocalesItems item = analytics.parseStringToLocalesItems(locales.get(i));
                if (item.locale.equals(userLocaleString)) {
                    item.number++;
                    locales.set(i, item.toString());
                    isULTracked = true;
                    break;
                }
            }
            if (!isULTracked) locales.add(new LocalesItems(userLocaleString, 1).toString());

            Interaction interaction = event.getInteraction();
            int interactionType = interaction.getType().getKey();
            String interactionName =
                    interactionType == InteractionType.COMMAND.getKey()
                            ? ((SlashCommandInteraction) interaction).getName()
                                : interactionType == InteractionType.COMPONENT.getKey()
                                ? ((ComponentInteraction) interaction).getComponentId()
                                    : interactionType == InteractionType.COMMAND_AUTOCOMPLETE.getKey()
                                    ? ((CommandAutoCompleteInteraction) interaction).getName()
                                        : interactionType == InteractionType.MODAL_SUBMIT.getKey()
                                        ? ((ModalInteraction) interaction).getModalId()
                                            : null;

            if (interactionName == null) return;

            boolean isITracked = false;
            for (int i = 0; i < interactions.size(); i++) {
                InteractionItem item = analytics.parseStringToInteractionItem(interactions.get(i));
                if (item.name.equals(interactionName)) {
                    item.number++;
                    interactions.set(i, item.toString());
                    isITracked = true;
                    break;
                }
            }
            if (!isITracked) interactions.add(new InteractionItem(interactionName, interactionType, 1).toString());

            analytics.setData(new HashMap<>() {{
                put("date", dateString);
                put("guilds", guildCount);
                put("users", userCount);
                put("interactions", interactions);
                put("locales", locales);
                put("guildsLocales", guildsLocales);
            }});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
