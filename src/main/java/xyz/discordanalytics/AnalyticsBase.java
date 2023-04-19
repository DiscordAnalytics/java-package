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
    protected final HashMap<Object, Object> dataNotSent;
    protected Date precedentPostDate;
    protected String baseAPIUrl;

    public AnalyticsBase(EventsTracker eventsToTrack, String apiKey) {
        this.eventsToTrack = eventsToTrack;
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newHttpClient();
        this.dataNotSent = new HashMap<>() {{
            put("interactions", new HashMap<>());
            put("messages", new HashMap<>());
        }};
        this.precedentPostDate = new Date();
    }

    protected boolean isConfigInvalid(String id, String libType) throws IOException, InterruptedException {

        HttpResponse<String> response = httpClient.send(HttpRequest.newBuilder()
            .uri(URI.create(ApiEndpoints.BASE_URL + ApiEndpoints.EDIT_SETTINGS_URL))
            .header("Authorization", apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(new HashMap<>() {{
                put("tracks", new HashMap<>() {{
                    put("interactions", eventsToTrack.trackInteractions);
                    put("guilds", eventsToTrack.trackGuilds);
                    put("userCount", eventsToTrack.trackUserCount);
                    put("userLanguage", eventsToTrack.trackUserLanguage);
                    put("guildsLocale", eventsToTrack.trackGuildsLocale);
                }});
                put("lib", libType);
                put("botId", id);
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
        return new Date().getTime() - precedentPostDate.getTime() < 600000;
    }

    public EventsTracker getEventsToTrack() {
        return eventsToTrack;
    }

    public HttpResponse<String> post(String route, Object data) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseAPIUrl + route))
                .header("Authorization", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(data.toString()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 401) new IOException(ErrorCodes.INVALID_API_TOKEN).printStackTrace();
        if (response.statusCode() == 429) new IOException(ErrorCodes.ON_COOLDOWN).printStackTrace();
        if (response.statusCode() == 451) new IOException(ErrorCodes.SUSPENDED_BOT).printStackTrace();

        return response;
    }

    public HashMap<Object, Object> getDataNotSent() {
        return dataNotSent;
    }
    public void putToDataNotSent(String key, Object value) {
        dataNotSent.put(key, value);
    }
    public void sendDataNotSent() {
        if (dataNotSent.get("interactions") != null) {
            try {
                HttpResponse<String> response = post(ApiEndpoints.ROUTES.INTERACTIONS, dataNotSent.get("interactions"));

                if (response.statusCode() == 200) dataNotSent.put("interactions", new HashMap<>());
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (dataNotSent.get("guilds") != null) {
            try {
                HttpResponse<String> response = post(ApiEndpoints.ROUTES.GUILDS, dataNotSent.get("guilds"));

                if (response.statusCode() == 200) dataNotSent.put("guilds", new HashMap<>());
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static String monthToNumber(String month) {
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        int monthIndex = Arrays.asList(months).indexOf(month);
        return (monthIndex < 9 ? "0" : "") + (monthIndex + 1);
    }
}
