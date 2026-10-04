package de.vptr.midas.api.resource;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponseSchema;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import de.vptr.midas.api.dto.UserRankDto;
import de.vptr.midas.api.dto.UserRankResponseDto;
import de.vptr.midas.api.exception.ApiErrorResponse;
import de.vptr.midas.api.service.UserRankService;
import de.vptr.midas.api.util.ResponseUtil;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
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

/** REST resource for managing user ranks. */
@Path("/user-ranks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
@Tag(name = "UserRank")
public class UserRankResource {

    @Inject
    UserRankService rankService;

    // No "user-rank:view" role exists in the current security model; use "user-rank:edit"
    // as the closest privilege that grants read access to rank definitions.
    @GET
    @RolesAllowed({ "user-rank:edit", "user-rank:delete" })
    public List<UserRankResponseDto> getAllRanks() {
        return this.rankService.getAllRanks();
    }

    @GET
    @Path("/{publicId}")
    @RolesAllowed({ "user-rank:edit", "user-rank:delete" })
    @APIResponseSchema(value = UserRankResponseDto.class, responseDescription = "Rank found")
    @APIResponse(responseCode = "404", description = "Rank not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public Response getRank(@PathParam("publicId") final String publicId) {
        return ResponseUtil.okOrNotFound(this.rankService.findById(publicId));
    }

    @GET
    @Path("/name/{name}")
    @RolesAllowed({ "user-rank:edit", "user-rank:delete" })
    @APIResponseSchema(value = UserRankResponseDto.class, responseDescription = "Rank found")
    @APIResponse(responseCode = "404", description = "Rank not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "422", description = "Name longer than 255 characters",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public Response getRankByName(@PathParam("name") @Size(max = 255) final String name) {
        return ResponseUtil.okOrNotFound(this.rankService.findByName(name));
    }

    /** Returns all ranks matching the search query. */
    @GET
    @Path("/search")
    @RolesAllowed({ "user-rank:edit", "user-rank:delete" })
    public List<UserRankResponseDto> searchRanks(@QueryParam("query") final String query) {
        if (query == null || query.trim().isEmpty()) {
            return this.getAllRanks();
        }
        return this.rankService.searchRanks(query);
    }

    @POST
    @RolesAllowed({ "user-rank:add" })
    @APIResponseSchema(value = UserRankResponseDto.class, responseCode = "201", responseDescription = "Rank created")
    @APIResponse(responseCode = "422", description = "Validation failed",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public Response createRank(@Valid final UserRankDto rankDto) {
        final UserRankResponseDto created = this.rankService.createRank(rankDto);
        return ResponseUtil.created(created);
    }

    @PUT
    @Path("/{publicId}")
    @RolesAllowed({ "user-rank:edit" })
    @APIResponseSchema(value = UserRankResponseDto.class, responseDescription = "Rank replaced")
    @APIResponse(responseCode = "404", description = "Rank not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "422", description = "Validation failed",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public Response updateRank(@PathParam("publicId") final String publicId, @Valid final UserRankDto rankDto) {
        final UserRankResponseDto updated = this.rankService.updateRank(publicId, rankDto);
        return ResponseUtil.ok(updated);
    }

    @PATCH
    @Path("/{publicId}")
    @RolesAllowed({ "user-rank:edit" })
    @APIResponseSchema(value = UserRankResponseDto.class, responseDescription = "Rank updated")
    @APIResponse(responseCode = "404", description = "Rank not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "422", description = "Validation failed",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public Response patchRank(@PathParam("publicId") final String publicId, final UserRankDto rankDto) {
        final UserRankResponseDto updated = this.rankService.patchRank(publicId, rankDto);
        return ResponseUtil.ok(updated);
    }

    /** Deletes the rank with the given publicId. */
    @DELETE
    @Path("/{publicId}")
    @RolesAllowed({ "user-rank:delete" })
    @APIResponse(responseCode = "204", description = "Rank deleted")
    @APIResponse(responseCode = "404", description = "Rank not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public Response deleteRank(@PathParam("publicId") final String publicId) {
        final boolean deleted = this.rankService.deleteRank(publicId);
        if (deleted) {
            return ResponseUtil.noContent();
        }
        return ResponseUtil.notFound();
    }
}
