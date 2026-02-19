package Tutorial;

public class TutorialStep {
    public enum StepType {
        INFO,
        MOVE,
        SHOOT,
        DASH,
        EXPLOSION,
        KILL_ENEMIES,
        COMPLETE
    }

    private String title;
    private String description;
    private StepType type;

    public TutorialStep(String title, String description, StepType type) {
        this.title = title;
        this.description = description;
        this.type = type;
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public StepType getType() { return type; }
}