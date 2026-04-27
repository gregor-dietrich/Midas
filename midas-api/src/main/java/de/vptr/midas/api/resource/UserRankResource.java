package de.vptr.midas.api.resource;

import java.util.List;

import de.vptr.midas.api.dto.UserRankDto;
import de.vptr.midas.api.dto.UserRankResponseDto;
import de.vptr.midas.api.service.UserRankService;
import de.vptr.midas.api.util.ResponseUtil;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
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
public class UserRankResource {

    @Inject
    UserRankService rankService;

    @GET
    public List<UserRankResponseDto> getAllRanks() {
        return this.rankService.getAllRanks();
    }

    @GET
    @Path("/{id}")
    public Response getRank(@PathParam("id") @Min(1) final Long id) {
        return ResponseUtil.okOrNotFound(
                this.rankService.findById(id));
    }

    @GET
    @Path("/name/{name}")
    public Response getRankByName(@PathParam("name") @Size(max = 255) final String name) {
        return ResponseUtil.okOrNotFound(
                this.rankService.findByName(name));
    }

    /** Returns all ranks matching the search query. */
    @GET
    @Path("/search")
    public List<UserRankResponseDto> searchRanks(@QueryParam("query") final String query) {
        if (query == null || query.trim().isEmpty()) {
            return this.getAllRanks();
        }
        return this.rankService.searchRanks(query);
    }

    @POST
    @RolesAllowed({ "user-rank:add" })
    public Response createRank(@Valid final UserRankDto rankDto) {
        final UserRankResponseDto created = this.rankService.createRank(rankDto);
        return ResponseUtil.created(created);
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({ "user-rank:edit" })
    public Response updateRank(@PathParam("id") @Min(1) final Long id, @Valid final UserRankDto rankDto) {
        final UserRankResponseDto updated = this.rankService.updateRank(id, rankDto);
        return ResponseUtil.ok(updated);
    }

    @PATCH
    @Path("/{id}")
    @RolesAllowed({ "user-rank:edit" })
    public Response patchRank(@PathParam("id") @Min(1) final Long id, @Valid final UserRankDto rankDto) {
        final UserRankResponseDto updated = this.rankService.patchRank(id, rankDto);
        return ResponseUtil.ok(updated);
    }

    /** Deletes the rank with the given id. */
    @DELETE
    @Path("/{id}")
    @RolesAllowed({ "user-rank:delete" })
    public Response deleteRank(@PathParam("id") @Min(1) final Long id) {
        final boolean deleted = this.rankService.deleteRank(id);
        if (deleted) {
            return ResponseUtil.noContent();
        }
        return ResponseUtil.notFound();
    }
}
