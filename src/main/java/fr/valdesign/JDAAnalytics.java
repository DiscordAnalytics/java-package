package fr.valdesign;

import fr.valdesign.jda.GuildsTrackerListener;
import fr.valdesign.jda.InteractionTrackerListener;
import fr.valdesign.utilities.ApiEndpoints;
import fr.valdesign.utilities.ErrorCodes;
import fr.valdesign.utilities.EventsTracker;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.SelfUser;

import java.io.IOException;

public class JDAAnalytics extends AnalyticsBase {
    private final JDA client;

    public JDAAnalytics(JDA jda, EventsTracker eventsToTrack, String apiKey) {
        super(eventsToTrack, apiKey);
        this.client = jda;
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

        if (isConfigInvalid(userClient.getId())) {
            new IOException(ErrorCodes.INVALID_CONFIGURATION).printStackTrace();
            return;
        }

        String baseAPIUrl = ApiEndpoints.BASE_URL + ApiEndpoints.TRACK_URL.replace("[id]", userClient.getId());

        if (eventsToTrack.trackInteractions) client.addEventListener(new InteractionTrackerListener(client, baseAPIUrl, apiKey, this));
        if (eventsToTrack.trackGuilds) client.addEventListener(new GuildsTrackerListener(client, baseAPIUrl, apiKey, this));
    }
}
