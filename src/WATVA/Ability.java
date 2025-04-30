package WATVA;

public class Ability {
    private String name;
    private String description;
    private int cost;
    private String type;
    private int maxLevel;

    public Ability(String name, String description, int cost, String type, int maxLevel) {
        this.name = name;
        this.description = description;
        this.cost = cost;
        this.type = type;
        this.maxLevel = maxLevel;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getCost() { return cost; }
    public String getType() { return type; }
    public int getMaxLevel() { return maxLevel; }
}