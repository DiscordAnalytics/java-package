package xyz.discordanalytics;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.*;
import xyz.discordanalytics.utilities.*;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class JavacordAnalytics extends AnalyticsBase {
    private final DiscordApi client;

    public JavacordAnalytics(DiscordApi api, EventsTracker eventsToTrack, String apiKey) {
        super(eventsToTrack, apiKey);
        this.client = api;
        this.baseAPIUrl = ApiEndpoints.BASE_URL + ApiEndpoints.BOT_STATS.replace("[id]", client.getYourself().getIdAsString());

        String[] date = new Date().toString().split(" ");

        this.setData(new HashMap<>() {{
            put("date", date[5] + "-" + monthToNumber(date[1]) + "-" + date[2]);
            put("guilds", client.getServers().size());
            put("users", client.getCachedUsers().size());
            put("interactions", new ArrayList<>());
            put("locales", new ArrayList<>());
            put("guildsLocales", new ArrayList<>());
        }});
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

        if (eventsToTrack.trackInteractions) {
            client.addInteractionCreateListener(listener -> {
                try {
                    Number guildCount = eventsToTrack.trackGuilds ? client.getServers().size() : null;
                    Number userCount = eventsToTrack.trackUserCount ? client.getCachedUsers().size() : null;
                    ArrayList<String> guildsLocales = (ArrayList<String>) getData().get("guildsLocales");
                    ArrayList<String> locales = (ArrayList<String>) getData().get("locales");
                    ArrayList<String> interactions = (ArrayList<String>) getData().get("interactions");

                    String[] date = new Date().toString().split(" ");
                    String dateString = date[5] + "-" + monthToNumber(date[1]) + "-" + date[2];

                    DiscordLocale guildLocale = listener.getInteraction().getServerLocale().isPresent() ? listener.getInteraction().getServerLocale().get() : null;
                    String guildLocaleString = guildLocale != null ? listener.getInteraction().getServerLocale().get().getLocaleCode() : null;
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

                    DiscordLocale userLocale = listener.getInteraction().getLocale();
                    String userLocaleString = userLocale.getLocaleCode();
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

                    setData(new HashMap<>() {{
                        put("date", dateString);
                        put("guilds", guildCount);
                        put("users", userCount);
                        put("interactions", interactions);
                        put("locales", eventsToTrack.trackUserLanguage ? locales : new ArrayList<>());
                        put("guildsLocales", eventsToTrack.trackGuildsLocale ? guildsLocales : new ArrayList<>());
                    }});
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        new Thread(() -> {
            while (true) {
                try {
                    postStats();
                    Thread.sleep(60000*5);
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void postStats() throws IOException, InterruptedException {
        Number guildCount = eventsToTrack.trackGuilds ? client.getServers().size() : null;
        Number userCount = eventsToTrack.trackUserCount ? client.getCachedUsers().size() : null;

        HashMap<String, Object> data = super.getData();

        if (
                data.get("guilds") == guildCount
                && data.get("users") == userCount
                && ((ArrayList<String>) data.get("interactions")).size() == 0
                && ((ArrayList<String>) data.get("locales")).size() == 0
                && ((ArrayList<String>) data.get("guildsLocales")).size() == 0
        ) return;

        HttpResponse<String> response = super.post(new ObjectMapper()
                .writeValueAsString(data));

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
            super.setData(new HashMap<>() {{
                put("date", date[5] + "-" + monthToNumber(date[1]) + "-" + date[2]);
                put("guilds", guildCount);
                put("users", userCount);
                put("interactions", new ArrayList<>());
                put("locales", new ArrayList<>());
                put("guildsLocales", new ArrayList<>());
            }});
        }
    }
}