package example.jda;

import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.object.entity.User;
import xyz.discordanalytics.JDAAnalytics;
import xyz.discordanalytics.utilities.EventsTracker;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class ReadyListener implements EventListener {
    @Override
    public void onEvent(@NotNull GenericEvent genericEvent) {
        if (genericEvent instanceof ReadyEvent) {
            final User self = ((ReadyEvent) genericEvent).getSelf();
            System.out.printf("Logged in as %s#%s%n", self.getUsername(), self.getDiscriminator());

            // Initialize what you want to track
            EventsTracker eventsTracker = new EventsTracker();
            eventsTracker.trackInteractions = true;
            eventsTracker.trackGuilds = true;
            eventsTracker.trackUserCount = true;
            eventsTracker.trackUserLanguage = true;
            eventsTracker.trackGuildsLocale = true;

            // Initialize the DiscordAnalytics class
            // Don't forget to replace YOUR_API_TOKEN by your Discord Analytics token !
            JDAAnalytics analytics = new JDAAnalytics(genericEvent.getJDA(), eventsTracker, "YOUR_API_KEY");
            // Start the tracking, it will be done every 10 minutes to avoid spamming the API
            try {
                analytics.trackEvents();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
