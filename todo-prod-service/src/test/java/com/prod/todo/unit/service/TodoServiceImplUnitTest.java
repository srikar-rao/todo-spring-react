package com.prod.todo.unit.service;

import com.prod.todo.entity.TodoEntity;
import com.prod.todo.entity.TodoLogEntity;
import com.prod.todo.mapper.TodoMapper;
import com.prod.todo.model.ResponseStatus;
import com.prod.todo.model.Todo;
import com.prod.todo.repository.TodoLogRepository;
import com.prod.todo.repository.TodoRepository;
import com.prod.todo.service.impl.TodoServiceImpl;
import org.instancio.Instancio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TodoServiceImplUnitTest {

    @Mock
    private TodoRepository todoRepository;

    @Mock
    private TodoLogRepository todoLogRepository;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private TodoMapper todoMapper;

    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private TodoServiceImpl todoService;

    @Test
    @DisplayName("Should return list of Todos when repository returns data")
    void getAllTodos_ShouldReturnMappedList() {
        TodoEntity entity = Instancio.of(TodoEntity.class).create();
        Todo expectedTodo = Instancio.of(Todo.class).create();

        when(todoRepository.findAllWithTasks()).thenReturn(List.of(entity));
        when(todoMapper.toModelListWithLocale(List.of(entity))).thenReturn(List.of(expectedTodo));

        List<Todo> result = todoService.getAllTodos();

        assertThat(result).containsExactly(expectedTodo);
        verify(todoRepository).findAllWithTasks();
    }

    @Test
    @DisplayName("Should return mapped Todo when entity is found by ID")
    void getTodoById_ShouldReturnMappedTodo() {
        Long id = 1L;
        TodoEntity entity = new TodoEntity();
        Todo expectedTodo = new Todo();

        when(todoRepository.findByIdWithTasks(id)).thenReturn(Optional.of(entity));
        when(modelMapper.map(entity, Todo.class)).thenReturn(expectedTodo);

        Todo result = todoService.getTodoById(id);

        assertThat(result).isEqualTo(expectedTodo);
        verify(todoRepository).findByIdWithTasks(id);
    }

    @Test
    @DisplayName("Should save and return new Todo")
    void saveTodo_ShouldSaveAndReturnMappedTodo() {
        Todo input = new Todo();
        TodoEntity entityToSave = new TodoEntity();
        TodoEntity savedEntity = new TodoEntity();
        Todo expected = new Todo();

        when(todoMapper.toNewEntity(input)).thenReturn(entityToSave);
        when(todoRepository.save(entityToSave)).thenReturn(savedEntity);
        when(todoLogRepository.save(any())).thenReturn(Instancio.of(TodoLogEntity.class).create());
        when(todoMapper.toModelWithLocale(savedEntity)).thenReturn(expected);

        Todo result = todoService.saveTodo(any(), input);

        assertThat(result).isEqualTo(expected);
        verify(todoRepository).save(entityToSave);
        verify(todoLogRepository).save(any());
    }

    @Test
    @DisplayName("Should delete Todo successfully and return success response")
    void deleteTodo_WhenSuccess_ShouldReturnResponseStatus() {
        Long id = 1L;
        TransactionStatus mockStatus = mock(TransactionStatus.class);

        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(mockStatus);
        });

        ResponseStatus result = todoService.deleteTodo(id);

        assertThat(result).isNotNull();
        assertThat(result.getMessage()).isEqualTo("Successfully deleted todo of id " + id);
        verify(todoRepository).deleteById(id);
        verify(mockStatus, never()).setRollbackOnly();
    }

    @Test
    @DisplayName("Should rollback and throw exception when repository fails")
    void deleteTodo_WhenRepositoryFails_ShouldRollbackAndThrowException() {
        Long id = 1L;
        TransactionStatus mockStatus = mock(TransactionStatus.class);

        doThrow(new RuntimeException("Database error"))
                .when(todoRepository).deleteById(id);

        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(mockStatus);
        });

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            todoService.deleteTodo(id);
        });

        assertThat(exception.getMessage()).contains("Database error");
        verify(todoRepository).deleteById(id);
        verify(mockStatus).setRollbackOnly();
    }
}
