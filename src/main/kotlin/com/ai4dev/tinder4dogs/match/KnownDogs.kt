package com.ai4dev.tinder4dogs.match

import com.ai4dev.tinder4dogs.dog.DogRepository
import org.springframework.stereotype.Component

/**
 * The one thing matching needs to know about a dog: whether it exists.
 *
 * `DogRepository` is owned by the `dog` package and is not this feature's to
 * change, and depending on it directly would drag forty inherited methods into
 * every matching test. So `match` declares the question it actually asks and
 * keeps the adapter on its own side of the boundary. The dependency still runs
 * `match -> dog`; `dog` stays unaware of matching.
 */
interface KnownDogs {
    fun exists(dogId: Long): Boolean
}

@Component
class KnownDogsFromRepository(private val dogs: DogRepository) : KnownDogs {
    override fun exists(dogId: Long): Boolean = dogs.existsById(dogId)
}
