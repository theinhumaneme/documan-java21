// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.kalyan.documan.service;

import com.kalyan.documan.dao.CommentDao;
import com.kalyan.documan.dao.PostDao;
import com.kalyan.documan.dao.UserDao;
import com.kalyan.documan.entity.Comment;
import com.kalyan.documan.entity.Post;
import com.kalyan.documan.entity.User;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class VoteService {
  private static final Logger log = LoggerFactory.getLogger(VoteService.class);
  private final PostDao postDao;
  private final CommentDao commentDao;
  private final UserDao userDao;

  @Autowired
  public VoteService(PostDao postdao, CommentDao commentdao, UserDao userdao) {
    this.postDao = postdao;
    this.commentDao = commentdao;
    this.userDao = userdao;
  }

  public Optional<Comment> voteCommment(Integer userId, Integer commentId, String voteType) {
    Optional<User> user = userDao.findById(userId);
    Optional<Comment> comment = commentDao.findById(commentId);
    if (user.isPresent() && comment.isPresent()) {
      Comment voteComment = comment.get();
      return switch (voteType) {
        case "upvote" -> applyVote(voteComment, user.get(), true);
        case "downvote" -> applyVote(voteComment, user.get(), false);
        default -> Optional.empty();
      };
    } else {
      return Optional.empty();
    }
  }

  public Optional<Post> votePost(Integer userId, Integer postId, String voteType) {
    Optional<User> user = userDao.findById(userId);
    Optional<Post> post = postDao.findById(postId);
    if (user.isPresent() && post.isPresent()) {
      Post votePost = post.get();
      return switch (voteType) {
        case "upvote" -> applyVote(votePost, user.get(), true);
        case "downvote" -> applyVote(votePost, user.get(), false);
        default -> Optional.empty();
      };
    } else {
      return Optional.empty();
    }
  }

  private <T> Optional<T> applyVote(T votable, User user, boolean isUpvote) {
    try {
      // Check if the votable is a Post
      if (votable instanceof Comment comment) {
        List<User> upvotedUsers = comment.getUpvotedUsers();
        List<User> downvotedUsers = comment.getDownvotedUsers();

        if (isUpvote) {
          downvotedUsers.remove(user);
          upvotedUsers.add(user);
        } else {
          upvotedUsers.remove(user);
          downvotedUsers.add(user);
        }
        comment.setUpvotedUsers(upvotedUsers);
        comment.setDownvotedUsers(downvotedUsers);
        return Optional.of((T) commentDao.save(comment));
      }

      // Check if the votable is a Post
      if (votable instanceof Post post) {
        List<User> upvotedUsers = post.getUpvotedUsers();
        List<User> downvotedUsers = post.getDownvotedUsers();

        if (isUpvote) {
          downvotedUsers.remove(user);
          upvotedUsers.add(user);
        } else {
          upvotedUsers.remove(user);
          downvotedUsers.add(user);
        }
        post.setUpvotedUsers(upvotedUsers);
        post.setDownvotedUsers(downvotedUsers);
        return Optional.of((T) postDao.save(post));
      }
    } catch (Exception e) {
      log.error(e.toString());
    }
    return Optional.empty();
  }

  public Optional<Comment> downvoteComment(Integer userId, Integer commentId) {
    Optional<User> user = userDao.findById(userId);
    Optional<Comment> comment = commentDao.findById(commentId);
    if (user.isPresent() && comment.isPresent()) {
      Comment upvoteComment = comment.get();
      List<User> upVotedUsers = upvoteComment.getDownvotedUsers();
      if (upVotedUsers.stream().noneMatch(c -> c.getId().equals(userId))) {
        upVotedUsers.add(user.get());
        upvoteComment.setDownvotedUsers(upVotedUsers);
      }
      return Optional.of(upvoteComment);
    } else {
      return Optional.empty();
    }
  }
}
