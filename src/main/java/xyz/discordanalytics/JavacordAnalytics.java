package xyz.discordanalytics;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.Icon;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.*;
import org.json.JSONArray;
import org.json.JSONObject;
import xyz.discordanalytics.utilities.*;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class JavacordAnalytics extends AnalyticsBase {
    private final DiscordApi client;

    public JavacordAnalytics(DiscordApi api, String apiKey, Boolean debug) {
        super(apiKey, debug);
        this.client = api;
        this.baseAPIUrl = ApiEndpoints.BASE_URL + ApiEndpoints.BOT_STATS.replace("[id]", client.getYourself().getIdAsString());

        String[] date = new Date().toString().split(" ");
        this.setData(new JSONObject("{" +
                "\"date\": \"" + date[5] + "-" + monthToNumber(date[1]) + "-" + date[2] + "\"," +
                "\"guilds\": " + client.getServers().size() + "," +
                "\"users\": " + client.getCachedUsers().size() + "," +
                "\"interactions\": []," +
                "\"locales\": []," +
                "\"guildsLocales\": []," +
                "\"guildMembers\": {\"little\": 0, \"medium\": 0, \"big\": 0, \"huge\": 0}," +
                "\"guildsStats\": []," +
                "\"addedGuilds\": 0," +
                "\"removedGuilds\": 0," +
            "}"));
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

        if (isConfigInvalid(userClient.getName(), userClient.getAvatarHash().orElse(null), userClient.getIdAsString(), LibType.JAVACORD)) {
            new IOException(ErrorCodes.INVALID_CONFIGURATION).printStackTrace();
            return;
        }
        if (this.debug) {
            System.out.println("[DISCORDANALYTICS] Instance successfully initialized");
        }
        this.ready = true;

        client.addInteractionCreateListener(listener -> {
            try {
                Number guildCount = client.getServers().size();
                Number userCount = client.getCachedUsers().size();
                JSONArray guildsLocales = getData().getJSONArray("guildsLocales"); // [{locale: string, number: int}]
                JSONArray locales = getData().getJSONArray("locales"); // [{locale: string, number: int}]
                JSONArray interactions = getData().getJSONArray("interactions"); // [{name: string, type: int, number: int}]
                JSONArray guilds = getData().getJSONArray("guildsStats"); // [{guildId: string, name: string, icon: string, members: int, interactions: int}]

                String[] date = new Date().toString().split(" ");
                String dateString = date[5] + "-" + monthToNumber(date[1]) + "-" + date[2];

                DiscordLocale guildLocale = listener.getInteraction().getServerLocale().isPresent() ? listener.getInteraction().getServerLocale().get() : null;
                String guildLocaleString = guildLocale != null ? listener.getInteraction().getServerLocale().get().getLocaleCode() : null;
                if (guildLocaleString != null) {
                    boolean isGLTracked = false;
                    for (int i = 0; i < guildsLocales.length(); i++) {
                        LocalesItems item = parseStringToLocalesItems(guildsLocales.getString(i));
                        if (item.locale.equals(guildLocaleString)) {
                            item.number++;
                            guildsLocales.put(i, item.toString());
                            isGLTracked = true;
                            break;
                        }
                    }
                    if (!isGLTracked) guildsLocales.put(new LocalesItems(guildLocaleString, 1).toString());
                }

                DiscordLocale userLocale = listener.getInteraction().getLocale();
                String userLocaleString = userLocale.getLocaleCode();
                boolean isULTracked = false;
                for (int i = 0; i < locales.length(); i++) {
                    LocalesItems item = parseStringToLocalesItems(locales.getString(i));
                    if (item.locale.equals(userLocaleString)) {
                        item.number++;
                        locales.put(i, item.toString());
                        isULTracked = true;
                        break;
                    }
                }
                if (!isULTracked) locales.put(new LocalesItems(userLocaleString, 1).toString());

                Interaction interaction = listener.getInteraction();
                int interactionType = interaction.getType().getValue();
                String interactionName =
                        interactionType == InteractionType.APPLICATION_COMMAND.getValue()
                                ? ((ApplicationCommandInteraction) interaction).getCommandName()
                                    : interactionType == InteractionType.MESSAGE_COMPONENT.getValue()
                                    ? ((MessageComponentInteraction) interaction).getCustomId()
                                        : interactionType == InteractionType.APPLICATION_COMMAND_AUTOCOMPLETE.getValue()
                                        ? ((AutocompleteInteraction) interaction).getCommandName()
                                            : interactionType == InteractionType.MODAL_SUBMIT.getValue()
                                            ? ((ModalInteraction) interaction).getCustomId()
                                                : null;

                if (interactionName == null) return;

                boolean isITracked = false;
                for (int i = 0; i < interactions.length(); i++) {
                    InteractionItem item = parseStringToInteractionItem(interactions.getString(i));
                    if (item.name.equals(interactionName)) {
                        item.number++;
                        interactions.put(i, item.toString());
                        isITracked = true;
                        break;
                    }
                }
                if (!isITracked) interactions.put(new InteractionItem(interactionName, interactionType, 1).toString());

                boolean guildFound = false;
                Optional<Server> server = interaction.getServer();
                if (server.isPresent()) {
                    String name = server.get().getName();
                    Icon icon = server.get().getIcon().isPresent() ? server.get().getIcon().get() : null;
                    String iconId = icon != null ? icon.getUrl().toString().split("/")[5].split("\\.")[0] : null;
                    int memberCount = server.get().getMemberCount();
                    for (int i = 0; i < guilds.length(); i++) {
                        JSONObject guild = guilds.getJSONObject(i);
                        if (guild.getString("guildId").equals(server.get().getIdAsString())) {
                            guild.put("name", name);
                            guild.put("icon", iconId);
                            guild.put("members", memberCount);
                            guild.put("interactions", guild.getInt("interactions") + 1);
                            guilds.put(i, guild);
                            guildFound = true;
                            break;
                        }
                    }
                    if (!guildFound) {
                        guilds.put(new JSONObject("{" +
                                "\"guildId\": \"" + server.get().getIdAsString() + "\"," +
                                "\"name\": \"" + name + "\"," +
                                "\"icon\": \"" + iconId + "\"," +
                                "\"members\": " + memberCount + "," +
                                "\"interactions\": 1" +
                            "}"));
                    }
                }

                setData(new JSONObject("{" +
                        "\"date\": \"" + dateString + "\"," +
                        "\"guilds\": " + guildCount + "," +
                        "\"users\": " + userCount + "," +
                        "\"interactions\": " + interactions + "," +
                        "\"locales\": " + locales + "," +
                        "\"guildsLocales\": " + guildsLocales + "," +
                        "\"guildMembers\": " + calculateGuildMembersRepartition() + "," +
                        "\"guildsStats\": " + guilds + "," +
                        "\"addedGuilds\": " + getData().getInt("addedGuilds") + "," +
                        "\"removedGuilds\": " + getData().getInt("removedGuilds") +
                    "}"));

                System.out.println(getData().toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
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
        }).start();
    }

    private JSONObject calculateGuildMembersRepartition() {
        JSONObject result = new JSONObject();
        AtomicInteger little = new AtomicInteger();
        AtomicInteger medium = new AtomicInteger();
        AtomicInteger big = new AtomicInteger();
        AtomicInteger huge = new AtomicInteger();

        client.getServers().forEach((guild) -> {
            int members = guild.getMemberCount();
            if (members <= 100) little.getAndIncrement();
            else if (members <= 500) medium.getAndIncrement();
            else if (members <= 1500) big.getAndIncrement();
            else huge.getAndIncrement();
        });

        result.put("little", little.get());
        result.put("medium", medium.get());
        result.put("big", big.get());
        result.put("huge", huge.get());

        return result;
    }

    public void postStats() throws IOException, InterruptedException {
        Number guildCount = client.getServers().size();
        Number userCount = client.getCachedUsers().size();

        JSONObject data = getData();
        if (
                data.getInt("guilds") == guildCount.intValue()
                && data.getInt("users") == userCount.intValue()
                && data.getJSONArray("interactions").isEmpty()
                && data.getJSONArray("locales").isEmpty()
                && data.getJSONArray("guildsLocales").isEmpty()
        ) return;

        HttpResponse<String> response = super.post(data.toString());

        if (response.statusCode() == 401) {
            new IOException(ErrorCodes.INVALID_API_TOKEN).printStackTrace();
            return;
        }

        if (response.statusCode() == 429) {
            new IOException(ErrorCodes.ON_COOLDOWN).printStackTrace();
            return;
        }

        if (response.statusCode() == 423) {
            new IOException(ErrorCodes.SUSPENDED_BOT).printStackTrace();
            return;
        }

        if (response.statusCode() != 200) {
            new IOException(ErrorCodes.DATA_NOT_SENT).printStackTrace();
            return;
        }

        if (response.statusCode() == 200) {
            String[] date = new Date().toString().split(" ");
            super.setData(new JSONObject("{" +
                    "\"date\": \"" + date[5] + "-" + monthToNumber(date[1]) + "-" + date[2] + "\"," +
                    "\"guilds\": " + guildCount + "," +
                    "\"users\": " + userCount + "," +
                    "\"interactions\": []," +
                    "\"locales\": []," +
                    "\"guildsLocales\": []," +
                    "\"guildMembers\": {\"little\": 0, \"medium\": 0, \"big\": 0, \"huge\": 0}," +
                    "\"guildsStats\": []," +
                    "\"addedGuilds\": 0," +
                    "\"removedGuilds\": 0," +
                "}"));
        }
    }
}