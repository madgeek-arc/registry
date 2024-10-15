package gr.uoa.di.madgik.registry.annotation;


import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Parameters(value = {
        @Parameter(in = ParameterIn.QUERY, name = "keyword", description = "Keyword to refine the search", content = @Content(schema = @Schema(type = "string"))),
        @Parameter(in = ParameterIn.QUERY, name = "from", description = "Starting index in the result set", content = @Content(schema = @Schema(type = "string", defaultValue = "0"))),
        @Parameter(in = ParameterIn.QUERY, name = "quantity", description = "Quantity to be fetched", content = @Content(schema = @Schema(type = "string", defaultValue = "10"))),
        @Parameter(in = ParameterIn.QUERY, name = "sort", description = "Field to use for sorting", content = @Content(schema = @Schema(type = "string"))),
        @Parameter(in = ParameterIn.QUERY, name = "order", description = "Order of results", content = @Content(schema = @Schema(type = "string", defaultValue = "asc", allowableValues = {"asc", "desc"})))
})
public @interface BrowseParameters {
}