package io.playwright.kotlin.connection

import io.netty.bootstrap.Bootstrap
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.LineBasedFrameDecoder
import io.netty.handler.codec.string.StringDecoder
import io.netty.handler.codec.string.StringEncoder
import io.playwright.kotlin.PlaywrightException
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicBoolean

class Transport(
    private val host: String,
    private val port: Int,
    private val messageHandler: (ResponseMessage) -> Unit,
    private val failureHandler: (Throwable) -> Unit = {}
) {
    private val group: NioEventLoopGroup = NioEventLoopGroup(1)
    private val shutdown = AtomicBoolean(false)
    private var channel: Channel? = null

    fun connect(): Channel {
        check(!shutdown.get()) { "Transport is already shut down" }

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
                        addLast("inboundHandler", InboundHandler(messageHandler, failureHandler))

                        // Outbound: Request -> json string -> bytes
                        addLast("stringEncoder", StringEncoder(StandardCharsets.UTF_8))
                        addLast("jsonEncoder", JsonRequestEncoder())
                    }
                }
            })

        return try {
            val future = bootstrap.connect(host, port).sync()
            channel = future.channel()
            future.channel()
        } catch (error: Throwable) {
            shutdown()
            throw error
        }
    }

    fun shutdown() {
        if (!shutdown.compareAndSet(false, true)) return
        try {
            channel?.close()?.syncUninterruptibly()
        } finally {
            group.shutdownGracefully().syncUninterruptibly()
        }
    }

    private class InboundHandler(
        private val messageHandler: (ResponseMessage) -> Unit,
        private val failureHandler: (Throwable) -> Unit
    ) : SimpleChannelInboundHandler<ResponseMessage>() {
        override fun channelRead0(ctx: ChannelHandlerContext, msg: ResponseMessage) {
            messageHandler(msg)
        }

        override fun channelInactive(ctx: ChannelHandlerContext) {
            failureHandler(PlaywrightException("Transport connection closed"))
            super.channelInactive(ctx)
        }

        override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
            failureHandler(cause)
            ctx.close()
        }
    }
}
