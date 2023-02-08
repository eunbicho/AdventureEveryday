import axios from 'axios'
import ArticleMoreButton from './ArticleMoreButton'
import { useSelector } from 'react-redux'
import { useEffect, useState, useMemo } from 'react'
import style from './ArticleDetail.module.css'
import { AiOutlineHeart, AiFillHeart, AiFillMessage } from  "react-icons/ai"
import { useNavigate } from 'react-router-dom'
import {GrFlag} from 'react-icons/gr'
import {GoRadioTower} from 'react-icons/go'
import {MdPersonOutline} from 'react-icons/md'


function ArticleDetail({article, isFeed}) {
  let URL = useSelector((state) => state.url)
  let TOKEN = useSelector((state) => state.token)
  let USER = useSelector((state) => state.user)

  const navigate = useNavigate()


  // 해당 게시글의 좋아요 수 및 좋아요 여부 알아오기
  const [likes, setLikes] = useState(0)
  const [isLike, setIsLike] = useState(false)
  const getLikes = function() {
    axios({
      url: URL + `/posts/${article.postId}/post-like`,
      method: 'get',
      headers: {
        Authorization: `Bearer ${TOKEN}`
      }
    })
    .then((res) => {
      setLikes(res.data.result.cnt)
      setIsLike(res.data.result.isLike)
    })
    .catch((err) => console.log(err))
  }

  // 해당 게시글의 댓글 및 댓글 수 조회
  const [comments, setComments] = useState([])
  const getComments = function() {
    axios({
      url: URL + `/posts/${article.postId}/comments`,
      method: 'get',
      headers: {
        Authorization: `Bearer ${TOKEN}`
      }
    })
    .then((res) => {
      setComments(res.data.result)
    })
    .catch((err) => console.log(err))
  }

  // 페이지 로드되었을 때 axios되도록 useEffect
  useEffect(() => {
    getComments()
    getLikes()
    if (article.userDetailRes.userId === USER.userId) {
      setIsMe(true)
    } else {
      setIsMe(false)
    }
  }, [])

  // ... 버튼 토글용 + 내 글인지 야부
  const [isOn, setIsOn] = useState(false)
  function toggle() {
    setIsOn((prev) => !prev)
  }
  const [isMe, setIsMe] = useState(false)

  // 좋아요
  const doLike = function() {
    axios({
      url: URL + `/posts/${article.postId}/post-like`,
      method: 'post',
      headers: {
        Authorization: `Bearer ${TOKEN}`
      },
    })
    .then((res) => {
      getLikes()
    })
    .catch((err) => console.log(err))
  }

  // 좋아요 취소
  const unDoLike = function() {
    axios({
      url: URL + `/posts/${article.postId}/post-like`,
      method: 'delete',
      headers: {
        Authorization: `Bearer ${TOKEN}`
      },
    })
    .then((res) => {
      getLikes()
    })
    .catch((err) => console.log(err))
  }

  // 피드에서는 상세보기로 이동 가능
  const goToDetail = function() {
    if (isFeed) {
      navigate(`/article/${article.postId}`)
  }}

  
  


  



  return(

    <div className={style.articleContainer} onClick={() => {
      if (isOn) {
        setIsOn(false)
      } 
    }
    }>
      
      
      <div className={style.userInfo}>
        <div className={style.profileAndNicknameAndw3w} onClick={() => {navigate(`/profile/${article.userDetailRes.userId}`)}}>
          <div className={style.profileContainer}>
            <img className={style.profile} src={article.userDetailRes.photoUrl ? article.userDetailRes.photoUrl : 'defaultProfile.jpg'}/>
          </div>
          <div className={style.nicknameAndw3w}>
            <div className={style.nickname}>{article.userDetailRes.nickname}</div>
            <div className={style.w3w}>{article.w3w}</div>
          </div>
        </div>
        <div calssName={style.iconHolder}>
          {article.isChallenge !== 0 && <GrFlag size={33} className={style.icon}/>}
          {article.isAntenna !== 0 && <GoRadioTower size={33} className={style.icon}/>}
          {article.isFollowing !== !0 && <MdPersonOutline size={36} className={style.isFollowingicon}/>}
        </div>
        {!isFeed && <div className={style.moreButton}><ArticleMoreButton article={article} toggle={toggle} isOn={isOn} isMe={isMe}/></div>}
      </div>
      <img className={style.photo} src={article.photoUrl} onClick={() => {goToDetail()}} />
      <div className={style.articleInfo}>
        <div className={style.title} onClick={() => {goToDetail()}}>{article.title}</div> 
        <div className={style.content} onClick={() => {goToDetail()}}>{article.content}</div>
        <div className={style.time}>{article.createTime && article.createTime.substr(0,10)}</div> {/*TODO: 시간 차이 구하는 알고리즘.. */ }
        <div className={style.likeAndComment}>
          <div className={style.like}>{isLike ? <AiFillHeart style={{fontSize:"larger", marginRight:"1vw"}} onClick={()=>{unDoLike()}}/> : <AiOutlineHeart style={{fontSize:"larger", marginRight:"1vw"}} onClick={()=>{doLike()}}/>}{likes}</div>
          <div className={style.comment} onClick={() => {navigate(`/article/${article.postId}/comments`)}}><AiFillMessage style={{fontSize:"larger", marginRight:"1vw"}} />{comments.length}</div>
        </div>
      </div>

    </div>

  )
}

export default ArticleDetail