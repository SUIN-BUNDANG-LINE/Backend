package com.sbl.sulmun2yong.global.converter

import org.bson.BsonBinarySubType
import org.bson.types.Binary
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.WritingConverter
import java.nio.ByteBuffer
import java.util.UUID

@WritingConverter
class BinaryToUUIDConverter : Converter<UUID, Binary> {
    override fun convert(source: UUID): Binary {
        val byteBuffer = ByteBuffer.wrap(ByteArray(16))
        byteBuffer.putLong(source.mostSignificantBits)
        byteBuffer.putLong(source.leastSignificantBits)
        return Binary(BsonBinarySubType.UUID_STANDARD, byteBuffer.array())
    }
}
