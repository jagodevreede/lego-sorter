package org.acme.lego.database.rebrickable;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.stream.Collectors;

@Transactional
@Produces(MediaType.APPLICATION_JSON)
@Path("/rebrickableDatabase")
public class RebrickableResource {

    @Inject
    RebrickableDatabase rebrickableDatabase;

    @GET
    @Path("popular-parts")
    public List<String> popularPars() {
        return rebrickableDatabase.getPopularParts();
    }

    @GET
    @Path("partcolor/{part}")
    @Produces(MediaType.TEXT_PLAIN)
    public String getColorsForPart(@PathParam("part") String part) {
        return "brick_colors=(" +
                rebrickableDatabase.getColorsForPart(part).stream()
                .sorted()
                .map(c -> "\"lg_" + c.toLowerCase().replace(' ', '_') + "\"")
                .collect(Collectors.joining(" ")) +
                ")";
    }

}