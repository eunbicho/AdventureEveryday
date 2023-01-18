package com.ssafy.antenna.domain.adventure;

import com.ssafy.antenna.domain.Base;
import com.ssafy.antenna.domain.badge.Badge;
import com.ssafy.antenna.domain.category.Category;
import com.ssafy.antenna.domain.user.Follow;
import com.ssafy.antenna.domain.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class Adventure extends Base {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long adventureId;

    @ManyToOne
    @JoinColumn(name = "userId")
    private User user;
    @ManyToOne
    @JoinColumn(name = "categoryId")
    private Category category;
    @ManyToOne
    @JoinColumn(name = "badgeId")
    private Badge badge;

    @Column(columnDefinition = "varchar(255) not null")
    private String feat;

    @Column(columnDefinition = "double not null")
    private String avgReviewRate;

    @Column(columnDefinition = "varchar(255) not null")
    private String title;

    @Column(columnDefinition = "varchar(255) default null")
    private String content;

    @Column(columnDefinition = "int not null")
    private int difficulty;

    @Lob
    @Column(columnDefinition = "blob default null")
    private byte[] photo;


    @Column(columnDefinition = "int default 6")
    private int validDate;
    @Column(columnDefinition = "datetime(6) default null")
    private LocalDateTime endDate;

    @OneToMany(mappedBy = "adventure", cascade = CascadeType.ALL)
    private List<AdventurePlace> adventurePlaces = new ArrayList<>();


}
