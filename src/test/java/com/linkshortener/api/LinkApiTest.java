package com.linkshortener.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Tests the whole HTTP layer: routes, status codes, JSON and redirects.
 * Each test uses its own alias, because all tests share one running application.
 */
@SpringBootTest
@AutoConfigureMockMvc
class LinkApiTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void healthIsUp() throws Exception {
        mvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void createReturns201WithShortUrl() throws Exception {
        mvc.perform(post("/api/v1/links").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\": \"https://example.com/create-test\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.code").isNotEmpty())
                .andExpect(jsonPath("$.shortUrl").isNotEmpty())
                .andExpect(jsonPath("$.targetUrl").value("https://example.com/create-test"));
    }

    @Test
    void customAliasRedirectsAndCountsClicks() throws Exception {
        mvc.perform(post("/api/v1/links").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\": \"https://docs.oracle.com/en/java/\", \"alias\": \"api-test-docs\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("api-test-docs"));

        mvc.perform(get("/api-test-docs"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://docs.oracle.com/en/java/"))
                .andExpect(header().string("Cache-Control", "no-store"));

        mvc.perform(get("/api/v1/links/api-test-docs/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalClicks").value(1));
    }

    @Test
    void duplicateAliasIs409() throws Exception {
        String body = "{\"url\": \"https://example.com/\", \"alias\": \"conflict-test\"}";
        mvc.perform(post("/api/v1/links").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/links").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("alias is already in use"));
    }

    @Test
    void invalidInputIs400() throws Exception {
        mvc.perform(post("/api/v1/links").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\": \"ftp://example.com/\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
        mvc.perform(post("/api/v1/links").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\": \"http://localhost:9000/x\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/links").contentType(MediaType.APPLICATION_JSON)
                        .content("not json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownCodeIs404() throws Exception {
        mvc.perform(get("/no-such-code-xyz"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("no such link"));
    }

    @Test
    void disabledLinkIsGoneAndDeleteReturns204() throws Exception {
        mvc.perform(post("/api/v1/links").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\": \"https://example.com/off\", \"alias\": \"disable-test\"}"))
                .andExpect(status().isCreated());

        mvc.perform(delete("/api/v1/links/disable-test")).andExpect(status().isNoContent());
        mvc.perform(get("/disable-test")).andExpect(status().isGone());
    }

    @Test
    void testPageIsAtUrlshortnerAndNotAtRoot() throws Exception {
        mvc.perform(get("/urlshortner"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Link Shortener")));
        mvc.perform(get("/")).andExpect(status().isNotFound());
    }
}
