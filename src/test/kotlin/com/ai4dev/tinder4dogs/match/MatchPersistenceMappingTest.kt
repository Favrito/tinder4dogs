package com.ai4dev.tinder4dogs.match

import jakarta.persistence.Column
import jakarta.persistence.Table
import java.lang.Long.TYPE as LONG
import java.time.Instant
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

/**
 * What these tests pin, and why they are worth having without a database.
 *
 * `ddl-auto: validate` only speaks up when the application starts against a
 * real schema, and `mise run test` starts neither. So the two things that make
 * the persistence layer correct — that the mappings name the same tables and
 * columns as migration `005`, and that the conflict-free insert names the
 * constraint the migration actually created — would otherwise be pinned by
 * nothing at all until someone ran the service.
 *
 * Each assertion below fails on a concrete mistake: a renamed column, a lost
 * unique constraint, a derived query Spring Data would try to invent at
 * startup, or an insert that conflicts on the wrong constraint.
 */
class MatchPersistenceMappingTest {

    private val migration: String = requireNotNull(
        javaClass.getResource("/db/changelog/changes/005-create-matching.sql"),
    ) { "migration 005-create-matching.sql must be on the classpath" }.readText()

    private fun columnOf(type: Class<*>, property: String): String =
        requireNotNull(type.getDeclaredField(property).getAnnotation(Column::class.java)) {
            "$property must map to an explicit column"
        }.name

    // ── Decision: one slot per directed pair ────────────────────────────────

    @Test
    fun `a decision is a like or a pass and nothing else`() {
        assertThat(DecisionKind.entries.map { it.name }).containsExactly("LIKE", "PASS")
    }

    @Test
    fun `the decision mapping names the table and columns the migration created`() {
        val table = requireNotNull(Decision::class.java.getAnnotation(Table::class.java))

        assertThat(table.name).isEqualTo("dog_decision")
        assertThat(columnOf(Decision::class.java, "actorId")).isEqualTo("actor_dog_id")
        assertThat(columnOf(Decision::class.java, "targetId")).isEqualTo("target_dog_id")
        assertThat(columnOf(Decision::class.java, "kind")).isEqualTo("decision")

        assertThat(migration)
            .contains("CREATE TABLE dog_decision")
            .contains("actor_dog_id")
            .contains("target_dog_id")
            .contains("decision")
    }

    @Test
    fun `one directed pair has one decision slot, named as the migration names it`() {
        val table = requireNotNull(Decision::class.java.getAnnotation(Table::class.java))
        val unique = table.uniqueConstraints.single()

        assertThat(unique.name).isEqualTo("uk_dog_decision_actor_target")
        assertThat(unique.columnNames).containsExactly("actor_dog_id", "target_dog_id")
        assertThat(migration).contains(
            "CONSTRAINT uk_dog_decision_actor_target UNIQUE (actor_dog_id, target_dog_id)",
        )
    }

    @Test
    fun `the decision adapter implements the narrow port over JPA`() {
        assertThat(Decisions::class.java.isAssignableFrom(DecisionRepository::class.java)).isTrue()
        assertThat(JpaRepository::class.java.isAssignableFrom(DecisionRepository::class.java)).isTrue()
    }

    @Test
    fun `the decision lookups are answered by derived queries, not invented from the port names`() {
        // Spring Data would try to derive a query for any abstract method it
        // does not recognise, and `find` / `findLike` are not derivable names.
        // They must therefore reach the derived finders as default methods.
        assertThat(DecisionRepository::class.java.getMethod("find", LONG, LONG).isDefault).isTrue()
        assertThat(DecisionRepository::class.java.getMethod("findLike", LONG, LONG).isDefault).isTrue()

        val derived = DecisionRepository::class.java.declaredMethods
            .filter { java.lang.reflect.Modifier.isAbstract(it.modifiers) }
            .map { it.name }

        assertThat(derived).contains("findByActorIdAndTargetId", "findByActorIdAndTargetIdAndKind")
    }

    // ── Match: one canonical row per unordered pair ─────────────────────────

    @Test
    fun `the match mapping names the table and columns the migration created`() {
        val table = requireNotNull(Match::class.java.getAnnotation(Table::class.java))

        assertThat(table.name).isEqualTo("dog_match")
        assertThat(columnOf(Match::class.java, "lowDogId")).isEqualTo("low_dog_id")
        assertThat(columnOf(Match::class.java, "highDogId")).isEqualTo("high_dog_id")
        assertThat(columnOf(Match::class.java, "createdAt")).isEqualTo("created_at")

        assertThat(migration)
            .contains("CREATE TABLE dog_match")
            .contains("low_dog_id")
            .contains("high_dog_id")
            .contains("created_at")
    }

    @Test
    fun `a match creation time is never rewritten`() {
        val createdAt = requireNotNull(
            Match::class.java.getDeclaredField("createdAt").getAnnotation(Column::class.java),
        )

        assertThat(createdAt.updatable).isFalse()
        assertThat(createdAt.nullable).isFalse()
        assertThat(Match::class.java.getDeclaredField("createdAt").type).isEqualTo(Instant::class.java)
    }

    @Test
    fun `one unordered pair has one match slot, named as the migration names it`() {
        val table = requireNotNull(Match::class.java.getAnnotation(Table::class.java))
        val unique = table.uniqueConstraints.single()

        assertThat(unique.name).isEqualTo("uk_dog_match_pair")
        assertThat(unique.columnNames).containsExactly("low_dog_id", "high_dog_id")
        assertThat(migration).contains(
            "CONSTRAINT uk_dog_match_pair UNIQUE (low_dog_id, high_dog_id)",
        )
    }

    @Test
    fun `the match adapter implements the narrow port over JPA`() {
        assertThat(Matches::class.java.isAssignableFrom(MatchRepository::class.java)).isTrue()
        assertThat(JpaRepository::class.java.isAssignableFrom(MatchRepository::class.java)).isTrue()
    }

    @Test
    fun `the canonical pair lookup is answered by a derived query`() {
        assertThat(MatchRepository::class.java.getMethod("findPair", LONG, LONG).isDefault).isTrue()

        val derived = MatchRepository::class.java.declaredMethods
            .filter { java.lang.reflect.Modifier.isAbstract(it.modifiers) }
            .map { it.name }

        assertThat(derived).contains("findByLowDogIdAndHighDogId")
    }

    @Test
    fun `inserting a match conflicts on the named pair constraint and does nothing`() {
        val insert = MatchRepository::class.java
            .getMethod("insertIgnoringConflict", LONG, LONG, Instant::class.java)
        val query = requireNotNull(insert.getAnnotation(Query::class.java)) {
            "the conflict-free insert must be a declared query"
        }
        val sql = query.value.replace(Regex("\\s+"), " ").trim()

        assertThat(insert.isAnnotationPresent(Modifying::class.java)).isTrue()
        assertThat(query.nativeQuery).isTrue()
        assertThat(sql)
            .contains("INSERT INTO dog_match (low_dog_id, high_dog_id, created_at)")
            .contains("VALUES (:lowDogId, :highDogId, :createdAt)")
            .contains("ON CONFLICT ON CONSTRAINT uk_dog_match_pair DO NOTHING")
        assertThat(insert.returnType).isEqualTo(Void.TYPE)
    }

    @Test
    fun `a dog's matches are read from either side of the pair, newest first`() {
        val findAllFor = MatchRepository::class.java.getMethod("findAllFor", LONG)
        val query = requireNotNull(findAllFor.getAnnotation(Query::class.java)) {
            "the either-side lookup spans two columns and must be a declared query"
        }
        val jpql = query.value.replace(Regex("\\s+"), " ").trim()

        assertThat(query.nativeQuery).isFalse()
        assertThat(jpql)
            .contains("m.lowDogId = :dogId")
            .contains("m.highDogId = :dogId")
            .contains("ORDER BY m.createdAt DESC")
        assertThat(jpql).containsIgnoringCase(" OR ")
    }
}
