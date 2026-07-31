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
                      and table_name in ('assets', 'liabilities')
                    order by table_name
                    """.trimIndent(),
                    String::class.java,
                )

            assertEquals(listOf("assets", "liabilities"), tables)
            assertFailsWith<DataIntegrityViolationException> {
                jdbc.update(
                    "insert into assets (id, name, asset_type, liquidity) values (?, ?, ?, ?)",
                    UUID.randomUUID(),
                    " ",
                    "CASH",
                    "LIQUID",
                )
            }
        }

        companion object {
            @Container
            @ServiceConnection
            @JvmStatic
            val postgres = PostgreSQLContainer("postgres:18-alpine")
        }
    }
