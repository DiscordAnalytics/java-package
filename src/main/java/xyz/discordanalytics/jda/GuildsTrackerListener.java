package xyz.discordanalytics.jda;

import xyz.discordanalytics.JDAAnalytics;
import xyz.discordanalytics.utilities.ApiEndpoints;
import xyz.discordanalytics.utilities.EventsTracker;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;

public class GuildsTrackerListener extends ListenerAdapter {
    private final JDA client;
    private final String baseAPIUrl;
    private final String apiKey;
    private Date precedentPostDate;
    private final JDAAnalytics analytics;

    public GuildsTrackerListener(JDA client, String baseAPIUrl, String apiKey, JDAAnalytics analytics) {
        this.client = client;
        this.baseAPIUrl = baseAPIUrl;
        this.apiKey = apiKey;
        this.precedentPostDate = new Date();
        this.analytics = analytics;
    }

    private boolean isOnCooldown() {
        return new Date().getTime() - precedentPostDate.getTime() < 600000;
    }

    @Override
    public void onGuildJoin(@NotNull GuildJoinEvent event) {
        guildEvent();
    }
    @Override
    public void onGuildLeave(@NotNull GuildLeaveEvent event) {
        guildEvent();
    }

    public void guildEvent() {
        if (isOnCooldown()) return;

        try {
            post(baseAPIUrl + ApiEndpoints.ROUTES.INTERACTIONS, apiKey,
                    client.getUsers().size(),
                    client.getGuilds().size()
            );
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        precedentPostDate = new Date();
    }

    private void post(String apiUrl, String apiKey, int users, int guilds) throws IOException, InterruptedException {
        EventsTracker eventsToTrack = analytics.getEventsToTrack();
        String[] date = new Date().toString().split(" ");

        try {
            analytics.post(apiUrl, apiKey, new HashMap<>() {{
                put("users", eventsToTrack.trackUserCount ? users : null);
                put("guilds", eventsToTrack.trackGuilds ? guilds : null);
                put("date", date[5] + "-" + monthToNumber(date[1]) + "-" + date[2]);
            }});
        } catch (IOException e) {
            e.printStackTrace();

            analytics.putToDataNotSent("guilds", new HashMap<>() {{
                put("users", eventsToTrack.trackUserCount ? users : null);
                put("guilds", eventsToTrack.trackGuilds ? guilds : null);
                put("date", date[5] + "-" + monthToNumber(date[1]) + "-" + date[2]);
            }});
        }
    }

    private static String monthToNumber(String month) {
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        int monthIndex = Arrays.asList(months).indexOf(month);
        return (monthIndex < 9 ? "0" : "") + (monthIndex + 1);
    }
}
