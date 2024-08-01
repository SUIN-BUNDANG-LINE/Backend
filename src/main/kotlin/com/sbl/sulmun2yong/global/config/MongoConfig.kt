package com.sbl.sulmun2yong.global.config

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.client.MongoClients
import com.sbl.sulmun2yong.global.converter.BinaryToUUIDConverter
import com.sbl.sulmun2yong.global.converter.UUIDToBinaryConverter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration
import org.springframework.data.mongodb.config.EnableMongoAuditing
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.convert.MongoCustomConversions

@Configuration
@EnableMongoAuditing
class MongoConfig(
    @Value("\${spring.data.mongodb.database}") private val databaseName: String,
    @Value("\${spring.data.mongodb.uri}") private val connectionString: String,
) : AbstractMongoClientConfiguration() {
    override fun getDatabaseName() = databaseName

    @Bean
    override fun mongoClient() =
        MongoClients.create(
            MongoClientSettings.builder()
                .applyConnectionString(ConnectionString(connectionString))
                .build(),
        )

    @Bean
    fun mongoTemplate() = MongoTemplate(mongoClient(), databaseName)

    @Bean
    override fun customConversions(): MongoCustomConversions {
        val converters: MutableList<Converter<*, *>> = ArrayList()
        converters.add(UUIDToBinaryConverter())
        converters.add(BinaryToUUIDConverter())
        return MongoCustomConversions(converters)
    }
}
