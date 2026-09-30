package com.taskflow.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** 任务 CRUD 与探针端到端测试（H2 内存库）。 */
@SpringBootTest
@AutoConfigureMockMvc
class TaskFlowApiTests {

    @Autowired
    private MockMvc mvc;

    private MvcResult create(String body) throws Exception {
        return mvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
    }

    @Test
    void healthzReturnsOk() throws Exception {
        mvc.perform(get("/healthz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.version").exists());
    }

    @Test
    void readyzChecksDatabase() throws Exception {
        mvc.perform(get("/readyz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ready"));
    }

    @Test
    void createAndReadTask() throws Exception {
        MvcResult created = create("""
                {"title":"verify-crud","description":"smoke","priority":1}
                """);
        String body = created.getResponse().getContentAsString();
        assertThat(body).contains("\"title\":\"verify-crud\"");
        assertThat(body).contains("\"done\":false");

        String id = com.jayway.jsonpath.JsonPath.read(body, "$.id").toString();
        mvc.perform(get("/tasks/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priority").value(1));
    }

    @Test
    void createValidationRejectsBadInput() throws Exception {
        mvc.perform(post("/tasks").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\"}"))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/tasks").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"x\",\"priority\":9}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listSupportsFilters() throws Exception {
        create("{\"title\":\"task-alpha\"}");
        create("{\"title\":\"task-beta\"}");

        mvc.perform(get("/tasks").param("q", "task-alpha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("task-alpha"));

        mvc.perform(get("/tasks").param("done", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].done", everyItem(is(true))));
    }

    @Test
    void patchUpdatesFields() throws Exception {
        MvcResult created = create("{\"title\":\"to-update\"}");
        String body = created.getResponse().getContentAsString();
        String id = com.jayway.jsonpath.JsonPath.read(body, "$.id").toString();

        mvc.perform(patch("/tasks/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"done\":true,\"priority\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.done").value(true))
                .andExpect(jsonPath("$.priority").value(3));
    }

    @Test
    void getMissingTaskReturns404() throws Exception {
        mvc.perform(get("/tasks/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteRemovesTask() throws Exception {
        MvcResult created = create("{\"title\":\"to-delete\"}");
        String body = created.getResponse().getContentAsString();
        String id = com.jayway.jsonpath.JsonPath.read(body, "$.id").toString();

        mvc.perform(delete("/tasks/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/tasks/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void statsSumsToTotal() throws Exception {
        MvcResult created = create("{\"title\":\"stats-task\"}");
        String body = created.getResponse().getContentAsString();
        String id = com.jayway.jsonpath.JsonPath.read(body, "$.id").toString();
        mvc.perform(patch("/tasks/" + id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"done\":true}")).andExpect(status().isOk());

        String stats = mvc.perform(get("/tasks/stats")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        int total = com.jayway.jsonpath.JsonPath.read(stats, "$.total");
        int done = com.jayway.jsonpath.JsonPath.read(stats, "$.done");
        int todo = com.jayway.jsonpath.JsonPath.read(stats, "$.todo");
        assertThat(total).isEqualTo(done + todo);
    }
}
