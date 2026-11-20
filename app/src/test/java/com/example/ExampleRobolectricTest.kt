package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Vault Card", appName)
  }

  @Test
  fun `generated card expiry and cvv format are valid`() {
    val expiry = com.example.core.SimFormatters.generateDemoExpiry()
    assertEquals(5, expiry.length)
    assertEquals('/', expiry[2])
    val cvv = com.example.core.SimFormatters.generateDemoCvv()
    assertEquals(3, cvv.length)
  }

  @Test
  fun `simulator QR codec accepts universal direct QR payloads`() {
    val encoded = com.example.core.SimQrCodec.encode("demo_store_apex", "Apex Gadgets Demo", 99.5)
    val parsed = com.example.core.SimQrCodec.parse(encoded)
    assertEquals("demo_store_apex", parsed?.sellerId)
    assertEquals("Apex Gadgets Demo", parsed?.storeName)

    val upi = com.example.core.SimQrCodec.parseUniversal("upi://pay?am=250.00&cu=INR")
    assertEquals(true, upi?.isDirectNoDetails)
    assertEquals(250.0, upi?.suggestedAmount ?: 0.0, 0.01)

    val generic = com.example.core.SimQrCodec.parseUniversal("https://paypal.me/demo")
    assertEquals(com.example.core.PaymentRail.PAYPAL, generic?.suggestedRail)
    assertEquals(true, generic?.isDirectNoDetails)
  }

  @Test
  fun `account balance starts at 0 and increments only on top-up or payment events`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val backend = com.example.data.repository.LocalSimulatorBackend(
      context = context,
      accountKey = "test_acct_${System.nanoTime()}"
    )
    backend.bootstrap("Test Member")

    val initialCards = backend.observeCards().first()
    val starterCard = initialCards.first()
    assertEquals(0.0, starterCard.balance, 0.001)

    val profile = backend.observeUserProfile().first()!!
    backend.customizeAndUpgradeCard(profile, starterCard, "TEST HOLDER", "#F5C451", upgrade = true)
    val afterUpgrade = backend.observeCards().first().first()
    assertEquals(0.0, afterUpgrade.balance, 0.001)

    backend.addVirtualMoney(afterUpgrade, 250.0)
    val afterTopUp = backend.observeCards().first().first()
    assertEquals(250.0, afterTopUp.balance, 0.001)

    val payResult = backend.processVirtualPayment(
      profile = profile,
      card = afterTopUp,
      amount = 125.0,
      merchantName = "Direct QR Pay",
      seller = null,
      cartItems = emptyList(),
      idempotencyKey = "test_pay_${System.nanoTime()}"
    )
    assertEquals(true, payResult.isSuccess)
    val afterPayment = backend.observeCards().first().first()
    assertEquals(375.0, afterPayment.balance, 0.001)

    backend.resetSimulatorBalances(listOf(afterPayment))
    val afterReset = backend.observeCards().first().first()
    assertEquals(0.0, afterReset.balance, 0.001)
  }
}
