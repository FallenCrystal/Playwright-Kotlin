package io.playwright.kotlin.connection

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToMessageDecoder
import io.netty.handler.codec.MessageToMessageEncoder
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val protocolJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    isLenient = true
}

class JsonRequestEncoder : MessageToMessageEncoder<Request>() {
    override fun encode(ctx: ChannelHandlerContext, msg: Request, out: MutableList<Any>) {
        val json = protocolJson.encodeToString(msg)
        out.add("$json\n")
    }
}

class JsonResponseDecoder : MessageToMessageDecoder<String>() {
    override fun decode(ctx: ChannelHandlerContext, msg: String, out: MutableList<Any>) {
        val trimmed = msg.trim()
        if (trimmed.isEmpty()) return
        val response = protocolJson.decodeFromString<ResponseMessage>(trimmed)
        out.add(response)
    }
}
