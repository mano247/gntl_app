package com.gentlemanstore.support.repository;

import com.gentlemanstore.support.model.BotQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BotQuestionRepository extends JpaRepository<BotQuestion, Long> {
    List<BotQuestion> findAllByDeletedFalseOrderByOrderIndexAsc();
}
