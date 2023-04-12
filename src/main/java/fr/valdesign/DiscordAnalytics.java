package fr.valdesign;

import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.guild.GuildDeleteEvent;
import discord4j.core.event.domain.guild.GuildEvent;
import discord4j.core.event.domain.interaction.InteractionCreateEvent;
import discord4j.core.object.command.Interaction;
import discord4j.discordjson.json.UserData;
import discord4j.discordjson.json.UserGuildData;
import fr.valdesign.utilities.ApiEndpoints;
import fr.valdesign.utilities.ErrorCodes;
import fr.valdesign.utilities.EventsTracker;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Objects;

public class DiscordAnalytics {
    private final DiscordClient client;
    private final EventsTracker eventsToTrack;
    private final String apiKey;
    private final HttpClient httpClient;
    private final HashMap<Object, Object> dataNotSent;

    public DiscordAnalytics(DiscordClient client, EventsTracker eventsToTrack, String apiKey) {
        this.client = client;
        this.eventsToTrack = eventsToTrack;
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newHttpClient();
        this.dataNotSent = new HashMap<>() {{
            put("interactions", new HashMap<>());
            put("guilds", new HashMap<>());
        }};
    }

    private boolean isConfigValid() throws IOException, InterruptedException {
        HashMap<String, Object> tracks = new HashMap<>() {{
            put("interactions", eventsToTrack.trackInteractions);
            put("guilds", eventsToTrack.trackGuilds);
            put("userCount", eventsToTrack.trackUserCount);
            put("userLanguage", eventsToTrack.trackUserLanguage);
            put("guildsLocale", eventsToTrack.trackGuildsLocale);
        }};
        HashMap<String, Object> map = new HashMap<>() {{
            put("tracks", tracks);
            put("lib", "d4j");
            put("botId", Objects.requireNonNull(client.getSelf().block()).id());
        }};

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ApiEndpoints.BASE_URL + ApiEndpoints.EDIT_SETTINGS_URL))
                .header("Authorization", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(map.toString()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 401) {
            new IOException(ErrorCodes.INVALID_API_TOKEN).printStackTrace();
            return false;
        }

        return response.statusCode() == 200;
    }
    private boolean isInvalidClient() {
        if (client == null) {
            new IOException(ErrorCodes.INVALID_CLIENT_TYPE).printStackTrace();
            return true;
        }
        UserData userClient = client.getSelf().block();
        if (userClient == null) {
            new IOException(ErrorCodes.CLIENT_NOT_READY).printStackTrace();
            return true;
        }
        return false;
    }

    public void trackEvents() throws IOException, InterruptedException {
        UserData userClient = client.getSelf().block();
        if (isInvalidClient()) {
            new IOException(ErrorCodes.INVALID_CLIENT_TYPE).printStackTrace();
            return;
        }
        assert userClient != null;

        if (!isConfigValid()) {
            new IOException(ErrorCodes.INVALID_CONFIGURATION).printStackTrace();
            return;
        }
        String baseAPIUrl = ApiEndpoints.BASE_URL + ApiEndpoints.TRACK_URL.replace("[id]", (CharSequence) userClient.id());

        client.withGateway((GatewayDiscordClient gateway) -> {
            if (eventsToTrack.trackInteractions) {
                gateway.on(InteractionCreateEvent.class, event -> {
                    Interaction interaction = event.getInteraction();
                    String[] date = new Date().toString().split(" ");
                    Flux<UserGuildData> clientGuilds = client.getGuilds();
                    Mono<Long> userCount = clientGuilds.flatMap(guild -> client.getGuildById(Snowflake.of(guild.id())).getMembers().count()).reduce(0L, Long::sum);

                    HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(baseAPIUrl + ApiEndpoints.ROUTES.INTERACTIONS))
                        .header("Authorization", apiKey)
                        .POST(HttpRequest.BodyPublishers.ofString(new HashMap<>() {{
                            put("type", interaction.getType().getValue());
                            put("name", interaction.getCommandInteraction().isPresent() ?
                                interaction.getCommandInteraction().get().getName().toString() :
                                interaction.getCommandInteraction().get().getCustomId().toString()
                            );
                            put("userLocale", eventsToTrack.trackUserLanguage ? interaction.getUserLocale() : null);
                            put("userCount", eventsToTrack.trackUserCount ? userCount : null);
                            put("guildCount", eventsToTrack.trackGuilds ? clientGuilds.count() : null);
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
                        return Mono.empty();
                    }
                    if (response.statusCode() == 401) {
                        new IOException(ErrorCodes.INVALID_API_TOKEN).printStackTrace();
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
                        notSentInteraction.put("name", interaction.getCommandInteraction().isPresent() ?
                                interaction.getCommandInteraction().get().getName().toString() :
                                interaction.getCommandInteraction().get().getCustomId().toString()
                        );
                        notSentInteraction.put("userLocale", eventsToTrack.trackUserLanguage ? interaction.getUserLocale() : null);
                        notSentInteraction.put("userCount", eventsToTrack.trackUserCount ? userCount : null);
                        notSentInteraction.put("guildCount", eventsToTrack.trackGuilds ? clientGuilds.count() : null);
                        notSentInteraction.put("date", date[5] + "-" + monthToNumber(date[1]) + "-" + date[2]);

                        dataNotSent.put("interactions", notSentInteraction);
                    }

                    if (response.statusCode() == 200 && notSentInteraction.size() > 0) {
                        sendDataNotSent();
                    }

                    return Mono.empty();
                });
            }
            if (eventsToTrack.trackGuilds) {
                gateway.on(GuildCreateEvent.class, event -> trackGuilds(baseAPIUrl));
                gateway.on(GuildDeleteEvent.class, event -> trackGuilds(baseAPIUrl));
            }

            return Mono.empty();
        });
    }

    private Mono<Object> trackGuilds(String baseAPIUrl) {
        String[] date = new Date().toString().split(" ");
        Flux<UserGuildData> clientGuilds = client.getGuilds();
        Mono<Long> userCount = clientGuilds.flatMap(guild -> client.getGuildById(Snowflake.of(guild.id())).getMembers().count()).reduce(0L, Long::sum);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseAPIUrl + ApiEndpoints.ROUTES.GUILDS))
            .header("Authorization", apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(new HashMap<>() {{
                put("date", date[5] + "-" + monthToNumber(date[1]) + "-" + date[2]);
                put("guildCount", eventsToTrack.trackGuilds ? clientGuilds.count() : null);
                put("userCount", eventsToTrack.trackUserCount ? userCount : null);
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
            return Mono.empty();
        }
        if (response.statusCode() == 401) {
            new IOException(ErrorCodes.INVALID_API_TOKEN).printStackTrace();
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
            notSentGuild.put("guildCount", eventsToTrack.trackGuilds ? clientGuilds.count() : null);
            notSentGuild.put("userCount", eventsToTrack.trackUserCount ? userCount : null);

            dataNotSent.put("guilds", notSentGuild);
        }

        if (response.statusCode() == 200 && notSentGuild.size() > 0) {
            sendDataNotSent();
        }

        return Mono.empty();
    }

    private void sendDataNotSent() {
        UserData userClient = client.getSelf().block();
        if (isInvalidClient()) {
            new IOException(ErrorCodes.INVALID_CLIENT_TYPE).printStackTrace();
            return;
        }
        assert userClient != null;

        HashMap<Object, Object> notSentInteraction = (HashMap<Object, Object>) dataNotSent.get("interactions");
        HashMap<Object, Object> notSentGuild = (HashMap<Object, Object>) dataNotSent.get("guilds");

        String baseAPIUrl = ApiEndpoints.BASE_URL + ApiEndpoints.TRACK_URL.replace("[id]", (CharSequence) userClient.id());

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

    private static String monthToNumber(String month) {
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        int monthIndex = Arrays.asList(months).indexOf(month);
        return (monthIndex < 9 ? "0" : "") + (monthIndex + 1);
    }
}