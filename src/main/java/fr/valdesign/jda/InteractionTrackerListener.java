package fr.valdesign.jda;

import fr.valdesign.JDAAnalytics;
import fr.valdesign.utilities.ApiEndpoints;
import fr.valdesign.utilities.ErrorCodes;
import fr.valdesign.utilities.EventsTracker;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;

public class InteractionTrackerListener extends ListenerAdapter {
    private final JDA client;
    private final String baseAPIUrl;
    private final String apiKey;
    private Date precedentPostDate;
    private final JDAAnalytics analytics;

    public InteractionTrackerListener(JDA client, String baseAPIUrl, String apiKey, JDAAnalytics analytics) {
        this.client = client;
        this.baseAPIUrl = baseAPIUrl;
        this.apiKey = apiKey;
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
            post(baseAPIUrl, apiKey,
                    interaction.getType().toString(),
                    interaction.getName(),
                    interaction.getUserLocale().getLocale(),
                    client.getUsers().size(),
                    client.getGuilds().size(),
                    date[5] + "-" + monthToNumber(date[1]) + "-" + date[2]
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
        precedentPostDate = new Date();
    }

    private void post(String url, String apiKey, String type, String name, String userLocale, int userCount, int guildCount, String date) throws IOException {
        EventsTracker eventsToTrack = analytics.getEventsToTrack();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(java.net.URI.create(url + ApiEndpoints.ROUTES.INTERACTIONS))
                .header("Content-Type", "application/json")
                .header("Authorization", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(new HashMap<>() {{
                    put("type", type);
                    put("name", name);
                    put("userLocale", eventsToTrack.trackUserLanguage ? userLocale : null);
                    put("userCount", eventsToTrack.trackUserCount ? userCount : null);
                    put("guildCount", eventsToTrack.trackGuilds ? guildCount : null);
                    put("date", date);
                }}.toString()))
                .build();

        HttpResponse<String> response = null;
        try {
            response = analytics.getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (response == null) {
            new IOException(ErrorCodes.DATA_NOT_SENT).printStackTrace();
            return;
        }
        if (response.statusCode() == 401) new IOException(ErrorCodes.INVALID_API_TOKEN).printStackTrace();
        if (response.statusCode() == 429) new IOException(ErrorCodes.ON_COOLDOWN).printStackTrace();
        if (response.statusCode() == 451) new IOException(ErrorCodes.SUSPENDED_BOT).printStackTrace();
        HashMap<Object, Object> notSentInteraction = (HashMap<Object, Object>) analytics.getDataNotSent().get("interactions");
        if (response.statusCode() != 200 && notSentInteraction.size() == 0) {
            new IOException(ErrorCodes.DATA_NOT_SENT).printStackTrace();
            notSentInteraction.put("type", type);
            notSentInteraction.put("name", name);
            notSentInteraction.put("userLocale", userLocale);
            notSentInteraction.put("userCount", userCount);
            notSentInteraction.put("guildCount", guildCount);
            notSentInteraction.put("date", date);
            analytics.putToDataNotSent("interactions", notSentInteraction);
        }

        if (response.statusCode() == 200 && notSentInteraction.size() > 0) sendDataNotSent();
    }

    private void sendDataNotSent() {
        HashMap<Object, Object> notSentInteraction = (HashMap<Object, Object>) analytics.getDataNotSent().get("interactions");

        if (notSentInteraction.size() > 0) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(java.net.URI.create(baseAPIUrl + ApiEndpoints.ROUTES.INTERACTIONS))
                        .header("Content-Type", "application/json")
                        .header("Authorization", apiKey)
                        .POST(HttpRequest.BodyPublishers.ofString(new HashMap<>() {{
                            put("type", notSentInteraction.get("type"));
                            put("name", notSentInteraction.get("name"));
                            put("userLocale", notSentInteraction.get("userLocale"));
                            put("userCount", notSentInteraction.get("userCount"));
                            put("guildCount", notSentInteraction.get("guildCount"));
                            put("date", notSentInteraction.get("date"));
                        }}.toString()))
                        .build();

                HttpResponse<String> response = null;
                try {
                    response = analytics.getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (response == null) {
                    new IOException(ErrorCodes.DATA_NOT_SENT).printStackTrace();
                    return;
                }
                if (response.statusCode() == 401) new IOException(ErrorCodes.INVALID_API_TOKEN).printStackTrace();
                if (response.statusCode() == 429) new IOException(ErrorCodes.ON_COOLDOWN).printStackTrace();
                if (response.statusCode() == 451) new IOException(ErrorCodes.SUSPENDED_BOT).printStackTrace();
                if (response.statusCode() == 200) analytics.putToDataNotSent("interactions", new HashMap<>());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static String monthToNumber(String month) {
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        int monthIndex = Arrays.asList(months).indexOf(month);
        return (monthIndex < 9 ? "0" : "") + (monthIndex + 1);
    }
}
