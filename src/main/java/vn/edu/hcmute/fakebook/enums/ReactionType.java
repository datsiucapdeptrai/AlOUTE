package vn.edu.hcmute.fakebook.enums;

public enum ReactionType {
    LIKE("👍", "Thích", "#0d6efd"),
    LOVE("❤️", "Yêu thích", "#dc3545"),
    HAHA("😆", "Haha", "#ffc107"),
    WOW("😮", "Wow", "#ffc107"),
    SAD("😢", "Buồn", "#ffc107"),
    ANGRY("😡", "Phẫn nộ", "#dc3545");

    private final String emoji;
    private final String label;
    private final String color;

    ReactionType(String emoji, String label, String color) {
        this.emoji = emoji;
        this.label = label;
        this.color = color;
    }

    public String getEmoji() { return emoji; }
    public String getLabel() { return label; }
    public String getColor() { return color; }
}