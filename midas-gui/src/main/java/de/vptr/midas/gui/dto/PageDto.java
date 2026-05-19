package de.vptr.midas.gui.dto;

/** Data transfer object for page. */
public class PageDto extends AbstractTitleContentDto {

    public PageDto() {
        super();
    }

    /** Creates a new instance with the given parameters. */
    public PageDto(final String publicId, final String title, final String content) {
        super(publicId, title, content);
    }
}
