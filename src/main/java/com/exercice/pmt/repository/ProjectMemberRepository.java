package com.exercice.pmt.repository;

import com.exercice.pmt.model.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    boolean existsByProjectIdAndUserId(Integer project_id, Long user_id);
    List<ProjectMember> findByProjectId(Integer projectId);
}
