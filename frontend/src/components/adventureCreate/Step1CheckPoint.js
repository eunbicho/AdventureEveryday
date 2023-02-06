import React, { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";

import AdventureMap from "./AdventureMap";
import SelectedCheckPoint from "./SelectedCheckPoint";
import SelectPostModal from "./SelectPostModal";

import styles from "./Step1CheckPoint.module.css";

const Step1CheckPoint = ({
  checkPoints,
  setCheckPoints,
  advCheckPoints,
  setAdvCheckPoints,
  adv,
  setAdv,
}) => {
  const navigate = useNavigate();

  const [showModal, setShowModal] = useState(false);

  const count = useMemo(() => {
    // console.log(setCheckPoints);
    return checkPoints && checkPoints.length;
  }, [checkPoints]);

  useEffect(() => {
    window.scrollTo(0, 0);
  }, []);

  const openModal = () => {
    if (checkPoints.length === 5) {
      alert("체크포인트는 최대 5개까지 선택할 수 있습니다.");
    } else {
      setShowModal(true);
      document.body.style.overflow = "hidden";
    }
  };
  const closeModal = () => {
    setShowModal(false);
    document.body.style.overflow = "unset";
  };

  const selectPost = (post) => {
    // console.log("닫기!", post);
    const check = checkPoints.every((point) => {
      return point.postId !== post.postId;
    });
    if (check) {
      setCheckPoints((checkPoints) => [...checkPoints, post]);
      closeModal();
      const newCheckPoint = {
        title: "",
        content: "",
        coordinate: [post.lat, post.lng],
        postId: post.postId,
      };
      setAdvCheckPoints((advCheckPoints) => [...advCheckPoints, newCheckPoint]);
    } else {
      alert("이미 선택한 게시글입니다.");
    }
  };

  const unSelectPost = (post) => {
    const newCheckPoints = checkPoints.filter((point) => {
      return point.postId !== post.postId;
    });
    setCheckPoints(newCheckPoints);
    const newAdvCheckPoints = advCheckPoints.filter((point) => {
      return point.postId !== post.postId;
    });
    setAdvCheckPoints(newAdvCheckPoints);
  };

  const getDistance = (lat1, lon1, lat2, lon2) => {
    if (lat1 === lat2 && lon1 === lon2) return 0;

    var radLat1 = (Math.PI * lat1) / 180;
    var radLat2 = (Math.PI * lat2) / 180;
    var theta = lon1 - lon2;
    var radTheta = (Math.PI * theta) / 180;
    var dist =
      Math.sin(radLat1) * Math.sin(radLat2) +
      Math.cos(radLat1) * Math.cos(radLat2) * Math.cos(radTheta);
    if (dist > 1) dist = 1;

    dist = Math.acos(dist);
    dist = (dist * 180) / Math.PI;
    dist = dist * 60 * 1.1515 * 1.609344 * 1000;
    if (dist < 100) dist = Math.round(dist / 10) * 10;
    else dist = Math.round(dist / 100) * 100;

    return dist;
  };

  const exp = useMemo(() => {
    let dist = 0;
    for (let i = 0; i < checkPoints.length; i++) {
      for (let j = 0; j < checkPoints.length; j++) {
        dist += getDistance(
          checkPoints[i].lat,
          checkPoints[i].lng,
          checkPoints[j].lat,
          checkPoints[j].lng
        );
      }
    }
    dist /= checkPoints.length * 2;
    // console.log(dist / 50);
    return dist / 50;
  }, [checkPoints]);

  const difficulty = useMemo(() => {
    if (exp < 100) {
      return 1;
    } else if (exp < 500) {
      return 2;
    } else if (exp < 1500) {
      return 3;
    } else if (exp < 5000) {
      return 4;
    } else {
      return 5;
    }
  }, [exp]);

  return (
    <>
      <h1>탐험 생성</h1>

      <p>탐험으로 만들 내 글을 선택하세요!</p>
      <p>게시글은 최대 5개까지 선택할 수 있습니다.</p>

      <p>현재 체크포인트 개수 {count}/5</p>

      {checkPoints.map((point) => (
        <SelectedCheckPoint
          key={point.postId}
          point={point}
          unSelectPost={unSelectPost}
          advCheckPoints={advCheckPoints}
          setAdvCheckPoints={setAdvCheckPoints}
          isRep={adv.photo === point.postUrl}
          setAdv={setAdv}
        />
      ))}

      <div className={styles.addBox} onClick={openModal}>
        체크포인트 추가
      </div>

      <AdventureMap checkPoints={checkPoints} />

      {checkPoints.length > 1 ? (
        <div>이 탐험의 난이도 {difficulty}</div>
      ) : (
        <div></div>
      )}

      <button onClick={() => navigate(-1)}>취소</button>
      <button
        onClick={() => {
          if (checkPoints.length < 2) {
            alert("체크포인트를 2개 이상 선택해주세요.");
          } else if (!adv.photo) {
            alert("대표 이미지를 선택해 주세요.");
          } else {
            setAdv((adv) => ({ ...adv, difficulty: difficulty }));
            navigate("/adventure/create/2");
          }
        }}
      >
        다음
      </button>

      {showModal && (
        <SelectPostModal closeModal={closeModal} selectPost={selectPost} />
      )}
    </>
  );
};

export default Step1CheckPoint;
