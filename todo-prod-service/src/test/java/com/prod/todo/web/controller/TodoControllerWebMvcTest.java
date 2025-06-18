package com.prod.todo.web.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.prod.todo.controller.TodoController;
import com.prod.todo.model.ResponseStatus;
import com.prod.todo.model.Todo;
import com.prod.todo.service.TodoService;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.instancio.Select.field;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TodoController.class)
@AutoConfigureMockMvc(addFilters = false)
class TodoControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TodoService todoService;

    private static String asJsonString(Object obj) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper.writeValueAsString(obj);
    }

    private Todo generateTodo() {
        return Instancio.of(Todo.class)
                .generate(field(Todo::getId), gen -> gen.longSeq().start(1L))
                .create();
    }

    private List<Todo> generateTodoList(int size) {
        return Instancio.ofList(Todo.class).size(size).create();
    }

    @Test
    void getAllTodos_ShouldReturnAllTodos() throws Exception {
        List<Todo> todos = generateTodoList(3);
        given(todoService.getAllTodos()).willReturn(todos);

        mockMvc.perform(get("/todo/all"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].id").exists());
    }

    @Test
    void getTodoById_WhenExists_ShouldReturnTodo() throws Exception {
        Todo todo = generateTodo();
        given(todoService.getTodoById(anyLong())).willReturn(todo);

        mockMvc.perform(get("/todo/{id}", todo.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(todo.getId()));
    }

    @Test
    void createTodo_WithValidData_ShouldReturnCreated() throws Exception {
        Todo todo = Instancio.of(Todo.class)
                .ignore(field(Todo::getCreatedAt))
                .ignore(field(Todo::getUpdatedAt))
                .generate(field(Todo::getId), gen -> gen.longSeq().start(1L))
                .create();

        given(todoService.saveTodo(any(), any())).willReturn(todo);

        String userId = UUID.randomUUID().toString();

        mockMvc.perform(post("/todo/save/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(todo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void deleteTodo_WhenExists_ShouldReturnSuccess() throws Exception {
        ResponseStatus success = ResponseStatus
                .builder()
                .message("Deleted successfully")
                .build();
        given(todoService.deleteTodo(anyLong())).willReturn(success);

        mockMvc.perform(delete("/todo/delete/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Deleted successfully"));
    }
}