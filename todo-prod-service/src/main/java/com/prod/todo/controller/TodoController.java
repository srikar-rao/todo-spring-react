package com.prod.todo.controller;

import com.prod.todo.model.ResponseStatus;
import com.prod.todo.model.Todo;
import com.prod.todo.service.TodoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/todo")
public class TodoController {

    private final TodoService todoService;

    @Operation(
            summary = "Fetch all todos",
            description = "Retrieves a list of all existing todo items."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list of todos",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Todo.class)))
    })
    @GetMapping(value = "/all", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Todo> getAllTodos() {
        return todoService.getAllTodos();
    }

    @Operation(
            summary = "Fetch all todos based on userId",
            description = "Retrieves a list of all existing todo items based on userId."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list of todos",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Todo.class))),
            @ApiResponse(responseCode = "404", description = "Todo not found",
                    content = @Content)
    })

    @GetMapping(value = "/user/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Todo> getAllTodosByUserId(@PathVariable String userId) {
        return todoService.getAllTodosByUserId(userId);
    }

    @Operation(
            summary = "Get todo by ID",
            description = "Retrieves a single todo item based on its unique ID."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Todo found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Todo.class))),
            @ApiResponse(responseCode = "404", description = "Todo not found",
                    content = @Content)
    })
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Todo getTodoById(@PathVariable Long id) {
        return todoService.getTodoById(id);
    }


    @Operation(
            summary = "Create new todo",
            description = "Creates a new todo item with the provided details."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Todo successfully created",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Todo.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content)
    })
    @PostMapping(
            value = "/save/{userId}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Todo createTodo(@PathVariable String userId, @RequestBody Todo todo) {
        return todoService.saveTodo(userId, todo);
    }

    @Operation(
            summary = "Update todo",
            description = "Update a todo item with the provided details."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Todo successfully updated",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Todo.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content)
    })
    @PutMapping(
            value = "/update",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Todo updateTodo(@RequestBody Todo todo) {
        return todoService.update(todo);
    }


    @Operation(
            summary = "Delete todo by ID",
            description = "Deletes a specific todo item using its ID."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Todo successfully deleted",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ResponseStatus.class))),
            @ApiResponse(responseCode = "404", description = "Todo not found", content = @Content)
    })
    @DeleteMapping(
            value = "/delete/{id}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseStatus createTodo(@PathVariable Long id) {
        return todoService.deleteTodo(id);
    }

}
