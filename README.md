# Visiting Card Scanner (ML Kit + Jetpack Compose)

An intelligent, Android-based Visiting/Business Card Scanner built entirely with **Jetpack Compose** and **Google ML Kit**. This project extracts text from physical business cards, intelligently parses the fields using a custom spatial heuristic algorithm, and presents the results in a beautiful, modern Android UI.

This was developed as a 2-month internship project focusing on Mobile Machine Learning and modern Android UI/UX design.

##  Features

- **On-Device Machine Learning**: Utilizes Google ML Kit's Text Recognition to parse physical cards entirely offline. No cloud APIs required.
- **Auto-Rotation Correction**: Intelligently detects if the user took the picture in portrait or landscape mode and automatically corrects the orientation of the text blocks for perfect OCR extraction.
- **Custom Spatial Heuristic Engine**: Goes beyond standard regex. The custom extraction engine analyzes the physical distance (X/Y bounding boxes) between OCR text blocks to correctly group scattered addresses, pair names with designations, and exclude isolated social media handles.
- **Smart Suggestions**: Automatically isolates Name, Designation, and Company Name. Provides one-tap "Smart Chips" that instantly autofill the corresponding text fields.
- **Beautiful UI**: Built completely in Jetpack Compose utilizing Material Design 3 guidelines (`surfaceContainerLow`, `SuggestionChips`, fluid typography).
- **Batch Testing Framework**: (Internal) Developed a fully automated Python-ADB pipeline to push 100+ Indiamart datasets to the device, run instrumented Android tests, and export the accuracy results to Excel.

##  Tech Stack

- **Language**: Kotlin
- **UI Toolkit**: Jetpack Compose (Material 3)
- **Machine Learning**: Google ML Kit (Vision Text Recognition v2)
- **Camera**: CameraX API
- **Architecture**: MVVM (Model-View-ViewModel) + Coroutines/Flow
- **Image Loading**: Coil (Compose)
- **Testing**: Python + ADB + AndroidX Espresso/JUnit4


##  How the Extraction Engine Works
Standard text parsers fail on business cards because design layouts vary wildly. Our custom `ExtractionPipelines.kt` uses a spatial proximity algorithm:
1. **Vertical/Horizontal Bounding Box Scanning**: Scans upwards and downwards from anchor keywords (like "Road", "Floor", or Pincodes) to piece together multi-line addresses based purely on their physical pixel proximity on the card.
2. **Social Media Noise Rejection**: Filters out OCR'd social media icons (e.g., Instagram/Facebook logos that are misread as text) by measuring word length, absence of spaces, and comparing against a robust local Indian City/State dictionary.
3. **Regex + Spatial Pairing**: Uses standard regex to find 10-digit Indian phone numbers and emails, then physically searches the bounding boxes right next to them to find the Person's Name.

##  Setup and Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/saumya-a-chauhan/Visiting-Card-Scanner-using-MLkit.git
   ```
2. Open the project in **Android Studio** (Koala or newer recommended).
3. Connect a physical Android device or emulator.
4. Click **Run** (`Shift + F10`). The app will install and request Camera permissions on launch.

##  License
This project is open-source and available under the [MIT License](LICENSE).
