package xyz.discordanalytics.utilities;

public class LocalesItems {
    public final String locale;
    public int number;

    public LocalesItems(String locale, int number) {
        this.locale = locale;
        this.number = number;
    }

    @Override
    public String toString() {
        return "{locale: \"" + locale + "\", number: " + number + "}";
    }
}
