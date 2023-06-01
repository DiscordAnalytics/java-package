package xyz.discordanalytics.jda;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import org.jetbrains.annotations.NotNull;
import xyz.discordanalytics.AnalyticsBase;
import xyz.discordanalytics.JDAAnalytics;
import xyz.discordanalytics.utilities.ApiEndpoints;
import xyz.discordanalytics.utilities.ErrorCodes;
import xyz.discordanalytics.utilities.EventsTracker;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.Date;
import java.util.HashMap;

public class InteractionTrackerListener extends ListenerAdapter {
    private final JDA client;
    private Date precedentPostDate;
    private final JDAAnalytics analytics;

    public InteractionTrackerListener(JDAAnalytics analytics) {
        this.client = analytics.getClient();
        this.precedentPostDate = new Date();
        this.analytics = analytics;
    }

    private boolean isOnCooldown() {
        return new Date().getTime() - precedentPostDate.getTime() < 600000;
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (isOnCooldown()) return;

        SlashCommandInteraction interaction = event.getInteraction();
        String[] date = new Date().toString().split(" ");

        try {
            post(interaction.getType().toString(),
                    interaction.getName(),
                    interaction.getUserLocale().getLocale(),
                    client.getUsers().size(),
                    client.getGuilds().size(),
                    date[5] + "-" + AnalyticsBase.monthToNumber(date[1]) + "-" + date[2]
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
        precedentPostDate = new Date();
    }

    private void post(String type, String name, String userLocale, int userCount, int guildCount, String date) throws IOException {
        try {
            EventsTracker eventsToTrack = analytics.getEventsToTrack();

            HttpResponse<String> response = analytics.post(ApiEndpoints.BOT_STATS, new HashMap<>() {{
                put("type", type);
                put("name", name);
                put("userLocale", eventsToTrack.trackUserLanguage ? userLocale : null);
                put("userCount", eventsToTrack.trackUserCount ? userCount : null);
                put("guildCount", eventsToTrack.trackGuilds ? guildCount : null);
                put("date", date);
            }}.toString());
            HashMap<Object, Object> notSentInteraction = (HashMap<Object, Object>) analytics.getDataToSend().get("interactions");
            if (response.statusCode() != 200 && notSentInteraction.size() == 0) {
                new IOException(ErrorCodes.DATA_NOT_SENT).printStackTrace();
                notSentInteraction.put("type", type);
                notSentInteraction.put("name", name);
                notSentInteraction.put("userLocale", userLocale);
                notSentInteraction.put("userCount", userCount);
                notSentInteraction.put("guildCount", guildCount);
                notSentInteraction.put("date", date);
                analytics.putToDataToSend("interactions", notSentInteraction);
            }

            if (response.statusCode() == 200 && notSentInteraction.size() > 0) analytics.sendDataToSend();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
