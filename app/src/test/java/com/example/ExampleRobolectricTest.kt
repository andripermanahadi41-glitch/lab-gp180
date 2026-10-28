package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ai.ToneGeneratorEngine
import com.example.data.PresetRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("GP-180 Lab", appName)
    }

    @Test
    fun `verify factory presets and json serialization`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = PresetRepository(context)
        val presets = repo.getAllPresets()

        assertTrue(presets.isNotEmpty())
        val first = presets.first()
        assertEquals("Metallica - Saint Anger", first.name)

        val json = repo.presetToJson(first)
        val reconstructed = repo.presetFromJson(json)
        assertEquals(first.name, reconstructed.name)
        assertEquals(first.modules.size, reconstructed.modules.size)
    }

    @Test
    fun `verify tone architect engine generation`() = runBlocking {
        val engine = ToneGeneratorEngine()
        val rig = engine.generatePreset(
            song = "Metallica - Saint Anger",
            prompt = "Raw aggressive high gain, mid scoop, tight gate chug",
            targetPatch = 0
        )
        assertNotNull(rig)
        assertTrue(rig.modules.any { it.id == "AMP" })
        assertTrue(rig.modules.any { it.id == "NR" })
    }

    @Test
    fun `verify usb midi service initialization`() {
        val service = com.example.service.UsbMidiService()
        assertNotNull(service)
    }
}
