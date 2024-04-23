package xyz.discordanalytics;

import org.json.JSONObject;
import xyz.discordanalytics.utilities.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.Date;

public class AnalyticsBase {
    protected final String apiKey;
    protected final HttpClient httpClient;
    protected JSONObject dataToSend;
    protected String baseAPIUrl;
    protected Boolean debug;
    protected Boolean ready;

    public AnalyticsBase(String apiKey, Boolean debug) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newHttpClient();
        this.debug = debug;
        this.ready = false;

        String[] date = new Date().toString().split(" ");
        this.dataToSend = new JSONObject("{" +
                "\"date\": \"" + date[5] + "-" + monthToNumber(date[1]) + "-" + date[2] + "\"," +
                "\"guilds\": 0," +
                "\"users\": 0," +
                "\"interactions\": []," +
                "\"locales\": []," +
                "\"guildsLocales\": []," +
                "\"guildMembers\": {\"little\": 0, \"medium\": 0, \"big\": 0, \"huge\": 0}," +
                "\"guildsStats\": []," +
                "\"addedGuilds\": 0," +
                "\"removedGuilds\": 0," +
            "}");
    }

    public InteractionItem parseStringToInteractionItem(String string) {
        String[] split = string.split(", ");
        String name = split[0].replace("{name: \"", "").replace("\"", "");
        int type = Integer.parseInt(split[1].replace("type: ", ""));
        int number = Integer.parseInt(split[2].replace("number: ", "").replace("}", ""));
        return new InteractionItem(name, type, number);
    }

    public LocalesItems parseStringToLocalesItems(String string) {
        String[] split = string.split(", ");
        String locale = split[0].replace("{locale: \"", "").replace("\"", "");
        int number = Integer.parseInt(split[1].replace("number: ", "").replace("}", ""));
        return new LocalesItems(locale, number);
    }

    protected boolean isConfigInvalid(String username, String avatar, String id, String libType) throws IOException, InterruptedException {
        JSONObject data = new JSONObject("{" +
                "\"framework\": \"" + libType + "\"," +
                "\"version\": \"" + AnalyticsBase.class.getPackage().getImplementationVersion() + "\"," +
                "\"username\": \"" + username + "\"," +
                "\"avatar\": \"" + avatar + "\"" +
            "}");
        String json = data.toString();

        HttpResponse<String> response = httpClient.send(HttpRequest.newBuilder()
                .uri(URI.create(ApiEndpoints.BASE_URL + ApiEndpoints.BOT_URL.replace("[id]", id)))
                .header("Authorization", "Bot " + apiKey)
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(json))
                .build(), HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 401) {
            new IOException(ErrorCodes.INVALID_API_TOKEN).printStackTrace();
            return true;
        }
        if (response.statusCode() == 429) {
            new IOException(ErrorCodes.ON_COOLDOWN).printStackTrace();
            return true;
        }
        if (response.statusCode() == 423) {
            new IOException(ErrorCodes.SUSPENDED_BOT).printStackTrace();
            return true;
        }

        return response.statusCode() != 200;
    }

    public HttpResponse<String> post(String data) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseAPIUrl))
                .header("Authorization", "Bot " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(data))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 401) new IOException(ErrorCodes.INVALID_API_TOKEN).printStackTrace();
        if (response.statusCode() == 429) new IOException(ErrorCodes.ON_COOLDOWN).printStackTrace();
        if (response.statusCode() == 423) new IOException(ErrorCodes.SUSPENDED_BOT).printStackTrace();

        return response;
    }

    public JSONObject getData() {
        return dataToSend;
    }
    public void setData(JSONObject data) {
        dataToSend = data;
    }

    public static String monthToNumber(String month) {
        int monthIndex = Arrays.asList(new String[]{"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"}).indexOf(month);
        return (monthIndex < 9 ? "0" : "") + (monthIndex + 1);
    }
}
