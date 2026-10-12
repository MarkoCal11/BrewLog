package hr.tvz.brewlog.brewlogapp.model;

import hr.tvz.brewlog.brewlogapp.enums.Category;
import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import java.util.HashSet;
import java.util.Set;

@Data
@EqualsAndHashCode(exclude = {"coffeeBean", "user"})
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Slf4j

@Table(name = "flavor_note")
public class FlavorNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    private Category category;

    @ManyToMany(mappedBy = "flavorNotes")
    private Set<CoffeeBean> coffeeBean = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
}