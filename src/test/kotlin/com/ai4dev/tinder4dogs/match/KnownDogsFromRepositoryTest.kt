package com.ai4dev.tinder4dogs.match

import com.ai4dev.tinder4dogs.dog.DogRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito

/**
 * The matching side needs exactly one question answered about a dog — does it
 * exist — and nothing else. These tests pin that the adapter asks the dog
 * package that one question, answers with what it is told, and reaches for
 * nothing else on the repository.
 */
class KnownDogsFromRepositoryTest {

    private val dogs: DogRepository = Mockito.mock(DogRepository::class.java)
    private val knownDogs: KnownDogs = KnownDogsFromRepository(dogs)

    @Test
    fun `a dog the repository holds is known`() {
        Mockito.`when`(dogs.existsById(7L)).thenReturn(true)

        assertThat(knownDogs.exists(7L)).isTrue()
    }

    @Test
    fun `a dog the repository does not hold is unknown`() {
        Mockito.`when`(dogs.existsById(404L)).thenReturn(false)

        assertThat(knownDogs.exists(404L)).isFalse()
    }

    @Test
    fun `existence is the only thing the adapter asks the dog repository`() {
        Mockito.`when`(dogs.existsById(7L)).thenReturn(true)

        knownDogs.exists(7L)

        Mockito.verify(dogs).existsById(7L)
        Mockito.verifyNoMoreInteractions(dogs)
    }

    @Test
    fun `the port offers matching one question and no repository surface`() {
        assertThat(KnownDogs::class.java.declaredMethods.map { it.name })
            .containsExactly("exists")
    }
}
