package de.vptr.midas.gui.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response DTO for post category operations.
 * Contains computed fields and safe data for client responses.
 */
public class PostCategoryResponseDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("parentId")
    private Long parentId;

    @JsonProperty("parentName")
    private String parentName;

    @JsonProperty("isRootCategory")
    private boolean isRootCategory;

    @JsonProperty("childrenCount")
    private int childrenCount;

    @JsonProperty("postsCount")
    private int postsCount;

    @JsonProperty("childrenIds")
    private List<Long> childrenIds;

    public PostCategoryResponseDto() {
        // Default constructor for Jackson
    }

    public Long getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public Long getParentId() {
        return this.parentId;
    }

    public String getParentName() {
        return this.parentName;
    }

    public boolean isRootCategory() {
        return this.isRootCategory;
    }

    public int getChildrenCount() {
        return this.childrenCount;
    }

    public int getPostsCount() {
        return this.postsCount;
    }

    public List<Long> getChildrenIds() {
        return this.childrenIds;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setParentId(final Long parentId) {
        this.parentId = parentId;
    }

    public void setParentName(final String parentName) {
        this.parentName = parentName;
    }

    public void setRootCategory(final boolean rootCategory) {
        this.isRootCategory = rootCategory;
    }

    public void setChildrenCount(final int childrenCount) {
        this.childrenCount = childrenCount;
    }

    public void setPostsCount(final int postsCount) {
        this.postsCount = postsCount;
    }

    public void setChildrenIds(final List<Long> childrenIds) {
        this.childrenIds = childrenIds;
    }

    /** Handles the to post category dto operation. */
    public PostCategoryDto toPostCategoryDto() {
        final PostCategoryDto dto = new PostCategoryDto();
        dto.setId(this.id);
        dto.setName(this.name);

        // Create minimal parent if we have parent information
        if (this.parentId != null) {
            final PostCategoryDto parentDto = new PostCategoryDto();
            parentDto.setId(this.parentId);
            parentDto.setName(this.parentName);
            dto.setParent(parentDto);
        }

        return dto;
    }

}
