package com.university.chatbotyarmouk.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;


import jo.edu.yu.chatbot.dto.response.*;
import jo.edu.yu.chatbot.security.UserPrincipal;
import jo.edu.yu.chatbot.service.SisService;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * StudentController - Student Data REST API Controller
 *
 * PURPOSE:
 * This controller provides authenticated students access to their
 * Student Information System (SIS) data:
 * - Profile information (name, ID, college, department)
 * - GPA and academic standing
 * - Current/past classes and grades
 * - Attendance records
 * - Financial information (optional)
 *
 * ACCESS CONTROL:
 * - All endpoints require STUDENT role
 * - Guests and Admins cannot access these endpoints
 * - Each student can only see their own data
 *
 * SIS INTEGRATION (Real-Life Analogy):
 *
 * Think of this like your university student portal:
 * - You log in with your student credentials
 * - You can view YOUR grades, schedule, etc.
 * - You cannot see other students' information
 * - Data comes from the university's central system
 *
 * MOCK vs REAL SIS:
 * - For development/testing: SisMockService provides fake data
 * - For production: Real SIS integration via API or database
 * - Configured in application.properties: sis.mock.enabled=true/false
 *
 * SECURITY ANNOTATIONS:
 * @PreAuthorize("hasRole('STUDENT')") - Method-level security
 * - Checks if authenticated user has STUDENT role
 * - Returns 403 Forbidden if not
 * - Applied at class level (affects all methods)
 *
 * @author YU ChatBot Team
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
@Tag(name = "Student", description = "Student profile and academic data endpoints")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('STUDENT')")  // All endpoints require STUDENT role
public class StudentController {

    /*
     * ==================== DEPENDENCY INJECTION ====================
     *
     * SisService: Interface for Student Information System
     * - Can be SisMockService (testing) or real implementation
     * - Spring injects the appropriate implementation based on config
     */
    private final SisService sisService;

    /**
     * GET STUDENT PROFILE
     *
     * Retrieves the authenticated student's profile information.
     *
     * HTTP Details:
     * - Method: GET
     * - URL: /api/student/profile
     * - Auth: Required (STUDENT role only)
     *
     * Response Example:
     * ```json
     * {
     *   "studentId": "20201234",
     *   "name": "Ahmad Hassan",
     *   "email": "20201234@yu.edu.jo",
     *   "college": "Faculty of Information Technology",
     *   "department": "Computer Science",
     *   "level": "Senior",
     *   "enrollmentYear": 2020,
     *   "status": "ACTIVE",
     *   "advisor": "Dr. Mohammad Ali",
     *   "photoUrl": "https://sis.yu.edu.jo/photos/20201234.jpg"
     * }
     * ```
     *
     * @param principal Authenticated student (from JWT token)
     * @return Student profile information
     *
     * Real-Life Analogy:
     * Like viewing your student ID card details:
     * - Your name, photo, student number
     * - College and major information
     * - Academic status
     */
    @GetMapping("/profile")
    @Operation(
            summary = "Get Student Profile",
            description = "Retrieve the authenticated student's profile information " +
                    "including personal details, college, and enrollment status."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Profile retrieved successfully",
                    content = @Content(schema = @Schema(implementation = StudentProfileResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Not authorized (not a student)"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Student profile not found in SIS"
            )
    })
    public ResponseEntity<StudentProfileResponse> getProfile(
            @AuthenticationPrincipal UserPrincipal principal) {

        log.info("Fetching profile for student: {}", principal.getStudentId());

        /*
         * principal.getStudentId():
         * - Returns the student's university ID (e.g., "20201234")
         * - This was set during login from SIS data
         * - Used to fetch student-specific information
         */
        StudentProfileResponse profile = sisService.getStudentProfile(
                principal.getStudentId()
        );

        log.debug("Profile retrieved for student: {}", principal.getStudentId());

        return ResponseEntity.ok(profile);
    }

    /**
     * GET STUDENT GPA
     *
     * Retrieves the student's GPA and academic standing.
     *
     * HTTP Details:
     * - Method: GET
     * - URL: /api/student/gpa
     * - Auth: Required (STUDENT role)
     *
     * Response Example:
     * ```json
     * {
     *   "cumulativeGpa": 3.45,
     *   "semesterGpa": 3.67,
     *   "totalCredits": 95,
     *   "completedCredits": 90,
     *   "academicStanding": "GOOD_STANDING",
     *   "dean'sList": true,
     *   "gpaHistory": [
     *     {"semester": "Fall 2023", "gpa": 3.67, "credits": 15},
     *     {"semester": "Spring 2023", "gpa": 3.50, "credits": 16},
     *     {"semester": "Fall 2022", "gpa": 3.25, "credits": 15}
     *   ]
     * }
     * ```
     *
     * @param principal Authenticated student
     * @return GPA and academic information
     *
     * Real-Life Analogy:
     * Like checking your transcript summary:
     * - Overall GPA
     * - This semester's performance
     * - Academic standing status
     */
    @GetMapping("/gpa")
    @Operation(
            summary = "Get Student GPA",
            description = "Retrieve the student's cumulative GPA, semester GPA, " +
                    "academic standing, and GPA history."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "GPA information retrieved successfully",
                    content = @Content(schema = @Schema(implementation = StudentGpaResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Not authorized (not a student)"
            )
    })
    public ResponseEntity<StudentGpaResponse> getGpa(
            @AuthenticationPrincipal UserPrincipal principal) {

        log.info("Fetching GPA for student: {}", principal.getStudentId());

        StudentGpaResponse gpa = sisService.getStudentGpa(principal.getStudentId());

        return ResponseEntity.ok(gpa);
    }

    /**
     * GET STUDENT CLASSES
     *
     * Retrieves the student's current and past classes.
     * Can filter by semester or show all.
     *
     * HTTP Details:
     * - Method: GET
     * - URL: /api/student/classes
     * - Auth: Required (STUDENT role)
     * - Query Params: semester (optional), current (optional)
     *
     * Request Examples:
     * - GET /api/student/classes                     → All classes
     * - GET /api/student/classes?current=true        → Current semester only
     * - GET /api/student/classes?semester=Fall2023   → Specific semester
     *
     * Response Example:
     * ```json
     * {
     *   "currentSemester": "Spring 2024",
     *   "classes": [
     *     {
     *       "courseCode": "CS492",
     *       "courseName": "Senior Project",
     *       "instructor": "Dr. Sara Ahmad",
     *       "credits": 3,
     *       "schedule": "Sun/Tue 10:00-11:30",
     *       "room": "IT-201",
     *       "grade": null,
     *       "status": "IN_PROGRESS"
     *     },
     *     {
     *       "courseCode": "CS480",
     *       "courseName": "Artificial Intelligence",
     *       "instructor": "Dr. Mohammad Ali",
     *       "credits": 3,
     *       "schedule": "Mon/Wed 14:00-15:30",
     *       "room": "IT-305",
     *       "grade": null,
     *       "status": "IN_PROGRESS"
     *     }
     *   ],
     *   "totalCredits": 15
     * }
     * ```
     *
     * @param principal Authenticated student
     * @param semester Optional semester filter (e.g., "Fall2023")
     * @param current If true, return only current semester classes
     * @return List of classes
     *
     * Real-Life Analogy:
     * Like viewing your class schedule:
     * - What courses you're taking
     * - When and where they meet
     * - Who's teaching them
     */
    @GetMapping("/classes")
    @Operation(
            summary = "Get Student Classes",
            description = "Retrieve the student's enrolled classes. " +
                    "Can filter by semester or show only current classes."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Classes retrieved successfully",
                    content = @Content(schema = @Schema(implementation = StudentClassesResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Not authorized (not a student)"
            )
    })
    public ResponseEntity<StudentClassesResponse> getClasses(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String semester,
            @RequestParam(required = false, defaultValue = "false") boolean current) {

        log.info("Fetching classes for student: {}, semester: {}, current: {}",
                principal.getStudentId(), semester, current);

        StudentClassesResponse classes;

        if (current) {
            // Get only current semester classes
            classes = sisService.getCurrentClasses(principal.getStudentId());
        } else if (semester != null && !semester.isEmpty()) {
            // Get classes for specific semester
            classes = sisService.getClassesBySemester(principal.getStudentId(), semester);
        } else {
            // Get all classes (all semesters)
            classes = sisService.getAllClasses(principal.getStudentId());
        }

        return ResponseEntity.ok(classes);
    }

    /**
     * GET STUDENT ATTENDANCE
     *
     * Retrieves the student's attendance records.
     * Shows attendance percentage per course.
     *
     * HTTP Details:
     * - Method: GET
     * - URL: /api/student/attendance
     * - Auth: Required (STUDENT role)
     * - Query Params: courseCode (optional)
     *
     * Response Example:
     * ```json
     * {
     *   "semester": "Spring 2024",
     *   "overallAttendance": 92.5,
     *   "courses": [
     *     {
     *       "courseCode": "CS492",
     *       "courseName": "Senior Project",
     *       "totalClasses": 20,
     *       "attended": 19,
     *       "absences": 1,
     *       "attendancePercentage": 95.0,
     *       "status": "GOOD",
     *       "absenceDetails": [
     *         {"date": "2024-02-15", "reason": "MEDICAL", "excused": true}
     *       ]
     *     },
     *     {
     *       "courseCode": "CS480",
     *       "courseName": "Artificial Intelligence",
     *       "totalClasses": 18,
     *       "attended": 16,
     *       "absences": 2,
     *       "attendancePercentage": 88.9,
     *       "status": "WARNING",
     *       "absenceDetails": [...]
     *     }
     *   ],
     *   "warnings": [
     *     "CS480: Attendance below 90%. Risk of FA grade."
     *   ]
     * }
     * ```
     *
     * @param principal Authenticated student
     * @param courseCode Optional filter for specific course
     * @return Attendance records
     *
     * Real-Life Analogy:
     * Like checking your attendance record:
     * - How many classes you attended
     * - Percentage per course
     * - Warnings if attendance is low
     */
    @GetMapping("/attendance")
    @Operation(
            summary = "Get Student Attendance",
            description = "Retrieve the student's attendance records for current semester. " +
                    "Shows attendance percentage and any warnings."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Attendance records retrieved successfully",
                    content = @Content(schema = @Schema(implementation = StudentAttendanceResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Not authorized (not a student)"
            )
    })
    public ResponseEntity<StudentAttendanceResponse> getAttendance(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String courseCode) {

        log.info("Fetching attendance for student: {}", principal.getStudentId());

        StudentAttendanceResponse attendance;

        if (courseCode != null && !courseCode.isEmpty()) {
            attendance = sisService.getCourseAttendance(
                    principal.getStudentId(),
                    courseCode
            );
        } else {
            attendance = sisService.getAttendance(principal.getStudentId());
        }

        return ResponseEntity.ok(attendance);
    }

    /**
     * GET STUDENT SCHEDULE
     *
     * Retrieves the student's weekly class schedule.
     * Formatted for calendar display.
     *
     * HTTP Details:
     * - Method: GET
     * - URL: /api/student/schedule
     * - Auth: Required (STUDENT role)
     *
     * Response Example:
     * ```json
     * {
     *   "semester": "Spring 2024",
     *   "schedule": {
     *     "Sunday": [
     *       {"time": "10:00-11:30", "course": "CS492", "room": "IT-201"}
     *     ],
     *     "Monday": [
     *       {"time": "14:00-15:30", "course": "CS480", "room": "IT-305"}
     *     ],
     *     "Tuesday": [
     *       {"time": "10:00-11:30", "course": "CS492", "room": "IT-201"}
     *     ],
     *     ...
     *   }
     * }
     * ```
     *
     * @param principal Authenticated student
     * @return Weekly schedule
     */
    @GetMapping("/schedule")
    @Operation(
            summary = "Get Student Schedule",
            description = "Retrieve the student's weekly class schedule for the current semester."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Schedule retrieved successfully",
                    content = @Content(schema = @Schema(implementation = StudentScheduleResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Not authorized (not a student)"
            )
    })
    public ResponseEntity<StudentScheduleResponse> getSchedule(
            @AuthenticationPrincipal UserPrincipal principal) {

        log.info("Fetching schedule for student: {}", principal.getStudentId());

        StudentScheduleResponse schedule = sisService.getSchedule(
                principal.getStudentId()
        );

        return ResponseEntity.ok(schedule);
    }

    /**
     * GET STUDENT GRADES
     *
     * Retrieves detailed grades for all courses.
     * Includes assignment grades, midterms, finals.
     *
     * HTTP Details:
     * - Method: GET
     * - URL: /api/student/grades
     * - Auth: Required (STUDENT role)
     * - Query Params: semester (optional)
     *
     * Response Example:
     * ```json
     * {
     *   "semester": "Spring 2024",
     *   "courses": [
     *     {
     *       "courseCode": "CS492",
     *       "courseName": "Senior Project",
     *       "credits": 3,
     *       "grades": {
     *         "assignments": [
     *           {"name": "Proposal", "score": 95, "maxScore": 100, "weight": 10},
     *           {"name": "Progress Report", "score": 88, "maxScore": 100, "weight": 15}
     *         ],
     *         "midterm": {"score": 42, "maxScore": 50, "weight": 25},
     *         "final": null,
     *         "project": {"score": null, "maxScore": 100, "weight": 50}
     *       },
     *       "currentGrade": "A-",
     *       "currentPercentage": 91.5
     *     }
     *   ]
     * }
     * ```
     *
     * @param principal Authenticated student
     * @param semester Optional semester filter
     * @return Detailed grades
     */
    @GetMapping("/grades")
    @Operation(
            summary = "Get Student Grades",
            description = "Retrieve detailed grades including assignments, exams, and projects."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Grades retrieved successfully",
                    content = @Content(schema = @Schema(implementation = StudentGradesResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Not authorized (not a student)"
            )
    })
    public ResponseEntity<StudentGradesResponse> getGrades(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String semester) {

        log.info("Fetching grades for student: {}", principal.getStudentId());

        StudentGradesResponse grades;

        if (semester != null && !semester.isEmpty()) {
            grades = sisService.getGradesBySemester(principal.getStudentId(), semester);
        } else {
            grades = sisService.getCurrentGrades(principal.getStudentId());
        }

        return ResponseEntity.ok(grades);
    }

    /**
     * GET FINANCIAL INFORMATION
     *
     * Retrieves the student's financial status.
     * Includes fees, payments, and balances.
     *
     * HTTP Details:
     * - Method: GET
     * - URL: /api/student/financial
     * - Auth: Required (STUDENT role)
     *
     * Response Example:
     * ```json
     * {
     *   "semester": "Spring 2024",
     *   "tuitionFees": 1500.00,
     *   "otherFees": 150.00,
     *   "totalFees": 1650.00,
     *   "paid": 1000.00,
     *   "balance": 650.00,
     *   "dueDate": "2024-03-15",
     *   "status": "PARTIAL_PAYMENT",
     *   "payments": [
     *     {"date": "2024-01-15", "amount": 1000.00, "method": "BANK_TRANSFER", "receiptNo": "RCP123"}
     *   ],
     *   "scholarships": [
     *     {"name": "Merit Scholarship", "amount": 500.00, "status": "APPLIED"}
     *   ]
     * }
     * ```
     *
     * @param principal Authenticated student
     * @return Financial information
     */
    @GetMapping("/financial")
    @Operation(
            summary = "Get Financial Information",
            description = "Retrieve the student's financial status including fees, payments, and balance."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Financial information retrieved successfully",
                    content = @Content(schema = @Schema(implementation = StudentFinancialResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Not authorized (not a student)"
            )
    })
    public ResponseEntity<StudentFinancialResponse> getFinancialInfo(
            @AuthenticationPrincipal UserPrincipal principal) {

        log.info("Fetching financial info for student: {}", principal.getStudentId());

        StudentFinancialResponse financial = sisService.getFinancialInfo(
                principal.getStudentId()
        );

        return ResponseEntity.ok(financial);
    }

    /**
     * GET ACADEMIC TRANSCRIPT
     *
     * Retrieves the student's complete academic transcript.
     * Shows all courses taken with grades.
     *
     * @param principal Authenticated student
     * @return Complete transcript
     */
    @GetMapping("/transcript")
    @Operation(
            summary = "Get Academic Transcript",
            description = "Retrieve the student's complete academic transcript."
    )
    public ResponseEntity<StudentTranscriptResponse> getTranscript(
            @AuthenticationPrincipal UserPrincipal principal) {

        log.info("Fetching transcript for student: {}", principal.getStudentId());

        StudentTranscriptResponse transcript = sisService.getTranscript(
                principal.getStudentId()
        );

        return ResponseEntity.ok(transcript);
    }
}

/*
 * ==================== FRONTEND INTEGRATION EXAMPLES ====================
 *
 * 1. Fetch Student Profile:
 * ```javascript
 * async function getProfile() {
 *     const response = await fetch('/api/student/profile', {
 *         headers: {
 *             'Authorization': `Bearer ${getToken()}`
 *         }
 *     });
 *
 *     if (response.status === 403) {
 *         // User is not a student
 *         throw new Error('Only students can access this feature');
 *     }
 *
 *     return response.json();
 * }
 * ```
 *
 * 2. Fetch GPA with Error Handling:
 * ```javascript
 * async function getGpa() {
 *     try {
 *         const response = await fetch('/api/student/gpa', {
 *             headers: {
 *                 'Authorization': `Bearer ${getToken()}`
 *             }
 *         });
 *
 *         if (!response.ok) {
 *             throw new Error(`HTTP error! status: ${response.status}`);
 *         }
 *
 *         return response.json();
 *     } catch (error) {
 *         console.error('Failed to fetch GPA:', error);
 *         throw error;
 *     }
 * }
 * ```
 *
 * 3. Display Schedule:
 * ```javascript
 * async function displaySchedule() {
 *     const schedule = await fetch('/api/student/schedule', {
 *         headers: { 'Authorization': `Bearer ${getToken()}` }
 *     }).then(r => r.json());
 *
 *     // Render schedule calendar
 *     Object.entries(schedule.schedule).forEach(([day, classes]) => {
 *         classes.forEach(classInfo => {
 *             console.log(`${day}: ${classInfo.course} at ${classInfo.time}`);
 *         });
 *     });
 * }
 * ```
 */