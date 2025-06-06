package com.prod.todo.service.impl;

import com.prod.todo.entity.TodoEntity;
import com.prod.todo.entity.TodoLogEntity;
import com.prod.todo.mapper.TodoMapper;
import com.prod.todo.model.ResponseStatus;
import com.prod.todo.model.Todo;
import com.prod.todo.repository.TodoLogRepository;
import com.prod.todo.repository.TodoRepository;
import com.prod.todo.service.TodoService;
import com.prod.todo.util.JsonUtil;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class TodoServiceImpl implements TodoService {

    private final TodoRepository todoRepository;
    private final TodoLogRepository todoLogRepository;
    private final ModelMapper modelMapper;
    private final TransactionTemplate transactionTemplate;
    private final TodoMapper todoMapper;

    @Override
    public List<Todo> getAllTodos() {
        return todoMapper.toModelListWithLocale(todoRepository.findAllWithTasks());
    }

    @Override
    public List<Todo> getAllTodosByUserId(String userId) {
        return todoMapper.toModelListWithLocale(todoRepository.findByUserId(userId));
    }

    @Override
    public Todo getTodoById(Long id) {
        TodoEntity todoEntity = todoRepository.findByIdWithTasks(id).orElse(new TodoEntity());
        return modelMapper.map(todoEntity, Todo.class);
    }

    //POSTGRES - defaults (READ_COMMITTED and REQUIRED)
    @Override
    @Transactional(
            isolation = Isolation.SERIALIZABLE,
            propagation = Propagation.REQUIRES_NEW,
            timeout = 10000
    )
    public Todo saveTodo(String userId, Todo todo) {

        TodoEntity newEntity = todoMapper.toNewEntity(todo);
        newEntity.setUserId(userId);

        TodoEntity savedTodo = todoRepository.save(newEntity);

        todoLogRepository.save(
                TodoLogEntity
                        .builder()
                        .todoId(savedTodo.getId())
                        .snapshot(JsonUtil.toJsonNode(savedTodo))
                        .build()
        );
        log.info("Successfully saved todo : {}", savedTodo);
        return todoMapper.toModelWithLocale(savedTodo);
    }

    @Override
    public ResponseStatus deleteTodo(Long id) {

        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
        transactionTemplate.setName("deleteTodoTransaction");

        return transactionTemplate.execute(status -> {
            try {
                todoRepository.deleteById(id);
                return ResponseStatus.builder()
                        .isSuccess(Boolean.TRUE)
                        .message("Successfully deleted todo of id " + id)
                        .build();
            } catch (Exception ex) {
                status.setRollbackOnly();
                throw ex;
            }
        });
    }

    @Override
    @Retryable(
            retryFor = {OptimisticLockException.class, CannotAcquireLockException.class, PessimisticLockingFailureException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000)
    )
    @Transactional(
            isolation = Isolation.SERIALIZABLE,
            propagation = Propagation.REQUIRES_NEW,
            timeout = 10000
    )
    public Todo update(Todo todo) {
        Optional<TodoEntity> optEntity = todoRepository.findById(todo.getId());
        if (optEntity.isPresent()) {
            TodoEntity todoEntity = optEntity.get();
            todoEntity.setTitle(todo.getTitle());
            todoEntity.setDescription(todo.getDescription());
            todoEntity.setStatus(todo.getStatus());
            todoEntity.setCompleted(todo.isCompleted());
            TodoEntity saved = todoRepository.saveAndFlush(todoEntity);
            return todoMapper.toModelWithLocale(saved);
        } else return new Todo();
    }


}
