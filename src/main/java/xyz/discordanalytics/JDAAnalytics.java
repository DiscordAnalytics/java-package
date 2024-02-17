package xyz.discordanalytics;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.SelfUser;
import xyz.discordanalytics.jda.InteractionTrackerListener;
import xyz.discordanalytics.utilities.ApiEndpoints;
import xyz.discordanalytics.utilities.ErrorCodes;
import xyz.discordanalytics.utilities.LibType;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class JDAAnalytics extends AnalyticsBase {
    private final JDA client;

    public JDAAnalytics(JDA jda, String apiKey) {
        super(apiKey);
        this.client = jda;
        this.baseAPIUrl = ApiEndpoints.BASE_URL + ApiEndpoints.BOT_STATS.replace("[id]", client.getSelfUser().getId());

        String[] date = new Date().toString().split(" ");

        this.setData(new HashMap<>() {{
            put("date", date[5] + "-" + monthToNumber(date[1]) + "-" + date[2]);
            put("guilds", client.getGuilds().size());
            put("users", client.getUsers().size());
            put("interactions", new ArrayList<>());
            put("locales", new ArrayList<>());
            put("guildsLocales", new ArrayList<>());
        }});
    }

    public JDA getClient() {
        return client;
    }

    private boolean isInvalidClient() {
        if (client == null) {
            new IOException(ErrorCodes.INVALID_CLIENT_TYPE).printStackTrace();
            return true;
        }
        return false;
    }

    public void trackEvents() throws IOException, InterruptedException {
        SelfUser userClient = client.getSelfUser();
        if (isInvalidClient()) return;

        if (isConfigInvalid(userClient.getName(), userClient.getAvatarId(), userClient.getId(), LibType.JDA)) {
            new IOException(ErrorCodes.INVALID_CONFIGURATION).printStackTrace();
            return;
        }

        client.addEventListener(new InteractionTrackerListener(this));

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

    public void postStats() throws IOException, InterruptedException {
        Number guildCount = client.getGuilds().size();
        Number userCount = client.getUsers().size();

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
