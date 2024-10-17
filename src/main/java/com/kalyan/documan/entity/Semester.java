/* Copyright (C)2024 Mudumby Kalyan / @theinhumaneme  */
package com.kalyan.documan.entity;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
    name = "semester",
    indexes = {
      @Index(name = "idx_semester_name", columnList = "name", unique = true),
      @Index(name = "idx_semester_year_id", columnList = "year_id")
    })
@Getter
@Setter
@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "id")
public class Semester {

  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @NotNull
  @Column(name = "name", nullable = false, unique = true)
  private String name;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "year_id", nullable = false)
  private Year year;

  @OneToMany(mappedBy = "semester", fetch = FetchType.LAZY)
  private ArrayList<Subject> subjects;
}
