package bot

import io.kotest.matchers.shouldBe
import org.junit.Test

class DirectoryConstantsTest {
    @Test
    fun testOut() {
        DirectoryConstants.outDir("hope") shouldBe  ("../../botoutput/hope/")
    }

    @Test
    fun testFile() {
        DirectoryConstants.file("hope", "test.txt") shouldBe  ("../../botoutput/hope/test.txt")
    }
}

