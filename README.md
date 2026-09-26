# MoodMuse 🎵🧠

**MoodMuse** is an intelligent, AI-powered music companion that reads your facial expressions in real-time to curate the perfect playlist for your current emotional state. By combining cutting-edge on-device machine learning with dynamic music streaming, MoodMuse creates a deeply personalized audio experience.

## ✨ Key Features

* **🧠 Advanced AI Emotion Engine:** Uses a highly optimized TensorFlow Lite (`.tflite`) model combined with Google ML Kit for 12-point facial landmark analysis. It detects emotions (Happy, Sad, Angry, Surprised, Neutral, Fear, Disgust) even in low-light conditions using Histogram Contrast Stretching.
* **🎵 Dynamic Music Curation:** Integrates with the Jamendo API to instantly fetch and stream royalty-free music that perfectly matches your mood.
* **🔒 Secure Authentication:** Powered by Firebase Authentication for seamless User and Admin sign-ins.
* **📊 Dashboard & History:** Keeps track of your mood history and listening habits locally using Room Database.
* **📱 Modern UI/UX:** Features a beautiful, glass-morphic design with smooth animations, custom neon vectors, and an intuitive user flow.

## 🛠️ Tech Stack

* **Language:** Java / Android SDK (Min SDK 28, Target SDK 35)
* **Machine Learning:** TensorFlow Lite, Google ML Kit (Face Detection)
* **Backend & Auth:** Firebase (Auth, Firestore, Storage)
* **Database:** Room Persistence Library (SQLite)
* **Networking:** Retrofit2, Gson
* **Media Playback:** Media3 ExoPlayer
* **UI/Animations:** Lottie, Material Design Components

## 🚀 Getting Started

### Prerequisites
* Android Studio (Latest Version)
* An Android device or Emulator running API 28+
* A valid `google-services.json` file from your Firebase Console.
* A Jamendo API Client ID (configured in `local.properties`).

### Installation
1. Clone the repository:
   ```bash
   git clone https://github.com/AbdullahAzeem12/MoodMuse.git
   ```
2. Open the project in **Android Studio**.
3. Create a `local.properties` file in the root directory and add your Jamendo API key:
   ```properties
   JAMENDO_CLIENT_ID="your_client_id_here"
   JAMENDO_CLIENT_SECRET="your_client_secret_here"
   ```
4. Place your `google-services.json` in the `app/` directory.
5. Build and run the project!

## 📸 How it Works
1. **Scan:** The app opens the camera and analyzes your facial expression using head pose tracking and asymmetry detection.
2. **Process:** The custom probabilistic fusion engine determines your exact mood with high accuracy.
3. **Listen:** A tailored playlist is instantly generated and played via ExoPlayer.

## 🛡️ License
This project is intended for educational and portfolio purposes. 

---
*Built with ❤️ and AI.*