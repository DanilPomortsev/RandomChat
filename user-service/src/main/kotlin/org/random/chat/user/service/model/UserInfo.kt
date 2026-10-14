package org.random.chat.user.service.model

import org.random.chat.user.service.jooq.tables.pojos.UserAvatars
import org.random.chat.user.service.jooq.tables.pojos.Users

data class UserInfo(
    val user: Users,
    val avatars: List<UserAvatars>,
)
