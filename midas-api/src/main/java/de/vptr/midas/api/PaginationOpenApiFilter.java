package de.vptr.midas.api;

import java.math.BigDecimal;

import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;

import de.vptr.midas.api.util.PaginationUtil;

/**
 * Documents the bounds {@link PaginationUtil} clamps the {@code page}, {@code size} and {@code limit} query parameters
 * to, so the contract states the page-size maximum instead of every resource repeating the annotations.
 */
public class PaginationOpenApiFilter implements OASFilter {

    @Override
    public Parameter filterParameter(final Parameter parameter) {
        final Schema schema = parameter.getSchema();
        if (parameter.getIn() != Parameter.In.QUERY || schema == null) {
            return parameter;
        }
        switch (parameter.getName()) {
            case "page" -> schema.minimum(BigDecimal.valueOf(PaginationUtil.DEFAULT_PAGE));
            case "size", "limit" -> schema.minimum(BigDecimal.ONE).maximum(BigDecimal.valueOf(PaginationUtil.MAX_SIZE));
            default -> {
                // not a pagination parameter
            }
        }
        return parameter;
    }
}
