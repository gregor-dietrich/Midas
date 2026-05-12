package de.vptr.midas.gui.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

/** Data transfer object for post comment. */
public class PostCommentDto extends AbstractPostCommentDto {

    @SuppressWarnings({ "NullAway", "PMD.UnnecessaryConstructor" })
    public PostCommentDto() {
        // Default constructor for Jackson
    }
}
