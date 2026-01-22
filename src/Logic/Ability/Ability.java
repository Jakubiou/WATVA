package Logic.Ability;

/**
 * Represents a character ability with name, description and type.
 */
public class Ability {
    private String name;
    private String description;
    private String type;

    /**
     * Creates a new Ability instance.
     * @param name The name of the ability
     * @param description Description of what the ability does
     * @param type The type/category of the ability
     */
    public Ability(String name, String description, String type) {
        this.name = name;
        this.description = description;
        this.type = type;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getType() { return type; }
}