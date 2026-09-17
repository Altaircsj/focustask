package br.edu.ufersa.pw.focustask.features.project;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "projects")
class Project { // Visibilidade default

   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private Long id;

   // Substituição da Entidade User pelo seu ID (desacoplamento total)
   @Column(name = "user_id", nullable = false)
   private Long userId;

   @NotBlank
   @Size(max = 255)
   @Column(nullable = false)
   private String name;

   @Column(columnDefinition = "TEXT")
   private String description;

   protected Project() {
   }

   public Project(Long userId, String name, String description) {
      this.userId = userId;
      this.name = name;
      this.description = description;
   }

   public Long getId() { return id; }
   public void setId(Long id) { this.id = id; }

   public Long getUserId() { return userId; }
   public void setUserId(Long userId) { this.userId = userId; }

   public String getName() { return name; }
   public void setName(String name) { this.name = name; }

   public String getDescription() { return description; }
   public void setDescription(String description) { this.description = description; }
}