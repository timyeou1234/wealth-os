package com.wealthos.adapter.persistence

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.jdbc.core.JdbcTemplate
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class PostgresMigrationTest
    @Autowired
    constructor(
        private val jdbc: JdbcTemplate,
    ) {
        @Test
        fun `flyway creates current position tables and constraints`() {
            val tables =
                jdbc.queryForList(
                    """
                    select table_name
                    from information_schema.tables
                    where table_schema = 'public'
                      and table_name in (
                          'assets',
                          'liabilities',
                          'snapshots',
                          'fx_rates',
                          'snapshot_asset_positions',
                          'snapshot_liability_positions',
                          'users'
                      )
                    order by table_name
                    """.trimIndent(),
                    String::class.java,
                )

            assertEquals(
                listOf(
                    "assets",
                    "fx_rates",
                    "liabilities",
                    "snapshot_asset_positions",
                    "snapshot_liability_positions",
                    "snapshots",
                    "users",
                ),
                tables,
            )
            val ownerId = insertUser()
            assertFailsWith<DataIntegrityViolationException> {
                jdbc.update(
                    "insert into assets (id, owner_id, name, asset_type, liquidity) values (?, ?, ?, ?, ?)",
                    UUID.randomUUID(),
                    ownerId,
                    " ",
                    "CASH",
                    "LIQUID",
                )
            }
        }

        @Test
        fun `snapshot schema preserves immutable facts and a linear correction chain`() {
            val originalId = UUID.randomUUID()
            val correctionId = UUID.randomUUID()
            val asOf = Instant.parse("2026-01-31T00:00:00Z")
            val ownerId = insertUser()

            insertSnapshot(ownerId, originalId, asOf, asOf)
            insertSnapshot(
                ownerId = ownerId,
                id = correctionId,
                asOf = asOf,
                recordedAt = asOf.plusSeconds(60),
                supersedesId = originalId,
                correctionReason = "Corrected bank balance",
            )

            assertFailsWith<DataIntegrityViolationException> {
                insertSnapshot(
                    ownerId = ownerId,
                    id = UUID.randomUUID(),
                    asOf = asOf,
                    recordedAt = asOf.plusSeconds(120),
                    supersedesId = originalId,
                    correctionReason = "Competing correction",
                )
            }
            assertFailsWith<DataIntegrityViolationException> {
                insertSnapshot(
                    ownerId = ownerId,
                    id = UUID.randomUUID(),
                    asOf = asOf,
                    recordedAt = asOf,
                    correctionReason = "Reason without predecessor",
                )
            }

            val assetId = UUID.randomUUID()
            insertAssetPosition(originalId, assetId, asOf)

            assertFailsWith<DataIntegrityViolationException> {
                jdbc.update(
                    """
                    update snapshot_asset_positions
                    set applied_original_amount = 100,
                        applied_original_currency = 'USD',
                        applied_rate = 32.292,
                        applied_rate_date = '2026-01-30',
                        applied_provider = 'USER',
                        applied_rate_type = 'USER_DECLARED',
                        applied_rounding_mode = 'HALF_EVEN'
                    where snapshot_id = ? and asset_id = ?
                    """.trimIndent(),
                    originalId,
                    assetId,
                )
            }

            assertFailsWith<DataIntegrityViolationException> {
                insertAssetPosition(originalId, assetId, asOf)
            }
        }

        private fun insertAssetPosition(
            snapshotId: UUID,
            assetId: UUID,
            effectiveAt: Instant,
        ) {
            jdbc.update(
                """
                insert into snapshot_asset_positions (
                    snapshot_id,
                    asset_id,
                    name,
                    asset_type,
                    liquidity,
                    amount,
                    currency,
                    effective_at,
                    source
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                snapshotId,
                assetId,
                "Emergency fund",
                "CASH",
                "LIQUID",
                BigDecimal("1000.00"),
                "USD",
                effectiveAt.atOffset(ZoneOffset.UTC),
                "Manual entry",
            )
        }

        private fun insertSnapshot(
            ownerId: UUID,
            id: UUID,
            asOf: Instant,
            recordedAt: Instant,
            supersedesId: UUID? = null,
            correctionReason: String? = null,
        ) {
            jdbc.update(
                """
                insert into snapshots (
                    id,
                    owner_id,
                    as_of,
                    recorded_at,
                    supersedes_id,
                    correction_reason
                ) values (?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                id,
                ownerId,
                asOf.atOffset(ZoneOffset.UTC),
                recordedAt.atOffset(ZoneOffset.UTC),
                supersedesId,
                correctionReason,
            )
        }

        private fun insertUser(): UUID {
            val id = UUID.randomUUID()
            jdbc.update(
                "insert into users (id, issuer, subject, email) values (?, ?, ?, ?)",
                id,
                "https://migration-test.example/",
                UUID.randomUUID().toString(),
                "migration-test@example.com",
            )
            return id
        }

        companion object {
            @Container
            @ServiceConnection
            @JvmStatic
            val postgres = PostgreSQLContainer("postgres:18-alpine")
        }
    }
