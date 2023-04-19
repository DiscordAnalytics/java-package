package xyz.discordanalytics;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.Interaction;
import xyz.discordanalytics.utilities.ApiEndpoints;
import xyz.discordanalytics.utilities.ErrorCodes;
import xyz.discordanalytics.utilities.EventsTracker;
import xyz.discordanalytics.utilities.LibType;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.Date;
import java.util.HashMap;

public class JavacordAnalytics extends AnalyticsBase {
    private final DiscordApi client;

    public JavacordAnalytics(DiscordApi api, EventsTracker eventsToTrack, String apiKey) {
        super(eventsToTrack, apiKey);
        this.client = api;
        this.baseAPIUrl = ApiEndpoints.BASE_URL + ApiEndpoints.TRACK_URL.replace("[id]", client.getYourself().getIdAsString());
    }

    private boolean isInvalidClient() {
        if (client == null) {
            new IOException(ErrorCodes.INVALID_CLIENT_TYPE).printStackTrace();
            return true;
        }
        User userClient = client.getYourself();
        if (userClient == null) {
            new IOException(ErrorCodes.CLIENT_NOT_READY).printStackTrace();
            return true;
        }
        return false;
    }

    public void trackEvents() throws IOException, InterruptedException {
        User userClient = client.getYourself();
        if (isInvalidClient()) return;
        assert userClient != null;

        if (isConfigInvalid(userClient.getIdAsString(), LibType.JAVACORD)) {
            new IOException(ErrorCodes.INVALID_CONFIGURATION).printStackTrace();
            return;
        }

        if (eventsToTrack.trackInteractions) {
            client.addInteractionCreateListener(listener -> {
                if (isOnCooldown()) return;

                try {
                    Interaction interaction = listener.getInteraction();
                    String[] date = new Date().toString().split(" ");

                    HttpResponse<String> response = post(ApiEndpoints.ROUTES.INTERACTIONS, new HashMap<>() {{
                        put("type", interaction.getType().toString());
                        put("name", interaction.getIdAsString());
                        put("userLocale", null); // because it's not possible to get the user locale
                        put("userCount", eventsToTrack.trackUserCount ? client.getCachedUsers().size() : null);
                        put("guildCount", eventsToTrack.trackGuilds ? client.getServers().size() : null);
                        put("date", date[5] + "-" + monthToNumber(date[1]) + "-" + date[2]);
                    }});

                    HashMap<Object, Object> notSentInteraction = (HashMap<Object, Object>) getDataNotSent().get("interactions");
                    if (response.statusCode() != 200) {
                        notSentInteraction.put("type", interaction.getType().getValue());
                        notSentInteraction.put("name", interaction.getIdAsString());
                        notSentInteraction.put("userLocale", null); // because it's not possible to get the user locale
                        notSentInteraction.put("userCount", eventsToTrack.trackUserCount ? client.getCachedUsers().size() : null);
                        notSentInteraction.put("guildCount", eventsToTrack.trackGuilds ? client.getServers().size() : null);
                        notSentInteraction.put("date", date[5] + "-" + monthToNumber(date[1]) + "-" + date[2]);

                        putToDataNotSent("interactions", notSentInteraction);
                    }

                    if (response.statusCode() == 200 && notSentInteraction.size() > 0) sendDataNotSent();
                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        if (eventsToTrack.trackGuilds) {
            client.addServerJoinListener(listener -> trackGuilds());
            client.addServerLeaveListener(listener -> trackGuilds());
        }

        precedentPostDate = new Date();
    }

    private void trackGuilds() {
        if (isOnCooldown()) {
            new IOException(ErrorCodes.ON_COOLDOWN).printStackTrace();
            return;
        }

        try {
            String[] date = new Date().toString().split(" ");

            HttpResponse<String> response = post(ApiEndpoints.ROUTES.GUILDS, new HashMap<>() {{
                put("date", date[5] + "-" + monthToNumber(date[1]) + "-" + date[2]);
                put("guildCount", eventsToTrack.trackGuilds ? client.getServers().size() : null);
                put("userCount", eventsToTrack.trackUserCount ? client.getCachedUsers().size() : null);
            }});

            HashMap<Object, Object> notSentGuild = (HashMap<Object, Object>) getDataNotSent().get("guilds");
            if (response.statusCode() != 200) {
                notSentGuild.put("date", date[5] + "-" + monthToNumber(date[1]) + "-" + date[2]);
                notSentGuild.put("guildCount", eventsToTrack.trackGuilds ? client.getServers().size() : null);
                notSentGuild.put("userCount", eventsToTrack.trackUserCount ? client.getCachedUsers().size() : null);

                putToDataNotSent("guilds", notSentGuild);
            }

            if (response.statusCode() == 200 && notSentGuild.size() > 0) sendDataNotSent();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}