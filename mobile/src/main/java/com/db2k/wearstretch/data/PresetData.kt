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
        "ic_launcher_foreground",
        "ic_launcher_background"
    )

    val presetStretches = listOf(
        // --- 1. Neck & Shoulders ---
        Stretch("12", "Chin Tuck", "Draw your head straight back as if making a double chin.", 15, 5, category = "Neck & Shoulders"),
        Stretch("14", "Clasp Open Stretch", "Interlace your fingers behind your back and gently lift your arms.", 30, 5, category = "Neck & Shoulders"),
        Stretch("10", "Cross-Body Shoulder Stretch", "Pull one arm straight across your chest using your opposite arm.", 30, 5, category = "Neck & Shoulders", isSplit = true),
        Stretch("13", "Eagle Arms Stretch", "Cross your arms at the elbows and wrists, then lift your elbows upward.", 30, 5, category = "Neck & Shoulders"),
        Stretch("9", "Levator Scapulae Stretch", "Look down into your armpit and gently pull your head forward.", 30, 5, category = "Neck & Shoulders", isSplit = true),
        Stretch("1", "Neck Stretch", "Gently tilt your head to one side.", 30, 5, category = "Neck & Shoulders", imageKey = "ic_launcher_foreground"),
        Stretch("2", "Shoulder Roll", "Roll your shoulders back and down.", 20, 5, category = "Neck & Shoulders"),
        Stretch("11", "Triceps Stretch", "Raise one arm overhead, bend the elbow, and push down gently with the other hand.", 30, 5, category = "Neck & Shoulders", isSplit = true),
        Stretch("8", "Upper Trapezius Stretch", "Gently pull your head down and sideways toward your shoulder.", 30, 5, category = "Neck & Shoulders", isSplit = true),

        // --- 2. Chest & Upper Back ---
        Stretch("16", "Cat-Cow Stretch", "Move between arching your back up and dipping it low on hands and knees.", 60, 5, category = "Chest & Upper Back"),
        Stretch("15", "Doorway Chest Stretch", "Place your forearms on a door frame and lean forward gently.", 45, 5, category = "Chest & Upper Back"),
        Stretch("19", "Prone T-Stretch", "Lie on your stomach with arms out wide, rotate your lower body to open one side of your chest.", 30, 5, category = "Chest & Upper Back", isSplit = true),
        Stretch("21", "Sphinx Pose", "Lie on your stomach propped up on your forearms, lifting your chest up and forward.", 45, 5, category = "Chest & Upper Back"),
        Stretch("17", "Thread the Needle", "Slide one arm along the floor underneath your torso, lowering your shoulder.", 30, 5, category = "Chest & Upper Back", isSplit = true),
        Stretch("18", "Upper Back Extension", "Sit or stand, interlace fingers in front of you, and push your palms away while rounding your back.", 30, 5, category = "Chest & Upper Back"),
        Stretch("20", "Wall Angels", "Flatten your back and arms against a wall, sliding your hands up and down slowly.", 45, 5, category = "Chest & Upper Back"),

        // --- 3. Arms & Wrists ---
        Stretch("24", "Bicep Wall Stretch", "Place one palm flat on a wall behind you and slowly rotate your torso away.", 30, 5, category = "Arms & Wrists", isSplit = true),
        Stretch("28", "Finger Fan Out", "Squeeze your hands into tight fists, then burst them open, flaring fingers wide.", 15, 5, category = "Arms & Wrists"),
        Stretch("26", "Praying Wrist Stretch", "Place your palms together in front of your chest and lower them toward your waist.", 30, 5, category = "Arms & Wrists"),
        Stretch("27", "Reverse Praying Stretch", "Place the backs of your hands together in front of your chest and lift elbows slightly.", 30, 5, category = "Arms & Wrists"),
        Stretch("25", "Wrist Circles", "Rotate your wrists slowly in a circular motion to improve mobility.", 20, 5, category = "Arms & Wrists"),
        Stretch("22", "Wrist Extensor Stretch", "Extend your arm straight out, palm down, and gently pull your fingers toward you.", 20, 5, category = "Arms & Wrists", isSplit = true),
        Stretch("23", "Wrist Flexor Stretch", "Extend your arm straight out, palm up, and gently pull your fingers downward.", 20, 5, category = "Arms & Wrists", isSplit = true),

        // --- 4. Lower Back & Core ---
        Stretch("5", "Child's Pose", "Kneel and lean forward with arms extended.", 60, 5, category = "Lower Back & Core"),
        Stretch("6", "Cobra Stretch", "Lie on stomach and push upper body up.", 30, 5, category = "Lower Back & Core"),
        Stretch("30", "Knees-to-Chest", "Lie on your back and hug both knees tightly into your chest.", 45, 5, category = "Lower Back & Core"),
        Stretch("34", "Pelvic Tilt", "Lie on your back with knees bent, flatten your lower back completely into the floor.", 30, 5, category = "Lower Back & Core"),
        Stretch("33", "Puppy Pose", "Keep your hips high over your knees and reach your arms far forward on the floor.", 45, 5, category = "Lower Back & Core"),
        Stretch("31", "Side Bend Stretch", "Reach one arm overhead and lean your torso deeply to the side.", 30, 5, category = "Lower Back & Core", isSplit = true),
        Stretch("32", "Sphinx Rotation", "From a sphinx pose, gently look over your shoulder toward your feet.", 20, 5, category = "Lower Back & Core", isSplit = true),
        Stretch("29", "Supine Spinal Twist", "Lie on your back, drop one bent knee across your body, and look the opposite way.", 45, 5, category = "Lower Back & Core", isSplit = true),
        Stretch("35", "Torso Twist", "Sit cross-legged and rotate your upper body to one side, placing your hand on your knee.", 30, 5, category = "Lower Back & Core", isSplit = true),

        // --- 5. Hips & Glutes ---
        Stretch("7", "Butterfly Stretch", "Sit with feet together and knees out.", 45, 5, category = "Hips & Glutes"),
        Stretch("37", "Figure-Four Stretch", "Lie on your back, cross one ankle over the opposite knee, and pull that thigh toward you.", 45, 5, category = "Hips & Glutes", isSplit = true),
        Stretch("39", "Frog Pose", "Widen your knees out to the sides on the floor and gently rock your hips backward.", 45, 5, category = "Hips & Glutes"),
        Stretch("42", "Happy Baby Pose", "Lie on your back, grab the outside edges of your feet, and pull your knees down toward the floor.", 45, 5, category = "Hips & Glutes"),
        Stretch("38", "Kneeling Hip Flexor Lunge", "Lunge forward onto one knee, tucking your pelvis to stretch the front of your back leg.", 30, 5, category = "Hips & Glutes", isSplit = true),
        Stretch("40", "Lizard Lunge", "Step one foot outside your hands into a deep lunge, dropping your hips low.", 45, 5, category = "Hips & Glutes", isSplit = true),
        Stretch("36", "Pigeon Pose", "Bring one bent leg forward flat on the floor with the other leg extended straight behind you.", 45, 5, category = "Hips & Glutes", isSplit = true),
        Stretch("41", "Seated Glute Twist", "Sit with one leg straight, cross the other foot over it, and hug that knee to your chest.", 30, 5, category = "Hips & Glutes", isSplit = true),

        // --- 6. Thighs (Quads & Hamstrings) ---
        Stretch("47", "Couch Stretch", "Place your back knee against a wall or couch while lunging forward to intensely stretch the quad.", 45, 5, category = "Thighs (Quads & Hamstrings)", isSplit = true),
        Stretch("46", "Half Splits", "Kneel on one knee, extend the front leg out straight with toes up, and lean forward.", 30, 5, category = "Thighs (Quads & Hamstrings)", isSplit = true),
        Stretch("3", "Hamstring Stretch", "Reach for your toes while keeping legs straight.", 45, 5, category = "Thighs (Quads & Hamstrings)", isSplit = true),
        Stretch("44", "Lying Quad Stretch", "Lie on your side or stomach and pull your foot back toward your glutes.", 30, 5, category = "Thighs (Quads & Hamstrings)", isSplit = true),
        Stretch("4", "Quadriceps Stretch", "Pull your heel towards your glutes.", 30, 5, category = "Thighs (Quads & Hamstrings)", isSplit = true),
        Stretch("43", "Seated Single-Leg Hamstring Stretch", "Extend one leg out, tuck the other foot in, and fold forward over the straight leg.", 45, 5, category = "Thighs (Quads & Hamstrings)", isSplit = true),
        Stretch("49", "Side Lunge Adductor Stretch", "Step out wide to the side, bend one knee deeply while keeping the other leg completely straight.", 30, 5, category = "Thighs (Quads & Hamstrings)", isSplit = true),
        Stretch("48", "Supine Hamstring Stretch", "Lie on your back, lift one leg straight into the air, and pull it gently toward your torso.", 45, 5, category = "Thighs (Quads & Hamstrings)", isSplit = true),
        Stretch("45", "Wide-Legged Forward Fold", "Stand with feet wide apart and slowly fold forward from your hips.", 45, 5, category = "Thighs (Quads & Hamstrings)"),

        // --- 7. Calves & Feet ---
        Stretch("54", "Ankle Rolls", "Lift one foot off the floor and roll the ankle in slow circles.", 20, 5, category = "Calves & Feet", isSplit = true),
        Stretch("51", "Downward Dog", "Push your hips up and back into an inverted V-shape, pressing your heels down.", 45, 5, category = "Calves & Feet"),
        Stretch("56", "Seated Foot Arch Stretch", "Sit down, cross one leg over, and use your hand to pull your toes back to stretch the bottom of your foot.", 30, 5, category = "Calves & Feet", isSplit = true),
        Stretch("57", "Shin Stretch", "Kneel with the tops of your feet flat on the floor, then lift your knees slightly upward.", 25, 5, category = "Calves & Feet"),
        Stretch("52", "Soleus Wall Stretch", "Stand close to a wall, step one foot back slightly, bend both knees, and press the back heel down.", 30, 5, category = "Calves & Feet", isSplit = true),
        Stretch("50", "Standing Calf Stretch", "Step one foot back, press that heel flat into the floor, and lean forward.", 30, 5, category = "Calves & Feet", isSplit = true),
        Stretch("55", "Step Calf Stretch", "Stand on the edge of a step and let your heel hang off the back, dropping it lower than the step.", 45, 5, category = "Calves & Feet", isSplit = true),
        Stretch("53", "Toe Squat", "Kneel on the floor with your toes tucked under, sitting back gently onto your heels.", 30, 5, category = "Calves & Feet")
    )

    val presetRoutines = listOf(
        Routine(
            id = "preset_morning",
            name = "Morning Wake-Up",
            stretches = listOf(
                presetStretches.find { it.id == "16" }!!, // Cat-Cow Stretch
                presetStretches.find { it.id == "5" }!!,  // Child's Pose
                presetStretches.find { it.id == "51" }!!, // Downward Dog
                presetStretches.find { it.id == "38" }!!, // Kneeling Hip Flexor Lunge
                presetStretches.find { it.id == "3" }!!,  // Hamstring Stretch
                presetStretches.find { it.id == "31" }!!, // Side Bend Stretch
                presetStretches.find { it.id == "2" }!!   // Shoulder Roll
            ),
            defaultBreakDurationSeconds = 5
        ),
        Routine(
            id = "preset_post_workout",
            name = "Full-Body Post-Workout",
            stretches = listOf(
                presetStretches.find { it.id == "15" }!!, // Doorway Chest Stretch
                presetStretches.find { it.id == "4" }!!,  // Quadriceps Stretch
                presetStretches.find { it.id == "36" }!!, // Pigeon Pose
                presetStretches.find { it.id == "48" }!!, // Supine Hamstring Stretch
                presetStretches.find { it.id == "50" }!!, // Standing Calf Stretch
                presetStretches.find { it.id == "29" }!!, // Supine Spinal Twist
                presetStretches.find { it.id == "30" }!!  // Knees-to-Chest
            ),
            defaultBreakDurationSeconds = 10
        ),
        Routine(
            id = "preset_desk_worker",
            name = "5-Minute Desk-Worker Reset",
            stretches = listOf(
                presetStretches.find { it.id == "12" }!!, // Chin Tuck
                presetStretches.find { it.id == "18" }!!, // Upper Back Extension
                presetStretches.find { it.id == "22" }!!, // Wrist Extensor Stretch
                presetStretches.find { it.id == "23" }!!  // Wrist Flexor Stretch
            ),
            defaultBreakDurationSeconds = 5
        ),
        Routine(
            id = "preset_pre_bed",
            name = "Pre-Bed Relaxation",
            stretches = listOf(
                presetStretches.find { it.id == "5" }!!,  // Child's Pose
                presetStretches.find { it.id == "33" }!!, // Puppy Pose
                presetStretches.find { it.id == "7" }!!,  // Butterfly Stretch
                presetStretches.find { it.id == "37" }!!, // Figure-Four Stretch
                presetStretches.find { it.id == "42" }!!, // Happy Baby Pose
                presetStretches.find { it.id == "6" }!!   // Cobra Stretch
            ),
            defaultBreakDurationSeconds = 8
        )
    )
}
