package org.acme.lego.database.rebrickable;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;

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
                        select s.partnum from (
                             select i.partnum, sum(quantity) as total, count(inventoryid) as sets from inventoryparts i
                             left join inventories iv on i.inventoryId = iv.id
                             left join sets s on iv.setNum = s.setNum
                             left join parts p on i.partnum = p.partnum
                             where (s.year > 1985 OR s.year IS NULL)
                             and i.isspare <> true
                             and p.partcategoryid NOT IN (3, 17, 24, 27, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65)
                             group by i.partnum
                        ) s
                        left join partrelationship pr on partnum = pr.childpartnum
                        where s.total > 50
                        and (pr.reltype is null)
                        order by s.sets desc
                        """)
                .setMaxResults(1000)
                .getResultStream()
                .map(o -> o.toString()).toList();
    }

    public List<String> getColorsForPart(String part) {
        return entityManager.createNativeQuery("""
                select distinct(c.name) from inventoryparts i
                right join colors c on i.colorId = c.id
                where partnum = :part
               """)
                .setParameter("part", part)
                // Stupid hack to get nice string list, as datatype string is not known by hibernate
                .getResultStream()
                .map(o -> o.toString()).toList();
    }
}
