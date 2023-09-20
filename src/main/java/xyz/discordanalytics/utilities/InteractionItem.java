package xyz.discordanalytics.utilities;

public class InteractionItem {
    public final String name;
    public final int type;
    public int number;

    public InteractionItem(String name, int type, int number) {
        this.name = name;
        this.type = type;
        this.number = number;
    }

    @Override
    public String toString() {
        return "{name: \"" + name + "\", type: " + type + ", number: " + number + "}";
    }
}
