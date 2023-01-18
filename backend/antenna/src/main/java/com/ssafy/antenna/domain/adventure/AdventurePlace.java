package com.ssafy.antenna.domain.adventure;

import com.ssafy.antenna.domain.Base;
import com.ssafy.antenna.domain.category.Category;
import com.ssafy.antenna.domain.post.CheckpointPost;
import com.ssafy.antenna.domain.user.Checkpoint;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.geo.Point;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class AdventurePlace  extends Base {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long adventurePlaceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "adventureId")
    private Adventure adventure;

    @Column(columnDefinition = "Point not null")
    private Point coordinate;

    @Column(columnDefinition = "varchar(255) not null")
    private String title;

    @Column(columnDefinition = "varchar(255) default null")
    private String content;

    @OneToMany(mappedBy = "adventurePlace", cascade = CascadeType.ALL)
    private List<Checkpoint> checkpoints = new ArrayList<>();

    @OneToMany(mappedBy = "adventurePlace",cascade = CascadeType.ALL)
    private List<CheckpointPost> checkpointPosts = new ArrayList<>();
}
