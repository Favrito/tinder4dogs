package com.ai4dev.tinder4dogs.match

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

/**
 * One durable match between two dogs.
 *
 * A match belongs to the *pair*, not to either dog: there is no initiator and
 * no direction. That is stored canonically — the smaller dog id is always
 * `lowDogId` and the larger is always `highDogId` — so the same pair has one
 * and only one representation, and the unique constraint on the two columns is
 * enough to make "at most one match per pair" a fact about the database rather
 * than a race the service has to win.
 *
 * `createdAt` is UTC and never rewritten after insertion.
 */
@Entity
@Table(
    name = "dog_match",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_dog_match_pair",
            columnNames = ["low_dog_id", "high_dog_id"],
        ),
    ],
)
class Match(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "low_dog_id", nullable = false, updatable = false)
    var lowDogId: Long = 0,

    @Column(name = "high_dog_id", nullable = false, updatable = false)
    var highDogId: Long = 0,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.EPOCH,
)

/**
 * The only three things the matching rules need from match storage.
 *
 * [insertIgnoringConflict] is the interesting one. It is written so that losing
 * a race is *not an error*: on PostgreSQL a unique-constraint violation aborts
 * the whole transaction, so catching it and reading the winner back on the same
 * transaction cannot work. Conflict is therefore avoided, not caught — the
 * insert does nothing when the pair is already there, and the caller always
 * reads the pair back afterwards with [findPair]. Both racers see the same row.
 */
interface Matches {

    /** The match for an already-canonicalised pair, if there is one. */
    fun findPair(lowDogId: Long, highDogId: Long): Match?

    /**
     * Insert the canonical pair, or do nothing if it already exists.
     *
     * Raises nothing on conflict and reports nothing about which happened:
     * the caller reads the pair back either way.
     */
    fun insertIgnoringConflict(lowDogId: Long, highDogId: Long, createdAt: Instant)

    /** Every match [dogId] takes part in, on either side, newest first. */
    fun findAllFor(dogId: Long): List<Match>
}
