package com.fineasy.external.dart;

import com.fineasy.entity.DartCorpCodeEntity;
import com.fineasy.repository.DartCorpCodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

@Service
@ConditionalOnExpression("!'${dart.api.key:}'.isEmpty()")
public class DartCorpCodeSyncService {

    private static final Logger log = LoggerFactory.getLogger(DartCorpCodeSyncService.class);

    private final DartApiClient dartApiClient;
    private final DartCorpCodeRepository corpCodeRepository;

    public DartCorpCodeSyncService(DartApiClient dartApiClient,
                                    DartCorpCodeRepository corpCodeRepository) {
        this.dartApiClient = dartApiClient;
        this.corpCodeRepository = corpCodeRepository;
    }

    @PostConstruct
    public void syncOnStartup() {
        long existingCount = corpCodeRepository.count();
        if (existingCount > 0) {
            log.info("DART corp codes already in DB ({} records), skipping sync.", existingCount);
            return;
        }

        log.info("Starting DART corp code sync...");
        try {
            syncCorpCodes();
        } catch (Exception e) {
            log.warn("DART corp code sync failed (non-fatal): {}", e.getMessage());
        }
    }

    @Transactional
    public void syncCorpCodes() {
        byte[] zipBytes = dartApiClient.downloadCorpCodeZip();
        if (zipBytes == null) {
            log.warn("DART corpCode.xml ZIP download returned null, aborting sync.");
            return;
        }

        byte[] xmlBytes = extractXmlFromZip(zipBytes);
        if (xmlBytes == null) {
            log.warn("Failed to extract XML from DART corpCode ZIP, aborting sync.");
            return;
        }

        List<DartCorpCodeEntity> entities = parseCorpCodeXml(xmlBytes);
        if (entities.isEmpty()) {
            log.warn("No listed companies found in DART corpCode.xml");
            return;
        }

        Set<String> existingStockCodes = corpCodeRepository.findAll().stream()
                .map(DartCorpCodeEntity::getStockCode)
                .collect(Collectors.toSet());

        List<DartCorpCodeEntity> newEntities = entities.stream()
                .filter(e -> !existingStockCodes.contains(e.getStockCode()))
                .toList();

        if (!newEntities.isEmpty()) {
            corpCodeRepository.saveAll(newEntities);
        }

        log.info("DART corp code sync completed. Total listed: {}, newly saved: {}",
                entities.size(), newEntities.size());
    }

    private byte[] extractXmlFromZip(byte[] zipBytes) {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().toLowerCase().endsWith(".xml")) {
                    byte[] xmlBytes = zis.readAllBytes();
                    log.debug("Extracted {} from ZIP ({} bytes)", entry.getName(), xmlBytes.length);
                    return xmlBytes;
                }
            }
            log.warn("No XML file found in DART corpCode ZIP");
            return null;
        } catch (Exception e) {
            log.error("Failed to extract XML from ZIP: {}", e.getMessage());
            return null;
        }
    }

    private List<DartCorpCodeEntity> parseCorpCodeXml(byte[] xmlBytes) {
        List<DartCorpCodeEntity> entities = new ArrayList<>();

        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            SAXParser parser = factory.newSAXParser();

            parser.parse(new ByteArrayInputStream(xmlBytes), new DefaultHandler() {
                private final StringBuilder chars = new StringBuilder();
                private String corpCode;
                private String corpName;
                private String stockCode;
                private int totalParsed = 0;
                private boolean inList = false;

                @Override
                public void startElement(String uri, String localName, String qName, Attributes attributes) {
                    chars.setLength(0);
                    if ("list".equals(qName)) {
                        inList = true;
                        corpCode = null;
                        corpName = null;
                        stockCode = null;
                    }
                }

                @Override
                public void characters(char[] ch, int start, int length) {
                    if (inList) {
                        chars.append(ch, start, length);
                    }
                }

                @Override
                public void endElement(String uri, String localName, String qName) {
                    if (!inList) return;

                    String text = chars.toString().trim();
                    switch (qName) {
                        case "corp_code" -> corpCode = text;
                        case "corp_name" -> corpName = text;
                        case "stock_code" -> stockCode = text;
                        case "list" -> {
                            totalParsed++;
                            inList = false;
                            if (stockCode != null && !stockCode.isBlank()) {
                                entities.add(new DartCorpCodeEntity(corpCode, corpName, stockCode.trim()));
                            }
                        }
                    }
                }

                @Override
                public void endDocument() {
                    log.info("Parsed {} total entries from corpCode.xml, {} are listed companies",
                            totalParsed, entities.size());
                }
            });

        } catch (Exception e) {
            log.error("Failed to parse corpCode.xml: {}", e.getMessage());
        }

        return entities;
    }
}
