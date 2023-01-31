package com.ssafy.antenna.service;

import com.ssafy.antenna.domain.ResultResponse;
import com.ssafy.antenna.domain.adventure.Adventure;
import com.ssafy.antenna.domain.adventure.AdventureSucceed;
import com.ssafy.antenna.domain.antenna.Antenna;
import com.ssafy.antenna.domain.antenna.dto.DetailAntennaRes;
import com.ssafy.antenna.domain.antenna.dto.PostAntennaReq;
import com.ssafy.antenna.domain.email.dto.AuthEmailRes;
import com.ssafy.antenna.domain.email.dto.CheckEmailRes;
import com.ssafy.antenna.domain.user.Follow;
import com.ssafy.antenna.domain.user.User;
import com.ssafy.antenna.domain.user.dto.*;
import com.ssafy.antenna.exception.not_found.*;
import com.ssafy.antenna.exception.unauthorized.InvalidPasswordException;
import com.ssafy.antenna.repository.AdventureSucceedRepository;
import com.ssafy.antenna.repository.AntennaRepository;
import com.ssafy.antenna.repository.FollowRepository;
import com.ssafy.antenna.repository.UserRepository;
import com.ssafy.antenna.util.EmailUtil;
import com.ssafy.antenna.util.ImageUtil;
import com.ssafy.antenna.util.W3WUtil;
import com.what3words.javawrapper.response.ConvertTo3WA;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final JavaMailSender javaMailSender;
    private final PasswordEncoder passwordEncoder;
    private final ImageUtil imageUtil;
    private final AntennaRepository antennaRepository;
    private final AdventureSucceedRepository adventureSucceedRepository;
    private final W3WUtil w3WUtil;

    public User getUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
    }


    public User deleteUser(Long userId) {
        //유저 정보가 존재 하는지 먼저 검색
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        //존재한다면, delete 작업 수행한다.
        userRepository.deleteById(userId);
        return user;
    }

    public User modifyPwdUser(Long userId, ModifyPwdUserReq modifyPwdUserReq) {
        //유저 정보가 존재 하는지 먼저 검색
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        //존재한다면, 기존 비밀번호가 일치하는지 확인한다.
        if (passwordEncoder.matches(modifyPwdUserReq.oldPassword(), user.getPassword())) {
            User newUser = new User(user.getCreateTime(), user.getUpdateTime(), user.getUserId(), user.getEmail(), user.getNickname(), passwordEncoder.encode(modifyPwdUserReq.newPassword()), user.getLevel(), user.getExp(), user.getIntroduce(), user.getPhoto());
            return userRepository.save(newUser);
        } else {
            throw new InvalidPasswordException();
        }
    }

    public FollowDetailRes createFollowUser(Long userId, CreateFollowUserReq createFollowUserReq) {
        //두 유저가 존재하는지 먼저 확인하기
        User follower = userRepository.findById(userId).orElseThrow(FollowerNotFoundException::new);
        User following = userRepository.findById(createFollowUserReq.followingId()).orElseThrow(FollowingNotFoundException::new);
        //두 유저가 모두 존재한다면, 데이터 넣어주기.
        Follow newFollow = new Follow();
        newFollow.setFollowerId(userId);
        newFollow.setFollowingId(createFollowUserReq.followingId());
        newFollow.setFollowerUser(follower);
        newFollow.setFollowingUser(following);
        followRepository.save(newFollow);
        return newFollow.toResponse();
    }

    public List<UserDetailRes> getFollowingUser(Long userId) {
        //유저가 존재하는지 먼저 확인
        userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        List<Follow> followList = followRepository.findByFollowingId(userId);
        List<UserDetailRes> userDetailResList = new ArrayList<>();
        for (int i = 0; i < followList.size(); i++) {
            userDetailResList.add(followList.get(i).getFollowerUser().toResponse());
        }
        return userDetailResList;
    }

    public List<UserDetailRes> getFollowerUser(Long userId) {
        //유저가 존재하는지 먼저 확인
        userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        List<Follow> followList = followRepository.findByFollowerId(userId);
        List<UserDetailRes> userDetailResList = new ArrayList<>();
        for (int i = 0; i < followList.size(); i++) {
            userDetailResList.add(followList.get(i).getFollowingUser().toResponse());
        }
        return userDetailResList;
    }

    public Follow deleteFollowingUser(Long userId, Long followId) {
        //유저가 존재하는지 먼저 확인
        userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        //팔로우 아이디 존재하는지 확인
        Follow deletedFollow = followRepository.findById(followId).orElseThrow(FollowNotFoundException::new);
        followRepository.deleteById(followId);
        return deletedFollow;
    }

    public CheckEmailRes checkEmailUser(String email) {
        int count = userRepository.countByEmail(email);
        CheckEmailRes checkEmailRes = new CheckEmailRes(false, null);
        if (count == 1) {
            User user = userRepository.findByEmail(email).orElseThrow(UserNotFoundException::new);
            checkEmailRes = new CheckEmailRes(true, user.toResponse());
        }
        return checkEmailRes;
    }

    @Transactional
    public AuthEmailRes resetPwdUser(ResetPwdUserReq resetPwdUserReq) throws Exception {
        EmailUtil emailUtil = new EmailUtil(javaMailSender);
        User user = userRepository.findById(resetPwdUserReq.userId()).orElseThrow(UserNotFoundException::new);
        emailUtil.setTo(user.getEmail());
        emailUtil.setSubject("antenna 임시 비밀번호 발급 안내입니다.");
        Random rand = new Random();
        StringBuffer key = new StringBuffer();
        for (int i = 0; i < 12; i++) {
            int index = rand.nextInt(3);
            switch (index) {
                case 0:
                    key.append(Character.toChars(((rand.nextInt(26)) + 97)));
                    break;
                case 1:
                    key.append(Character.toChars(((rand.nextInt(26)) + 65)));
                    break;
                case 2:
                    key.append(Character.toChars((rand.nextInt(10)) + 48));
                    break;
            }
        }
        String htmlContent = "<p> 임시 비밀번호는 [" + key + "] 입니다.<p>";
        emailUtil.setText(htmlContent, true);
        emailUtil.send();
        User newUser = new User(user.getCreateTime(), user.getUpdateTime(), user.getUserId(), user.getEmail(), user.getNickname(), passwordEncoder.encode(key.toString()), user.getLevel(), user.getExp(), user.getIntroduce(), user.getPhoto());
        userRepository.save(newUser);

        return new AuthEmailRes(true);
    }

    public CheckNicknameRes checkNicknameUser(String nickname) {
        int count = userRepository.countByNickname(nickname);
        CheckNicknameRes checkNicknameRes = new CheckNicknameRes(false, null);
        if (count == 1) {
            User user = userRepository.findByNickname(nickname).orElseThrow(UserNotFoundException::new);
            checkNicknameRes = new CheckNicknameRes(true, user.toResponse());
        }
        return checkNicknameRes;
    }

    public List<UserDetailRes> likeNicknameUser(String nickname) {
        List<User> userList = userRepository.findAllByNicknameStartingWith(nickname);
        List<UserDetailRes> userDetailResList = new ArrayList<>();
        for (int i = 0; i < userList.size(); i++) {
            userDetailResList.add(userList.get(i).toResponse());
        }
        return userDetailResList;
    }

    public String uploadImage(MultipartFile multipartFile, Long userId) {
        User user = getUser(userId);
        try {
            user.setPhoto(ImageUtil.compressImage(multipartFile.getBytes()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        userRepository.save(user);
        return "succeed";
    }

//    public byte[] downloadImage(Long userId) {
//        User user = getUser(userId);
//        byte[] photo = ImageUtil.decompressImage(user.getPhoto());
//        return photo;
//    }

    public ResponseEntity<byte[]> getImage(Long userId) {
        //유저가 있는지 확인
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        byte[] photo = ImageUtil.decompressImage(user.getPhoto());
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("image/"+user.getPhotoType()))
                .body(photo);
    }

    public UserDetailRes modifyProfileUser(String introduce, Long userId) {
        //유저가 존재하는지 먼저 확인
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        //소개글 수정 후 저장
        User newUser = new User(user.getCreateTime(), user.getUpdateTime(), user.getUserId(), user.getEmail(), user.getNickname(), user.getPassword(), user.getLevel(), user.getExp(), introduce, user.getPhoto());
        userRepository.save(newUser);
        return newUser.toResponse();
    }

    public ResultResponse<UserDetailRes> deleteImage(Long userId) {
        User user = userRepository.findById(userId).orElseGet(User::new);
        user.setPhoto(null);
        userRepository.save(user);
        return ResultResponse.success(
                new UserDetailRes(
                        user.getUserId(),
                        user.getEmail(),
                        user.getNickname(),
                        user.getLevel(),
                        user.getExp(),
                        user.getIntroduce(),
                        user.getPhoto()
                )
        );
    }

    public ResultResponse<UserFeatsRes> getUserFeats(Long userId) {
        List<AdventureSucceed> adventureSucceeds = adventureSucceedRepository.findAllByUser(
                userRepository.findById(userId).orElseThrow(UserNotFoundException::new));
        List<String> result = adventureSucceeds.stream()
                .map(b -> b.getAdventure().getFeatTitle())
                .collect(Collectors.toList());
        return ResultResponse.success(new UserFeatsRes(result));
    }
    public DetailAntennaRes createAntenna(PostAntennaReq postAntennaReq, Long userId) {
        //유저가 존재하는지 먼저 확인
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        ConvertTo3WA w3wWords = w3WUtil.getW3W(postAntennaReq.lng(), postAntennaReq.lat());
        Antenna antenna = Antenna.builder()
                .user(user)
                .area(postAntennaReq.area())
                .coordinate(new GeometryFactory().createPoint(new Coordinate(w3wWords.getCoordinates().getLng(), w3wWords.getCoordinates().getLat())))
                .w3w(w3wWords.getWords())
                .nearestPlace(w3wWords.getNearestPlace())
                .build();
        antennaRepository.save(antenna);
        return antenna.toResponse();
    }


    public DetailAntennaRes deleteAntenna(Long antennaId, Long userId) {
        //유저가 존재하는지 확인
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        //유저가 존재하면, 유저가 그 안테나 아이디를 가지고 있는지 확인
        Antenna antenna = antennaRepository.findByAntennaIdAndUser(antennaId, user).orElseThrow(AntennaNotFoundException::new);
        antennaRepository.deleteById(antennaId);
        return antenna.toResponse();
    }

    public List<DetailAntennaRes> getAllAntennae(Long userId) {
        //유저가 존재하는지 확인
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        //유저가 존재하면, 안테나 리스트 가져오기
        List<Antenna> antennaList = antennaRepository.findAllByUser(user);
        List<DetailAntennaRes> detailAntennaResList = new ArrayList<>();
        for (int i = 0; i < antennaList.size(); i++) {
            detailAntennaResList.add(antennaList.get(i).toResponse());
        }
        return detailAntennaResList;
    }

    public DetailAntennaRes getAntenna(Long antennaId, Long userId) {
        //유저가 존재하는지 확인
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        //유저가 존재하면, 유저와 안테나를 함께 조회
        Antenna antenna = antennaRepository.findByAntennaIdAndUser(antennaId, user).orElseThrow(AntennaNotFoundException::new);
        return antenna.toResponse();
    }
}
