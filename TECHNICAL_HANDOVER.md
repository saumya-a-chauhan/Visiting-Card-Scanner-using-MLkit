# Technical Handover Document: Visiting Card Scanner

This document serves as a comprehensive technical guide to the architecture, logic, and extraction flow of the Visiting Card Scanner Android App. It is designed to help a senior developer or maintainer instantly understand how the project functions under the hood.

---

## 1. High-Level Architecture
The application is built using modern Android development standards:
- **UI Framework**: Jetpack Compose (Material Design 3)
- **Architecture**: MVVM (Model-View-ViewModel) 
- **Concurrency**: Kotlin Coroutines & StateFlow/MutableState
- **Machine Learning**: Google ML Kit (Vision Text Recognition v2)
- **Camera integration**: CameraX

---

## 2. Core Components Overview

### `MainActivity.kt`
The entry point of the app. It handles runtime camera permissions using the Accompanist permissions library and sets up the Compose Navigation graph (`NavHost`), routing between the `HomeScreen`, `CameraScreen`, and `ResultsScreen`.

### `MainViewModel.kt`
The central state holder. It manages:
- **State variables**: `isProcessing`, `currentImageUri`, and `currentResults` (which holds the parsed `CardResult` data class).
- **Auto-Rotation Engine**: Physical business cards are often captured in portrait mode. The ViewModel runs a "pre-pass" on the ML Kit text blocks, reading the angle of the text. If it detects a 90, 180, or 270-degree rotation, it physically rotates the underlying Android `Bitmap` using a `Matrix` and re-feeds it to ML Kit. This ensures all OCR bounding boxes are perfectly horizontal, which is critical for our spatial heuristic algorithms.

### `ExtractionPipelines.kt`
The "brain" of the app. Since standard ML Kit Entity Extraction fails on highly customized business card layouts, this class completely bypasses NLP models in favor of a **Custom Spatial Proximity Algorithm**. It analyzes the raw physical X/Y pixel coordinates (`boundingBox`) of the text to understand layout context.

---

## 3. The Extraction Pipeline Flow (Step-by-Step)

When an image is passed to `processPipeline3(visionText: Text)`, the following flow occurs:

### Step 1: Pre-cleaning and Setup
The engine extracts all `Text.Line` objects from ML Kit. It maintains a `remainingLines` mutable list. As entities are identified and extracted, their lines are aggressively removed from `remainingLines` to prevent data duplication (e.g., preventing a phone number from later being parsed as an address).

### Step 2: High-Confidence Regex Extraction
The engine does a first pass for data that follows strict structural formats:
- **Email**: Matched via regex. 
- **Website**: Matched via regex (`(www[./]?|http://|https://)`). Explicitly allows slashes because OCR frequently misreads `www.site.com` as `www/site.com`.
- **GSTIN**: Matched against standard Indian GSTIN formats.
- **Phones**: Scans for 10-digit combinations, landlines with STD codes, and handles grouped spacing (e.g., `+91 80 581 22 143`).

### Step 3: Designation & Company (Keyword + Geometry Extraction)
- **Designation**: Scans for keywords (`Manager`, `Director`, `CEO`). Once found, it isolates the designation from surrounding noise using word indexing.
- **Company Name**: First searches for corporate suffixes (`Pvt. Ltd.`, `Industries`, `Enterprises`). If not found, it falls back to a purely geometric approach: it iterates through `remainingLines` and selects the line with the **largest bounding box height/area**, under the assumption that the company name is printed in the largest font on the card.

### Step 4: Person Name (Anchoring & Spatial Proximity)
Standard OCR struggles to differentiate a person's name from a brand name. We use three strategies:
1. **Titles**: Looks for `Mr.`, `Dr.`, `CA`.
2. **Phone Proximity Anchoring**: The engine measures the physical pixel distance between the extracted Phone Number bounding box and all valid Name candidates (capitalized strings, no numbers, no address keywords). If a name candidate is physically positioned right next to or directly above/below the phone number (within ~150 pixels), it locks it in as the Person's Name.
3. **Designation Proximity Anchoring**: Performs the exact same spatial distance check, but anchors off the detected Designation box instead of the phone number.

### Step 5: The Address Compiler (Vertical Ray-Casting)
Business card addresses are often scattered across 3 or 4 separate lines. To compile them into a single string:
1. **Find Anchor**: Scans for a line containing an address keyword (`Road`, `Floor`, `State`, etc.) or a 6-digit Pincode.
2. **Upward/Downward Proximity Scan**: Once an anchor is found, the algorithm acts like a vertical ray-cast. It checks all other lines to see if their `Y` coordinates are within 45 pixels above or below the anchor, and if their `X` left-alignment is within 150 pixels.
3. **Noise Filtering (`isLikelySocialHandle`)**: Before merging a physically adjacent line into the address, it passes it through a blacklist. It explicitly rejects emails, websites, and **Social Media Handles**. 
   - *Context*: ML Kit often misreads the Instagram/Facebook logo as random characters (e.g., `a00kalyanlaminates`). Since these sit right below the address, the spatial algorithm would normally absorb them. Our `isLikelySocialHandle` heuristic filters out any line >= 12 characters that lacks spaces/commas and doesn't contain a known City/State name, ensuring the compiled address is perfectly clean.

---

## 4. UI Layer (`ResultsScreen.kt`)
The UI is dynamically populated by the `CardResult` object generated by the pipeline.
- **Identify Section**: Standard text fields for Name, Designation, and Company.
- **Smart Suggestions**: Because Name/Company extraction can sometimes be subjective on complex cards, the extracted values are presented as Material `SuggestionChips` at the top of the screen. Tapping a chip automatically maps and autofills the value into the correct text field below.
- **Contact Info (Chips)**: List fields (Phones, Emails, Websites) use a custom `SingleChipField` composable. If the ML pipeline detects multiple phone numbers, they are presented as appendable chips below the text field, allowing the user to select which numbers to keep.

---

## 5. Potential Areas for Future Optimization
- **ML Kit Dependency**: Currently `build.gradle.kts` is heavily optimized. However, if future maintainers wish to experiment with cloud OCR, Google Cloud Vision API provides better handwriting recognition than the on-device ML Kit.
- **State Keywords**: The `ExtractionPipelines` class contains a hardcoded array of Indian Cities/States used for Address noise-filtering. If the app is expanded globally, this list should be moved to a localized JSON file or room database.
