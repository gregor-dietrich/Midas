package de.vptr.midas.gui.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Data transfer object for post category. */
public class PostCategoryDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("parentId")
    private Long parentId;

    @JsonProperty("parent")
    private PostCategoryDto parent;

    @JsonProperty("children")
    private List<PostCategoryDto> children;

    public PostCategoryDto() {
        // Default constructor for Jackson
    }

    /** Creates a new instance with the given parameters. */
    public PostCategoryDto(final Long id, final String name, final PostCategoryDto parent) {
        this.id = id;
        this.name = name;
        this.parent = parent;
        this.parentId = parent != null ? parent.getId() : null;
    }

    // Additional constructor for API usage
    /** Creates a new instance with the given parameters. */
    public PostCategoryDto(final Long id, final String name, final Long parentId) {
        this.id = id;
        this.name = name;
        this.parentId = parentId;
    }

    // Getters and setters
    public Long getId() {
        return this.id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public Long getParentId() {
        return this.parentId;
    }

    public void setParentId(final Long parentId) {
        this.parentId = parentId;
    }

    public PostCategoryDto getParent() {
        return this.parent;
    }

    public void setParent(final PostCategoryDto parent) {
        this.parent = parent;
        this.parentId = parent != null ? parent.getId() : null;
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
