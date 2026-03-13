package com.exercice.pmt.repository;

import com.exercice.pmt.model.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    boolean existsByProjectIdAndUserId(Integer project_id, Long user_id);
    List<ProjectMember> findByProjectId(Integer projectId);

    Optional<ProjectMember> findByProjectIdAndUserId(Integer projectId, Integer userId);

    boolean existsByProjectIdAndUserId(Integer projectId, Integer userId);

}
