package de.vptr.midas.api.filter;

import java.io.IOException;

import de.vptr.midas.api.service.AdminAccessService;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

/** Clears per-request admin access cache after each response. */
@Provider
public class AdminAccessCacheClearingFilter implements ContainerResponseFilter {

    @Inject
    AdminAccessService adminAccessService;

    @Override
    public void filter(final ContainerRequestContext requestContext, final ContainerResponseContext responseContext)
            throws IOException {
        this.adminAccessService.clearCache();
    }
}
