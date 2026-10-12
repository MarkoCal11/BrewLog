package hr.tvz.brewlog.brewlogapp.model;


import hr.tvz.brewlog.brewlogapp.enums.ProcessingMethod;
import hr.tvz.brewlog.brewlogapp.enums.RoastLevel;
import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Data
@EqualsAndHashCode(exclude = {"flavorNotes", "user"})
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Slf4j

@Table(name = "coffee_bean")
public class CoffeeBean {

    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String origin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private RoastLevel roastLevel;

    @Enumerated(EnumType.STRING)
    @Column(length = 100)
    private ProcessingMethod processingMethod;

    @Column(nullable = false, length = 100)
    private String roasterName;

    private LocalDate roastDate;

    private Double price;

    private Integer altitude;

    private String tastingNotes;

    @Column(nullable = false)
    private boolean favorite;

    private Integer weightGrams;

    @Column(nullable = false)
    private boolean finished;

    private String photoUrl;

    @ManyToMany
    @JoinTable(name = "coffee_bean_flavor_note",
            joinColumns = @JoinColumn(name = "coffee_bean_id"),
            inverseJoinColumns = @JoinColumn(name = "flavor_note_id"))
    private Set<FlavorNote> flavorNotes =  new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
}