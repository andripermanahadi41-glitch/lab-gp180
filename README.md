# GP-180 Preset Lab

[![Android](https://img.shields.io/badge/Platform-Android%2024%2B-brightgreen.svg)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-blue.svg)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-M3-cyan.svg)](https://developer.android.com/jetpack/compose)
[![MIDI](https://img.shields.io/badge/Protocol-BLE%20MIDI%20%7C%20USB%20MIDI-teal.svg)](https://www.midi.org/)

Hardware Rig Sync & AI Tone Generator for **Valeton GP-180** multi-effects guitar processor.

---

## 🎸 Fitur Utama

- **Konektivitas Ganda (Dual Connection)**:
  - **Nirkabel Bluetooth (BLE MIDI)**: Menggunakan spesifikasi standar BLE MIDI Service (`03b80e5a-ede8-4b33-a028-51000c730eab`) dan Characteristic (`7772e5db-3868-4112-a1a9-f2669d106bf3`).
  - **Kabel USB MIDI (Wired)**: Memanfaatkan `android.media.midi.MidiManager` untuk komunikasi latensi rendah langsung via kabel USB-C OTG.
  - **Virtual Hardware Simulator**: Mode simulasi rig bawaan untuk pengetesan tanpa pedal fisik.
- **Komunikasi Dua Arah (Bi-directional Sync)**:
  - Mengirim parameter CC secara real-time saat kenop diputar atau footswitch diinjak.
  - Menerima sinyal balik (feedback) dari pedal fisik untuk menyinkronkan tampilan layar secara otomatis.
- **Rantai Sinyal 10 Modul GP-180**:
  - `NR` (Noise Gate), `WAH` (Cry Wah), `BST` (Overdrive), `AMP` (NAM / High Gain / Clean), `CAB` (Cabinet IR), `EQ` (Graphic 5-Band), `MOD` (Chorus / Phaser / Flanger), `DLY` (Tape / Analog Echo), `RVB` (Plate / Room / Hall), dan `VOL` (Master / Solo Boost).
  - Dial rotari knurled dengan gestur drag putar halus, tampilan nilai numerik, dan toggle bypass instan.
- **Tone Concept & AI Tone Generator**:
  - Generator rig otomatis berdasarkan referensi lagu/artis dan deskripsi karakter suara (didukung Gemini 3.5 Flash & mesin lokal Tone Architect).
  - Sinkronisasi langsung ke memori pedal (`Send Preset to GP-180 Memory`).
- **Pemetaan 3-Footswitch Fisik**:
  - Footswitch 1 (CC 81), Footswitch 2 (CC 82), Footswitch 3 / CTRL (CC 83).
- **Manajemen Preset & Backup**:
  - Pustaka bank preset (Factory & User).
  - Ekspor / Impor file format `.prst` (JSON).
  - Live MIDI Monitor terintegrasi dengan filter arah pesan (TX/RX) dan inspeksi kode heksadesimal.

---

## 🛠️ Persyaratan Sistem & Build

- **Android Studio**: Ladybug / Meerkat atau yang lebih baru
- **JDK**: Version 11 atau 17
- **Min SDK**: API 24 (Android 7.0)
- **Target SDK**: API 36
- **Gradle**: 8.x + AGP 9.1.x

### Kompilasi Aplikasi

```bash
# Jalankan unit & Robolectric tests
gradle :app:testDebugUnitTest

# Build APK Debug
gradle :app:assembleDebug
```

---

## 📡 Panduan Koneksi Pedal Valeton GP-180

1. **Koneksi Bluetooth (Nirkabel)**:
   - Nyalakan pedal Valeton GP-180 dan pastikan modul Bluetooth menyala.
   - Buka aplikasi dan klik tombol **Bluetooth**.
   - Pilih perangkat **Valeton GP-180 BLE** pada daftar yang ditemukan.
   - Status akan berubah menjadi **BLE WIRELESS** (Lampu Biru).

2. **Koneksi USB (Kabel)**:
   - Hubungkan kabel USB-C OTG dari HP Android ke port USB GP-180.
   - Klik tombol **USB MIDI**.
   - Status akan berubah menjadi **USB WIRED** (Lampu Hijau).

3. **Mode Simulasi**:
   - Jika sedang tidak membawa pedal fisik, klik tombol robot 🤖 untuk mengaktifkan **Virtual Simulator**.

---

## 📄 Lisensi

Proyek ini dibuat untuk komunitas gitaris dan pengguna Valeton GP-180.
