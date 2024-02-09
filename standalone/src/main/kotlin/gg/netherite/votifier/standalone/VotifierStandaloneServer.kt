package gg.netherite.votifier.standalone

import com.vexsoftware.votifier.model.Vote
import com.vexsoftware.votifier.net.VotifierServerBootstrap
import com.vexsoftware.votifier.net.VotifierSession
import com.vexsoftware.votifier.platform.LoggingAdapter
import com.vexsoftware.votifier.platform.VotifierPlugin
import com.vexsoftware.votifier.platform.scheduler.VotifierScheduler
import gg.netherite.votifier.standalone.logging.StandaloneLoggingAdapter
import gg.netherite.votifier.standalone.redis.RedisForwarding
import gg.netherite.votifier.standalone.scheduler.VotifierStandaloneScheduler
import java.security.Key
import java.security.KeyPair
import kotlin.concurrent.thread

class VotifierStandaloneServer : VotifierPlugin {

    private lateinit var config: VotifierStandaloneConfig
    private lateinit var redisForwarding: RedisForwarding
    private lateinit var bootstrap: VotifierServerBootstrap

    @Volatile
    private var running = false

    fun start(config: VotifierStandaloneConfig) {
        this.running = true
        this.config = config

        /**
         * REDIS_URI="redis://:Netherite1Redis@192.168.0.1:6379/0"
         * VOTIFIER_PORT=7084
         */

        thread(start = true) {
            try {
                VotifierStandalone.LOGGER.info("Redis URI: ${config.redisUri}")
                this.redisForwarding = RedisForwarding(config)
                this.bootstrap = VotifierServerBootstrap(config.host, config.port, this, false)
                this.bootstrap.start { error: Throwable? ->
                    if (error != null) {
                        VotifierStandalone.LOGGER.error("Error in VotifierServerBootstrap", error)
                    } else {
                        VotifierStandalone.LOGGER.info("Successfully bound to ${config.host}:${config.port}")
                    }
                }

                while (running) {
                    Thread.sleep(1000)
                }
            } catch (e: Exception) {
                VotifierStandalone.LOGGER.error("Error in VotifierStandaloneServer", e)
            }
        }
    }

    fun shutdown() {
        running = false
        redisForwarding.halt()
    }

    override fun onVoteReceived(
        vote: Vote?,
        protocolVersion: VotifierSession.ProtocolVersion?,
        remoteAddress: String?
    ) {
        VotifierStandalone.LOGGER.info("Received vote from $remoteAddress: $vote")
        vote?.let { redisForwarding.forward(it) }
    }

    override fun getTokens(): MutableMap<String, Key> = mutableMapOf()
    override fun getProtocolV1Key(): KeyPair = config.keyPair
    override fun getPluginLogger(): LoggingAdapter = StandaloneLoggingAdapter
    override fun getScheduler(): VotifierScheduler = VotifierStandaloneScheduler
}