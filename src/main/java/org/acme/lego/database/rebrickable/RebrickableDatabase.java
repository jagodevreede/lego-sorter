package org.acme.lego.database.rebrickable;

import io.quarkus.runtime.StartupEvent;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.io.File;

@Slf4j
@ApplicationScoped
public class RebrickableDatabase {
    private final File dbFolder = new File("./rebrickableDb");

    @Inject
    RebrickableDatabaseInitializer databaseInitializer;

    // startup event
    void onStart(@Observes StartupEvent ev) {
        new RebrickableDownloader(dbFolder).download();
        databaseInitializer.initialize(dbFolder);
    }
}
