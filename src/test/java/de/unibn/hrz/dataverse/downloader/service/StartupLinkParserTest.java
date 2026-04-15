package de.unibn.hrz.dataverse.downloader.service;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class StartupLinkParserTest {

    @Test
    void returnsNullForNullArgs() {
        assertNull(StartupLinkParser.parseStartupInput(null));
    }

    @Test
    void returnsPlainUrlUnchanged() {
        String url = "https://dataverse.harvard.edu/dataset.xhtml?persistentId=doi:10.7910/DVN/TEST";
        assertEquals(url, StartupLinkParser.parseStartupInput(new String[]{url}));
    }

    @Test
    void decodesCustomUrlScheme() {
        String arg = "hvdvdl://open?url=https%3A%2F%2Fexample.org%2Fdataset.xhtml%3FpersistentId%3Ddoi%3A10.1%2FABC";
        assertEquals(
            "https://example.org/dataset.xhtml?persistentId=doi:10.1/ABC",
            StartupLinkParser.parseStartupInput(new String[]{arg})
        );
    }

    @Test
    void decodesCustomDoiScheme() {
        String arg = "hvdvdl://open?doi=doi%3A10.7910%2FDVN%2F12345";
        assertEquals(
            "doi:10.7910/DVN/12345",
            StartupLinkParser.parseStartupInput(new String[]{arg})
        );
    }

    @Test
    void ignoresBlankArguments() {
        assertEquals(
            "doi:10.7910/DVN/12345",
            StartupLinkParser.parseStartupInput(new String[]{"   ", "doi:10.7910/DVN/12345"})
        );
    }

    @Test
    void returnsNullForMalformedCustomScheme() {
        assertNull(StartupLinkParser.parseStartupInput(new String[]{"hvdvdl://open?url=%ZZ"}));
    }
}