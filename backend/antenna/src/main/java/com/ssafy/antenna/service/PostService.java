package com.ssafy.antenna.service;

import com.ssafy.antenna.domain.ResultResponse;
import com.ssafy.antenna.domain.adventure.Adventure;
import com.ssafy.antenna.domain.antenna.Antenna;
import com.ssafy.antenna.domain.comment.Comment;
import com.ssafy.antenna.domain.comment.PostCommentReq;
import com.ssafy.antenna.domain.comment.SubComment;
import com.ssafy.antenna.domain.comment.SubCommentDtoMapper;
import com.ssafy.antenna.domain.comment.dto.PostSubCommentReq;
import com.ssafy.antenna.domain.comment.dto.SubCommentDto;
import com.ssafy.antenna.domain.comment.dto.commentDto;
import com.ssafy.antenna.domain.like.AdventureLike;
import com.ssafy.antenna.domain.like.CommentLike;
import com.ssafy.antenna.domain.like.PostLike;
import com.ssafy.antenna.domain.like.SubCommentLike;
import com.ssafy.antenna.domain.like.dto.CommentLikeDto;
import com.ssafy.antenna.domain.like.dto.PostLikeDto;
import com.ssafy.antenna.domain.like.dto.SubCommentLikeDto;
import com.ssafy.antenna.domain.location.Location;
import com.ssafy.antenna.domain.post.CheckpointPost;
import com.ssafy.antenna.domain.post.Post;
import com.ssafy.antenna.domain.post.dto.PostDetailRes;
import com.ssafy.antenna.domain.post.dto.PostDetailWithCategory;
import com.ssafy.antenna.domain.post.dto.PostDto;
import com.ssafy.antenna.domain.post.dto.PostUpdateReq;
import com.ssafy.antenna.domain.post.mapper.PostDtoMapper;
import com.ssafy.antenna.domain.user.Follow;
import com.ssafy.antenna.domain.user.User;
import com.ssafy.antenna.exception.not_found.AdventureNotFoundException;
import com.ssafy.antenna.exception.not_found.AdventurePlaceNotFoundException;
import com.ssafy.antenna.exception.not_found.UserNotFoundException;
import com.ssafy.antenna.repository.*;
import com.ssafy.antenna.util.CardinalDirection;
import com.ssafy.antenna.util.GeometryUtil;
import com.ssafy.antenna.util.W3WUtil;
import com.what3words.javawrapper.response.ConvertTo3WA;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {
    private final EntityManager entityManager;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final W3WUtil w3WUtil;
    private final CommentRepository commentRepository;
    private final PostDtoMapper postDtoMapper;
    private final PostLikeRepository postLikeRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final SubCommentRepository subCommentRepository;
    private final SubCommentDtoMapper subCommentDtoMapper;
    private final SubCommentLikeRepository subCommentLikeRepository;
    private final AntennaRepository antennaRepository;
    private final AwsS3Service awsS3Service;
    private final CheckpointPostRepository checkpointPostRepository;
    private final AdventureLikeRepository adventureLikeRepository;
    private final AdventureRepository adventureRepository;
    private final FollowRepository followRepository;
    private final AdventurePlaceRepository adventurePlaceRepository;

    @Value("${aws-cloud.aws.s3.bucket.url}")
    private String bucketUrl;


    public PostDetailWithCategory getPostById(Long postId, Long userId) {
        /*
         * 1. 안테나의 범위에 있는지, 참가 중인 알람을 킨 모험의 글인지, 팔로잉 중인 사람의 글인지에 따라 카테고리를 넣어주기.
         * */
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        //먼저 글이 있는지를 조회한다.
        Post post = postRepository.findById(postId).orElseThrow(NoSuchElementException::new);
        Long isAntenna = 0L, isChallenge = 0L, isFollowing = 0L;
        //있다면, 그 글이 안테나의 범위 안에 속해있는지를 조회해야한다. -> 있다면 안테나의 id를 isChallenge에 넣어줘야함
        Optional<List<Antenna>> antennaList = antennaRepository.findAllByUser(user);
        System.out.println();
        for (Antenna antenna :
                antennaList.get()) {
            System.out.println(antenna.toResponse());
        }
        if (antennaList.isPresent()) {
            for (Antenna antenna : antennaList.get()) {
                //안테나 별로 주변 게시글을 조회해서 그 게시글 중 내가 가진 postId가 있는지를 체크한다.
                System.out.println(isAntenna.toString());
                System.out.println(antenna.getCoordinate().getX());
                Long isAntennaId = isPostWithArea(antenna.getCoordinate().getX(), antenna.getCoordinate().getY(), antenna.getArea(), postId);
                if (isAntennaId != 0) {
                    //찾았으면 종료
                    isAntenna = antenna.getAntennaId();
                    break;
                }
            }
        }

        //이번엔 알람을 킨 모험의 글인지 조회한다.
        // 1. 체크포인트 게시글인지 조회
        // 2. 체크포인트 게시글이라면 거기서 나온 어드벤처id와 유저id로 탐험좋아요 테이블에서 조회해서 나온 결과값이 있나 확인
        // 3. 있으면? isChallenge에 어드벤처id를 넣어준다.
        Optional<CheckpointPost> checkpointPost = checkpointPostRepository.findByPost(post);
        if (checkpointPost.isPresent()) {
            //게시글이 있다면
            Adventure adventure = adventureRepository.findById(checkpointPost.get().getAdventure().getAdventureId()).orElseThrow(AdventureNotFoundException::new);

            Optional<AdventureLike> adventureLike = adventureLikeRepository.findByAdventureAndUser(adventure, user);
            if (adventureLike.isPresent()) {
                //알람설정한 모험이 존재하면, isChallenge에 어드벤처id를 넣어준다.
                isChallenge = adventureLike.get().getAdventure().getAdventureId();
            }
        }
        //마지막으로, 팔로잉 중인 사람의 글인지에 따라 카테고리를 넣어주기. -> 내가 팔로워인 데이터 조회해서 팔로잉 유저들이
        //내가 가지고있는 post의 유저와 같은지 확인한다.
        Optional<List<Follow>> followList = followRepository.findAllByFollowerUser(user);
        if (followList.isPresent()) {
            for (Follow follow : followList.get()) {
                if (post.getUser().getUserId().equals(follow.getFollowingUser().getUserId())) {
                    isFollowing = post.getUser().getUserId();
                }
            }
        }
        PostDetailWithCategory postDetailWithCategory = new PostDetailWithCategory(
                post.getPostId(),
                post.getTitle(),
                post.getContent(),
                post.getCoordinate().getX(),
                post.getCoordinate().getY(),
                post.getNearestPlace(),
                post.getW3w(),
                post.isPublic(),
                post.getCreateTime(),
                post.getPhotoUrl(),
                isAntenna,
                isChallenge,
                isFollowing,
                post.getUser().toResponse()
        );

        //getPostWithArea
        return postDetailWithCategory;
    }

    public PostDetailRes deletePost(Long userId, Long postId) throws IllegalAccessException {
        Post post = postRepository.findById(postId)
                .orElseThrow(NoSuchElementException::new);
        if (post.getPhotoName() != null) {
            awsS3Service.deleteImage(post.getPhotoName());
        }
        if (post.getUser().getUserId().equals(userId)) {
            postRepository.delete(post);
            return post.toResponse();
        }
        {
            throw new IllegalAccessException("잘못된 접근입니다");
        }
    }

    public PostDetailRes createPost(Long userId, String title, String content, String lat, String lng, String isPublic, MultipartFile photo, String isCheckpoint, String adventureId, String adventurePlaceId) throws IOException {
        ConvertTo3WA w3wWords = w3WUtil.getW3W(Double.parseDouble(lng), Double.parseDouble(lat));
        Post post = Post.builder()
                .user(userRepository.findById(userId).orElseThrow(UserNotFoundException::new))
                .title(title).content(content)
                .coordinate(new GeometryFactory().createPoint(new Coordinate(w3wWords.getCoordinates().getLng(), w3wWords.getCoordinates().getLat())))
                .w3w(w3wWords.getWords())
                .nearestPlace(w3wWords.getNearestPlace())
                .isPublic(Boolean.valueOf(isPublic))
                .build();
        if (photo != null) {
            String photoName = awsS3Service.uploadImage(photo);
            String photoUrl = bucketUrl + photoName;
            post = Post.builder().user(userRepository.findById(userId).orElseThrow(UserNotFoundException::new)).title(title).content(content).coordinate(new GeometryFactory().createPoint(new Coordinate(w3wWords.getCoordinates().getLng(), w3wWords.getCoordinates().getLat()))).w3w(w3wWords.getWords()).nearestPlace(w3wWords.getNearestPlace()).isPublic(Boolean.valueOf(isPublic)).photoUrl(photoUrl).photoName(photoName).build();

        }

        Post save = postRepository.save(post);

        if (Boolean.valueOf(isCheckpoint)) {
            checkpointPostRepository.save(CheckpointPost.builder()
                    .adventure(adventureRepository.findById(Long.valueOf(adventureId)).orElseThrow(AdventureNotFoundException::new))
                    .adventurePlace(adventurePlaceRepository.findById(Long.valueOf(adventurePlaceId)).orElseThrow(AdventurePlaceNotFoundException::new))
                    .post(save)
                    .build()
            );
        }

        return post.toResponse();
    }

//    public ResponseEntity<?> getPostPhoto(Long postId) {
//        Post post = postRepository.findById(postId).orElseThrow(NoSuchElementException::new);
//        byte[] photo;
//        try {
//            photo = ImageUtil.decompressImage(post.getPhoto());
//        } catch (NullPointerException e) {
//            throw new NoSuchElementException("사진이 없습니다");
//        }
//        return ResponseEntity.ok().contentType(MediaType.valueOf("image/" + post.getPhotoType())).body(photo);
//    }

    public ResultResponse<PostDetailRes> updatePost(
            Long postId,
            PostUpdateReq postUpdateReq,
            Authentication authentication
    ) throws IllegalAccessException {
        Post post = postRepository.findById(postId).orElseThrow(NoSuchElementException::new);
        if (!Long.valueOf(authentication.getName()).equals(post.getUser().getUserId())) {
            throw new IllegalAccessException();
        } else {
            Post newPost = Post.builder()
                    .postId(post.getPostId())
                    .user(post.getUser())
                    .title(postUpdateReq.title())
                    .content(postUpdateReq.content())
                    .isPublic(Boolean.valueOf(postUpdateReq.isPublic()))
                    .photoUrl(post.getPhotoUrl())
                    .photoName(post.getPhotoName())
                    .nearestPlace(post.getNearestPlace())
                    .w3w(post.getW3w())
                    .coordinate(post.getCoordinate()).build();
            return ResultResponse.success(postRepository.save(newPost).toResponse());
        }
    }

    public ResultResponse<?> postComment(
            Long postId,
            PostCommentReq postCommentReq,
            Long userId
    ) {
        Post post = postRepository.findById(postId).orElseThrow(NoSuchElementException::new);
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        Comment comment = commentRepository.save(Comment.builder().post(post).user(user).content(postCommentReq.content()).build());
        return ResultResponse.success(postId);
    }

    public ResultResponse<?> getPostByUserId(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        List<PostDto> postDtoList = postRepository.findAllByUser(user).stream()
                .filter(Post::isPublic)
                .map(postDtoMapper)
                .collect(Collectors.toList());
        return ResultResponse.success(postDtoList);
    }

    public ResultResponse<?> getCommentsByPostId(Long postId) {
        Post post = postRepository.findById(postId).orElseThrow(NoSuchElementException::new);
        List<commentDto> commentList = post.getComments().stream().map(comment -> new commentDto(comment.getUser().getNickname(), comment.getContent())).collect(Collectors.toList());
        return ResultResponse.success(commentList);
    }

    public ResultResponse<?> deleteComment(Long commentId, Long userId)
            throws IllegalAccessException {
        Comment comment = commentRepository.findById(commentId).orElseThrow(NoSuchElementException::new);
        if (!comment.getUser().getUserId().equals(userId)) {
            throw new IllegalAccessException("잘못된 접근입니다");
        } else {
            commentRepository.delete(comment);
        }
        return ResultResponse.success("삭제 성공");
    }

    public ResultResponse<?> postPostLike(Long postId, Long userId) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        Post post = postRepository.findById(postId).orElseThrow(NoSuchElementException::new);
        postLikeRepository.save(PostLike.builder()
                .user(user)
                .post(post)
                .build());
        return ResultResponse.success("좋아요 등록 성공");
    }

    public ResultResponse<?> getPostLike(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(NoSuchElementException::new);
        List<PostLike> postLikeList = postLikeRepository.findAllByPost(post);
        boolean isLiked = postLikeList.stream()
                .filter(postLike -> postLike.getUser().getUserId().equals(userId))
                .collect(Collectors.toList()).isEmpty();
        return ResultResponse.success(new PostLikeDto(post.getPostLikes().size(), !isLiked));
//        return ResultResponse.success(post.getPostLikes().size());
    }

    public ResultResponse<?> deletePostLike(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(NoSuchElementException::new);
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        List<PostLike> postLikeList = postLikeRepository.findAllByPost(post).stream()
                .filter(postLike -> postLike.getUser().equals(user)).collect(Collectors.toList());
        if (postLikeList.size() > 0) {
            postLikeRepository.delete(postLikeList.get(0));
        } else {
            throw new NoSuchElementException();
        }
        return ResultResponse.success("삭제 성공");
    }

    public ResultResponse<?> postCommentLike(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(NoSuchElementException::new);
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        CommentLike save = commentLikeRepository.save(CommentLike.builder()
                .comment(comment)
                .user(user)
                .build());
        return ResultResponse.success("댓글 좋아요 성공");
    }

    public ResultResponse<?> getCommentLike(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(NoSuchElementException::new);
        List<CommentLike> commentLikes = comment.getCommentLikes();
        boolean isLiked = commentLikes.stream()
                .filter(commentLike -> commentLike.getUser().getUserId().equals(userId))
                .collect(Collectors.toList()).isEmpty();
        return ResultResponse.success(new CommentLikeDto(commentLikes.size(), !isLiked));
    }

    public ResultResponse<?> deleteCommentLike(Long commentId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        List<CommentLike> commentLikes = commentLikeRepository.findAllByUser(user).stream()
                .filter(commentLike -> commentLike.getComment().getCommentId().equals(commentId))
                .collect(Collectors.toList());
        if (commentLikes.size() > 0) {
            commentLikeRepository.delete(commentLikes.get(0));
            return ResultResponse.success("좋아요 삭제 성공");
        } else {
            throw new NoSuchElementException();
        }
    }

    public ResultResponse<?> postSubComment(
            Long commentId,
            PostSubCommentReq postSubCommentReq,
            Long userId
    ) {
        SubComment subComment = SubComment.builder()
                .comment(commentRepository.findById(commentId)
                        .orElseThrow(NoSuchElementException::new))
                .user(userRepository.findById(userId)
                        .orElseThrow(UserNotFoundException::new))
                .content(postSubCommentReq.content())
                .build();
        subCommentRepository.save(subComment);
        return ResultResponse.success("대댓글 작성 성공");

    }

    public ResultResponse<?> getSubComments(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(NoSuchElementException::new);
        List<SubCommentDto> subCommentDtos = comment.getSubComments().stream()
                .map(subCommentDtoMapper)
                .collect(Collectors.toList());
        return ResultResponse.success(subCommentDtos);
    }

    public ResultResponse<?> deleteSubComment(Long subCommentId, Long userId)
            throws IllegalAccessException {
        SubComment subComment = subCommentRepository.findById(subCommentId)
                .orElseThrow(NoSuchElementException::new);
        if (subComment.getUser().getUserId().equals(userId)) {
            subCommentRepository.delete(subComment);
            return ResultResponse.success("대댓글 삭제 성공");
        }
        throw new IllegalAccessException("잘못된 접근입니다");
    }

    public ResultResponse<?> postSubCommentLike(Long subCommentId, Long userId) {
        SubComment subComment = subCommentRepository.findById(subCommentId)
                .orElseThrow(NoSuchElementException::new);
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        subCommentLikeRepository.save(SubCommentLike.builder()
                .subComment(subComment)
                .user(user)
                .build());
        return ResultResponse.success("좋아요 성공");
    }

    public ResultResponse<?> getSubCommentLike(Long subCommentId, Long userId) {
        SubComment subComment = subCommentRepository.findById(subCommentId)
                .orElseThrow(NoSuchElementException::new);
        boolean isLiked = subComment.getSubCommentLikes().stream()
                .filter(subCommentLike -> subCommentLike.getUser().getUserId().equals(userId))
                .collect(Collectors.toList()).isEmpty();
        return ResultResponse.success(new SubCommentLikeDto(
                subComment.getSubCommentLikes().size(),
                !isLiked
        ));
    }

    public ResultResponse<?> deleteSubCommentLike(Long subCommentId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        List<SubCommentLike> subCommentLikes = subCommentLikeRepository.findAllByUser(user);
        List<SubCommentLike> collect = subCommentLikes.stream()
                .filter(subCommentLike -> subCommentLike.getSubComment().getSubCommentId().equals(subCommentId))
                .collect(Collectors.toList());
        if (!collect.isEmpty()) {
            subCommentLikeRepository.delete(collect.get(0));
            return ResultResponse.success("삭제 성공");
        }
        throw new NoSuchElementException();
    }

    public List<PostDetailRes> getPostWithArea(double lng, double lat, double area) {
        Location northEast = GeometryUtil.calculateByDirection(lat, lng, area, CardinalDirection.NORTHEAST
                .getBearing());
        Location southWest = GeometryUtil.calculateByDirection(lat, lng, area, CardinalDirection.SOUTHWEST
                .getBearing());
        System.out.println(northEast);
        System.out.println(southWest);
        double x1 = northEast.lng();
        double y1 = northEast.lat();
        double x2 = southWest.lng();
        double y2 = southWest.lat();
        String pointFormat = String.format("'LINESTRING(%f %f, %f %f)')", x1, y1, x2, y2);
        Query query = entityManager.createNativeQuery("" +
                                "SELECT * FROM post as p " +
                                "WHERE MBRContains(ST_LINESTRINGFROMTEXT(" + pointFormat + ", p.coordinate) and p.is_public=true"
                        , Post.class)
                .setMaxResults(100);
        List<Post> postList = query.getResultList();
        List<PostDetailRes> postDetailResList = new ArrayList<>();
        for (Post post :
                postList) {
            postDetailResList.add(post.toResponse());
        }
        return postDetailResList;
    }

    public Long isPostWithArea(double lng, double lat, double area, Long postId) {
        Location northEast = GeometryUtil.calculateByDirection(lat, lng, area, CardinalDirection.NORTHEAST
                .getBearing());
        Location southWest = GeometryUtil.calculateByDirection(lat, lng, area, CardinalDirection.SOUTHWEST
                .getBearing());
        System.out.println(northEast);
        System.out.println(southWest);
        double x1 = northEast.lng();
        double y1 = northEast.lat();
        double x2 = southWest.lng();
        double y2 = southWest.lat();
        String pointFormat = String.format("'LINESTRING(%f %f, %f %f)')", x1, y1, x2, y2);
        Query query = entityManager.createNativeQuery("" +
                        "SELECT * FROM (SELECT post_id FROM post as p " +
                        "WHERE MBRContains(ST_LINESTRINGFROMTEXT(" + pointFormat + ", p.coordinate) and p.is_public=true" +
                        ") as list where post_id=" + postId
                , Long.class);
        List<Long> isAntennaId = query.getResultList();
        if (isAntennaId.size() == 0) {
            return 0L;
        } else {
            return isAntennaId.get(0);
        }
    }


}
