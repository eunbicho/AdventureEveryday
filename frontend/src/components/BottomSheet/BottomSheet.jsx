import React from "react";
import { useEffect, useRef, useState, useMemo } from "react";
import { BottomSheet } from "react-spring-bottom-sheet";
import "react-spring-bottom-sheet/dist/style.css";
// import "./BottomSheet.css";
import ArticleListItem from '../ArticleListItem';
import AdventureBanner from './../Adventure/AdventureBanner';
import { useSelector } from "react-redux"
import axios from "axios"

// props로 리스트와 contentType을 받을 것
const BottomSheetContainer = (props) => {
  const [open, setOpen] = useState(true);
  const focusRef = useRef();
  const contentType = "article"
  const antennaList = props.antennae
 
  

  
  // props.center 로 받아온 좌표로 axios
  let URL = useSelector((state) => state.URL)
  let TOKEN = useSelector((state) => state.TOKEN)
  const [articleList, setArticleList] = useState([])
  const [isAntenna, setIsAntenna] = useState([])
  useMemo(() => {
    axios({
      url: URL + '/posts',
      method: 'get',
      headers: {
        Authorization: `Bearer ${TOKEN}`
      },
      params: {
        lng: props.center.lng,
        lat: props.center.lat,
        area: 1 //TODO: 여기는 안테나의 경우에는 동적할당 생각하기
      }
    })
    .then((res) => {
      setArticleList(res.data)
    })
    .catch((err) => {
      console.log(err)
    })
  }, [props.center]) //TODO: 만약 안되면 오브젝트 풀어서 넣기 
  console.log(isAntenna)

  const dummy = [
    {post_id: 1, title : 'TITLEaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', nickName: 'NICKNAME', date: 'DATE.MM.DD'},
    {post_id: 2, title : 'TITLE', nickName: 'NICKNAME', date: 'DATE.MM.DD'},
    {post_id: 3, title : 'TITLE', nickName: 'NICKNAME', date: 'DATE.MM.DD'},
    {post_id: 4, title : 'TITLE', nickName: 'NICKNAME', date: 'DATE.MM.DD'},
    {post_id: 5, title : 'TITLE', nickName: 'NICKNAME', date: 'DATE.MM.DD'},
    {post_id: 6, title : 'TITLE', nickName: 'NICKNAME', date: 'DATE.MM.DD'},
  ]
  useEffect(() => {
    setIsAntenna(
      antennaList.filter((antenna) =>{
        return antenna.lat === props.center.lat && antenna.lng === props.center.lng
      }
      )
    )
  }, [props.center])
  useEffect(() => {
    // Setting focus is to aid keyboard and screen reader nav when activating this iframe
    focusRef.current.focus();
  }, []);

  return (
    <p ref={focusRef}>
      {/* <button onClick={() => setOpen(open => !open)} ref={focusRef}>
        {open ? "Close" : "Open"}
      </button> */}
      <button>요기</button>
      <BottomSheet
        open={open}
        // 사라지게 하는 부분
        // onDismiss={() => setOpen(false)}
        blocking={false}
        header={
          <div>{isAntenna.length === 1 ? <button>안테나 뽑기</button> : <button>안테나 심기</button>}</div>
        }
        // 첫번쨰가 1차 높이, 두번째가 최대 높이
        snapPoints={({ maxHeight }) => [maxHeight / 4, maxHeight]}
      >
        <div className="forScrollBar">
          {/* dummy => list로 교체 */}
          {dummy.map((dataList) => {
            if (contentType === 'article') {
                return(
                <ArticleListItem articleListItem={dataList}/>
              )}
            else if (contentType === 'adventure') {
              return(
                <AdventureBanner AdventureListItem={dataList}/>
              )}
            })    
          }
        </div>
      </BottomSheet>
    </p>
  );
};

export default BottomSheetContainer;

