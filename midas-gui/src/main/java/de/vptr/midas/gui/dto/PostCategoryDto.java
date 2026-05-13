package de.vptr.midas.gui.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/** Data transfer object for post category. */
@SuppressFBWarnings(value = "EI_EXPOSE_REP",
        justification = "DTO used only within GUI; not serialized across trust boundaries")
public class PostCategoryDto {

    @JsonProperty("publicId")
    private String publicId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("parentPublicId")
    private String parentPublicId;

    @JsonProperty("parent")
    private PostCategoryDto parent;

    @JsonProperty("children")
    private List<PostCategoryDto> children;

    @SuppressWarnings("NullAway")
    public PostCategoryDto() {
        // Default constructor for Jackson
    }

    /** Creates a new instance with the given parameters. */
    @SuppressWarnings("NullAway")
    public PostCategoryDto(final String publicId, final String name, final PostCategoryDto parent) {
        this.publicId = publicId;
        this.name = name;
        this.parent = parent;
        this.parentPublicId = parent != null ? parent.getPublicId() : null;
    }

    // Additional constructor for API usage
    /** Creates a new instance with the given parameters. */
    @SuppressWarnings("NullAway")
    public PostCategoryDto(final String publicId, final String name, final String parentPublicId) {
        this.publicId = publicId;
        this.name = name;
        this.parentPublicId = parentPublicId;
    }

    // Getters and setters
    public String getPublicId() {
        return this.publicId;
    }

    public void setPublicId(final String publicId) {
        this.publicId = publicId;
    }

    public String getName() {
        return this.name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getParentPublicId() {
        return this.parentPublicId;
    }

    public void setParentPublicId(final String parentPublicId) {
        this.parentPublicId = parentPublicId;
    }

    public PostCategoryDto getParent() {
        return this.parent;
    }

    @SuppressWarnings("NullAway")
    public void setParent(final PostCategoryDto parent) {
        this.parent = parent;
        this.parentPublicId = parent != null ? parent.getPublicId() : null;
    }

    public List<PostCategoryDto> getChildren() {
        return this.children;
    }

    public void setChildren(final List<PostCategoryDto> children) {
        this.children = children;
    }

    public boolean isRootCategory() {
        return this.parent == null;
    }

}
