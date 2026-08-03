package com.wealthos.fxrate.application

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.slf4j.LoggerFactory

@Component
@ConditionalOnProperty(name = ["wealthos.fx.scheduling-enabled"], havingValue = "true", matchIfMissing = true)
class FxRateSynchronization(
    private val sync: SyncFxRates,
) : ApplicationRunner {
    override fun run(args: ApplicationArguments) {
        catchUp()
    }

    @Scheduled(cron = "0 30 18 * * MON-FRI", zone = "Asia/Taipei")
    fun scheduledSync() {
        catchUp()
    }

    private fun catchUp() {
        val today = java.time.LocalDate.now(TAIPEI)
        try {
            sync.catchUp(today)
        } catch (exception: RuntimeException) {
            logger.warn("CBC FX synchronization failed; retained persisted rates", exception)
        }
    }

    private companion object {
        val TAIPEI = java.time.ZoneId.of("Asia/Taipei")
        val logger = LoggerFactory.getLogger(FxRateSynchronization::class.java)
    }
}
