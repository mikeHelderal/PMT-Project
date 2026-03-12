package com.exercice.pmt.DTO;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ProjectMemberResponse {

    private Integer id;
    private Long userId;
    private String username;
    private String email;
    private String role;
    private LocalDate dateArrivee;


    public ProjectMemberResponse(Integer id, Long userId, String username, String email, String role, LocalDate dateArrivee){
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.role = role;
        this.dateArrivee = dateArrivee;
    }

}
