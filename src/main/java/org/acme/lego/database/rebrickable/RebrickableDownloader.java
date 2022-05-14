package org.acme.lego.database.rebrickable;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.zip.GZIPInputStream;

@Slf4j
@RequiredArgsConstructor
class RebrickableDownloader {
    private static final String REBRICKABLE_DB_CDN_URL = "https://cdn.rebrickable.com/media/downloads/";
    static final List<String> REBRICKABLE_DB_FILES = List.of("colors.csv", "inventories.csv", "inventory_parts.csv", "minifigs.csv", "part_relationships.csv", "sets.csv",
            "elements.csv", "inventory_minifigs.csv", "inventory_sets.csv", "part_categories.csv" ,"parts.csv", "themes.csv");

    private final File dbFolder;

    private void extractCsvFiles() {
        REBRICKABLE_DB_FILES.forEach(file -> {
            File dbFileGz = new File(dbFolder, file + ".gz");
            File dbFile = new File(dbFolder, file);
            if (!dbFile.exists()) {
                log.info("Extracting file {}", file);
                decompressGzip(dbFileGz, dbFile);
            }
        });
    }

    private void downloadCompressedCsvFiles() {
        REBRICKABLE_DB_FILES.forEach(file -> {
            File dbFile = new File(dbFolder, file +".gz");
            if (!dbFile.exists()) {
                log.info("Downloading file {}", file);
                try {
                    downloadUrlToFile(new URL(REBRICKABLE_DB_CDN_URL + file), dbFile);
                } catch (MalformedURLException e) {
                    log.error("Error creating url", e);
                }
            }
        });
    }

    private void downloadUrlToFile(URL url, File file) {
        try {
            InputStream in = url.openStream();
            Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Error downloading file at {}", url, e);
        }
    }

    private void decompressGzip(File source, File target) {
        try (GZIPInputStream gis = new GZIPInputStream(
                new FileInputStream(source))) {
            Files.copy(gis, target.toPath());
        } catch (IOException e) {
            log.error("Error decompressing file", e);
        }
    }

    public void download() {
        dbFolder.mkdirs();
        downloadCompressedCsvFiles();
        extractCsvFiles();
    }
}
