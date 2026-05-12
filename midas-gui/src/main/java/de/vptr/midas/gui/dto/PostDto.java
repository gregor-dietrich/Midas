package de.vptr.midas.gui.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

/** Data transfer object for post. */
public class PostDto extends AbstractPostDto {

    @SuppressWarnings({ "NullAway", "PMD.UnnecessaryConstructor" })
    public PostDto() {
        // Default constructor for Jackson
    }
}
