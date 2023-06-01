package xyz.discordanalytics;

import xyz.discordanalytics.utilities.ApiEndpoints;
import xyz.discordanalytics.utilities.ErrorCodes;
import xyz.discordanalytics.utilities.EventsTracker;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;

public class AnalyticsBase {
    protected final EventsTracker eventsToTrack;
    protected final String apiKey;
    protected final HttpClient httpClient;
    protected final HashMap<Object, Object> dataToSend;
    protected Date precedentPostDate;
    protected String baseAPIUrl;

    public AnalyticsBase(EventsTracker eventsToTrack, String apiKey) {
        this.eventsToTrack = eventsToTrack;
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newHttpClient();

        String[] date = new Date().toString().split(" ");
        this.dataToSend = new HashMap<>() {{
            put("date", date[5] + "-" + monthToNumber(date[1]) + "-" + date[2]);
            put("guilds", 0);
            put("users", 0);
            put("interactions", new HashMap<>());
            put("locales", new HashMap<>());
            put("guildsLocales", new HashMap<>());
        }};
        this.precedentPostDate = new Date();
    }

    protected boolean isConfigInvalid(String username, String avatar, String id, String libType) throws IOException, InterruptedException {

        HttpResponse<String> response = httpClient.send(HttpRequest.newBuilder()
                .uri(URI.create(ApiEndpoints.BASE_URL + ApiEndpoints.BOT_URL.replace("[id]", id)))
                .header("Authorization", "Bot " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(new HashMap<>() {{
                    put("settings", new HashMap<>() {{
                        put("trackGuilds", eventsToTrack.trackGuilds);
                        put("trackGuildsLocale", eventsToTrack.trackGuildsLocale);
                        put("trackInteractions", eventsToTrack.trackInteractions);
                        put("trackUserCount", eventsToTrack.trackUserCount);
                        put("trackUserLanguage", eventsToTrack.trackUserLanguage);
                    }});
                    put("framework", libType);
                    put("username", username);
                    put("avatar", avatar);
                }}.toString()))
                .build(), HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 401) {
            new IOException(ErrorCodes.INVALID_API_TOKEN).printStackTrace();
            return true;
        }
        if (response.statusCode() == 429) {
            new IOException(ErrorCodes.ON_COOLDOWN).printStackTrace();
            return true;
        }
        if (response.statusCode() == 451) {
            new IOException(ErrorCodes.SUSPENDED_BOT).printStackTrace();
            return true;
        }

        return response.statusCode() != 200;
    }
    protected boolean isOnCooldown() {
        return new Date().getTime() - precedentPostDate.getTime() < 60000;
    }

    public EventsTracker getEventsToTrack() {
        return eventsToTrack;
    }

    public HttpResponse<String> post(String route, Object data) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseAPIUrl + route))
                .header("Authorization", "Bot " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(data.toString()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 401) new IOException(ErrorCodes.INVALID_API_TOKEN).printStackTrace();
        if (response.statusCode() == 429) new IOException(ErrorCodes.ON_COOLDOWN).printStackTrace();
        if (response.statusCode() == 451) new IOException(ErrorCodes.SUSPENDED_BOT).printStackTrace();

        return response;
    }

    public HashMap<Object, Object> getDataToSend() {
        return dataToSend;
    }
    public void putToDataToSend(String key, Object value) {
        dataToSend.put(key, value);
    }
    public void sendDataToSend() {

    }

    public static String monthToNumber(String month) {
        int monthIndex = Arrays.asList(new String[]{"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"}).indexOf(month);
        return (monthIndex < 9 ? "0" : "") + (monthIndex + 1);
    }
}
