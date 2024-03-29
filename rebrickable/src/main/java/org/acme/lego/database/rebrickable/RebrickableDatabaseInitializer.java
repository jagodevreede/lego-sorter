package org.acme.lego.database.rebrickable;

import io.quarkus.narayana.jta.runtime.TransactionConfiguration;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.acme.lego.database.rebrickable.domain.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@ApplicationScoped
@TransactionConfiguration(timeout = 60 * 60 * 10) // Loading can take a while
public class RebrickableDatabaseInitializer {
    public static final int BATCH_SIZE = 10000;
    private File dbFolder;

    @Inject
    EntityManager entityManager;

    // startup event
    void initialize(final File dbFolder) {
        this.dbFolder = dbFolder;
        new RebrickableDownloader(dbFolder).download();
        val startTime = System.nanoTime();
        loadPartCategories();
        loadParts();
        loadElements();
        loadColors();
        loadInventoryParts();
        loadInventories();
        loadSets();
        loadPartRelationship();
        log.info("Loaded rebrickable database in {}ms", Duration.ofNanos(System.nanoTime() - startTime).toMillis());
    }

    @Transactional
    @SneakyThrows
    void loadInventories() {
        if (Inventories.count() > 0) {
            log.debug("Inventories already loaded");
            return;
        }
        log.info("Loading inventories");
        Iterable<CSVRecord> records = getCsvRecords("inventories.csv");
        for (CSVRecord record : records) {
            val entity = Inventories.builder()
                    .idVersion(IdVersion.builder()
                            .id(Integer.parseInt(record.get("id")))
                            .version(Short.parseShort(record.get("version")))
                            .build())
                    .setNum(record.get("set_num"))
                    .build();
            entity.persist();
        }
    }

    @SneakyThrows
    void loadInventoryParts() {
        if (InventoryParts.count() > 0) {
            log.debug("InventoryParts already loaded");
            return;
        }
        // There are a lot of inventory parts, so we log the progress
        int recordCount = 0;
        log.info("Loading inventory parts");
        Iterable<CSVRecord> records = getCsvRecords("inventory_parts.csv");
        List<InventoryParts> batch = new ArrayList<>();
        for (CSVRecord record : records) {
            val entity = InventoryParts.builder()
                    .inventoryId(Integer.parseInt(record.get("inventory_id")))
                    .partNum(record.get("part_num"))
                    .colorId(Short.parseShort(record.get("color_id")))
                    .quantity(Short.parseShort(record.get("quantity")))
                    .isSpare("t".equalsIgnoreCase(record.get("is_spare")))
                    .build();
            batch.add(entity);
            // log interim progress
            if (++recordCount % BATCH_SIZE == 0) {
                insertBatch(batch);
                log.info("Loaded {} inventory parts", recordCount);
            }
        }
    }

    @Transactional
    void insertBatch(List<InventoryParts> batch) {
        for (InventoryParts inventoryParts : batch) {
            inventoryParts.persist();
        }
        entityManager.flush();
        entityManager.clear();
        batch.clear();
    }

    @Transactional
    @SneakyThrows
    void loadElements() {
        if (Elements.count() > 0) {
            log.debug("Elements already loaded");
            return;
        }
        log.info("Loading elements");
        Iterable<CSVRecord> records = getCsvRecords("elements.csv");
        for (CSVRecord record : records) {
            val entity = Elements.builder()
                    .id(record.get("element_id"))
                    .partNum(record.get("part_num"))
                    .colorId(Short.parseShort(record.get("color_id")))
                    .build();
            entity.persist();
        }
    }

    @Transactional
    @SneakyThrows
    void loadColors() {
        if (Colors.count() > 0) {
            log.debug("Colors already loaded");
            return;
        }
        log.info("Loading colors");
        Iterable<CSVRecord> records = getCsvRecords("colors.csv");
        for (CSVRecord record : records) {
            val entity = Colors.builder()
                    .id(Short.parseShort(record.get("id")))
                    .name(record.get("name"))
                    .rgb(record.get("rgb"))
                    .isTrans("t".equalsIgnoreCase(record.get("is_trans")))
                    .build();
            entity.persist();
        }
    }

    @Transactional
    @SneakyThrows
    void loadPartCategories() {
        if (PartCategories.count() > 0) {
            log.debug("Part categories already loaded");
            return;
        }
        log.info("Loading part categories");
        Iterable<CSVRecord> records = getCsvRecords("part_categories.csv");
        for (CSVRecord record : records) {
            val entity = PartCategories.builder()
                    .id(Short.parseShort(record.get("id")))
                    .name(record.get("name"))
                    .build();
            entity.persist();
        }
    }

    @Transactional
    @SneakyThrows
    void loadParts() {
        if (Parts.count() > 0) {
            log.debug("Parts already loaded");
            return;
        }
        log.info("Loading parts");
        Iterable<CSVRecord> records = getCsvRecords("parts.csv");
        for (CSVRecord record : records) {
            val entity = Parts.builder()
                    .partNum(record.get("part_num"))
                    .partCategoryId(Short.parseShort(record.get("part_cat_id")))
                    .name(record.get("name"))
                    .build();
            entity.persist();
        }
    }

    @Transactional
    @SneakyThrows
    void loadSets() {
        if (Sets.count() > 0) {
            log.debug("Sets already loaded");
            return;
        }
        log.info("Loading sets");
        Iterable<CSVRecord> records = getCsvRecords("sets.csv");
        for (CSVRecord record : records) {
            val entity = Sets.builder()
                    .numParts(Short.parseShort(record.get("num_parts")))
                    .year(Short.parseShort(record.get("year")))
                    .themeId(Short.parseShort(record.get("theme_id")))
                    .setNum(record.get("set_num"))
                    .name(record.get("name"))
                    .build();
            entity.persist();
        }
    }

    @Transactional
    @SneakyThrows
    void loadPartRelationship() {
        if (PartRelationship.count() > 0) {
            log.debug("PartRelationship already loaded");
            return;
        }
        log.info("Loading PartRelationship");
        Iterable<CSVRecord> records = getCsvRecords("part_relationships.csv");
        for (CSVRecord record : records) {
            val entity = PartRelationship.builder()
                    .relType(record.get("rel_type"))
                    .childPartNum(record.get("child_part_num"))
                    .parentPartNum(record.get("parent_part_num"))
                    .build();
            entity.persist();
        }
    }

    private Iterable<CSVRecord> getCsvRecords(String csvFile) throws IOException {
        Reader in = new FileReader(dbFolder.getAbsolutePath() + "/" + csvFile);
        return CSVFormat.RFC4180.builder().setHeader()
                .setSkipHeaderRecord(true)
                .build().parse(in);
    }

}
