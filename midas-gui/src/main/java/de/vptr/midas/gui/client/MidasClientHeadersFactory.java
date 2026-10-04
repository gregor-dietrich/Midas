package de.vptr.midas.gui.client;

import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

import de.vptr.midas.gui.service.AuthService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedMap;

/**
 * Supplies the backend credentials for every generated REST client call.
 *
 * <p>
 * The hand-written clients took the Authorization header as an explicit method parameter, which meant every service
 * method had to fetch and thread it. Generated clients take their parameters from the contract, which does not describe
 * that header, so it is injected here instead. Callers that are not authenticated still fail before any request is
 * sent, because {@link AuthService#requireAuthorizationHeader()} throws.
 */
@ApplicationScoped
public class MidasClientHeadersFactory implements ClientHeadersFactory {

    @Inject
    AuthService authService;

    @Override
    public MultivaluedMap<String, String> update(final MultivaluedMap<String, String> incomingHeaders,
            final MultivaluedMap<String, String> outgoingHeaders) {
        outgoingHeaders.putSingle(HttpHeaders.AUTHORIZATION, this.authService.requireAuthorizationHeader());
        return outgoingHeaders;
    }
}
