const Groq = require("groq-sdk");
const PDFParser = require("pdf2json");
const { createCanvas } = require("@napi-rs/canvas");
const { createWorker } = require("tesseract.js");

const HospitalModel = require("../new_models/new_hosp_model");

const groq = new Groq({
    apiKey: process.env.GROQ_API_KEY
});


// ======================================================
// NORMAL PDF TEXT EXTRACTION
// ======================================================

async function extractPdfText(buffer) {
    return await new Promise((resolve, reject) => {
        const pdfParser = new PDFParser(null, 1);

        pdfParser.on(
            "pdfParser_dataError",
            (err) => {
                reject(err.parserError);
            }
        );

        pdfParser.on(
            "pdfParser_dataReady",
            () => {
                try {
                    const rawText =
                        pdfParser.getRawTextContent();

                    const decodedText =
                        decodeURIComponent(rawText);

                    resolve(decodedText);
                } catch (error) {
                    reject(error);
                }
            }
        );

        pdfParser.parseBuffer(buffer);
    });
}


// ======================================================
// OCR PDF
// ======================================================

async function ocrPdf(buffer) {
    const pdfjsLib = await import(
        "pdfjs-dist/legacy/build/pdf.mjs"
    );

    const pdf = await pdfjsLib.getDocument({
        data: new Uint8Array(buffer),
        disableWorker: true
    }).promise;

    console.log(
        "PDF pages:",
        pdf.numPages
    );

    const worker = await createWorker("eng");

    let fullText = "";

    try {
        for (
            let pageNumber = 1;
            pageNumber <= pdf.numPages;
            pageNumber++
        ) {
            console.log(
                `OCR processing page ${pageNumber}/${pdf.numPages}`
            );

            const page =
                await pdf.getPage(pageNumber);

            // Higher scale = better OCR quality
            const scale = 2;
            const viewport =
                page.getViewport({
                    scale
                });

            const canvas =
                createCanvas(
                    Math.ceil(viewport.width),
                    Math.ceil(viewport.height)
                );

            const context =
                canvas.getContext("2d");

            await page.render({
                canvasContext: context,
                viewport: viewport
            }).promise;

            // Convert rendered page into PNG
            const imageBuffer =
                canvas.toBuffer("image/png");

            // OCR
            const result =
                await worker.recognize(
                    imageBuffer
                );

            console.log(
                "========== OCR RESULT =========="
            );

            console.log(
                result.data.text
            );

            console.log(
                "OCR TEXT LENGTH:",
                result.data.text.length
            );

            console.log(
                "================================"
            );

            fullText +=
                `\n\n--- Page ${pageNumber} ---\n\n`;

            fullText +=
                result.data.text;
        }
    } finally {
        await worker.terminate();
    }

    return fullText;
}


// ======================================================
// SCAN DOCTOR RESUME
// ======================================================

const scanDoctorResume = async (req, res) => {
    try {

        // ==================================================
        // CHECK FILE
        // ==================================================

        if (!req.file) {
            return res.status(400).json({
                success: false,
                message:
                    "No resume file provided. Please upload a PDF."
            });
        }

        // ==================================================
        // GET HOSPITAL ID
        // ==================================================

        const { hospitalId } = req.body;

        if (!hospitalId) {
            return res.status(400).json({
                success: false,
                message:
                    "hospitalId is required."
            });
        }

        // ==================================================
        // FETCH HOSPITAL
        // ==================================================

        const hospital =
            await HospitalModel
                .findOne({code:hospitalId})
                .select("departments");

        if (!hospital) {
            return res.status(404).json({
                success: false,
                message:
                    "Hospital not found."
            });
        }

        // ==================================================
        // GET HOSPITAL DEPARTMENTS
        // ==================================================

        const hospitalDepartments =
            hospital.departments || [];

        console.log(
            "Hospital departments:",
            hospitalDepartments
        );

        if (hospitalDepartments.length === 0) {
            return res.status(400).json({
                success: false,
                message:
                    "This hospital has no departments configured."
            });
        }

        let resumeText = "";

        // ==================================================
        // PDF FILE
        // ==================================================

        if (
            req.file.mimetype ===
            "application/pdf"
        ) {
            console.log(
                "Trying normal PDF text extraction..."
            );

            // ------------------------------------------------
            // STEP 1: NORMAL PDF TEXT EXTRACTION
            // ------------------------------------------------

            try {
                resumeText =
                    await extractPdfText(
                        req.file.buffer
                    );
            } catch (error) {
                console.log(
                    "Normal PDF extraction failed:",
                    error.message
                );
                resumeText = "";
            }

            console.log(
                "Normal extracted text length:",
                resumeText.length
            );

            // ------------------------------------------------
            // STEP 2: CHECK MEANINGFUL TEXT
            // ------------------------------------------------

            const meaningfulText =
                resumeText
                    .replace(
                        /[^a-zA-Z0-9]/g,
                        ""
                    )
                    .trim();

            console.log(
                "Meaningful text length:",
                meaningfulText.length
            );

            // ------------------------------------------------
            // STEP 3: OCR FALLBACK
            // ------------------------------------------------

            if (
                meaningfulText.length < 20
            ) {
                console.log(
                    "Little/no meaningful text found."
                );

                console.log(
                    "Starting OCR..."
                );

                resumeText =
                    await ocrPdf(
                        req.file.buffer
                    );

                console.log(
                    "OCR extracted text length:",
                    resumeText.length
                );
            }

        } else {
            // ==================================================
            // NON-PDF FILE
            // ==================================================

            resumeText =
                req.file.buffer.toString(
                    "utf8"
                );
        }

        // ==================================================
        // FINAL TEXT CHECK
        // ==================================================

        const finalMeaningfulText =
            resumeText
                .replace(
                    /[^a-zA-Z0-9]/g,
                    ""
                )
                .trim();

        if (
            !resumeText ||
            finalMeaningfulText.length < 20
        ) {
            return res.status(400).json({
                success: false,
                message:
                    "Could not extract readable text from the uploaded document."
            });
        }

        // ==================================================
        // DEBUG OCR / EXTRACTED TEXT
        // ==================================================

        console.log(
            "========== EXTRACTED RESUME TEXT =========="
        );

        console.log(
            resumeText
        );

        console.log(
            "==========================================="
        );

        // ==================================================
        // EXACT EMAIL EXTRACTION
        // ==================================================

        const emailMatch =
            resumeText.match(
                /[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}/i
            );

        const extractedEmail =
            emailMatch
                ? emailMatch[0]
                : null;

        // ==================================================
        // EXACT PHONE EXTRACTION
        // ==================================================

        const phoneMatches =
            resumeText.match(
                /(?:\+?\d[\d\s().-]{7,}\d)/g
            );

        let extractedPhone = null;

        if (phoneMatches) {
            extractedPhone =
                phoneMatches
                    .map(
                        phone =>
                            phone.trim()
                    )
                    .find(
                        phone => {
                            const digits =
                                phone.replace(
                                    /\D/g,
                                    ""
                                );

                            return (
                                digits.length >= 10 &&
                                digits.length <= 15
                            );
                        }
                    ) || null;
        }

        console.log(
            "Detected email:",
            extractedEmail
        );

        console.log(
            "Detected phone:",
            extractedPhone
        );

        // ==================================================
        // PREPARE TEXT FOR GROQ
        // ==================================================

        const MAX_RESUME_CHARS = 20000;

        const resumeForAI =
            resumeText.substring(
                0,
                MAX_RESUME_CHARS
            );


        // ==================================================
        // GROQ PROMPT
        // ==================================================

        const prompt = `
You are an AI assistant for a hospital management system.

Analyze the following medical professional's resume
and extract their details accurately.

Return ONLY a valid JSON object.

The JSON must contain exactly these keys:

{
    "name": string or null,
    "email": string or null,
    "phoneNumber": string or null,
    "qualification": string or null,
    "departments": [],
    "specializations": [],
    "experience": [
        {
            "role": string,
            "hospital": string,
            "startDate": string or null,
            "endDate": string or null,
            "duration": string or null,
            "highlights": []
        }
    ],
    "education": [
        {
            "degree": string,
            "institution": string,
            "year": string or null,
            "status": string or null
        }
    ],
    "rating": number,
    "matchScore": number,
    "matchSummary": string or null,
    "isAiVerified": boolean
}


==================================================
1. NAME
==================================================

Extract the doctor's complete name.
Do not invent a name.


==================================================
2. EMAIL
==================================================

Extract the doctor's email address.
If an email exists in the resume, return the exact email address. If none exists, return null.


==================================================
3. PHONE NUMBER
==================================================

Extract the doctor's phone/mobile number.
Do NOT interpret addresses, ZIP codes, years, URLs, or social handles as phone numbers. If none exists, return null.


==================================================
4. QUALIFICATION
==================================================

Extract medical degrees and certifications as a single string (e.g., "MBBS, MD, ACLS"). If unavailable, return null.


==================================================
5. DEPARTMENTS

The doctor is being enrolled in this hospital.
The hospital has ONLY these departments:

${hospitalDepartments.join(", ")}

Your task is to determine whether the doctor's resume provides
CLEAR evidence that the doctor belongs to one or more of these
existing hospital departments.

STRICT RULES:
1. You MUST select departments ONLY from the hospital department list above.
2. You MUST return the department name EXACTLY as it appears in the hospital department list.
3. DO NOT create, rename, or modify a department.
4. Do NOT select a department simply because it is loosely related. The resume must strongly indicate the doctor practices there.
5. If no match exists or there is insufficient evidence, return an empty array.


==================================================
6. SPECIALIZATIONS
==================================================

Extract the doctor's specific medical specializations, areas of expertise, and subspecialties from the resume.
Do not invent specializations.


==================================================
7. EXPERIENCE
==================================================

Extract the doctor's professional experience.
Each experience object must contain:
{
    "role": string,
    "hospital": string,
    "startDate": string or null,
    "endDate": string or null,
    "duration": string or null,
    "highlights": []
}
If the resume says "Current", use "endDate": "Current". Do not invent missing dates.


==================================================
8. EDUCATION
==================================================

Extract the doctor's education history (degrees, colleges/institutions, graduation years).
Each education object must contain:
{
    "degree": string,
    "institution": string,
    "year": string or null,
    "status": string or null
}


==================================================
9. RATING & MATCH SCORE
==================================================

- "rating": Give an estimated professional rating between 1 and 5 (default to 5.0 if insufficient info).
- "matchScore": Calculate a candidate match score out of 5.0 (e.g. 4.8) based on qualifications and hospital department alignment.
- "matchSummary": Provide a short summary sentence explaining the match score.
- "isAiVerified": Set to true if the profile contains valid credentials.


==================================================
IMPORTANT RULES
==================================================
- Do NOT invent information.
- Extract information only from the resume.
- Departments MUST come from the hospital department list.
- If information is genuinely missing, return null.
- For arrays, return [] if there is no information.
- Return valid JSON only.


==================================================
HOSPITAL DEPARTMENTS
==================================================

${hospitalDepartments.join(", ")}


==================================================
RESUME TEXT
==================================================

"""
${resumeForAI}
"""
`;


        // ==================================================
        // DEBUG WHAT IS SENT TO GROQ
        // ==================================================

        console.log(
            "========== TEXT SENT TO GROQ =========="
        );

        console.log(
            resumeForAI.substring(
                0,
                3000
            )
        );

        console.log(
            "========================================"
        );


        // ==================================================
        // GROQ API
        // ==================================================

        const chatCompletion =
            await groq.chat.completions.create({
                messages: [
                    {
                        role: "system",
                        content:
                            "You are a precise medical data extraction engine that outputs strictly valid JSON."
                    },
                    {
                        role: "user",
                        content:
                            prompt
                    }
                ],
              model: "openai/gpt-oss-120b",
                response_format: {
                    type: "json_object"
                }
            });


            
        // ==================================================
        // PARSE GROQ RESPONSE
        // ==================================================

        const extractedData =
            JSON.parse(
                chatCompletion
                    .choices[0]
                    ?.message
                    ?.content || "{}"
            );


        // ==================================================
        // OVERRIDE EMAIL WITH EXACT OCR VALUE
        // ==================================================

        if (extractedEmail) {
            extractedData.email =
                extractedEmail;
        }


        // ==================================================
        // OVERRIDE PHONE WITH EXACT OCR VALUE
        // ==================================================

        if (extractedPhone) {
            extractedData.phoneNumber =
                extractedPhone;
        }


        // ==================================================
        // SAFETY CHECK:
        // REMOVE ANY DEPARTMENT THAT IS NOT
        // ACTUALLY PRESENT IN THE HOSPITAL
        // ==================================================
        if (
            Array.isArray(extractedData.departments)
        ) {
            extractedData.departments =
                extractedData.departments.filter(
                    department =>
                        hospitalDepartments.includes(
                            department
                        )
                );
        } else {
            extractedData.departments = [];
        }


        // ======================================================
        // CHECK FOR MATCHING DEPARTMENT
        // ======================================================

        if (extractedData.departments.length === 0) {
            return res.status(200).json({
                success: false,
                message:
                    "No matching department found for this doctor in this hospital.",
                data: {
                    ...extractedData,
                    availableDepartments:
                        hospitalDepartments
                }
            });
        }


        // ==================================================
        // ENSURE ARRAYS EXIST AND ARE CORRECT TYPE
        // ==================================================

        if (
            !Array.isArray(
                extractedData.specializations
            )
        ) {
            extractedData.specializations = [];
        }

        if (
            !Array.isArray(
                extractedData.experience
            )
        ) {
            extractedData.experience = [];
        } else {
            // Ensure nested highlights array exists in each experience item
            extractedData.experience = extractedData.experience.map(exp => ({
                ...exp,
                highlights: Array.isArray(exp.highlights) ? exp.highlights : []
            }));
        }

        if (
            !Array.isArray(
                extractedData.education
            )
        ) {
            extractedData.education = [];
        }


        // ==================================================
        // RESPONSE
        // ==================================================

        return res.status(200).json({
            success: true,
            message:
                "Resume scanned successfully via Groq AI.",
            data: extractedData
        });

    } catch (error) {
        console.error(
            "Groq Resume Scan Error:",
            error
        );

        return res.status(500).json({
            success: false,
            message:
                "Failed to scan resume using AI.",
            error: error.message
        });
    }
};


// ======================================================
// EXPORT
// ======================================================

module.exports = {
    scanDoctorResume
};