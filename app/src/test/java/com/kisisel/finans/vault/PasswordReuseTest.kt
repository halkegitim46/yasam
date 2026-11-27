package com.kisisel.finans.vault

import com.kisisel.finans.vault.PasswordReuse.Result
import org.junit.Assert.assertEquals
import org.junit.Test

class PasswordReuseTest {
    private val previous = listOf("eski1", "eski2", "eski3")

    @Test fun newPasswordIsNone() {
        assertEquals(Result.NONE, PasswordReuse.check("tamamenYeni!", "guncel", previous))
    }

    @Test fun samePasswordAsCurrentMeansNoChange() {
        assertEquals(Result.CURRENT, PasswordReuse.check("guncel", "guncel", previous))
    }

    @Test fun oldPasswordIsReuse() {
        assertEquals(Result.PREVIOUS, PasswordReuse.check("eski2", "guncel", previous))
    }

    @Test fun emptyCandidateIsNone() {
        assertEquals(Result.NONE, PasswordReuse.check("", "guncel", previous))
    }

    @Test fun newItemHasNoCurrentOrHistory() {
        assertEquals(Result.NONE, PasswordReuse.check("herhangi", null, emptyList()))
    }

    @Test fun comparisonIsExactCaseAndSpaceSensitive() {
        assertEquals(Result.NONE, PasswordReuse.check("Eski1", "guncel", previous))
        assertEquals(Result.NONE, PasswordReuse.check("eski1 ", "guncel", previous))
    }

    @Test fun currentWinsWhenAlsoInHistory() {
        assertEquals(Result.CURRENT, PasswordReuse.check("a", "a", listOf("a", "b")))
    }
}
