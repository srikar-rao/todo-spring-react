package com.prod.todo.repository;

import com.prod.todo.annotation.ReadOnlyQuery;
import com.prod.todo.entity.TodoEntity;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TodoRepository extends JpaRepository<TodoEntity, Long> {

    @Query("SELECT t FROM TodoEntity t LEFT JOIN FETCH t.tasks ORDER BY t.createdAt DESC")
    @ReadOnlyQuery
    List<TodoEntity> findAllWithTasks();

    @Query("SELECT t FROM TodoEntity t LEFT JOIN FETCH t.tasks WHERE t.userId = :userId")
    List<TodoEntity> findByUserId(String userId);


    @EntityGraph(attributePaths = {"tasks"})
    @ReadOnlyQuery
    @Query("SELECT t FROM TodoEntity t")
    List<TodoEntity> findAllWithTasksUsingGraph();

    @Query("SELECT t FROM TodoEntity t LEFT JOIN FETCH t.tasks WHERE t.id =:id")
    Optional<TodoEntity> findByIdWithTasks(@Param("id") Long id);


    /**
     * Fetches the entity using a pessimistic locks on the parent row.
     * Only the TodoEntity row is locked — child TaskEntity rows are not locked and must be explicitly locked via TaskRepository if needed.
     */
    // Shared lock — prevents updates
    @Lock(LockModeType.PESSIMISTIC_READ)
    @QueryHints({
            @QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000"),  // in milliseconds
            @QueryHint(name = "org.hibernate.readOnly", value = "true")
    })
    @Query("SELECT t FROM TodoEntity t WHERE t.id = :id")
    Optional<TodoEntity> findByIdForReadLock(@Param("id") Long id);

    // Exclusive lock — prevents reads/writes
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({
            @QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000")
    })
    @Query("SELECT t FROM TodoEntity t WHERE t.id = :id")
    Optional<TodoEntity> findByIdForWriteLock(@Param("id") Long id);

    // Exclusive + version increment (rare use-case)
    @Lock(LockModeType.PESSIMISTIC_FORCE_INCREMENT)
    @QueryHints({
            @QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000")
    })
    @Query("SELECT t FROM TodoEntity t WHERE t.id = :id")
    Optional<TodoEntity> findByIdWithForceVersion(@Param("id") Long id);


}
