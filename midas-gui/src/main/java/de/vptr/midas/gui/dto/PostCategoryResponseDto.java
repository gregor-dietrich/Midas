package de.vptr.midas.gui.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Response DTO for post category operations. Contains computed fields and safe data for client responses.
 */
@SuppressFBWarnings(value = "EI_EXPOSE_REP",
        justification = "DTO used only within GUI; not serialized across trust boundaries")
public class PostCategoryResponseDto {

    @JsonProperty("publicId")
    private String publicId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("parentPublicId")
    private String parentPublicId;

    @JsonProperty("parentName")
    private String parentName;

    @JsonProperty("isRootCategory")
    private boolean isRootCategory;

    @JsonProperty("childrenCount")
    private int childrenCount;

    @JsonProperty("postsCount")
    private int postsCount;

    @JsonProperty("childrenPublicIds")
    private List<String> childrenPublicIds;

    @SuppressWarnings({ "NullAway", "PMD.UnnecessaryConstructor" })
    public PostCategoryResponseDto() {
        // Default constructor for Jackson
    }

    public String getPublicId() {
        return this.publicId;
    }

    public String getName() {
        return this.name;
    }

    public String getParentPublicId() {
        return this.parentPublicId;
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

    public List<String> getChildrenPublicIds() {
        return this.childrenPublicIds;
    }

    public void setPublicId(final String publicId) {
        this.publicId = publicId;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setParentPublicId(final String parentPublicId) {
        this.parentPublicId = parentPublicId;
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

    public void setChildrenPublicIds(final List<String> childrenPublicIds) {
        this.childrenPublicIds = childrenPublicIds;
    }

    /** Handles the to post category dto operation. */
    public PostCategoryDto toPostCategoryDto() {
        final PostCategoryDto dto = new PostCategoryDto();
        dto.setPublicId(this.publicId);
        dto.setName(this.name);

        // Create minimal parent if we have parent information
        if (this.parentPublicId != null) {
            final PostCategoryDto parentDto = new PostCategoryDto();
            parentDto.setPublicId(this.parentPublicId);
            parentDto.setName(this.parentName);
            dto.setParent(parentDto);
        }

        return dto;
    }

}
