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
import static org.mockito.ArgumentMatchers.*;
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
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .writeValueAsString(obj);
    }

    private Todo generateTodo() {
        return Instancio.of(Todo.class)
                .generate(field(Todo::getId), gen -> gen.longSeq().start(1L))
                .create();
    }

    private List<Todo> generateTodoList(int size) {
        return Instancio.ofList(Todo.class).size(size).create();
    }


    private List<Todo> generateTodoListWithUserId(int size, String userId) {
        return Instancio.ofList(Todo.class).size(size)
                .supply(field(Todo::getUserId), () -> userId)
                .supply(field(Todo::getId), () -> 1L)
                .supply(field(Todo::getTitle), () -> "Task Title")
                .supply(field(Todo::isCompleted), () -> false)
                .create();
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
    void getTodoByUserId_WhenExists_ShouldReturnTodos() throws Exception {
        // Prepare single userId and test data
        String userId = UUID.randomUUID().toString();

        List<Todo> todos = generateTodoListWithUserId(3, userId);

        given(todoService.getAllTodosByUserId(userId)).willReturn(todos);

        mockMvc.perform(get("/todo/user/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].userId").value(userId));
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

        given(todoService.saveTodo(anyString(), any())).willReturn(todo);

        mockMvc.perform(post("/todo/save/123")
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