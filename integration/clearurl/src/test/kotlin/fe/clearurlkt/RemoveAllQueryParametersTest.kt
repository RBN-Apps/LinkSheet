package fe.clearurlkt

import fe.clearurlskt.ClearUrls
import fe.clearurlskt.Modification
import fe.clearurlskt.loader.BundledClearURLConfigLoader
import fe.clearurlskt.provider.Provider
import kotlin.test.Test
import kotlin.test.assertEquals

internal class RemoveAllQueryParametersTest {
    private val cleaner = ClearUrls(BundledClearURLConfigLoader.load().getOrThrow()!!)
    private val instagram = setOf("instagram")

    @Test
    fun `selection removes unknown repeated and empty parameters and records their names`() {
        val (url, operations) = cleaner.clearUrl(
            "https://www.instagram.com/p/ABC/?custom=one&custom=two&empty=&flag#comments", instagram
        )
        assertEquals("https://www.instagram.com/p/ABC/#comments", url)
        val removal = operations.filterIsInstance<Modification.ParameterRemoval>().last()
        assertEquals("instagram", removal.provider)
        assertEquals(setOf("custom", "empty", "flag"), removal.fields)
        assertEquals(emptySet(), removal.fragment)
    }

    @Test
    fun `deselecting a provider restores normal cleaning on the same instance`() {
        val input = "https://www.instagram.com/p/ABC/?igsh=tracking&custom=keep"
        assertEquals("https://www.instagram.com/p/ABC/", cleaner.clearUrl(input, instagram).first)
        assertEquals("https://www.instagram.com/p/ABC/?custom=keep", cleaner.clearUrl(input).first)
    }

    @Test
    fun `unselected providers keep functional parameters`() {
        assertEquals(
            "https://example.com/?custom=keep",
            cleaner.clearUrl("https://example.com/?custom=keep&utm_source=tracking", instagram).first
        )
    }

    @Test
    fun `instagram redirect to another provider preserves destination parameters`() {
        assertEquals(
            "https://example.com/?id=42",
            cleaner.clearUrl("https://l.instagram.com/?u=https%3A%2F%2Fexample.com%2F%3Fid%3D42&e=tracking", instagram).first
        )
    }

    @Test
    fun `redirect into selected provider removes destination parameters`() {
        assertEquals(
            "https://www.instagram.com/p/ABC/",
            cleaner.clearUrl("https://www.google.com/url?q=https%3A%2F%2Fwww.instagram.com%2Fp%2FABC%2F%3Fcustom%3D42", instagram).first
        )
    }

    @Test
    fun `explicit override applies even to provider exceptions and preserves exact fragments`() {
        val provider = Provider(
            sortPosition = 0, key = "custom", url = Regex("^https://example\\.com/"),
            completeProvider = false, rules = emptyList(), rawRules = emptyList(),
            referralMarketing = emptyList(), exceptions = listOf(Regex(".*")), redirections = emptyList()
        )
        val customCleaner = ClearUrls(listOf(provider))
        for (input in listOf("https://example.com/?#part?value", "https://example.com/?foo=bar#part?value")) {
            assertEquals("https://example.com/#part?value", customCleaner.clearUrl(input, setOf("custom")).first)
            assertEquals(input, customCleaner.clearUrl(input).first)
        }
        val fragmentOnly = "https://example.com/#part?value"
        assertEquals(fragmentOnly, customCleaner.clearUrl(fragmentOnly, setOf("custom")).first)
    }
}
