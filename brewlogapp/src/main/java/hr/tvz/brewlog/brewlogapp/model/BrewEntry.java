package hr.tvz.brewlog.brewlogapp.model;

import hr.tvz.brewlog.brewlogapp.enums.Method;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Slf4j

@Table(name = "brew_entry")
public class BrewEntry {

    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    private Method method;

    private Double grindSize;

    @Column(name = "water_temp_c", length = 100)
    private Double waterTempC;

    @Column(length = 100)
    private Double rating;

    @Column(nullable = false, length = 100)
    private LocalDateTime brewedAt;

    @Column(length = 100)
    private Double brewTimeSeconds;

    @Column(nullable = false, length = 100)
    private Double doseGrams;

    @Column(length = 100)
    private Double waterGrams;

    @Column(length = 100)
    private Double yieldGrams;

    @Column(length = 100)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bean_id", nullable = false)
    private CoffeeBean coffeeBean;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
}