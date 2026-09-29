package com.ai4dev.tinder4dogs.match

import java.time.Instant
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

/**
 * The JPA adapter behind the [Matches] port.
 *
 * `findPair` reads as the service wants to read; the derived finder below is
 * the name Spring Data can actually build a query from. The other two need
 * writing out: the either-side lookup spans two columns, and the insert has to
 * name the constraint it tolerates.
 */
interface MatchRepository : JpaRepository<Match, Long>, Matches {

    fun findByLowDogIdAndHighDogId(lowDogId: Long, highDogId: Long): Match?

    override fun findPair(lowDogId: Long, highDogId: Long): Match? =
        findByLowDogIdAndHighDogId(lowDogId, highDogId)

    /**
     * Insert the canonical pair unless it is already there.
     *
     * `ON CONFLICT ON CONSTRAINT uk_dog_match_pair DO NOTHING` is the whole
     * point: it leaves the transaction healthy whether the row was written by
     * this caller or by a concurrent one, so the unconditional [findPair] that
     * follows always succeeds and always returns the same row for both racers.
     * The constraint is named rather than inferred from the columns so that a
     * migration renaming it fails loudly instead of silently widening what this
     * insert forgives.
     */
    @Modifying
    @Query(
        value = """
            INSERT INTO dog_match (low_dog_id, high_dog_id, created_at)
            VALUES (:lowDogId, :highDogId, :createdAt)
            ON CONFLICT ON CONSTRAINT uk_dog_match_pair DO NOTHING
        """,
        nativeQuery = true,
    )
    override fun insertIgnoringConflict(
        @Param("lowDogId") lowDogId: Long,
        @Param("highDogId") highDogId: Long,
        @Param("createdAt") createdAt: Instant,
    )

    @Query(
        """
            SELECT m FROM Match m
            WHERE m.lowDogId = :dogId OR m.highDogId = :dogId
            ORDER BY m.createdAt DESC
        """,
    )
    override fun findAllFor(@Param("dogId") dogId: Long): List<Match>
}
