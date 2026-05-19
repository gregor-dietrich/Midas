package de.vptr.midas.gui.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.annotation.Nullable;

/** Data transfer object for post category. */
@SuppressFBWarnings(value = "EI_EXPOSE_REP",
        justification = "DTO used only within GUI; not serialized across trust boundaries")
public class PostCategoryDto {

    @JsonProperty("publicId")
    @Nullable
    private String publicId;

    @JsonProperty("name")
    @Nullable
    private String name;

    @JsonProperty("parentPublicId")
    @Nullable
    private String parentPublicId;

    @JsonProperty("parent")
    @Nullable
    private PostCategoryDto parent;

    @JsonProperty("children")
    @Nullable
    private List<PostCategoryDto> children;

    public PostCategoryDto() {
        // Default constructor for Jackson
    }

    /** Creates a new instance with the given parameters. */
    public PostCategoryDto(final String publicId, final String name, @Nullable final PostCategoryDto parent) {
        this.publicId = publicId;
        this.name = name;
        this.parent = parent;
        this.parentPublicId = parent != null ? parent.getPublicId() : null;
    }

    // Additional constructor for API usage
    /** Creates a new instance with the given parameters. */
    public PostCategoryDto(final String publicId, final String name, @Nullable final String parentPublicId) {
        this.publicId = publicId;
        this.name = name;
        this.parentPublicId = parentPublicId;
    }

    // Getters and setters
    @Nullable
    public String getPublicId() {
        return this.publicId;
    }

    public void setPublicId(@Nullable final String publicId) {
        this.publicId = publicId;
    }

    @Nullable
    public String getName() {
        return this.name;
    }

    public void setName(@Nullable final String name) {
        this.name = name;
    }

    @Nullable
    public String getParentPublicId() {
        return this.parentPublicId;
    }

    public void setParentPublicId(@Nullable final String parentPublicId) {
        this.parentPublicId = parentPublicId;
    }

    @Nullable
    public PostCategoryDto getParent() {
        return this.parent;
    }

    public void setParent(@Nullable final PostCategoryDto parent) {
        this.parent = parent;
        this.parentPublicId = parent != null ? parent.getPublicId() : null;
    }

    @Nullable
    public List<PostCategoryDto> getChildren() {
        return this.children;
    }

    public void setChildren(@Nullable final List<PostCategoryDto> children) {
        this.children = children;
    }

    public boolean isRootCategory() {
        return this.parent == null;
    }

}
