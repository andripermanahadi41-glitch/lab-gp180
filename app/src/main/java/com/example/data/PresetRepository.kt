package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.FxModule
import com.example.model.PresetRig
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class PresetRepository(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("gp180_presets_pref", Context.MODE_PRIVATE)

    companion object {
        fun getDefaultModules(): List<FxModule> {
            return listOf(
                FxModule(
                    id = "NR",
                    name = "Noise Gate",
                    category = "Gate",
                    isEnabled = true,
                    cc = 21,
                    params = linkedMapOf("Threshold" to 68, "Decay" to 35)
                ),
                FxModule(
                    id = "WAH",
                    name = "Cry Wah",
                    category = "Filter",
                    isEnabled = false,
                    cc = 28,
                    params = linkedMapOf("Position" to 45, "Q" to 60, "Range" to 75)
                ),
                FxModule(
                    id = "BST",
                    name = "Green OD",
                    category = "Overdrive",
                    isEnabled = true,
                    cc = 22,
                    params = linkedMapOf("Gain" to 25, "Tone" to 65, "Level" to 80)
                ),
                FxModule(
                    id = "AMP",
                    name = "Cali Recto (NAM)",
                    category = "High Gain Amp",
                    isEnabled = true,
                    cc = 23,
                    params = linkedMapOf(
                        "Gain" to 85,
                        "Bass" to 65,
                        "Mid" to 28,
                        "Treble" to 75,
                        "Presence" to 70,
                        "Master" to 72
                    )
                ),
                FxModule(
                    id = "CAB",
                    name = "V30 4x12",
                    category = "Cabinet IR",
                    isEnabled = true,
                    cc = 24,
                    params = linkedMapOf("Mic" to 57, "Center" to 40, "HiCut" to 75, "LoCut" to 20)
                ),
                FxModule(
                    id = "EQ",
                    name = "Graphic EQ",
                    category = "Equalizer",
                    isEnabled = true,
                    cc = 25,
                    params = linkedMapOf(
                        "100Hz" to 55,
                        "400Hz" to 45,
                        "800Hz" to 32,
                        "1.6kHz" to 50,
                        "3.2kHz" to 68
                    )
                ),
                FxModule(
                    id = "MOD",
                    name = "Stereo Chorus",
                    category = "Modulation",
                    isEnabled = false,
                    cc = 29,
                    params = linkedMapOf("Rate" to 35, "Depth" to 50, "Mix" to 40)
                ),
                FxModule(
                    id = "DLY",
                    name = "Tape Echo",
                    category = "Delay",
                    isEnabled = false,
                    cc = 26,
                    params = linkedMapOf("Time" to 45, "Feedback" to 30, "Mix" to 25)
                ),
                FxModule(
                    id = "RVB",
                    name = "Room",
                    category = "Reverb",
                    isEnabled = true,
                    cc = 27,
                    params = linkedMapOf("Decay" to 30, "PreDly" to 15, "Mix" to 18)
                ),
                FxModule(
                    id = "VOL",
                    name = "Master Out",
                    category = "Utility",
                    isEnabled = true,
                    cc = 30,
                    params = linkedMapOf("Volume" to 80, "SoloBoost" to 25)
                )
            )
        }

        val FACTORY_PRESETS: List<PresetRig> = listOf(
            PresetRig(
                id = "factory_01",
                name = "Metallica - Saint Anger",
                songReference = "Metallica - Saint Anger",
                soundPrompt = "Raw aggressive high gain, mid scoop, tight gate chug",
                patchNumber = 0,
                modules = getDefaultModules(),
                fs1Title = "Toggle BST",
                fs1Cc = 81,
                fs2Title = "Toggle DLY",
                fs2Cc = 82,
                fs3Title = "Solo Boost (+3dB)",
                fs3Cc = 83
            ),
            PresetRig(
                id = "factory_02",
                name = "Pink Floyd - Comfortably Numb",
                songReference = "Pink Floyd - Comfortably Numb",
                soundPrompt = "Singing sustained lead, stereo tape echo, lush hall reverb",
                patchNumber = 1,
                modules = listOf(
                    FxModule("NR", "Noise Gate", "Gate", true, 21, linkedMapOf("Threshold" to 45, "Decay" to 50)),
                    FxModule("BST", "Big Fuzz Pi", "Distortion", true, 22, linkedMapOf("Gain" to 75, "Tone" to 55, "Level" to 70)),
                    FxModule("AMP", "British Hi-Watt", "Tube Amp", true, 23, linkedMapOf("Gain" to 55, "Bass" to 60, "Mid" to 65, "Treble" to 70, "Presence" to 65, "Master" to 80)),
                    FxModule("CAB", "Fane 4x12", "Cabinet IR", true, 24, linkedMapOf("Mic" to 57, "Center" to 35, "HiCut" to 70, "LoCut" to 15)),
                    FxModule("MOD", "Rotary Speaker", "Modulation", true, 29, linkedMapOf("Rate" to 40, "Depth" to 60, "Mix" to 35)),
                    FxModule("DLY", "Analog BBD", "Delay", true, 26, linkedMapOf("Time" to 65, "Feedback" to 45, "Mix" to 40)),
                    FxModule("RVB", "Lush Plate", "Reverb", true, 27, linkedMapOf("Decay" to 65, "PreDly" to 25, "Mix" to 35)),
                    FxModule("VOL", "Master Out", "Utility", true, 30, linkedMapOf("Volume" to 85, "SoloBoost" to 30))
                ),
                fs1Title = "Toggle Fuzz",
                fs1Cc = 81,
                fs2Title = "Toggle Delay",
                fs2Cc = 82,
                fs3Title = "Rotary Fast",
                fs3Cc = 83
            ),
            PresetRig(
                id = "factory_03",
                name = "Polyphia - Playing God",
                songReference = "Polyphia - Playing God",
                soundPrompt = "Modern nylon spanky clean, compressor, acoustic simulator, crystal shimmer",
                patchNumber = 2,
                modules = listOf(
                    FxModule("NR", "Noise Gate", "Gate", true, 21, linkedMapOf("Threshold" to 30, "Decay" to 60)),
                    FxModule("BST", "Studio Compressor", "Dynamics", true, 22, linkedMapOf("Gain" to 35, "Tone" to 70, "Level" to 85)),
                    FxModule("AMP", "Acoustic Sim & Match", "Clean Amp", true, 23, linkedMapOf("Gain" to 22, "Bass" to 45, "Mid" to 60, "Treble" to 85, "Presence" to 80, "Master" to 85)),
                    FxModule("CAB", "Studio Direct IR", "Cabinet IR", true, 24, linkedMapOf("Mic" to 87, "Center" to 50, "HiCut" to 90, "LoCut" to 30)),
                    FxModule("EQ", "Sparkle EQ", "Equalizer", true, 25, linkedMapOf("100Hz" to 40, "400Hz" to 50, "800Hz" to 55, "1.6kHz" to 65, "3.2kHz" to 80)),
                    FxModule("MOD", "Dimension Chorus", "Modulation", true, 29, linkedMapOf("Rate" to 25, "Depth" to 40, "Mix" to 30)),
                    FxModule("DLY", "Pitched Delay", "Delay", false, 26, linkedMapOf("Time" to 35, "Feedback" to 20, "Mix" to 15)),
                    FxModule("RVB", "Shimmer Reverb", "Reverb", true, 27, linkedMapOf("Decay" to 50, "PreDly" to 10, "Mix" to 28)),
                    FxModule("VOL", "Master Out", "Utility", true, 30, linkedMapOf("Volume" to 80, "SoloBoost" to 20))
                ),
                fs1Title = "Comp Bypass",
                fs1Cc = 81,
                fs2Title = "Shimmer Octave",
                fs2Cc = 82,
                fs3Title = "Acoustic / Electric",
                fs3Cc = 83
            ),
            PresetRig(
                id = "factory_04",
                name = "Van Halen - Brown Sound",
                songReference = "Van Halen - Panama / Ain't Talkin' 'Bout Love",
                soundPrompt = "1968 Variac Plexi crunch, punchy midrange, MXR Phase 90 & slapback",
                patchNumber = 3,
                modules = listOf(
                    FxModule("NR", "Noise Gate", "Gate", true, 21, linkedMapOf("Threshold" to 50, "Decay" to 40)),
                    FxModule("BST", "Warm Echoplex Preamp", "Booster", true, 22, linkedMapOf("Gain" to 30, "Tone" to 60, "Level" to 75)),
                    FxModule("AMP", "Plexi 100 Lead", "Vintage Amp", true, 23, linkedMapOf("Gain" to 78, "Bass" to 50, "Mid" to 80, "Treble" to 72, "Presence" to 75, "Master" to 85)),
                    FxModule("CAB", "Greenback 4x12", "Cabinet IR", true, 24, linkedMapOf("Mic" to 57, "Center" to 42, "HiCut" to 72, "LoCut" to 25)),
                    FxModule("MOD", "Script Phaser 90", "Modulation", true, 29, linkedMapOf("Rate" to 28, "Depth" to 70, "Mix" to 50)),
                    FxModule("DLY", "Slapback Echo", "Delay", true, 26, linkedMapOf("Time" to 22, "Feedback" to 18, "Mix" to 28)),
                    FxModule("RVB", "Small Plate", "Reverb", true, 27, linkedMapOf("Decay" to 35, "PreDly" to 12, "Mix" to 20)),
                    FxModule("VOL", "Master Out", "Utility", true, 30, linkedMapOf("Volume" to 82, "SoloBoost" to 25))
                ),
                fs1Title = "Phaser 90",
                fs1Cc = 81,
                fs2Title = "Slapback Delay",
                fs2Cc = 82,
                fs3Title = "Solo Boost",
                fs3Cc = 83
            )
        )
    }

    fun getAllPresets(): List<PresetRig> {
        val userList = loadUserPresets()
        return FACTORY_PRESETS + userList
    }

    fun saveUserPreset(preset: PresetRig) {
        val current = loadUserPresets().toMutableList()
        val index = current.indexOfFirst { it.id == preset.id }
        if (index >= 0) {
            current[index] = preset
        } else {
            current.add(0, preset)
        }
        val array = JSONArray()
        current.forEach { array.put(presetToJson(it)) }
        prefs.edit().putString("saved_presets", array.toString()).apply()
    }

    fun loadUserPresets(): List<PresetRig> {
        val jsonStr = prefs.getString("saved_presets", null) ?: return emptyList()
        val result = mutableListOf<PresetRig>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                result.add(presetFromJson(obj))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    // JSON Serialization for .prst export / import
    fun presetToJson(preset: PresetRig): JSONObject {
        val json = JSONObject()
        json.put("id", preset.id)
        json.put("name", preset.name)
        json.put("songReference", preset.songReference)
        json.put("soundPrompt", preset.soundPrompt)
        json.put("patchNumber", preset.patchNumber)
        json.put("fs1Title", preset.fs1Title)
        json.put("fs1Cc", preset.fs1Cc)
        json.put("fs2Title", preset.fs2Title)
        json.put("fs2Cc", preset.fs2Cc)
        json.put("fs3Title", preset.fs3Title)
        json.put("fs3Cc", preset.fs3Cc)

        val modArray = JSONArray()
        preset.modules.forEach { mod ->
            val modObj = JSONObject()
            modObj.put("id", mod.id)
            modObj.put("name", mod.name)
            modObj.put("type", mod.category)
            modObj.put("category", mod.category)
            modObj.put("enabled", mod.isEnabled)
            modObj.put("cc", mod.cc)

            val paramsObj = JSONObject()
            mod.params.forEach { (k, v) -> paramsObj.put(k, v) }
            modObj.put("params", paramsObj)
            modArray.put(modObj)
        }
        json.put("modules", modArray)
        return json
    }

    fun presetFromJson(json: JSONObject): PresetRig {
        val id = json.optString("id", UUID.randomUUID().toString())
        val name = json.optString("name", "Custom Rig")
        val song = json.optString("songReference", "")
        val prompt = json.optString("soundPrompt", "")
        val patchNumber = json.optInt("patchNumber", 0)
        val fs1Title = json.optString("fs1Title", "Toggle BST")
        val fs1Cc = json.optInt("fs1Cc", 81)
        val fs2Title = json.optString("fs2Title", "Toggle DLY")
        val fs2Cc = json.optInt("fs2Cc", 82)
        val fs3Title = json.optString("fs3Title", "Solo Boost (+3dB)")
        val fs3Cc = json.optInt("fs3Cc", 83)

        val modulesList = mutableListOf<FxModule>()
        val modArray = json.optJSONArray("modules")
        if (modArray != null) {
            for (i in 0 until modArray.length()) {
                val mObj = modArray.getJSONObject(i)
                val mId = mObj.optString("id", "MOD")
                val mName = mObj.optString("name", "Effect")
                val mCat = mObj.optString("category", mObj.optString("type", "Overdrive"))
                val mEnabled = mObj.optBoolean("enabled", true)
                val mCc = mObj.optInt("cc", 20 + i)

                val paramsMap = linkedMapOf<String, Int>()
                val pObj = mObj.optJSONObject("params")
                if (pObj != null) {
                    val keys = pObj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        paramsMap[k] = pObj.optInt(k, 50)
                    }
                }
                modulesList.add(FxModule(mId, mName, mCat, mEnabled, mCc, paramsMap))
            }
        } else {
            modulesList.addAll(getDefaultModules())
        }

        return PresetRig(
            id = id,
            name = name,
            songReference = song,
            soundPrompt = prompt,
            patchNumber = patchNumber,
            modules = modulesList,
            fs1Title = fs1Title,
            fs1Cc = fs1Cc,
            fs2Title = fs2Title,
            fs2Cc = fs2Cc,
            fs3Title = fs3Title,
            fs3Cc = fs3Cc
        )
    }
}
