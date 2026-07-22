package com.example.visiting_cardbusiness_card

import android.content.Context
import android.util.Log
import com.example.visiting_cardbusiness_card.model.CardResult
import com.google.mlkit.vision.text.Text
import java.util.regex.Pattern

class ExtractionPipelines(private val context: Context) {

    // Dictionary of anchor words used to trigger spatial address extraction
    private val addressKeywords = listOf("Road", "Marg", "Nagar", "Sector", "Gali", "Lane", "Building", "Floor", "Street", "City", "State", "India", "Dist", "Pin", "Kishan Gunj", "Andheri", "Marol", "MIDC", "Bazar", "Bazaar", "Chowk", "Complex", "Plot", "Flat", "Market", "Colony", "Villa", "Bhavan", "Square", "Opposite", "Opp.", "Near", "Behind", "Phase", "Block", "GIDC", "Estate", "Tower", "Shop No", "Gala No", "Andhra Pradesh", "A.P.", "Arunachal Pradesh", "Assam", "Bihar", "Chhattisgarh", "Goa", "Gujarat", "Haryana", "Himachal Pradesh", "Jharkhand", "Karnataka", "Kerala", "Madhya Pradesh", "M.P.", "Maharashtra", "Manipur", "Meghalaya", "Mizoram", "Nagaland", "Odisha", "Punjab", "Rajasthan", "Sikkim", "Tamil Nadu", "Telangana", "Tripura", "Uttar Pradesh", "U.P.", "Uttarakhand", "West Bengal", "Andaman", "Chandigarh", "Daman", "Diu", "Delhi", "Lakshadweep", "Puducherry", "Visakhapatnam", "Vijayawada", "Guntur", "Nellore", "Kurnool", "Itanagar", "Tawang", "Pasighat", "Roing", "Ziro", "Guwahati", "Silchar", "Dibrugarh", "Jorhat", "Nagaon", "Patna", "Gaya", "Bhagalpur", "Muzaffarpur", "Purnia", "Raipur", "Bhilai", "Bilaspur", "Korba", "Durg", "Panaji", "Margao", "Vasco", "Mapusa", "Ponda", "Ahmedabad", "Surat", "Vadodara", "Rajkot", "Bhavnagar", "Faridabad", "Gurugram", "Panipat", "Ambala", "Rohtak", "Shimla", "Manali", "Dharamshala", "Solan", "Mandi", "Ranchi", "Jamshedpur", "Dhanbad", "Bokaro", "Deoghar", "Bengaluru", "Mysuru", "Hubballi", "Mangaluru", "Belagavi", "Thiruvananthapuram", "Kochi", "Kozhikode", "Kollam", "Thrissur", "Indore", "Bhopal", "Jabalpur", "Gwalior", "Ujjain", "Mumbai", "Pune", "Nagpur", "Nashik", "Aurangabad", "Imphal", "Thoubal", "Kakching", "Ukhrul", "Churachandpur", "Shillong", "Tura", "Nongstoin", "Jowai", "Baghmara", "Aizawl", "Lunglei", "Saiha", "Champhai", "Kolasib", "Dimapur", "Kohima", "Mokokchung", "Tuensang", "Wokha", "Bhubaneswar", "Cuttack", "Rourkela", "Berhampur", "Sambalpur", "Ludhiana", "Amritsar", "Jalandhar", "Patiala", "Bathinda", "Jaipur", "Jodhpur", "Udaipur", "Kota", "Bikaner", "Gangtok", "Namchi", "Gyalshing", "Mangan", "Jorethang", "Chennai", "Coimbatore", "Madurai", "Tiruchirappalli", "Salem", "Hyderabad", "Warangal", "Nizamabad", "Karimnagar", "Khammam", "Agartala", "Dharmanagar", "Kailashahar", "Belonia", "Lucknow", "Kanpur", "Agra", "Varanasi", "Meerut", "Noida", "Dehradun", "Haridwar", "Roorkee", "Haldwani", "Rudrapur", "Kolkata", "Asansol", "Siliguri", "Durgapur", "Bardhaman")

    private val serviceBlacklist = listOf("Works", "Repair", "Service", "Mfg", "Deals in", "Manufacturers", "Exporters")
    
    // Strict Regex rules for extracting structured fields (Phone, Email, GSTIN)
    private val pinRegex = Pattern.compile("\\b\\d{6}\\b")
    private val phone10Regex = Pattern.compile("\\b\\d{10}\\b")
    private val indianPhoneRegex = Pattern.compile("(\\+91[\\-\\s]?)?\\b[6-9]\\d{9}\\b")
    private val emailRegex = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
    private val webRegex = Pattern.compile("(www[./]?|http://|https://)[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
    private val gstinRegex = Pattern.compile("\\b\\d{2}[A-Z]{5}\\d{4}[A-Z]{1}[A-Z\\d]{1}[Z]{1}[A-Z\\d]{1}\\b")
    private val designationKeywords = listOf("CEO", "Director", "Manager", "Proprietor", "Partner", "Founder", "President", "Executive", "Engineer", "Agent", "Consultant", "Representative")
    private val nameTitles = listOf("Mr.", "Ms.", "Mrs.", "Dr.", "Adv.", "Shri", "Smt.", "CA")
    private val companySuffixes = listOf("Pvt. Ltd.", "LLP", "Ltd.", "Industries", "Group", "Solutions", "Services", "Books", "Depot", "Store", "Agency", "Enterprises", "Corporation", "Merchants", "Exporters", "COWork")

    // Spacing tolerant email regex
    private val tolerantEmailRegex = Pattern.compile("[a-zA-Z0-9._%+-]+\\s*@\\s*[a-zA-Z0-9.-]+\\s*\\.\\s*[a-zA-Z]{2,}")

    private fun fixOcrSpelling(text: String): String {
        var res = text.lowercase().trim()
        if (res.contains("@") || res.contains("©") || res.contains("®")) {
            res = res.replace("©", "@").replace("®", "@")
            res = res.replace("gmai1.com", "gmail.com")
                     .replace("gma1l.com", "gmail.com")
                     .replace("gma-il.com", "gmail.com")
                     .replace(".c0m", ".com")
                     .replace(".co1n", ".co.in")
                     .replace(".co.1n", ".co.in")
        } else {
            res = res.replace(".c0m", ".com")
                     .replace(".co1n", ".co.in")
                     .replace(".co.1n", ".co.in")
        }
        return res
    }

    private fun splitPhoneNumbers(p: String): List<String> {
        val digits = p.filter { it.isDigit() }
        val results = mutableListOf<String>()
        if (digits.length >= 20) {
            for (i in 0..digits.length - 10 step 10) {
                results.add(digits.substring(i, i + 10))
            }
        } else if (digits.length == 14) {
            results.add(digits.substring(0, 7))
            results.add(digits.substring(7, 14))
        } else if (digits.length == 16) {
            results.add(digits.substring(0, 8))
            results.add(digits.substring(8, 16))
        } else {
            results.add(p)
        }
        return results
    }

    private fun extractPhonesFromLine(lineText: String): List<String> {
        val results = mutableListOf<String>()
        val parts = lineText.split(Regex("[/,]"))
        var basePrefix = ""
        for (part in parts) {
            val cleanPart = part.replace(Regex("[\\s\\-\\(\\)]"), "")
            val digits = cleanPart.filter { it.isDigit() }
            if (digits.length == 10 || digits.length == 11 || digits.length == 12) {
                val normalized = if (digits.length == 12 && digits.startsWith("91")) digits.substring(2) else digits
                results.add(normalized)
                if (digits.length >= 7) {
                    basePrefix = digits.substring(0, digits.length - 2)
                }
            } else if (digits.length == 7 || digits.length == 8) {
                results.add(digits)
                basePrefix = digits.substring(0, digits.length - 2)
            } else if (digits.length == 2 && basePrefix.isNotEmpty()) {
                results.add(basePrefix + digits)
            } else if (digits.length >= 3 && digits.length < 7 && basePrefix.isNotEmpty() && basePrefix.length >= (7 - digits.length)) {
                val prefixLen = 7 - digits.length
                results.add(basePrefix.substring(0, prefixLen) + digits)
            }
        }
        
        if (results.isEmpty()) {
            val cleanText = lineText.replace(Regex("[\\s\\-\\(\\)]"), "")
            val mobMatcher = Pattern.compile("(?:91)?[6-9]\\d{9}").matcher(cleanText)
            while (mobMatcher.find()) {
                val num = mobMatcher.group()
                val finalNum = if (num.startsWith("91") && num.length == 12) num.substring(2) else num
                results.addAll(splitPhoneNumbers(finalNum))
            }
            val landMatcher = Pattern.compile("\\b\\d{7,8}\\b").matcher(cleanText)
            while (landMatcher.find()) {
                results.add(landMatcher.group())
            }
        }
        
        val areaCodeMatcher = Pattern.compile("\\(?\\b0\\d{2,4}\\)?").matcher(lineText)
        if (areaCodeMatcher.find()) {
            val areaCode = areaCodeMatcher.group().replace(Regex("[\\(\\)]"), "")
            for (i in 0 until results.size) {
                val p = results[i]
                if ((p.length == 7 || p.length == 8) && !p.startsWith(areaCode)) {
                    results[i] = areaCode + p
                }
            }
        }
        
        return results
    }

    private fun isolateDesignation(lineText: String, keywords: List<String>): String {
        val matchedKeyword = keywords.find { lineText.contains(it, ignoreCase = true) } ?: return lineText
        val cleaned = lineText.replace(Regex("[()\\-:]"), " ").trim()
        val words = cleaned.split(Regex("\\s+"))
        val kwIdx = words.indexOfFirst { it.contains(matchedKeyword, ignoreCase = true) }
        if (kwIdx != -1) {
            val start = maxOf(0, kwIdx - 1)
            val end = minOf(words.size, kwIdx + 1)
            return words.subList(start, end).joinToString(" ")
        }
        return lineText
    }

    private fun sortLinesSpatially(lines: List<Text.Line>): List<Text.Line> {
        return lines.sortedWith(Comparator { l1, l2 ->
            val y1 = l1.boundingBox?.top ?: 0
            val y2 = l2.boundingBox?.top ?: 0
            val x1 = l1.boundingBox?.left ?: 0
            val x2 = l2.boundingBox?.left ?: 0
            if (Math.abs(y1 - y2) < 20) {
                x1.compareTo(x2)
            } else {
                y1.compareTo(y2)
            }
        })
    }



    // Pipeline 3: Pure Heuristics Alone
    fun processPipeline3(visionText: Text): CardResult {
        return runHeuristicExtraction(visionText, CardResult(pipelineName = "Pipeline 3: Pure Heuristics"))
    }

    private fun runHeuristicExtraction(visionText: Text, initial: CardResult): CardResult {
        val blocks = visionText.textBlocks
        fun isValidPersonName(text: String): Boolean {
            val t = text.trim()
            if (t.isEmpty() || t.length < 3 || t.length > 30) return false
            val words = t.split("\\s+".toRegex())
            if (words.size > 3) return false
            if (t.any { it.isDigit() }) return false
            if (t.contains("@") || t.contains("www") || t.contains("&") || t.contains("+")) return false
            val lower = t.lowercase()
            // Ensure it doesn't match address keywords
            if (addressKeywords.any { lower.contains(it.lowercase()) }) return false
            // Ensure it doesn't match service keywords
            if (serviceBlacklist.any { lower.contains(it.lowercase()) }) return false
            // Ensure it doesn't match company suffixes
            if (companySuffixes.any { lower.contains(it.lowercase()) }) return false
            return true
        }


        val unsortedLines = blocks.flatMap { it.lines }
        
        val remainingLines = sortLinesSpatially(unsortedLines).toMutableList()

        var company = initial.company
        var name = initial.name
        var designation = initial.designation
        val phones = initial.phone.toMutableList()
        val emails = initial.email.toMutableList()
        val websites = initial.website.toMutableList()
        var address = initial.address
        var fax = initial.fax
        var gstin = initial.gstin

        // Remove initial address text from line pool to prevent duplicates
        if (address.isNotEmpty()) {
            remainingLines.removeAll { line ->
                address.contains(line.text, ignoreCase = true) || line.text.contains(address, ignoreCase = true)
            }
        }

        // 1. Emails
        val emailMatches = mutableListOf<String>()
        val mEmail = tolerantEmailRegex.matcher(visionText.text)
        while (mEmail.find()) { 
            val rawEmail = mEmail.group()
            val cleanedEmail = rawEmail.replace(" ", "")
            emailMatches.add(fixOcrSpelling(cleanedEmail)) 
        }
        emailMatches.forEach { if (!emails.contains(it)) emails.add(it) }
        emails.forEach { email -> remainingLines.removeAll { it.text.replace(" ", "").contains(email, ignoreCase = true) } }

        // 2. Websites
        val webMatches = mutableListOf<String>()
        val mWeb = webRegex.matcher(visionText.text)
        while (mWeb.find()) { webMatches.add(fixOcrSpelling(mWeb.group())) }
        webMatches.forEach { if (!websites.contains(it)) websites.add(it) }
        websites.forEach { site -> remainingLines.removeAll { it.text.contains(site, ignoreCase = true) } }

        // 3. GSTIN
        if (gstin.isEmpty()) {
            val gstinLine = remainingLines.find { gstinRegex.matcher(it.text).find() }
            if (gstinLine != null) {
                val m = gstinRegex.matcher(gstinLine.text)
                if (m.find()) gstin = m.group()
                remainingLines.remove(gstinLine)
            }
        }

        // 4. Fax
        val faxPrefix = listOf("fax", "facsimile")
        if (fax.isEmpty()) {
            val faxLine = remainingLines.find { line -> faxPrefix.any { line.text.contains(it, ignoreCase = true) } }
            if (faxLine != null) {
                fax = faxLine.text
                remainingLines.remove(faxLine)
            }
        }

        // 5. Designation (extract early) & Anchored Name
        if (designation.isEmpty()) {
            val desigLine = remainingLines.find { line -> designationKeywords.any { line.text.contains(it, ignoreCase = true) } }
            if (desigLine != null) {
                designation = isolateDesignation(desigLine.text, designationKeywords)
                remainingLines.remove(desigLine)
                
                // Strategy 3: Designation Anchoring for Person Name
                if (name.isEmpty()) {
                    val anchorCandidates = remainingLines.filter { isValidPersonName(it.text) }
                    val closestName = anchorCandidates.minByOrNull { line ->
                        val desigTop = desigLine.boundingBox?.top ?: 0
                        val desigLeft = desigLine.boundingBox?.left ?: 0
                        val lineBottom = line.boundingBox?.bottom ?: 0
                        val lineRight = line.boundingBox?.right ?: 0
                        Math.min(
                            Math.abs(desigTop - lineBottom), // Above
                            Math.abs(desigLeft - lineRight) // Left
                        )
                    }
                    if (closestName != null) {
                        val desigTop = desigLine.boundingBox?.top ?: 0
                        val desigLeft = desigLine.boundingBox?.left ?: 0
                        val lineBottom = closestName.boundingBox?.bottom ?: 0
                        val lineRight = closestName.boundingBox?.right ?: 0
                        if (Math.min(Math.abs(desigTop - lineBottom), Math.abs(desigLeft - lineRight)) < 150) {
                            name = closestName.text
                            remainingLines.remove(closestName)
                        }
                    }
                }
            }
        }

        // 6. Company (extract early)
        if (company.isEmpty()) {
            val compLine = remainingLines.find { line -> companySuffixes.any { line.text.contains(it, ignoreCase = true) } }
            if (compLine != null) {
                company = compLine.text
                remainingLines.remove(compLine)
            } else if (remainingLines.isNotEmpty()) {
                val bestLine = remainingLines
                    .filter { it.text.length in 3..59 }
                    .maxByOrNull { line ->
                        val height = (line.boundingBox?.height() ?: 0).toDouble()
                        height * height
                    }
                if (bestLine != null) {
                    company = bestLine.text
                    remainingLines.remove(bestLine)
                }
            }
        }

        // 7. Person Name (Strict Structural & Proximity)
        if (name.isEmpty()) {
            val nameCandidates = remainingLines.filter { isValidPersonName(it.text) }
            
            // 7a. Name Titles (Mr., Dr., etc.)
            val nameLine = nameCandidates.find { line -> nameTitles.any { line.text.contains(it, ignoreCase = true) } }
            if (nameLine != null) {
                name = nameLine.text
                remainingLines.remove(nameLine)
            }
            
            // 7b. Phone Proximity Anchoring
            if (name.isEmpty() && nameCandidates.isNotEmpty()) {
                val phoneLines = remainingLines.filter { line -> indianPhoneRegex.matcher(line.text).find() || phone10Regex.matcher(line.text).find() }
                for (pLine in phoneLines) {
                    val pTop = pLine.boundingBox?.top ?: 0
                    val pLeft = pLine.boundingBox?.left ?: 0
                    
                    val closestName = nameCandidates.minByOrNull { line ->
                        val lineBottom = line.boundingBox?.bottom ?: 0
                        val lineRight = line.boundingBox?.right ?: 0
                        Math.min(
                            Math.abs(pTop - lineBottom), // Above
                            Math.abs(pLeft - lineRight)  // Left
                        )
                    }
                    if (closestName != null) {
                        val lineBottom = closestName.boundingBox?.bottom ?: 0
                        val lineRight = closestName.boundingBox?.right ?: 0
                        if (Math.min(Math.abs(pTop - lineBottom), Math.abs(pLeft - lineRight)) < 150) {
                            name = closestName.text
                            remainingLines.remove(closestName)
                            break
                        }
                    }
                }
            }

            // 7c. Corner/Quadrant Name Detection
            if (name.isEmpty() && nameCandidates.isNotEmpty()) {
                val topName = nameCandidates.minByOrNull { it.boundingBox?.top ?: Int.MAX_VALUE }
                if (topName != null) {
                    name = topName.text
                    remainingLines.remove(topName)
                }
            }
        }

        // 8. Phones (Aggressive Regex)
        val aggPhoneRegex = Pattern.compile("(\\+?91)?[\\s\\-]*\\d[\\s\\-]*\\d[\\s\\-]*\\d[\\s\\-]*\\d[\\s\\-]*\\d[\\s\\-]*\\d[\\s\\-]*\\d[\\s\\-]*\\d[\\s\\-]*\\d[\\s\\-]*\\d")
        val mPhone = aggPhoneRegex.matcher(visionText.text)
        while (mPhone.find()) {
            val rawPhone = mPhone.group()
            val digits = rawPhone.filter { it.isDigit() }
            val finalNum = if (digits.length == 12 && digits.startsWith("91")) digits.substring(2) else digits
            if (finalNum.length == 10 && !phones.contains(finalNum)) {
                phones.add(finalNum)
            }
        }

        val extractedPhones = mutableListOf<String>()
        val phoneLinesToRemove = mutableListOf<Text.Line>()
        for (line in remainingLines) {
            val linePhones = extractPhonesFromLine(line.text)
            if (linePhones.isNotEmpty()) {
                extractedPhones.addAll(linePhones)
                phoneLinesToRemove.add(line)
            }
        }
        extractedPhones.forEach { if (!phones.contains(it)) phones.add(it) }
        remainingLines.removeAll(phoneLinesToRemove)

        phones.removeIf { it.filter { c -> c.isDigit() }.length == 6 }

        // 9. Address & Pincode with Column-Protected Proximity Logic (vertical proximity grouping)
        val foundAddressLines = mutableListOf<String>()
        val linesToRemove = mutableListOf<Text.Line>()

        for (line in remainingLines) {
            val text = line.text
            val isPinLine = pinRegex.matcher(text).find()
            val hasAddrKeywords = addressKeywords.any { text.contains(it, ignoreCase = true) }
            
            if (isPinLine || hasAddrKeywords) {
                foundAddressLines.add(text)
                linesToRemove.add(line)
                
                val currentBox = line.boundingBox
                val y = currentBox?.top ?: 0
                val xStart = currentBox?.left ?: 0
                
                // A. Backward (Upward) Proximity Scan
                val upwardLines = remainingLines.filter { other ->
                    other != line && !linesToRemove.contains(other) &&
                    (y - (other.boundingBox?.bottom ?: 0)) in 1..45 &&
                    Math.abs((other.boundingBox?.left ?: 0) - xStart) < 150
                }
                upwardLines.forEach { adj ->
                    val adjText = adj.text
                    val isLikelySocialHandle = adjText.trim().indexOf(' ') == -1 && adjText.trim().indexOf(',') == -1 && adjText.length >= 12 && !addressKeywords.any { adjText.contains(it, ignoreCase = true) }
                    val isInvalidAddrLine = emailRegex.matcher(adjText).find() || 
                                           webRegex.matcher(adjText).find() ||
                                           faxPrefix.any { adjText.contains(it, ignoreCase = true) } ||
                                           serviceBlacklist.any { adjText.contains(it, ignoreCase = true) } ||
                                           isLikelySocialHandle
                    
                    if (!isInvalidAddrLine) {
                        foundAddressLines.add(0, adjText)
                        linesToRemove.add(adj)
                    }
                }

                // B. Forward (Downward) Proximity Scan
                val adjacentLines = remainingLines.filter { other ->
                    other != line && !linesToRemove.contains(other) &&
                    Math.abs((other.boundingBox?.top ?: 0) - y) < 45 &&
                    Math.abs((other.boundingBox?.left ?: 0) - xStart) < 150
                }
                adjacentLines.forEach { adj ->
                    val adjText = adj.text
                    val isLikelySocialHandle = adjText.trim().indexOf(' ') == -1 && adjText.trim().indexOf(',') == -1 && adjText.length >= 12 && !addressKeywords.any { adjText.contains(it, ignoreCase = true) }
                    val isInvalidAddrLine = emailRegex.matcher(adjText).find() || 
                                           webRegex.matcher(adjText).find() ||
                                           faxPrefix.any { adjText.contains(it, ignoreCase = true) } ||
                                           serviceBlacklist.any { adjText.contains(it, ignoreCase = true) } ||
                                           isLikelySocialHandle
                    
                    if (!isInvalidAddrLine) {
                        foundAddressLines.add(adjText)
                        linesToRemove.add(adj)
                    }
                }
            }
        }
        
        if (foundAddressLines.isNotEmpty()) {
            val newAddr = foundAddressLines.distinct().joinToString(", ")
            address = if (address.isEmpty()) newAddr else "$address, $newAddr"
            remainingLines.removeAll(linesToRemove)
        }

        // 10. Extras
        val extras = visionText.text

        return initial.copy(
            company = company,
            name = name,
            designation = designation,
            phone = phones.distinct(),
            email = emails.distinct(),
            website = websites.distinct(),
            address = address,
            fax = fax,
            gstin = gstin,
            extras = extras,
            rawOcrText = visionText.text,
            rawOcrBlocks = visionText.textBlocks.map { it.text }
        )
    }
}
