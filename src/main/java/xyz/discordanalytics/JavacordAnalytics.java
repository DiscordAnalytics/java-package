package xyz.discordanalytics;

import xyz.discordanalytics.utilities.ApiEndpoints;
import xyz.discordanalytics.utilities.ErrorCodes;
import xyz.discordanalytics.utilities.EventsTracker;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.Interaction;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Date;
import java.util.HashMap;

public class JavacordAnalytics extends AnalyticsBase {
    private final DiscordApi client;

    public JavacordAnalytics(DiscordApi api, EventsTracker eventsToTrack, String apiKey) {
        super(eventsToTrack, apiKey);
        this.client = api;
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

        if (isConfigInvalid(userClient.getIdAsString())) {
            new IOException(ErrorCodes.INVALID_CONFIGURATION).printStackTrace();
            return;
        }

        String baseAPIUrl = ApiEndpoints.BASE_URL + ApiEndpoints.TRACK_URL.replace("[id]", userClient.getIdAsString());

        if (eventsToTrack.trackInteractions) {
            client.addInteractionCreateListener(listener -> {
                if (isOnCooldown()) return;

                Interaction interaction = listener.getInteraction();
                String[] date = new Date().toString().split(" ");

                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseAPIUrl + ApiEndpoints.ROUTES.INTERACTIONS))
                    .header("Authorization", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(new HashMap<>() {{
                        put("type", interaction.getType().toString());
                        put("name", interaction.getIdAsString());
                        put("userLocale", null); // because it's not possible to get the user locale
                        put("userCount", eventsToTrack.trackUserCount ? client.getCachedUsers().size() : null);
                        put("guildCount", eventsToTrack.trackGuilds ? client.getServers().size() : null);
                        put("date", date[5] + "-" + monthToNumber(date[1]) + "-" + date[2]);
                    }}.toString()))
                    .build();

                HttpResponse<String> response = null;

                try {
                    response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }

                if (response == null) {
                    new IOException(ErrorCodes.DATA_NOT_SENT).printStackTrace();
                }
                assert response != null;
                if (response.statusCode() == 401) {
                    new IOException(ErrorCodes.INVALID_API_TOKEN).printStackTrace();
                }
                if (response.statusCode() == 429) {
                    new IOException(ErrorCodes.ON_COOLDOWN).printStackTrace();
                }
                if (response.statusCode() == 451) {
                    new IOException(ErrorCodes.SUSPENDED_BOT).printStackTrace();
                }
                HashMap<Object, Object> notSentInteraction = (HashMap<Object, Object>) dataNotSent.get("interactions");
                if (response.statusCode() != 200) {
                    if (notSentInteraction.size() == 0) {
                        new IOException(ErrorCodes.DATA_NOT_SENT).printStackTrace();
                    }
                    notSentInteraction.put("type", interaction.getType().getValue());
                    notSentInteraction.put("name", interaction.getIdAsString());
                    notSentInteraction.put("userLocale", null); // because it's not possible to get the user locale
                    notSentInteraction.put("userCount", eventsToTrack.trackUserCount ? client.getCachedUsers().size() : null);
                    notSentInteraction.put("guildCount", eventsToTrack.trackGuilds ? client.getServers().size() : null);
                    notSentInteraction.put("date", date[5] + "-" + monthToNumber(date[1]) + "-" + date[2]);

                    dataNotSent.put("interactions", notSentInteraction);
                }

                if (response.statusCode() == 200 && notSentInteraction.size() > 0) {
                    sendDataNotSent();
                }
            });
        }
        if (eventsToTrack.trackGuilds) {
            client.addServerJoinListener(listener -> trackGuilds(baseAPIUrl));
            client.addServerLeaveListener(listener -> trackGuilds(baseAPIUrl));
        }

        precedentPostDate = new Date();
    }

    private void trackGuilds(String baseAPIUrl) {
        if (isOnCooldown()) {
            new IOException(ErrorCodes.ON_COOLDOWN).printStackTrace();
            return;
        }

        String[] date = new Date().toString().split(" ");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseAPIUrl + ApiEndpoints.ROUTES.GUILDS))
                .header("Authorization", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(new HashMap<>() {{
                    put("date", date[5] + "-" + monthToNumber(date[1]) + "-" + date[2]);
                    put("guildCount", eventsToTrack.trackGuilds ? client.getServers().size() : null);
                    put("userCount", eventsToTrack.trackUserCount ? client.getCachedUsers().size() : null);
                }}.toString()))
                .build();

        HttpResponse<String> response = null;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        if (response == null) {
            new IOException(ErrorCodes.DATA_NOT_SENT).printStackTrace();
        }
        assert response != null;
        if (response.statusCode() == 401) {
            new IOException(ErrorCodes.INVALID_API_TOKEN).printStackTrace();
        }
        if (response.statusCode() == 429) {
            new IOException(ErrorCodes.ON_COOLDOWN).printStackTrace();
        }
        if (response.statusCode() == 451) {
            new IOException(ErrorCodes.SUSPENDED_BOT).printStackTrace();
        }
        HashMap<Object, Object> notSentGuild = (HashMap<Object, Object>) dataNotSent.get("guilds");
        if (response.statusCode() != 200) {
            if (notSentGuild.size() == 0) {
                new IOException(ErrorCodes.DATA_NOT_SENT).printStackTrace();
            }
            notSentGuild.put("date", date[5] + "-" + monthToNumber(date[1]) + "-" + date[2]);
            notSentGuild.put("guildCount", eventsToTrack.trackGuilds ? client.getServers().size() : null);
            notSentGuild.put("userCount", eventsToTrack.trackUserCount ? client.getCachedUsers().size() : null);

            dataNotSent.put("guilds", notSentGuild);
        }

        if (response.statusCode() == 200 && notSentGuild.size() > 0) {
            sendDataNotSent();
        }
    }

    private void sendDataNotSent() {
        User userClient = client.getYourself();
        if (isInvalidClient()) return;
        assert userClient != null;

        HashMap<Object, Object> notSentInteraction = (HashMap<Object, Object>) dataNotSent.get("interactions");
        HashMap<Object, Object> notSentGuild = (HashMap<Object, Object>) dataNotSent.get("guilds");

        String baseAPIUrl = ApiEndpoints.BASE_URL + ApiEndpoints.TRACK_URL.replace("[id]", userClient.getIdAsString());

        if (notSentInteraction.size() > 0) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseAPIUrl + ApiEndpoints.ROUTES.INTERACTIONS))
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
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }

            if (response == null) {
                new IOException(ErrorCodes.DATA_NOT_SENT).printStackTrace();
                return;
            }
            if (response.statusCode() == 401) {
                new IOException(ErrorCodes.INVALID_API_TOKEN).printStackTrace();
            }
            if (response.statusCode() == 451) {
                new IOException(ErrorCodes.SUSPENDED_BOT).printStackTrace();
            }
            if (response.statusCode() == 200) {
                dataNotSent.put("interactions", new HashMap<>());
            }
        }
        if (notSentGuild.size() > 0) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseAPIUrl + ApiEndpoints.ROUTES.GUILDS))
                    .header("Authorization", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(new HashMap<>() {{
                        put("date", notSentGuild.get("date"));
                        put("guildCount", notSentGuild.get("guildCount"));
                        put("userCount", notSentGuild.get("userCount"));
                    }}.toString()))
                    .build();

            HttpResponse<String> response = null;
            try {
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }

            if (response == null) {
                new IOException(ErrorCodes.DATA_NOT_SENT).printStackTrace();
                return;
            }
            if (response.statusCode() == 401) {
                new IOException(ErrorCodes.INVALID_API_TOKEN).printStackTrace();
            }
            if (response.statusCode() == 451) {
                new IOException(ErrorCodes.SUSPENDED_BOT).printStackTrace();
            }
            if (response.statusCode() == 200) {
                dataNotSent.put("guilds", new HashMap<>());
            }
        }
    }
}