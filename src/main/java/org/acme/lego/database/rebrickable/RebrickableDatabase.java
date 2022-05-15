package org.acme.lego.database.rebrickable;

import io.quarkus.runtime.StartupEvent;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.io.File;
import java.util.List;

@Slf4j
@ApplicationScoped
public class RebrickableDatabase {
    private final File dbFolder = new File("./rebrickableDb");

    @Inject
    RebrickableDatabaseInitializer databaseInitializer;

    @Inject
    EntityManager entityManager;

    // startup event
    void onStart(@Observes StartupEvent ev) {
        new RebrickableDownloader(dbFolder).download();
        databaseInitializer.initialize(dbFolder);
    }

    @SuppressWarnings("unchecked")
    public List<String> getPopularParts() {
        return entityManager.createNativeQuery("""
                select s.partnum from (select partnum, sum(quantity) as total, count(inventoryid) as sets from inventoryparts i
                group by partnum) as s
                where s.total > 280
                order by s.sets desc
                """, String.class)
                .setMaxResults(1000)
                .getResultList();
    }
}
