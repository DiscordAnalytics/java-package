package fr.valdesign;

import fr.valdesign.utilities.ApiEndpoints;
import fr.valdesign.utilities.ErrorCodes;
import fr.valdesign.utilities.EventsTracker;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;

public class DiscordAnalytics {
    private final EventsTracker eventsToTrack;
    private final String apiKey;
    private final HttpClient httpClient;

    public DiscordAnalytics(EventsTracker eventsToTrack, String apiKey) {
        this.eventsToTrack = eventsToTrack;
        this.apiKey = apiKey;
        HttpClient httpClient = HttpClient.newHttpClient();
        this.httpClient = httpClient;
    }

    public boolean isConfigValid() throws IOException, InterruptedException {
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
            put("botId", "123456789"); // TODO: get bot id
        }};

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ApiEndpoints.BASE_URL + ApiEndpoints.EDIT_SETTINGS_URL))
                .header("Authorization", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(map.toString()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 401) {
            IOException e = new IOException(ErrorCodes.INVALID_API_TOKEN);
            e.printStackTrace();
            return false;
        }

        return response.statusCode() == 200;
    }

    public void trackEvents() throws IOException, InterruptedException {
        boolean isConfigValid = isConfigValid();
        if (!isConfigValid) {
            IOException e = new IOException(ErrorCodes.INVALID_CONFIGURATION);
            e.printStackTrace();
            return;
        }
    }
}