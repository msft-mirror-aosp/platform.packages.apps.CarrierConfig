/*
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.carrierconfig

import android.content.Context
import android.content.res.Resources
import android.content.res.XmlResourceParser
import android.platform.test.flag.junit.SetFlagsRule
import android.service.carrier.CarrierIdentifier
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.internal.carrierconfig.flags.Flags
import java.io.StringReader
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

@RunWith(AndroidJUnit4::class)
class CarrierConfigKotlinTest {

    @get:Rule val flagsRule = SetFlagsRule()

    private lateinit var mockContext: Context
    private lateinit var mockResources: Resources
    private lateinit var service: DefaultCarrierConfigService

    private val carrierIdentifier =
        CarrierIdentifier("001", "001", "Test", "001001123456789", "", "")

    @Before
    fun setUp() {
        val targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext()
        mockContext = mock()
        mockResources = mock()
        whenever(mockContext.resources) doReturn mockResources
        whenever(mockContext.applicationContext) doReturn mockContext
        whenever(mockContext.assets) doReturn targetContext.assets

        // Mock resources for DefaultCarrierConfigService
        whenever(mockResources.getString(any())) doReturn ""

        // Default mock for vendor.xml to return an empty parser
        val vendorParser = createTestXmlParser("<carrier_config_list/>")
        whenever(mockResources.getXml(eq(R.xml.vendor))) doReturn vendorParser
        // Default mock for vendor_no_sim.xml to return an empty parser
        val vendorNoSimParser = createTestXmlParser("<carrier_config_list/>")
        whenever(mockResources.getXml(eq(R.xml.vendor_no_sim))) doReturn vendorNoSimParser

        service =
            object : DefaultCarrierConfigService() {
                override fun getApplicationContext(): Context {
                    return mockContext
                }

                override fun getResources(): Resources {
                    return mockResources
                }
            }
    }

    private fun createTestXmlParser(xmlContent: String): XmlResourceParser {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xmlContent))

        return object : XmlResourceParser, XmlPullParser by parser {
            override fun close() {}

            override fun getAttributeNameResource(index: Int): Int = 0

            override fun getAttributeListValue(
                namespace: String?,
                attribute: String?,
                options: Array<out String>?,
                defaultValue: Int,
            ): Int = 0

            override fun getAttributeBooleanValue(
                namespace: String?,
                attribute: String?,
                defaultValue: Boolean,
            ): Boolean = defaultValue

            override fun getAttributeResourceValue(
                namespace: String?,
                attribute: String?,
                defaultValue: Int,
            ): Int = defaultValue

            override fun getAttributeIntValue(
                namespace: String?,
                attribute: String?,
                defaultValue: Int,
            ): Int = defaultValue

            override fun getAttributeUnsignedIntValue(
                namespace: String?,
                attribute: String?,
                defaultValue: Int,
            ): Int = defaultValue

            override fun getAttributeFloatValue(
                namespace: String?,
                attribute: String?,
                defaultValue: Float,
            ): Float = defaultValue

            override fun getAttributeListValue(
                index: Int,
                options: Array<out String>?,
                defaultValue: Int,
            ): Int = 0

            override fun getAttributeBooleanValue(index: Int, defaultValue: Boolean): Boolean =
                defaultValue

            override fun getAttributeResourceValue(index: Int, defaultValue: Int): Int =
                defaultValue

            override fun getAttributeIntValue(index: Int, defaultValue: Int): Int = defaultValue

            override fun getAttributeUnsignedIntValue(index: Int, defaultValue: Int): Int =
                defaultValue

            override fun getAttributeFloatValue(index: Int, defaultValue: Float): Float =
                defaultValue

            override fun getIdAttribute(): String? = null

            override fun getClassAttribute(): String? = null

            override fun getIdAttributeResourceValue(defaultValue: Int): Int = defaultValue

            override fun getStyleAttribute(): Int = 0
        }
    }

    @Test
    fun testVendorDefaultsXmlParse_flagEnabled() {
        flagsRule.enableFlags(Flags.FLAG_ADD_VENDOR_DEFAULTS_TO_CARRIER_CONFIG)
        val testXml =
            """
                <carrier_config_list>
                    <carrier_config>
                        <boolean name="test_vendor_defaults_loaded" value="true" />
                    </carrier_config>
                </carrier_config_list>
            """
                .trimIndent()
        val defaultsParser = createTestXmlParser(testXml)
        whenever(mockResources.getXml(eq(R.xml.vendor_defaults))) doReturn defaultsParser

        val config = service.onLoadConfig(carrierIdentifier)

        assertNotNull("Config should not be null", config)
        assertTrue(
            "vendor_defaults.xml should be loaded",
            config!!.getBoolean("test_vendor_defaults_loaded", false),
        )
        verify(mockResources).getXml(eq(R.xml.vendor_defaults))
    }

    @Test
    fun testVendorDefaultsXmlParse_flagDisabled() {
        flagsRule.disableFlags(Flags.FLAG_ADD_VENDOR_DEFAULTS_TO_CARRIER_CONFIG)
        val testXml =
            """
                <carrier_config_list>
                    <carrier_config>
                        <boolean name="test_vendor_defaults_loaded" value="true" />
                    </carrier_config>
                </carrier_config_list>
            """
                .trimIndent()
        val defaultsParser = createTestXmlParser(testXml)
        whenever(mockResources.getXml(eq(R.xml.vendor_defaults))) doReturn defaultsParser

        val config = service.onLoadConfig(carrierIdentifier)

        assertNotNull("Config should not be null", config)
        assertFalse(
            "vendor_defaults.xml should NOT be loaded",
            config!!.containsKey("test_vendor_defaults_loaded"),
        )
        verify(mockResources, never()).getXml(eq(R.xml.vendor_defaults))
    }
}
