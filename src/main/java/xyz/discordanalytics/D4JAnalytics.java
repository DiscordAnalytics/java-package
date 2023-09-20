package xyz.discordanalytics;

import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.guild.GuildDeleteEvent;
import discord4j.core.event.domain.interaction.InteractionCreateEvent;
import discord4j.core.object.command.Interaction;
import discord4j.discordjson.json.UserData;
import discord4j.discordjson.json.UserGuildData;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.discordanalytics.utilities.ApiEndpoints;
import xyz.discordanalytics.utilities.ErrorCodes;
import xyz.discordanalytics.utilities.EventsTracker;
import xyz.discordanalytics.utilities.LibType;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.Date;
import java.util.HashMap;
import java.util.Objects;

public class D4JAnalytics extends AnalyticsBase {
    private final DiscordClient client;

    public D4JAnalytics(DiscordClient client, EventsTracker eventsToTrack, String apiKey) {
        super(eventsToTrack, apiKey);
        this.client = client;
        this.baseAPIUrl = ApiEndpoints.BASE_URL + ApiEndpoints.BOT_STATS.replace("[id]", Objects.requireNonNull(client.getSelf().block().id().asString()));
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
        if (isInvalidClient()) return;
        assert userClient != null;

        if (isConfigInvalid(userClient.username(), userClient.avatar().orElse(null), userClient.id().asString(), LibType.DISCORD4J)) {
            new IOException(ErrorCodes.INVALID_CONFIGURATION).printStackTrace();
            return;
        }

        client.withGateway((GatewayDiscordClient gateway) -> {
            if (eventsToTrack.trackInteractions) {
                gateway.on(InteractionCreateEvent.class, event -> {
                    if (isOnCooldown()) return Mono.empty();

                    try {
                        Interaction interaction = event.getInteraction();
                        String[] date = new Date().toString().split(" ");
                        Flux<UserGuildData> clientGuilds = client.getGuilds();
                        Mono<Long> userCount = clientGuilds.flatMap(guild -> client.getGuildById(Snowflake.of(guild.id())).getMembers().count()).reduce(0L, Long::sum);

                        HttpResponse<String> response =
                                post(new HashMap<>() {{
                                            put("type", interaction.getType().getValue());
                                            put("name", interaction.getCommandInteraction().isPresent() ?
                                                    interaction.getCommandInteraction().get().getName().toString() :
                                                    interaction.getCommandInteraction().get().getCustomId().toString()
                                            );
                                            put("userLocale", eventsToTrack.trackUserLanguage ? interaction.getUserLocale() : null);
                                            put("userCount", eventsToTrack.trackUserCount ? userCount : null);
                                            put("guildCount", eventsToTrack.trackGuilds ? clientGuilds.count() : null);
                                            put("date", date[5] + "-" + monthToNumber(date[1]) + "-" + date[2]);
                                }}.toString());

                        HashMap<Object, Object> notSentInteraction = (HashMap<Object, Object>) getData().get("interactions");
                        if (response.statusCode() != 200 & notSentInteraction.size() == 0) {
                            new IOException(ErrorCodes.DATA_NOT_SENT).printStackTrace();
                            notSentInteraction.put("type", interaction.getType().getValue());
                            notSentInteraction.put("name", interaction.getCommandInteraction().isPresent() ?
                                    interaction.getCommandInteraction().get().getName().toString() :
                                    interaction.getCommandInteraction().get().getCustomId().toString()
                            );
                            notSentInteraction.put("userLocale", eventsToTrack.trackUserLanguage ? interaction.getUserLocale() : null);
                            notSentInteraction.put("userCount", eventsToTrack.trackUserCount ? userCount : null);
                            notSentInteraction.put("guildCount", eventsToTrack.trackGuilds ? clientGuilds.count() : null);
                            notSentInteraction.put("date", date[5] + "-" + monthToNumber(date[1]) + "-" + date[2]);
                            putToDataToSend("interactions", notSentInteraction);
                        }

                        if (response.statusCode() == 200 && notSentInteraction.size() > 0) sendDataToSend();

                        return Mono.empty();
                    } catch (IOException | InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
            if (eventsToTrack.trackGuilds) {
                gateway.on(GuildCreateEvent.class, event -> trackGuilds());
                gateway.on(GuildDeleteEvent.class, event -> trackGuilds());
            }

            precedentPostDate = new Date();

            return Mono.empty();
        });
    }

    private Mono<Object> trackGuilds() {
        if (isOnCooldown()) return Mono.empty();

        // String[] date = new Date().toString().split(" ");
        // Flux<UserGuildData> clientGuilds = client.getGuilds();
        // Mono<Long> userCount = clientGuilds.flatMap(guild -> client.getGuildById(Snowflake.of(guild.id())).getMembers().count()).reduce(0L, Long::sum);

        // HttpResponse<String> response = post(ApiEndpoints.ROUTES.GUILDS, new HashMap<>() {{
        //     put("date", date[5] + "-" + monthToNumber(date[1]) + "-" + date[2]);
        //     put("guildCount", eventsToTrack.trackGuilds ? clientGuilds.count() : null);
        //     put("userCount", eventsToTrack.trackUserCount ? userCount : null);
        // }}.toString());

        // HashMap<Object, Object> notSentGuild = (HashMap<Object, Object>) getDataToSend().get("guilds");
        // if (response.statusCode() != 200) {
        //     if (notSentGuild.size() == 0) new IOException(ErrorCodes.DATA_NOT_SENT).printStackTrace();
        //     notSentGuild.put("date", date[5] + "-" + monthToNumber(date[1]) + "-" + date[2]);
        //     notSentGuild.put("guildCount", eventsToTrack.trackGuilds ? clientGuilds.count() : null);
        //     notSentGuild.put("userCount", eventsToTrack.trackUserCount ? userCount : null);

        //     putToDataToSend("guilds", notSentGuild);
        // }

        // if (response.statusCode() == 200 && notSentGuild.size() > 0) sendDataToSend();

        return Mono.empty();
    }
}