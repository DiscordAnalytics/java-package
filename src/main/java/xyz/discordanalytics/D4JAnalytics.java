package xyz.discordanalytics;

import com.fasterxml.jackson.databind.ObjectMapper;
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.*;
import discord4j.core.object.command.Interaction;
import discord4j.discordjson.json.UserData;
import reactor.core.publisher.Mono;
import xyz.discordanalytics.utilities.*;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.*;

public class D4JAnalytics extends AnalyticsBase {
    private final DiscordClient client;

    public D4JAnalytics(DiscordClient client, String apiKey, Boolean debug) {
        super(apiKey, debug);
        this.client = client;
        this.baseAPIUrl = ApiEndpoints.BASE_URL + ApiEndpoints.BOT_STATS.replace("[id]", Objects.requireNonNull(Objects.requireNonNull(client.getSelf().block()).id().asString()));

        String[] date = new Date().toString().split(" ");

//        this.setData(new HashMap<>() {{
//            put("date", date[5] + "-" + monthToNumber(date[1]) + "-" + date[2]);
//            put("guilds", client.getGuilds().count().block());
//            put("users", client.getGuilds().flatMap(guild -> client.getGuildById(Snowflake.of(guild.id())).getMembers().count()).reduce(0L, Long::sum).block());
//            put("interactions", new ArrayList<>());
//            put("locales", new ArrayList<>());
//            put("guildsLocales", new ArrayList<>());
//        }});
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
            gateway.on(ApplicationCommandInteractionEvent.class, event -> {
                Number guildCount = client.getGuilds().count().block();
                Number userCount = client.getGuilds().flatMap(guild -> client.getGuildById(Snowflake.of(guild.id())).getMembers().count()).reduce(0L, Long::sum).block();
                ArrayList<String> guildsLocales = (ArrayList<String>) getData().get("guildsLocales");
                ArrayList<String> locales = (ArrayList<String>) getData().get("locales");
                ArrayList<String> interactions = (ArrayList<String>) getData().get("interactions");

                String[] date = new Date().toString().split(" ");
                String dateString = date[5] + "-" + monthToNumber(date[1]) + "-" + date[2];

                Optional<String> guildLocale = event.getInteraction().getGuildLocale();
                String guildLocaleString = guildLocale.orElse(null);
                if (guildLocaleString != null) {
                    boolean isGLTracked = false;
                    for (int i = 0; i < guildsLocales.size(); i++) {
                        LocalesItems item = parseStringToLocalesItems(guildsLocales.get(i));
                        if (item.locale.equals(guildLocaleString)) {
                            item.number++;
                            guildsLocales.set(i, item.toString());
                            isGLTracked = true;
                            break;
                        }
                    }
                    if (!isGLTracked) guildsLocales.add(new LocalesItems(guildLocaleString, 1).toString());
                }

                String userLocaleString = event.getInteraction().getUserLocale();
                boolean isULTracked = false;
                for (int i = 0; i < locales.size(); i++) {
                    LocalesItems item = parseStringToLocalesItems(locales.get(i));
                    if (item.locale.equals(userLocaleString)) {
                        item.number++;
                        locales.set(i, item.toString());
                        isULTracked = true;
                        break;
                    }
                }
                if (!isULTracked) locales.add(new LocalesItems(userLocaleString, 1).toString());

                Interaction interaction = event.getInteraction();
                int interactionType = interaction.getType().getValue();
                String interactionName = event.getCommandName();

                boolean isITracked = false;
                for (int i = 0; i < interactions.size(); i++) {
                    InteractionItem item = parseStringToInteractionItem(interactions.get(i));
                    if (item.name.equals(interactionName)) {
                        item.number++;
                        interactions.set(i, item.toString());
                        isITracked = true;
                        break;
                    }
                }
                if (!isITracked) interactions.add(new InteractionItem(interactionName, interactionType, 1).toString());

//                setData(new HashMap<>() {{
//                    put("date", dateString);
//                    put("guilds", guildCount);
//                    put("users", userCount);
//                    put("interactions", interactions);
//                    put("locales", locales);
//                    put("guildsLocales", guildsLocales);
//                }});

                return Mono.empty();
            });

            gateway.on(ComponentInteractionEvent.class, event -> {
                Number guildCount = client.getGuilds().count().block();
                Number userCount = client.getGuilds().flatMap(guild -> client.getGuildById(Snowflake.of(guild.id())).getMembers().count()).reduce(0L, Long::sum).block();
                ArrayList<String> guildsLocales = (ArrayList<String>) getData().get("guildsLocales");
                ArrayList<String> locales = (ArrayList<String>) getData().get("locales");
                ArrayList<String> interactions = (ArrayList<String>) getData().get("interactions");

                String[] date = new Date().toString().split(" ");
                String dateString = date[5] + "-" + monthToNumber(date[1]) + "-" + date[2];

                Optional<String> guildLocale = event.getInteraction().getGuildLocale();
                String guildLocaleString = guildLocale.orElse(null);
                if (guildLocaleString != null) {
                    boolean isGLTracked = false;
                    for (int i = 0; i < guildsLocales.size(); i++) {
                        LocalesItems item = parseStringToLocalesItems(guildsLocales.get(i));
                        if (item.locale.equals(guildLocaleString)) {
                            item.number++;
                            guildsLocales.set(i, item.toString());
                            isGLTracked = true;
                            break;
                        }
                    }
                    if (!isGLTracked) guildsLocales.add(new LocalesItems(guildLocaleString, 1).toString());
                }

                String userLocaleString = event.getInteraction().getUserLocale();
                boolean isULTracked = false;
                for (int i = 0; i < locales.size(); i++) {
                    LocalesItems item = parseStringToLocalesItems(locales.get(i));
                    if (item.locale.equals(userLocaleString)) {
                        item.number++;
                        locales.set(i, item.toString());
                        isULTracked = true;
                        break;
                    }
                }
                if (!isULTracked) locales.add(new LocalesItems(userLocaleString, 1).toString());

                Interaction interaction = event.getInteraction();
                int interactionType = interaction.getType().getValue();
                String interactionName = event.getCustomId();

                boolean isITracked = false;
                for (int i = 0; i < interactions.size(); i++) {
                    InteractionItem item = parseStringToInteractionItem(interactions.get(i));
                    if (item.name.equals(interactionName)) {
                        item.number++;
                        interactions.set(i, item.toString());
                        isITracked = true;
                        break;
                    }
                }
                if (!isITracked) interactions.add(new InteractionItem(interactionName, interactionType, 1).toString());

//                setData(new HashMap<>() {{
//                    put("date", dateString);
//                    put("guilds", guildCount);
//                    put("users", userCount);
//                    put("interactions", interactions);
//                    put("locales", locales);
//                    put("guildsLocales", guildsLocales);
//                }});

                return Mono.empty();
            });

            gateway.on(ModalSubmitInteractionEvent.class, event -> {
                Number guildCount = client.getGuilds().count().block();
                Number userCount = client.getGuilds().flatMap(guild -> client.getGuildById(Snowflake.of(guild.id())).getMembers().count()).reduce(0L, Long::sum).block();
                ArrayList<String> guildsLocales = (ArrayList<String>) getData().get("guildsLocales");
                ArrayList<String> locales = (ArrayList<String>) getData().get("locales");
                ArrayList<String> interactions = (ArrayList<String>) getData().get("interactions");

                String[] date = new Date().toString().split(" ");
                String dateString = date[5] + "-" + monthToNumber(date[1]) + "-" + date[2];

                Optional<String> guildLocale = event.getInteraction().getGuildLocale();
                String guildLocaleString = guildLocale.orElse(null);
                if (guildLocaleString != null) {
                    boolean isGLTracked = false;
                    for (int i = 0; i < guildsLocales.size(); i++) {
                        LocalesItems item = parseStringToLocalesItems(guildsLocales.get(i));
                        if (item.locale.equals(guildLocaleString)) {
                            item.number++;
                            guildsLocales.set(i, item.toString());
                            isGLTracked = true;
                            break;
                        }
                    }
                    if (!isGLTracked) guildsLocales.add(new LocalesItems(guildLocaleString, 1).toString());
                }

                String userLocaleString = event.getInteraction().getUserLocale();
                boolean isULTracked = false;
                for (int i = 0; i < locales.size(); i++) {
                    LocalesItems item = parseStringToLocalesItems(locales.get(i));
                    if (item.locale.equals(userLocaleString)) {
                        item.number++;
                        locales.set(i, item.toString());
                        isULTracked = true;
                        break;
                    }
                }
                if (!isULTracked) locales.add(new LocalesItems(userLocaleString, 1).toString());

                Interaction interaction = event.getInteraction();
                int interactionType = interaction.getType().getValue();
                String interactionName = event.getCustomId();

                boolean isITracked = false;
                for (int i = 0; i < interactions.size(); i++) {
                    InteractionItem item = parseStringToInteractionItem(interactions.get(i));
                    if (item.name.equals(interactionName)) {
                        item.number++;
                        interactions.set(i, item.toString());
                        isITracked = true;
                        break;
                    }
                }
                if (!isITracked) interactions.add(new InteractionItem(interactionName, interactionType, 1).toString());

//                setData(new HashMap<>() {{
//                    put("date", dateString);
//                    put("guilds", guildCount);
//                    put("users", userCount);
//                    put("interactions", interactions);
//                    put("locales", locales);
//                    put("guildsLocales", guildsLocales);
//                }});

                return Mono.empty();
            });

            gateway.on(ChatInputAutoCompleteEvent.class, event -> {
                Number guildCount = client.getGuilds().count().block();
                Number userCount = client.getGuilds().flatMap(guild -> client.getGuildById(Snowflake.of(guild.id())).getMembers().count()).reduce(0L, Long::sum).block();
                ArrayList<String> guildsLocales = (ArrayList<String>) getData().get("guildsLocales");
                ArrayList<String> locales = (ArrayList<String>) getData().get("locales");
                ArrayList<String> interactions = (ArrayList<String>) getData().get("interactions");

                String[] date = new Date().toString().split(" ");
                String dateString = date[5] + "-" + monthToNumber(date[1]) + "-" + date[2];

                Optional<String> guildLocale = event.getInteraction().getGuildLocale();
                String guildLocaleString = guildLocale.orElse(null);
                if (guildLocaleString != null) {
                    boolean isGLTracked = false;
                    for (int i = 0; i < guildsLocales.size(); i++) {
                        LocalesItems item = parseStringToLocalesItems(guildsLocales.get(i));
                        if (item.locale.equals(guildLocaleString)) {
                            item.number++;
                            guildsLocales.set(i, item.toString());
                            isGLTracked = true;
                            break;
                        }
                    }
                    if (!isGLTracked) guildsLocales.add(new LocalesItems(guildLocaleString, 1).toString());
                }

                String userLocaleString = event.getInteraction().getUserLocale();
                boolean isULTracked = false;
                for (int i = 0; i < locales.size(); i++) {
                    LocalesItems item = parseStringToLocalesItems(locales.get(i));
                    if (item.locale.equals(userLocaleString)) {
                        item.number++;
                        locales.set(i, item.toString());
                        isULTracked = true;
                        break;
                    }
                }
                if (!isULTracked) locales.add(new LocalesItems(userLocaleString, 1).toString());

                Interaction interaction = event.getInteraction();
                int interactionType = interaction.getType().getValue();
                String interactionName = event.getCommandName();

                boolean isITracked = false;
                for (int i = 0; i < interactions.size(); i++) {
                    InteractionItem item = parseStringToInteractionItem(interactions.get(i));
                    if (item.name.equals(interactionName)) {
                        item.number++;
                        interactions.set(i, item.toString());
                        isITracked = true;
                        break;
                    }
                }
                if (!isITracked) interactions.add(new InteractionItem(interactionName, interactionType, 1).toString());

//                setData(new HashMap<>() {{
//                    put("date", dateString);
//                    put("guilds", guildCount);
//                    put("users", userCount);
//                    put("interactions", interactions);
//                    put("locales", locales);
//                    put("guildsLocales", guildsLocales);
//                }});

                return Mono.empty();
            });

            return Mono.empty();
        });

        new Thread(() -> {
            while (true) {
                try {
                    postStats();
                    Thread.sleep(5*60000);
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void postStats() throws IOException, InterruptedException {
        Number guildCount = client.getGuilds().count().block();
        Number userCount = client.getGuilds().flatMap(guild -> client.getGuildById(Snowflake.of(guild.id())).getMembers().count()).reduce(0L, Long::sum).block();

//        HashMap<String, Object> data = super.getData();

//        if (
//                data.get("guilds") == guildCount
//                && data.get("users") == userCount
//                && ((ArrayList<String>) data.get("interactions")).size() == 0
//                && ((ArrayList<String>) data.get("locales")).size() == 0
//                && ((ArrayList<String>) data.get("guildsLocales")).size() == 0
//        ) return;
//
//        HttpResponse<String> response = super.post(new ObjectMapper()
//                .writeValueAsString(data));
//
//        if (response.statusCode() == 401) {
//            new IOException(ErrorCodes.INVALID_API_TOKEN).printStackTrace();
//            return;
//        }
//        if (response.statusCode() == 429) {
//            new IOException(ErrorCodes.ON_COOLDOWN).printStackTrace();
//            return;
//        }
//        if (response.statusCode() == 423) {
//            new IOException(ErrorCodes.SUSPENDED_BOT).printStackTrace();
//            return;
//        }
//        if (response.statusCode() != 200) {
//            new IOException(ErrorCodes.DATA_NOT_SENT).printStackTrace();
//            return;
//        }
//
//        if (response.statusCode() == 200) {
//            String[] date = new Date().toString().split(" ");
//            super.setData(new HashMap<>() {{
//                put("date", date[5] + "-" + monthToNumber(date[1]) + "-" + date[2]);
//                put("guilds", guildCount);
//                put("users", userCount);
//                put("interactions", new ArrayList<>());
//                put("locales", new ArrayList<>());
//                put("guildsLocales", new ArrayList<>());
//            }});
//        }
    }
}