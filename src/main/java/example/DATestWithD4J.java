package example;

import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.object.entity.User;
import fr.valdesign.D4JAnalytics;
import fr.valdesign.utilities.EventsTracker;
import reactor.core.publisher.Mono;

import java.io.IOException;

public class DATestWithD4J {
    public static void main(String[] args) {
        // Create a Discord client
        // Don't forget to replace YOUR_BOT_TOKEN by your Discord bot token !
        DiscordClient client = DiscordClient.create("YOUR_BOT_TOKEN");

        Mono<Void> login = client.withGateway((GatewayDiscordClient gateway) ->
            // Handle the ReadyEvent
            gateway.on(ReadyEvent.class, event ->
                Mono.fromRunnable(() -> {
                    final User self = event.getSelf();
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
                    D4JAnalytics analytics = new D4JAnalytics(client, eventsTracker, "YOUR_API_KEY");
                    // Start the tracking in a new thread
                    // The tracking will be done every 10 minutes to avoid spamming the API (10 minutes is the minimum)
                    new Thread(() -> {
                        while (true) {
                            try {
                                analytics.trackEvents();
                                Thread.sleep(600000);
                            } catch (IOException | InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                })));

        // Login the client
        login.block();
    }
}
