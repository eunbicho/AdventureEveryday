package com.ssafy.antenna.repository;

import com.ssafy.antenna.domain.adventure.Adventure;
import com.ssafy.antenna.domain.adventure.AdventureReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AdventureReviewRepository extends JpaRepository<AdventureReview, Long> {
    Optional<List<AdventureReview>> findAllByAdventure(Adventure adventure);

    Optional<Long> countAdventureReviewByAdventure(Adventure adventure);

    @Query(
            "select sum(ar.grade) "
                    + "from AdventureReview ar "
                    + "where ar.adventure.adventureId=:adventureId")
    Optional<Double> sumOfAdventureReviews(@Param("adventureId") Long adventureId);
}
