package xyz.discordanalytics;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.SelfUser;
import xyz.discordanalytics.jda.GuildsTrackerListener;
import xyz.discordanalytics.jda.InteractionTrackerListener;
import xyz.discordanalytics.utilities.ApiEndpoints;
import xyz.discordanalytics.utilities.ErrorCodes;
import xyz.discordanalytics.utilities.EventsTracker;
import xyz.discordanalytics.utilities.LibType;

import java.io.IOException;

public class JDAAnalytics extends AnalyticsBase {
    private final JDA client;

    public JDAAnalytics(JDA jda, EventsTracker eventsToTrack, String apiKey) {
        super(eventsToTrack, apiKey);
        this.client = jda;
        this.baseAPIUrl = ApiEndpoints.BASE_URL + ApiEndpoints.TRACK_URL.replace("[id]", client.getSelfUser().getId());
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

        if (isConfigInvalid(userClient.getId(), LibType.JDA)) {
            new IOException(ErrorCodes.INVALID_CONFIGURATION).printStackTrace();
            return;
        }

        if (eventsToTrack.trackInteractions) client.addEventListener(new InteractionTrackerListener(this));
        if (eventsToTrack.trackGuilds) client.addEventListener(new GuildsTrackerListener(this));
    }
}
