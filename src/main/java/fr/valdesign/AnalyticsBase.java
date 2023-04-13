package fr.valdesign;

import fr.valdesign.utilities.ApiEndpoints;
import fr.valdesign.utilities.ErrorCodes;
import fr.valdesign.utilities.EventsTracker;

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

    protected boolean isConfigInvalid(String id) throws IOException, InterruptedException {
        HashMap<String, Object> tracks = new HashMap<>() {{
            put("interactions", eventsToTrack.trackInteractions);
            put("guilds", eventsToTrack.trackGuilds);
            put("userCount", eventsToTrack.trackUserCount);
            put("userLanguage", eventsToTrack.trackUserLanguage);
            put("guildsLocale", eventsToTrack.trackGuildsLocale);
        }};
        HashMap<String, Object> map = new HashMap<>() {{
            put("tracks", tracks);
            put("lib", "javacord");
            put("botId", id);
        }};

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ApiEndpoints.BASE_URL + ApiEndpoints.EDIT_SETTINGS_URL))
                .header("Authorization", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(map.toString()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 401) {
            new IOException(ErrorCodes.INVALID_API_TOKEN).printStackTrace();
            return true;
        }

        return response.statusCode() != 200;
    }

    protected boolean isOnCooldown() {
        long diff = new Date().getTime() - precedentPostDate.getTime();
        return diff < 600000;
    }

    protected static String monthToNumber(String month) {
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        int monthIndex = Arrays.asList(months).indexOf(month);
        return (monthIndex < 9 ? "0" : "") + (monthIndex + 1);
    }
}
