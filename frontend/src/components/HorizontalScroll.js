import React from "react";
import { ScrollMenu } from "react-horizontal-scrolling-menu";

import ArticleListItem from "./ArticleListItem";
import AdventureBanner from "./Adventure/AdventureBanner";

import style from "./HorizontalScroll.module.css";

// props로 contentType 지정해서 넘겨줄 것. adventure/article
function HorizontalScroll(props) {
  const dummy = [
    {
      post_id: 1,
      title: "TITLEaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
      nickName: "NICKNAME",
      date: "DATE.MM.DD",
    },
    { post_id: 2, title: "TITLE", nickName: "NICKNAME", date: "DATE.MM.DD" },
    { post_id: 3, title: "TITLE", nickName: "NICKNAME", date: "DATE.MM.DD" },
    { post_id: 4, title: "TITLE", nickName: "NICKNAME", date: "DATE.MM.DD" },
    { post_id: 5, title: "TITLE", nickName: "NICKNAME", date: "DATE.MM.DD" },
    { post_id: 6, title: "TITLE", nickName: "NICKNAME", date: "DATE.MM.DD" },
  ];

  const listItem = props.contentType;

  switch (listItem) {
    case "adventure":
      return (
        <div className={style.articleList}>
          <ScrollMenu>
            {dummy.map((articleListItem) => {
              return (
                <AdventureBanner
                  key={articleListItem}
                  articleListItem={articleListItem}
                />
              );
            })}
          </ScrollMenu>
        </div>
      );
    case "article":
      return (
        <div className={style.articleList}>
          <ScrollMenu>
            {dummy.map((articleListItem) => {
              return <ArticleListItem articleListItem={articleListItem} />;
            })}
          </ScrollMenu>
        </div>
      );
    default:
      return <div></div>;
  }
  // props.articleList.map(function(articleListItem){
  //   return (
  //     <ArticleListItem articleListItem={articleListItem}/>
  //   )
  // })
}

export default HorizontalScroll;
