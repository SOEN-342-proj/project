package model;

/** Represents a tag that can be applied to tasks. */
public class Tag {
    private String tagName;

    public Tag(String tagName) {
        this.tagName = tagName;
    }

    public String getTagName() { return tagName; }
    public void setTagName(String tagName) { this.tagName = tagName; }

    @Override
    public String toString() { return tagName; }
}