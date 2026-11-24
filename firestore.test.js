const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");
const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_director_1";
const BOB_UID = "bob_director_2";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users cannot read or write any LumenReel collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("projects").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_1").set({
      ownerId: ALICE_UID,
      title: "Unauthorized Film",
      genreStyle: "Neo-Noir",
      defaultSourceType: "book",
      continuityBible: "",
      sequencesCount: 0,
      assetsCount: 0,
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Owner creates profile with studio config, project, sequence, milestones, storyboard asset, agent message, and lesson", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const past = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      displayName: "Elena Morales",
      email: "elena@lumenreel.studio",
      role: "director",
      preferredStyle: "Épica Monumental IMAX",
      themeMode: "dark",
      defaultAspectRatio: "2.39:1",
      defaultThinkingDepth: "STANDARD",
      autoSaveDrafts: true,
      stage3DDefault: true,
      lastProjectId: "",
      lastSequenceId: "",
      lastTab: "agent",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Valid configuration update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      themeMode: "light",
      defaultAspectRatio: "1.43:1",
      defaultThinkingDepth: "DEEP",
      autoSaveDrafts: false,
      stage3DDefault: false,
      lastProjectId: "proj_1",
      lastSequenceId: "seq_1",
      lastTab: "screenplay",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Invalid themeMode / aspectRatio rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      themeMode: "neon_invalid",
      updatedAt: new Date(Date.now() - 900),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      defaultAspectRatio: "4:3",
      updatedAt: new Date(Date.now() - 900),
    })
  );

  // Reject ghost `id` field when @DocumentId is used in Kotlin
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_bad").set({
      id: "proj_bad",
      ownerId: ALICE_UID,
      title: "Bad Payload",
      genreStyle: "IMAX",
      defaultSourceType: "book",
      continuityBible: "",
      sequencesCount: 0,
      assetsCount: 0,
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_1").set({
      ownerId: ALICE_UID,
      title: "El Puerto de Bruma",
      genreStyle: "Thriller Psicológico Neo-Noir",
      defaultSourceType: "book",
      continuityBible: "Regla 1: lluvia constante.",
      templateId: "feature_script",
      productionFolders: [
        {
          id: "ft_script",
          name: "01_Guion_Tecnico_Actos_I_III",
          category: "script",
          description: "Guion maestro",
          notes: "",
          targetTab: "screenplay",
        },
      ],
      sequencesCount: 0,
      assetsCount: 0,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Milestone CRUD
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_1")
      .collection("milestones")
      .doc("ms_1")
      .set({
        projectId: "proj_1",
        ownerId: ALICE_UID,
        title: "Rodaje Muelle Nocturno",
        notes: "Llevar lentes anamórficos",
        dateMillis: Date.now(),
        milestoneType: "shooting_day",
        sequenceId: "seq_1",
        completed: false,
        createdAt: past,
        updatedAt: past,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_1")
      .collection("milestones")
      .doc("ms_1")
      .update({
        completed: true,
        updatedAt: new Date(Date.now() - 800),
      })
  );

  // Update productionFolders on owned project with notes and linkedSequenceIds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_1").update({
      productionFolders: [
        {
          id: "ft_script",
          name: "01_Guion_Tecnico_Actos_I_III",
          category: "script",
          description: "Guion maestro",
          notes: "Revisar Acto II",
          targetTab: "screenplay",
          linkedSequenceIds: ["seq_1"],
        },
      ],
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject >20 productionFolders
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_overflow").set({
      ownerId: ALICE_UID,
      title: "Too Many Folders",
      genreStyle: "IMAX",
      defaultSourceType: "book",
      continuityBible: "",
      templateId: "custom",
      productionFolders: Array.from({ length: 21 }, (_, idx) => ({ id: `f_${idx}`, name: `Folder ${idx}` })),
      sequencesCount: 0,
      assetsCount: 0,
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_1")
      .collection("sequences")
      .doc("seq_1")
      .set({
        projectId: "proj_1",
        ownerId: ALICE_UID,
        title: "Capítulo I: El Muelle",
        versionStatus: "draft",
        sourceType: "book",
        directorStyle: "Thriller Psicológico Neo-Noir",
        aspectRatio: "2.39:1",
        logline: "Un inspector busca un barco desaparecido entre la niebla.",
        directorVision: "Tonos de sodio y sombras densas.",
        continuitySummary: "El faro parpadea cada 4 segundos.",
        sequenceData: {
          cameraSystem: "RED V-Raptor XL",
          lensPackage: "Cooke Anamorphic",
        },
        createdAt: past,
        updatedAt: past,
      })
  );

  // Finalize sequence and rate finalized sequence
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_1")
      .collection("sequences")
      .doc("seq_1")
      .update({
        versionStatus: "final",
        updatedAt: new Date(Date.now() - 500),
      })
  );

  // Storyboard asset write succeeds even on finalized sequences
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_1")
      .collection("storyboard_assets")
      .doc("seq_1_sb_1")
      .set({
        projectId: "proj_1",
        ownerId: ALICE_UID,
        sequenceId: "seq_1",
        frameId: "sb_1",
        localImagePath: "/data/user/0/com.aistudio.lumenreel.kxvnqr/files/sb_1.png",
        updatedAt: new Date(Date.now() - 300),
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_1")
      .collection("sequences")
      .doc("seq_1")
      .update({
        sequenceRating: 5,
        updatedAt: new Date(Date.now() - 250),
      })
  );

  // Finalized sequence content cannot be overwritten or deleted
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_1")
      .collection("sequences")
      .doc("seq_1")
      .update({
        title: "Edited Final",
        updatedAt: new Date(Date.now() - 100),
      })
  );
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_1")
      .collection("sequences")
      .doc("seq_1")
      .delete()
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_1")
      .collection("agent_messages")
      .doc("msg_1")
      .set({
        projectId: "proj_1",
        ownerId: ALICE_UID,
        role: "agent",
        focus: "council",
        content: "Propuesta de iluminación Kelvin a 3200K.",
        actions: [{ toolName: "actualizar_biblia_continuidad", summary: "Actualizada", affectedArea: "biblia" }],
        createdAt: past,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("learning_memory")
      .doc("lesson_1")
      .set({
        ownerId: ALICE_UID,
        category: "camera",
        insight: "Cooke Anamorphic a T2.0 refuerza la intimidad en lluvia.",
        rating: 5,
        sourceProjectTitle: "El Puerto de Bruma",
        createdAt: past,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_1")
      .collection("milestones")
      .doc("ms_1")
      .delete()
  );
});

test("Cross-user isolation blocks Bob from reading or modifying Alice workspace and milestones", async () => {
  const past = new Date(Date.now() - 5000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const projRef = ctx.firestore().collection("users").doc(ALICE_UID).collection("projects").doc("proj_a");
    await projRef.set({
      ownerId: ALICE_UID,
      title: "Proyecto Privado",
      genreStyle: "IMAX",
      defaultSourceType: "book",
      continuityBible: "",
      sequencesCount: 0,
      assetsCount: 0,
      createdAt: past,
      updatedAt: past,
    });
    await projRef.collection("milestones").doc("ms_a").set({
      projectId: "proj_a",
      ownerId: ALICE_UID,
      title: "Privado",
      notes: "",
      dateMillis: Date.now(),
      milestoneType: "deadline",
      completed: false,
      createdAt: past,
      updatedAt: past,
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("projects").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_a").collection("milestones").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_a").update({
      continuityBible: "Hijacked",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Rejects future timestamps and privilege escalation on user profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      displayName: "Admin Spoofer",
      email: "a@b.com",
      role: "admin",
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      displayName: "Clock Tamperer",
      email: "a@b.com",
      role: "director",
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});

test("Owner saves, updates label/note, and deletes script_versions while blocking invalid payloads and cross-user reads", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_sv").set({
      ownerId: ALICE_UID,
      title: "Guion Versionado",
      genreStyle: "Drama",
      defaultSourceType: "story",
      continuityBible: "",
      sequencesCount: 1,
      assetsCount: 0,
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_sv")
      .collection("script_versions")
      .doc("sv_1")
      .set({
        projectId: "proj_sv",
        ownerId: ALICE_UID,
        sequenceId: "seq_1",
        sequenceTitle: "Acto I",
        versionNumber: 1,
        versionLabel: "v1 · Borrador inicial",
        changeNote: "Primer corte de diálogos",
        sceneCount: 1,
        wordCount: 24,
        scenes: [
          {
            sceneNumber: "1",
            slugline: "INT. SALA - NOCHE",
            timeOfDay: "NOCHE",
            beats: [{ type: "action", text: "Silencio en el estudio." }],
          },
        ],
        createdAt: past,
        updatedAt: past,
      })
  );

  // Label & note update allowed
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_sv")
      .collection("script_versions")
      .doc("sv_1")
      .update({
        versionLabel: "v1 · Revisado",
        changeNote: "Ajuste de ritmo en escena 1",
        updatedAt: new Date(Date.now() - 500),
      })
  );

  // Mutating immutable scenes snapshot or versionNumber on existing snapshot fails
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_sv")
      .collection("script_versions")
      .doc("sv_1")
      .update({
        versionNumber: 99,
        updatedAt: new Date(Date.now() - 400),
      })
  );

  // Ghost `id` field rejected
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_sv")
      .collection("script_versions")
      .doc("sv_bad")
      .set({
        id: "sv_bad",
        projectId: "proj_sv",
        ownerId: ALICE_UID,
        sequenceId: "seq_1",
        sequenceTitle: "Acto I",
        versionNumber: 2,
        versionLabel: "v2",
        changeNote: "",
        sceneCount: 0,
        wordCount: 0,
        scenes: [],
        createdAt: past,
        updatedAt: past,
      })
  );

  // Cross-user read/write blocked
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_sv")
      .collection("script_versions")
      .get()
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_sv")
      .collection("script_versions")
      .doc("sv_1")
      .delete()
  );
});
