package org.random.chat.common.dbqueue

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.support.TransactionTemplate
import ru.yoomoney.tech.dbqueue.api.QueueConsumer
import ru.yoomoney.tech.dbqueue.api.QueueProducer
import ru.yoomoney.tech.dbqueue.api.impl.MonitoringQueueProducer
import ru.yoomoney.tech.dbqueue.api.impl.NoopPayloadTransformer
import ru.yoomoney.tech.dbqueue.api.impl.ShardingQueueProducer
import ru.yoomoney.tech.dbqueue.api.impl.SingleQueueShardRouter
import ru.yoomoney.tech.dbqueue.config.DatabaseDialect
import ru.yoomoney.tech.dbqueue.config.QueueService
import ru.yoomoney.tech.dbqueue.config.QueueShard
import ru.yoomoney.tech.dbqueue.config.QueueShardId
import ru.yoomoney.tech.dbqueue.config.QueueTableSchema
import ru.yoomoney.tech.dbqueue.config.impl.LoggingTaskLifecycleListener
import ru.yoomoney.tech.dbqueue.config.impl.LoggingThreadLifecycleListener
import ru.yoomoney.tech.dbqueue.settings.QueueConfig
import ru.yoomoney.tech.dbqueue.settings.QueueConfigsReader
import ru.yoomoney.tech.dbqueue.settings.QueueId
import ru.yoomoney.tech.dbqueue.spring.dao.SpringDatabaseAccessLayer

@Configuration
class DbQueueConfiguration {
    @Bean
    @ConditionalOnMissingBean
    fun objectMapper(): ObjectMapper {
        return jacksonObjectMapper().registerModule(JavaTimeModule())
    }

    @Bean
    fun queueDatabaseAccessLayer(
        jdbcTemplate: JdbcTemplate,
        transactionTemplate: TransactionTemplate,
    ): SpringDatabaseAccessLayer {
        return SpringDatabaseAccessLayer(
            DatabaseDialect.POSTGRESQL,
            QueueTableSchema.builder().build(),
            jdbcTemplate,
            transactionTemplate,
        )
    }

    @Bean
    fun queueShard(accessLayer: SpringDatabaseAccessLayer): QueueShard<SpringDatabaseAccessLayer> {
        return QueueShard(QueueShardId("main"), accessLayer)
    }

    @Bean
    fun queueConfigs(): Map<QueueId, QueueConfig> {
        val path = ClassPathResource(DbQueue.PROPERTIES).file.toPath()
        return QueueConfigsReader(listOf(path), DbQueue.SETTINGS_PREFIX)
            .parse()
            .associateBy { it.location.queueId }
    }

    @Bean
    fun queueProducers(
        queueConfigs: Map<QueueId, QueueConfig>,
        queueShard: QueueShard<SpringDatabaseAccessLayer>,
    ): QueueProducers {
        val producers = queueConfigs.map { (queueId, config) ->
            val producer: QueueProducer<String> = MonitoringQueueProducer(
                ShardingQueueProducer(
                    config,
                    NoopPayloadTransformer.getInstance(),
                    SingleQueueShardRouter(queueShard),
                ),
                queueId,
            )
            queueId.asString() to producer
        }.toMap()
        return QueueProducers(producers)
    }

    @Bean
    fun dbQueueEnqueueService(
        queueProducers: QueueProducers,
        objectMapper: ObjectMapper,
    ): DbQueueEnqueueService {
        return DbQueueEnqueueService(queueProducers, objectMapper)
    }

    @Bean(destroyMethod = "shutdown")
    fun queueService(
        queueShard: QueueShard<SpringDatabaseAccessLayer>,
        consumers: List<QueueConsumer<*>>,
    ): QueueService {
        val queueService = QueueService(
            listOf(queueShard),
            LoggingThreadLifecycleListener(),
            LoggingTaskLifecycleListener(),
        )
        consumers.forEach { consumer ->
            queueService.registerQueue(consumer)
        }
        queueService.start()
        return queueService
    }
}
