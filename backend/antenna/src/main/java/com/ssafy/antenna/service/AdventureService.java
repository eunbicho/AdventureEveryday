package com.ssafy.antenna.service;

import com.ssafy.antenna.domain.adventure.*;
import com.ssafy.antenna.domain.adventure.dto.*;
import com.ssafy.antenna.domain.like.AdventureLike;
import com.ssafy.antenna.domain.location.Location;
import com.ssafy.antenna.domain.user.User;
import com.ssafy.antenna.repository.*;
import com.ssafy.antenna.util.CardinalDirection;
import com.ssafy.antenna.util.GeometryUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdventureService {
    private final EntityManager entityManager;
    private final AdventureRepository adventureRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final AdventureSucceedRepository adventureSucceedRepository;
    private final AdventurePlaceRepository adventurePlaceRepository;
    private final AdventureReviewRepository adventureReviewRepository;
    private final AdventureLikeRepository adventureLikeRepository;
    private final AdventureInProgressRepository adventureInProgressRepository;

    // 탐험 추가
    public void createAdventure(CreateAdventureReq createAdventureReq, Long userId){
        Optional<User> curUser = userRepository.findById(userId);

        // 탐험을 생성한 후,
        Adventure newAdventure = Adventure.builder()
                .category(categoryRepository.findCategoryIdByCategory(createAdventureReq.category()).orElseThrow())
                .featTitle(createAdventureReq.featTitle())
                .featContent(createAdventureReq.featContent())
                .title(createAdventureReq.title())
                .content(createAdventureReq.content())
                .difficulty(createAdventureReq.difficulty())
                .photo(createAdventureReq.photo())
                .startDate(createAdventureReq.startDate())
                .endDate(createAdventureReq.endDate())
                .user(curUser.orElseThrow())
                .build();

        adventureRepository.save(newAdventure);
    }

    // 특정 탐험 조회
    public ReadAdventureRes readAdventure(Long adventureId){
        Adventure newAdventure = adventureRepository.findById(adventureId).orElseThrow();

        ReadAdventureRes newReadAdventureRes = new ReadAdventureRes(
                newAdventure.getAdventureId(),
                newAdventure.getCategory().getCategory(),
                newAdventure.getFeatTitle(),
                newAdventure.getFeatContent(),
                newAdventure.getTitle(),
                newAdventure.getContent(),
                newAdventure.getDifficulty(),
                newAdventure.getPhoto(),
                newAdventure.getStartDate(),
                newAdventure.getEndDate(),
                newAdventure.getAvgReviewRate()
        );

        return newReadAdventureRes;
    }

    // 특정 탐험 삭제
    public void deleteAdventure(Long adventureId){
        adventureRepository.deleteById(adventureId);
    }

    // 모든 탐험 조회(생성순, 달성순, 거리순)
    public List<ReadAdventureRes> readAdventures(String order, Double lat, Double lng){
        // 거리순 조회
        List<ReadAdventureRes> result=new ArrayList<>();
        if(lat!=null && lng!=null){

        }else{
            // 생성시간 조회
            if(order.equals("update")){
                List<Adventure>temp = adventureRepository.findAllByOrderByCreateTimeAsc().orElseThrow();
                result = adventureToReadAdventureRes(temp);
            }
            // 달성자순 조회
            else if(order.equals("user")){

            }
        }

        return result;
    }

    // Adventure를 ReadAdventureRes로 변환.
    public List<ReadAdventureRes> adventureToReadAdventureRes(List<Adventure> temp){
        List<ReadAdventureRes> result=new ArrayList<>();

        for(Adventure adventure : temp){
            ReadAdventureRes newReadAdventureRes = new ReadAdventureRes(
                    adventure.getAdventureId(),
                    adventure.getCategory().getCategory(),
                    adventure.getFeatTitle(),
                    adventure.getFeatContent(),
                    adventure.getTitle(),
                    adventure.getContent(),
                    adventure.getDifficulty(),
                    adventure.getPhoto(),
                    adventure.getStartDate(),
                    adventure.getEndDate(),
                    adventure.getAvgReviewRate()
            );
            result.add(newReadAdventureRes);
        }

        return result;
    }

    // 특정 탐험 장소(체크포인트) 추가
    public void createAdventurePlace(Long adventureId,CreateAdventurePlaceReq[] places){
        Adventure curAdventure = adventureRepository.findById(adventureId).orElseThrow();

        // 좌표의 개수만큼 반복
        for(CreateAdventurePlaceReq place:places) {
            AdventurePlace newAdventurePlace = AdventurePlace.builder()
                    .title(place.title())
                    .content(place.content())
                    .coordinate(new GeometryFactory().createPoint(new Coordinate(place.coordinate()[0],place.coordinate()[1])))
                    .photo(place.photo())
                    .adventure(curAdventure)
                    .build();

            adventurePlaceRepository.save(newAdventurePlace);
        }
    }

    // 특정 탐험의 장소들(체크포인트들) 조회
    public List<ReadAdventurePlaceRes> readAdventurePlace (Long adventureId){
        Adventure curAdventure = adventureRepository.findById(adventureId).orElseThrow();

        List<ReadAdventurePlaceRes> result = new ArrayList<>();

        // AdventurePlace를 꺼내와서,
        List<AdventurePlace> temp = adventurePlaceRepository.findAllByAdventure(curAdventure).orElseThrow();
        // Response로 변환 후 return.
        for(AdventurePlace ap:temp){
            ReadAdventurePlaceRes newReadAdventurePlaceRes = new ReadAdventurePlaceRes(
                    ap.getAdventurePlaceId(),
                    ap.getTitle(),
                    ap.getContent(),
                    new Double[] {ap.getCoordinate().getX(),ap.getCoordinate().getY()},
                    ap.getPhoto()
            );

            result.add(newReadAdventurePlaceRes);
        }

        return result;
    }



    // 특정 유저가 참가중인 탐험 추가(탐험 참가하기)
    public void createAdventureInProgress(Long adventureId, Long userId) {
        User curUser = userRepository.findById(userId).orElseThrow();
        Adventure curAdventure = adventureRepository.findById(adventureId).orElseThrow();
        Long totalPoint = adventurePlaceRepository.countByAdventure(curAdventure);

        AdventureInProgress newAdventureInProgress = AdventureInProgress.builder()
                .totalPoint(totalPoint.intValue())
                .user(curUser)
                .adventure(curAdventure)
                .build();

        adventureInProgressRepository.save(newAdventureInProgress);
    }

    // 특정 유저가 참가중인 탐험 조회
    public List<ReadAdventureInProgressRes> readAdventureInProgress(Long userId){
        User curUser = userRepository.findById(userId).orElseThrow();

        List<ReadAdventureInProgressRes> result = new ArrayList<>();

        List<AdventureInProgress> temp = adventureInProgressRepository.findAllByUser(curUser).orElseThrow();

        for(AdventureInProgress aip : temp){
            ReadAdventureInProgressRes newReadAdventureInProgressRes = new ReadAdventureInProgressRes(
                    aip.getAdventure().getAdventureId(),
                    aip.getTotalPoint(),
                    aip.getCurrentPoint()
            );

            result.add(newReadAdventureInProgressRes);
        }

        return result;
    }

    // 탐험 포기(특정 유저가 참가중인 탐험 삭제)
    public void deleteAdventureInProgress(Long adventureId){
        Adventure curAdventure = adventureRepository.findById(adventureId).orElseThrow();

        adventureInProgressRepository.deleteByAdventure(curAdventure);
    }



    // 특정 유저가 참가중인 모험의 피드 켜기(좋아요 추가)
    public void createAdventureLike(Long adventureId,Long userId){
        User curUser = userRepository.findById(userId).orElseThrow();
        Adventure curAdventure = adventureRepository.findById(adventureId).orElseThrow();

        AdventureLike newAdventureLike = AdventureLike.builder()
                .user(curUser)
                .adventure(curAdventure)
                .build();

        adventureLikeRepository.save(newAdventureLike);
    }

    // 특정 유저가 참가중인 모험의 알림 조회
    public ReadAdventureLikeRes readAdventureLike(Long adventureId,Long userId){
        User curUser = userRepository.findById(userId).orElseThrow();
        Adventure curAdventure = adventureRepository.findById(adventureId).orElseThrow();

        Optional<AdventureLike> findByAdventureandUser = adventureLikeRepository.findByAdventureAndUser(curAdventure,curUser);

        ReadAdventureLikeRes result=null;

        if(findByAdventureandUser.isPresent()){
            result = new ReadAdventureLikeRes(findByAdventureandUser.orElseThrow().getAdventureLikeId(), Boolean.TRUE);
        }else{
            result=new ReadAdventureLikeRes(null,Boolean.FALSE);
        }
        return result;
    }

    // 탐험 알림 끄기
    public void deleteAdventureLike(Long adventureLikeId){
        adventureLikeRepository.deleteById(adventureLikeId);
    }

    // 특정 탐험 달성자 추가
    public void createAdventureSucceed(Long adventureId, Long userId) {
        User curUser = userRepository.findById(userId).orElseThrow();
        Adventure curAdventure = adventureRepository.findById(adventureId).orElseThrow();

        AdventureSucceed newAdventureSucceed = AdventureSucceed.builder()
                .user(curUser)
                .adventure(curAdventure)
                .build();

        adventureSucceedRepository.save(newAdventureSucceed);
    }



    // 특정 탐험 달성자의 후기 추가
    public void createAdventureReview(Long adventureId, CreateAdventureReviewReq createAdventureReviewReq, Long userId) {
        User curUser = userRepository.findById(userId).orElseThrow();
        Adventure curAdventure = adventureRepository.findById(adventureId).orElseThrow();

        AdventureReview newAdventureReview = AdventureReview.builder()
                .content(createAdventureReviewReq.content())
                .rate(createAdventureReviewReq.rate())
                .user(curUser)
                .adventure(curAdventure)
                .build();

        adventureReviewRepository.save(newAdventureReview);
    }

    // 특정 탐험 달성자들의 후기 조회
    public List<ReadAdventureReviewRes> readAdventureReview(Long adventureId) {
        List<ReadAdventureReviewRes> result = new ArrayList<>();

        Adventure curAdventure = adventureRepository.findById(adventureId).orElseThrow();

        List<AdventureReview> temp = adventureReviewRepository.findAllByAdventure(curAdventure).orElseThrow();

        for(AdventureReview ar:temp){
            ReadAdventureReviewRes newReadAdventureReviewRes = new ReadAdventureReviewRes(
                    ar.getAdventureReviewId(),
                    ar.getUser().getUserId(),
                    ar.getUser().getNickname(),
                    ar.getRate(),
                    ar.getContent()
            );

            result.add(newReadAdventureReviewRes);
        }

        return result;
    }

    // 탐험 후기 수정
    public void updateAdventureReview(Long adventurereviewId, UpdateAdventureReviewReq updateAdventureReviewReq, Long userId) {
        User curUser = userRepository.findById(userId).orElseThrow();

        // 꺼내와서,
        AdventureReview curAdvventureReview = adventureReviewRepository.findById(adventurereviewId).orElseThrow();

        // 내용 갈아끼운 후,
        AdventureReview updateAdventureReview = AdventureReview.builder()
                .adventureReviewId(curAdvventureReview.getAdventureReviewId())
                .content(updateAdventureReviewReq.content())
                .rate(updateAdventureReviewReq.rate())
                .user(curUser)
                .adventure(curAdvventureReview.getAdventure())
                .build();

        curAdvventureReview = updateAdventureReview;

        // 저장.
        adventureReviewRepository.save(curAdvventureReview);
    }

    // 탐험 후기 삭제
    public void deleteAdventureReview(Long adventureReviewId) {
        adventureReviewRepository.deleteById(adventureReviewId);
    }

    // 모험 검색(모든 모험 키워드 조회)
    public List<ReadAdventureRes> readAdventureSearch(String keyword) {
        System.out.println(keyword);
        List<Adventure> adventureList = adventureRepository.findByTitleContaining(keyword).orElseThrow();
        System.out.println("==================================================================");

        List<ReadAdventureRes> readAdventureResList = new ArrayList<>();
        for(Adventure adventure:adventureList){
            ReadAdventureRes newReadAdventureRes = new ReadAdventureRes(
                    adventure.getAdventureId(),
                    adventure.getCategory().getCategory(),
                    adventure.getFeatTitle(),
                    adventure.getFeatContent(),
                    adventure.getTitle(),
                    adventure.getContent(),
                    adventure.getDifficulty(),
                    adventure.getPhoto(),
                    adventure.getStartDate(),
                    adventure.getEndDate(),
                    adventure.getAvgReviewRate()
            );

            readAdventureResList.add(newReadAdventureRes);
        }

        return readAdventureResList;
    }

    // 특정 위치에서 일정 거리 안에 내가 참가중인 탐험과 탐험 장소 조회하기
    public List<ReadAdventureInProgressWithinDistanceRes> readAdventureInProgressWithinDistance(Double lat,Double lng,Long userId) {
        User curUser = userRepository.findById(userId).orElseThrow();

        Double area = 0.05;

        System.out.println(lng + " " + lat + " " + area);

        Location northEast = GeometryUtil.calculateByDirection(lng, lat, area, CardinalDirection.NORTHEAST
                .getBearing());
        Location southWest = GeometryUtil.calculateByDirection(lng, lat, area, CardinalDirection.SOUTHWEST
                .getBearing());
        double x1 = northEast.lat();
        double y1 = northEast.lng();
        double x2 = southWest.lat();
        double y2 = southWest.lng();

        String pointFormat = String.format("'LINESTRING(%f %f, %f %f)')", x1, y1, x2, y2);
        Query query = entityManager.createNativeQuery("" +
                                "SELECT * FROM adventure_place as ap " +
                                "WHERE ap.adventure_place_id="+"(select aip.progress_id from adventure_in_progress as aip where aip.user_id ="+curUser.getUserId().toString()+ ") "
                                +"and MBRContains(ST_LINESTRINGFROMTEXT(" + pointFormat + ", ap.coordinate)"
                        , AdventurePlace.class)
                .setMaxResults(100);
        List<AdventurePlace> adventurePlaceList = query.getResultList();

        System.out.println(adventurePlaceList);
        System.out.println(adventurePlaceList.size());

        List<ReadAdventureInProgressWithinDistanceRes> readAdventureInProgressWithinDistanceRes = new ArrayList<>();
        for (AdventurePlace ap :
                adventurePlaceList) {
            ReadAdventureInProgressWithinDistanceRes newReadAdventureInProgressWithinDistanceRes1 = new ReadAdventureInProgressWithinDistanceRes(
                    ap.getAdventure().getAdventureId(),
                    ap.getAdventure().getTitle(),
                    ap.getAdventurePlaceId(),
                    ap.getTitle()
            );
            readAdventureInProgressWithinDistanceRes.add(newReadAdventureInProgressWithinDistanceRes1);
        }

        return readAdventureInProgressWithinDistanceRes;
    }
}
