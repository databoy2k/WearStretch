package com.db2k.wearstretch.data

import com.db2k.wearstretch.model.Routine
import com.db2k.wearstretch.model.Stretch

object PresetData {
    val presetCategories = listOf(
        "Neck & Shoulders",
        "Chest & Upper Back",
        "Arms & Wrists",
        "Lower Back & Core",
        "Hips & Glutes",
        "Thighs (Quads & Hamstrings)",
        "Calves & Feet"
    )

    val presetAnimations = listOf(
        "ic_launcher_foreground"
    )

    // Hold times: for two-sided stretches (isSplit) durationSeconds is the TOTAL
    // for both sides, so 60 = 30 s per side. Targets follow common static-stretch
    // guidance: ~30 s per hold, ~60 s per muscle group per session.
    val presetStretches = listOf(
        // --- 1. Neck & Shoulders ---
        Stretch("12", "Chin Tuck", "Draw your head straight back as if making a double chin.", 15, 5, category = "Neck & Shoulders", imageKey = "stretch_12"),
        Stretch("14", "Clasp Open Stretch", "Interlace your fingers behind your back and gently lift your arms.", 30, 5, category = "Neck & Shoulders", imageKey = "stretch_14"),
        Stretch("10", "Cross-Body Shoulder Stretch", "Pull one arm straight across your chest using your opposite arm.", 60, 5, category = "Neck & Shoulders", isSplit = true, imageKey = "stretch_10"),
        Stretch("13", "Eagle Arms Stretch", "Cross your arms at the elbows and wrists, then lift your elbows upward.", 30, 5, category = "Neck & Shoulders", imageKey = "stretch_13"),
        Stretch("9", "Levator Scapulae Stretch", "Look down into your armpit and gently pull your head forward.", 60, 5, category = "Neck & Shoulders", isSplit = true, imageKey = "stretch_9"),
        Stretch("1", "Neck Stretch", "Gently tilt your ear toward your shoulder.", 60, 5, category = "Neck & Shoulders", isSplit = true, imageKey = "stretch_1"),
        Stretch("2", "Shoulder Roll", "Roll your shoulders back and down.", 20, 5, category = "Neck & Shoulders", imageKey = "stretch_2"),
        Stretch("11", "Triceps Stretch", "Raise one arm overhead, bend the elbow, and push down gently with the other hand.", 60, 5, category = "Neck & Shoulders", isSplit = true, imageKey = "stretch_11"),
        Stretch("8", "Upper Trapezius Stretch", "Gently pull your head down and sideways toward your shoulder.", 60, 5, category = "Neck & Shoulders", isSplit = true, imageKey = "stretch_8"),

        // --- 2. Chest & Upper Back ---
        Stretch("16", "Cat-Cow Stretch", "Move between arching your back up and dipping it low on hands and knees.", 60, 5, category = "Chest & Upper Back", imageKey = "stretch_16"),
        Stretch("15", "Doorway Chest Stretch", "Place your forearms on a door frame and lean forward gently.", 45, 5, category = "Chest & Upper Back", imageKey = "stretch_15"),
        Stretch("19", "Prone T-Stretch", "Lie on your stomach with arms out wide, rotate your lower body to open one side of your chest.", 60, 5, category = "Chest & Upper Back", isSplit = true, imageKey = "stretch_19"),
        Stretch("21", "Sphinx Pose", "Lie on your stomach propped up on your forearms, lifting your chest up and forward.", 45, 5, category = "Chest & Upper Back", imageKey = "stretch_21"),
        Stretch("17", "Thread the Needle", "Slide one arm along the floor underneath your torso, lowering your shoulder.", 60, 5, category = "Chest & Upper Back", isSplit = true, imageKey = "stretch_17"),
        Stretch("18", "Upper Back Extension", "Sit or stand, interlace fingers in front of you, and push your palms away while rounding your back.", 30, 5, category = "Chest & Upper Back", imageKey = "stretch_18"),
        Stretch("20", "Wall Angels", "Flatten your back and arms against a wall, sliding your hands up and down slowly.", 45, 5, category = "Chest & Upper Back", imageKey = "stretch_20"),

        // --- 3. Arms & Wrists ---
        Stretch("24", "Bicep Wall Stretch", "Place one palm flat on a wall behind you and slowly rotate your torso away.", 60, 5, category = "Arms & Wrists", isSplit = true, imageKey = "stretch_24"),
        Stretch("28", "Finger Fan Out", "Squeeze your hands into tight fists, then burst them open, flaring fingers wide.", 15, 5, category = "Arms & Wrists", imageKey = "stretch_28"),
        Stretch("26", "Praying Wrist Stretch", "Place your palms together in front of your chest and lower them toward your waist.", 30, 5, category = "Arms & Wrists", imageKey = "stretch_26"),
        Stretch("27", "Reverse Praying Stretch", "Place the backs of your hands together in front of your chest and lift elbows slightly.", 30, 5, category = "Arms & Wrists", imageKey = "stretch_27"),
        Stretch("25", "Wrist Circles", "Rotate your wrists slowly in a circular motion to improve mobility.", 20, 5, category = "Arms & Wrists", imageKey = "stretch_25"),
        Stretch("22", "Wrist Extensor Stretch", "Extend your arm straight out, palm down, and gently pull your fingers toward you.", 40, 5, category = "Arms & Wrists", isSplit = true, imageKey = "stretch_22"),
        Stretch("23", "Wrist Flexor Stretch", "Extend your arm straight out, palm up, and gently pull your fingers downward.", 40, 5, category = "Arms & Wrists", isSplit = true, imageKey = "stretch_23"),

        // --- 4. Lower Back & Core ---
        Stretch("5", "Child's Pose", "Kneel and lean forward with arms extended.", 60, 5, category = "Lower Back & Core", imageKey = "stretch_5"),
        Stretch("6", "Cobra Stretch", "Lie on stomach and push upper body up.", 30, 5, category = "Lower Back & Core", imageKey = "stretch_6"),
        Stretch("30", "Knees-to-Chest", "Lie on your back and hug both knees tightly into your chest.", 45, 5, category = "Lower Back & Core", imageKey = "stretch_30"),
        Stretch("34", "Pelvic Tilt", "Lie on your back with knees bent, flatten your lower back completely into the floor.", 30, 5, category = "Lower Back & Core", imageKey = "stretch_34"),
        Stretch("33", "Puppy Pose", "Keep your hips high over your knees and reach your arms far forward on the floor.", 45, 5, category = "Lower Back & Core", imageKey = "stretch_33"),
        Stretch("31", "Side Bend Stretch", "Reach one arm overhead and lean your torso deeply to the side.", 60, 5, category = "Lower Back & Core", isSplit = true, imageKey = "stretch_31"),
        Stretch("32", "Sphinx Rotation", "From a sphinx pose, gently look over your shoulder toward your feet.", 40, 5, category = "Lower Back & Core", isSplit = true, imageKey = "stretch_32"),
        Stretch("29", "Supine Spinal Twist", "Lie on your back, drop one bent knee across your body, and look the opposite way.", 60, 5, category = "Lower Back & Core", isSplit = true, imageKey = "stretch_29"),
        Stretch("35", "Torso Twist", "Sit cross-legged and rotate your upper body to one side, placing your hand on your knee.", 60, 5, category = "Lower Back & Core", isSplit = true, imageKey = "stretch_35"),

        // --- 5. Hips & Glutes ---
        Stretch("7", "Butterfly Stretch", "Sit with feet together and knees out.", 45, 5, category = "Hips & Glutes", imageKey = "stretch_7"),
        Stretch("37", "Figure-Four Stretch", "Lie on your back, cross one ankle over the opposite knee, and pull that thigh toward you.", 60, 5, category = "Hips & Glutes", isSplit = true, imageKey = "stretch_37"),
        Stretch("39", "Frog Pose", "Widen your knees out to the sides on the floor and gently rock your hips backward.", 45, 5, category = "Hips & Glutes", imageKey = "stretch_39"),
        Stretch("42", "Happy Baby Pose", "Lie on your back, grab the outside edges of your feet, and pull your knees down toward the floor.", 45, 5, category = "Hips & Glutes", imageKey = "stretch_42"),
        Stretch("38", "Kneeling Hip Flexor Lunge", "Lunge forward onto one knee, tucking your pelvis to stretch the front of your back leg.", 60, 5, category = "Hips & Glutes", isSplit = true, imageKey = "stretch_38"),
        Stretch("40", "Lizard Lunge", "Step one foot outside your hands into a deep lunge, dropping your hips low.", 60, 5, category = "Hips & Glutes", isSplit = true, imageKey = "stretch_40"),
        Stretch("36", "Pigeon Pose", "Bring one bent leg forward flat on the floor with the other leg extended straight behind you.", 60, 5, category = "Hips & Glutes", isSplit = true, imageKey = "stretch_36"),
        Stretch("41", "Seated Glute Twist", "Sit with one leg straight, cross the other foot over it, and hug that knee to your chest.", 60, 5, category = "Hips & Glutes", isSplit = true, imageKey = "stretch_41"),

        // --- 6. Thighs (Quads & Hamstrings) ---
        Stretch("47", "Couch Stretch", "Place your back knee against a wall or couch while lunging forward to intensely stretch the quad.", 60, 5, category = "Thighs (Quads & Hamstrings)", isSplit = true, imageKey = "stretch_47"),
        Stretch("46", "Half Splits", "Kneel on one knee, extend the front leg out straight with toes up, and lean forward.", 60, 5, category = "Thighs (Quads & Hamstrings)", isSplit = true, imageKey = "stretch_46"),
        Stretch("3", "Hamstring Stretch", "Reach for your toes while keeping legs straight.", 60, 5, category = "Thighs (Quads & Hamstrings)", isSplit = true, imageKey = "stretch_3"),
        Stretch("44", "Lying Quad Stretch", "Lie on your side or stomach and pull your foot back toward your glutes.", 60, 5, category = "Thighs (Quads & Hamstrings)", isSplit = true, imageKey = "stretch_44"),
        Stretch("4", "Quadriceps Stretch", "Pull your heel towards your glutes.", 60, 5, category = "Thighs (Quads & Hamstrings)", isSplit = true, imageKey = "stretch_4"),
        Stretch("43", "Seated Single-Leg Hamstring Stretch", "Extend one leg out, tuck the other foot in, and fold forward over the straight leg.", 60, 5, category = "Thighs (Quads & Hamstrings)", isSplit = true, imageKey = "stretch_43"),
        Stretch("49", "Side Lunge Adductor Stretch", "Step out wide to the side, bend one knee deeply while keeping the other leg completely straight.", 60, 5, category = "Thighs (Quads & Hamstrings)", isSplit = true, imageKey = "stretch_49"),
        Stretch("48", "Supine Hamstring Stretch", "Lie on your back, lift one leg straight into the air, and pull it gently toward your torso.", 60, 5, category = "Thighs (Quads & Hamstrings)", isSplit = true, imageKey = "stretch_48"),
        Stretch("45", "Wide-Legged Forward Fold", "Stand with feet wide apart and slowly fold forward from your hips.", 45, 5, category = "Thighs (Quads & Hamstrings)", imageKey = "stretch_45"),

        // --- 7. Calves & Feet ---
        Stretch("54", "Ankle Rolls", "Lift one foot off the floor and roll the ankle in slow circles.", 40, 5, category = "Calves & Feet", isSplit = true, imageKey = "stretch_54"),
        Stretch("51", "Downward Dog", "Push your hips up and back into an inverted V-shape, pressing your heels down.", 45, 5, category = "Calves & Feet", imageKey = "stretch_51"),
        Stretch("56", "Seated Foot Arch Stretch", "Sit down, cross one leg over, and use your hand to pull your toes back to stretch the bottom of your foot.", 60, 5, category = "Calves & Feet", isSplit = true, imageKey = "stretch_56"),
        Stretch("57", "Shin Stretch", "Kneel with the tops of your feet flat on the floor, then lift your knees slightly upward.", 25, 5, category = "Calves & Feet", imageKey = "stretch_57"),
        Stretch("52", "Soleus Wall Stretch", "Stand close to a wall, step one foot back slightly, bend both knees, and press the back heel down.", 60, 5, category = "Calves & Feet", isSplit = true, imageKey = "stretch_52"),
        Stretch("50", "Standing Calf Stretch", "Step one foot back, press that heel flat into the floor, and lean forward.", 60, 5, category = "Calves & Feet", isSplit = true, imageKey = "stretch_50"),
        Stretch("55", "Step Calf Stretch", "Stand on the edge of a step and let your heel hang off the back, dropping it lower than the step.", 60, 5, category = "Calves & Feet", isSplit = true, imageKey = "stretch_55"),
        Stretch("53", "Toe Squat", "Kneel on the floor with your toes tucked under, sitting back gently onto your heels.", 30, 5, category = "Calves & Feet", imageKey = "stretch_53"),
    )

    private fun st(id: String) = presetStretches.first { it.id == id }

    val presetRoutines = listOf(
        // Gentle, partly dynamic: good first thing in the morning.
        Routine(
            id = "preset_morning",
            name = "Morning Wake-Up",
            stretches = listOf(
                st("16"), // Cat-Cow Stretch
                st("5"),  // Child's Pose
                st("51"), // Downward Dog
                st("38"), // Kneeling Hip Flexor Lunge
                st("3"),  // Hamstring Stretch
                st("31"), // Side Bend Stretch
                st("2")   // Shoulder Roll
            ),
            defaultBreakDurationSeconds = 5
        ),
        // Standing → kneeling → floor, so you only get down once.
        Routine(
            id = "preset_post_workout",
            name = "Full-Body Post-Workout",
            stretches = listOf(
                st("15"), // Doorway Chest Stretch
                st("50"), // Standing Calf Stretch
                st("4"),  // Quadriceps Stretch
                st("38"), // Kneeling Hip Flexor Lunge
                st("36"), // Pigeon Pose
                st("48"), // Supine Hamstring Stretch
                st("29"), // Supine Spinal Twist
                st("30")  // Knees-to-Chest
            ),
            defaultBreakDurationSeconds = 10
        ),
        // Neck, chest and upper back from hunching; hips from sitting; wrists from typing.
        Routine(
            id = "preset_desk_worker",
            name = "5-Minute Desk-Worker Reset",
            stretches = listOf(
                // Shorter holds than the library here so the whole thing fits ~5 min.
                st("12"),                                 // Chin Tuck
                st("8"),                                  // Upper Trapezius Stretch (30 s/side)
                st("15").copy(durationSeconds = 30),      // Doorway Chest Stretch
                st("38").copy(durationSeconds = 40),      // Kneeling Hip Flexor Lunge (20 s/side)
                st("41").copy(durationSeconds = 40),      // Seated Glute Twist (20 s/side)
                st("22"),                                 // Wrist Extensor Stretch (20 s/side)
                st("23")                                  // Wrist Flexor Stretch (20 s/side)
            ),
            defaultBreakDurationSeconds = 5
        ),
        // Calm and on the floor; ends lying down (no energising backbend at the end).
        Routine(
            id = "preset_pre_bed",
            name = "Pre-Bed Relaxation",
            stretches = listOf(
                st("5"),  // Child's Pose
                st("33"), // Puppy Pose
                st("7"),  // Butterfly Stretch
                st("37"), // Figure-Four Stretch
                st("42"), // Happy Baby Pose
                st("29"), // Supine Spinal Twist
                st("30")  // Knees-to-Chest
            ),
            defaultBreakDurationSeconds = 8
        ),
        // Terry's traditional routine. Order and most holds are his; calf, quad,
        // hip flexor, triceps, biceps and neck raised to 30 s per side.
        Routine(
            id = "preset_full_stretch",
            name = "Stretching Routine",
            stretches = listOf(
                st("50").copy(durationSeconds = 60, breakDurationSeconds = 8), // Standing Calf Stretch
                st("4").copy(durationSeconds = 60, breakDurationSeconds = 8),  // Quadriceps Stretch
                st("38").copy(durationSeconds = 60, breakDurationSeconds = 8), // Kneeling Hip Flexor Lunge
                st("7").copy(durationSeconds = 40, breakDurationSeconds = 8),  // Butterfly Stretch
                st("35").copy(durationSeconds = 40, breakDurationSeconds = 8), // Torso Twist
                st("43").copy(durationSeconds = 60, breakDurationSeconds = 8), // Seated Single-Leg Hamstring Stretch
                st("37").copy(durationSeconds = 50, breakDurationSeconds = 8), // Figure-Four Stretch
                st("29").copy(durationSeconds = 50, breakDurationSeconds = 8), // Supine Spinal Twist
                st("6").copy(durationSeconds = 30, breakDurationSeconds = 8),  // Cobra Stretch
                st("5").copy(durationSeconds = 45, breakDurationSeconds = 8),  // Child's Pose
                st("18").copy(durationSeconds = 30, breakDurationSeconds = 8), // Upper Back Extension
                st("11").copy(durationSeconds = 60, breakDurationSeconds = 8), // Triceps Stretch
                st("24").copy(durationSeconds = 60, breakDurationSeconds = 8), // Bicep Wall Stretch
                st("1").copy(durationSeconds = 60, breakDurationSeconds = 8)   // Neck Stretch
            ),
            defaultBreakDurationSeconds = 8
        ),
        // After push/pull/upper-body days (also what openGym upper days get suggested).
        Routine(
            id = "preset_upper_cooldown",
            name = "Upper-Body Cool-Down",
            stretches = listOf(
                st("15"), // Doorway Chest Stretch
                st("10"), // Cross-Body Shoulder Stretch
                st("11"), // Triceps Stretch
                st("24"), // Bicep Wall Stretch
                st("8"),  // Upper Trapezius Stretch
                st("17"), // Thread the Needle
                st("5")   // Child's Pose
            ),
            defaultBreakDurationSeconds = 8
        ),
        // After leg days, runs and rides.
        Routine(
            id = "preset_lower_cooldown",
            name = "Lower-Body Cool-Down",
            stretches = listOf(
                st("50"), // Standing Calf Stretch
                st("52"), // Soleus Wall Stretch
                st("4"),  // Quadriceps Stretch
                st("49"), // Side Lunge Adductor Stretch
                st("38"), // Kneeling Hip Flexor Lunge
                st("48"), // Supine Hamstring Stretch
                st("37"), // Figure-Four Stretch
                st("29")  // Supine Spinal Twist
            ),
            defaultBreakDurationSeconds = 8
        ),
        // Gentle, mostly lying down: mobilise first, then hold.
        Routine(
            id = "preset_lower_back",
            name = "Lower-Back Relief",
            stretches = listOf(
                st("34"), // Pelvic Tilt
                st("30"), // Knees-to-Chest
                st("29"), // Supine Spinal Twist
                st("37"), // Figure-Four Stretch
                st("21"), // Sphinx Pose
                st("16"), // Cat-Cow Stretch
                st("5")   // Child's Pose
            ),
            defaultBreakDurationSeconds = 8
        )
    )
}
