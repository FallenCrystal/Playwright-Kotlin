package io.playwright.kotlin.connection

import io.netty.bootstrap.Bootstrap
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.LineBasedFrameDecoder
import io.netty.handler.codec.string.StringDecoder
import io.netty.handler.codec.string.StringEncoder
import java.nio.charset.StandardCharsets

class Transport(
    private val host: String,
    private val port: Int,
    private val messageHandler: (ResponseMessage) -> Unit
) {
    private val group: NioEventLoopGroup = NioEventLoopGroup(1)
    private var channel: Channel? = null

    fun connect(): Channel {
        val bootstrap = Bootstrap()
            .group(group)
            .channel(NioSocketChannel::class.java)
            .option(ChannelOption.TCP_NODELAY, true)
            .option(ChannelOption.SO_KEEPALIVE, true)
            .handler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(ch: SocketChannel) {
                    ch.pipeline().apply {
                        // Inbound: bytes -> lines -> string -> ResponseMessage
                        addLast("frameDecoder", LineBasedFrameDecoder(16 * 1024 * 1024)) // 16MB max
                        addLast("stringDecoder", StringDecoder(StandardCharsets.UTF_8))
                        addLast("jsonDecoder", JsonResponseDecoder())
                        addLast("inboundHandler", InboundHandler(messageHandler))

                        // Outbound: Request -> json string -> bytes
                        addLast("stringEncoder", StringEncoder(StandardCharsets.UTF_8))
                        addLast("jsonEncoder", JsonRequestEncoder())
                    }
                }
            })

        val future = bootstrap.connect(host, port).sync()
        channel = future.channel()
        return future.channel()
    }

    fun shutdown() {
        try {
            channel?.close()?.sync()
        } finally {
            group.shutdownGracefully()
        }
    }

    private class InboundHandler(
        private val messageHandler: (ResponseMessage) -> Unit
    ) : SimpleChannelInboundHandler<ResponseMessage>() {
        override fun channelRead0(ctx: ChannelHandlerContext, msg: ResponseMessage) {
            messageHandler(msg)
        }

        override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
            cause.printStackTrace()
            ctx.close()
        }
    }
}
