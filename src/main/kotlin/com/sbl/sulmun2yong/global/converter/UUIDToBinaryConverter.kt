package com.sbl.sulmun2yong.global.converter

import org.bson.types.Binary
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import java.nio.ByteBuffer
import java.util.UUID

@ReadingConverter
class UUIDToBinaryConverter : Converter<Binary, UUID> {
    override fun convert(source: Binary): UUID {
        val byteBuffer = ByteBuffer.wrap(source.data)
        val mostSigBits = byteBuffer.long
        val leastSigBits = byteBuffer.long
        return UUID(mostSigBits, leastSigBits)
    }
}
