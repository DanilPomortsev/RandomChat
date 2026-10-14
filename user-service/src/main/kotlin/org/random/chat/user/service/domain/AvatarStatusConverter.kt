package org.random.chat.user.service.domain

import org.jooq.impl.AbstractConverter

class AvatarStatusConverter : AbstractConverter<String, AvatarStatus>(String::class.java, AvatarStatus::class.java) {
    override fun from(databaseObject: String?): AvatarStatus? =
        databaseObject?.let { AvatarStatus.valueOf(it.uppercase()) }

    override fun to(userObject: AvatarStatus?): String? =
        userObject?.name?.lowercase()
}
