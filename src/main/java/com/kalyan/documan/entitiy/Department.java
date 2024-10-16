package com.kalyan.documan.entitiy;


import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;

@Entity
@Table(name = "department")
@Getter
@Setter
@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "id")
public class Department {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Column(name = "department", nullable = false)
    private String department;

    @OneToMany(mappedBy = "department", fetch = FetchType.LAZY)
    private ArrayList<Subject> subjects;

    @OneToMany(mappedBy = "department", fetch = FetchType.LAZY)
    private ArrayList<User> users;
}
