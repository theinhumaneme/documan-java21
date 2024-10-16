package com.kalyan.documan.entitiy;


import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;

@Entity
@Table(name = "year")
@Getter
@Setter
@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "id")
public class Year {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Column(name = "year", nullable = false)
    private String year;

    @OneToMany(mappedBy = "year", fetch = FetchType.LAZY)
    private ArrayList<Semester> semesters;

    @OneToMany(mappedBy = "year", fetch = FetchType.LAZY)
    private ArrayList<Subject> subjects;


}
