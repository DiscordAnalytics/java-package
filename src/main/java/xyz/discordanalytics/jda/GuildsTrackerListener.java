package xyz.discordanalytics.jda;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import xyz.discordanalytics.JDAAnalytics;
import xyz.discordanalytics.utilities.ApiEndpoints;
import xyz.discordanalytics.utilities.EventsTracker;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;

public class GuildsTrackerListener extends ListenerAdapter {
    private final JDA client;
    private Date precedentPostDate;
    private final JDAAnalytics analytics;

    public GuildsTrackerListener(JDAAnalytics analytics) {
        this.client = analytics.getClient();
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
            post(client.getUsers().size(), client.getGuilds().size());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        precedentPostDate = new Date();
    }

    private void post(int users, int guilds) throws IOException, InterruptedException {
        EventsTracker eventsToTrack = analytics.getEventsToTrack();
        String[] date = new Date().toString().split(" ");

        try {
            HttpResponse<String> response = analytics.post(ApiEndpoints.ROUTES.GUILDS, new HashMap<>() {{
                put("users", eventsToTrack.trackUserCount ? users : null);
                put("guilds", eventsToTrack.trackGuilds ? guilds : null);
                put("date", date[5] + "-" + monthToNumber(date[1]) + "-" + date[2]);
            }});

            if (response.statusCode() != 200) {
                Object guildsData = analytics.getDataNotSent().get("guilds");
                guildsData = guildsData == null ? new HashMap<>() : guildsData;
                ((HashMap<String, Object>) guildsData).put("users", users);
                ((HashMap<String, Object>) guildsData).put("guilds", guilds);
                ((HashMap<String, Object>) guildsData).put("date", date[5] + "-" + monthToNumber(date[1]) + "-" + date[2]);

                analytics.putToDataNotSent("guilds", guildsData);
            }
            if (response.statusCode() == 200 && analytics.getDataNotSent().get("guilds") != null) {
                analytics.sendDataNotSent();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String monthToNumber(String month) {
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        int monthIndex = Arrays.asList(months).indexOf(month);
        return (monthIndex < 9 ? "0" : "") + (monthIndex + 1);
    }
}
