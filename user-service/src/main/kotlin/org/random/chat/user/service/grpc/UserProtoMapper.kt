package org.random.chat.user.service.grpc

import org.random.chat.user.api.Avatar
import org.random.chat.user.api.User
import org.random.chat.user.api.UserStatus as ProtoUserStatus
import org.random.chat.user.service.domain.UserStatus
import org.random.chat.user.service.jooq.tables.pojos.UserAvatars
import org.random.chat.user.service.model.UserInfo

internal fun UserInfo.toProto(): User = User.newBuilder()
    .setId(checkNotNull(user.id))
    .setEmail(user.email)
    .setNickname(user.nickname)
    .setStatus(user.status.toProto())
    .addAllAvatars(avatars.map { it.toProto() })
    .build()

private fun UserAvatars.toProto(): Avatar = Avatar.newBuilder()
    .setId(checkNotNull(id))
    .setUrl(url)
    .build()

private fun UserStatus.toProto(): ProtoUserStatus = when (this) {
    UserStatus.ACTIVE -> ProtoUserStatus.USER_STATUS_ACTIVE
    UserStatus.BLOCKED -> ProtoUserStatus.USER_STATUS_BLOCKED
}
