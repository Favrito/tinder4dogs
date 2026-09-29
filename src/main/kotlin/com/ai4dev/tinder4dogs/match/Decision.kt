package com.ai4dev.tinder4dogs.match

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

/** What one dog decided about another. There is no third option. */
enum class DecisionKind {
    LIKE,
    PASS,
}

/**
 * The latest decision one dog has recorded about another.
 *
 * A decision is *directed*: `actorId` decided about `targetId`, and the pair in
 * the other direction is a different row. Only the latest decision per directed
 * pair is kept, which the unique constraint below makes an invariant of the
 * database rather than a habit of the service.
 *
 * The two dog references are plain `Long` columns, not `@ManyToOne`
 * associations: nothing here navigates to a `Dog`, and the foreign keys still
 * exist in the schema. Mapping them as columns keeps `ddl-auto: validate`
 * agreeing with migration `005` and avoids lazy proxies under
 * `open-in-view: false`.
 */
@Entity
@Table(
    name = "dog_decision",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_dog_decision_actor_target",
            columnNames = ["actor_dog_id", "target_dog_id"],
        ),
    ],
)
class Decision(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "actor_dog_id", nullable = false, updatable = false)
    var actorId: Long = 0,

    @Column(name = "target_dog_id", nullable = false, updatable = false)
    var targetId: Long = 0,

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 10)
    var kind: DecisionKind = DecisionKind.PASS,
)

/**
 * The only three things the matching rules need from decision storage.
 *
 * The service depends on this and not on `JpaRepository`, so a test can stand
 * up a fake with three methods instead of forty.
 */
interface Decisions {

    /** The latest decision from [actorId] about [targetId], if there is one. */
    fun find(actorId: Long, targetId: Long): Decision?

    /** The same lookup, but only when the stored decision is a like. */
    fun findLike(actorId: Long, targetId: Long): Decision?

    /** Record a new decision, or replace the one already held for the pair. */
    fun save(decision: Decision): Decision
}
