package com.ai4dev.tinder4dogs.match

import org.springframework.data.jpa.repository.JpaRepository

/**
 * The JPA adapter behind the [Decisions] port.
 *
 * The port's names (`find`, `findLike`) are the names the *service* wants;
 * they are not names Spring Data could derive a query from. So each is a
 * default method delegating to a derived finder whose name says exactly what
 * it queries. `save` is inherited from [JpaRepository] and already matches the
 * port.
 *
 * This adapter records decisions. It does not create matches and it does not
 * apply any domain rule — that belongs to the service.
 */
interface DecisionRepository : JpaRepository<Decision, Long>, Decisions {

    fun findByActorIdAndTargetId(actorId: Long, targetId: Long): Decision?

    fun findByActorIdAndTargetIdAndKind(
        actorId: Long,
        targetId: Long,
        kind: DecisionKind,
    ): Decision?

    override fun find(actorId: Long, targetId: Long): Decision? =
        findByActorIdAndTargetId(actorId, targetId)

    override fun findLike(actorId: Long, targetId: Long): Decision? =
        findByActorIdAndTargetIdAndKind(actorId, targetId, DecisionKind.LIKE)
}
