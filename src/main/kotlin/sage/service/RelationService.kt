package sage.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import sage.domain.commons.IdCommons
import sage.entity.Follow
import sage.entity.Tag
import sage.entity.User
import sage.entity.UserTag
import sage.transfer.UserLabel

@Service
class RelationService
@Autowired constructor(private val notifService: NotificationService) {
  private val logger = LoggerFactory.getLogger(javaClass)

  @Suppress("NAME_SHADOWING")
      /**
   * Act as 'follow' or 'editFollow'
   * @param userId The acting user
   * *
   * @param targetId The target user to follow
   * *
   * @param reason The reason of following
   * *
   * @param tagIds The tags to follow
   * *
   * @param includeNew If auto-include new tags
   * *
   * @param includeAll If include all tags, ignoring selected tags
   * *
   * @param userTagOffset The offset for includeNew. Fetch from DB if null
   */
  fun follow(userId: Long, targetId: Long, reason: String?, tagIds: Collection<Long>,
             includeNew: Boolean, includeAll: Boolean, userTagOffset: Long?) {
    if (IdCommons.equal(userId, targetId)) {
      logger.warn("user {} should not follow himself!", userId)
      return
    }
    val follow = Follow.find(userId, targetId)
    val followedTags = Tag.multiGet(tagIds)

    val userTagOffset = userTagOffset ?: UserTag.lastIdByUser(targetId)

    if (follow == null) {
      Follow(User.ref(userId), User.ref(targetId),
          reason?:"", followedTags, includeNew, includeAll, userTagOffset).save()
      notifService.followed(targetId, userId)
    } else {
      follow.tags = followedTags
      if (reason != null) follow.reason = reason
      follow.isIncludeNew = includeNew
      follow.isIncludeAll = includeAll
      follow.userTagOffset = userTagOffset
      follow.update()
    }
  }

  fun follow(userId: Long, targetId: Long, tagIds: Collection<Long>) {
    follow(userId, targetId, "", tagIds, true, false, 0L)
  }

  fun unfollow(userId: Long, targetId: Long) {
    val follow = Follow.find(userId, targetId)
    if (follow != null) {
      follow.delete()
    } else {
      logger.warn("user {} should not unfollow user {} duplicately!", userId, targetId)
    }
  }

  fun followings(userId: Long): List<Follow> {
    return Follow.followings(userId)
  }

  fun followers(userId: Long): List<Follow> {
    return Follow.followers(userId)
  }

  fun friends(userId: Long): List<UserLabel> {
    val followingUsers = followings(userId).map { it.target }
    val followerUsers = followers(userId).map { it.source }
    return followingUsers.intersect(followerUsers).map(User::toUserLabel)
  }
}
