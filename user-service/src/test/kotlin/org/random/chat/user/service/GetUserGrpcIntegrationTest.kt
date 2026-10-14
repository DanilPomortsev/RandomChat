package org.random.chat.user.service

import io.grpc.Status
import io.grpc.StatusRuntimeException
import java.nio.file.Path
import java.sql.DriverManager
import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.DirectoryResourceAccessor
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.random.chat.common.dbqueue.DbQueue
import org.random.chat.user.api.GetUserRequest
import org.random.chat.user.api.SaveUserRequest
import org.random.chat.user.api.UserServiceGrpc.UserServiceBlockingStub
import org.random.chat.user.api.UserStatus as ProtoUserStatus
import org.random.chat.user.service.domain.AvatarStatus as DomainAvatarStatus
import org.random.chat.user.service.domain.UserStatus
import org.random.chat.user.service.jooq.tables.references.USERS
import org.random.chat.user.service.jooq.tables.references.USER_AVATARS
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureTestGrpcTransport
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.grpc.client.ImportGrpcClients
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.postgresql.PostgreSQLContainer

@SpringBootTest
@AutoConfigureTestGrpcTransport
@ImportGrpcClients(types = [UserServiceBlockingStub::class])
class GetUserGrpcIntegrationTest(
    @Autowired private val stub: UserServiceBlockingStub,
    @Autowired private val dsl: DSLContext,
) {
    @BeforeEach
    fun clean() {
        dsl.deleteFrom(DSL.table(DbQueue.TABLE)).execute()
        dsl.deleteFrom(USER_AVATARS).execute()
        dsl.deleteFrom(USERS).execute()
    }

    @Test
    fun `returns the user and only validated avatars`() {
        val userId = insertUser(email = "ada@example.com", nickname = "ada")
        insertAvatar(userId, "https://cdn.example/old.png", DomainAvatarStatus.DISCARDED)
        val firstValidated = insertAvatar(userId, "https://cdn.example/one.png", DomainAvatarStatus.VALIDATED)
        insertAvatar(userId, "https://cdn.example/pending.png", DomainAvatarStatus.VALIDATION_IN_PROGRESS)
        val secondValidated = insertAvatar(userId, "https://cdn.example/two.png", DomainAvatarStatus.VALIDATED)

        val response = stub.getUser(GetUserRequest.newBuilder().setId(userId).build())

        assertThat(response.id).isEqualTo(userId)
        assertThat(response.email).isEqualTo("ada@example.com")
        assertThat(response.nickname).isEqualTo("ada")
        assertThat(response.status).isEqualTo(ProtoUserStatus.USER_STATUS_ACTIVE)
        assertThat(response.avatarsList.map { it.id }).containsExactly(firstValidated, secondValidated)
        assertThat(response.avatarsList.map { it.url })
            .containsExactly("https://cdn.example/one.png", "https://cdn.example/two.png")
    }

    @Test
    fun `returns not found when the user does not exist`() {
        assertThatThrownBy {
            stub.getUser(GetUserRequest.newBuilder().setId(404).build())
        }.isInstanceOf(StatusRuntimeException::class.java)
            .extracting { exception -> (exception as StatusRuntimeException).status.code }
            .isEqualTo(Status.Code.NOT_FOUND)
    }

    @Test
    fun `saves user avatar and validates it through db-queue`() {
        val saved = stub.saveUser(
            SaveUserRequest.newBuilder()
                .setEmail("bob@example.com")
                .setNickname("bob")
                .setAvatarUrl("https://cdn.example/bob.png")
                .build(),
        )

        assertThat(saved.id).isPositive()

        val userId = saved.id
        val avatarId = checkNotNull(
            dsl.select(USER_AVATARS.ID)
                .from(USER_AVATARS)
                .where(USER_AVATARS.USER_ID.eq(userId))
                .fetchOne(USER_AVATARS.ID),
        )

        awaitValidatedAvatar(userId, avatarId)

        val loaded = stub.getUser(GetUserRequest.newBuilder().setId(userId).build())
        assertThat(loaded.avatarsList.map { it.id }).containsExactly(avatarId)
        assertThat(loaded.avatarsList.map { it.url }).containsExactly("https://cdn.example/bob.png")
    }

    private fun awaitValidatedAvatar(userId: Long, avatarId: Long) {
        val deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5)
        while (System.nanoTime() < deadline) {
            val status = dsl.fetchOne(USER_AVATARS, USER_AVATARS.ID.eq(avatarId))?.status
            if (status == DomainAvatarStatus.VALIDATED) {
                val loaded = stub.getUser(GetUserRequest.newBuilder().setId(userId).build())
                if (loaded.avatarsList.any { it.id == avatarId }) {
                    return
                }
            }
            Thread.sleep(50)
        }
        error("Avatar $avatarId was not validated in time")
    }

    private fun insertUser(email: String, nickname: String): Long =
        checkNotNull(
            dsl.insertInto(USERS)
                .set(USERS.EMAIL, email)
                .set(USERS.NICKNAME, nickname)
                .set(USERS.STATUS, UserStatus.ACTIVE)
                .returningResult(USERS.ID)
                .fetchSingle(USERS.ID),
        )

    private fun insertAvatar(userId: Long, url: String, status: DomainAvatarStatus): Long =
        checkNotNull(
            dsl.insertInto(USER_AVATARS)
                .set(USER_AVATARS.USER_ID, userId)
                .set(USER_AVATARS.URL, url)
                .set(USER_AVATARS.STATUS, status)
                .returningResult(USER_AVATARS.ID)
                .fetchSingle(USER_AVATARS.ID),
        )

    companion object {
        private val postgres = PostgreSQLContainer("postgres:17").apply { start() }

        init {
            migrate()
        }

        @JvmStatic
        @DynamicPropertySource
        fun datasource(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
        }

        private fun migrate() {
            val changelogDirectory = Path.of("db/changelog").toAbsolutePath()
            DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use { connection ->
                val database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(JdbcConnection(connection))
                Liquibase(
                    "db.changelog-master.yaml",
                    DirectoryResourceAccessor(changelogDirectory),
                    database,
                ).use { liquibase ->
                    liquibase.update("")
                }
            }
        }
    }
}
