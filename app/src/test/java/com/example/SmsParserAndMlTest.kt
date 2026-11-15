package com.example

import com.example.capture.IndianSmsRegexParser
import com.example.capture.SmsTransactionParser
import com.example.domain.ExpenseCategory
import com.example.domain.InrFormatter
import com.example.ml.DiasporaMlClassifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsParserAndMlTest {

    private val classifier = DiasporaMlClassifier()

    @Test
    fun indianRegexParserExtractsVerbCurrencyAccountAndRef() {
        val sms = "INR 280.00 spent on Axis Bank Card no. XX9012 at CHAI POINT on 15-11-26. Ref 6319881122"
        val res = IndianSmsRegexParser.parse(sms, "AX-AXISBK")
        assertTrue(res.isFinancialDebit)
        assertTrue(res.isValidTraiHeader)
        assertEquals(2, res.scopedClauseCount)
        assertEquals("spent", res.matchedDebitKeyword?.lowercase())
        assertEquals("INR", res.matchedCurrencySymbol)
        assertEquals(280.0, res.amountInr ?: 0.0, 0.01)
        assertEquals("Chai Point", res.merchant)
        assertEquals("XX9012", res.accountOrCardTail)
        assertEquals("6319881122", res.referenceNumber)
        assertEquals("CARD_SPENT_AT", res.matchedPatternName)
    }

    @Test
    fun rejectsUpiLiteWalletTopUpToAvoidDoubleCounting() {
        val topUpSms = "Rs.1,000.00 debited from HDFC Bank A/c *1234 for UPI Lite top-up on 15-11-26."
        val res = IndianSmsRegexParser.parse(topUpSms, "AD-HDFCBK")
        assertFalse(res.isFinancialDebit)
        assertEquals("UPI_LITE_TOPUP_FILTER", res.matchedPatternName)
    }

    @Test
    fun parsesHdfcUpiSwiggyAndClassifiesAsFood() {
        val sms = "Amt Sent Rs.485.00 From HDFC Bank A/C *1234 To SWIGGY ON 15-11-26 Ref 6319284712"
        val trace = SmsTransactionParser.inspect(sms, classifier)
        assertTrue(trace.accepted)
        assertEquals(485.0, trace.amountInr ?: 0.0, 0.01)
        assertEquals("Swiggy", trace.merchant)
        assertEquals(ExpenseCategory.FOOD_DINING, trace.prediction?.category)
    }

    @Test
    fun parsesIciciDebitWithMerchantCreditedPhraseAndClassifiesAsShopping() {
        val sms = "ICICI Bank Acct XX123 debited for Rs 1,899.00 on 15-Nov-26; ZUDIO RETAIL credited. UPI:431928"
        val trace = SmsTransactionParser.inspect(sms, classifier)
        assertTrue(trace.accepted)
        assertEquals(1899.0, trace.amountInr ?: 0.0, 0.01)
        assertEquals(ExpenseCategory.SHOPPING_LIFESTYLE, trace.prediction?.category)
        assertEquals("ICICI_SEMI_CREDITED", trace.matchedRegexRule)
    }

    @Test
    fun parsesSbiUpiBlinkitAndClassifiesAsGroceries() {
        val sms = "Dear UPI user A/C X5421 debited by 640.50 on date 15Nov26 trf to BLINKIT GROCERIES Refno 43198234"
        val trace = SmsTransactionParser.inspect(sms, classifier)
        assertTrue(trace.accepted)
        assertEquals(640.5, trace.amountInr ?: 0.0, 0.01)
        assertEquals(ExpenseCategory.GROCERIES_KIRANA, trace.prediction?.category)
        assertEquals("UPI_TRF_TO", trace.matchedRegexRule)
    }

    @Test
    fun parsesDiasporaPatelBrothersUpiPayment() {
        val sms = "Paid ₹2,450.00 to patelbrothers@okaxis via Google Pay for groceries"
        val trace = SmsTransactionParser.inspect(sms, classifier)
        assertTrue(trace.accepted)
        assertEquals(2450.0, trace.amountInr ?: 0.0, 0.01)
        assertEquals(ExpenseCategory.GROCERIES_KIRANA, trace.prediction?.category)
    }

    @Test
    fun rejectsOtpPromosFailedAndCreditOnlyAlerts() {
        val otp = "847291 is your OTP for Rs.485.00 transaction at Swiggy. Do not share with anyone."
        val promo = "Pre-approved loan of Rs.5,00,000! Win cashback offer on UPI spends today."
        val failed = "Your UPI transaction of Rs.500 at Zomato has failed and will be reversed."
        val credit = "Rs.15,000.00 credited to your A/c XX1234 via NEFT on 15-Nov-26."

        assertFalse(SmsTransactionParser.inspect(otp, classifier).accepted)
        assertFalse(SmsTransactionParser.inspect(promo, classifier).accepted)
        assertFalse(SmsTransactionParser.inspect(failed, classifier).accepted)
        assertFalse(SmsTransactionParser.inspect(credit, classifier).accepted)
    }

    @Test
    fun onlineLearningUpdatesFuturePrediction() {
        val localClassifier = DiasporaMlClassifier()
        val merchant = "Kaveri Local Store"
        val updates = localClassifier.buildOnlineLearningUpdates(
            merchant = merchant,
            rawContext = "Paid ₹320 at Kaveri Local Store",
            correctedCategory = ExpenseCategory.HEALTH_WELLNESS,
        )
        assertTrue(updates.isNotEmpty())
        val prediction = localClassifier.classify(merchant)
        assertEquals(ExpenseCategory.HEALTH_WELLNESS, prediction.category)
        assertNotNull(prediction.topTokens)
    }

    @Test
    fun formatsInrWithIndianNumberingSystem() {
        assertEquals("₹1,24,500", InrFormatter.format(124500.0))
        assertEquals("₹485", InrFormatter.format(485.0))
    }
}
