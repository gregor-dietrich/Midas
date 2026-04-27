package de.vptr.midas.api.resource;

import java.util.List;

import de.vptr.midas.api.dto.PageDto;
import de.vptr.midas.api.dto.PageResponseDto;
import de.vptr.midas.api.service.PageService;
import de.vptr.midas.api.util.PaginationUtil;
import de.vptr.midas.api.util.ResponseUtil;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/** REST resource for managing pages. */
@Path("/pages")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
public class PageResource {

    @Inject
    PageService pageService;

    /** Returns all pages with normalized pagination parameters. */
    @GET
    @PermitAll
    public List<PageResponseDto> getAllPages(
            @QueryParam("page") @DefaultValue("0") final int page,
            @QueryParam("size") @DefaultValue("20") final int size) {
        final int safePage = PaginationUtil.sanitizePage(page);
        final int safeSize = PaginationUtil.sanitizeSize(size);
        return this.pageService.getAllPages(safePage, safeSize);
    }

    @GET
    @Path("/{id}")
    @PermitAll
    public Response getPage(@PathParam("id") @Min(1) final Long id) {
        return ResponseUtil.okOrNotFound(this.pageService.findById(id));
    }

    /** Returns all pages matching the search query. */
    @GET
    @Path("/search")
    @PermitAll
    public List<PageResponseDto> searchPages(@QueryParam("query") final String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return this.pageService.getAllPages(PaginationUtil.DEFAULT_PAGE, PaginationUtil.MAX_SIZE);
        }
        return this.pageService.searchPages(searchTerm);
    }

    @POST
    @RolesAllowed({ "page:add" })
    public Response createPage(@Valid final PageDto pageDto) {
        final PageResponseDto created = this.pageService.createPage(pageDto);
        return ResponseUtil.created(created);
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({ "page:edit" })
    public Response updatePage(@PathParam("id") @Min(1) final Long id, @Valid final PageDto pageDto) {
        final PageResponseDto updated = this.pageService.updatePage(id, pageDto);
        return ResponseUtil.ok(updated);
    }

    @PATCH
    @Path("/{id}")
    @RolesAllowed({ "page:edit" })
    public Response patchPage(@PathParam("id") @Min(1) final Long id, @Valid final PageDto pageDto) {
        final PageResponseDto updated = this.pageService.patchPage(id, pageDto);
        return ResponseUtil.ok(updated);
    }

    /** Deletes the page with the given id. */
    @DELETE
    @Path("/{id}")
    @RolesAllowed({ "page:delete" })
    public Response deletePage(@PathParam("id") @Min(1) final Long id) {
        final boolean deleted = this.pageService.deletePage(id);
        if (deleted) {
            return ResponseUtil.noContent();
        }
        return ResponseUtil.notFound();
    }
}
